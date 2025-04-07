/*******************************************************************************
 * Copyright (c) 2008, 2018 SWTChart project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * yoshitaka - initial API and implementation
 *******************************************************************************/
package org.eclipse.swtchart;

/**
 * A line style.
 */
public enum LineStyle {
	/** none */
	NONE("None"),
	/** solid */
	SOLID("Solid"),
	/** dash */
	DASH("Dash"),
	/** dot */
	DOT("Dot"),
	/** dash dot */
	DASHDOT("Dash Dot"),
	/** dash dot dot */
	DASHDOTDOT("Dash Dot Dot");

	/** the label for line style */
	public final String label;

	/**
	 * Constructor.
	 * 
	 * @param label
	 *            the label for line style
	 */
	private LineStyle(String label) {
		this.label = label;
	}
	
	public static LineStyle getLineStyle(String name) {
		if(NONE.name().equals(name)) return NONE;
		if(SOLID.name().equals(name)) return SOLID;
		if(DASH.name().equals(name)) return DASH;
		if(DOT.name().equals(name)) return DOT;
		if(DASHDOT.name().equals(name)) return DASHDOT;
		if(DASHDOTDOT.name().equals(name)) return DASHDOTDOT;
		return DOT;
	}
}