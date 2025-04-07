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
package org.eclipse.swtchart.extensions.charts;

import java.util.Locale;

import org.eclipse.osgi.util.NLS;

/**
 * Messages
 */
public class Messages extends NLS {
	
	private static String BUNDLE_NAME = "org.eclipse.swtchart.extensions.charts.messages";//$NON-NLS-1$

	/** the menu group for adjust axis range menus */
	public static String ADJUST_AXIS_RANGE_GROUP;
	/** the menu for adjust axis range */
	public static String ADJUST_AXIS_RANGE;;
	/** the menu for adjust X axis range */
	public static String ADJUST_X_AXIS_RANGE;
	/** the menu for adjust Y axis range */
	public static String ADJUST_Y_AXIS_RANGE;
	/** the menu group for zoom in menus */
	public static String ZOOMIN_GROUP;
	/** the menu for zoom in */
	public static String ZOOMIN;
	/** the menu for zoom in X axis */
	public static String ZOOMIN_X;
	/** the menu for zoom in Y axis */
	public static String ZOOMIN_Y;
	/** the menu group for zoom out menus */
	public static String ZOOMOUT_GROUP;
	/** the menu for zoom out */
	public static String ZOOMOUT;
	/** the menu for zoom out X axis */
	public static String ZOOMOUT_X;
	/** the menu for zoom out Y axis */
	public static String ZOOMOUT_Y;
	/** the menu for save as */
	public static String SAVE_AS;
	/** the menu for opening properties dialog */
	public static String PROPERTIES;
	/** the title for save as dialog */
	public static String SAVE_AS_DIALOG_TITLE;
	
	static {
		// load message values from bundle file
		String bn = BUNDLE_NAME;
		Locale locale = Locale.getDefault();
		if (locale.getLanguage().equals(Locale.of("fr").getLanguage())) bn = BUNDLE_NAME + "_fr";
		NLS.initializeMessages(bn, Messages.class);
	}
}
