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
package fr.univamu.ism.rtswtchart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;


public class RTSWTXYSerie extends RTSWTSerie {
	
	/**
	 * Values in x axis. 
	 */
	protected ArrayList<Double> x_XYValues;
	/**
	 * Values in y axis. 
	 */
	protected ArrayList<Double> y_XYValues;
	
	private  ArrayList<Double> x_XYValues_Buffer;
	private  ArrayList<Double> y_XYValues_Buffer;
	private  List<Double> xExtractedValues;
	private  List<Double> yExtractedValues;
	private Double[] xBuffer;
	private Double[] yBuffer;
	
	/**
	 * Most recent real time values
	 */
	private double lastPointX = Double.NEGATIVE_INFINITY;
	private double lastPointY = Double.NEGATIVE_INFINITY;
	
	protected RTSWTXYSerie(RTSWTChart chart, String id, Color lineColor, int lineStyle, int lineWidth) {
		super(chart, id, lineColor, lineStyle, lineWidth);
	}
	/**
	 * Add a new value over nbPixels range
	 * @param X
	 * @param Y
	 * @param nbPixelsX
	 * @param nbPixelsY
	 */
	private void addValue(double x, double y, int nbPixelsX, int nbPixelsY) {
//		double dx = chart.getDx();
//		double dy = chart.getDy();
		x_XYValues.add(x);
		y_XYValues.add(y);
		lastPointX = x;
		lastPointY = y;
	}
	
	/**
	 * Add new values to the serie. Values
	 * will be filtered in order to be painted within chart screen dimensions.
	 * @param x time values
	 * @param y amplitude values
	 */
	public void addPoints(final Double[] x, final Double[] y) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				
				if(chart.isDisposed()) return;
				
				if(y_XYValues == null || x_XYValues == null) reset();
				
				if(x.length == 0) y_XYValues_Buffer.addAll(Arrays.asList(y));
				if(y.length == 0) x_XYValues_Buffer.addAll(Arrays.asList(x));
				
				if(y_XYValues_Buffer.size() == 0 || x_XYValues_Buffer.size() == 0) return;
				
				int maxIndex = Math.min(y_XYValues_Buffer.size(), x_XYValues_Buffer.size());
				xExtractedValues = x_XYValues_Buffer.subList(0, maxIndex);
				yExtractedValues = y_XYValues_Buffer.subList(0, maxIndex);
				
				xBuffer = xExtractedValues.toArray(new Double[maxIndex]);
				yBuffer = yExtractedValues.toArray(new Double[maxIndex]);
				
				for (int i = 0; i < maxIndex; i++) {
					x_XYValues_Buffer.remove(0);
					y_XYValues_Buffer.remove(0);
				}
				
				double dx = chart.getDx();
				double dy = chart.getDy();
				
				for (int i = 0; i < xBuffer.length; i++) {
					int nbPixelsX = Math.abs((int)((xBuffer[i] - lastPointX) / dx));
					int nbPixelsY = Math.abs((int)((yBuffer[i] - lastPointY) / dy));
					boolean addPoint = nbPixelsX > 0 || nbPixelsY > 0;
					if(addPoint) {
						addValue(xBuffer[i], yBuffer[i], nbPixelsX, nbPixelsY);
						setModified(true);
					}
				}
				
				
				
				chart.render();
			}
		});
		
	}

	@Override
	protected void reset() {
		if(y_XYValues != null) {
			y_XYValues.clear();
			y_XYValues_Buffer.clear();
		}
		if(x_XYValues != null) {
			x_XYValues.clear();
			x_XYValues_Buffer.clear();
		}
		y_XYValues = new ArrayList<Double>();
		x_XYValues = new ArrayList<Double>();
		y_XYValues_Buffer = new ArrayList<Double>();
		x_XYValues_Buffer = new ArrayList<Double>();
		lastPointX = Double.NEGATIVE_INFINITY;
		lastPointY = Double.NEGATIVE_INFINITY;
		setModified(false);
	}
	
	public int getNumberOfPoints() {
		return x_XYValues.size();
	}
	
	@Override
	protected double getyMinHeight() {
		Double[] d = y_XYValues.toArray(new Double[0]);
		return Utils.getMin(d);
	}
	
	@Override
	protected double getyMaxHeight() {
		Double[] d = y_XYValues.toArray(new Double[0]);
		return Utils.getMax(d);
	}
	
	protected double getxMinHeight() {
		Double[] d = x_XYValues.toArray(new Double[0]);
		return Utils.getMin(d);
	}
	
	protected double getxMaxHeight() {
		Double[] d = x_XYValues.toArray(new Double[0]);
		return Utils.getMax(d);
	}
	
	/**
	 * Return an array containing points in pixels coordinates to draw. 
	 * @return an int array of point
	 */
	public int[] getPointsArrayToDraw() {
		double xMin = chart.getxMin();
		double yMin = chart.getyMin();
		double dx = chart.getDx();
		double dy = chart.getDy();
		ArrayList<Integer> pointsArray = new ArrayList<Integer>(0);
		int height = chart.getHeight() - chart.getBottomAxisHeight();
		if(chart.isLegendVisible()) height = height - chart.getLegendHeight();
		int min = Math.min(x_XYValues.size(), y_XYValues.size());
		for (int i = 0; i < min; i++) {
			if(Double.compare(x_XYValues.get(i), Double.NaN) != 0 && Double.compare(y_XYValues.get(i), Double.NaN) != 0) {
				pointsArray.add((int) Math.round((x_XYValues.get(i) - xMin)/dx));
				pointsArray.add(height - (int) Math.round((y_XYValues.get(i) - yMin)/dy));
			} //else break;
		} 
		int[] pointsInt = new int[pointsArray.size()];
		for (int i = 0; i < pointsInt.length; i++) pointsInt[i] = pointsArray.get(i);
		return pointsInt;
	}

}
