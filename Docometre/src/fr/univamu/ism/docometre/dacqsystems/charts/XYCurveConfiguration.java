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
package fr.univamu.ism.docometre.dacqsystems.charts;

import java.io.IOException;
import java.nio.FloatBuffer;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelObserver;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.PropertyObserver;
import fr.univamu.ism.rtswtchart.RTSWTXYSerie;

public class XYCurveConfiguration extends CurveConfiguration implements PropertyObserver, ChannelObserver  {

	private static final long serialVersionUID = 1L;
	
	private Channel xChannel;
	private Channel yChannel;
	
	transient private RTSWTXYSerie xySerie;
	
	public XYCurveConfiguration(Channel xChannel, Channel yChannel) {
		XYCurveConfigurationProperties.populateProperties(this);
		this.xChannel = xChannel;
		this.yChannel = yChannel;
		setProperty(XYCurveConfigurationProperties.X_CHANNEL_NAME, xChannel.getID());
		setProperty(XYCurveConfigurationProperties.Y_CHANNEL_NAME, yChannel.getID());
		initializeObservers();
	}
	
	@Override
	public void initializeObservers() {
		xChannel.addObserver((PropertyObserver)this);
		xChannel.addChannelObserver((ChannelObserver)this);
		yChannel.addObserver((PropertyObserver)this);
		yChannel.addChannelObserver((ChannelObserver)this);
	}

	public Channel getXChannel() {
		return xChannel;
	}

	public Channel getYChannel() {
		return yChannel;
	}
	
	// Override in order to initialize observers 
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		initializeObservers();
	}
	
	public void setSerie(RTSWTXYSerie xySerie) {
		this.xySerie = xySerie;
	}

	@Override
	public boolean mustBeRemoved(Object object) {
		return object == xChannel || object == yChannel;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return XYCurveConfigurationProperties.clone(this);
	}

	@Override
	public void update(FloatBuffer floatBuffer, String channelID) {
		if(xySerie != null) {
			floatBuffer.flip();
			float[] values = new float[floatBuffer.remaining()];
			if(values.length > 0) {
				floatBuffer.get(values);
				Double[] doubleValues = new Double[values.length];
				for (int i = 0; i < doubleValues.length; i++) {
					doubleValues[i] = (double) floatBuffer.get(i);
				}
				if(xChannel.getID().equals(channelID)) xySerie.addPoints(doubleValues, new Double[0]);
				if(yChannel.getID().equals(channelID)) xySerie.addPoints(new Double[0], doubleValues);
			}
		}
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		if(property == ChannelProperties.NAME && element == xChannel || element == yChannel) {
			if(element == xChannel) setProperty(OscilloCurveConfigurationProperties.CHANNEL_NAME, xChannel.getProperty(ChannelProperties.NAME));
			if(element == yChannel) setProperty(OscilloCurveConfigurationProperties.CHANNEL_NAME, yChannel.getProperty(ChannelProperties.NAME));
			notifyObservers(property, newValue, oldValue);
		}
		if(property == ChannelProperties.TRANSFER && element == xChannel || element == yChannel) {
			if(newValue.equals("false") && oldValue.equals("true")) {
				if(element == xChannel) notifyObservers(ChartConfigurationProperties.UNTRANSFERED_CHANNEL, null, xChannel);
				if(element == yChannel) notifyObservers(ChartConfigurationProperties.UNTRANSFERED_CHANNEL, null, yChannel);
			}
		}
		
//		if(property == ChannelProperties.SAMPLE_FREQUENCY && element == xChannel || element == yChannel) {
//			sampleFrequency = Double.parseDouble(channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY));
//		}
//		if(property == ChannelProperties.REMOVE && element == oldValue) {
//			chartConfiguration.removeCurveConfiguration(this);
////			setProperty(OscilloCurveConfigurationProperties.CHANNEL_NAME, "");
//			notifyObservers(property, newValue, oldValue);
//		}
		
	}
	
}
