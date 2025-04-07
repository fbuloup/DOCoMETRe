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

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class ADWinAnInModule extends Module {

	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	private boolean includeSegmentPassed;

	public ADWinAnInModule(DACQConfiguration daqGeneralConfiguration) {
		super(daqGeneralConfiguration);
		ADWinAnInModuleProperties.populateProperties(this);
	}

	public String getCodeSegment(Object segment) {
		String code = "";
		
		if (segment == ADWinCodeSegmentProperties.INCLUDE && !includeSegmentPassed){
			includeSegmentPassed = true;
			String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
			String cpuType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
			String suffix = "";
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO)) code = code + "#INCLUDE ADwpAD.inc\n";
			if(systemType.equals(ADWinDACQConfigurationProperties.GOLD) && cpuType.equals(ADWinDACQConfigurationProperties.II)) suffix = "II";
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.II)) suffix = "II";
			String temp = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH) + File.separator;
			boolean useDocker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.USE_DOCKER);
			if(useDocker) temp = "";
			temp = temp + "CALLANIN" + dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE) + suffix + ".INC\n";
			temp =	ADWinProcess.processPathForMacOSX(temp);
			code = code + "#INCLUDE " + temp;
		}	
		
		String amplitudeMax = getProperty(ADWinAnInModuleProperties.AMPLITUDE_MAX);
		String amplitudeMin = getProperty(ADWinAnInModuleProperties.AMPLITUDE_MIN);
		
		if (segment == ADWinCodeSegmentProperties.DECLARATION) {
			code = "\nREM ******** Dynamique Entrees analogiques\n";
			code = code + "#DEFINE Analog_In_Max " + amplitudeMax + "\n";
			code = code + "#DEFINE Analog_In_Min " + amplitudeMin + "\n";
		}
		
		if (segment == ADWinCodeSegmentProperties.INITIALIZATION) {
			String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
			String cpuType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
			String seDiffProperty = getProperty(ADWinAnInModuleProperties.SINGLE_ENDED_DIFFERENTIAL);
			boolean differential = seDiffProperty.equals(ADWinAnInModuleProperties.DIFF);
			String moduleNumber = getProperty(ADWinModuleProperties.MODULE_NUMBER);
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.I)) {
				code = code + "SE_Diff(" + moduleNumber + ", " + (differential?"1":"0") + ")\n";
				
			}
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.II)) {
				code = code + "P2_SE_Diff(" + moduleNumber + ", " + (differential?"1":"0") + ")\n";
			}
		}
		
		for (int i = 0; i < getChannelsNumber(); i++) {
			Channel channel = getChannel(i);
			
			String name = channel.getProperty(ChannelProperties.NAME);
			String transferNumber = channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
			String bufferSize = channel.getProperty(ChannelProperties.BUFFER_SIZE);
			String channelNumber = channel.getProperty(ChannelProperties.CHANNEL_NUMBER);
			String gain=channel.getProperty(ADWinAnInChannelProperties.GAIN);
			String ampMax = channel.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MAX);
			String ampMin = channel.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MIN);
			String unitMax = channel.getProperty(ADWinAnInChannelProperties.UNIT_MAX);
			String unitMin = channel.getProperty(ADWinAnInChannelProperties.UNIT_MIN);
			String transfer = channel.getProperty(ChannelProperties.TRANSFER);
			String auto_transfer = channel.getProperty(ChannelProperties.AUTO_TRANSFER);
			boolean isTransfered = Boolean.valueOf(transfer);
			boolean isAutoTransfered = Boolean.valueOf(auto_transfer);
			String gsfProcess = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
			float gsfFloat = Float.parseFloat(gsfProcess);	
			String sfChannel = channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			float sfFloat = Float.parseFloat(sfChannel);
			int frequencyRatio = (int) (gsfFloat/sfFloat);
			
//			if (segment == ADWinCodeSegmentProperties.INCLUDE && !includeSegmentPassed){
//				includeSegmentPassed = true;
//				String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
//				String cpuType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
//				String suffix = "";
//				if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.I)) code = code + "#INCLUDE ADwpAD.INC\n";
//				if(systemType.equals(ADWinDACQConfigurationProperties.GOLD) && cpuType.equals(ADWinDACQConfigurationProperties.II)) suffix = "II";
//				if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.II)) suffix = "II";
//				String temp = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH) + File.separator;
//				temp = temp + "CALLANIN" + dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE) + suffix + ".INC\n";
//				temp =	ADWinProcess.processPathForMacOSX(temp);
//				code = code + "#INCLUDE " + temp;
//			}	
			
			if (segment == ADWinCodeSegmentProperties.DECLARATION){
				code = code + "\nREM ******** Entree analogique : " + name + "\n";
				code = code + "DIM " + name + " AS FLOAT\n";
				code = code + "DIM Acquisition_" + name + " AS LONG\n";
//				code = code + "DIM Nouveau_" + name + " AS LONG\n";
				if(isTransfered){
					code = code + "\nREM ******** Variables pour transfert de: " + name + " \'Utilisées par Transfert\n";
					code = code + "#DEFINE " + name + "_TAB DATA_" + transferNumber + " \'Utilisé par Transfert\n";
					code = code + "DIM " + name + "_TAB[" + bufferSize + "] AS FLOAT AS FIFO \'Utilisé par Transfert\n";
					code = code + "DIM TRANSFERT_" + name + " AS LONG \'Utilisé par Transfert\n";
				}
			}	
			
			if (segment == ADWinCodeSegmentProperties.INITIALIZATION){
				code = code + "\nAcquisition_" + name + " = " + frequencyRatio + "\'******** init acquisition " + name + "\n";
				if(isTransfered){
					code = code + "TRANSFERT_" + name + " = " + frequencyRatio + "\n";
					code = code + "FIFO_CLEAR(" + transferNumber + ")\n";
					code = code + "PAR_" + transferNumber + " = 0\n";
				}
			}
			
			if (segment == ADWinCodeSegmentProperties.ACQUISITION){
				String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
//				code = code + "\nNouveau_" + name + " = 0\n";
				code = code + "IF (Acquisition_" + name + " = " + frequencyRatio + ") THEN\n";
				code = code + "\tAcquisition_" + name + " = 0\n"; 
				if(systemType.equals(ADWinDACQConfigurationProperties.GOLD)) code = code + "\t" + name + " = Call_AnIn(" + channelNumber + ", " + gain + ", " + ampMin + ", " + ampMax + ", " + unitMin + ", " + unitMax + ")\n";
				if(systemType.equals(ADWinDACQConfigurationProperties.PRO)) {
					String moduleNumber = getProperty(ADWinModuleProperties.MODULE_NUMBER);
					code = code + "\t" + name + " = Call_AnIn(" + moduleNumber + ", " + channelNumber + ", " + gain + ", " + ampMin + ", " + ampMax + ", " + unitMin + ", " + unitMax + ")\n";
				}
//				code = code + "\tNouveau_" + name + " = 1\n";
				code = code + "ENDIF\n";
				code = code + "INC(Acquisition_" + name + ")\n";
			}
					
			if (segment == ADWinCodeSegmentProperties.TRANSFER){
				if(isAutoTransfered){
					code = code + "\nIF (TRANSFERT_" + name + " = " + frequencyRatio + ") THEN\n";
					code = code + "\tTRANSFERT_" + name + " = 0\n";
					code = code + "\tIF (FIFO_EMPTY(" +  transferNumber + ") = 0) THEN\n";
					code = code + "\t\tPAR_" +  transferNumber + " = PAR_" +  transferNumber + " + 1\n";
					code = code + "\t\tFIFO_CLEAR(" +  transferNumber + ")\n";
					code = code + "\tENDIF\n";
					code = code + "\t" + name + "_TAB = " + name + "\n";
					code = code + "ENDIF\n";
					code = code + "INC(TRANSFERT_" + name + ")\n";
				}
			}
			if (segment==ADWinCodeSegmentProperties.FINISH){
				//TODO
			}
		}
		return code;
	}

	public void update(Property property, Object newValue,  Object oldValue, AbstractElement element) {
		if(property.equals(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY)){
			ADWinDACQConfiguration.changeChannelsFrequencies((String)newValue, (String)oldValue, getChannels());
			notifyObservers(ChannelProperties.SAMPLE_FREQUENCY, newValue, oldValue);
		}
	}

	@Override
	public Channel initializeChannelProperties() {
		Channel channel = new ADWinChannel(this);
		ADWinAnInChannelProperties.populateProperties(channel);
		if(dacqConfiguration != null) {
			String property = channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			property = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
			channel.setProperty(ChannelProperties.SAMPLE_FREQUENCY, property);
		}
		return channel;
	}
	
	public void recovery(){
		RecoveryDelegate.recover(null, this, (ADWinProcess) process);
	}

	public void generation() {
		// TODO Nothing to do !
	}

	/*
	 * Must be reset for a new adbasic file generation
	 * @see fr.univamu.ism.docometre.daqsystems.ModuleBehaviour#reset()
	 */
	public void reset() {
		includeSegmentPassed = false;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Module newModule = ADWinAnInModuleProperties.cloneModule(this);
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

