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
package fr.univamu.ism.docometre.dacqsystems.adwin.calibration;

import de.adwin.driver.ADwinCommunicationError;
import de.adwin.driver.ADwinDevice;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.calibration.CalibrationTimerTask;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnInModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnInModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnOutModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnOutModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDigInOutChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDigInOutModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.BootDelegate;
import fr.univamu.ism.docometre.dacqsystems.adwin.LoadProcessDelegate;
import fr.univamu.ism.docometre.widgets.ChannelViewer;

public class ADWinTimerTask extends CalibrationTimerTask {
		
		private boolean terminate;
		private ChannelViewer[] channelsViewers;
		private double value;
		private ADWinDACQConfiguration adwinDacqConfiguration;

		public ADWinTimerTask(ChannelViewer[] channelsViewers) throws Exception {
			this.channelsViewers = channelsViewers;
			if(channelsViewers.length == 0) return;
			adwinDacqConfiguration = (ADWinDACQConfiguration) channelsViewers[0].getChannel().getModule().getDACQConfiguration();
			// Boot adwin if necessary
			BootDelegate.boot(adwinDacqConfiguration);
			
			// Load calibration, monitoring process
			LoadProcessDelegate.loadCalibrationProcess("1", adwinDacqConfiguration);
			
			// Start process
			adwinDacqConfiguration.getADwinDevice().Start_Process(1);
			
			// Prepare DIO configuration for ADWin Pro systems
			// For Gold systems, don't need as dig in/out are preconfigured
			String systemType = adwinDacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
			String cpuType = adwinDacqConfiguration.getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
			if(systemType.contentEquals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.I)) {
				Module[] modules = adwinDacqConfiguration.getModules();
				for (Module module : modules) {
					if(module instanceof ADWinDigInOutModule) {
						ADWinDigInOutModule adwinDigInOutModule = (ADWinDigInOutModule) module;
						int moduleNumber =  Integer.parseInt(adwinDigInOutModule.getProperty(ADWinModuleProperties.MODULE_NUMBER));
						String revision = adwinDigInOutModule.getProperty(ADWinModuleProperties.REVISION);
						revision = revision == null ? ADWinModuleProperties.REV_A : revision;
						Channel[] channels = adwinDigInOutModule.getChannels();
						int digprog = 0;
						for (Channel channel : channels) {
							if(channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT).equals(ADWinDigInOutChannelProperties.OUTPUT)) {
								int channelNumber = Integer.parseInt(channel.getProperty(ChannelProperties.CHANNEL_NUMBER));
								if(ADWinModuleProperties.REV_A.equals(revision)) {
									digprog = (int) (digprog + Math.pow(2, channelNumber - 1));
								}
								if(ADWinModuleProperties.REV_B.equals(revision)) {
									if(channelNumber <= 8) digprog = digprog | 1;
									if(channelNumber <= 16 && channelNumber >= 9) digprog = digprog | (int)Math.pow(2, 8);
									if(channelNumber <= 24 && channelNumber >= 17) digprog = digprog | (int)Math.pow(2, 16);
									if(channelNumber <= 32 && channelNumber >= 25) digprog = digprog | (int)Math.pow(2, 24);
								}
								
							}
						}
						int digprog1 = digprog & 65535;
						int digprog2 = digprog >> 16;
						adwinDacqConfiguration.getADwinDevice().Set_Par(10, digprog1);
						adwinDacqConfiguration.getADwinDevice().Set_Par(11, digprog2);
						adwinDacqConfiguration.getADwinDevice().Set_Par(15, moduleNumber);
						adwinDacqConfiguration.getADwinDevice().Set_Par(9, 1);
						do {
						} while ((adwinDacqConfiguration.getADwinDevice().Get_Par(9) != 0) && (adwinDacqConfiguration.getADwinDevice().Process_Status(1) != 0));
						// Read Digital input output states
						int initialStateValues =  adwinDacqConfiguration.getADwinDevice().Get_Par(13);
						for (ChannelViewer channelViewer : channelsViewers) {
							channelViewer.setInitialDigitalOutputStates(initialStateValues);
						}
					}
					
					if(module instanceof ADWinAnInModule) {
						ADWinAnInModule adwinAnInModule = (ADWinAnInModule) module;
						int moduleNumber =  Integer.parseInt(adwinAnInModule.getProperty(ADWinModuleProperties.MODULE_NUMBER));
						int seDiff = adwinAnInModule.getProperty(ADWinAnInModuleProperties.SINGLE_ENDED_DIFFERENTIAL).equals(ADWinAnInModuleProperties.DIFF)?1:0;
						adwinDacqConfiguration.getADwinDevice().Set_Par(15, moduleNumber);
						adwinDacqConfiguration.getADwinDevice().Set_Par(12, seDiff);
						adwinDacqConfiguration.getADwinDevice().Set_Par(16, 1);
						do {
						} while ((adwinDacqConfiguration.getADwinDevice().Get_Par(16) != 0) && (adwinDacqConfiguration.getADwinDevice().Process_Status(1) != 0));
					}
					
					if(module instanceof ADWinAnOutModule) {
						ADWinAnOutModule adwinAnOutModule = (ADWinAnOutModule) module;
						int moduleNumber =  Integer.parseInt(adwinAnOutModule.getProperty(ADWinModuleProperties.MODULE_NUMBER));
						int ampMax = Integer.parseInt(adwinAnOutModule.getProperty(ADWinAnOutModuleProperties.AMPLITUDE_MAX));
						int ampMin = Integer.parseInt(adwinAnOutModule.getProperty(ADWinAnOutModuleProperties.AMPLITUDE_MIN));
						Channel[] channels = adwinAnOutModule.getChannels();
						for (int i = 0; i < channels.length; i++) {
							Channel channel = channels[i];
							int channelNumber = Integer.parseInt(channel.getProperty(ChannelProperties.CHANNEL_NUMBER));
							adwinDacqConfiguration.getADwinDevice().Set_Par(15, moduleNumber);
							adwinDacqConfiguration.getADwinDevice().Set_Par(17, channelNumber);
							do {
							} while ((adwinDacqConfiguration.getADwinDevice().Get_Par(17) != 0) && (adwinDacqConfiguration.getADwinDevice().Process_Status(1) != 0));
							double initialValue =  adwinDacqConfiguration.getADwinDevice().Get_Par(18);
							initialValue = initialValue*(ampMax - ampMin)/65535.0 + ampMin;
							for (ChannelViewer channelViewer : channelsViewers) {
								if(channelViewer.getChannel() == channel) {
									channelViewer.setInitialAnalogState(initialValue);
									break;
								}
							}
						}
					}
				}
			}
		}

		@Override
		public void run() {
			if(!terminate) {
				try {
					for (ChannelViewer channelViewer : channelsViewers) {
						Channel channel = channelViewer.getChannel();
						if(channel.getModule() instanceof ADWinAnInModule) {
							// Get new value from ADwin analog input
							value = ADWinRWDelegate.getAnalogValue(channel);
						} else
						if(channel.getModule() instanceof ADWinAnOutModule) {
							// Get new analog value
							value = (double)channelViewer.getValue();
							// Push to ADwin
							ADWinRWDelegate.putAnalogValue(channel, value);
						} else
						if(channel.getModule() instanceof ADWinDigInOutModule) {
							// Digital
							if(channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT).equals(ADWinDigInOutChannelProperties.INPUT)) {
								// Get new value from ADwin digital input
								value = ADWinRWDelegate.getDigitalValue(channel);
							} else {
	 							// Get new digital value
		 						value = (boolean)channelViewer.getValue() == true ? 1.0:0.0;
								// Push to ADwin
								ADWinRWDelegate.putDigitalValue(channel, (int)value);
							}
						}
						if(channelViewer != null && !channelViewer.isDisposed() && !terminate)
							channelViewer.getDisplay().syncExec(new Runnable() {
								@Override
								public void run() {
									channelViewer.addSample(System.currentTimeMillis()/1000.0, (double) value);
								}
							});
						
						
						Thread.sleep(1, 0);
						
					}
				} catch (Exception e) {
					Activator.logErrorMessageWithCause(e);
					terminate = true;
					e.printStackTrace();
				}
					
			} else {
				// Terminate calibration, monitoring process
				try {
					ADwinDevice adwinDevice = adwinDacqConfiguration.getADwinDevice();
					adwinDevice.Stop_Process(1);
				} catch (ADwinCommunicationError e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				} finally {
					cancel();
				}
			}
		}

		public void terminate() {
			terminate = true;
		}
		
	}
