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

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.Property;

public class XYCurveConfigurationProperties extends Property {
	
	public static final XYCurveConfigurationProperties X_CHANNEL_NAME = new XYCurveConfigurationProperties("XYCurveConfigurationProperties.X_CHANNEL_NAME", DocometreMessages.XChannelName_Label, DocometreMessages.XChannelName_Tooltip, "");
	public static final XYCurveConfigurationProperties Y_CHANNEL_NAME = new XYCurveConfigurationProperties("XYCurveConfigurationProperties.Y_CHANNEL_NAME", DocometreMessages.YChannelName_Label, DocometreMessages.YChannelName_Tooltip, "");
	
	
	public static void populateProperties(XYCurveConfiguration xyCurveConfiguration){
		CurveConfigurationProperties.populateProperties(xyCurveConfiguration);
		xyCurveConfiguration.setProperty(X_CHANNEL_NAME, "Select Channel (X)");
		xyCurveConfiguration.setProperty(Y_CHANNEL_NAME, "Select Channel (Y)");
	}
	
	public static XYCurveConfiguration clone(XYCurveConfiguration xyCurveConfiguration) {
		XYCurveConfiguration newXYCurveConfiguration = new XYCurveConfiguration(xyCurveConfiguration.getXChannel(), xyCurveConfiguration.getYChannel());
		CurveConfigurationProperties.clone(xyCurveConfiguration, newXYCurveConfiguration);
		newXYCurveConfiguration.setProperty(X_CHANNEL_NAME, new String(xyCurveConfiguration.getProperty(X_CHANNEL_NAME)));
		newXYCurveConfiguration.setProperty(Y_CHANNEL_NAME, new String(xyCurveConfiguration.getProperty(Y_CHANNEL_NAME)));
		return newXYCurveConfiguration;
	}
	
	
	private XYCurveConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private XYCurveConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
