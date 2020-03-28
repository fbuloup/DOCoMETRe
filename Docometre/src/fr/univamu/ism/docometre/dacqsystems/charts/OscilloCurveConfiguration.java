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
import fr.univamu.ism.rtswtchart.RTSWTOscilloSerie;

public class OscilloCurveConfiguration extends CurveConfiguration implements PropertyObserver, ChannelObserver {

	private static final long serialVersionUID = 1L;
	
	private Channel channel;
	
	transient private RTSWTOscilloSerie oscilloSerie;
	transient double sampleFrequency;
	transient private double previousTime = 0;

	public OscilloCurveConfiguration(Channel channel) {
		this.channel = channel;
		OscilloCurveConfigurationProperties.populateProperties(this);
		initializeObservers();
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return OscilloCurveConfigurationProperties.clone(this);
	}
	
	@Override
	public void initializeObservers() {
		channel.addObserver((PropertyObserver)this);
		channel.addChannelObserver((ChannelObserver)this);
		sampleFrequency = Double.parseDouble(channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY));
	}
	
//	@Override
//	public void clearObservers() {
//		System.out.println("clearObservers in OscilloCurveConfiguration");
//		channel.removeObserver(this);
//	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		if(property == ChannelProperties.NAME && element == channel) {
			setProperty(OscilloCurveConfigurationProperties.CHANNEL_NAME, channel.getProperty(ChannelProperties.NAME));
			notifyObservers(property, newValue, oldValue);
		}
		if(property == ChannelProperties.TRANSFER && element == channel) {
			if(newValue.equals("false") && oldValue.equals("true")) {
				notifyObservers(ChartConfigurationProperties.UNTRANSFERED_CHANNEL, null, channel);
			}
		}
		
		if(property == ChannelProperties.SAMPLE_FREQUENCY && element == channel) {
			sampleFrequency = Double.parseDouble(channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY));
		}
//		if(property == ChannelProperties.REMOVE && element == oldValue) {
//			chartConfiguration.removeCurveConfiguration(this);
////			setProperty(OscilloCurveConfigurationProperties.CHANNEL_NAME, "");
//			notifyObservers(property, newValue, oldValue);
//		}
	}
	
	// Override in order to initialize observers 
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		initializeObservers();
	}

	@Override
	public boolean mustBeRemoved(Object object) {
		return object == channel;
	}

	public void setSerie(RTSWTOscilloSerie oscilloSerie) {
		this.oscilloSerie = oscilloSerie;
		previousTime = 0;
	}

	@Override
	public void update(FloatBuffer floatBuffer, String channelID) {
		if(oscilloSerie != null /*&& oscilloSerie.getId().equals(channelID)*/) {
			floatBuffer.flip();
			float[] values = new float[floatBuffer.remaining()];
			floatBuffer.get(values);
			Double[] doubleValues = new Double[values.length];
			for (int i = 0; i < doubleValues.length; i++) {
				doubleValues[i] = (double) floatBuffer.get(i);
			}
			if(values.length > 0) {
				Double[] timeValues = new  Double[values.length];
				for (int i = 0; i < timeValues.length; i++) {
					timeValues[i] = previousTime;
					previousTime = timeValues[i] + 1f/sampleFrequency;
				}
				oscilloSerie.addPoints(timeValues, doubleValues);
			}
		}
	}

}
