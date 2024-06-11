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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [01/06/2024]
 ******************************************************************************/
package fr.univamu.ism.nrtswtchart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

public class RTSWTXYSerie {

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

	private RTSWTXYChart rtswtChart;
	private boolean modified;
	private String title;
	private Color color;

	protected RTSWTXYSerie(RTSWTXYChart rtswtChart, String title, Color color) {
		if(rtswtChart == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(title == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(title.equals("")) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		if(color == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(color.isDisposed()) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		this.rtswtChart = rtswtChart;
		this.title = title;
		this.color = color;
	}

	protected Color getColor() {
		return color;
	}

	protected String getTitle() {
		return title;
	}

	public void addPoints(final Double[] x, final Double[] y) {

		if(y_XYValues == null || x_XYValues == null) reset();

		if(y.length != 0) y_XYValues_Buffer.addAll(Arrays.asList(y));
		if(x.length != 0) x_XYValues_Buffer.addAll(Arrays.asList(x));

		if(y_XYValues_Buffer.size() == 0 || x_XYValues_Buffer.size() == 0) return;

		int maxIndex = Math.min(y_XYValues_Buffer.size(), x_XYValues_Buffer.size());
		xExtractedValues = x_XYValues_Buffer.subList(0, maxIndex);
		yExtractedValues = y_XYValues_Buffer.subList(0, maxIndex);

		xBuffer = xExtractedValues.toArray(new Double[maxIndex]);
		yBuffer = yExtractedValues.toArray(new Double[maxIndex]);

		try {
			for (int i = 0; i < maxIndex; i++) {
				x_XYValues_Buffer.remove(0);
				y_XYValues_Buffer.remove(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		double dx = rtswtChart.getDx();
		double dy = rtswtChart.getDy();

		for (int i = 0; i < xBuffer.length; i++) {
			int nbPixelsX = Math.abs((int)((xBuffer[i] - lastPointX) / dx));
			int nbPixelsY = Math.abs((int)((yBuffer[i] - lastPointY) / dy));
			boolean addPoint = nbPixelsX > 0 || nbPixelsY > 0;
			if(addPoint) {
				addValue(xBuffer[i], yBuffer[i]);//, nbPixelsX, nbPixelsY);
				setModified(true);
			}
		}

		rtswtChart.checkUpdate();
	}

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

	private void addValue(double x, double y) {
		x_XYValues.add(x);
		y_XYValues.add(y);
		lastPointX = x;
		lastPointY = y;
	}

	protected boolean getModified() {
		return this.modified;
	}

	protected void setModified(boolean modified) {
		this.modified = modified;
	}

	protected double getyMin() {
		Double[] d = y_XYValues.toArray(new Double[y_XYValues.size()]);
		return RTSWTChartUtils.getMin(d);
	}

	protected double getyMax() {
		Double[] d = y_XYValues.toArray(new Double[y_XYValues.size()]);
		return RTSWTChartUtils.getMax(d);
	}

	protected double getxMin() {
		Double[] d = x_XYValues.toArray(new Double[x_XYValues.size()]);
		return RTSWTChartUtils.getMin(d);
	}

	protected double getxMax() {
		Double[] d = x_XYValues.toArray(new Double[x_XYValues.size()]);
		return RTSWTChartUtils.getMax(d);
	}

	protected int[] getPointsArrayToDraw() {
		double xMin = rtswtChart.getxMin();
		double yMin = rtswtChart.getyMin();
		double dx = rtswtChart.getDx();
		double dy = rtswtChart.getDy();
		ArrayList<Integer> pointsArray = new ArrayList<Integer>(0);
		int height = rtswtChart.getHeight() - rtswtChart.getBottomAxisHeight();
		if(rtswtChart.isLegendVisible()) height = height - rtswtChart.getLegendHeight();
		int min = Math.min(x_XYValues.size(), y_XYValues.size());
		for (int i = 0; i < min; i++) {
			if(Double.compare(x_XYValues.get(i), Double.NaN) != 0 && Double.compare(y_XYValues.get(i), Double.NaN) != 0) {
				int vx = (int) Math.round((x_XYValues.get(i) - xMin)/dx);
				int vy = height - (int) Math.round((y_XYValues.get(i) - yMin)/dy) - 1;
				pointsArray.add(vx);
				pointsArray.add(vy);
			} //else break;
		} 
		int[] pointsInt = new int[pointsArray.size()];
		for (int i = 0; i < pointsInt.length; i++) pointsInt[i] = pointsArray.get(i);
		return pointsInt;
	}

//	protected double getCurrentValue() {
//		if(currentIndex > -1) return yValues[currentIndex];
//		return Double.NaN;
//	}

}