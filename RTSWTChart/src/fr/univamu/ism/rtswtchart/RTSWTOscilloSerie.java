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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * This class is intended to be used in an RTSWTOscilloChart. It cannot be instantiated.
 * Just call {@link #addPoints(Double[], Double[])} method in order to add
 * new values to the serie.
 * @author frank buloup
 *
 */
public final class RTSWTOscilloSerie extends RTSWTSerie {
	/**
	 * Current position in values array yValues
	 */
	private int currentIndex = -1;
	/**
	 * Most recent real time value
	 */
	private double lastPointTime = Double.NEGATIVE_INFINITY;
	
	/**
	 * This constructor is used within corresponding chart class.
	 * 
	 * @param chart parent chart
	 * @param id name of the serie. Must be unique in the parent chart or an error is raised
	 * @param lineColor colour of the line serie
	 * @param lineStyle style of the line serie
	 * @param lineWidth width of the line serie
	 */
	protected RTSWTOscilloSerie(RTSWTChart chart, String id, Color lineColor, int lineStyle, int lineWidth) {
		super(chart, id, lineColor, lineStyle, lineWidth);
		if(id.startsWith(HORIZONTAL_REFERENCE)) {
			String value = id.split("\\[")[1].replaceAll("\\]", "");
			yValues = new double[1];
			yValues[0] = Double.parseDouble(value);
			isHorizontalReference = true;
			currentIndex = 0;
			setModified(true);
		}
	}
	
	@Override
	protected void reset() {
		if(isHorizontalReference) return;
		yValues = new double[chart.getWidth()];
		for (int i = 0; i < yValues.length; i++) yValues[i] = Double.NaN;
		currentIndex = -1;
		if(lastPointTime == Double.NEGATIVE_INFINITY) lastPointTime = - ((RTSWTOscilloChart)chart).getDx();
		setModified(false);
	}
	
	/**
	 * Add a new value over nbPixels range
	 * @param Y
	 * @param nbPixels
	 */
	private void addValue(double Y, int nbPixels) {
		if(yValues.length == 0) return;
		int width = chart.getWidth() - chart.getLeftAxisWidth();
		double dx = ((RTSWTOscilloChart)chart).getDx();
		int localCurrentIndex = 0;
		for (int i = 1; i <= nbPixels; i++) {
			localCurrentIndex = (currentIndex + i) % width;
			yValues[localCurrentIndex] = Y;
		}
		currentIndex = (currentIndex + nbPixels) % width;
		lastPointTime = lastPointTime + nbPixels*dx;
	}
	
	/**
	 * Add new values to the serie. Values
	 * will be filtered in order to be painted within chart screen dimensions.
	 * @param x time values
	 * @param y amplitude values
	 */
	public void addPoints(final Double[] x, final Double[] y) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				if(chart.isDisposed()) return;
				if(yValues == null) reset();
				double dx = chart.getDx();
				for (int i = 0; i < x.length; i++) {
					int nbPixels = (int)((x[i] - lastPointTime) / dx);
					boolean addPoint = nbPixels > 0;
					if(addPoint) {
						addValue(y[i], nbPixels);
						setModified(true);
					}
				}
				chart.render();
			}
		});
	}
	
	/**
	 * Return an array containing points in pixels coordinates to draw. 
	 * @return an int array of point
	 */
	public int[] getPointsArrayToDraw() {
		ArrayList<Integer> pointsArray = new ArrayList<Integer>(0);
		int height = chart.getHeight() - chart.getBottomAxisHeight();
		if(chart.isLegendVisible()) height = height - chart.getLegendHeight();
		double yMin = ((RTSWTOscilloChart)chart).getyMin();
		double dy = ((RTSWTOscilloChart)chart).getDy();
		for (int i = 0; i < yValues.length; i++) {
			if(Double.compare(yValues[i], Double.NaN) != 0) {
				pointsArray.add(i);
				pointsArray.add(height - (int) Math.round((yValues[i] - yMin)/dy));
			} else break;
		} 
		int[] pointsInt = new int[pointsArray.size()];
		for (int i = 0; i < pointsInt.length; i++) pointsInt[i] = pointsArray.get(i);
		return pointsInt;
	}

	/**
	 * Return the current index
	 * @return int
	 */
	public int getCurrentIndex() {
		return currentIndex;
	}
	
	public double getCurrentValue() {
		if(currentIndex > -1) return yValues[currentIndex];
		return Double.NaN;
	}
	
}
