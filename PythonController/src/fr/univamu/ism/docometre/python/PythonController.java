/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.docometre.python;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;

import py4j.ClientServer;
import py4j.ClientServer.ClientServerBuilder;

public final class PythonController  {
	
	// Polling period used while waiting for the Python process
	private static final long POLLING_PERIOD = 20;
	// Maximum length of a message sent to the output listener
	private static final int MAXIMUM_MESSAGE_LENGTH = 10000;
	// Marks the end of Python output in the lines queue
	private static final String END_OF_OUTPUT = new String("");
	
	public static synchronized PythonController getInstance() {
		if(pythonController == null) pythonController = new PythonController();
		return pythonController;
	}

	private static PythonController pythonController;
	private ClientServer server;
	private volatile boolean isStarted;
	private ProcessBuilder pythonProcessBuilder;
	private JavaEntryPoint javaEntryPoint;
	private Process pythonProcess;
	private File logFile;
	private volatile Consumer<String> outputListener;
	private Thread outputReaderThread;
	
	private PythonController() {
	}
	
	public boolean startServer(String pythonLocation, String pythonScriptsLocation, int timeOut, IProgressMonitor monitor, int javaPort, int pythonPort) throws Exception {
		try {
			javaEntryPoint = new JavaEntryPoint();
			ClientServerBuilder clientServerBuilder = new ClientServer.ClientServerBuilder(javaEntryPoint);
			clientServerBuilder.javaPort(javaPort);
			clientServerBuilder.pythonPort(pythonPort);
			clientServerBuilder.autoStartJavaServer(false);
			server = clientServerBuilder.build();
			server.getJavaServer().start();
			
			pythonProcessBuilder = new ProcessBuilder(pythonLocation, pythonScriptsLocation + File.separator + "DOCoMETRe.py", "--jvm", "--javaPort", String.valueOf(javaPort), "--pythonPort", String.valueOf(pythonPort));
			// Python output (stdout and stderr) is read by a dedicated thread, written to a log file and sent to the output listener
			logFile = new File(System.getProperty("java.io.tmpdir"), "DOCoMETRe_Python_" + pythonPort + ".log");
			pythonProcessBuilder.redirectErrorStream(true);
			pythonProcessBuilder.environment().put("PYTHONUNBUFFERED", "1");
			pythonProcessBuilder.environment().put("PYTHONIOENCODING", "utf-8");
			pythonProcess = pythonProcessBuilder.start();
			startOutputThreads(pythonProcess);
			
			long t0 = System.currentTimeMillis();
			while (System.currentTimeMillis() - t0 < timeOut*1000 && getPythonEntryPoint() == null && pythonProcess.isAlive() && !monitor.isCanceled()) {
				Thread.sleep(POLLING_PERIOD);
			}
			
			if(!pythonProcess.isAlive() && pythonProcess.exitValue() != 0) {
				outputReaderThread.join(1000); // all output written to log file
				Throwable throwable = new Throwable(readLogFile());
				throw new ExecutionException(throwable);
			}
			
			isStarted = getPythonEntryPoint() != null && pythonProcess.isAlive();
		} catch (Exception e) {
			isStarted = false;
			throw e;
		} finally {
			// Time out, user cancellation or error : release ports and process so that a new start is possible
			if(!isStarted) release();
		}
		
		return isStarted;
	}
	
	public void stopServer(int timeOut) throws InterruptedException {
		try {
			PythonEntryPoint pythonEntryPoint = getPythonEntryPoint();
			if(pythonEntryPoint != null) pythonEntryPoint.shutDownServer(this);
		} catch (Exception e) {
			// Python may close the connection before answering : this is not an error
		}
		if(server != null) server.shutdown();
		server = null;
		
		if(pythonProcess != null) {
			// Let Python exit by itself, then kill it (and its children, e.g. Windows "py" launcher) if necessary
			if(!pythonProcess.waitFor(timeOut, TimeUnit.SECONDS)) {
				pythonProcess.descendants().forEach(ProcessHandle::destroy);
				pythonProcess.destroy();
				if(!pythonProcess.waitFor(timeOut, TimeUnit.SECONDS)) pythonProcess.destroyForcibly();
			}
			isStarted = pythonProcess.isAlive();
		} else isStarted = false;
	}
	
	/*
	 * Reader thread : always reads the pipe (an unread pipe blocks Python as soon as it is full) and writes the log file.
	 * Forwarder thread : groups lines received close together and sends them to the output listener, which may block (UI).
	 */
	private void startOutputThreads(Process process) {
		File currentLogFile = logFile;
		BlockingQueue<String> lines = new ArrayBlockingQueue<>(10000);
		outputReaderThread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
				 Writer writer = Files.newBufferedWriter(currentLogFile.toPath(), StandardCharsets.UTF_8)) {
				String line;
				while ((line = reader.readLine()) != null) {
					writer.write(line);
					writer.write(System.lineSeparator());
					if(!reader.ready()) writer.flush();
					lines.offer(line); // never blocks : lines are dropped if the listener is too slow
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					lines.put(END_OF_OUTPUT);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}, "Python output reader");
		outputReaderThread.setDaemon(true);
		outputReaderThread.start();
		
		Thread forwarderThread = new Thread(() -> {
			try {
				boolean end = false;
				while (!end) {
					String line = lines.take();
					if(line == END_OF_OUTPUT) break;
					StringBuilder message = new StringBuilder(line);
					String next;
					while (message.length() < MAXIMUM_MESSAGE_LENGTH && (next = lines.poll(100, TimeUnit.MILLISECONDS)) != null) {
						if(next == END_OF_OUTPUT) {
							end = true;
							break;
						}
						message.append('\n').append(next);
					}
					if(message.length() > MAXIMUM_MESSAGE_LENGTH) {
						message.setLength(MAXIMUM_MESSAGE_LENGTH);
						message.append("... (see ").append(currentLogFile.getAbsolutePath()).append(")");
					}
					Consumer<String> listener = outputListener;
					if(listener != null) {
						try {
							listener.accept(message.toString());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "Python output forwarder");
		forwarderThread.setDaemon(true);
		forwarderThread.start();
	}
	
	/**
	 * @param outputListener receives Python standard and error outputs (grouped lines), called from a non UI thread
	 */
	public void setOutputListener(Consumer<String> outputListener) {
		this.outputListener = outputListener;
	}
	
	private void release() {
		if(server != null) {
			try {
				server.shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}
			server = null;
		}
		if(pythonProcess != null && pythonProcess.isAlive()) {
			pythonProcess.descendants().forEach(ProcessHandle::destroy);
			pythonProcess.destroy();
		}
	}
	
	private String readLogFile() {
		if(logFile == null || !logFile.exists()) return "";
		try {
			return new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return e.getMessage();
		}
	}
	
	/**
	 * @return the file where Python standard and error outputs are written
	 */
	public File getLogFile() {
		return logFile;
	}
	
	public boolean isStarted() {
		return isStarted;
	}

	public JavaEntryPoint getJavaEntryPoint() {
		return javaEntryPoint;
	}
	
	public PythonEntryPoint getPythonEntryPoint() {
		return javaEntryPoint == null ? null : javaEntryPoint.getPythonEntryPoint();
	}
	
	

}
