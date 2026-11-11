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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelFileWriter;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.Property;

public class ArduinoUnoChannel extends Channel {
	
	public static final long serialVersionUID = AbstractElement.serialVersionUID;

	transient private FloatBuffer floatBuffer;
	transient private FloatBuffer displayBuffer;
	transient private boolean notify = false;
	transient private String channelID = "";
	transient private String recordedFileName;

	public ArduinoUnoChannel(Module module) {
		super(module);
	}
	
	public String getID() {
		return (channelID == null || channelID.equals(""))? getProperty(ChannelProperties.NAME) : channelID;
	}
	
	@Override
	public void notifyChannelObservers() {
		if(notify && channelObserversList != null && channelObserversList.size() > 0) {
			displayBuffer.put(floatBuffer.get(0));
			if(displayBuffer.remaining() == 0) {
				for (int i = 0; i < channelObserversList.size(); i++) 
					channelObserversList.get(i).update(displayBuffer, getID());
				displayBuffer.clear();
			}
			
			notify = false;
		}
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addSamples(float[] buffer) {
//		if(module instanceof ArduinoUnoADS1115Module) {
//			// Convert samples values to volt for ADS1115 to do or not to do... ! Also for standard Arduino Analog inputs ? 
//			float maxVoltage = ArduinoUnoAnInChannelProperties.getMaxVoltageForADS1115Gain(getProperty(ArduinoUnoAnInChannelProperties.GAIN));
//			for (int i = 0; i < buffer.length/2; i++) {
//				int j = 2*i+ 1;
//				buffer[j] = buffer[j]*maxVoltage/32767;
//			}
//		}
		ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.length*4);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		floatBuffer = byteBuffer.asFloatBuffer();
		floatBuffer.put(buffer);
		notify = true;
		// Written by a dedicated thread : a slow disk does not block the real-time loop
		ChannelFileWriter.write(fileChannel, byteBuffer);
	}

	@Override
	public float[] getSamples(int nbData) {
		// Unused as there are no stimuli in ArduinoUno
		return new float[0];
	}

	@Override
	public void open(Process process, String prefix, String suffix) {
		try {
			boolean isStimulus = false;
			boolean isTransfered = Boolean.valueOf(getProperty(ChannelProperties.TRANSFER));
			String fileName = prefix + getProperty(ChannelProperties.NAME) + suffix;
			IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
//			IResource processResource = ObjectsController.getResourceForObject(process);
			IFolder outputFolder = process.getOutputFolder();
			String directoryName = wsPath.toOSString() + outputFolder.getFullPath().toOSString();
			
			// For now no stimulus signals with Arduino
			if(this instanceof ArduinoUnoVariable) {
				isStimulus = false;
			}
			if(module instanceof ArduinoUnoAnOutModule) {
				isStimulus = false;
			}
			if(module instanceof ArduinoUnoDigInOutModule) {
				isStimulus = false;
			}
			
			if(isStimulus) {
				// Cannot happen
			} else if(isTransfered) {
				fileName = fileName + ".samples";
				deleteChannelFile(outputFolder, fileName);
				file = new File(directoryName + File.separator + fileName);
				outputFile = new FileOutputStream(directoryName + File.separator + fileName);
				fileChannel = outputFile.getChannel();
			}
			
			double sf = Double.parseDouble(getProperty(ChannelProperties.SAMPLE_FREQUENCY));
			int nbSamples = (int)Math.max(1, 30.0 * 0.001*sf);
			displayBuffer =  FloatBuffer.allocate(nbSamples);
		} catch (FileNotFoundException e) {
			Logger.getLogger(Process.class).error("Exception", e);
		}
	}
		
	@Override
	public void close(Process process) {
		recordedFileName = null;
		try {
			if(fileChannel != null) {
				// Waits for the samples still to be written, then closes the file
				ChannelFileWriter.close(fileChannel);
				// The workspace is refreshed only once for all channels, by ArduinoUnoProcess.postExecute :
				// a deleted file disappears from the workspace and recordedFileName gets its type
				boolean isRecorded = Boolean.valueOf(getProperty(ChannelProperties.RECORD));
				if(!isRecorded && file != null) file.delete();
				if(isRecorded && file != null) recordedFileName = file.getName();
			}
			// Last samples received since the last notification (display buffer not full) :
			// given to the charts too, they would otherwise never be displayed
			if(displayBuffer != null && displayBuffer.position() > 0 && channelObserversList != null) {
				for (int i = 0; i < channelObserversList.size(); i++)
					channelObserversList.get(i).update(displayBuffer, getID());
			}
			if(displayBuffer != null) displayBuffer.clear();
		} catch (IOException e) {
			Logger.getLogger(Process.class).error("Exception", e);
		}
	}
	
	/**
	 * Name of the samples file written by this channel during the last run, null
	 * if this channel did not record (to be used after {@link #close(Process)})
	 */
	public String getRecordedFileName() {
		return recordedFileName;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Channel channel = null;
		if(module instanceof ArduinoUnoAnInModule) channel = ArduinoUnoAnInChannelProperties.cloneChannel(this);
		else if(module instanceof ArduinoUnoAnOutModule) channel = ArduinoUnoAnOutChannelProperties.cloneChannel(this);
		else if(module instanceof ArduinoUnoDigInOutModule) channel = ArduinoUnoDigInOutChannelProperties.cloneChannel(this);
		return channel;
	}

	@Override
	public void initializeObservers() {
		// TODO Auto-generated method stub

	}

}
