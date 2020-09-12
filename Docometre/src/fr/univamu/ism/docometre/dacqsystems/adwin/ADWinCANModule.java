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

import java.util.ArrayList;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;

public class ADWinCANModule extends Module {
	
	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	private boolean includeSegmentPassed;

	public ADWinCANModule(DACQConfiguration dacqConfiguration) {
		super(dacqConfiguration);
		ADWinCANModuleProperties.populateProperties(this);
	}
	
	@Override
	public String getCodeSegment(Object segment) throws Exception {
		String code = "";
		
		String moduleNumber = getProperty(ADWinModuleProperties.MODULE_NUMBER);
		String interfaceNumber = getProperty(ADWinCANModuleProperties.INTERFACE_NUMBER);
		String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
		String cpuType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
		String adbasicVersion = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.ADBASIC_VERSION);
		String canName = getProperty(ADWinCANModuleProperties.NAME);
		String baudRate = getProperty(ADWinCANModuleProperties.FREQUENCY);
		
		String mode = getProperty(ADWinCANModuleProperties.MODE);
		boolean receive = mode.equals(ADWinCANModuleProperties.RECEIVE);
		String messageObject = getProperty(ADWinCANModuleProperties.MESSAGE_OBJECT);
		String messageIDLength = getProperty(ADWinCANModuleProperties.MESSAGE_ID_LENGTH);
		String messageIDLength_Code = messageIDLength.equals(ADWinCANModuleProperties.MESSAGE_ID_LENGTH_11)?"0":"1";
		String messageID = getProperty(ADWinCANModuleProperties.MESSAGE_ID);
		
		if(useCodamotion() || useGyroscope() || useTimeStamp()) baudRate = "1000000";
		else {
			int baudRateSpeed = (int) (Double.parseDouble(baudRate) * 1000);
			baudRate = String.valueOf(baudRateSpeed);
		}
		int nbSensors = 0;
		if(getProperty(ADWinCANModuleProperties.NB_SENSORS) != null) nbSensors = Integer.parseInt(getProperty(ADWinCANModuleProperties.NB_SENSORS));
		boolean adbasicVersionSup4 = ADWinDACQConfigurationProperties.VSUP5.equals(adbasicVersion);
		
		
		
		if (segment == ADWinCodeSegmentProperties.INCLUDE && !includeSegmentPassed) {
			includeSegmentPassed = true;
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.I)) code = code + "#INCLUDE ADWPEXT.INC\n";
//			if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && cpuType.equals(ADWinDACQConfigurationProperties.II)) code = code + "#INCLUDE ADWINPRO2.INC\n";
			if(systemType.equals(ADWinDACQConfigurationProperties.GOLD) && cpuType.equals(ADWinDACQConfigurationProperties.I)) code = code + "#INCLUDE ADWGCAN.INC\n";
//			if(systemType.equals(ADWinDACQConfigurationProperties.GOLD) && cpuType.equals(ADWinDACQConfigurationProperties.II)) code = code + "#INCLUDE ADWINGOLDII.INC\n";
		}
		
		if (segment == ADWinCodeSegmentProperties.DECLARATION) {
			if(useCodamotion() || useGyroscope() || useTimeStamp()) code = code + "DIM " + canName + "_SYSTEM_ID AS LONG\n";
			if(useCodamotion()) { // There is CODA
				if(nbSensors > 0) {
					code = code + "\nREM ******** Déclaration variables CODAmotion CAN module " + moduleNumber + " interface " + interfaceNumber + "\n";
					code = code + "DIM " + canName + "_xValue[" + nbSensors + "] AS LONG\n";
					code = code + "DIM " + canName + "_yValue[" + nbSensors + "] AS LONG\n";
					code = code + "DIM " + canName + "_zValue[" + nbSensors + "] AS LONG\n";
					code = code + "DIM " + canName + "_visibility[" + nbSensors + "] AS LONG\n";
					code = code + "DIM " + canName + "_currentMarkerIndex AS LONG\n";
					code = code + "DIM " + canName + "_FRAME_ID_LONG AS LONG\n";
				}
			}
			if(useGyroscope()) { // There is GYRO
				code = code + "\nREM ******** Déclaration variables Gyroscope CAN module " + moduleNumber + " interface " + interfaceNumber + "\n";
				code = code + "DIM " + canName + "_Acceleration_xValue AS LONG\n";
				code = code + "DIM " + canName + "_Acceleration_yValue AS LONG\n";
				code = code + "DIM " + canName + "_Gyroscope_zValue AS LONG\n";
			}
			if(useTimeStamp()) { // There is TIMESTAMP
				code = code + "\nREM ******** Déclaration variables Timer server CAN module " + moduleNumber + " interface " + interfaceNumber + "\n";
				code = code + "DIM " + canName + "_TIME_STAMP_LONG AS LONG\n";
			}
		}
		
		if (segment == ADWinCodeSegmentProperties.INITIALIZATION) {
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO)) {
				code = code + "\nREM ******** Initialisation CAN interface\n";
				code = code + "INIT_CAN("+ moduleNumber + ", " + interfaceNumber + ")\n";
				code = code + "SET_CAN_REG("+ moduleNumber + ", " + interfaceNumber + ", 6, 0)\n";
				code = code + "SET_CAN_REG("+ moduleNumber + ", " + interfaceNumber + ", 7, 0)\n";
				code = code + "fpar_80 = SET_CAN_BAUDRATE("+ moduleNumber + ", " + interfaceNumber + ", " + baudRate + ")\n";
				if(useCodamotion() || useGyroscope() || useTimeStamp()) {
					code = code + "REM Reception de message ID 2 dans message objet 2, ID normal (pas étendu)\n";
					code = code + "EN_RECEIVE("+ moduleNumber + ", " + interfaceNumber + ", 2, 2, 0)\n";
				} else {
					code = code + "REM " + (receive?"Receive ":"Transmit ");
					code = code + " Message object " + messageObject;
					code = code + " Message ID " + messageID;
					code = code + " Message ID length " + messageIDLength + "\n";
					if(receive)	code = code + "EN_RECEIVE("+ moduleNumber + ", " + interfaceNumber + ", " + messageObject + ", " + messageID + ", " + messageIDLength_Code + ")\n";
					else code = code + "EN_TRANSMIT("+ moduleNumber + ", " + interfaceNumber + ", " + messageObject + ", " + messageID + ", " + messageIDLength_Code + ")\n";
					
				}
			}
			if(systemType.equals(ADWinDACQConfigurationProperties.GOLD)) {
				code = code + "\nREM ******** Initialisation CAN interface\n";
				code = code + "INIT_CAN(" + interfaceNumber + ")\n";
				code = code + "SET_CAN_REG(" + interfaceNumber + ", 6, 0)\n";
				code = code + "SET_CAN_REG(" + interfaceNumber + ", 7, 0)\n";
				code = code + "fpar_80 = SET_CAN_BAUDRATE(" + interfaceNumber + ", " + baudRate + ")\n";
				if(useCodamotion() || useGyroscope() || useTimeStamp()) {
					code = code + "REM Reception de message ID 2 dans message objet 2, ID normal (pas étendu)\n";
					code = code + "EN_RECEIVE(" + interfaceNumber + ", 2, 2, 0)\n";
				} else {
					code = code + "REM " + (receive?"Receive ":"Transmit ");
					code = code + " Message object " + messageObject;
					code = code + " Message ID " + messageID;
					code = code + " Message ID length " + messageIDLength + "\n";
					if(receive)	code = code + "EN_RECEIVE(" + interfaceNumber + ", " + messageObject + ", " + messageID + ", " + messageIDLength_Code + ")\n";
					else code = code + "EN_TRANSMIT(" + interfaceNumber + ", " + messageObject + ", " + messageID + ", " + messageIDLength_Code + ")\n";
				}
				
			}
			
			if(useCodamotion()) { // There is CODA
				if(nbSensors > 0) {
					code = code + "\nREM ******** Initialisation variables CODAmotion CAN module " + moduleNumber + " interface " + interfaceNumber + "\n";
					code = code + canName + "_currentMarkerIndex = 0\n";
					code = code + canName + "_FRAME_ID_LONG = 0\n";
					for (int i = 1; i <= nbSensors; i++) {
						code = code + canName + "_xValue[" + i + "] = 0\n";
						code = code + canName + "_yValue[" + i + "] = 0\n";
						code = code + canName + "_zValue[" + i + "] = 0\n";
						code = code + canName + "_visibility[" + i + "] = 0\n";
					}
					
					
				}
			}
			
		}
		
		
		
		if (segment == ADWinCodeSegmentProperties.ACQUISITION && !ADWinCANModuleProperties.NOT_SPECIFIED_SYSTEM_TYPE.equals(getProperty(ADWinCANModuleProperties.SYSTEM_TYPE))){
			code = code + "REM ******** Debut acquition CAN interface - Check message ID 2\n";
//			if(ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE.equals(canSystemType)) {
			String TAB = "";
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && adbasicVersionSup4)
				code = code + "\nIF (READ_MSG_CON(" + moduleNumber +", " + interfaceNumber + ", " + 2 + ") = 2) THEN\n";
			if(systemType.equals(ADWinDACQConfigurationProperties.GOLD) && adbasicVersionSup4)
				code = code + "\nIF (READ_MSG_CON(" + interfaceNumber + ", " + 2 + ") = 2) THEN\n";
			if(systemType.equals(ADWinDACQConfigurationProperties.PRO) && !adbasicVersionSup4)
				code = code + "\nIF (READ_MSG(" + moduleNumber +", " + interfaceNumber + ", " + 2 + ") = 2) THEN\n";
			if(systemType.equals(ADWinDACQConfigurationProperties.GOLD) && !adbasicVersionSup4)
				code = code + "\nIF (READ_MSG(" + interfaceNumber + ", " + 2 + ") = 2) THEN\n";
			if(!adbasicVersionSup4) {
				TAB = "\t\t";
				code = code + "\t\tIF (CAN_MSG[9] = 8) THEN ' All bytes have been read\n";
			}
			code = code + TAB + "\t\tREM Get System ID : 00b -> CODA, 01b -> XSens, 10b -> Time Server\n";
			code = code + TAB + "\t\t" + canName + "_SYSTEM_ID = SHIFT_RIGHT(CAN_MSG[1], 6)\n";
			if(useCodamotion() && nbSensors > 0) {
				code = code + TAB + "\t\tIF (" + canName + "_SYSTEM_ID = 0) THEN ' This is CODA system\n";
				code = code + TAB + "\t\t\t\tREM Marker number on 5 bits : max 32 possibilities\n";
				code = code + TAB + "\t\t\t\t" + canName + "_currentMarkerIndex = CAN_MSG[1] AND 01Fh\n";
				code = code + TAB + "\t\t\t\tREM Read frame ID (8 signed bits) in long\n";
				code = code + TAB + "\t\t\t\t" + canName + "_FRAME_ID_LONG = CAN_MSG[2]\n";
				code = code + TAB + "\t\t\t\tIF (SHIFT_RIGHT(CAN_MSG[2],7) = 1) THEN ' Manage sign bit\n";
				code = code + TAB + "\t\t\t\t\t\t" + canName + "_FRAME_ID_LONG = " + canName + "_FRAME_ID_LONG OR 0FFFFFF00h\n";
				code = code + TAB + "\t\t\t\tENDIF\n";
				code = code + TAB + "\t\t\t\tREM Convert to float\n";
				code = code + TAB + "\t\t\t\t" + canName + "_FrameID = " + canName + "_FRAME_ID_LONG\n";
				code = code + TAB + "\t\t\t\tREM Get marker visibility\n"; 
				code = code + TAB + "\t\t\t\t" + canName + "_visibility[" + canName + "_currentMarkerIndex] = SHIFT_RIGHT(CAN_MSG[1],5) AND 001h\n";
			    code = code + TAB + "\t\t\t\tREM Get Marker x value\n";
			    code = code + TAB + "\t\t\t\t" + canName + "_xValue[" + canName + "_currentMarkerIndex] = CAN_MSG[3]*256 + CAN_MSG[4]\n";
			    code = code + TAB + "\t\t\t\tIF (SHIFT_RIGHT(CAN_MSG[3],7) = 1) THEN ' Manage sign bit\n";
			    code = code + TAB + "\t\t\t\t\t\t" + canName + "_xValue[" + canName + "_currentMarkerIndex] = " + canName + "_xValue[" + canName + "_currentMarkerIndex] OR 0FFFF0000h\n";
			    code = code + TAB + "\t\t\t\tENDIF\n";
			    code = code + TAB + "\t\t\t\tREM Get Marker y value\n";
			    code = code + TAB + "\t\t\t\t" + canName + "_yValue[" + canName + "_currentMarkerIndex] = CAN_MSG[5]*256 + CAN_MSG[6]\n";
			    code = code + TAB + "\t\t\t\tIF (SHIFT_RIGHT(CAN_MSG[5],7) = 1) THEN ' Manage sign bit\n";
			    code = code + TAB + "\t\t\t\t\t\t" + canName + "_yValue[" + canName + "_currentMarkerIndex] = " + canName + "_yValue[" + canName + "_currentMarkerIndex] OR 0FFFF0000h\n";
			    code = code + TAB + "\t\t\t\tENDIF\n";
			    code = code + TAB + "\t\t\t\tREM Get Marker z value\n";
			    code = code + TAB + "\t\t\t\t" + canName + "_zValue[" + canName + "_currentMarkerIndex] = CAN_MSG[7]*256 + CAN_MSG[8]\n";
			    code = code + TAB + "\t\t\t\tIF (SHIFT_RIGHT(CAN_MSG[7],7) = 1) THEN ' Manage sign bit\n";
			    code = code + TAB + "\t\t\t\t\t\t" + canName + "_zValue[" + canName + "_currentMarkerIndex] = " + canName + "_zValue[" + canName + "_currentMarkerIndex] OR 0FFFF0000h\n";
			    code = code + TAB + "\t\t\t\tENDIF\n";
			    for (int i = 0; i < nbSensors; i++) {
			    	code = code + TAB + "\t\t\t\t" + canName + "_Marker" + (i+1) + "_X = " + canName + "_xValue[" + (i+1) + "]/100.0\n";
				    code = code + TAB + "\t\t\t\t" + canName + "_Marker" + (i+1) + "_Y = " + canName + "_yValue[" + (i+1) + "]/100.0\n";
				    code = code + TAB + "\t\t\t\t" + canName + "_Marker" + (i+1) + "_Z = " + canName + "_zValue[" + (i+1) + "]/100.0\n";
				    code = code + TAB + "\t\t\t\t" + canName + "_Marker" + (i+1) + "_VISIBILITY = " + canName + "_visibility[" + (i+1) + "]\n";
				}
				code = code + TAB + "\t\tENDIF\n";
			}
			if(useGyroscope()) {
				
				code = code + TAB + "\t\tIF (CAN_SYSTEM_ID = 1) THEN ' This is Gyroscope system\n";

				code = code + TAB + "\t\t\t\tREM Get X Accel value\n";
				code = code + TAB + "\t\t\t\t" + canName + "_Acceleration_xValue = CAN_MSG[2]*256 + CAN_MSG[3]\n";
			    code = code + TAB + "\t\t\t\tIF(shift_right(CAN_MSG[2],7) = 1) THEN\n";
			    code = code + TAB + "\t\t\t\t\t\t" + canName + "_Acceleration_xValue = " + canName + "_Acceleration_xValue OR 0FFFF0000h\n";
			    code = code + TAB + "\t\t\t\tENDIF\n";
			    
			    code = code + TAB + "\t\t\t\tREM Get Y Accel value\n";
				code = code + TAB + "\t\t\t\t" + canName + "_Acceleration_yValue = CAN_MSG[4]*256 + CAN_MSG[5]\n";
			    code = code + TAB + "\t\t\t\tIF(shift_right(CAN_MSG[4],7) = 1) THEN\n";
			    code = code + TAB + "\t\t\t\t\t\t" + canName + "_Acceleration_yValue = " + canName + "_Acceleration_yValue OR 0FFFF0000h\n";
			    code = code + TAB + "\t\t\t\tENDIF\n";
			    
			    code = code + TAB + "\t\t\t\tREM Get Z Accel value\n";
				code = code + TAB + "\t\t\t\t" + canName + "_Gyroscope_zValue = CAN_MSG[6]*256 + CAN_MSG[7]\n";
			    code = code + TAB + "\t\t\t\tIF(shift_right(CAN_MSG[6],7) = 1) THEN\n";
			    code = code + TAB + "\t\t\t\t\t\t" + canName + "_Gyroscope_zValue = " + canName + "_Gyroscope_zValue OR 0FFFF0000h\n";
			    code = code + TAB + "\t\t\t\tENDIF\n";
			    
			    code = code + TAB + "\t\t\t\t" + canName + "_Acceleration_X = " + canName + "_Acceleration_xValue / 1000.0 ' m/s\n";
			    code = code + TAB + "\t\t\t\t" + canName + "_Acceleration_Y = " + canName + "_Acceleration_yValue / 1000.0 ' m/s\n";
			    code = code + TAB + "\t\t\t\t" + canName + "_Gyroscope_Z = " + canName + "_Gyroscope_zValue / 100.0 ' deg/s\n";
//			    PS1_XXSensAccelValue = XXSensAccelValue/1000.0
//			    PS1_YXSensAccelValue = YXSensAccelValue/1000.0
//			    PS1_ZXSensGyroValue = ZXSensGyroValue/100.0
//			  ENDIF
			    
			    code = code + TAB + "\t\tENDIF\n";
				
			}
			if(useTimeStamp()) {
				code = code + TAB + "\t\tIF (" + canName + "_SYSTEM_ID = 2) THEN ' This is TIME SERVER system\n";
				code = code + TAB + "\t\t\t\tREM Get time stamp value\n";
				code = code + TAB + "\t\t\t\t" + canName + "_TIME_STAMP_LONG = (CAN_MSG[1] AND 01Fh)\n";
				code = code + TAB + "\t\t\t\t" + canName + "_TimeStamp = " + canName + "_TIME_STAMP_LONG*3600 + CAN_MSG[2]*60 + CAN_MSG[3]\n";
				code = code + TAB + "\t\tENDIF\n";
			}
			if(!adbasicVersionSup4) code = code + TAB + "ENDIF\n";
			code = code + "ENDIF\n";
//			}
			
//			String systemType = getDACQConfiguration().getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
////			code = code + "\nNouveau_" + name + " = 0\n";
//			code = code + "IF (Acquisition_" + name + " = " + frequencyRatio + ") THEN\n";
//			code = code + "\tAcquisition_" + name + " = 0\n"; 
//			if(systemType.equals(ADWinDACQConfigurationProperties.GOLD)) code = code + "\t" + name + " = Call_AnIn(" + channelNumber + ", " + gain + ", " + ampMin + ", " + ampMax + ", " + unitMin + ", " + unitMax + ")\n";
//			if(systemType.equals(ADWinDACQConfigurationProperties.PRO)) {
//				String moduleNumber = getProperty(ADWinModuleProperties.MODULE_NUMBER);
//				code = code + "\t" + name + " = Call_AnIn(" + moduleNumber + ", " + channelNumber + ", " + gain + ", " + ampMin + ", " + ampMax + ", " + unitMin + ", " + unitMax + ")\n";
//			}
////			code = code + "\tNouveau_" + name + " = 1\n";
//			code = code + "ENDIF\n";
//			code = code + "INC(Acquisition_" + name + ")\n";
			code = code + "REM ******** Fin acquition CAN interface\n";
		}
		
		for (int i = 0; i < getChannelsNumber(); i++) {
			Channel channel = getChannel(i);
			
			String name = channel.getProperty(ChannelProperties.NAME);
			String transferNumber = channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
			String bufferSize = channel.getProperty(ChannelProperties.BUFFER_SIZE);
			//String channelNumber = channel.getProperty(ChannelProperties.CHANNEL_NUMBER);
			String transfer = channel.getProperty(ChannelProperties.TRANSFER);
			String auto_transfer = channel.getProperty(ChannelProperties.AUTO_TRANSFER);
			boolean isTransfered = Boolean.valueOf(transfer);
			boolean isAutoTransfered = Boolean.valueOf(auto_transfer);
			String gsfProcess = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY);
			float gsfFloat = Float.parseFloat(gsfProcess);	
			String sfChannel = getProperty(ADWinCANModuleProperties.FREQUENCY);
			float sfFloat = Float.parseFloat(sfChannel);
			int frequencyRatio = (int) (gsfFloat/sfFloat);
			
			
			
			if (segment == ADWinCodeSegmentProperties.DECLARATION){
//				String systemType = getProperty(ADWinCANModuleProperties.SYSTEM_TYPE);
				code = code + "\nREM ******** Déclaration canal : " + name + "\n";
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
//			
			if (segment == ADWinCodeSegmentProperties.INITIALIZATION){
				code = code + "\nAcquisition_" + name + " = " + frequencyRatio + "\'******** init acquisition " + name + "\n";
				code = code + name + " = 0\n";
				if(isTransfered){
					code = code + "TRANSFERT_" + name + " = " + frequencyRatio + "\n";
					code = code + "FIFO_CLEAR(" + transferNumber + ")\n";
					code = code + "PAR_" + transferNumber + " = 0\n";
				}
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
//			if (segment==ADWinCodeSegmentProperties.FINISH){
//				//TODO
//			}
			
		}
		
		code += "\n";
		return code;
	}
	
	@Override
	public void recovery() {
		RecoveryDelegate.recover(null, this, (ADWinProcess) process);
	} 

	@Override
	public void generation() {
		// TODO Nothing to do !
	}

	@Override
	public void reset() {
		includeSegmentPassed = false;
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// TODO Auto-generated method stub
		System.out.println(property);
		
	}

	@Override
	public Channel initializeChannelProperties() {
		Channel channel = new ADWinChannel(this);
		ChannelProperties.populateProperties(channel);
		if(dacqConfiguration != null) {
			String property = getProperty(ADWinCANModuleProperties.FREQUENCY);
			channel.setProperty(ChannelProperties.SAMPLE_FREQUENCY, property);
		}
		return channel;
	}
	
	public void createCodamotionChannels() {
		// Get current channels number
		int nbChannels = getCodamotionChannels().length;
		int currentNbSensors = (nbChannels - 1) / 4;
		boolean isFrameIDCreated = nbChannels > 0;
		// Prefix name
		String prefix = getProperty(ADWinCANModuleProperties.NAME);
		// Create frame ID channel if necessary
		Channel frameID = null;
		if(!isFrameIDCreated) {
			frameID = super.createChannel();
			String channelName = prefix + "_FrameID";
			frameID.setProperty(ChannelProperties.NAME, channelName);
			frameID.setProperty(ADWinCANModuleProperties.SYSTEM_TYPE, ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE);
		}
		// Create three more channels
		// Marker N X
		Channel channel_X = super.createChannel();
		String channelName = prefix + "_Marker" + (currentNbSensors + 1) + "_X";
		channel_X.setProperty(ChannelProperties.NAME, channelName);
		channel_X.setProperty(ADWinCANModuleProperties.SYSTEM_TYPE, ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE);
		// Marker N Y
		Channel channel_Y = super.createChannel();
		channelName = prefix + "_Marker" + (currentNbSensors + 1) + "_Y";
		channel_Y.setProperty(ChannelProperties.NAME, channelName);
		channel_Y.setProperty(ADWinCANModuleProperties.SYSTEM_TYPE, ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE);
		// Marker N Z
		Channel channel_Z = super.createChannel();
		channelName = prefix + "_Marker" + (currentNbSensors + 1) + "_Z";
		channel_Z.setProperty(ChannelProperties.NAME, channelName);
		channel_Z.setProperty(ADWinCANModuleProperties.SYSTEM_TYPE, ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE);
		// Marker N visibility
		Channel channel_VISIBILITY = super.createChannel();
		channelName = prefix + "_Marker" + (currentNbSensors + 1) + "_VISIBILITY";
		channel_VISIBILITY.setProperty(ChannelProperties.NAME, channelName);
		channel_VISIBILITY.setProperty(ADWinCANModuleProperties.SYSTEM_TYPE, ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE);
	}
	
	public Channel[] getCodamotionChannels() {
		Channel[] channels = getChannels();
		ArrayList<Channel> codaChannels = new ArrayList<Channel>(0);
		for (Channel channel : channels) {
			if(ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE.equals(channel.getProperty(ADWinCANModuleProperties.SYSTEM_TYPE))) codaChannels.add(channel);
		}
		return codaChannels.toArray(new Channel[codaChannels.size()]); 
	}
	
	public void createGyroscopeChannels() {
		String prefix = getProperty(ADWinCANModuleProperties.NAME);
		// Acceleration X Value
		Channel gyroXValue = super.createChannel();
		String channelName = prefix + "_Acceleration_X";
		gyroXValue.setProperty(ChannelProperties.NAME, channelName);
		gyroXValue.setProperty(ADWinCANModuleProperties.SYSTEM_TYPE, ADWinCANModuleProperties.GYROSCOPE_SYSTEM_TYPE);
		// Acceleration Y Value
		Channel gyroYValue = super.createChannel();
		channelName = prefix + "_Acceleration_Y";
		gyroYValue.setProperty(ChannelProperties.NAME, channelName);
		gyroYValue.setProperty(ADWinCANModuleProperties.SYSTEM_TYPE, ADWinCANModuleProperties.GYROSCOPE_SYSTEM_TYPE);
		// Gyroscope Z Value
		Channel gyroZValue = super.createChannel();
		channelName = prefix + "_Gyroscope_Z";
		gyroZValue.setProperty(ChannelProperties.NAME, channelName);
		gyroZValue.setProperty(ADWinCANModuleProperties.SYSTEM_TYPE, ADWinCANModuleProperties.GYROSCOPE_SYSTEM_TYPE);
	}
	
	public Channel[] getGyroscopeChannels() {
		Channel[] channels = getChannels();
		ArrayList<Channel> gyroChannels = new ArrayList<Channel>(0);
		for (Channel channel : channels) {
			if(ADWinCANModuleProperties.GYROSCOPE_SYSTEM_TYPE.equals(channel.getProperty(ADWinCANModuleProperties.SYSTEM_TYPE))) gyroChannels.add(channel);
		}
		return gyroChannels.toArray(new Channel[gyroChannels.size()]); 
	}
	
	public void createTimeStampChannels() {
		String prefix = getProperty(ADWinCANModuleProperties.NAME);
		Channel timeStampValue = super.createChannel();
		String channelName = prefix + "_TimeStamp";
		timeStampValue.setProperty(ChannelProperties.NAME, channelName);
		timeStampValue.setProperty(ADWinCANModuleProperties.SYSTEM_TYPE, ADWinCANModuleProperties.TIMESTAMP_SYSTEM_TYPE);
	}
	
	public Channel[] getTimeStampChannels() {
		Channel[] channels = getChannels();
		ArrayList<Channel> timeStampChannels = new ArrayList<Channel>(0);
		for (Channel channel : channels) {
			if(ADWinCANModuleProperties.TIMESTAMP_SYSTEM_TYPE.equals(channel.getProperty(ADWinCANModuleProperties.SYSTEM_TYPE))) timeStampChannels.add(channel);
		}
		return timeStampChannels.toArray(new Channel[timeStampChannels.size()]); 
	}
	
	@Override
	public boolean removeChannel(Channel channel) {
		boolean result = super.removeChannel(channel);
		if(result) {
			if(getCodamotionChannels().length == 1) {
				// If it only remains Frame ID Channel, remove it
				return removeChannel(getCodamotionChannels()[0]);
			}
		}
		return result;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		Module newModule = ADWinCANModuleProperties.cloneModule(this);
		for (int i = 0; i < getChannelsNumber(); i++) {
			Channel channel = getChannel(i);
			Channel newChannel = (Channel) channel.clone();
			newModule.addChannel(newChannel);
		}
		return newModule;
	}

	@Override
	public void initializeObservers() {
		dacqConfiguration.addObserver(this);
	}
	
	private boolean useCodamotion() {
		return getProperty(ADWinCANModuleProperties.SYSTEM_TYPE).contains(ADWinCANModuleProperties.CODAMOTION_SYSTEM_TYPE);
	}
	
	private boolean useGyroscope() {
		return getProperty(ADWinCANModuleProperties.SYSTEM_TYPE).contains(ADWinCANModuleProperties.GYROSCOPE_SYSTEM_TYPE);
	}
	
	private boolean useTimeStamp() {
		return getProperty(ADWinCANModuleProperties.SYSTEM_TYPE).contains(ADWinCANModuleProperties.TIMESTAMP_SYSTEM_TYPE);
	}

}
