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
package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.osgi.framework.Bundle;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.ModuleBehaviour;
import fr.univamu.ism.docometre.dacqsystems.ParameterVariable;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.TrialTimings;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.dacqconfigurationeditor.ArduinoUnoDACQConfigurationEditor;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.processeditor.ArduinoUnoProcessEditor;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionDialog;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionHandler.DeviceType;
import fr.univamu.ism.docometre.dacqsystems.ui.ProcessEditor;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.views.ExperimentsView;
import fr.univamu.ism.process.GlobalDeclarations;
import fr.univamu.ism.process.ScriptSegmentType;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

@SuppressWarnings("restriction")
public class ArduinoUnoProcess extends Process {
	
	private static double displayTimeRate = 0.1; 
	
	// Message sent by the Arduino when it has (re)started and waits for 'r' char (see end handshake in RealTimeLoopJob)
	private static final String READY_MESSAGE = "w";
	
	// Arduino Uno R3 : number of bytes of strings variables above which a warning is given at compile time (2048 bytes of RAM)
	private static final int STRINGS_BYTES_WARNING_R3 = 512;
	
	// Sending of the parameters before the start of the process (see RealTimeLoopJob.sendParameters()) :
	// maximum wait for the answer of the Arduino (A or N frame) and number of attempts. The answer comes
	// within a few ms : the time out is reached only if the board does not answer (process compiled before
	// parameters management, board issue).
	private static final long PARAMETERS_ANSWER_TIMEOUT_MS = 1000;
	private static final int PARAMETERS_SENDING_ATTEMPTS = 2;

	public static final long serialVersionUID = DACQConfiguration.serialVersionUID;
	public static String previousINOHexFilePath = "";
	public static boolean forceUpload = false;
	
//	private static String processPathForMacOSX(String path) {
//		if(Platform.getOS().equals(Platform.OS_MACOSX)) {
//			// Replace all spaces by "/" in adbasicfilePath by "\"
//			String[] pathSplitted = path.split("/");
//			path = "";
//			for (int i = 0; i < pathSplitted.length; i++) {
//				path = path + pathSplitted[i];
//				if(i < pathSplitted.length - 1) path = path + "\\";
//			}
//		}
//		return path;
//	}
	
	public static File createSketchFile(IResource processFile, Process process, String sketchFilePath) {
		return createSketchFile(processFile, process, sketchFilePath, false);
	}
	
	/*
	 * If compilation is true, the signature of the compiled code is saved : compilation markers
	 * will be linked to blocks as long as the process code is unchanged
	 */
	private static File createSketchFile(IResource processFile, Process process, String sketchFilePath, boolean compilation) {
		// Create Sketch file
		File file = new File(sketchFilePath);
		file.getParentFile().mkdirs();
		try(FileWriter fileWriter = new FileWriter(file)) {
			// Get process code
			String processCode = process.getCode(null);
			if(compilation) ResourceProperties.setCompiledCodeSignature(processFile, processCode, 0);
			// Write it to file
			fileWriter.write(processCode);
			return file;
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
	}
	
	private class RealTimeLoopJob extends Job {

		// End handshake with the Arduino (see run()) : maximum waiting times. The PC only
		// waits until the expected char is received : these durations are reached only
		// if the char never comes (process compiled before the handshake, board issue).
		// Maximum wait for the 's' char of the Arduino after a stop requested by the user
		// (same as the former fixed wait)
		private static final long STOP_REPLY_TIMEOUT_MS = 300;
		// Maximum wait for the 'w' char sent by the Arduino when it has restarted and waits
		// for 'r'. The process is now restarted by software (a few ms). A process compiled
		// before restarts with a board reset (watchdog) : measured about 530 ms, 1000 ms
		// gives a margin for slower boards.
		private static final long READY_TIMEOUT_MS = 1000;
		// Maximum wait for the 's' char of the Arduino after a stop requested by the user, when parameters are
		// propagated : the Arduino sends them before 's' and waits for the answer of the PC (2 attempts of
		// about 500 ms at most, see ArduinoUnoParametersCode).
		private static final long STOP_REPLY_WITH_PROPAGATION_TIMEOUT_MS = 1500;
		
		private SerialPort serialPort;
		private boolean terminate;
		private StringBuilder message;
		private float time;
		private float value;
		private float realTime, workload;
		private Channel[] transferedChannelsOrderedByTransferNumber;
		private int[] nbSamples;
		private int trsfrNum;
		private DecimalFormat formater;
		private boolean forceTermination;
		private boolean stopReceived;
		private double timeBefore;
		private double timeAfter;
//		private String channelName;
		// Timing trace (see TrialTimings)
		private boolean processEndTraced;
		private boolean firstDataTraced;
		// Propagated parameters : expected at the end of the process, reception in progress (see handleParametersFrame())
		private boolean propagationExpected = hasPropagatedParameters();
		private ArduinoUnoParameterFrames.Reception propagationReception;
		private long propagationStartNanos;
		// 'w' char received while waiting for the 's' char of the Arduino (see waitForStopReply())
		private boolean readyReceived;

		public RealTimeLoopJob(String name) {
			super(name);
			try {
				message = new StringBuilder();
				DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
        		dfs.setDecimalSeparator('.');
        		formater = new DecimalFormat("0.000000", dfs);
				serialPort = new SerialPort(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH));
				if(serialPort.isOpened()) serialPort.closePort();
				serialPort.openPort();
				String baudRate = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.BAUD_RATE);
				serialPort.setParams(Integer.parseInt(baudRate), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, true, true);
				Channel[] transferedChannels = getDACQConfiguration().getTransferedChannels();
				transferedChannelsOrderedByTransferNumber = new Channel[transferedChannels.length];
				for (int i = 0; i < transferedChannelsOrderedByTransferNumber.length; i++) {
					trsfrNum = Integer.parseInt(transferedChannels[i].getProperty(ChannelProperties.TRANSFER_NUMBER));
					transferedChannelsOrderedByTransferNumber[trsfrNum - 1] = transferedChannels[i];
				}
				nbSamples = new int[transferedChannelsOrderedByTransferNumber.length];
				appendToEventDiary("Transfered channels :");
				for (Channel channel : transferedChannelsOrderedByTransferNumber) {
					appendToEventDiary(channel.getProperty(ChannelProperties.NAME));
				}
				appendToEventDiary("\n");
//				Thread.sleep(100);
			} catch (Exception e) {
				Activator.logErrorMessageWithCause(e);
				try {
					serialPort.closePort();
				} catch (SerialPortException e1) {
					Activator.logErrorMessageWithCause(e1);
					e1.printStackTrace();
				}
			}
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			double processBeginTime = 0;
			// Timing trace of the loop iterations (see TrialTimings)
			long iterations = 0, lastIterationNanos = 0, maxIterationNanos = 0;
			
			try {
				getThread().setPriority(Thread.MAX_PRIORITY);
				
				serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
				serialPort.purgePort(SerialPort.PURGE_RXABORT | SerialPort.PURGE_RXCLEAR);
				TrialTimings.step("RealTimeLoopJob started (serial port purged)");
				
				timeBefore = System.currentTimeMillis()/1000d;
				serialPort.writeString("r");
				TrialTimings.end("Start : 'r' sent to Arduino");
				
				processBeginTime = System.currentTimeMillis()/1000d;
				if(Process.processEndTime > -1) {
					double dt = processBeginTime - Process.processEndTime;
					appendToEventDiary(NLS.bind(ArduinoUnoMessages.ArduinoDiary_TimeSinceLastProcessStopped, dt));
				}
				String date = new SimpleDateFormat("EEE d MMM yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
				appendToEventDiary(date);
				appendToEventDiary("Starting " + ObjectsController.getResourceForObject(ArduinoUnoProcess.this).getName() + " at " + Double.toString(processBeginTime) + " sec.\n");
				
				
				while(!terminate) {
					TrialTimings.firstLoopIteration();
					if(monitor.isCanceled()) {
						forceTermination = true;
						monitor.setCanceled(false);
					}
	    			if(serialPort.getInputBufferBytesCount() > 0) {
	    				// Timing trace : only the iterations which read bytes are measured (the loop polls the serial port)
	    				long iterationStart = TrialTimings.trace ? System.nanoTime() : 0;
	    				byte[] bytes = serialPort.readBytes();
		            	for (byte b: bytes) {
		                    if ( (b == '\r' || b == '\n') && message.length() > 0 && !terminate) {
		                    	String messageString = message.toString().replaceAll("\\r", "").replaceAll("\\n", "");
		                    	if(messageString.length() == 0) continue;
		                    	message.setLength(0);
		                    	// 'w' : the board has restarted and waits for 'r' (see end handshake), not a data message
		                    	if(READY_MESSAGE.equals(messageString)) continue;
		                    	// Parameters frame : N0 is sent at the start when the process expects parameters that were not sent
		                    	ArduinoUnoParameterFrames.Frame parametersFrame = ArduinoUnoParameterFrames.parse(messageString);
		                    	if(parametersFrame != null) {
		                    		handleParametersFrame(parametersFrame);
		                    		continue;
		                    	}
		                    	String[] segments = messageString.split(":");
		                    	
			                    if(segments.length != 2 && segments.length != 3) {
			                    	appendToEventDiary("ERROR : wrong segments number (" + segments.length + " instead of 2 or 3) - Parse message : " + messageString);
									appendErrorMarkerAtCurrentDiaryLine("ERROR");
									continue;
			                    }
			                    	
		                    	if(segments.length == 2) {
		                    		try {
										trsfrNum = Integer.parseInt(segments[0]);
										value = Float.parseFloat(segments[1]);
									} catch (Exception e) {
										Activator.logErrorMessageWithCause(e);
										e.printStackTrace();
										appendToEventDiary("ERROR : " + e.getMessage() + " - Parse message : " + messageString);
										appendErrorMarkerAtCurrentDiaryLine("ERROR");
										PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
											@Override
											public void run() {
												StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
												statusLineManger.setErrorMessage(Activator.getImage(IImageKeys.ERROR_ANNOTATION_ICON), ADWinMessages.ProcessDataLoss_Label);
											}
										});
										continue;
									} 
		                    	}
		                    	
		                    	if(segments.length == 3) {
		                    		try {
										trsfrNum = 0;
										time = Float.parseFloat(segments[1])/1000000f; 
										value = Float.parseFloat(segments[2]);
									} catch (Exception e) {
										Activator.logErrorMessageWithCause(e);
										e.printStackTrace();
										appendToEventDiary("ERROR : " + e.getMessage() + " - Parse message : " + messageString);
										appendErrorMarkerAtCurrentDiaryLine("ERROR");
										PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
											@Override
											public void run() {
												StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
												statusLineManger.setErrorMessage(Activator.getImage(IImageKeys.ERROR_ANNOTATION_ICON), ADWinMessages.ProcessDataLoss_Label);
											}
										});
										continue;
									} 
		                    	}

		                    		
		                    	if(trsfrNum == 0) {
		                    		realTime = time;
		                    		workload = value;
		                    		timeAfter = System.currentTimeMillis()/1000d;
		                    		if(timeAfter - timeBefore > 1) {
		                    			String name;
	                    				int nb ;
		                    			for (int n = 0; n < transferedChannelsOrderedByTransferNumber.length; n++) {
		                    				name = transferedChannelsOrderedByTransferNumber[n].getProperty(ChannelProperties.NAME);
		                    				nb = nbSamples[n];
		                    				appendToEventDiary("Total number of transferred samples for " + name + " : " + nb);
										}
		                    			appendToEventDiary("At : " + formater.format(realTime) + "sec. - Workload : " + workload + "%");
			                    		timeBefore = timeAfter;
		                    		}
									
			    					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
										@Override
			    						public void run() {
//				    							StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
//				    							statusLineManger.setMessage(NLS.bind(DocometreMessages.Workload_Time, workload, formater.format(realTime)));
			    							ApplicationActionBarAdvisor.workloadTimeContributionItem.setText(NLS.bind(DocometreMessages.Workload_Time, workload, formater.format(realTime)));
			    						}
			    					});
		                    	} else {
		                    		try {
//				                    		timeBefore = System.currentTimeMillis()/1000d;
//											transferedChannelsOrderedByTransferNumber[trsfrNum - 1].addSamples(new float[] {time, value});
										transferedChannelsOrderedByTransferNumber[trsfrNum - 1].addSamples(new float[] {value});
										if(TrialTimings.trace && !firstDataTraced) {
											firstDataTraced = true;
											TrialTimings.dataReceived(transferedChannelsOrderedByTransferNumber[trsfrNum - 1].getProperty(ChannelProperties.NAME), false);
										}
//			                    			channelName = transferedChannelsOrderedByTransferNumber[trsfrNum - 1].getProperty(ChannelProperties.NAME);
										nbSamples[trsfrNum - 1] +=1;
										transferedChannelsOrderedByTransferNumber[trsfrNum - 1].notifyChannelObservers();
//				                    		timeAfter = System.currentTimeMillis()/1000d;
//				        					appendToEventDiary("Display time : " + (timeAfter - timeBefore));
//				                    		appendToEventDiary("New sample for channel " + channelName + "  - dt : " + formater.format(time) + "  - Total " + nbSamples[trsfrNum - 1] + " samples");
									} catch (Exception e) {
										Activator.logErrorMessageWithCause(e);
										e.printStackTrace();
										appendToEventDiary("ERROR : " + e.getMessage() + " - Parse message : " + messageString);
										appendErrorMarkerAtCurrentDiaryLine("ERROR");
										PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
											@Override
											public void run() {
												StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
												statusLineManger.setErrorMessage(Activator.getImage(IImageKeys.ERROR_ANNOTATION_ICON), ADWinMessages.ProcessDataLoss_Label);
											}
										});
//										continue;
									}
		                    	}
			                    
		                    } else {
		                    	// 's' at the beginning of a line (a propagated char parameter can be 's'). The message
		                    	// may hold the '\n' of the previous line : the '\r' of "\r\n" ends the line, then '\n' is appended.
		                    	if(b == 's' && isBlank(message)) {
		                    		if(TrialTimings.trace && !processEndTraced) {
		                    			processEndTraced = true;
		                    			TrialTimings.processEnded();
		                    			TrialTimings.log("end of process signaled by Arduino ('s' received)");
		                    		}
		                    		appendToEventDiary("\nReceived stop 's' char");
		                    		stopReceived = true;
		                    		forceTermination = true;// To send 's' in reply (end handshake) as Arduino is waiting for it.
		                    	} else message.append((char)b);
		                    }
		            	}
		            	// The end handshake is measured separately
		            	if(TrialTimings.trace && !stopReceived) {
		            		iterations++;
		            		lastIterationNanos = System.nanoTime() - iterationStart;
		            		if(lastIterationNanos > maxIterationNanos) maxIterationNanos = lastIterationNanos;
		            	}
	    			}
	    			if(forceTermination) {
	    				if(TrialTimings.trace && !processEndTraced) {
	    					processEndTraced = true;
	    					TrialTimings.processEnded();
	    					TrialTimings.log("forced termination (stop requested, 's' not received from Arduino)");
	    				}
	    				// End handshake. At the end of its process, the Arduino sends 's' then waits
	    				// for 's' to restart (by software, see restartProcess() in the generated code ;
	    				// board reset by the watchdog for a process compiled before). When it has
	    				// restarted, it sends 'w' and waits for 'r' (next start). Before, fixed waits
	    				// were used (300 ms after each 's' sent) : now the PC waits for these chars.
	    				if(!stopReceived) {
	    					// Stop requested by the user : first 's' to terminate the process,
	    					// then wait for the 's' of the Arduino (process terminated)
	    					appendToEventDiary("\nStop 's' char not received !");
                    		appendToEventDiary("\nSend 's' char");
	    					serialPort.writeByte((byte) 's');
	    					long replyStartNanos = System.nanoTime();
	    					// Propagated parameters are sent by the Arduino before its 's' char
	    					long stopReplyTimeOut = propagationExpected ? STOP_REPLY_WITH_PROPAGATION_TIMEOUT_MS : STOP_REPLY_TIMEOUT_MS;
	    					boolean replyReceived = waitForStopReply(stopReplyTimeOut);
	    					double replyDelayMs = (System.nanoTime() - replyStartNanos)/1e6;
	    					if(replyReceived) appendToEventDiary("\nReceived stop 's' char after " + String.format("%.1f", replyDelayMs) + " ms");
	    					else {
	    						appendToEventDiary("\nStop 's' char not received after " + stopReplyTimeOut + " ms");
	    						appendErrorMarkerAtCurrentDiaryLine("Stop 's' char not received after " + stopReplyTimeOut + " ms");
	    					}
	    					TrialTimings.log("Arduino stop reply ('s') " + (replyReceived ? "received after " + String.format("%.1f", replyDelayMs) + " ms" : "not received (time out " + stopReplyTimeOut + " ms)"));
	    				}
	    				// 's' to restart the Arduino, then wait for its 'w' : restarted and ready for next start
	    				// ('w' already received : the Arduino had already sent its 's' and has been restarted
	    				// by the 's' of the stop request, it now waits for 'r' : no other 's')
	    				long resetStartNanos = System.nanoTime();
	    				boolean ready = readyReceived;
	    				if(!readyReceived) {
	    					appendToEventDiary("\nSend 's' char to start reset");
	    					serialPort.writeByte((byte) 's');
	    					ready = waitForChar(READY_MESSAGE.charAt(0), READY_TIMEOUT_MS);
	    				} else appendToEventDiary("\nArduino already restarted ('w' char received while waiting for its 's' char)");
	    				double readyDelayMs = (System.nanoTime() - resetStartNanos)/1e6;
	    				if(ready) appendToEventDiary("\nArduino restarted ('w' char received) after " + String.format("%.1f", readyDelayMs) + " ms");
	    				else {
	    					appendToEventDiary("\nArduino restart ('w' char) not received after " + READY_TIMEOUT_MS + " ms : compile and upload the process again to get it");
	    					appendErrorMarkerAtCurrentDiaryLine("Arduino restart ('w' char) not received : compile and upload the process again");
	    				}
	    				TrialTimings.log("Arduino restart ('w') " + (ready ? "received after " + String.format("%.1f", readyDelayMs) + " ms" : "not received (time out " + READY_TIMEOUT_MS + " ms, process compiled before ? compile it again)"));
	    				TrialTimings.step("stop handshake with Arduino ('s' chars and waits)");
	    				if(propagationExpected && propagatedValues == null) {
	    					// The parameters file will not be modified (see getPropagatedParametersValues())
	    					appendToEventDiary("\nERROR : " + ArduinoUnoMessages.PropagatedParametersNotReceived);
	    					appendErrorMarkerAtCurrentDiaryLine(ArduinoUnoMessages.PropagatedParametersNotReceived);
	    				}
	    				forceTermination = false;
	    				terminate = true;
	    			}
				}
				
			} catch (Exception e) {
				Activator.logErrorMessageWithCause(e);							
				appendToEventDiary("ERROR receiving string from port : " + e.getMessage());
				appendErrorMarkerAtCurrentDiaryLine("Error receiving string from port: " + e.getMessage());
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
						statusLineManger.setErrorMessage(Activator.getImage(IImageKeys.ERROR_ANNOTATION_ICON), "Error receiving string from port");
					}
				});
                return new Status(Status.ERROR, Activator.PLUGIN_ID, "Error receiving string from port: " + e.getMessage());
			} finally {
				if(TrialTimings.trace && !processEndTraced) {
					// Loop ended by an error
					processEndTraced = true;
					TrialTimings.processEnded();
				}
				TrialTimings.loopSummary("real-time loop (iterations which read bytes)", "", iterations, lastIterationNanos, maxIterationNanos);
				ArduinoUnoProcess.this.close();
				TrialTimings.step("ArduinoUnoProcess.close() (channels files)");
				try {
					serialPort.closePort();
					TrialTimings.step("serial port closed");
				} catch (SerialPortException e) {
					Activator.logErrorMessageWithCause(e);
					return new Status(Status.ERROR, Activator.PLUGIN_ID, "Error closing port: " + e.getMessage());
				}
				double processEndTime = System.currentTimeMillis()/1000d;
				Process.processEndTime = processEndTime;
				appendToEventDiary(ObjectsController.getResourceForObject(ArduinoUnoProcess.this).getName() + " duration (s) is about : " + Double.toString(processEndTime - processBeginTime));
				appendToEventDiary("End time : " + Double.toString(processEndTime) + " sec.\n");
				
				appendToEventDiary("Total samples for transferred channels :");
				for (int i = 0; i < nbSamples.length; i++) {
					appendToEventDiary(transferedChannelsOrderedByTransferNumber[i].getProperty(ChannelProperties.NAME) + " : " + nbSamples[i]);
				}
				IResource processResource = ObjectsController.getResourceForObject(ArduinoUnoProcess.this);
				appendToEventDiary(processResource.getFullPath() + " end.");
				
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
//						StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
//						statusLineManger.setMessage(NLS.bind(DocometreMessages.Workload_Time, "0", formater.format(time)));
						ApplicationActionBarAdvisor.workloadTimeContributionItem.setText(NLS.bind(DocometreMessages.Workload_Time, "0", formater.format(realTime)));
					}
				});
			}
			return Status.OK_STATUS;
			
		}
		
		public void forceTermination() {
			forceTermination = true;
		}
		
		/*
		 * Sends the values of the parameters to the Arduino (P frames then E frame, see ArduinoUnoParameterFrames)
		 * and waits for its answer : A frame (acknowledged) or N frame (error). Called before the start of the
		 * process, while the Arduino waits for 'r' char. On failure, the serial port is closed and an exception is thrown.
		 */
		public void sendParameters(Map<ParameterVariable, String> parameters) throws Exception {
			StringBuilder frames = new StringBuilder();
			int number = 0;
			for (Map.Entry<ParameterVariable, String> entry : parameters.entrySet()) {
				// Parameter number : rank in getParameters() (same order as the map)
				number++;
				String type = ((ArduinoUnoVariable)entry.getKey()).getProperty(ArduinoUnoVariableProperties.TYPE);
				frames.append(ArduinoUnoParameterFrames.parameterFrame(number, ArduinoUnoParameterFrames.toFrameValue(type, entry.getValue())));
			}
			frames.append(ArduinoUnoParameterFrames.endFrame(number));
			byte[] bytes = frames.toString().getBytes(StandardCharsets.US_ASCII);
			String answer = "none";
			boolean countError = false;
			try {
				for (int attempt = 1; attempt <= PARAMETERS_SENDING_ATTEMPTS && !countError; attempt++) {
					serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
					long startNanos = System.nanoTime();
					serialPort.writeBytes(bytes);
					ArduinoUnoParameterFrames.Frame frame = waitForParametersAnswer(PARAMETERS_ANSWER_TIMEOUT_MS);
					double delayMs = (System.nanoTime() - startNanos)/1e6;
					if(frame != null && frame.getKind() == ArduinoUnoParameterFrames.Kind.ACKNOWLEDGEMENT && frame.getNumber() == number) {
						appendToEventDiary("Parameters sent (" + number + ") and acknowledged by Arduino after " + String.format("%.1f", delayMs) + " ms (" + bytes.length + " bytes)");
						TrialTimings.log("Arduino parameters (" + number + ", " + bytes.length + " bytes) acknowledged after " + String.format("%.1f", delayMs) + " ms");
						return;
					}
					if(frame == null) answer = "none";
					else answer = (frame.getKind() == ArduinoUnoParameterFrames.Kind.ERROR ? "N" : "A") + frame.getNumber();
					countError = frame != null && frame.getKind() == ArduinoUnoParameterFrames.Kind.ERROR && frame.getNumber() == ArduinoUnoParameterFrames.COUNT_ERROR;
					appendToEventDiary("Parameters not acknowledged by Arduino (attempt " + attempt + ", answer : " + answer + ")");
					appendErrorMarkerAtCurrentDiaryLine("ERROR");
				}
			} finally {
				TrialTimings.step("ArduinoUnoProcess sending of parameters");
			}
			// Process not started : serial port is released
			serialPort.closePort();
			if(countError) throw new Exception(ArduinoUnoMessages.ParametersCountError);
			throw new Exception(NLS.bind(ArduinoUnoMessages.ParametersNotAcknowledged, answer));
		}
		
		/*
		 * Parameters frame received from the Arduino :
		 * - N0 at the start : the process expects parameters that were not sent (process to compile again) ;
		 * - P and E frames at the end of the process : values of the propagated parameters, the E frame is
		 *   answered with an A frame (acknowledged : values kept for the parameters file) or an N frame.
		 */
		private void handleParametersFrame(ArduinoUnoParameterFrames.Frame frame) throws SerialPortException {
			if(frame.getKind() == ArduinoUnoParameterFrames.Kind.ERROR) {
				appendToEventDiary("ERROR : " + ArduinoUnoMessages.ParametersCountError);
				appendErrorMarkerAtCurrentDiaryLine(ArduinoUnoMessages.ParametersCountError);
				return;
			}
			if(!propagationExpected) return;
			// New sending of the propagated parameters (first one or after an N answer)
			if(propagationReception == null || (propagationReception.isEnded() && frame.getKind() == ArduinoUnoParameterFrames.Kind.PARAMETER)) {
				propagationReception = createPropagationReception();
				propagationStartNanos = System.nanoTime();
			}
			String answer = propagationReception.accept(frame);
			if(answer == null) return;
			serialPort.writeBytes(answer.getBytes(StandardCharsets.US_ASCII));
			double delayMs = (System.nanoTime() - propagationStartNanos)/1e6;
			if(propagationReception.isAcknowledged()) {
				ParameterVariable[] parameters = getDACQConfiguration().getParameters();
				Map<ParameterVariable, String> values = new LinkedHashMap<>();
				String valuesString = "";
				for (int i = 0; i < parameters.length; i++) {
					String value = propagationReception.getValue(i + 1);
					if(value == null) continue;
					values.put(parameters[i], value);
					valuesString = valuesString + (valuesString.isEmpty() ? "" : " - ") + parameters[i].getParameterName() + " = " + value;
				}
				propagatedValues = values;
				appendToEventDiary("Propagated parameters received from Arduino and acknowledged : " + valuesString);
				TrialTimings.log("Arduino propagated parameters (" + values.size() + ") received and acknowledged in " + String.format("%.1f", delayMs) + " ms");
			} else {
				appendToEventDiary("Propagated parameters not acknowledged (answer : " + answer.trim() + ")");
				appendErrorMarkerAtCurrentDiaryLine("ERROR");
			}
		}
		
		/*
		 * Reception of the values of the propagated parameters : types of all the parameters (numbers are
		 * the ranks in getParameters()), only the propagated ones are expected
		 */
		private ArduinoUnoParameterFrames.Reception createPropagationReception() {
			ParameterVariable[] parameters = getDACQConfiguration().getParameters();
			String[] types = new String[parameters.length];
			int[] sizes = new int[parameters.length];
			for (int i = 0; i < parameters.length; i++) {
				types[i] = ((ArduinoUnoVariable)parameters[i]).getProperty(ArduinoUnoVariableProperties.TYPE);
				sizes[i] = ((ArduinoUnoVariable)parameters[i]).getSize();
			}
			boolean int32 = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION));
			return new ArduinoUnoParameterFrames.Reception(types, sizes, int32, getPropagatedFlags());
		}
		
		/*
		 * True if the message holds only end of line chars
		 */
		private boolean isBlank(StringBuilder message) {
			for (int i = 0; i < message.length(); i++) {
				char c = message.charAt(i);
				if(c != '\r' && c != '\n') return false;
			}
			return true;
		}
		
		/*
		 * Reads the serial port, byte per byte (the next bytes are kept for the next call), until the 's' char
		 * (at the beginning of a line) is received or the time out is reached. Parameters frames received before
		 * (propagated parameters) are handled, data frames are ignored. Returns true if 's' has been received.
		 * If the 'w' char is received, the Arduino had already sent its 's' and has been restarted by the 's'
		 * of the PC : readyReceived is set and true is returned (the restart must not be requested again).
		 */
		private boolean waitForStopReply(long timeOutMs) throws SerialPortException, InterruptedException {
			StringBuilder line = new StringBuilder();
			long deadline = System.nanoTime() + timeOutMs*1_000_000L;
			while(System.nanoTime() < deadline) {
				if(serialPort.getInputBufferBytesCount() > 0) {
					byte[] bytes = serialPort.readBytes(1);
					if(bytes == null || bytes.length == 0) continue;
					byte b = bytes[0];
					if(b == '\r' || b == '\n') {
						if(READY_MESSAGE.equals(line.toString())) {
							readyReceived = true;
							return true;
						}
						ArduinoUnoParameterFrames.Frame frame = ArduinoUnoParameterFrames.parse(line.toString());
						if(frame != null) handleParametersFrame(frame);
						line.setLength(0);
					} else if(b == 's' && line.length() == 0) return true;
					else line.append((char)b);
				} else Thread.sleep(1);
			}
			return false;
		}
		
		/*
		 * Reads the serial port, line per line, until an A or N frame is received or the time out is reached.
		 * Returns the frame, null if not received.
		 */
		private ArduinoUnoParameterFrames.Frame waitForParametersAnswer(long timeOutMs) throws SerialPortException, InterruptedException {
			StringBuilder line = new StringBuilder();
			long deadline = System.nanoTime() + timeOutMs*1_000_000L;
			while(System.nanoTime() < deadline) {
				if(serialPort.getInputBufferBytesCount() > 0) {
					byte[] bytes = serialPort.readBytes();
					if(bytes == null) continue;
					for (byte b : bytes) {
						if(b == '\r' || b == '\n') {
							ArduinoUnoParameterFrames.Frame frame = ArduinoUnoParameterFrames.parse(line.toString());
							line.setLength(0);
							if(frame != null && (frame.getKind() == ArduinoUnoParameterFrames.Kind.ACKNOWLEDGEMENT || frame.getKind() == ArduinoUnoParameterFrames.Kind.ERROR)) return frame;
						} else line.append((char)b);
					}
				} else Thread.sleep(1);
			}
			return null;
		}
		
		/*
		 * Reads the serial port, byte per byte (the next bytes are kept for the next
		 * call), until the expected char is received or the time out is reached.
		 * Returns true if the char has been received.
		 */
		private boolean waitForChar(char expectedChar, long timeOutMs) throws SerialPortException, InterruptedException {
			long deadline = System.nanoTime() + timeOutMs*1_000_000L;
			while(System.nanoTime() < deadline) {
				if(serialPort.getInputBufferBytesCount() > 0) {
					byte[] bytes = serialPort.readBytes(1);
					if(bytes != null && bytes.length > 0 && bytes[0] == expectedChar) return true;
				} else Thread.sleep(1);
			}
			return false;
		}
		
	}

	private transient String devicePath;
	private transient RealTimeLoopJob realTimeLoopJob;
	// Values of the propagated parameters received and acknowledged at the end of the process, null if not received
	private transient Map<ParameterVariable, String> propagatedValues;

	

	@Override
	public String getCodeSegment(Object segment) throws Exception {
		String code = "";
		
		String arduinoUnoRelease = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
		boolean isRelease3 = ArduinoUnoDACQConfigurationProperties.REVISION_R3.equals(arduinoUnoRelease);
		
		int delay = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.ARDUINO_DELAY_TIME_AFTER_SERIAL_PRINT);
		
		if(segment == ArduinoUnoCodeSegmentProperties.INCLUDE) {
			if(isRelease4Wifi) {
				code = code + "#include \"WDT.h\"\n";
				code = code + "#include \"FspTimer.h\"\n";
			}
			if(isRelease3) code = code + "#include <avr/wdt.h>\n";
			if(getDACQConfiguration().getParameters().length > 0) {
				code = code + "// Parameters reading (see readParameterValue())\n";
				code = code + "#include <errno.h>\n";
				code = code + "#include <limits.h>\n";
				code = code + "#include <string.h>\n";
			}
			code = code + getGlobalLibrariesCode();
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.INCLUDE);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.INCLUDE);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.INCLUDE) + "\n";
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.DEFINE) {
			code = code + "#define pi PI\n";
			code = code + getGlobalConstantsCode();
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.DEFINE);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.DEFINE);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.DEFINE) + "\n";
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.DECLARATION) {
			if(isRelease4Wifi) {
				code = code + "FspTimer realtimeLoop_Timer;\n";
			}
			String gf = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
			code = code + "// Loop frequency in Hz\n";
			code = code + "const unsigned long loopFrequency = " + gf + ";\n\n";
			code = code + "// Loop period in microsecond\n";
			code = code + "const float loopPeriod = (1000000.0/(1.0*loopFrequency));\n\n";
			code = code + "// Percents of loop period in microsecond to compute workload;\n";
			code = code + "const unsigned long loopPeriod_05 = (int)(05*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_10 = (int)(10*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_15 = (int)(15*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_20 = (int)(20*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_25 = (int)(25*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_30 = (int)(30*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_35 = (int)(35*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_40 = (int)(40*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_45 = (int)(45*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_50 = (int)(50*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_55 = (int)(55*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_60 = (int)(60*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_65 = (int)(65*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_70 = (int)(70*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_75 = (int)(75*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_80 = (int)(80*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_85 = (int)(85*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_90 = (int)(90*1000000.0/(100.0*loopFrequency));\n";
			code = code + "const unsigned long loopPeriod_95 = (int)(95*1000000.0/(100.0*loopFrequency));\n\n";
			code = code + "// Loop workload\n";
			code = code + "byte workload;\n\n";
//			code = code + "// Loop start time\n";
//			code = code + "unsigned long startTime;\n\n";
//			code = code + "// Variables to run process periodically\n";
//			code = code + "long previousLoopTime;\n";
//			code = code + "unsigned long currentLoopTime;\n\n";
//			code = code + "// Try to compensate time drift\n";
//			code = code + "long delta;\n";
			code = code + "// Loop time in second\n";
			if(isRelease4Wifi) code = code + "double rtTime;\n\n";
			if(isRelease3) code = code + "double time;\n\n";
			if(!isRelease3 && !isRelease4Wifi) {
				IResource dacqResource = ObjectsController.getResourceForObject(getDACQConfiguration());
				Activator.logErrorMessage(ArduinoUnoMessages.arduinoRevisionNotSpecified);
				Activator.logErrorMessage(NLS.bind(ArduinoUnoMessages.fixArduinoRevisionProperty, dacqResource.getFullPath().toPortableString()));
			}
//			code = code + "// Loop time in usecond\n";
//			code = code + "unsigned long timeMicros;\n\n";
			code = code + "// Loop index to compute time\n";
			code = code + "unsigned long timeIndex;\n\n";
			code = code + "// Start loop when true\n";
			code = code + "bool startLoop;\n\n";
			code = code + "// Restart requested when true : end of process and 's' char received.\n";
			code = code + "// The process is then restarted by software (see loop() and restartProcess()),\n";
			code = code + "// without board reset (a board reset takes about 0.5 s).\n";
			code = code + "volatile bool restartRequested;\n\n";
			code = code + "// True until the end of the first start of the process (one-time initializations)\n";
			code = code + "bool firstStart = true;\n\n";
			code = code + "void startProcess();\n";
			code = code + "void restartProcess();\n";
			code = code + "void finalize();\n\n";
			code = code + "// Stop loop when true\n";
			code = code + "bool terminateProcess;\n\n";
//			code = code + "// First loop flag\n";
//			code = code + "bool firstLoop = true;\n\n";
			code = code + "// String serialMessage\n";
			code = code + "char serialMessage[64];\n";
			if(isRelease4Wifi) {
				// Messages added in the real time loop interrupt : no dynamic memory (String) in the interrupt, the
				// memory allocator is not reentrant (interrupt adding messages while loop() releases them)
				code = code + "// Messages for the PC : added by the real time loop interrupt (globalSerialMessage += ...),\n";
				code = code + "// sent by loop(). Two buffers without dynamic memory, exchanged by loop() while interrupts are masked\n";
				code = code + "#define SERIAL_MESSAGES_SIZE 2048\n";
				code = code + "class SerialMessages {\n";
				code = code + "\tpublic:\n";
				code = code + "\t\tSerialMessages& operator+=(const char* text) {\n";
				code = code + "\t\t\t\tsize_t length = strlen(text);\n";
				code = code + "\t\t\t\tif(sizes[current] + length <= SERIAL_MESSAGES_SIZE) {\n";
				code = code + "\t\t\t\t\t\tmemcpy(buffers[current] + sizes[current], text, length);\n";
				code = code + "\t\t\t\t\t\tsizes[current] += length;\n";
				code = code + "\t\t\t\t} else lost = true;\n";
				code = code + "\t\t\t\treturn *this;\n";
				code = code + "\t\t}\n";
				code = code + "\t\t// Messages removed (start of the process)\n";
				code = code + "\t\tSerialMessages& operator=(const char* text) {\n";
				code = code + "\t\t\t\tnoInterrupts();\n";
				code = code + "\t\t\t\tsizes[0] = 0;\n";
				code = code + "\t\t\t\tsizes[1] = 0;\n";
				code = code + "\t\t\t\tlost = false;\n";
				code = code + "\t\t\t\tinterrupts();\n";
				code = code + "\t\t\t\treturn *this += text;\n";
				code = code + "\t\t}\n";
				code = code + "\t\t// Sends the messages added since the last call : called by loop(), never in the interrupt\n";
				code = code + "\t\tvoid send() {\n";
				code = code + "\t\t\t\tnoInterrupts();\n";
				code = code + "\t\t\t\tuint8_t full = current;\n";
				code = code + "\t\t\t\tsize_t size = sizes[full];\n";
				code = code + "\t\t\t\tbool messagesLost = lost;\n";
				code = code + "\t\t\t\tif(size > 0 || messagesLost) {\n";
				code = code + "\t\t\t\t\t\tcurrent = 1 - current;\n";
				code = code + "\t\t\t\t\t\tsizes[current] = 0;\n";
				code = code + "\t\t\t\t\t\tlost = false;\n";
				code = code + "\t\t\t\t}\n";
				code = code + "\t\t\t\tinterrupts();\n";
				code = code + "\t\t\t\tif(size > 0) Serial.write((uint8_t*)buffers[full], size);\n";
				code = code + "\t\t\t\t// Written in the diary of the trial by DOCoMETRe (not a data message)\n";
				code = code + "\t\t\t\tif(messagesLost) Serial.println(\"ERROR : messages lost, messages buffer full\");\n";
				code = code + "\t\t\t\tif(size > 0 || messagesLost) Serial.flush();\n";
				code = code + "\t\t}\n";
				code = code + "\tprivate:\n";
				code = code + "\t\tchar buffers[2][SERIAL_MESSAGES_SIZE];\n";
				code = code + "\t\tvolatile size_t sizes[2] = {0, 0};\n";
				code = code + "\t\tvolatile uint8_t current = 0;\n";
				code = code + "\t\tvolatile bool lost = false;\n";
				code = code + "};\n";
				code = code + "SerialMessages globalSerialMessage;\n\n";
			}
			code = code + "\n";
			code = code + "// Index to send time and workload at regular intervall (#200ms)\n";
			code = code + "unsigned long sendTimeWorkload;\n\n";
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.DECLARATION);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.DECLARATION);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.DECLARATION);
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.INITIALIZATION) {
			code = "void setup() {\n";
			code = code + "\t\t// One-time initializations (done again only after a board reset)\n";
			
			if(isRelease3) {
				code = code + "\t\t// ADC CONFIGURATON\n";
				code = code + "\t\t// From datasheet at :\n";
				code = code + "\t\t// https://ww1.microchip.com/downloads/en/DeviceDoc/ATmega48A-PA-88A-PA-168A-PA-328-P-DS-DS40002061B.pdf\n";
				code = code + "\t\t// One can read at 24.4 p249 :\n";
				code = code + "\t\t// \"By default, the successive approximation circuitry requires an input clock frequency\n";
				code = code + "\t\t// between 50kHz and 200kHz to get maximum resolution. If a lower resolution than 10 bits is needed,\n";
				code = code + "\t\t// the input clock frequency to the ADC can be higher than 200kHz to get a higher sample rate.\"\n";
				code = code + "\t\t// Following this, prescaler could be set to 8 for ADC to decrease conversion time and keep resolution to maximum.\n";
				code = code + "\t\t// Because of corsstalk in analog multiplexer, sample frequency must be decreased by a scale factor.\n";
				code = code + "\t\t// We set prescaler to 16.\n";
				code = code + "\t\t// 16MHz/16 = 1MHz - Max Sample freq. in theory is = 1MHz/13 # 76.923kHz, convertion time of 13us.\n";
				code = code + "\t\t// Measured #18.6us which gives a Max Sample freq. of #53kHz\n";
				code = code + "\t\t// Then ADC is enabled and ADSP[2:0] = 100\n";
				code = code + "\t\tADCSRA  =  bit (ADEN) | 1*bit (ADPS2) | 0*bit (ADPS1) | 0*bit (ADPS0);\n";
			}
			if(isRelease4Wifi) code = code + "\t\tanalogReadResolution(14);\n";
			if(((ArduinoUnoDACQConfiguration)getDACQConfiguration()).hasADS1115Module()) {
				code = code + "\t\t// Start I2C interface\n";
				code = code + "\t\tWire.begin();\n";
			}
			String baudRate = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.BAUD_RATE);
			code = code + "\t\tSerial.begin(" + baudRate + ");\n";
			code = code + "\t\tstartProcess();\n";
			code = code + "}\n\n";
			
			code = code + "// Start of the process : called by setup() and, at the end of each process,\n";
			code = code + "// by restartProcess(). Everything the process uses is initialized here, as after a board reset.\n";
			code = code + "void startProcess() {\n";
			double gf = Double.parseDouble(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY));
			int value = (int)(displayTimeRate*gf);
			if(value == 0) value = 1;
			code = code + "\t\tsendTimeWorkload = " + value + ";\n";
			code = code + "\t\tworkload = 0;\n";
			if(isRelease4Wifi) code = code + "\t\trtTime = 0;\n";
			if(isRelease3) code = code + "\t\ttime = 0;\n";
			code = code + "\t\ttimeIndex = 0;\n";
			code = code + "\t\tterminateProcess = false;\n";
			code = code + "\t\tstartLoop = false;\n";
			code = code + "\t\trestartRequested = false;\n";
			if(isRelease4Wifi) code = code + "\t\tglobalSerialMessage = \"\";\n";
			code = code + "\t\t// Tell the PC that the board has (re)started and waits for 'r' char (end handshake)\n";
			code = code + "\t\tSerial.println('" + READY_MESSAGE + "');\n";
			code = code + "\t\tSerial.flush();\n";
			if(hasPropagatedParameters()) code = code + "\t\tpropagationInProgress = false;\n";
			code = code + ArduinoUnoParametersCode.getWaitForStartCode(getDACQConfiguration().getParameters().length, "\t\t");
			
			// Variables initialization
			code = code + VariablesCodeGenerationDelegate.getCode(this, ArduinoUnoCodeSegmentProperties.INITIALIZATION);
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.INITIALIZATION);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.INITIALIZATION);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.INITIALIZATION);
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.LOOP) {
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.LOOP);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.LOOP);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.LOOP);
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.ACQUISITION) {
			
			if(isRelease3) code = "}\n\nISR(TIMER1_COMPA_vect){\n";
			if(isRelease4Wifi) {
				code = "}\n\nvoid processing(timer_callback_args_t *p_args){\n\n";
				code = code + "\t\tunsigned long dt = micros();\n\n";
			}
			
			
			code = code + "\t\tif(terminateProcess) {\n";
			code = code + "\t\t\t\t// End of process : 's' char from the PC requests the restart (done by loop())\n";
			if(hasPropagatedParameters()) {
				code = code + "\t\t\t\t// Serial port is not read while the propagated parameters are sent (answer of the PC read by loop())\n";
				code = code + "\t\t\t\tif(!propagationInProgress && Serial.available()) {\n";
			} else code = code + "\t\t\t\tif(Serial.available()) {\n";
			code = code + "\t\t\t\t\t\tif(((char)Serial.read()) == 's') restartRequested = true;\n";
			code = code + "\t\t\t\t}\n";
			code = code + "\t\t\t\treturn;\n";
			code = code + "\t\t}\n";
			
			if(isRelease3) {
				code = code + "\t\t// Reset timer 0 for workload computation\n";
				code = code + "\t\tTCNT0 = 0;\n";
			}
		
			code = code + "\t\t// If we receive 's'(top) from serial port,\n";
			code = code + "\t\t// then force process termination\n";
			code = code + "\t\tif(Serial.available()) {\n";
			code = code + "\t\t\t\tstartLoop = ((char)Serial.read()) != 's';\n";
			code = code + "\t\t\t\tif(!startLoop ) terminateProcess = true;\n";
			code = code + "\t\t}\n";
			if(isRelease4Wifi) code = code + "\t\trtTime = timeIndex*((double)loopPeriod/1000000.0);\n";
			if(isRelease3) code = code + "\t\ttime = timeIndex*((double)loopPeriod/1000000.0);\n";
			code = code + "\t\ttimeIndex++;\n";
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.ACQUISITION);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.ACQUISITION);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.ACQUISITION);
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.RECOVERY) {
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.RECOVERY);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.RECOVERY);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.RECOVERY);
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.TRANSFER) {
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.TRANSFER);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.TRANSFER);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.TRANSFER);
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.GENERATION) {
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.GENERATION);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.GENERATION);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.GENERATION);
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.FINALIZATION) {
			
			double gf = Double.parseDouble(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY));
			int value = (int)(displayTimeRate*gf);
			if(value == 0) value = 1;
			
			code = code + "\t\tif(sendTimeWorkload == " + value + ") {\n";
			int prescaler = computeCounter0Prescaler(gf);
			double dt = 1/(16000000.0/prescaler)*1000000;
			if(isRelease3) code = code + "\t\t\t\tworkload = computeWorkload(TCNT0*" + dt + ");\n";
			if(isRelease4Wifi) {
				code = code + "\t\t\t\tdt = micros() - dt;\n";
				code = code + "\t\t\t\tworkload = computeWorkload((unsigned long)dt);\n";
			}
//			code = code + "\t\t\t\t\t\tsprintf(serialMessage, \"%d:%lu:%d\", 0, loopTime_MS, workload);\n";
//			code = code + "\t\t\t\t\t\tsprintf(serialMessage, \"%d:%lu:%d\", 0, 10000.0 /*(unsigned long)(time*1000000)*/, workload);\n";
			
			if(isRelease4Wifi) {
				code = code + "\t\t\t\tsprintf(serialMessage, \"%d:%lu:%d\", 0, (unsigned long)(rtTime*1000000.0), workload);\n";
				code = code + "\t\t\t\tglobalSerialMessage += serialMessage;\n";
				code = code + "\t\t\t\tglobalSerialMessage += \"\\n\";\n";
				
			}
			if(isRelease3) {
				code = code + "\t\t\t\tsprintf(serialMessage, \"%d:%lu:%d\", 0, (unsigned long)(time*1000000.0), workload);\n";
				code = code + "\t\t\t\tSerial.println(serialMessage);\n";
				code = code + "\t\t\t\tSerial.flush();\n";
			}
			if(delay > 0) code = code + "\t\t\t\tdelayMicroseconds(" + delay + ");\n";
			code = code + "\t\t\t\tsendTimeWorkload = 0;\n";
			
			code = code + "\t\t}\n";
			code = code + "\t\tsendTimeWorkload++;\n\n";
			
			code = code + "\t\tif(terminateProcess) {\n";
			code = code + "\t\t\t\tfinalize();\n";
//			code = code + "\t\t\t\texit(0);\n";
			code = code + "\t\t}\n";
			
//			code = code + "\t\t}\n";
			code = code + "}\n\n";
			
			code = code + "void finalize() {\n";
			
//			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.FINALIZATION);
//			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.FINALIZATION);
//			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.FINALIZATION);
			

			code = code + "\t\t// ******** Début algorithme finalisation\n\n";
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ScriptSegmentType.FINALIZE);
			code = code + "\n\t\t// ******** Fin algorithme finalisation\n\n";
			if(hasPropagatedParameters()) {
				code = code + "\t\t// Propagated parameters, then 's' char, are sent by loop()\n";
				code = code + "\t\tpropagationInProgress = true;\n";
			}
			if(isRelease4Wifi) code = code + "\t\tstartLoop = false;\n";
			if(isRelease3 && !hasPropagatedParameters()) {
				code = code + "\t\tSerial.println('s');\n";
				code = code + "\t\tSerial.flush();\n";
				if(delay > 0) code = code + "\t\t\t\t\t\tdelayMicroseconds(" + delay + ");\n";
			}
//			code = code + "\t\t// Now wait to receive 's' char before board restart\n";
//			code = code + "\t\twhile (startLoop) {\n";
//			code = code + "\t\t\t\tif(Serial.available()) {\n";
//			code = code + "\t\t\t\t\t\tstartLoop = ((char)Serial.read()) != 's';\n";
//			code = code + "\t\t\t\t}\n";
//			code = code + "\t\t}\n\n";
			code = code + "}\n\n";

			code = code + "// Restart of the process by software, at the end of the process ('s' char received) :\n";
			code = code + "// real time loop stopped, outputs as after a board reset, then startProcess()\n";
			code = code + "void restartProcess() {\n";
			if(isRelease3) {
				code = code + "\t\t// Stop timer 1 (real time loop) and clear a pending compare interrupt\n";
				code = code + "\t\tTIMSK1 &= ~(1 << OCIE1A);\n";
				code = code + "\t\tTCCR1B = 0;\n";
				code = code + "\t\tTIFR1 = (1 << OCF1A);\n";
			}
			if(isRelease4Wifi) {
				code = code + "\t\t// Stop real time loop timer\n";
				code = code + "\t\trealtimeLoop_Timer.stop();\n";
			}
			code = code + getOutputsResetCode();
			code = code + "\t\tstartProcess();\n";
			code = code + "}\n\n";
			
			code = code + "void loop() {\n";
			if(isRelease4Wifi) {
				code = code + "\t\tglobalSerialMessage.send();\n";
				if(hasPropagatedParameters()) {
					code = code + "\t\t// End of process : values of the propagated parameters sent before 's' char\n";
					code = code + "\t\tif(propagationInProgress) {\n";
					code = code + "\t\t\t\tsendPropagatedParameters();\n";
					code = code + "\t\t\t\tpropagationInProgress = false;\n";
					code = code + "\t\t}\n";
				}
				code = code + "\t\tif(terminateProcess) {\n";
				
				if(hasPropagatedParameters()) code = code + "\t\t\t\tif(!startLoop && !propagationInProgress) {\n";
				else code = code + "\t\t\t\tif(!startLoop) {\n";
				code = code + "\t\t\t\t\t\tSerial.println('s');\n";
				code = code + "\t\t\t\t\t\tSerial.flush();\n";
				if(delay > 0) code = code + "\t\t\t\t\t\t\t\tdelayMicroseconds(" + delay + ");\n";
				code = code + "\t\t\t\t\t\tstartLoop = true;\n";
			    code = code + "\t\t\t\t}\n";
				code = code + "\t\t}\n";
			}
			if(isRelease3 && hasPropagatedParameters()) {
				code = code + "\t\t// End of process : values of the propagated parameters, then 's' char\n";
				code = code + "\t\tif(propagationInProgress) {\n";
				code = code + "\t\t\t\tsendPropagatedParameters();\n";
				code = code + "\t\t\t\tSerial.println('s');\n";
				code = code + "\t\t\t\tSerial.flush();\n";
				if(delay > 0) code = code + "\t\t\t\tdelayMicroseconds(" + delay + ");\n";
				code = code + "\t\t\t\tpropagationInProgress = false;\n";
				code = code + "\t\t}\n";
			}
			code = code + "\t\tif(restartRequested) restartProcess();\n";
			code = code + "}\n\n";
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.FUNCTION) {
//			code = code + "\t\t\t\t\t\tpreviousLoopTime = micros() - currentLoopTime;\n";
//			code = code + "\t\t\t\t\t\tworkload = computeWorkload(previousLoopTime);\n";
//			code = code + "\t\t\t\t\t\tsprintf(serialMessage, \"%d:%lu:%d\", 0, loopTime_MS, workload);\n";
//			code = code + "\t\t\t\t\t\tSerial.println(serialMessage);\n";
//			code = code + "\t\t\t\t\t\tpreviousLoopTime = currentLoopTime;\n";
//			code = code + "\t\t\t\t}\n";
//			code = code + "\t\t\t\tif(terminateProcess) {\n";
//			code = code + "\t\t\t\t\t\tSerial.println('s');\n";
//			code = code + "\t\t\t\t\t\twdt_enable(WDTO_15MS);\n";
//			code = code + "\t\t\t\t\t\twhile(true) {\n";
//			code = code + "\t\t\t\t\t\t\t\t// Wait for board to restart\n";
//			code = code + "\t\t\t\t\t\t}\n";
//			code = code + "\t\t\t\t}\n";
//			code = code + "\t\t}\n";
//			code = code + "}\n\n";
			code = code + "byte computeWorkload(unsigned long computeTime) {\n";
			code = code + "\t\tif(computeTime <= loopPeriod_05) return 05;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_10) return 10;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_15) return 15;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_20) return 20;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_25) return 25;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_30) return 30;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_35) return 35;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_40) return 40;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_45) return 45;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_50) return 50;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_55) return 55;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_60) return 60;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_65) return 65;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_70) return 70;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_75) return 75;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_80) return 80;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_85) return 85;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_90) return 90;\n";
			code = code + "\t\tif(computeTime <= loopPeriod_95) return 95;\n";
			code = code + "\t\tif(computeTime <= loopPeriod) return 100;\n";
			code = code + "\t\tif(computeTime > loopPeriod) return 200;\n";
			code = code + "}\n\n";
			code = code + "unsigned int acquireAnalogInput(byte inputNumber, bool transfert, byte transferNumber) {\n";
			//code = code + "\t\t//unsigned long dt = micros();\n";
			code = code + "\t\tint value = analogRead(inputNumber);\n";
			//code = code + "\t\t//dt = micros() - dt;\n";
			//code = code + "\t\t//Serial.println(dt);\n";
			//code = code + "\t\t//serialMessage = String(inputNumber) + \":\" + String(loopTime - *lastAcquisitionTime) + \":\" + String(value);\n";
			code = code + "\t\tif(transfert) {\n";
//			code = code + "\t\t\t\tsprintf(serialMessage, \"%d:%lu:%d\", transferNumber, (loopTime_MS - *lastAcquisitionTime), value);\n";
			code = code + "\t\t\t\tsprintf(serialMessage, \"%d:%d\", transferNumber, value);\n";
			if(isRelease3) {
				code = code + "\t\t\t\tSerial.println(serialMessage);\n";
				code = code + "\t\t\t\tSerial.flush();\n";
			}
			if(isRelease4Wifi) {
				code = code + "\t\t\t\tglobalSerialMessage += serialMessage;\n";
				code = code + "\t\t\t\tglobalSerialMessage += \"\\n\";\n";
			}
			if(delay > 0)code = code + "\t\t\t\tdelayMicroseconds(" + delay + ");\n";
			code = code + "\t\t}\n";
			code = code + "\t\treturn value;\n";
			code = code + "}\n";
			
			if(((ArduinoUnoDACQConfiguration)getDACQConfiguration()).hasADS1115Module()) {
				// Signed values (int16_t of the ADS1X15 library) : a voltage slightly lower than 0V gives a small negative value
				code = code + "int acquireADS1115AnalogInput(ADS1115 ads, byte inputNumber, byte gain, bool transfert, byte transferNumber) {\n";
				code = code + "\t\tint value = 0;\n";
				code = code + "\t\tads.setGain(gain);\n";
				// R3 : interrupts are disabled in the real time loop interrupt, I2C needs them. R4 : I2C interrupts can
				// interrupt the real time loop (see its priority) ; noInterrupts() would disable all interrupts after the
				// end of the real time loop interrupt (PRIMASK of the Cortex-M4 is not restored at the end of an interrupt)
				if(isRelease3) code = code + "\t\tinterrupts();\n";
				code = code + "\t\tvalue = ads.readADC(inputNumber);\n";
				if(isRelease3) code = code + "\t\tnoInterrupts();\n";
				code = code + "\t\tif(transfert) {\n";
				code = code + "\t\t\t\tsprintf(serialMessage, \"%d:%d\", transferNumber, value);\n";
				if(isRelease4Wifi) {
					// Sent by loop() : in the real time loop interrupt, Serial would wait forever (see acquireAnalogInput())
					code = code + "\t\t\t\tglobalSerialMessage += serialMessage;\n";
					code = code + "\t\t\t\tglobalSerialMessage += \"\\n\";\n";
				} else {
					code = code + "\t\t\t\tSerial.println(serialMessage);\n";
					code = code + "\t\t\t\tSerial.flush();\n";
				}
				code = code + "\t\t}\n";
				code = code + "\t\treturn value;\n";
				code = code + "}\n";
//				code = code + "unsigned int acquireADS1115AnalogInput(int moduleAddress, byte inputNumber, byte gain, byte frequency, bool transfert, byte transferNumber) {\n";
//				code = code + "\t\tint wordValue = 0;\n";
//				code = code + "\t\tinterrupts();\n";
//				code = code + "\t\tWire.beginTransmission(moduleAddress);\n";
//				code = code + "\t\tWire.write(0b00000001);\n";
//				code = code + "\t\tbyte message = 0b11 << 6 | inputNumber << 4 | gain << 1 | 1;\n";
//				code = code + "\t\tWire.write(message);\n";
//				code = code + "\t\tmessage = frequency << 5 | 3;\n";
//				code = code + "\t\tWire.write(message);\n";
//				code = code + "\t\tWire.endTransmission(true);\n";
//				code = code + "\t\tWire.beginTransmission(moduleAddress);\n";
//				code = code + "\t\tWire.write(0b00000000);\n";
//				code = code + "\t\tWire.endTransmission(true);\n";
//				code = code + "\t\tWire.requestFrom(moduleAddress, 2, true);\n";
//				code = code + "\t\tif(Wire.available() == 2) {\n";
//				code = code + "\t\t\t\twordValue = Wire.read();\n";
//				code = code + "\t\t\t\twordValue = wordValue << 8 | Wire.read();\n";
//				code = code + "\t\t}\n";
//				code = code + "\t\tWire.endTransmission();\n";
//				code = code + "\t\t noInterrupts();\n";
//				code = code + "\t\tif(transfert) {\n";
//				code = code + "\t\t\t\tsprintf(serialMessage, \"%d:%d\", transferNumber, wordValue);\n";
//				code = code + "\t\t\t\tSerial.println(serialMessage);\n";
//				code = code + "\t\t\t\tSerial.flush();\n";
//				if(delay > 0)code = code + "\t\t\t\tdelayMicroseconds(" + delay + ");\n";
//				code = code + "\t\t}\n";
//				code = code + "\t\treturn wordValue;\n";
//				code = code + "}\n";
			}
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.FUNCTION);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.FUNCTION);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.FUNCTION);
			code = code + getGlobalSubroutinesCode();
			
		}
		
		return code;
	}

	/*
	 * Code of restartProcess() putting the outputs used by the process as after a board
	 * reset : digital outputs in high impedance (input) - pinMode(OUTPUT) is done again by
	 * the modules initialization in startProcess(), the output is then LOW as after a reset -
	 * and analog outputs to 0. The pin mode of analog outputs is not changed : on R4, a DAC
	 * pin whose mode is changed after its first analogWrite is no more connected to the DAC.
	 */
	private String getOutputsResetCode() {
		String code = "";
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			if(!(module instanceof ArduinoUnoDigInOutModule) && !(module instanceof ArduinoUnoAnOutModule)) continue;
			for (Channel channel : ((Module)module).getChannels()) {
				if(!Boolean.valueOf(channel.getProperty(ArduinoUnoChannelProperties.USED))) continue;
				String channelNumber = channel.getProperty(ChannelProperties.CHANNEL_NUMBER);
				if(module instanceof ArduinoUnoAnOutModule) {
					code = code + "\t\tanalogWrite(" + channelNumber + ", 0);\n";
				} else {
					String inOut = channel.getProperty(ArduinoUnoDigInOutChannelProperties.IN_OUT);
					boolean isOutput = !ArduinoUnoDigInOutChannelProperties.INPUT.equals(inOut) && !ArduinoUnoDigInOutChannelProperties.INPUT_PULLUP.equals(inOut);
					if(isOutput) {
						code = code + "\t\tdigitalWrite(" + channelNumber + ", LOW);\n";
						code = code + "\t\tpinMode(" + channelNumber + ", INPUT);\n";
					}
				}
			}
		}
		if(!code.isEmpty()) code = "\t\t// Outputs as after a board reset\n" + code;
		return code;
	}
	
	/*
	 * Global libraries declared in INITIALIZE segment editor, at the beginning of the code after system libraries
	 */
	private String getGlobalLibrariesCode() {
		List<GlobalDeclarations.Library> libraries = getCurrentProcess().getScript().getGlobalDeclarations().getLibraries();
		if(libraries.isEmpty()) return "";
		String code = "// Global libraries\n";
		for (GlobalDeclarations.Library library : libraries) {
			if(!library.getComment().isEmpty()) code = code + "// " + library.getComment() + "\n";
			code = code + "#include " + library.getFile() + "\n";
		}
		return code;
	}
	
	/*
	 * Global constants declared in INITIALIZE segment editor
	 */
	private String getGlobalConstantsCode() {
		List<GlobalDeclarations.Constant> constants = getCurrentProcess().getScript().getGlobalDeclarations().getConstants();
		if(constants.isEmpty()) return "";
		String code = "\n// Global constants\n";
		for (GlobalDeclarations.Constant constant : constants) {
			if(!constant.getComment().isEmpty()) code = code + "// " + constant.getComment() + "\n";
			code = code + "#define " + constant.getName() + " " + constant.getValue() + "\n";
		}
		return code;
	}
	
	/*
	 * Global subroutines declared in INITIALIZE segment editor.
	 * Sketch is an .ino file : functions prototypes are generated by Arduino builder.
	 */
	private String getGlobalSubroutinesCode() {
		List<GlobalDeclarations.Subroutine> subroutines = getCurrentProcess().getScript().getGlobalDeclarations().getSubroutines();
		if(subroutines.isEmpty()) return "";
		String code = "\n// Global subroutines\n\n";
		for (GlobalDeclarations.Subroutine subroutine : subroutines) {
			code = code + "void " + subroutine.getName() + "(" + subroutine.getParameters() + ") {\n";
			String body = subroutine.getBody().stripTrailing();
			if(!body.isEmpty()) for (String line : body.split("\\R")) code = code + "\t" + line + "\n";
			code = code + "}\n\n";
		}
		return code;
	}
	
	@Override
	public void recovery() {
		ArduinoUnoVariable[] variables = ((ArduinoUnoDACQConfiguration)getDACQConfiguration()).getVariables();
		RecoveryDelegate.recover(variables, null, this);
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			module.recovery();
		}
	}

	@Override
	public void generation() {
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			module.generation();
		}
	}

	@Override
	public void open(Process process, String prefix, String suffix) {
		ArduinoUnoVariable[] variables = ((ArduinoUnoDACQConfiguration)getDACQConfiguration()).getVariables();
		for (ArduinoUnoVariable variable : variables) {
			variable.open(this, prefix, suffix);
		}
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			module.open(this, prefix, suffix);
		}
	}

	@Override
	public void close() {
		ArduinoUnoVariable[] variables = ((ArduinoUnoDACQConfiguration)getDACQConfiguration()).getVariables();
		for (ArduinoUnoVariable variable : variables) {
			variable.close(this);
		}
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			module.close();
		}
		// The workspace is refreshed by postExecute (see refreshChannelsFiles)
	}
	
	@Override
	protected void postExecuteInWorkspaceOperation(IResource resource) throws Exception {
		refreshChannelsFiles();
		super.postExecuteInWorkspaceOperation(resource);
	}
	
	@Override
	protected void checkParameterValue(ParameterVariable parameter, String value) throws Exception {
		if(!parameter.isValidParameterValue(value)) {
			String type = ((ArduinoUnoVariable)parameter).getProperty(ArduinoUnoVariableProperties.TYPE);
			throw new Exception(NLS.bind(ArduinoUnoMessages.InvalidParameterValue, new Object[] {value, parameter.getParameterName(), type}));
		}
	}
	
	/*
	 * Declarations and functions of the generated code to receive the values of the parameters,
	 * in the order of getParameters() (number of a parameter : its rank)
	 */
	private String getParametersDeclarationCode() {
		ParameterVariable[] parameters = getDACQConfiguration().getParameters();
		String[] names = new String[parameters.length];
		String[] types = new String[parameters.length];
		int[] sizes = new int[parameters.length];
		boolean[] propagated = getPropagatedFlags();
		for (int i = 0; i < parameters.length; i++) {
			ArduinoUnoVariable variable = (ArduinoUnoVariable)parameters[i];
			names[i] = variable.getParameterName();
			types[i] = variable.getProperty(ArduinoUnoVariableProperties.TYPE);
			sizes[i] = variable.getSize();
		}
		boolean avr = ArduinoUnoDACQConfigurationProperties.REVISION_R3.equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION));
		return ArduinoUnoParametersCode.getDeclarationCode(names, types, sizes) + ArduinoUnoParametersCode.getPropagationDeclarationCode(names, types, propagated, avr);
	}
	
	/*
	 * propagated[n - 1] is true if parameter number n (rank in getParameters()) is propagated :
	 * numeric parameters only, a string is never propagated
	 */
	private boolean[] getPropagatedFlags() {
		ParameterVariable[] parameters = getDACQConfiguration().getParameters();
		boolean[] propagated = new boolean[parameters.length];
		for (int i = 0; i < parameters.length; i++) propagated[i] = parameters[i].isParameterPropagated() && !parameters[i].isStringParameter();
		return propagated;
	}
	
	private boolean hasPropagatedParameters() {
		return ArduinoUnoParametersCode.hasPropagatedParameters(getPropagatedFlags());
	}
	
	@Override
	protected Map<ParameterVariable, String> getPropagatedParametersValues() throws Exception {
		// Values sent by the Arduino at the end of the process and acknowledged (see RealTimeLoopJob.handleParametersFrame()),
		// in the format of the parameters file : null if not received (the parameters file is then not modified)
		return propagatedValues;
	}
	
	/*
	 * Refreshes the output folder ONCE for all the channels closed by close(), and
	 * sets the type of the recorded samples files. Called by postExecute, within its
	 * single workspace operation (with diary modifications) : only one resource change
	 * notification is sent.
	 * (Previously, each recorded channel refreshed the whole output folder and
	 * each not recorded channel deleted its resource : N workspace operations.)
	 */
	private void refreshChannelsFiles() {
		IFolder outputFolder = getOutputFolder();
		if(outputFolder == null || !outputFolder.exists()) return;
		List<String> recordedFilesNames = new ArrayList<>();
		for (ArduinoUnoVariable variable : ((ArduinoUnoDACQConfiguration)getDACQConfiguration()).getVariables()) {
			if(variable.getRecordedFileName() != null) recordedFilesNames.add(variable.getRecordedFileName());
		}
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			if(!(module instanceof Module)) continue;
			for (Channel channel : ((Module)module).getChannels()) {
				if(channel instanceof ArduinoUnoChannel && ((ArduinoUnoChannel)channel).getRecordedFileName() != null)
					recordedFilesNames.add(((ArduinoUnoChannel)channel).getRecordedFileName());
			}
		}
		try {
			outputFolder.refreshLocal(IResource.DEPTH_ONE, null);
			for (String fileName : recordedFilesNames) {
				IResource fileResource = outputFolder.findMember(fileName);
				if(fileResource != null) ResourceProperties.setTypePersistentProperty(fileResource, ResourceType.SAMPLES.toString());
			}
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

	@Override
	public void reset() {
		for (int i = 0; i < getDACQConfiguration().getModulesNumber(); i++) {
			ModuleBehaviour module = getDACQConfiguration().getModule(i);
			module.reset();
		}
	}

	@Override
	public String getCode(ModuleBehaviour script) throws Exception {
		getCurrentProcess().getScript().setIndentCode(true);
		
		String arduinoUnoRelease = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
		boolean isRelease3 = ArduinoUnoDACQConfigurationProperties.REVISION_R3.equals(arduinoUnoRelease);
		
		reset();
		
		String code = "";
		String codeModule = "";
		
		ArduinoUnoCodeSegmentProperties[] segments = ArduinoUnoCodeSegmentProperties.values();
		for (int i = 0; i < segments.length; i++) {
			code = code + getCodeSegment(segments[i]);
			
			codeModule = "";
			for (int j = 0; j < getDACQConfiguration().getModulesNumber(); j++) {
					ModuleBehaviour module = getDACQConfiguration().getModule(j);
					if((module instanceof Module) || (module == script && module != null))
					codeModule = codeModule + module.getCodeSegment(segments[i]);
			}
			
			code = code + codeModule;
			
			if(segments[i].equals(ArduinoUnoCodeSegmentProperties.DECLARATION)) {
				// Variables declaration
				code = code + VariablesCodeGenerationDelegate.getCode(this, ArduinoUnoCodeSegmentProperties.DECLARATION) + "\n";
				// Parameters reception (uses the variables)
				code = code + getParametersDeclarationCode();
			}
			
			if(segments[i].equals(ArduinoUnoCodeSegmentProperties.INITIALIZATION)) {
				code = code + "\t\t// ******** Début algorithme initialisation\n\n";
				code = code + getCurrentProcess().getScript().getInitializeCode(this, ScriptSegmentType.INITIALIZE);
				
				code = code + "\t\t// ******** Fin algorithme initialisation\n\n";
				
				if(isRelease3) {
					code = code + "\t\t// Disable interrupts\n";
					code = code + "\t\tcli();\n";
					code = code + "\t\t// Timer 0 is used to compute workload\n";
					double gf = Double.parseDouble(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY));
					int prescaler = computeCounter0Prescaler(gf);
					double dt = 1/(16000000.0/prescaler)*1000000;
					code = code + "\t\t// Prescaler is set to " + prescaler + ", each count is " + dt + "us\n";
					code = code + "\t\t// Prescaler can take 1, 8, 64, 256 or 1024\n";
					code = code + "\t\t// Prescaler >= 16*10^6/(250*" + gf + ") in order to have max 250 counts in realtime loop\n";
					code = code + "\t\tTCCR0A = 0;\n";
					code = code + "\t\tTCNT0 = 0;\n";
					if(prescaler == 1) code = code + "\t\tTCCR0B = (1 << CS00);\n";
					if(prescaler == 8) code = code + "\t\tTCCR0B = (1 << CS01);\n";
					if(prescaler == 64) code = code + "\t\tTCCR0B = (1 << CS01) | (1 << CS00);\n";
					if(prescaler == 256) code = code + "\t\tTCCR0B = (1 << CS02);\n";
					if(prescaler == 1024) code = code + "\t\tTCCR0B = (1 << CS02) | (1 << CS00);\n";
					int[] values = computePrescaleAndCmpValues(gf); 
					code = code + "\t\t// Set timer1 interrupt at " + gf +"Hz\n";
					code = code + "\t\tTCCR1A = 0;// set entire TCCR2A register to 0\n";
					code = code + "\t\tTCCR1B = 0;// same for TCCR2B\n";
					code = code + "\t\tTCNT1  = 0;//initialize counter value to 0\n";
					code = code + "\t\t// Set compare match register for " + gf + "Hz increments : " + values[1] + "\n";
					code = code + "\t\tOCR1A = " + values[1] +";// = (16*10^6) / (" + gf + "*" + values[0] + ") - 1 (must be <65535)\n";
					code = code + "\t\t// CTC mode\n";
					code = code + "\t\tTCCR1B |= (1 << WGM12);\n";
					code = code + "\t\t// Set prescaler CS11 and CS10 bits to "+ values[0] +"\n";
					if(values[0] == 1) code = code + "\t\tTCCR1B |= (1 << CS10);\n";;
					if(values[0] == 8) code = code + "\t\tTCCR1B |= (1 << CS11);\n";;
					if(values[0] == 64) code = code + "\t\tTCCR1B |= (1 << CS11) | (1 << CS10);\n";;
					if(values[0] == 256) code = code + "\t\tTCCR1B |= (1 << CS12);\n";;
					if(values[0] == 1024) code = code + "\t\tTCCR1B |= (1 << CS12) | (1 << CS10);\n";;
					code = code + "\t\t// Clear a pending compare interrupt (restart) and enable timer compare interrupt\n";
					code = code + "\t\tTIFR1 = (1 << OCF1A);\n";
					code = code + "\t\tTIMSK1 |= (1 << OCIE1A);\n";
					code = code + "\t\t// Enable interrupts\n";
					code = code + "\t\tsei();\n";
					code = code + "\t\tfirstStart = false;\n";
				}
				if(isRelease4Wifi) {
					String sf = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
					code = code + "\t\tbool realtimeLoop_Timer_Started = true;\n";
					code = code + "\t\t// The timer is configured once : at restart, it has only been stopped (see restartProcess())\n";
					code = code + "\t\tif(firstStart) {\n";
					code = code + "\t\t\t\tuint8_t timerType = GPT_TIMER;\n";
					code = code + "\t\t\t\tint8_t timerIndex = FspTimer::get_available_timer(timerType);\n";
					code = code + "\t\t\t\tif (timerIndex < 0) timerIndex = FspTimer::get_available_timer(timerType, true);\n";
					code = code + "\t\t\t\trealtimeLoop_Timer_Started = (timerIndex >= 0);\n";
					code = code + "\t\t\t\tFspTimer::force_use_of_pwm_reserved_timer();\n";
					code = code + "\t\t\t\trealtimeLoop_Timer_Started = realtimeLoop_Timer_Started && realtimeLoop_Timer.begin(TIMER_MODE_PERIODIC, timerType, timerIndex, " + sf + ", 0.0f, processing);\n";
					// Priority 13, lower than the one of the interrupts of the serial port (UART to the ESP32), I2C and USB (12) :
					// they can interrupt the real time loop (end of a transmission, I2C transfer of ADS1115 inputs)
					code = code + "\t\t\t\trealtimeLoop_Timer_Started = realtimeLoop_Timer_Started && realtimeLoop_Timer.setup_overflow_irq(13);\n";
					code = code + "\t\t\t\trealtimeLoop_Timer_Started = realtimeLoop_Timer_Started && realtimeLoop_Timer.open();\n";
					code = code + "\t\t}\n";
					code = code + "\t\trealtimeLoop_Timer_Started = realtimeLoop_Timer_Started && realtimeLoop_Timer.start();\n";
					code = code + "\t\tif(!realtimeLoop_Timer_Started) {\n";
					code = code + "\t\t\t\tSerial.println(\"Error : unable to start real time loop timer !\");\n";
					code = code + "\t\t\t\twhile(true);\n";
					code = code + "\t\t}\n";
					code = code + "\t\tfirstStart = false;\n";
				}
			}
			if(segments[i].equals(ArduinoUnoCodeSegmentProperties.TRANSFER)) {
				// Variables transfer
				code = code + VariablesCodeGenerationDelegate.getCode(this, ArduinoUnoCodeSegmentProperties.TRANSFER);
			}
			
			if (segments[i] == ArduinoUnoCodeSegmentProperties.LOOP){
				code = code + "\t\t// ******** Début algorithme boucle\n\n";
				code = code + getCurrentProcess().getScript().getLoopCode(this, ScriptSegmentType.LOOP);
				code = code + "\n\t\t// ******** Fin algorithme boucle\n\n";
			}
			
//			if (segments[i] == ArduinoUnoCodeSegmentProperties.FINALIZATION){
//				code = code + "\t\t\t\t\t\t// ******** Début algorithme finalisation\n\n";
//				code = code + getCurrentProcess().getScript().getFinalizeCode(this, ScriptSegmentType.FINALIZE);
//				code = code + "\n\t\t\t\t\t\t// ******** Fin algorithme finalisation\n\n";
//			}
			
//			codeModule = "";
//			for (int j = 0; j < getDACQConfiguration().getModulesNumber(); j++) {
//					ModuleBehaviour module = getDACQConfiguration().getModule(j);
//					if((module instanceof Module) || (module == script && module != null))
//					codeModule = codeModule + module.getCodeSegment(segments[i]);
//			}
//			
//			code = code + codeModule;
		}
		
		return code;
	}
	
	private int computeCounter0Prescaler(double gf) {
		double prescaler = 16000000/(250*gf);
		if(prescaler <= 1) return 1;
		else if(prescaler <= 8) return 8;
		else if(prescaler <= 64) return 64;
		else if(prescaler <= 256) return 256;
		return 1024;
	}
	
	private int[] computePrescaleAndCmpValues(double gf) {
		double[] cmpValues = new double[5];
		double prescaleValue = 0;
		for (int i = 0; i < 5; i++) {
			if(i == 0) prescaleValue = 1;
			if(i == 1) prescaleValue = 8;
			if(i == 2) prescaleValue = 64;
			if(i == 3) prescaleValue = 256;
			if(i == 4) prescaleValue = 1024;
			cmpValues[i] = 16000000.0/(prescaleValue*gf) - 1;
			if ((cmpValues[i] == Math.floor(cmpValues[i])) && !Double.isInfinite(cmpValues[i]) && cmpValues[i] < 65536) {
			    return new int[] {(int) prescaleValue, (int) cmpValues[i]};
			} else {
				
			}
		}
		// Default return result from prescaler and counter value with nearest sampling frequency
		int selectedCmpValue = 0;
		int selectedPrescaler = 0;
		if(cmpValues[0] < 65536) {
			selectedPrescaler = 1;
			selectedCmpValue = (int) cmpValues[0];
		} else if(cmpValues[1] < 65536) {
			selectedPrescaler = 8;
			selectedCmpValue = (int) cmpValues[1];
		} else if(cmpValues[2] < 65536) {
			selectedPrescaler = 64;
			selectedCmpValue = (int) cmpValues[2];
		} else if(cmpValues[3] < 65536) {
			selectedPrescaler = 256;
			selectedCmpValue = (int) cmpValues[3];
		} else if(cmpValues[4] < 65536) {
			selectedPrescaler = 1024;
			selectedCmpValue = (int) cmpValues[4];
		} 
		Activator.logWarningMessage(NLS.bind(ArduinoUnoMessages.gfNotMatchMessage1, gf));
		double f = 16000000.0/(selectedPrescaler*(selectedCmpValue + 1));
		Activator.logWarningMessage(NLS.bind(ArduinoUnoMessages.gfNotMatchMessage2, new Object[] {f, selectedPrescaler, selectedCmpValue}));
		Activator.logWarningMessage(ArduinoUnoMessages.gfNotMatchMessage3);
		return new int[] {selectedPrescaler, selectedCmpValue};
	}
	
	
	
	private void deleteSketchDirectory(File file) {
		// if the file is directory or not
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			// if the directory contains any file
			if (files != null) {
				for (File innerFile : files) {
					// recursive call if the subdirectory is non-empty
					deleteSketchDirectory(innerFile);
				}
			}
		}
		if (file.exists()) if(!file.delete()) Activator.logWarningMessage("Clean build. File " + file + " not deleted !");
	}
	
	@Override
	public void cleanBuild() {
		IResource processResource = ObjectsController.getResourceForObject(this);
		IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
		String currentFolder = wsPath.append(processResource.getParent().getFullPath()).toOSString();
		String outputFolder = currentFolder + File.separator + "BinSource" + File.separator + processResource.getName().replaceAll(Activator.processFileExtension +"$", "");
		File outputFolderFile = new File(outputFolder);
		deleteSketchDirectory(outputFolderFile);
	}
	
	/*
	 * Read a text file trying UTF-8 first (strict), then falling back to the platform native encoding.
	 * Never throws MalformedInputException : unmappable bytes are replaced.
	 */
	private static String readTextWithFallback(File file) throws IOException {
		byte[] bytes = Files.readAllBytes(file.toPath());
		try {
			return StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(bytes)).toString();
		} catch (CharacterCodingException e) {
			Charset charset = Charset.defaultCharset();
			String nativeEncoding = System.getProperty("native.encoding");
			if(nativeEncoding != null && Charset.isSupported(nativeEncoding)) charset = Charset.forName(nativeEncoding);
			return new String(bytes, charset);
		}
	}
	
	@Override
	public void compile(IProgressMonitor progressMonitor) throws Exception {
		cleanBuild();
		
		IResource dacqConfigurationResource = getDACQConfiguration().getResource();
		dacqConfigurationResource.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		
		// Check if revision has been specified
		String value = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		if( value == null) {
			IMarker marker = getDACQConfiguration().getResource().createMarker("ArduinoUnoDACQConfiguration.Revision");
			marker.setAttribute(IMarker.MESSAGE, ArduinoUnoMessages.arduinoRevisionNotSpecified);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		}
		// Arduino Uno R3 : 2048 bytes of RAM, warn if strings variables use a large part of it
		if(ArduinoUnoDACQConfigurationProperties.REVISION_R3.equals(value)) {
			int stringsBytes = 0;
			for (ArduinoUnoVariable variable : ((ArduinoUnoDACQConfiguration)getDACQConfiguration()).getVariables()) {
				if(variable.isString()) stringsBytes += variable.getSize() + 1;
			}
			if(stringsBytes > STRINGS_BYTES_WARNING_R3) {
				IMarker marker = getDACQConfiguration().getResource().createMarker(DocometreBuilder.MARKER_ID);
				marker.setAttribute(IMarker.MESSAGE, NLS.bind(ArduinoUnoMessages.StringsMemoryWarning, stringsBytes));
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			}
		}
		ExperimentsView.refresh(getDACQConfiguration().getResource(), null);
		
		IResource processResource = ObjectsController.getResourceForObject(this);
		IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
		String currentFolder = wsPath.append(processResource.getParent().getFullPath()).toOSString();
		String outputFolder = currentFolder + File.separator + "BinSource" + File.separator + processResource.getName().replaceAll(Activator.processFileExtension +"$", "");
		File outputFolderFile = new File(outputFolder);
		outputFolderFile.mkdirs();
		File buildFolderFile = new File(outputFolder + File.separator + "Build");
		buildFolderFile.mkdirs();
		
		final File compileFile = new File(outputFolder + File.separator + "Build" + File.separator + "compile.txt");
		if(compileFile.exists()) compileFile.delete();
		
		progressMonitor.subTask(DocometreMessages.ArduinoUnoProcess_CompileCMDLineMessage);
		String sketchFilePath = outputFolder + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension +"$", ".ino");
		createSketchFile(processResource, this, sketchFilePath, true);
		String arduinoUnoCompiler = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH);
		String cmdLine = "";
		if(Platform.getOS().equals(Platform.OS_WIN32)) cmdLine = createWindowsCompileProcess(currentFolder, outputFolder, arduinoUnoCompiler, sketchFilePath);
		if(Platform.getOS().equals(Platform.OS_LINUX)) cmdLine = createLinuxCompileProcess(currentFolder, outputFolder, arduinoUnoCompiler, sketchFilePath);
		if(Platform.getOS().equals(Platform.OS_MACOSX)) cmdLine = createOSXCompileProcess(currentFolder, outputFolder, arduinoUnoCompiler, sketchFilePath);
		progressMonitor.worked(1);
	    
	    java.lang.Process process = Runtime.getRuntime().exec(new String[] {cmdLine});
	    boolean stillWaiting = true;
	    while (stillWaiting && !progressMonitor.isCanceled()) {
	    	stillWaiting = !process.waitFor(1, TimeUnit.SECONDS);						
		}
	    if(progressMonitor.isCanceled() && process.isAlive()) 
	    	process.destroy();
	    if(progressMonitor.isCanceled()) throw new OperationCanceledException();
	    
	    // Exit code of the build (exit code of the last command of the script : the compilation)
	    int exitCode = process.exitValue();
	    
	    String line;
	    // Output of the build when it is not written in compile.txt (arduino-builder)
	    StringBuilder standardOutput = new StringBuilder();
	    BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
		while((line=input.readLine()) != null){
			Activator.logInfoMessage(line, ArduinoUnoProcess.class);
			standardOutput.append(line).append("\n");
		}
		input.close();
	    
	    // Get compile results
	    progressMonitor.subTask(DocometreMessages.ArduinoUnoProcess_GetCompileErrorsMessage);
	    processResource.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		// compile.txt is written by shell redirection : its encoding depends on the OS/console
		// (e.g. cp1252/cp850 on Windows with accented paths or localized messages), not always UTF-8.
		String compileOutput = compileFile.exists() ? readTextWithFallback(compileFile) : "";
		Activator.logInfoMessage(compileOutput, ArduinoUnoProcess.class);
		// Errors of the compiler, errors of the linker and failure of the build (exit code, failure lines)
		ArduinoUnoBuildOutput buildOutput = ArduinoUnoBuildOutput.analyze(compileOutput + "\n" + standardOutput, exitCode);
		boolean errorDetected = buildOutput.hasFailed();
		for (ArduinoUnoBuildOutput.Error error : buildOutput.getErrors()) {
			String errorMessage = error.getMessage();
			if(error.getKind() == ArduinoUnoBuildOutput.Kind.LINKER) errorMessage = NLS.bind(ArduinoUnoMessages.LinkError, errorMessage);
			if(error.getKind() == ArduinoUnoBuildOutput.Kind.BUILD) errorMessage = NLS.bind(ArduinoUnoMessages.BuildFailed, errorMessage);
			int errorLine = error.getLine();
			int errorColumn = error.getColumn();
			if(error.getKind() == ArduinoUnoBuildOutput.Kind.COMPILER && !error.isInSketch()) {
				// Error in another file (library, core...) : its line is not a line of the sketch
				errorMessage = error.getFileName() + ":" + errorLine + ":" + errorColumn + ": " + errorMessage;
				errorLine = 1;
				errorColumn = 0;
			}
			createMarker(IMarker.SEVERITY_ERROR, processResource, errorLine, errorColumn, errorMessage);
		}

		Bundle librariesBundle = Platform.getBundle("Libraries");
		String message = NLS.bind(DocometreMessages.CurrentLibrariesVersion, librariesBundle.getVersion().toString());
		Activator.logInfoMessage(message, getClass());
		ArduinoUnoDACQConfiguration dacqConfig = (ArduinoUnoDACQConfiguration)getDACQConfiguration();
		String librariesAbsolutePath = dacqConfig.getProperty(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH);
		Pattern pattern = Pattern.compile("Libraries_\\d\\.\\d.\\d.\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d");
		Matcher matcher = pattern.matcher(librariesAbsolutePath);
		if (matcher.find()) {
			String usedLibrariesVersion = librariesAbsolutePath.substring(matcher.start(), matcher.end());
			usedLibrariesVersion = usedLibrariesVersion.replaceAll("Libraries_", "");
			message = NLS.bind(DocometreMessages.DacqConfLibrariesVersion, usedLibrariesVersion);
			Activator.logInfoMessage(message, getClass());
			if(!usedLibrariesVersion.equals(librariesBundle.getVersion().toString())) {
				message = NLS.bind(DocometreMessages.UpdateLibrariesVersion, usedLibrariesVersion, librariesBundle.getVersion().toString());
				Activator.logWarningMessage(message);
				IMarker marker = getDACQConfiguration().getResource().createMarker("ArduinoUnoDACQConfiguration.Libraries");
				marker.setAttribute(IMarker.MESSAGE, message);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			}
		}
		ExperimentsView.refresh(getDACQConfiguration().getResource(), null);
		

		if (getScript().getCodeGenerationStatus().length > 0) {
			IStatus[] statuses = getScript().getCodeGenerationStatus();
			for (IStatus status : statuses) {
				try {
					IMarker marker = processResource.createMarker(DocometreBuilder.MARKER_ID);
					marker.setAttribute(IMarker.MESSAGE, status.getMessage());
					marker.setAttribute(IMarker.LINE_NUMBER, 1);
					marker.setAttribute(DocometreBuilder.CODE_GENERATION_MARKER_ATTRIBUTE, true);
					marker.setAttribute(IMarker.SEVERITY,
							status.getSeverity() == IStatus.ERROR ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
					if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR) == IMarker.SEVERITY_ERROR)
						Activator.logErrorMessage(status.getMessage());
					if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_WARNING)
						Activator.logWarningMessage(status.getMessage());

				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
		}
		
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchPart editorPart = Activator.getEditor(ArduinoUnoProcess.this, ArduinoUnoProcessEditor.ID);
				if(editorPart != null) {
					((ProcessEditor)editorPart).updateTitleImage();
					PageChangedEvent event = new PageChangedEvent((IPageChangeProvider) editorPart, ((ProcessEditor)editorPart));
					((ProcessEditor)editorPart).pageChanged(event);
				}
				ArduinoUnoDACQConfiguration dacqConfiguration = (ArduinoUnoDACQConfiguration) ObjectsController.getDACQConfiguration(ArduinoUnoProcess.this);
//				IResource dacqConfigurationResource = ObjectsController.getResourceForObject(dacqConfiguration);
				editorPart = Activator.getEditor(dacqConfiguration, ArduinoUnoDACQConfigurationEditor.ID);
				if(editorPart != null) {
					((ArduinoUnoDACQConfigurationEditor)editorPart).updateTitleImage();
				}
			}
		});
			

		forceUpload = true;
		
		progressMonitor.worked(1);
		
		if (errorDetected) {
			throw new Exception(DocometreMessages.ArduinoUnoProcess_CompileErrorsMessage);
		}
		
		
	}
	
	private void createMarker(int severity, IResource processResource, final int numLine, final int numColumn, final String message) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					if(severity == IMarker.SEVERITY_ERROR) Activator.logErrorMessage(message);
					if(severity == IMarker.SEVERITY_WARNING) Activator.logWarningMessage(message);
					IMarker marker = processResource.createMarker(DocometreBuilder.MARKER_ID);
					marker.setAttribute(IMarker.MESSAGE, message);
					marker.setAttribute(IMarker.LINE_NUMBER, numLine);
					marker.setAttribute(IMarker.CHAR_START, numColumn);
					marker.setAttribute(IMarker.CHAR_END, message.length() - numColumn);
					marker.setAttribute(IMarker.SEVERITY, severity);
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
		});
	}
	
//	private void createMarker(int severity, IResource processResource, final String errorLine) {
//		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
//			@Override
//			public void run() {
//				if(severity == IMarker.SEVERITY_ERROR) Activator.logErrorMessage(errorLine);
//				if(severity == IMarker.SEVERITY_WARNING) Activator.logWarningMessage(errorLine);
//				String[] lines = errorLine.split("\\r?\\n");
//				for (String line : lines) {
//					if(line.contains("error:") || line.contains("warning:")) {
//						try {
//							IMarker marker = processResource.createMarker(DocometreBuilder.MARKER_ID);
//							String[] localLines = line.split(":");
//							marker.setAttribute(IMarker.MESSAGE, localLines[localLines.length - 1]);
//							//boolean useCLI = "true".equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI))?true:false;
//							//if(useCLI) marker.setAttribute(IMarker.LINE_NUMBER, Integer.parseInt(localLines[localLines.length - 5]));
//							//else
//							marker.setAttribute(IMarker.LINE_NUMBER, Integer.parseInt(localLines[localLines.length - 4]));
//							marker.setAttribute(IMarker.SEVERITY, severity);
////							marker.setAttribute(IMarker.CHAR_START, 0);
////							marker.setAttribute(IMarker.CHAR_END, localLines[localLines.length - 1].length() - 1);
//						} catch (CoreException e) {
//							Activator.logErrorMessageWithCause(e);
//							e.printStackTrace();
//						}
//					}
//				}
//			}
//		});
//	}
	
	private String createWindowsCompileProcess(String currentFolder, String outputFolder, String arduinoUnoCompiler, String sketchFilePath) throws IOException, InterruptedException {
		// We need to create a bash file in order to launch Arduino uno's compilation
		// Bash file path
		final String bashFilePath = outputFolder + File.separator + "compileSketch.bat";
		// Create Bash file
		final File bashFile = new File(bashFilePath);
		if(bashFile.exists()) bashFile.delete();
		FileWriter fileWriter = new FileWriter(bashFile);
		// E.G. : ./arduino-builder -hardware=hardware/ -tools=hardware/tools/ -tools=tools-builder/ -fqbn=arduino:avr:uno -verbose -build-path=c:\Users\frank\Desktop\TEST\test\build -compile ./TEST/SketchSample/SketchSample.ino
		String rootPath = arduinoUnoCompiler.replaceAll("arduino-builder.exe", "");
		String cmd = "rmdir /S /Q \"" + outputFolder + File.separator + "Build\"\n";
		cmd = cmd + "mkdir \"" + outputFolder + File.separator + "Build\"\n";
		
		String arduinoUnoRelease = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
//		boolean isRelease3 = ArduinoUnoDACQConfigurationProperties.REVISION_R3.equals(arduinoUnoRelease);
		String fullQualifiedBoardName = "arduino:avr:uno"; //$NON-NLS-N$ default is release 3
		if(isRelease4Wifi) fullQualifiedBoardName = "arduino:renesas_uno:unor4wifi"; //$NON-NLS-N$ Release 4 Wifi
		
		boolean useCLI = "true".equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI))?true:false;
		if(!useCLI) {			
			cmd = cmd + "\"" +arduinoUnoCompiler + "\" -hardware=\"" + rootPath + "hardware\" -tools=\"" + rootPath + "hardware\\tools\" -tools=\"" + rootPath + "tools-builder\" -fqbn=" + fullQualifiedBoardName + " -quiet -verbose";
			cmd = cmd + " -built-in-libraries=\"" + rootPath + "libraries\"";
//			IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
//			String userLibrariesPath = preferenceStore.getString(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getKey());
			String userLibrariesPath = getDACQConfiguration().getProperty((ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH));
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " -libraries " + userLibrariesPath;
			cmd = cmd + " -build-path=" + outputFolder + File.separator + "Build";
			cmd = cmd + " -compile " + sketchFilePath;// + " > stdout.txt 2>stderr.txt";
		} else {
			String arduinoCLIPATH = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.ARDUINOCLI_PATH);
			
			String userLibrariesPath = getDACQConfiguration().getProperty((ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH));
			// --libraries must be given before the output redirection
			cmd = cmd + "\"" + arduinoCLIPATH + "\"" + " compile -b " + fullQualifiedBoardName + " --output-dir \"" + outputFolder + File.separator + "Build\" \"" + sketchFilePath + "\"";
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " --libraries \"" + userLibrariesPath + "\"";
			cmd = cmd + " > " + outputFolder + File.separator + "Build" +  File.separator + "compile.txt 2>&1";
		}
		fileWriter.write(cmd);
		fileWriter.close();
		// This bash file must be executable 
//		java.lang.Process process = Runtime.getRuntime().exec("chmod 777 " + bashFilePath);
//		process.waitFor();
		return /*"/bin/sh " +*/ bashFilePath;
	}
	
	private String createOSXCompileProcess(String currentFolder, String outputFolder, String arduinoUnoCompiler, String sketchFilePath) throws IOException, InterruptedException {
		// We need to create a bash file in order to launch Arduino uno's compilation
		// Bash file path
		final String bashFilePath = outputFolder + File.separator + "compileSketch.sh";
		// Create Bash file
		final File bashFile = new File(bashFilePath);
		if(bashFile.exists()) bashFile.delete();
		FileWriter fileWriter = new FileWriter(bashFile);
		// E.G. : ./arduino-builder -hardware=hardware/ -tools=hardware/tools/ -tools=tools-builder/ -fqbn=arduino:avr:uno -verbose -build-path=c:\Users\frank\Desktop\TEST\test\build -compile ./TEST/SketchSample/SketchSample.ino
		String rootPath = arduinoUnoCompiler.replaceAll("arduino-builder", "");
		String cmd = "rm -R -f " + outputFolder + File.separator + "Build\n";
		cmd = cmd + "mkdir " + outputFolder + File.separator + "Build\n";
		
		String arduinoUnoRelease = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
		String fullQualifiedBoardName = "arduino:avr:uno"; //$NON-NLS-N$ default is release 3
		if(isRelease4Wifi) fullQualifiedBoardName = "arduino:renesas_uno:unor4wifi"; //$NON-NLS-N$ Release 4 Wifi
		
		boolean useCLI = "true".equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI))?true:false;
		if(!useCLI) {
			cmd = cmd + arduinoUnoCompiler + " -hardware=" + rootPath + "hardware/ -tools=" + rootPath + "hardware/tools/ -tools=" + rootPath + "tools-builder/ -fqbn=" + fullQualifiedBoardName + " -quiet -verbose";
			cmd = cmd + " -built-in-libraries=" + rootPath + "libraries";
			String userLibrariesPath = getDACQConfiguration().getProperty((ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH));
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " -libraries " + userLibrariesPath;
			cmd = cmd + " -build-path=" + outputFolder + File.separator + "Build";
			cmd = cmd + " -compile " + sketchFilePath;// + " > stdout.txt 2>stderr.txt";
		} else {
			String arduinoCLIPATH = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.ARDUINOCLI_PATH);
			String userLibrariesPath = getDACQConfiguration().getProperty((ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH));
			// --libraries must be given before the output redirection
			cmd = cmd + "\"" + arduinoCLIPATH + "\"" + " compile -b " + fullQualifiedBoardName + " --output-dir \"" + outputFolder + File.separator + "Build\" \"" + sketchFilePath + "\"";
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " --libraries \"" + userLibrariesPath + "\"";
			cmd = cmd + " > " + outputFolder + File.separator + "Build" +  File.separator + "compile.txt 2>&1";
		}
		
		fileWriter.write(cmd);
		fileWriter.close();
		// This bash file must be executable 
		java.lang.Process process = Runtime.getRuntime().exec(new String[]{"chmod", "777", bashFilePath});
		process.waitFor();
		return /*"/bin/sh " +*/ bashFilePath;
	}
	
	private String createLinuxCompileProcess(String currentFolder, String outputFolder, String arduinoUnoCompiler, String sketchFilePath) throws IOException, InterruptedException {
		// We need to create a bash file in order to launch Arduino uno's compilation
		// Bash file path
		final String bashFilePath = outputFolder + File.separator + "compileSketch.sh";
		// Create Bash file
		final File bashFile = new File(bashFilePath);
		if(bashFile.exists()) bashFile.delete();
		FileWriter fileWriter = new FileWriter(bashFile);
		// E.G. : ./arduino-builder -hardware=hardware/ -tools=hardware/tools/ -tools=tools-builder/ -fqbn=arduino:avr:uno -verbose -build-path=c:\Users\frank\Desktop\TEST\test\build -compile ./TEST/SketchSample/SketchSample.ino
		String rootPath = arduinoUnoCompiler.replaceAll("arduino-builder", "");
		String cmd = "rm -R -f " + outputFolder + File.separator + "Build\n";
		cmd = cmd + "mkdir " + outputFolder + File.separator + "Build\n";
		
		String arduinoUnoRelease = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
		String fullQualifiedBoardName = "arduino:avr:uno"; //$NON-NLS-N$ default is release 3
		if(isRelease4Wifi) fullQualifiedBoardName = "arduino:renesas_uno:unor4wifi"; //$NON-NLS-N$ Release 4 Wifi
		
		boolean useCLI = "true".equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI))?true:false;
		if(!useCLI) {
			cmd = cmd + arduinoUnoCompiler + " -hardware=" + rootPath + "hardware/ -tools=" + rootPath + "hardware/tools/ -tools=" + rootPath + "tools-builder/ -fqbn=" + fullQualifiedBoardName + " -quiet -verbose";
			cmd = cmd + " -built-in-libraries=" + rootPath + "libraries";
			String userLibrariesPath = getDACQConfiguration().getProperty((ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH));
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " -libraries " + userLibrariesPath;
			cmd = cmd + " -build-path=" + outputFolder + File.separator + "Build";
			cmd = cmd + " -compile " + sketchFilePath;// + " > stdout.txt 2>stderr.txt";
		} else {
			String arduinoCLIPATH = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.ARDUINOCLI_PATH);
			String userLibrariesPath = getDACQConfiguration().getProperty((ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH));
			// --libraries must be given before the output redirection
			cmd = cmd + "\"" + arduinoCLIPATH + "\"" + " compile -b " + fullQualifiedBoardName + " --output-dir \"" + outputFolder + File.separator + "Build\" \"" + sketchFilePath + "\"";
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " --libraries \"" + userLibrariesPath + "\"";
			cmd = cmd + " > " + outputFolder + File.separator + "Build" +  File.separator + "compile.txt 2>&1";
		}
		
		fileWriter.write(cmd);
		fileWriter.close();
		// This bash file must be executable 
		java.lang.Process process = Runtime.getRuntime().exec(new String[]{"chmod", "777", bashFilePath});
		process.waitFor();
		return /*"/bin/sh " +*/ bashFilePath;
	}
	

	@Override
	public void run(boolean compile, String prefix, String suffix) throws Exception {
		if(compile) compile(null);
		execute(prefix, suffix);
	}

	@Override
	public void stop() throws Exception {
		long stopStartNanos = System.nanoTime();
		realTimeLoopJob.forceTermination();
		// No time out : the termination (handshake with Arduino) is done by the real time loop
		TrialTimings.stopMeasured(System.nanoTime() - stopStartNanos, false);
	}

	@Override
	public Job execute(String prefix, String suffix) throws Exception {
		// Load process
		boolean processLoaded = false;
		try {
			processLoaded = loadProcess();
			TrialTimings.step("ArduinoUnoProcess.loadProcess() (upload if needed)");
			if(processLoaded) {
				// Create Job
				// Values of the propagated parameters, received at the end of the process (see RealTimeLoopJob)
				propagatedValues = null;
				realTimeLoopJob = new RealTimeLoopJob(DocometreMessages.RunningProcess);
				TrialTimings.step("RealTimeLoopJob created (serial port opened)");
				// Values of the parameters, sent while the Arduino waits for 'r' char
				if(!getCurrentTrialParameters().isEmpty()) realTimeLoopJob.sendParameters(getCurrentTrialParameters());
				// Open modules and variables
				open(this, prefix, suffix);
				TrialTimings.step("ArduinoUnoProcess.open() (channels files)");
				generation();
				TrialTimings.step("ArduinoUnoProcess.generation()");
				return realTimeLoopJob;
			} else throw new Exception("Impossible to load process.");
		} catch (Exception e) {
			close();
			throw e;
		}
	}

	private boolean loadProcess() throws Exception {
		// Verify device path
		ArduinoUnoDACQConfiguration dacqConfiguration = (ArduinoUnoDACQConfiguration)getDACQConfiguration();
		devicePath = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH);
		String[] choices = SerialPortList.getPortNames();
		if(!Arrays.asList(choices).contains(devicePath)) {
			forceUpload = true;
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					DeviceSelectionDialog deviceSelectionDialog = new DeviceSelectionDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), DeviceType.USB);
					if(deviceSelectionDialog.open() == Dialog.OK) {
						devicePath = deviceSelectionDialog.getSelection();
						getDACQConfiguration().setProperty(ArduinoUnoDACQConfigurationProperties.DEVICE_PATH, devicePath);
					}
				}
			});
		}
		
		String cmd = "";
		IResource processResource = ObjectsController.getResourceForObject(this);
		String resourcePath = processResource.getParent().getLocation().toOSString() + File.separatorChar + "BinSource" + File.separator + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + File.separator + "Build" + File.separator;
		String inoHexFilePath = resourcePath + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + ".ino.hex";
		String arduinoUnoRelease = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
		if(isRelease4Wifi) inoHexFilePath = resourcePath + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + ".ino.bin";
		if(previousINOHexFilePath.equals(inoHexFilePath) && !forceUpload) {
			appendToEventDiary("Process " + processResource.getFullPath() + " was already loaded\n" );
			return true;
		}
		IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
		String currentFolder = wsPath.append(processResource.getParent().getFullPath()).toOSString();
		String outputFolder = currentFolder + File.separator + "BinSource" + File.separator + processResource.getName().replaceAll(Activator.processFileExtension +"$", "");
		
		cmd = createUploadProcess(outputFolder);
		
		try {
			java.lang.Process processCMD = Runtime.getRuntime().exec(new String[] {cmd});
			processCMD.waitFor();
			String line;
			BufferedReader error = new BufferedReader(new InputStreamReader(processCMD.getErrorStream()));
			String message = "";
			while((line = error.readLine()) != null){
				message = message + line + "\n";
			}
			error.close();
			
			if(!message.equals("")) Activator.logErrorMessage(message);
			if(message.toLowerCase().contains("error")) {
				forceUpload = true;
				return false;
			}
			appendToEventDiary("Process " + processResource.getFullPath() + " loaded\n" );
			return true;
//			message = "";
//			BufferedReader input = new BufferedReader(new InputStreamReader(processCMD.getInputStream()));
//			while((line=input.readLine()) != null){
//				message = message + line + "\n";
//			}
//			input.close();
//			if(!message.equals("")) Activator.logInfoMessage(message, ArduinoUnoProcess.class);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
			return false;
		}
	}
	
	private String createUploadProcess(String outputFolder) throws IOException, InterruptedException {
		IResource processResource = ObjectsController.getResourceForObject(this);
		String resourcePath = processResource.getParent().getLocation().toOSString() + File.separatorChar + "BinSource" + File.separator + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + File.separator + "Build" + File.separator;
		boolean useCLI = "true".equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI))?true:false;
		String arduinoUnoRelease = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
		// .hex file with avrdude, .bin file with arduino-cli for R4 WiFi
		String inoHexFilePath = resourcePath + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + ".ino.hex";
		if(useCLI && isRelease4Wifi) inoHexFilePath = resourcePath + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + ".ino.bin";
		previousINOHexFilePath = inoHexFilePath;
		String cmd = ArduinoUnoUploadDelegate.getUploadCommand(getDACQConfiguration(), devicePath, inoHexFilePath);
		forceUpload = false;
		return ArduinoUnoUploadDelegate.createUploadScript(outputFolder, cmd);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initializeObservers() {
		// TODO Auto-generated method stub

	}

}
