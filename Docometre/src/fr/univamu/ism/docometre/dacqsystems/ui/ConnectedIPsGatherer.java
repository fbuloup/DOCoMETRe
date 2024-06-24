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
package fr.univamu.ism.docometre.dacqsystems.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;

public class ConnectedIPsGatherer {
	
	private int start;
	private int end;
	private int timeOut;
	private int count;
	private Stack<Pinger> pingers = new Stack<>();
	
	private class Pinger extends Thread {
		
		private String cmdLine;
		private boolean isReachable;
		private String ip;
		private long timeOut;
		private boolean isWindows;
		private boolean isMac;
		private boolean isLinux;
		private boolean terminated;
		
		public Pinger(String ip, int count, int timeOut) {
			this.ip = ip;
			this.timeOut = timeOut;
			String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
	        if ((os.contains("mac")) || (os.contains("darwin"))) {
	        	cmdLine = "ping -c " + count + " -W " + timeOut + " " + ip;
	        	isMac = true;
	        } else if (os.contains("win")) {
	        	cmdLine = "ping -n " + count + " -w " + timeOut + " " + ip;
	        	isWindows = true;
	        } else if (os.contains("nux")) {
	        	cmdLine = "fping -c " + count + " -t" + timeOut + " " + ip;
	        	isLinux = true;
	        } 
		}
		
		@Override
		public void run() {
			try {
				if(cmdLine != null) {
					java.lang.Process process = Runtime.getRuntime().exec(new String[] {cmdLine});
					process.waitFor(2*timeOut, TimeUnit.MILLISECONDS);
					String line;
					BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
					String outString = "";
					while((line = out.readLine()) != null){
						outString = outString + line + "\n";
					}
					out.close();
					if(isWindows) if(outString.contains("Received = 1"))  isReachable = true;
					if(isLinux) if(outString.contains("1 packets received"))  isReachable = true;
					if(isMac) if(outString.contains("1 packets received"))  isReachable = true;
					if(isWindows) if(outString.contains("perdus = 0"))  isReachable = true;
				}
			} catch (IOException | InterruptedException e) {
				Activator.logErrorMessage(cmdLine);
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			} finally {
				terminated = true;
			}
		}
	}

	public ConnectedIPsGatherer(int start, int end, int count, int timeOut) {
		this.start = start;
		this.end = end;
		this.count = count;
		this.timeOut = timeOut;
	}
	
	public void startGathering(IProgressMonitor monitor) {
		monitor.beginTask(DocometreMessages.IPsGathererMessage1, end - start + 1);
		String[] addresses = getIPV4Addresses();
		for (String address : addresses) {
			String mask = address.replaceAll("\\.\\d+$", "");
			for (int i = start; i <= end; i++) {
				Pinger pinger = new Pinger(mask + "." + i, count, timeOut);
				pinger.start();
				pingers.add(pinger);
				monitor.subTask(NLS.bind(DocometreMessages.IPsGathererMessage2, mask + "." + i));
				monitor.worked(1);
				if(monitor.isCanceled()) break;
			}
			if(monitor.isCanceled()) break;
		}
		monitor.subTask(DocometreMessages.IPsGathererMessage3);
		while(!hasEnded() && !monitor.isCanceled());
		monitor.done();
	}
	
	private boolean hasEnded() {
		for (Pinger pinger : pingers) {
			if(!pinger.terminated) 
				return false;
		}
		return true;
	}
	
	public String[] getReachableIPs() {
		ArrayList<String> ips = new ArrayList<>();
		for (Pinger pinger : pingers) {
			if(!pinger.isAlive()) {
				if(pinger.isReachable) ips.add(pinger.ip);
			}
		}
		return ips.toArray(new String[ips.size()]);
	}
	
	private String[] getIPV4Addresses() {
		ArrayList<String> ipv4Addresses = new ArrayList<>();
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while(networkInterfaces.hasMoreElements()) {
			    NetworkInterface networkInterface = (NetworkInterface) networkInterfaces.nextElement();
			    if(!networkInterface.isLoopback()) {
			    	Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				    while (inetAddresses.hasMoreElements()) {
				        InetAddress inetAddress = (InetAddress) inetAddresses.nextElement();
				        if(inetAddress instanceof Inet4Address) ipv4Addresses.add(inetAddress.getHostAddress());
				    }
			    }
			}
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		return ipv4Addresses.toArray(new String[ipv4Addresses.size()]);
	}
	
}
