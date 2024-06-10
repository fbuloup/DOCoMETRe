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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class RTSWTOscilloSerie {

	public static final String HORIZONTAL_REFERENCE = "Horizontal Reference";

	private double[] yValues;
	private RTSWTOscilloChart rtswtChart;
	private int currentIndex;
	private double lastPointTime = Double.NEGATIVE_INFINITY;
	private boolean modified;
	private String title;
	private Color color;
	/**
	 * If true, serie will be a simple horizontal line at value specified in yValues[0]
	 */
	private boolean isHorizontalReference;

	protected RTSWTOscilloSerie(RTSWTOscilloChart rtswtChart, String title, Color color) {
		if(rtswtChart == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(title == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(title.equals("")) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		if(color == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if(color.isDisposed()) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		this.rtswtChart = rtswtChart;
		this.title = title;
		this.color = color;
		if(title.startsWith(HORIZONTAL_REFERENCE)) {
			String value = title.split("\\[")[1].replaceAll("\\]", "");
			yValues = new double[1];
			yValues[0] = Double.parseDouble(value);
			isHorizontalReference = true;
			currentIndex = 0;
			setModified(true);
		}
	}

	protected Color getColor() {
		return color;
	}

	protected String getTitle() {
		return title;
	}

	public boolean isHorizontalReference() {
		return isHorizontalReference;
	}

	public void addPoints(final Double[] x, final Double[] y) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				if(rtswtChart.getChart().isDisposed()) return;
				if(yValues == null) reset();
				double dx = rtswtChart.getDx();
				for (int i = 0; i < x.length; i++) {
					int nbPixels = (int)((x[i] - lastPointTime) / dx);
					boolean addPoint = nbPixels > 0;
					if(addPoint) {
						addValue(y[i], nbPixels);
						setModified(true);
					}
				}
			}
		});

		rtswtChart.checkUpdate();
	}

	protected void reset() {
		if(isHorizontalReference) return;
		yValues = new double[rtswtChart.getWidth()];
		for (int i = 0; i < yValues.length; i++) yValues[i] = Double.NaN;
		currentIndex = -1;
		if(lastPointTime == Double.NEGATIVE_INFINITY) lastPointTime = - rtswtChart.getDx();
		setModified(false);
	}

	private void addValue(double Y, int nbPixels) {
		if(yValues.length == 0) return;
		int width = rtswtChart.getWidth() - rtswtChart.getLeftAxisWidth();
		double dx = rtswtChart.getDx();
		int localCurrentIndex = 0;
		for (int i = 1; i <= nbPixels; i++) {
			localCurrentIndex = (currentIndex + i) % width;
			yValues[localCurrentIndex] = Y;
		}
		currentIndex = (currentIndex + nbPixels) % width;
		lastPointTime = lastPointTime + nbPixels*dx;
	}

	protected boolean getModified() {
		return this.modified;
	}

	protected void setModified(boolean modified) {
		this.modified = modified;
	}

	protected double getyMin() {
		return Utils.getMin(yValues);
	}

	protected double getyMax() {
		return Utils.getMax(yValues);
	}

	protected int[] getPointsArrayToDraw() {
		ArrayList<Integer> pointsArray = new ArrayList<Integer>(0);
		int height = rtswtChart.getHeight() - rtswtChart.getBottomAxisHeight();
		height = height - rtswtChart.getLegendHeight();
		double yMin = rtswtChart.getyMin();
		double dy = rtswtChart.getDy();
		for (int i = 0; i < yValues.length; i++) {
			if(Double.compare(yValues[i], Double.NaN) != 0) {
				int value = height - (int) Math.round((yValues[i] - yMin)/dy);
				value = (value >= height)?height-1:value;
				value = (value < 0)?0:value;
				pointsArray.add(i);
				pointsArray.add(value);
			} else break;
		} 
		int[] pointsInt = new int[pointsArray.size()];
		for (int i = 0; i < pointsInt.length; i++) pointsInt[i] = pointsArray.get(i);
		return pointsInt;
	}

	protected double getCurrentValue() {
		if(currentIndex > -1) return yValues[currentIndex];
		return Double.NaN;
	}

}