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
package fr.univamu.ism.docometre;

import java.util.Random;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;

public final class ColorUtil {
	
	private static ColorRegistry colorRegistry;
	private static Byte index = 0;
	private final static Byte maxColors = 15;
	
	private static ColorRegistry getColorRegistry() {
		if(colorRegistry == null) {
			Random rnd = new Random();
			colorRegistry = new ColorRegistry(PlatformUI.getWorkbench().getDisplay(), true);
			int i = 0;
			while (i <= maxColors) {
				float hue = rnd.nextInt(361);
				float saturation = rnd.nextFloat();
				RGB rgb = new RGB(hue, saturation, 1);
				if(isAcceptable(rgb)) {
					colorRegistry.put(String.valueOf(i), rgb);
					i++;
				}
			}				
				
		}
		return colorRegistry;
	}
	
	private static boolean isAcceptable(RGB newRGB) {
		int i = 0;
		while (colorRegistry.get(String.valueOf(i)) != null) {
			RGB rgb = colorRegistry.get(String.valueOf(i)).getRGB();
			double distance = Math.pow(rgb.red - newRGB.red, 2) + Math.pow(rgb.green - newRGB.green, 2) + Math.pow(rgb.blue - newRGB.blue, 2);
			if(distance < 5000) {
				return false;
			}
			i++;
		}
		return true;
	}
	
	public static Color getColor() {
		Color color = getColorRegistry().get(index.toString());
		index++;
		return color;
	}
	
	public static Color getColor(Byte index) {
		if(index < 0) index = 0;
		if(index > ColorUtil.index) index = ColorUtil.index;
		Color color = getColorRegistry().get(index.toString());
		return color;
	}

}
