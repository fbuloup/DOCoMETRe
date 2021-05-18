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

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;

public final class ColorUtil {
	
	private static ColorRegistry colorRegistry;
	private static RGB[] rgbs = new RGB[] { 
									new RGB(0xFF, 0x00, 0x00),
									new RGB(0x00, 0xFF, 0x00),
									new RGB(0x00, 0x00, 0xFF),
									new RGB(0xFF, 0xFF, 0x00),
									new RGB(0x00, 0xFF, 0xFF),
									new RGB(0xFF, 0x00, 0xFF),
									new RGB(0xFF, 0x80, 0x00),
									new RGB(0x80, 0x80, 0x80),
									new RGB(0xA0, 0x60, 0x20),
									new RGB(0x60, 0xA0, 0x20),
									new RGB(0x20, 0x60, 0xA0),
									new RGB(0x6D, 0x1D, 0x73),
									new RGB(0xFF, 0xAA, 0x00),
									new RGB(0x99, 0x91, 0x26),
									new RGB(0xFF, 0x80, 0x91),
									new RGB(0x36, 0xB8, 0xD9)
								};
	private static int maxColors = rgbs.length;;
	
	private static ColorRegistry getColorRegistry() {
		if(colorRegistry == null) {
			colorRegistry = new ColorRegistry(PlatformUI.getWorkbench().getDisplay(), true);
			for (int i = 0; i < rgbs.length; i++) {
				colorRegistry.put(String.valueOf(i), rgbs[i]);
			}
		}
		return colorRegistry;
	}
	
	public static Color getColor(Byte index) {
		if(colorRegistry == null) getColorRegistry();
		if(index < 0) index = 0;
		index = (byte) (index % maxColors);
		Color color = getColorRegistry().get(index.toString());
		return color;
	}

}
