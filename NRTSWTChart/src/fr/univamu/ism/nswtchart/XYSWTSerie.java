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
package fr.univamu.ism.nswtchart;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;

import fr.univamu.ism.nrtswtchart.RTSWTChartUtils;

public class XYSWTSerie {

	private double[] yValues;
	private double[] xValues;
	private XYSWTChart xyswtChart;
	private String title;
	private Color color;
	private int thickness;
	
	protected XYSWTSerie(double[] xValues, double[] yValues, XYSWTChart xyswtChart, String title, Color color, int thickness) {
		if(xValues == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(yValues == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(xValues.length != yValues.length) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		if(xyswtChart == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(title == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(title.equals("")) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		if(color == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(color.isDisposed()) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		if(thickness <= 0) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		this.xValues = xValues;
		this.yValues = yValues;
		this.xyswtChart = xyswtChart;
		this.title = title;
		this.color = color;
		this.thickness = thickness;
	}

	protected Color getColor() {
		return color;
	}

	public String getTitle() {
		return title;
	}

	public int getThickness() {
		return thickness;
	}

	public void setThickness(int thickness) {
		this.thickness = thickness;
	}

	protected double getyMin() {
		return RTSWTChartUtils.getMin(yValues);
	}

	protected double getyMax() {
		return RTSWTChartUtils.getMax(yValues);
	}

	protected double getxMin() {
		return RTSWTChartUtils.getMin(xValues);
	}

	protected double getxMax() {
		return RTSWTChartUtils.getMax(xValues);
	}
	
	protected ArrayList<int[]> getPointsArrayToDraw() {
		Point previousPoint = null;
		Window window = xyswtChart.getWindow();
		ArrayList<ArrayList<Integer>> allPointsArray = new ArrayList<ArrayList<Integer>>(0);
		ArrayList<Integer> pointsArray = null;
		boolean wasInside = false;
		boolean addPoint = false;
		for (int i = 0; i < xValues.length; i++) {
			if(window.isInside(xValues[i], yValues[i])) {
				if(!wasInside) {
					pointsArray = new ArrayList<Integer>(0);
					allPointsArray.add(pointsArray);
					wasInside = true;
				}
				int xPixel = xyswtChart.xValueToPixel(xValues[i]);
				int yPixel = xyswtChart.yValueToPixel(yValues[i]);
				addPoint = false;
				if(previousPoint == null || xPixel != previousPoint.x || yPixel != previousPoint.y) {
					previousPoint = new Point(xPixel, yPixel);
					addPoint = true;
				}
				if(addPoint) {
					pointsArray.add(xPixel);
					pointsArray.add(yPixel);
				}
			} else wasInside = false;
		}
		ArrayList<int[]> allPointsInt = new ArrayList<int[]>(0);
		for (ArrayList<Integer> currentPointsArray : allPointsArray) {
			int[] pointsInt = new int[currentPointsArray.size()];
			for (int i = 0; i < pointsInt.length; i++) pointsInt[i] = currentPointsArray.get(i);
			allPointsInt.add(pointsInt);
		}
		return allPointsInt;
	}

	public double getYValue(double xValue) {
		double yValue = Double.NaN;
		for (int i = 0; i < xValues.length - 1; i++) {
			if(xValues[i] <= xValue && xValue <= xValues[i+1]) {
				yValue = (xValue - xValues[i])/(xValues[i+1] - xValues[i])*(yValues[i+1] - yValues[i]) + yValues[i];
			}
		}
		return yValue;
	}
	
}