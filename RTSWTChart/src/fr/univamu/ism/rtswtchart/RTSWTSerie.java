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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

public abstract class RTSWTSerie {
	/**
	 * Values in pixels width. 
	 */
	protected double[] yValues;
	
	/**
	 * The chart that serie belongs to
	 */
	protected RTSWTChart chart;
	/**
	 * Serie ID. Will be displayed in legend chart area
	 */
	protected String id;
	/**
	 * Serie color
	 */
	protected Color lineColor;
	/**
	 * Serie style
	 */
	protected int lineStyle;
	/**
	 * Serie width
	 */
	protected int lineWidth;
	/**
	 * Flag to tell if new values has been sent to the serie
	 */
	private boolean modified;
	
	public RTSWTSerie(RTSWTChart chart, String id, Color lineColor, int lineStyle, int lineWidth) {
		if(chart == null) SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, " chart cannot be null");
		if(id == null) SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, " serie ID cannot be null");
		if(id.equals("")) SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, " serie ID cannot be empty");
		if(!(lineStyle == SWT.LINE_DASH || lineStyle == SWT.LINE_DASHDOT || lineStyle == SWT.LINE_DASHDOTDOT || 
				lineStyle == SWT.LINE_DOT || lineStyle == SWT.LINE_SOLID)) SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, " bad line style");
		if(lineWidth <= 0) SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, " serie width cannot be negative or null");
		this.id = id;
		this.lineColor = lineColor;
		this.chart = chart;
		this.lineStyle = lineStyle;
		this.lineWidth = lineWidth;
	}
	
	/**
	 * Serie is modified if new points have been added
	 * @return true or false
	 */
	protected boolean getModified() {
		return this.modified;
	}
	
	/**
	 * Set modified flag of the serie
	 * @param modified true or false
	 */
	protected void setModified(boolean modified) {
		this.modified = modified;
	}
	
	/**
	 * Return the colour of the line serie
	 * @return line serie colour
	 */
	public Color getLineColor() {
		return lineColor;
	}

	/**
	 *  Return the width of the line serie
	 * @return line serie width
	 */
	public int getLineWidth() {
		return lineWidth;
	}

	/**
	 *  Return the style of the line serie
	 * @return lines serie style
	 */
	public int getLineStyle() {
		return lineStyle;
	}
	
	/**
	 * Return the serie ID
	 * @return id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Return the minimum value in pixel coordinate of the history
	 * @return minimum value
	 */
	protected double getyMinHeight() {
		return Utils.getMin(yValues);
	}

	/**
	 * Return the maximum value in pixel coordinate of the history
	 * @return maximum value
	 */
	protected double getyMaxHeight() {
		return Utils.getMax(yValues);
	}
	
	protected abstract void reset();
}
