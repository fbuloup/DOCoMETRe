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


import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.calibration.CalibrationTimerTask;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinAnInChannelProperties;
import fr.univamu.ism.docometre.widgets.CalibrationHeader;
import fr.univamu.ism.docometre.widgets.CalibrationListener;
import fr.univamu.ism.docometre.widgets.ChannelViewer;

public final class ADWinCalibration {

	private static ADWinTimerTask adwinTimerTask;
	
	public static CalibrationHeader createHeader(Composite container, Channel channel) {
		double uMax = Double.parseDouble(channel.getProperty(ADWinAnInChannelProperties.UNIT_MAX));
		double uMin = Double.parseDouble(channel.getProperty(ADWinAnInChannelProperties.UNIT_MIN));
		double ampMax = Double.parseDouble(channel.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MAX));
		double ampMin = Double.parseDouble(channel.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MIN));
		CalibrationHeader calibrationHeaderDelegate = new CalibrationHeader(container, new double[] {ampMin, ampMax, uMin, uMax});
		calibrationHeaderDelegate.addAMaxListener(new CalibrationListener() {
			@Override
			public void push(double value) {
				channel.setProperty(ADWinAnInChannelProperties.AMPLITUDE_MAX, Double.toString(value));
				ObjectsController.serialize(channel.getModule().getDACQConfiguration());
			}
			@Override
			public double get() throws Exception {
				return ADWinRWDelegate.getAnalogValue(channel);
			}
		});
		calibrationHeaderDelegate.addAMinListener(new CalibrationListener() {
			@Override
			public void push(double value) {
				channel.setProperty(ADWinAnInChannelProperties.AMPLITUDE_MIN, Double.toString(value));
				ObjectsController.serialize(channel.getModule().getDACQConfiguration());
			}
			@Override
			public double get() throws Exception {
				return ADWinRWDelegate.getAnalogValue(channel);
			}
		});
		calibrationHeaderDelegate.addUMaxListener(new CalibrationListener() {
			@Override
			public void push(double value) {
				channel.setProperty(ADWinAnInChannelProperties.UNIT_MAX, Double.toString(value));
				ObjectsController.serialize(channel.getModule().getDACQConfiguration());
			}
			@Override
			public double get() throws Exception {
				return ADWinRWDelegate.getAnalogValue(channel);
			}
		});
		calibrationHeaderDelegate.addUMinListener(new CalibrationListener() {
			@Override
			public void push(double value) {
				channel.setProperty(ADWinAnInChannelProperties.UNIT_MIN, Double.toString(value));
				ObjectsController.serialize(channel.getModule().getDACQConfiguration());
			}
			@Override
			public double get() throws Exception {
				return ADWinRWDelegate.getAnalogValue(channel);
			}
		});
		return calibrationHeaderDelegate;
	}

	
	public static double computeCalibration(double y, Channel channel) {
		double uMax = Double.parseDouble(channel.getProperty(ADWinAnInChannelProperties.UNIT_MAX));
		double uMin = Double.parseDouble(channel.getProperty(ADWinAnInChannelProperties.UNIT_MIN));
		double ampMax = Double.parseDouble(channel.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MAX));
		double ampMin = Double.parseDouble(channel.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MIN));
//		double moduleAmpMax = Double.parseDouble(((ADWinAnInModule)channel.getModule()).getProperty(ADWinAnInModuleProperties.AMPLITUDE_MAX));
//		double moduleAmpMin = Double.parseDouble(((ADWinAnInModule)channel.getModule()).getProperty(ADWinAnInModuleProperties.AMPLITUDE_MIN));
//		double value = (moduleAmpMax - moduleAmpMin)/65535*y + moduleAmpMin;
		double value = (uMax - uMin)/(ampMax - ampMin)*(y - ampMin) + uMin;
		return value;
	}
	
	public static CalibrationTimerTask runCalibrationTask(ChannelViewer[] channelsViewers) throws Exception {
		// Schedule timer task
//		calibrationTimer = new Timer();
		adwinTimerTask = new ADWinTimerTask(channelsViewers);
		return adwinTimerTask;
	}
	
}
