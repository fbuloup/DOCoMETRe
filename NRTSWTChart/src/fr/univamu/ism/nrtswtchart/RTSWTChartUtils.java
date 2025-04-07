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
package fr.univamu.ism.nrtswtchart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;

/**
 * Just an utility class to compute array max and min 
 * @author frankbuloup
 */
public final class RTSWTChartUtils {

	/**
	 * Return maximum value from x double array.
	 * This value is searched between zero and maxIndex indices in the array.
	 * NaN values are ignored. 
	 * @param x The double array
	 * @param maxIndex Search is done in x array from zero until maxIndex is reached
	 * @return maximum value in x[0..maxIndex]
	 */
	public static double getMax(double[] x, int maxIndex) {
		if(x.length <= maxIndex) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		double max = Double.MIN_VALUE;
		for (int i = 0; i <= maxIndex; i++) {
			if(Double.compare(x[i], Double.NaN) != 0) max = Math.max(max, x[i]);
		}
		return max;
	}

	/**
	 * Return minimum value from x double array.
	 * This value is searched between zero and maxIndex indices in the array.
	 * NaN values are ignored. 
	 * @param x The double array
	 * @param maxIndex Search is done in x array from zero until maxIndex is reached
	 * @return minimum value in x[0..maxIndex]
	 */
	public static double getMin(double[] x, int maxIndex) {
		if(x.length<=maxIndex) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		double min = Double.MAX_VALUE;
		for (int i = 0; i <= maxIndex; i++) {
			if(Double.compare(x[i], Double.NaN) != 0) min = Math.min(min, x[i]);
		}
		return min;
	}

	/**
	 * Return maximum value from x double array.
	 * NaN values are ignored. 
	 * @param x The double array
	 * @return maximum value
	 */
	public static double getMax(double[] x) {
		return getMax(x, x.length - 1);
	}

	/**
	 * Return minimum value from x double array.
	 * NaN values are ignored. 
	 * @param x The double array
	 * @return minimum value
	 */
	public static double getMin(double[] x) {
		return getMin(x, x.length - 1);
	}

	/*
	 * SAME BUT WITH Double
	 */

	/**
	 * Return maximum value from x Double array.
	 * This value is searched between zero and maxIndex indices in the array.
	 * NaN values are ignored. 
	 * @param x The Double array
	 * @param maxIndex Search is done in x array from zero until maxIndex is reached
	 * @return maximum value in x[0..maxIndex]
	 */
	public static double getMax(Double[] x, int maxIndex) {
		if(x.length <= maxIndex) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		double max = Double.MIN_VALUE;
		for (int i = 0; i <= maxIndex; i++) {
			if(Double.compare(x[i], Double.NaN) != 0) max = Math.max(max, x[i]);
		}
		return max;
	}

	/**
	 * Return minimum value from x Double array.
	 * This value is searched between zero and maxIndex indices in the array.
	 * NaN values are ignored. 
	 * @param x The Double array
	 * @param maxIndex Search is done in x array from zero until maxIndex is reached
	 * @return minimum value in x[0..maxIndex]
	 */
	public static double getMin(Double[] x, int maxIndex) {
		if(x.length<=maxIndex) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		double min = Double.MAX_VALUE;
		for (int i = 0; i <= maxIndex; i++) {
			if(Double.compare(x[i], Double.NaN) != 0) min = Math.min(min, x[i]);
		}
		return min;
	}

	/**
	 * Return maximum value from x Double array.
	 * NaN values are ignored. 
	 * @param x The Double array
	 * @return maximum value
	 */
	public static double getMax(Double[] x) {
		return getMax(x, x.length - 1);
	}

	/**
	 * Return minimum value from x Double array.
	 * NaN values are ignored. 
	 * @param x The Double array
	 * @return minimum value
	 */
	public static double getMin(Double[] x) {
		return getMin(x, x.length - 1);
	}
	
	public static void plotLineLow(ImageData imageData, int x0, int y0, int x1, int y1, int color) {
		int dx = x1 - x0;
		int dy = y1 - y0;
		int yi = 1;
		if (dy < 0) {
			yi = -1;
			dy = -dy;
		}
		int D = (2 * dy) - dx;
		int y = y0;
		for(int x = x0; x <= x1; x++) {
			imageData.setPixel(x, y, color);
			if (D > 0) {
				y = y + yi;
				D = D + (2 * (dy - dx));
			} else D = D + 2*dy;
		}
	}

	public static void plotLineHigh(ImageData imageData, int x0, int y0, int x1, int y1, int color) {
		int dx = x1 - x0;
		int dy = y1 - y0;
		int xi = 1;
		if (dx < 0) {
			xi = -1;
			dx = -dx;
		}
		int D = (2 * dx) - dy;
		int x = x0;
		for(int y = y0; y <= y1; y++) {
			imageData.setPixel(x, y, color);
			if (D > 0) {
				x = x + xi;
				D = D + (2 * (dx - dy));
			} else D = D + 2*dx;
		}
	}

	public static void plotLine(ImageData imageData, int x0, int y0, int x1, int y1, int color) {
		if (x0 >= imageData.width || y0 >= imageData.height || x0 < 0 || y0 < 0) return;
		if (x1 >= imageData.width || y1 >= imageData.height || x1 < 0 || y1 < 0) return;
		if(x0 != x1 && y0 != y1) {
			if (Math.abs(y1 - y0) < Math.abs(x1 - x0)) {
				if (x0 > x1) plotLineLow(imageData, x1, y1, x0, y0, color);
				else plotLineLow(imageData, x0, y0, x1, y1, color);
			} else {
				if (y0 > y1) plotLineHigh(imageData, x1, y1, x0, y0, color);
				else plotLineHigh(imageData, x0, y0, x1, y1, color);
			}
		} else {
			if(x0 != x1 && y0 == y1) {
				if(x0 < x1) {
					for (int x = x0; x <= x1; x++) {
						imageData.setPixel(x, y0, color);
					}
				} else {
					for (int x = x1; x <= x0; x++) {
						imageData.setPixel(x, y0, color);
					}
				}
			} else {
				if(y0 < y1) {
					for (int y = y0; y <= y1; y++) {
						imageData.setPixel(x0, y, color);
					}
				} else {
					for (int y = y1; y <= y0; y++) {
						imageData.setPixel(x0, y, color);
					}
				}

			}
		}

	}
}