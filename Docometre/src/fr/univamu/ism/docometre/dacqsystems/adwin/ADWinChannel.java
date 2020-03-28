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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.Property;

public class ADWinChannel extends Channel {

	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	transient private boolean notify = false;
	transient private FloatBuffer floatBuffer;
	transient private long generationIndex = 0;
	transient private String channelID = "";
	
	public ADWinChannel(Module module) {
		super(module);
	}
	
	public String getID() {
		return (channelID == null || channelID.equals(""))? getProperty(ChannelProperties.NAME) : channelID;
	}
	
	public void addSamples(float[] buffer){
		ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.length*4);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		floatBuffer = byteBuffer.asFloatBuffer();
		floatBuffer.put(buffer);
		notify = true;
		try {
			fileChannel.write(byteBuffer);
		} catch (IOException e) {
			Logger.getLogger(Process.class).error("Exception message",e);
		}
	}
	
	public float[] getSamples(int nbData){
	
		ByteBuffer byteBuffer = ByteBuffer.allocate(nbData*4);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		floatBuffer = byteBuffer.asFloatBuffer();
		notify = true;
		
		float[] array = new float[nbData];

		try {
			fileChannel.read(byteBuffer);
			long oldPosition = fileChannel.position();
			
			for (int i = 0; i < array.length; i++) {
				array[i] = floatBuffer.get();
			}
			
			byteBuffer.rewind();
			fileChannel.position(generationIndex);
			fileChannel.read(byteBuffer);
			if((oldPosition - 4*nbData) != 0)
			generationIndex = generationIndex + nbData*4;
			
			fileChannel.position(oldPosition);
			
			
			
		} catch (IOException e) {
			Logger.getLogger(Process.class).error("Exception", e);
		}
		
		return array;
		
	}

	public void open(Process process, String prefix, String suffix) {
		try {
			 
			boolean isStimulus = false;
			boolean isTransfered = false;
//			boolean isRecorded = false;
			String fileName = prefix + getProperty(ChannelProperties.NAME) + suffix;

			IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
//			IResource processResource = ObjectsController.getResourceForObject(process);
			String directoryName = wsPath.toOSString() + process.getOutputFolder().getFullPath().toOSString();
			isTransfered = Boolean.valueOf(getProperty(ChannelProperties.TRANSFER));
			
//			if(this instanceof ADWinVariable) {
//				isStimulus = false;
//				isTransfered = Boolean.valueOf(getProperty(ChannelProperties.TRANSFER));
//				if(isTransfered) {
//					fileName = fileName + ".samples";
//					file = new File(directoryName + File.separator + fileName);
//					outputFile = new FileOutputStream(directoryName + File.separator + fileName);
//					fileChannel = outputFile.getChannel();
//				}
//			}
//			if(module instanceof ADWinAnInModule) {
//				isStimulus = false;
//				isTransfered = Boolean.valueOf(getProperty(ChannelProperties.TRANSFER));
//				if(isTransfered) {
//					fileName = fileName + ".samples";
//					file = new File(directoryName + File.separator + fileName);
//					outputFile = new FileOutputStream(directoryName + File.separator + fileName);
//					fileChannel = outputFile.getChannel();
//				}
//			}
			if(module instanceof ADWinAnOutModule) {
				isStimulus = Boolean.valueOf(getProperty(ADWinAnOutChannelProperties.STIMULUS));
//				isTransfered = Boolean.valueOf(getProperty(ChannelProperties.TRANSFER));
//				if(isStimulus) {
//					fileName = fileName + ".values";
//					inputFile = new FileInputStream(directoryName + File.separator + fileName);
//					fileChannel = inputFile.getChannel();
//				}
//				else if(isTransfered) {
//					fileName = fileName + ".samples";
//					file = new File(directoryName + File.separator + fileName);
//					outputFile = new FileOutputStream(directoryName + File.separator + fileName);
//					fileChannel = outputFile.getChannel();
//				}
			}
			if(module instanceof ADWinDigInOutModule) {
				isStimulus = Boolean.valueOf(getProperty(ADWinDigInOutChannelProperties.STIMULUS));
//				isTransfered = Boolean.valueOf(getProperty(ChannelProperties.TRANSFER));
//				if(isStimulus) {
//					fileName = fileName + ".values";
//					inputFile = new FileInputStream(directoryName + File.separator + fileName);
//					fileChannel = inputFile.getChannel();
//				}
//				else if(isTransfered) {
//					fileName = fileName + ".samples";
//					file = new File(directoryName + File.separator + fileName);
//					outputFile = new FileOutputStream(directoryName + File.separator + fileName);
//					fileChannel = outputFile.getChannel();
//				}
			}
			
			
			if(isStimulus) {
				fileName = fileName + ".values";
				inputFile = new FileInputStream(directoryName + File.separator + fileName);
				fileChannel = inputFile.getChannel();
			}
			else if(isTransfered) {
				fileName = fileName + ".samples";
				file = new File(directoryName + File.separator + fileName);
				outputFile = new FileOutputStream(directoryName + File.separator + fileName);
				fileChannel = outputFile.getChannel();
			}
			
		} catch (FileNotFoundException e) {
			Logger.getLogger(Process.class).error("Exception", e);
		}
	}
	
	public void close(Process process) {
		try {
			if(fileChannel != null) {
				fileChannel.close();
				boolean isRecorded = Boolean.valueOf(getProperty(ChannelProperties.RECORD));
				if(!isRecorded && file != null) {
					file.delete();
					IResource fileResource = process.getOutputFolder().findMember(file.getName());
					if(fileResource != null) fileResource.delete(true, null);
				}
				if(isRecorded && file != null) {
					 try {
						process.getOutputFolder().refreshLocal(IResource.DEPTH_ONE, null);
					} catch (CoreException e) {
						e.printStackTrace();
						Activator.logErrorMessageWithCause(e);
					}
					IResource fileResource = process.getOutputFolder().findMember(file.getName());
					ResourceProperties.setTypePersistentProperty(fileResource, ResourceType.SAMPLES.toString());
				}
				
				
			}
		} catch (IOException | CoreException e) {
			Logger.getLogger(Process.class).error("Exception", e);
		}
	}
	
	public void notifyChannelObservers() {
		if(notify && channelObserversList != null && channelObserversList.size() > 0) {
			for (int i = 0; i < channelObserversList.size(); i++) channelObserversList.get(i).update(floatBuffer, getID());
			notify = false;
		}
			
	}
	
	public boolean isRecoveryAllowed() {
		return "true".equals(getProperty(ChannelProperties.TRANSFER));
	}
	
	public boolean isGenerationAllowed() {
		if(module instanceof ADWinAnInModule) return false;
		if(module instanceof ADWinAnOutModule) return Boolean.valueOf(getProperty(ADWinAnOutChannelProperties.STIMULUS));
		if(module instanceof ADWinDigInOutModule) return Boolean.valueOf(getProperty(ADWinDigInOutChannelProperties.STIMULUS));
		return false;
	}
	
	public int getTransferNumber() {
		return Integer.parseInt(getProperty(ChannelProperties.TRANSFER_NUMBER));	
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Channel channel = null;
		if(module instanceof ADWinAnInModule) channel = ADWinAnInChannelProperties.cloneChannel(this);
		else if(module instanceof ADWinAnOutModule) channel = ADWinAnOutChannelProperties.cloneChannel(this);
		else if(module instanceof ADWinDigInOutModule) channel = ADWinDigInOutChannelProperties.cloneChannel(this);
		else if(module instanceof ADWinCANModule) channel = ADWinCANModuleProperties.cloneChannel(this);
		return channel;
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// TODO Auto-generated method stub
	}

	@Override
	public void initializeObservers() {
	}

	
}
