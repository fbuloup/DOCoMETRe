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
package fr.univamu.ism.docometre.dacqsystems.adwin;

import java.io.File;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public class ADWinDigInOutModule extends Module {

	public static final long serialVersionUID = DACQConfiguration.serialVersionUID;
	
	private boolean includeSegmentPassedFor_DIGIN;
	private boolean includeSegmentPassedFor_DIGOUT;

	public ADWinDigInOutModule(DACQConfiguration daqGeneralConfiguration) {
		super(daqGeneralConfiguration);
		ADWinDigInOutModuleProperties.populateProperties(this);
	}

	public String getCodeSegment(Object segment) throws Exception{
		String code = "";
		
		if (segment==ADWinCodeSegmentProperties.INITIALIZATION){
			
			if(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE).equals(ADWinDACQConfigurationProperties.GOLD)){
				int confDioNumber=0;
				for (int i = 0; i < getChannelsNumber(); i++) {
					Channel channel = getChannel(i);
					
					String inOut=channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
					String channelNumber=channel.getProperty(ChannelProperties.CHANNEL_NUMBER);
					boolean isInput=inOut.equals(ADWinDigInOutChannelProperties.INPUT);
					int channelN=Integer.parseInt(channelNumber) - 1;	
					if(!isInput){
						if (channelN<=7) confDioNumber = confDioNumber | 1;
						if (8<=channelN && channelN<=15) confDioNumber = confDioNumber | 2;						
						if (16<=channelN && channelN<=23) confDioNumber = confDioNumber | 4;
						if (24<=channelN && channelN<=31) confDioNumber = confDioNumber | 8;
					}
				}
				code = "\nREM ******** Configuration Entrees/Sorties numeriques\n";
				code = code + "CONF_DIO(" + confDioNumber + ")\n";
			}
			
			if(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE).equals(ADWinDACQConfigurationProperties.PRO)){
				if(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.CPU_TYPE).equals(ADWinDACQConfigurationProperties.I)){
					
					String revision = getProperty(ADWinModuleProperties.REVISION);
					revision = revision == null ? ADWinModuleProperties.REV_A : revision;
					int digProg = 0;
					for (int i = 0; i < getChannelsNumber(); i++) {
						Channel channel = getChannel(i);
						
						String inOut=channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
						String channelNumber=channel.getProperty(ChannelProperties.CHANNEL_NUMBER);
						boolean isInput=inOut.equals(ADWinDigInOutChannelProperties.INPUT);
						int channelN=Integer.parseInt(channelNumber) - 1;	
						if(!isInput){
							
							if(ADWinModuleProperties.REV_A.equals(revision)) {
								digProg = (int) (digProg + Math.pow(2, channelN - 1));
							}
							if(ADWinModuleProperties.REV_B.equals(revision)) {
								if (channelN<=7) digProg = digProg | 1;
								if (8<=channelN && channelN<=15) digProg = digProg | (int)Math.pow(2, 8);						
								if (16<=channelN && channelN<=23) digProg = digProg | (int)Math.pow(2, 16);
								if (24<=channelN && channelN<=31) digProg = digProg | (int)Math.pow(2, 24);
							}
						}
					}
					int digProg1 = digProg & 65535;
					int digProg2 = digProg >> 16;
					code = "\nREM ******** Configuration Entrees/Sorties numeriques\n";
					code = code + "DIGPROG1(" + getProperty(ADWinModuleProperties.MODULE_NUMBER) + "," + digProg1 + ")\n";
					code = code + "DIGPROG2(" + getProperty(ADWinModuleProperties.MODULE_NUMBER) + "," + digProg2 + ")\n";
				}
				
				if(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.CPU_TYPE).equals(ADWinDACQConfigurationProperties.II)){
					int digProgNumber=0;
					for (int i = 0; i < getChannelsNumber(); i++) {
						Channel channel = getChannel(i);
						
						String inOut=channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
						String channelNumber=channel.getProperty(ChannelProperties.CHANNEL_NUMBER);
						boolean isInput=inOut.equals(ADWinDigInOutChannelProperties.INPUT);
						int channelN=Integer.parseInt(channelNumber) - 1;	
						if(!isInput){
							if (channelN<=7) digProgNumber = digProgNumber | 1;
							if (8<=channelN && channelN<=15) digProgNumber = digProgNumber | 2;						
							if (16<=channelN && channelN<=23) digProgNumber = digProgNumber | 4;
							if (24<=channelN && channelN<=31) digProgNumber = digProgNumber | 8;
						}
					}
					code = "\nREM ******** Configuration Entrees/Sorties numeriques\n";
					code = code + "P2_DIGPROG(" + getProperty(ADWinModuleProperties.MODULE_NUMBER) + "," + digProgNumber + ")\n";
				}
			}
		}
		
			
		for (int i = 0; i < getChannelsNumber(); i++) {
			Channel channel = getChannel(i);
			
			String name = channel.getProperty(ChannelProperties.NAME);
			String inOut=channel.getProperty(ADWinDigInOutChannelProperties.IN_OUT);
			String transferNumber=channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
			String bufferSize=channel.getProperty(ChannelProperties.BUFFER_SIZE);
			String channelNumber=channel.getProperty(ChannelProperties.CHANNEL_NUMBER);
			String stimuli=channel.getProperty(ADWinDigInOutChannelProperties.STIMULUS);
			boolean isStimuli=Boolean.valueOf(stimuli);
			String transfer=channel.getProperty(ChannelProperties.TRANSFER);
			boolean isTransfered=Boolean.valueOf(transfer);
			boolean isInput=inOut.equals(ADWinDigInOutChannelProperties.INPUT);
			String gsf = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
			float gsfFloat = Float.parseFloat(gsf);	
			String sfChannel=channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			float sfFloat = Float.parseFloat(sfChannel);
			int frequencyRatio = (int) (gsfFloat/sfFloat);
		
			if(isStimuli && isInput) {
				StringBuffer message = new StringBuffer();
				message.append("You have configured a stimulus as input : it's impossible !");
				message.append(" Module : DigInOut - ");
				message.append("Channel name : " + channel.getProperty(ChannelProperties.NAME));
				throw new Exception(message.toString());
			}

			if (segment==ADWinCodeSegmentProperties.INCLUDE && (!includeSegmentPassedFor_DIGIN || !includeSegmentPassedFor_DIGOUT)){
				String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
				String cpuType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
				String suffix = "";
				if(!includeSegmentPassedFor_DIGIN && !includeSegmentPassedFor_DIGOUT)
					if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.I)) code = code + "#INCLUDE ADwpDIO.INC\n";
				if(systemType.equals(ADWinDACQConfigurationProperties.GOLD) && cpuType.equals(ADWinDACQConfigurationProperties.II)) suffix = "II";
				if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.II)) suffix = "II";
				if(isInput && !includeSegmentPassedFor_DIGIN){
					includeSegmentPassedFor_DIGIN = true;
					String temp = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH) + File.separator;
					temp = temp + "CALLDIGIN" + dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE) + suffix + ".INC\n";
					temp =	ADWinProcess.processPathForMacOSX(temp);
					code = code + "#INCLUDE " + temp;
				}
				else if(!isInput && !includeSegmentPassedFor_DIGOUT){
					includeSegmentPassedFor_DIGOUT = true;
					String temp = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH) + File.separator;
					temp = temp + "CALLDIGOUT" + dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE) + suffix + ".INC\n";
					temp =	ADWinProcess.processPathForMacOSX(temp);
					code = code + "#INCLUDE " + temp;
				}
			}	
			
			if (segment==ADWinCodeSegmentProperties.DECLARATION){
				if(isInput){
					code = code + "\nREM ******** Entree numerique: " + name + "\n";
					code = code + "DIM Acquisition_" + name + " AS LONG\n";
					code = code + "DIM " + name + " AS LONG\n";
				}
				else{
					code = code + "\nREM ******** Sortie numerique: " + name + "\n";
					code = code + "DIM Generation_" + name + " AS LONG\n";
					code = code + "DIM " + name + " AS LONG\n";
				}
				if(isTransfered){
					code = code + "\nREM ******** Variables pour transfert de: " + name + " \'Utilisé par Transfert\n";
					code = code + "#DEFINE " + name + "_TAB DATA_" + transferNumber + " \'Utilisé par Transfert\n";
					code = code + "DIM " + name + "_TAB[" + bufferSize + "] AS FLOAT AS FIFO \'Utilisé par Transfert\n";
					code = code + "DIM TRANSFERT_" + name + " AS LONG \'Utilisé par Transfert\n";
				}	
			}	
					
			if (segment==ADWinCodeSegmentProperties.INITIALIZATION){
				if(isInput){
					code = code + "\nAcquisition_" + name + " = " + frequencyRatio + " \'******** init acquisition " + name + "\n";
					if(isTransfered) {
						code = code + "TRANSFERT_" + name + " = " + frequencyRatio + "\n";
						code = code + "FIFO_CLEAR(" + transferNumber + ")\n";
						code = code + "PAR_" + transferNumber + " = 0\n";
					}
				}
				else{
					code = code + "\nGeneration_" + name + " = " + frequencyRatio + "\'******** init generation " + name + "\n";
					if (!isStimuli && isTransfered){
						code = code + "TRANSFERT_" + name + " = " + frequencyRatio + "\n";
						code = code + "FIFO_CLEAR(" + transferNumber + ")\n";
					}
					if(isStimuli) {
						code = code + "TRANSFERT_" + name + " = " + frequencyRatio + "\n";
					}
				}
			}
			
			if (segment==ADWinCodeSegmentProperties.ACQUISITION){
				if(isInput){
					String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
					String moduleNumber = getProperty(ADWinModuleProperties.MODULE_NUMBER);
					code = code + "\nIF (Acquisition_" + name + " = " + frequencyRatio + ") THEN\n";
					code = code + "\tAcquisition_" + name + " = 0\n"; 
					if(systemType.equals(ADWinDACQConfigurationProperties.GOLD)) code = code + "\t" + name + " = Call_DigIn(" + channelNumber + ")\n";
					if(systemType.equals(ADWinDACQConfigurationProperties.PRO)) code = code + "\t" + name + " = Call_DigIn(" + moduleNumber + ", " + channelNumber + ")\n";
					code = code + "ENDIF\n";
					code = code + "INC(Acquisition_" + name + ")\n";
				}
			}
					
			if (segment==ADWinCodeSegmentProperties.RECOVERY){
				if (isStimuli && !isInput){
//					code = code + "\nIF (TRANSFERT_" + name + " = " + frequencyRatio + ") THEN\n";
//					code = code + "\tTRANSFERT_" + name + " = 0\n";
//					code = code + "\tIF (FIFO_FULL(" +  transferNumber + ") = 0) THEN\n";
//					code = code + "\t\tPAR_" +  transferNumber + " = PAR_" +  transferNumber + " + 1\n";
//					code = code + "\t\tFIFO_CLEAR(" +  transferNumber + ")\n";
//					code = code + "\tELSE\n";
//					code = code + "\t\t" + name + " = " + name + "_TAB\n";
//					code = code + "\tENDIF\n";
//					code = code + "ENDIF\n";
//					code = code + "INC(TRANSFERT_" + name + ")\n";
				}	
			}
			
			if (segment==ADWinCodeSegmentProperties.TRANSFER){
				if (!isStimuli && isTransfered){
					code = code + "\nIF (TRANSFERT_" + name + " = " + frequencyRatio + ") THEN\n";
					code = code + "\tTRANSFERT_" + name + " = 0\n";
					code = code + "\tIF (FIFO_EMPTY(" + transferNumber + ") = 0) THEN\n";
					code = code + "\t\tPAR_" +  transferNumber + " = PAR_" +  transferNumber + " + 1\n";
					code = code + "\t\tFIFO_CLEAR(" +  transferNumber + ")\n";
					code = code + "\tENDIF\n";
					code = code + "\t" + name + "_TAB = " + name + "\n";
					code = code + "ENDIF\n";
					code = code + "INC(TRANSFERT_" + name + ")\n";
				}
			}
		
			if (segment==ADWinCodeSegmentProperties.GENERATION){
				if(!isInput){
					String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
					String moduleNumber = getProperty(ADWinModuleProperties.MODULE_NUMBER);
					code = code + "\nIF (Generation_" + name + " = " + frequencyRatio + ") THEN\n";
					code = code + "\tGeneration_" + name + " = 0\n";
					if(systemType.equals(ADWinDACQConfigurationProperties.GOLD)) code = code + "\tCall_DigOut(" + channelNumber + ", " + name + ")\n";
					if(systemType.equals(ADWinDACQConfigurationProperties.PRO)) code = code + "\tCall_DigOut(" + moduleNumber + ", " + channelNumber + ", " + name + ")\n";
					code = code + "ENDIF\n";
					code = code + "INC(Generation_" + name + ")\n";
				}
			}
			
			if (segment == ADWinCodeSegmentProperties.FINISH){
				
			}
		}		
		return code;
	}

	public void update(Property property, Object newValue,  Object oldValue, AbstractElement element) {
		if(property == ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY){
			ADWinDACQConfiguration.changeChannelsFrequencies((String)newValue, (String)oldValue, getChannels());
			notifyObservers(ChannelProperties.SAMPLE_FREQUENCY, newValue, oldValue);
		}
	}

	@Override
	public Channel initializeChannelProperties() {
		Channel channel = new ADWinChannel(this);
		ADWinDigInOutChannelProperties.populateProperties(channel);
		if(dacqConfiguration != null) {
			String property = channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			property = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
			channel.setProperty(ChannelProperties.SAMPLE_FREQUENCY, property);
		}
		return channel;
	}

	public void recovery() {
		/*
		ADwinDevice adwinDevice = ((ADWinProcess)getProcess()).adwinDevice;
		
		String processNum = getProcess().getPropertyValue(ADWinProcessProperties.PROCESS_NUMBER);
		int processNumInt = Integer.parseInt(processNum);	
		
		for (int j = 0; j < getChannelsNumber(); j++) {
			Channel channel = getChannel(j);
			if((channel.getPropertyValue(ADWinDigInOutChannelProperties.TRANSFER) == "true") && (channel.getPropertyValue(ADWinDigInOutChannelProperties.STIMULUS) == "false")){
				String transferNum = channel.getPropertyValue(ADWinDigInOutChannelProperties.TRANSFER_NUMBER);
				int transferNumInt = Integer.parseInt(transferNum);	
				try {
					int nbData = adwinDevice.Fifo_Full(transferNumInt);
					if (nbData > 0){
						float[] signal = ((ADWinProcess)getProcess()).getData(transferNumInt, nbData);
						channel.addSamples(signal);
						((ADWinProcess)getProcess()).appendToDiary("Recup Num Data: " + transferNum + " - Tampon: " + nbData + "\n");
						if(adwinDevice.Get_Par(transferNumInt) > 0){
							float real_time = adwinDevice.Get_FPar(processNumInt);
							String realTime=String.valueOf(real_time);
							((ADWinProcess)getProcess()).appendToDiary("Perte de donn???es Recup sur Num Data: " + transferNum + " ??? " + realTime + "\n");
							adwinDevice.Set_Par(transferNumInt,0);
						}
					}
				} catch (ADwinCommunicationError e) {
					e.printStackTrace();
				}
			}
		}	*/
		RecoveryDelegate.recover(null, this, (ADWinProcess) process);
	}
	
	public void generation() {
		/*ADwinDevice adwinDevice = ((ADWinProcess)getProcess()).adwinDevice;

		String processNum = getProcess().getPropertyValue(ADWinProcessProperties.PROCESS_NUMBER);
		int processNumInt = Integer.parseInt(processNum);	
		
		for (int j = 0; j < getChannelsNumber(); j++) {
			Channel channel = getChannel(j);
			boolean isStimulus = Boolean.valueOf(channel.getPropertyValue(ADWinDigInOutChannelProperties.STIMULUS));
			if(isStimulus){
				String transferNum = channel.getPropertyValue(ADWinDigInOutChannelProperties.TRANSFER_NUMBER);
				int transferNumInt = Integer.parseInt(transferNum);	
				try {
					int nbData = adwinDevice.Fifo_Empty(transferNumInt);
					if (nbData > 0){
						float[] data = channel.getSamples(nbData);
						adwinDevice.SetFifo_Float(transferNumInt, data, nbData);
						boolean noMoreData = data.length == 0;
						((ADWinProcess)getProcess()).appendToDiary("Gene Num Data: " + transferNum + " - Tampon: " + nbData + "\n");
						if (!noMoreData) {
							if(adwinDevice.Get_Par(transferNumInt) > 0){
								float real_time = adwinDevice.Get_FPar(processNumInt);
								String realTime=String.valueOf(real_time);
								((ADWinProcess)getProcess()).appendToDiary("Perte de donn???es Gene sur Num Data: " + transferNum + " ??? " + realTime + "\n");
								adwinDevice.Set_Par(transferNumInt,0);
							}
						} else ((ADWinProcess)getProcess()).appendToDiary("Plus de donn???es ??? g???n???rer");
						
					}
				}catch (ADwinCommunicationError e) {
					e.printStackTrace();
				}
			}
		}	*/
		GenerationDelegate.generate(this, (ADWinProcess) process);
	}
	
	public void reset() {
		includeSegmentPassedFor_DIGIN = false;
		includeSegmentPassedFor_DIGOUT = false;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		Module newModule = ADWinDigInOutModuleProperties.cloneModule(this);
		for (int i = 0; i < getChannelsNumber(); i++) {
			Channel channel = getChannel(i);
			Channel newChannel = (Channel) channel.clone();
			newModule.addChannel(newChannel);
		}
		return newModule;
	}
	
	@Override
	public boolean addChannel(Channel channel) {
		boolean succeed = super.addChannel(channel);
		notifyObservers(ChannelProperties.ADD, channel, null);
		return succeed;
	}
	
	@Override
	public boolean removeChannel(Channel channel) {
		boolean succeed = super.removeChannel(channel);
		notifyObservers(ChannelProperties.REMOVE, null, channel);
		return succeed;
	}
	
	@Override
	public void initializeObservers() {
		dacqConfiguration.addObserver(this);
	}

}
