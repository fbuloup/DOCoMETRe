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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class CurveConfigurationProperties extends Property {
	
	public static final CurveConfigurationProperties DUMMY = new CurveConfigurationProperties("CurveConfigurationProperties.DUMMY", "DUMMY :", "DUMMY", "");
	//RGB {239, 25, 255}
	public static final CurveConfigurationProperties COLOR = new CurveConfigurationProperties("CurveConfigurationProperties.COLOR", DocometreMessages.Color_Label, DocometreMessages.Color_Tooltip, "^RGB\\s\\{([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5]),\\s([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5]),\\s([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\}$");
	public static final CurveConfigurationProperties STYLE = new CurveConfigurationProperties("CurveConfigurationProperties.STYLE", DocometreMessages.Style_Label, DocometreMessages.Style_Tooltip, CurveConfigurationProperties.styleRegExpValues, CurveConfigurationProperties.styleAvailableValues);
	public static final CurveConfigurationProperties WIDTH = new CurveConfigurationProperties("CurveConfigurationProperties.WIDTH", DocometreMessages.Width_Label, DocometreMessages.Width_Tooltip, "^[1-9]+[0-9]*$");
	
	public final static String SOLID = "SOLID"; //SWT.LINE_SOLID = 1
	public final static String DASH = "DASH"; //SWT.LINE_DASH = 2
	public final static String DOT = "DOT"; // SWT.LINE_DOT = 3
	public final static String DASHDOT = "DASH DOT" ; // SWT.LINE_DASHDOT = 4
	public final static String DASHDOTDOT = "DASH DOT DOT"; // SWT.LINE_DASHDOTDOT = 5
	public final static String[] STYLES = new String[]{SOLID, DASH, DOT, DASHDOT, DASHDOTDOT};
	
	public final static String styleRegExpValues = "^(" + SOLID + "|" + DASH + "|" + DOT + "|" + DASHDOT + "|" + DASHDOTDOT + ")$";
	public final static String styleAvailableValues = SOLID + ":" + DASH + ":" + DOT + ":" + DASHDOT + ":" + DASHDOTDOT;
	
//	public static String getColorKey(CurveConfiguration curveConfiguration) {
//		return curveConfiguration.getProperty(COLOR);
//	}
	
	public static Color getColor(CurveConfiguration curveConfiguration) {
		String colorKey = curveConfiguration.getProperty(COLOR);
		Color color = JFaceResources.getColorRegistry().get(colorKey);
		if(color == null) {
			String value = colorKey.replaceAll("RGB", "");
			value = value.replaceAll("\\{", "");
			value = value.replaceAll("\\}", "");
			value = value.replaceAll("\\s+", "");
			String[] RGB = value.split(",");
			int red = Integer.parseInt(RGB[0]);
			int green = Integer.parseInt(RGB[1]);
			int blue = Integer.parseInt(RGB[2]);
			color = new Color(PlatformUI.getWorkbench().getDisplay(), red, green, blue);
			JFaceResources.getColorRegistry().put(colorKey, new RGB(red, green, blue));
		}
		return color;
	}
	
	public static void populateProperties(CurveConfiguration curveConfiguration){
		curveConfiguration.setProperty(COLOR, "RGB {255, 255, 255}");
		curveConfiguration.setProperty(STYLE, SOLID);
		curveConfiguration.setProperty(WIDTH, "3");
	}

	public static CurveConfiguration clone(CurveConfiguration curveConfiguration, CurveConfiguration newCurveConfiguration) {
		newCurveConfiguration.setProperty(COLOR, new String(curveConfiguration.getProperty(COLOR)));
		newCurveConfiguration.setProperty(STYLE, new String(curveConfiguration.getProperty(STYLE)));
		newCurveConfiguration.setProperty(WIDTH, new String(curveConfiguration.getProperty(WIDTH)));
		return newCurveConfiguration;
	}
	
	private CurveConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private CurveConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

	public static int getStyle(CurveConfiguration curveConfiguration) {
		String style = curveConfiguration.getProperty(STYLE);
		switch (style) {
		case SOLID:
			return SWT.LINE_SOLID;
		case DASH:
			return SWT.LINE_DASH;
		case DOT:
			return SWT.LINE_DOT;
		case DASHDOT:
			return SWT.LINE_DASHDOT;
		case DASHDOTDOT:
			return SWT.LINE_DASHDOTDOT;
		default:
			return SWT.LINE_SOLID;
		}
	}

}
