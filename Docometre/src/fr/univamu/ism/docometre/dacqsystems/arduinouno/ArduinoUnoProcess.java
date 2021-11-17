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
			while(!terminate) {
				try {
					
					if(monitor.isCanceled()) {
						forceTermination = true;
						monitor.setCanceled(false);
					}
					
	    			if(serialPort.getInputBufferBytesCount() > 0) {
	    				byte[] bytes = serialPort.readBytes();
		            	for (byte b: bytes) {
		                    if ( (b == '\r' || b == '\n') && message.length() > 0 && !terminate) {
		                    	String messageString = message.toString().replaceAll("\\r$", "").replaceAll("\\n", "");
//		                    	System.out.println(messageString);
		                    	message.setLength(0);
		                    	String[] segments = messageString.split(":");
		                    	
		                    	if(segments.length != 3) continue;
		                    	try {
									trsfrNum = Integer.parseInt(segments[0]);
									time = Float.parseFloat(segments[1])/1000000f; 
									value = Float.parseFloat(segments[2]);
								} catch (Exception e) {
									Activator.logErrorMessageWithCause(e);
									e.printStackTrace();
									appendToEventDiary("ERROR at " + time + " : " + e.getMessage());
									continue;
								} 
	                    		
		                    	if(trsfrNum == 0) {

		                    		timeAfter = System.currentTimeMillis()/1000d;
		                    		if(timeAfter - timeBefore > 1) {
		                    			String name;
	                    				int nb ;
		                    			for (int n = 0; n < transferedChannelsOrderedByTransferNumber.length; n++) {
		                    				name = transferedChannelsOrderedByTransferNumber[n].getProperty(ChannelProperties.NAME);
		                    				nb = nbSamples[n];
		                    				appendToEventDiary(name + " : " + nb);
										}
			                    		timeBefore = timeAfter;
		                    		}
		                    		
		                    		realTime = time;
		                    		workload = value;
			    					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
										@Override
			    						public void run() {
//			    							StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
//			    							statusLineManger.setMessage(NLS.bind(DocometreMessages.Workload_Time, workload, formater.format(realTime)));
			    							ApplicationActionBarAdvisor.workloadTimeContributionItem.setText(NLS.bind(DocometreMessages.Workload_Time, workload, formater.format(realTime)));
			    						}
			    					});
		                    	} else {
		                    		try {
//			                    		timeBefore = System.currentTimeMillis()/1000d;
										transferedChannelsOrderedByTransferNumber[trsfrNum - 1].addSamples(new float[] {time, value});
//		                    			channelName = transferedChannelsOrderedByTransferNumber[trsfrNum - 1].getProperty(ChannelProperties.NAME);
										nbSamples[trsfrNum - 1] +=1;
										transferedChannelsOrderedByTransferNumber[trsfrNum - 1].notifyChannelObservers();
//			                    		timeAfter = System.currentTimeMillis()/1000d;
//			        					appendToEventDiary("Display time : " + (timeAfter - timeBefore));
//			                    		appendToEventDiary("New sample for channel " + channelName + "  - dt : " + formater.format(time) + "  - Total " + nbSamples[trsfrNum - 1] + " samples");
									} catch (Exception e) {
										Activator.logErrorMessageWithCause(e);
										e.printStackTrace();
										appendToEventDiary("ERROR at " + time + " : "+ e.getMessage());
										continue;
									}
		                    	}
		                    } else {
		                    	if(b == 's') {
		                    		System.out.println("s recieived");
		                    		appendToEventDiary("Received stop 's' char");
		                    		terminate = true;
		                    		forceTermination = true;// Just to send s in reply as Arduino is waiting for it.
		                    		Thread.sleep(30);// Wait 30ms to be sure Arduino is waiting for 's' char.
		                    	}
		                    	else message.append((char)b);
		                    }
		            	}
//		            	System.out.println("*****");
	    			}
	    			if(forceTermination) {
	    				serialPort.writeByte((byte) 's');
	    				forceTermination = false;
	    			}
	            }
	            catch (Exception e1) {
	            	Activator.logErrorMessageWithCause(e1);
					double processEndTime = System.currentTimeMillis()/1000d;
					appendToEventDiary("\nEnd time (s) : " + Double.toString(processEndTime));
					appendToEventDiary(ObjectsController.getResourceForObject(ArduinoUnoProcess.this).getName() + " duration (s) is about : " + Double.toString(processEndTime - processBeginTime) + "\n");
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
//							StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
//							statusLineManger.setMessage(NLS.bind(DocometreMessages.Workload_Time, "0", formater.format(time)));
							ApplicationActionBarAdvisor.workloadTimeContributionItem.setText(NLS.bind(DocometreMessages.Workload_Time, "0", formater.format(realTime)));
						}
					});
					try {
						serialPort.closePort();
					} catch (SerialPortException e) {
						Activator.logErrorMessageWithCause(e);
					}
					appendToEventDiary("Error in receiving string from port: " + e1.getMessage());
					appendErrorMarkerAtCurrentDiaryLine("Error in receiving string from port: " + e1.getMessage());
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							StatusLineManager statusLineManger = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getStatusLineManager();
							statusLineManger.setErrorMessage(Activator.getImage(IImageKeys.ERROR_ANNOTATION_ICON), "Error in receiving string from port");
						}
					});
	                return new Status(Status.ERROR, Activator.PLUGIN_ID, "Error in receiving string from port: " + e1.getMessage());
				}
			}
			try {
				ArduinoUnoProcess.this.close();
				serialPort.closePort();
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
			} catch (Exception e2) {
                return new Status(Status.ERROR, Activator.PLUGIN_ID, "Error in receiving string from port: " + e2.getMessage());
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
		
		int delay = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.ARDUINO_DELAY_TIME_AFTER_SERIAL_PRINT);
		
		if(segment == ArduinoUnoCodeSegmentProperties.INCLUDE) {
			code = "// Include for watch dog timer\n";
			code = code + "#include  <avr/wdt.h>\n";
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.INCLUDE);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.INCLUDE);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.INCLUDE) + "\n";
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.DEFINE) {
			code = code + "// Defines for setting and clearing register bits\n";
			code = code + "// and configure sample clock frequency\n";
			code = code + "#ifndef cbi\n";
			code = code + "\t\t#define cbi(sfr, bit) (_SFR_BYTE(sfr) &= ~_BV(bit))\n";
			code = code + "#endif\n";
			code = code + "#ifndef sbi\n";
			code = code + "\t\t#define sbi(sfr, bit) (_SFR_BYTE(sfr) |= _BV(bit))\n";
			code = code + "#endif\n\n";
			code = code + "// Flags to convert double in eng. format string\n";
			code = code + "#define DTOSTR_ALWAYS_SIGN 0x01\n";
			code = code + "#define DTOSTR_PLUS_SIGN 0x02\n";
			code = code + "#define DTOSTR_UPPERCASE 0x04\n";
			code = code + "#define pi PI\n";
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.DEFINE);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.DEFINE);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.DEFINE) + "\n";
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.DECLARATION) {
			String gf = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY);
			code = code + "// Loop frequency in Hz\n";
			code = code + "const unsigned long loopFrequency = " + gf + ";\n\n";
			code = code + "// Loop period in microsecond\n";
			code = code + "const unsigned long loopPeriod = (1000000.0/(1.0*loopFrequency));\n\n";
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
			code = code + "// Loop start time\n";
			code = code + "unsigned long startTime;\n\n";
			code = code + "// Variables to run process periodically\n";
			code = code + "long previousLoopTime;\n";
			code = code + "unsigned long currentLoopTime;\n\n";
			code = code + "// Loop time in microsecond\n";
			code = code + "unsigned long loopTime_MS;\n";
			code = code + "// Loop time in second\n";
			code = code + "double time;\n\n";
			code = code + "// currentLoopTime - previousLoopTime\n";
			code = code + "unsigned long delta;\n";
			code = code + "// Time shift because of micros()\n";
			code = code + "unsigned long deviation;\n";
			code = code + "// Loop index to compute time\n";
			code = code + "unsigned long timeIndex;\n\n";
			code = code + "// Start loop when true\n";
			code = code + "bool startLoop = false;\n\n";
			code = code + "// Stop loop when true\n";
			code = code + "bool terminateProcess = false;\n\n";
			code = code + "// First loop flag\n";
			code = code + "bool firstLoop = true;\n\n";
			code = code + "//String serialMessage\n";
			code = code + "char serialMessage[64];\n\n";
			
			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.DECLARATION);
			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.DECLARATION);
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.DECLARATION);
			
		}
		
		if(segment == ArduinoUnoCodeSegmentProperties.INITIALIZATION) {
			code = "void setup() {\n";
			code = code + "\t\t// Set prescale to 32 for ADC to decrease conversion time\n";
			code = code + "\t\t// 16MHz/32 = 500kHz - Max Sample freq = 500kHz/13 # 38kHz\n";
			code = code + "\t\t// Convertion time # 30us\n";
			code = code + "\t\tsbi(ADCSRA,ADPS2);\n";
			code = code + "\t\tcbi(ADCSRA,ADPS1);\n";
			code = code + "\t\tsbi(ADCSRA,ADPS0);\n";
			String baudRate = getDACQConfiguration().getProperty(ArduinoUnoDACQConfigurationProperties.BAUD_RATE);
			code = code + "\t\tSerial.begin(" + baudRate + ");\n";
			code = code + "\t\twhile (!startLoop) {\n";
			code = code + "\t\t\t\tif(Serial.available()) {\n";
			code = code + "\t\t\t\t\t\tstartLoop = ((char)Serial.read()) == 'r';\n";
			code = code + "\t\t\t\t}\n";
			code = code + "\t\t}\n";
//			code = code + "\t\tstartTime =  micros();\n";
//			code = code + "\t\tpreviousLoopTime = startTime - loopPeriod;\n\n";
			
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
			
			code = "}\n\nvoid loop() {\n";
			code = code + "\t\twhile(true) {\n";
			
			code = code + "\t\t\t\t// If we receive 's'(top) from serial port,\n";
			code = code + "\t\t\t\t// then force process termination\n";
			code = code + "\t\t\t\tif(Serial.available()) {\n";
			code = code + "\t\t\t\t\t\tstartLoop = ((char)Serial.read()) != 's';\n";
			code = code + "\t\t\t\t\t\tif(!startLoop ) terminateProcess = true;\n";
			code = code + "\t\t\t\t}\n";
			
			code = code + "\t\t\t\tcurrentLoopTime = micros();\n";
			code = code + "\t\t\t\tif(firstLoop) {\n";
			code = code + "\t\t\t\t\t// Just to be sure start time is zero\n";
			code = code + "\t\t\t\t\t// (See micro() function doc. 4us or 8us shift)\n";
			code = code + "\t\t\t\t\tstartTime = currentLoopTime;\n";
			code = code + "\t\t\t\t\tpreviousLoopTime = startTime - loopPeriod;\n";
			code = code + "\t\t\t\t\tfirstLoop = false;\n";
			code = code + "\t\t\t\t}\n";
			
			code = code + "\t\t\t\tdelta = currentLoopTime - previousLoopTime;\n";
			code = code + "\t\t\t\tif(delta >= loopPeriod - deviation) {\n";
			code = code + "\t\t\t\t\t\tdeviation = delta - loopPeriod;\n";
			code = code + "\t\t\t\t\t\t// A way to overcompensate this time deviation : not used by default\n";
			code = code + "\t\t\t\t\t\t//deviation = deviation > 0 ? deviation + 4 : deviation; // Overcompensate right time drift\n";
			code = code + "\t\t\t\t\t\tloopTime_MS = currentLoopTime - startTime;\n";
			code = code + "\t\t\t\t\t\ttime = timeIndex*((double)loopPeriod/1000000.0);\n";
			code = code + "\t\t\t\t\t\ttimeIndex++;\n";
			
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
			code = code + "\t\t\t\t\t\tpreviousLoopTime = micros() - currentLoopTime;\n";
			code = code + "\t\t\t\t\t\tworkload = computeWorkload(previousLoopTime);\n";
			code = code + "\t\t\t\t\t\tsprintf(serialMessage, \"%d:%lu:%d\", 0, loopTime_MS, workload);\n";
			code = code + "\t\t\t\t\t\tSerial.println(serialMessage);\n";
			if(delay > 0) code = code + "\t\t\t\t\t\tdelayMicroseconds(" + delay + ");\n";
			code = code + "\t\t\t\t\t\tpreviousLoopTime = currentLoopTime;\n";
			code = code + "\t\t\t\t}\n";
			
			code = code + "\t\t\t\tif(terminateProcess) {\n";
			code = code + "\t\t\t\t\t\tfinalize();\n";
			code = code + "\t\t\t\t}\n";
			
			code = code + "\t\t}\n";
			code = code + "}\n\n";
			
			code = code + "void finalize() {\n";
			
			
//			code = code + getCurrentProcess().getScript().getInitializeCode(this, ArduinoUnoCodeSegmentProperties.FINALIZATION);
//			code = code + getCurrentProcess().getScript().getLoopCode(this, ArduinoUnoCodeSegmentProperties.FINALIZATION);
//			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ArduinoUnoCodeSegmentProperties.FINALIZATION);
			

			code = code + "\t\t// ******** Début algorithme finalisation\n\n";
			code = code + getCurrentProcess().getScript().getFinalizeCode(this, ScriptSegmentType.FINALIZE);
			code = code + "\n\t\t// ******** Fin algorithme finalisation\n\n";
			
			code = code + "\t\tSerial.println('s');\n";
			code = code + "\t\tdelay(20);// Just to be sure stop char has been sent\n";
			code = code + "\t\t// Now wait to receive 's' char before board restart\n";
			code = code + "\t\twhile (startLoop) {\n";
			code = code + "\t\t\t\tif(Serial.available()) {\n";
			code = code + "\t\t\t\t\t\tstartLoop = ((char)Serial.read()) != 's';\n";
			code = code + "\t\t\t\t}\n";
			code = code + "\t\t}\n";
			
			
			code = code + "\t\twdt_enable(WDTO_15MS);\n";
			code = code + "\t\twhile(true) {\n";
			code = code + "\t\t\t\t// Wait for board to restart\n";
			code = code + "\t\t}\n";
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
			code = code + "unsigned int acquireAnalogInput(byte inputNumber, unsigned long *lastAcquisitionTime, bool transfert, byte transferNumber) {\n";
			//code = code + "\t\t//unsigned long dt = micros();\n";
			code = code + "\t\tint value = analogRead(inputNumber);\n";
			//code = code + "\t\t//dt = micros() - dt;\n";
			//code = code + "\t\t//Serial.println(dt);\n";
			//code = code + "\t\t//serialMessage = String(inputNumber) + \":\" + String(loopTime - *lastAcquisitionTime) + \":\" + String(value);\n";
			code = code + "\t\tif(transfert) {\n";
			code = code + "\t\t\t\tsprintf(serialMessage, \"%d:%lu:%d\", transferNumber, (loopTime_MS - *lastAcquisitionTime), value);\n";
			code = code + "\t\t\t\tSerial.println(serialMessage);\n";
			if(delay > 0)code = code + "\t\t\t\tdelayMicroseconds(" + delay + ");\n";
			code = code + "\t\t}\n";
			code = code + "\t\t*lastAcquisitionTime = loopTime_MS;\n";
			code = code + "\t\treturn value;\n";
			code = code + "}\n";
			
			if(((ArduinoUnoDACQConfiguration)getDACQConfiguration()).hasADS1115Module()) {
				code = code + "unsigned int acquireADS1115AnalogInput(byte inputNumber, unsigned long *lastAcquisitionTime, bool transfert, byte transferNumber, ADS1115 ads1115, int gain) {\n";
				//code = code + "\t\t//unsigned long dt = micros();\n";
				code = code + "\t\tads1115.setGain(gain);\n";
				code = code + "\t\tint value = ads1115.readADC(inputNumber);\n";
				//code = code + "\t\t//dt = micros() - dt;\n";
				//code = code + "\t\t//Serial.println(dt);\n";
				//code = code + "\t\t//serialMessage = String(inputNumber) + \":\" + String(loopTime - *lastAcquisitionTime) + \":\" + String(value);\n";
				code = code + "\t\tif(transfert) {\n";
				code = code + "\t\t\t\tsprintf(serialMessage, \"%d:%lu:%d\", transferNumber, (loopTime_MS - *lastAcquisitionTime), value);\n";
				code = code + "\t\t\t\tSerial.println(serialMessage);\n";
				if(delay > 0)code = code + "\t\t\t\tdelayMicroseconds(" + delay + ");\n";
				code = code + "\t\t}\n";
				code = code + "\t\t*lastAcquisitionTime = loopTime_MS;\n";
				code = code + "\t\treturn value;\n";
				code = code + "}\n";
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
			}
			if(segments[i].equals(ArduinoUnoCodeSegmentProperties.TRANSFER)) {
				// Variables transfer
				code = code + VariablesCodeGenerationDelegate.getCode(this, ArduinoUnoCodeSegmentProperties.TRANSFER);
			}
			
			if (segments[i] == ArduinoUnoCodeSegmentProperties.LOOP){
				code = code + "\t\t\t\t\t\t// ******** Début algorithme boucle\n\n";
				code = code + getCurrentProcess().getScript().getLoopCode(this, ScriptSegmentType.LOOP);
				code = code + "\n\t\t\t\t\t\t// ******** Fin algorithme boucle\n\n";
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

	@Override
	public void compile(IProgressMonitor progressMonitor) throws Exception {
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
	    
	    
	    java.lang.Process process = Runtime.getRuntime().exec(cmdLine);
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
//		    System.out.println("Input : " + line);
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
					((ProcessEditor)editorPart).pageChanged(null);
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
							marker.setAttribute(IMarker.LINE_NUMBER, Integer.parseInt(localLines[localLines.length - 4]));
							marker.setAttribute(IMarker.SEVERITY, severity);
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
		cmd = cmd + "\"" +arduinoUnoCompiler + "\" -hardware=\"" + rootPath + "hardware\" -tools=\"" + rootPath + "hardware\\tools\" -tools=\"" + rootPath + "tools-builder\" -fqbn=arduino:avr:uno -quiet -verbose";
		cmd = cmd + " -built-in-libraries " + rootPath + "libraries";
		cmd = cmd + " -build-path=" + outputFolder + File.separator + "Build";
		cmd = cmd + " -compile " + sketchFilePath;// + " > stdout.txt 2>stderr.txt";
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
		cmd = cmd + arduinoUnoCompiler + " -hardware=" + rootPath + "hardware/ -tools=" + rootPath + "hardware/tools/ -tools=" + rootPath + "tools-builder/ -fqbn=arduino:avr:uno -quiet -verbose";
		cmd = cmd + " -built-in-libraries " + rootPath + "libraries";
		cmd = cmd + " -build-path=" + outputFolder + File.separator + "Build";
		cmd = cmd + " -compile " + sketchFilePath;// + " > stdout.txt 2>stderr.txt";
		fileWriter.write(cmd);
		fileWriter.close();
		// This bash file must be executable 
		java.lang.Process process = Runtime.getRuntime().exec("chmod 777 " + bashFilePath);
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
		cmd = cmd + arduinoUnoCompiler + " -hardware=" + rootPath + "hardware/ -tools=" + rootPath + "hardware/tools/ -tools=" + rootPath + "tools-builder/ -fqbn=arduino:avr:uno -quiet -verbose";
		cmd = cmd + " -built-in-libraries " + rootPath + "libraries";
		cmd = cmd + " -build-path=" + outputFolder + File.separator + "Build";
		cmd = cmd + " -compile " + sketchFilePath;// + " > stdout.txt 2>stderr.txt";
		fileWriter.write(cmd);
		fileWriter.close();
		// This bash file must be executable 
		java.lang.Process process = Runtime.getRuntime().exec("chmod 777 " + bashFilePath);
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
		if(loadProcess()) {
			// Create Job
			realTimeLoopJob = new RealTimeLoopJob(DocometreMessages.RunningProcess);
			// Open modules and variables
			open(this, prefix, suffix);
			generation();
			return realTimeLoopJob;
		} else throw new Exception("Impossible to load process.");
		
	}

	private boolean loadProcess() {
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
		
		// Load Process with AVRDUDE
		// Get AVRDude path
		IResource processResource = ObjectsController.getResourceForObject(this);
		String resourcePath = processResource.getParent().getLocation().toOSString() + File.separatorChar + "BinSource" + File.separator + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + File.separator + "Build" + File.separator;
		String inoHexFilePath = resourcePath + processResource.getName().replaceAll(Activator.processFileExtension + "$", "") + ".ino.hex";
		if(previousINOHexFilePath.equals(inoHexFilePath) && !forceUpload) {
			appendToEventDiary("Process " + processResource.getFullPath() + " was already loaded\n" );
			return true;
		}
		
		forceUpload = false;
		previousINOHexFilePath = inoHexFilePath;
		
		String avrDudePath = dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.AVRDUDE_PATH);
		String confFilePath = "";
		if(Platform.getOS().equals(Platform.OS_WIN32)) confFilePath = avrDudePath.replaceAll("avrdude.exe$", "");
		else confFilePath = avrDudePath.replaceAll("avrdude$", "");
		confFilePath = confFilePath + ".." + File.separator + "etc" + File.separator + "avrdude.conf";
		if(Platform.getOS().equals(Platform.OS_WIN32)) confFilePath = "\"" + confFilePath + "\"";
		
		String cmd = avrDudePath + " -C " + confFilePath +  " -p m328p -c arduino -P " + devicePath + " -U flash:w:" + inoHexFilePath + ":i"; 
		
		try {
			java.lang.Process processCMD = Runtime.getRuntime().exec(cmd);
			processCMD.waitFor();
			String line;
			BufferedReader error = new BufferedReader(new InputStreamReader(processCMD.getErrorStream()));
			String message = "";
			while((line = error.readLine()) != null){
				message = message + line + "\n";
			}
			error.close();
			if(!message.equals("")) Activator.logErrorMessage(message);
			if(message.contains("error")) return false;
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
