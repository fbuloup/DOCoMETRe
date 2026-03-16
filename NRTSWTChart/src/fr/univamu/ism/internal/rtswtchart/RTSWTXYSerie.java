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
package fr.univamu.ism.internal.rtswtchart;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import fr.univamu.ism.internal.nrtswtchart.RTSWTChartUtils;


public class RTSWTXYSerie extends RTSWTSerie {
	
	
	private double[] xValues;
	private double[] yValues;
	private double[] timeValues;
	private double lastTime;
	private int currentIndex;
	
	private int nbHistoryPoints = 100;

	/**
	 * Most recent real time values
	 */
	private double lastPointX = Double.NEGATIVE_INFINITY;
	private double lastPointY = Double.NEGATIVE_INFINITY;
	
	private RTSWTXYChart chart;
	
//	/**
//	 * Values in x axis. 
//	 */
//	protected ArrayList<Double> x_XYValues;
//	/**
//	 * Values in y axis. 
//	 */
//	protected ArrayList<Double> y_XYValues;
//	
//	private  ArrayList<Double> x_XYValues_Buffer;
//	private  ArrayList<Double> y_XYValues_Buffer;
//	private  List<Double> xExtractedValues;
//	private  List<Double> yExtractedValues;
//	private Double[] xBuffer;
//	private Double[] yBuffer;
	
	
	
	
	protected RTSWTXYSerie(RTSWTChart chart, String id, Color lineColor, int lineStyle, int lineWidth) {
		super(chart, id, lineColor, lineStyle, lineWidth);
		this.chart = (RTSWTXYChart) chart;
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
				
				if(xValues == null || yValues == null) 
					reset();
				double dx = chart.getDx();
				double dy = chart.getDy();
				int dn = 1;
				for (int i = 0; i < x.length; i++) {
					int nbPixelsX = Math.abs((int)((x[i] - lastPointX) / dx));
					int nbPixelsY = Math.abs((int)((y[i] - lastPointY) / dy));
					if(nbPixelsX > 0 || nbPixelsY > 0 || Double.isNaN(xValues[0])) {
						addValue(x[i], y[i], dn);
						setModified(true);
						dn = 0;
					}
					dn++;
				}
				
				chart.render();
			}
		});
		
	}

	@Override
	protected void reset() {
		nbHistoryPoints = (int)(chart.getSampleFrequency()*chart.getHistorySize());
		xValues = new double[nbHistoryPoints];
		yValues = new double[nbHistoryPoints];
		timeValues = new double[nbHistoryPoints];
		for(int i = 0; i < nbHistoryPoints; i++) {
			xValues[i] = Double.NaN;
			yValues[i] = Double.NaN;
			timeValues[i] = 0;
		}
		lastPointX = - chart.getDx();
		lastPointY = - chart.getDy();
		currentIndex = 0;
		lastTime = -1/chart.getSampleFrequency();
		setModified(false);
	}
	
	/**
	 * Add a new value over nbPixels range
	 * @param X
	 * @param Y
	 * @param nbPixelsX
	 * @param nbPixelsY
	 */
	private void addValue(double x, double y, int dn) {
//		System.out.println("Add point with dn : " + dn);
//		System.out.println("currentIndex before : " + currentIndex + " - nbHistoryPoints : " + nbHistoryPoints);
		
		if(currentIndex >= nbHistoryPoints) {
			System.arraycopy(xValues, 1, xValues, 0, xValues.length-1);
			System.arraycopy(yValues, 1, yValues, 0, yValues.length-1);
			System.arraycopy(timeValues, 1, timeValues, 0, timeValues.length-1);
			currentIndex = nbHistoryPoints - 1;
		}
		
		xValues[currentIndex] = x;
		yValues[currentIndex] = y;
		timeValues[currentIndex] = lastTime + 1.0*dn/chart.getSampleFrequency();
		
		double dt = timeValues[currentIndex] - timeValues[0];
		while (dt > chart.getHistorySize()) {
			System.arraycopy(xValues, 1, xValues, 0, xValues.length - 1);
			System.arraycopy(yValues, 1, yValues, 0, yValues.length - 1);
			System.arraycopy(timeValues, 1, timeValues, 0, timeValues.length - 1);
			currentIndex = currentIndex - 1;
			dt = timeValues[currentIndex] - timeValues[0];
		}
		
//		System.out.println("dt : " + dt);
		
		lastPointX = x;
		lastPointY = y;
		lastTime = timeValues[currentIndex];
		currentIndex ++;
//		System.out.println("currentIndex after : " + currentIndex + " - nbHistoryPoints : " + nbHistoryPoints);
}
	
//	public int getNumberOfPoints() {
//		return x_XYValues.size();
//	}
	
	@Override
	protected double getyMin() {
		return RTSWTChartUtils.getMin(yValues);
	}
	
	@Override
	protected double getyMax() {
		return RTSWTChartUtils.getMax(yValues);
	}
	
	protected double getxMin() {
		return RTSWTChartUtils.getMin(xValues);
	}
	
	protected double getxMax() {
		return RTSWTChartUtils.getMax(xValues);
	}
	
	/**
	 * Return an array containing points in pixels coordinates to draw. 
	 * @return an int array of point
	 */
	protected int[] getPointsArrayToDraw() {
		double xMin = chart.getxMin();
		double yMin = chart.getyMin();
		double dx = chart.getDx();
		double dy = chart.getDy();
		ArrayList<Integer> pointsArray = new ArrayList<Integer>(0);
		int height = chart.getHeight() - 1 - chart.getBottomAxisHeight() - chart.getLegendHeight();
		for (int i = 0; i < currentIndex; i++) {
			if(i < xValues.length && !Double.isNaN(xValues[i])) {
				int vx = (int) Math.round((xValues[i] - xMin)/dx);
				int vy = height - (int) Math.round((yValues[i] - yMin)/dy);
				pointsArray.add(vx);
				pointsArray.add(vy);
			}
		}
		int[] pointsInt = new int[pointsArray.size()];
		for (int i = 0; i < pointsInt.length; i++) pointsInt[i] = pointsArray.get(i);
		return pointsInt;
	}

}
