/*******************************************************************************
 * Copyright (c) 2017, 2018 Lablicate GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Dr. Philip Wenig - initial API and implementation
 *******************************************************************************/
package org.eclipse.swtchart.extensions.menu.export;

public class TSVExportHandler extends AbstractSeparatedValueHandler implements ISeriesExportConverter {

	private static final String FILE_EXTENSION = "*.tsv"; //$NON-NLS-1$
	public static final String NAME = Messages.TSVExportHandler_1 + FILE_EXTENSION + ")"; //$NON-NLS-2$
	//
	private static final String TITLE = Messages.TSVExportHandler_3;
	private static final String DELIMITER = "\t"; //$NON-NLS-1$

	@Override
	public String getName() {

		return NAME;
	}

	public TSVExportHandler() {
		super(TITLE, FILE_EXTENSION, DELIMITER);
	}
}
