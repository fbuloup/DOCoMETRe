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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.ModuleBehaviour;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.processeditor.ArduinoUnoProcessEditor;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionDialog;
import fr.univamu.ism.docometre.dacqsystems.ui.DeviceSelectionHandler.DeviceType;
import fr.univamu.ism.docometre.dacqsystems.ui.ProcessEditor;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.process.ScriptSegmentType;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

@SuppressWarnings("restriction")
public class ArduinoUnoProcess extends Process {
	
	private transient static double displayTimeRate = 0.1; 

	public transient static final long serialVersionUID = DACQConfiguration.serialVersionUID;
	public transient static String previousINOHexFilePath = "";
	public transient static boolean forceUpload = false;
	
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
		// Create Sketch file
		File file = new File(sketchFilePath);
		file.getParentFile().mkdirs();
		try(FileWriter fileWriter = new FileWriter(file)) {
			// Get process code
			String processCode = process.getCode(null);
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
		private double timeBefore;
		private double timeAfter;
//		private String channelName;

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
				Thread.sleep(2000);
				timeBefore = System.currentTimeMillis()/1000d;
				serialPort.writeString("r");
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
			getThread().setPriority(Thread.MAX_PRIORITY);
			double processBeginTime = System.currentTimeMillis()/1000d;
			String date = new SimpleDateFormat("EEE d MMM yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
			appendToEventDiary(date);
			appendToEventDiary("Starting " + ObjectsController.getResourceForObject(ArduinoUnoProcess.this).getName() + " at " + Double.toString(processBeginTime) + "\n");
			
			try {
				while(!terminate) {
					if(monitor.isCanceled()) {
						forceTermination = true;
						monitor.setCanceled(false);
					}
	    			if(serialPort.getInputBufferBytesCount() > 0) {
	    				byte[] bytes = serialPort.readBytes();
		            	for (byte b: bytes) {
		                    if ( (b == '\r' || b == '\n') && message.length() > 0 && !terminate) {
		                    	String messageString = message.toString().replaceAll("\\r$", "").replaceAll("\\n", "");
		                    	message.setLength(0);
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
		                    				appendToEventDiary(name + " : " + nb);
										}
		                    			appendToEventDiary("At : " + formater.format(realTime) + "s - Workload : " + workload);
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
		                    	if(b == 's') {
		                    		appendToEventDiary("Received stop 's' char");
		                    		terminate = true;
		                    		forceTermination = true;// Just to send s in reply as Arduino is waiting for it.
		                    		Thread.sleep(30);// Wait 30ms to be sure Arduino is waiting for 's' char.
		                    	} else message.append((char)b);
		                    }
		            	}
	    			}
	    			if(forceTermination) {
	    				serialPort.writeByte((byte) 's');
	    				forceTermination = false;
	    			}
				}
				
			} catch (Exception e) {
				Activator.logErrorMessageWithCause(e);							
				appendToEventDiary("Error receiving string from port : " + e.getMessage());
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
				ArduinoUnoProcess.this.close();
				try {
					serialPort.closePort();
				} catch (SerialPortException e) {
					Activator.logErrorMessageWithCause(e);
					return new Status(Status.ERROR, Activator.PLUGIN_ID, "Error closing port: " + e.getMessage());
				}
				double processEndTime = System.currentTimeMillis()/1000d;
				appendToEventDiary("\nEnd time (s) : " + Double.toString(processEndTime));
				appendToEventDiary(ObjectsController.getResourceForObject(ArduinoUnoProcess.this).getName() + " duration (s) is about : " + Double.toString(processEndTime - processBeginTime) + "\n");
				appendToEventDiary("Total samples for transfered channels :");
				for (int i = 0; i < nbSamples.length; i++) {
					appendToEventDiary(transferedChannelsOrderedByTransferNumber[i].getProperty(ChannelProperties.NAME) + " : " + nbSamples[i]);
				}
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
		
	}

	private transient String devicePath;
	private transient RealTimeLoopJob realTimeLoopJob;

	

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
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.INCLUDE);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.INCLUDE);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.INCLUDE) + "\n";
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.DEFINE) {
			code = code + "#define pi PI\n";
			
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
//			code = code + "// Loop time in usecond\n";
//			code = code + "unsigned long timeMicros;\n\n";
			code = code + "// Loop index to compute time\n";
			code = code + "unsigned long timeIndex;\n\n";
			code = code + "// Start loop when true\n";
			code = code + "bool startLoop;\n\n";
			code = code + "// Start WDT when true\n";
			code = code + "bool startWDT;\n\n";
			code = code + "// Stop loop when true\n";
			code = code + "bool terminateProcess;\n\n";
//			code = code + "// First loop flag\n";
//			code = code + "bool firstLoop = true;\n\n";
			code = code + "// String serialMessage\n";
			code = code + "char serialMessage[64];\n";
			if(isRelease4Wifi) code = code + "String globalSerialMessage;;\n\n";
			code = code + "\n";
			code = code + "// Index to send time and workload at regular intervall (#200ms)\n";
			code = code + "unsigned long sendTimeWorkload;\n\n";
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.DECLARATION);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.DECLARATION);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.DECLARATION);
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.INITIALIZATION) {
			code = "void setup() {\n";
			
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
			code = code + "\t\tstartWDT = false;\n";
			if(((ArduinoUnoDACQConfiguration)getDACQConfiguration()).hasADS1115Module()) {
				code = code + "\t\t// Start I2C interface\n";
				code = code + "\t\tWire.begin();\n";
			}
			String baudRate = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.BAUD_RATE);
			code = code + "\t\tSerial.begin(" + baudRate + ");\n";
			code = code + "\t\twhile (!startLoop) {\n";
			code = code + "\t\t\t\tif(Serial.available()) {\n";
			code = code + "\t\t\t\t\t\tstartLoop = ((char)Serial.read()) == 'r';\n";
			code = code + "\t\t\t\t}\n";
			code = code + "\t\t}\n";
			
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
			code = code + "\t\t\t\tif(Serial.available())\n";
			code = code + "\t\t\t\t\t\tstartWDT = ((char)Serial.read()) == 's';\n";
			code = code + "\t\t\t\tif(startWDT) {\n";
			
			if(isRelease4Wifi) code = code + "\t\t\t\t\t\tWDT.begin(15);\n";
			if(isRelease3) code = code + "\t\t\t\t\t\twdt_enable(WDTO_15MS);\n";
			code = code + "\t\t\t\t\t\tstartWDT = false;\n";
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
			if(isRelease4Wifi) code = code + "\t\tstartLoop = false;\n";
			if(isRelease3) {
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

			code = code + "void loop() {\n";
			if(isRelease3) code = code + "\t\t// Nothing to do !\n";
			if(isRelease4Wifi) {
				code = code + "\t\tif(!globalSerialMessage.equals(\"\")) {\n";
				code = code + "\t\t\t\tSerial.print(globalSerialMessage);\n";
				code = code + "\t\t\t\tglobalSerialMessage = \"\";\n";
				code = code + "\t\t\t\tSerial.flush();\n";
				code = code + "\t\t}\n";
				code = code + "\t\tif(terminateProcess) {\n";
				
				code = code + "\t\t\t\tif(!startLoop) {\n";
				code = code + "\t\t\t\t\t\tSerial.println('s');\n";
				code = code + "\t\t\t\t\t\tSerial.flush();\n";
				if(delay > 0) code = code + "\t\t\t\t\t\t\t\tdelayMicroseconds(" + delay + ");\n";
				code = code + "\t\t\t\t\t\tstartLoop = true;\n";
			    code = code + "\t\t\t\t}\n";
				code = code + "\t\t}\n";
			}
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
				code = code + "unsigned int acquireADS1115AnalogInput(ADS1115 ads, byte inputNumber, byte gain, bool transfert, byte transferNumber) {\n";
				code = code + "\t\tunsigned int value = 0;\n";
				code = code + "\t\tads.setGain(gain);\n";
				code = code + "\t\tinterrupts();\n";
				code = code + "\t\tvalue = ads.readADC(inputNumber);\n";
				code = code + "\t\tnoInterrupts();\n";
				code = code + "\t\tif(transfert) {\n";
				code = code + "\t\t\t\tsprintf(serialMessage, \"%d:%d\", transferNumber, value);\n";
				code = code + "\t\t\t\tSerial.println(serialMessage);\n";
				code = code + "\t\t\t\tSerial.flush();\n";
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
					code = code + "\t\t// Enable timer compare interrupt\n";
					code = code + "\t\tTIMSK1 |= (1 << OCIE1A);\n";
					code = code + "\t\t// Enable interrupts\n";
					code = code + "\t\tsei();\n";
				}
				if(isRelease4Wifi) {
					String sf = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
					code = code + "\t\tbool realtimeLoop_Timer_Started = true;\n";
					code = code + "\t\tuint8_t timerType = GPT_TIMER;\n";
					code = code + "\t\tint8_t timerIndex = FspTimer::get_available_timer(timerType);\n";
					code = code + "\t\tif (timerIndex < 0) timerIndex = FspTimer::get_available_timer(timerType, true);\n";
					code = code + "\t\trealtimeLoop_Timer_Started = (timerIndex >= 0);\n";
					code = code + "\t\tFspTimer::force_use_of_pwm_reserved_timer();\n";
					code = code + "\t\trealtimeLoop_Timer_Started = realtimeLoop_Timer_Started && realtimeLoop_Timer.begin(TIMER_MODE_PERIODIC, timerType, timerIndex, " + sf + ", 0.0f, processing);\n";
					code = code + "\t\trealtimeLoop_Timer_Started = realtimeLoop_Timer_Started && realtimeLoop_Timer.setup_overflow_irq();\n";
					code = code + "\t\trealtimeLoop_Timer_Started = realtimeLoop_Timer_Started && realtimeLoop_Timer.open();\n";
					code = code + "\t\trealtimeLoop_Timer_Started = realtimeLoop_Timer_Started && realtimeLoop_Timer.start();\n";
					code = code + "\t\tif(!realtimeLoop_Timer_Started) {\n";
					code = code + "\t\t\t\tSerial.println(\"Error : unable to start real time loop timer !\");\n";
					code = code + "\t\t\t\twhile(true);\n";
					code = code + "\t\t}\n";
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
	
	@Override
	public void compile(IProgressMonitor progressMonitor) throws Exception {
		cleanBuild();
		IResource processResource = ObjectsController.getResourceForObject(this);
		IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
		String currentFolder = wsPath.append(processResource.getParent().getFullPath()).toOSString();
		String outputFolder = currentFolder + File.separator + "BinSource" + File.separator + processResource.getName().replaceAll(Activator.processFileExtension +"$", "");
		File outputFolderFile = new File(outputFolder);
		outputFolderFile.mkdirs();
		File buildFolderFile = new File(outputFolder + File.separator + "Build");
		buildFolderFile.mkdirs();
		
//		final File errorFile = new File(outputFolder + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension + "$", ".ERR"));
//		if(errorFile.exists()) errorFile.delete();
//		final File outputFile = new File(outputFolder + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension + "$", ".OUT"));
//		if(outputFile.exists()) outputFile.delete();
		
		progressMonitor.subTask(DocometreMessages.ArduinoUnoProcess_CompileCMDLineMessage);
		String sketchFilePath = outputFolder + File.separator + processResource.getFullPath().lastSegment().replaceAll(Activator.processFileExtension +"$", ".ino");
		createSketchFile(processResource, this, sketchFilePath);
		String arduinoUnoCompiler = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.BUILDER_PATH);
		String cmdLine = "";
		if(Platform.getOS().equals(Platform.OS_WIN32)) cmdLine = createWindowsCompileProcess(currentFolder, outputFolder, arduinoUnoCompiler, sketchFilePath);
		if(Platform.getOS().equals(Platform.OS_LINUX)) cmdLine = createLinuxCompileProcess(currentFolder, outputFolder, arduinoUnoCompiler, sketchFilePath);
		if(Platform.getOS().equals(Platform.OS_MACOSX)) cmdLine = createOSXCompileProcess(currentFolder, outputFolder, arduinoUnoCompiler, sketchFilePath);
		progressMonitor.worked(1);
		
//	    ProcessBuilder pb = new ProcessBuilder(cmdLine);
//	    pb.redirectError(errorFile);
//	    pb.redirectOutput(outputFile);
//	    
//	    java.lang.Process p = pb.start();
//	    p.waitFor();
	    
	    
	    java.lang.Process process = Runtime.getRuntime().exec(new String[] {cmdLine});
		process.waitFor();
	    
	    String line;
		BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		String errorString = "";
		while((line = error.readLine()) != null){
			errorString = errorString + line + "\n";
		}
		error.close();
		
		BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
		while((line=input.readLine()) != null){
			Activator.logInfoMessage(line, ArduinoUnoProcess.class);
		}
		input.close();
	    
		processResource.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		
		// Get compile errors
		progressMonitor.subTask(DocometreMessages.ArduinoUnoProcess_GetCompileErrorsMessage);
		if (!errorString.equals("")) {
			createMarker(IMarker.SEVERITY_ERROR, processResource, errorString);
		}

		if (getScript().getCodeGenerationStatus().length > 0) {
			IStatus[] statuses = getScript().getCodeGenerationStatus();
			for (IStatus status : statuses) {
				try {
					IMarker marker = processResource.createMarker(DocometreBuilder.MARKER_ID);
					marker.setAttribute(IMarker.MESSAGE, status.getMessage());
					marker.setAttribute(IMarker.LINE_NUMBER, 1);
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
			}
		});
			

		forceUpload = true;
		
		progressMonitor.worked(1);
		
		if (!errorString.equals("")) {
			throw new Exception(DocometreMessages.ArduinoUnoProcess_CompileErrorsMessage);
		}
		
		
	}
	
	private void createMarker(int severity, IResource processResource, final String errorLine) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				if(severity == IMarker.SEVERITY_ERROR) Activator.logErrorMessage(errorLine);
				if(severity == IMarker.SEVERITY_WARNING) Activator.logWarningMessage(errorLine);
				String[] lines = errorLine.split("\\r?\\n");
				for (String line : lines) {
					if(line.contains("error:") || line.contains("warning:")) {
						try {
							IMarker marker = processResource.createMarker(DocometreBuilder.MARKER_ID);
							String[] localLines = line.split(":");
							marker.setAttribute(IMarker.MESSAGE, localLines[localLines.length - 1]);
							//boolean useCLI = "true".equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI))?true:false;
							//if(useCLI) marker.setAttribute(IMarker.LINE_NUMBER, Integer.parseInt(localLines[localLines.length - 5]));
							//else
							marker.setAttribute(IMarker.LINE_NUMBER, Integer.parseInt(localLines[localLines.length - 4]));
							marker.setAttribute(IMarker.SEVERITY, severity);
//							marker.setAttribute(IMarker.CHAR_START, 0);
//							marker.setAttribute(IMarker.CHAR_END, localLines[localLines.length - 1].length() - 1);
						} catch (CoreException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
					}
				}
			}
		});
	}
	
	private String createWindowsCompileProcess(String currentFolder, String outputFolder, String arduinoUnoCompiler, String sketchFilePath) throws IOException, InterruptedException {
		
		String arduinoUnoRelease = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
		boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
//		boolean isRelease3 = ArduinoUnoDACQConfigurationProperties.REVISION_R3.equals(arduinoUnoRelease);
		
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
		
		boolean useCLI = "true".equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI))?true:false;
		if(!useCLI) {			
			cmd = cmd + "\"" +arduinoUnoCompiler + "\" -hardware=\"" + rootPath + "hardware\" -tools=\"" + rootPath + "hardware\\tools\" -tools=\"" + rootPath + "tools-builder\" -fqbn=arduino:avr:uno -quiet -verbose";
			cmd = cmd + " -built-in-libraries=\"" + rootPath + "libraries\"";
//			IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
//			String userLibrariesPath = preferenceStore.getString(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getKey());
			String userLibrariesPath = getDACQConfiguration().getProperty((ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH));
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " -libraries " + userLibrariesPath;
			cmd = cmd + " -build-path=" + outputFolder + File.separator + "Build";
			cmd = cmd + " -compile " + sketchFilePath;// + " > stdout.txt 2>stderr.txt";
		} else {
			String arduinoCLIPATH = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.ARDUINOCLI_PATH);
			String fullQualifiedBoardName = "arduino:avr:uno"; //$NON-NLS-N$ default is release 3
			if(isRelease4Wifi) fullQualifiedBoardName = "arduino:renesas_uno:unor4wifi";
			cmd = cmd + "\"" + arduinoCLIPATH + "\"" + " compile -b " + fullQualifiedBoardName + " --output-dir \"" + outputFolder + File.separator + "Build\" \"" + sketchFilePath + "\"";			
			String userLibrariesPath = getDACQConfiguration().getProperty((ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH));
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " -libraries \"" + userLibrariesPath + "\"";
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
		
		boolean useCLI = "true".equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI))?true:false;
		if(!useCLI) {
			cmd = cmd + arduinoUnoCompiler + " -hardware=" + rootPath + "hardware/ -tools=" + rootPath + "hardware/tools/ -tools=" + rootPath + "tools-builder/ -fqbn=arduino:avr:uno -quiet -verbose";
			cmd = cmd + " -built-in-libraries=" + rootPath + "libraries";
			IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
			String userLibrariesPath = preferenceStore.getString(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getKey());
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " -libraries " + userLibrariesPath;
			cmd = cmd + " -build-path=" + outputFolder + File.separator + "Build";
			cmd = cmd + " -compile " + sketchFilePath;// + " > stdout.txt 2>stderr.txt";
		} else {
			String arduinoCLIPATH = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.ARDUINOCLI_PATH);
			cmd = cmd + "\"" + arduinoCLIPATH + "\"" + " compile -b arduino:avr:uno --output-dir \"" + outputFolder + File.separator + "Build\" \"" + sketchFilePath + "\"";			
			String userLibrariesPath = getDACQConfiguration().getProperty((ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH));
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " -libraries \"" + userLibrariesPath + "\"";
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
		
		boolean useCLI = "true".equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI))?true:false;
		if(!useCLI) {
			cmd = cmd + arduinoUnoCompiler + " -hardware=" + rootPath + "hardware/ -tools=" + rootPath + "hardware/tools/ -tools=" + rootPath + "tools-builder/ -fqbn=arduino:avr:uno -quiet -verbose";
			cmd = cmd + " -built-in-libraries=" + rootPath + "libraries";
			IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
			String userLibrariesPath = preferenceStore.getString(ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH.getKey());
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " -libraries " + userLibrariesPath;
			cmd = cmd + " -build-path=" + outputFolder + File.separator + "Build";
			cmd = cmd + " -compile " + sketchFilePath;// + " > stdout.txt 2>stderr.txt";
		} else {
			String arduinoCLIPATH = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.ARDUINOCLI_PATH);
			cmd = cmd + "\"" + arduinoCLIPATH + "\"" + " compile -b arduino:avr:uno --output-dir \"" + outputFolder + File.separator + "Build\" \"" + sketchFilePath + "\"";			
			String userLibrariesPath = getDACQConfiguration().getProperty((ArduinoUnoDACQConfigurationProperties.USER_LIBRARIES_ABSOLUTE_PATH));
			if(userLibrariesPath != null && !"".equals(userLibrariesPath)) cmd = cmd + " -libraries \"" + userLibrariesPath + "\"";
		}
		
		fileWriter.write(cmd);
		fileWriter.close();
		// This bash file must be executable 
		java.lang.Process process = Runtime.getRuntime().exec(new String[]{"chmod 777 " + bashFilePath});
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
		realTimeLoopJob.forceTermination();
	}

	@Override
	public Job execute(String prefix, String suffix) throws Exception {
		// Load process
		boolean processLoaded = false;
		try {
			processLoaded = loadProcess();
			if(processLoaded) {
				// Create Job
				realTimeLoopJob = new RealTimeLoopJob(DocometreMessages.RunningProcess);
				// Open modules and variables
				open(this, prefix, suffix);
				generation();
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
		// We need to create a bash file in order to launch Arduino uno's compilation
		// Bash file path
		String bashFilePath = outputFolder + File.separator;
		if(Platform.getOS().equals(Platform.OS_WIN32)) bashFilePath = bashFilePath + "uploadSketch.bat";
		if(Platform.getOS().equals(Platform.OS_MACOSX)) bashFilePath = bashFilePath + "uploadSketch.sh";
		if(Platform.getOS().equals(Platform.OS_LINUX)) bashFilePath = bashFilePath + "uploadSketch.sh";
		// Create Bash file
		final File bashFile = new File(bashFilePath);
		if(bashFile.exists()) bashFile.delete();
		FileWriter fileWriter = new FileWriter(bashFile);
		String cmd = "";
		IResource processResource = ObjectsController.getResourceForObject(this);
		String resourcePath = processResource.getParent().getLocation().toOSString() + File.separatorChar + "BinSource" + File.separator + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + File.separator + "Build" + File.separator;
		boolean useCLI = "true".equals(getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.USE_ARDUINOCLI))?true:false;
		if(!useCLI) {
			String inoHexFilePath = resourcePath + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + ".ino.hex";
			previousINOHexFilePath = inoHexFilePath;
			String avrDudePath = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH);
			String confFilePath = "";
			if(Platform.getOS().equals(Platform.OS_WIN32)) confFilePath = avrDudePath.replaceAll("avrdude.exe$", "");
			else confFilePath = avrDudePath.replaceAll("avrdude$", "");
			confFilePath = confFilePath + ".." + File.separator + "etc" + File.separator + "avrdude.conf";
			if(Platform.getOS().equals(Platform.OS_WIN32)) confFilePath = "\"" + confFilePath + "\"";
			cmd = avrDudePath + " -C " + confFilePath +  " -p m328p -c arduino -P " + devicePath + " -U flash:w:" + inoHexFilePath + ":i"; 
		} else {
			
			String arduinoUnoRelease = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.REVISION);
			boolean isRelease4Wifi = ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(arduinoUnoRelease);
			String fullQualifiedBoardName = "arduino:avr:uno";
			if(isRelease4Wifi) fullQualifiedBoardName = "arduino:renesas_uno:unor4wifi";
			String inoHexFilePath = resourcePath + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + ".ino.hex";
			previousINOHexFilePath = inoHexFilePath;
			String arduinoCLIPATH = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.ARDUINOCLI_PATH);
			cmd = arduinoCLIPATH + " upload -p " + devicePath + " --fqbn " + fullQualifiedBoardName + " -i " + inoHexFilePath;
		}
		forceUpload = false;
		fileWriter.write(cmd);
		fileWriter.close();
		if(!Platform.getOS().equals(Platform.OS_WIN32)) {
			// This bash file must be executable 
			java.lang.Process process = Runtime.getRuntime().exec(new String[]{"chmod 777 " + bashFilePath});
			process.waitFor();
		}
		return bashFilePath;
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
