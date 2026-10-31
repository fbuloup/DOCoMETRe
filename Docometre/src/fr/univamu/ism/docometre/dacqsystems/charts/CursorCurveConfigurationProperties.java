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
package fr.univamu.ism.docometre.dacqsystems.charts;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Color;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.chartxyz.CursorShape;
import fr.univamu.ism.docometre.dacqsystems.Property;

/*
 * Properties of a curve of a cursor 2D chart: symbol of the last XY value received.
 * The fill color of the shapes is the color of the curve (CurveConfigurationProperties.COLOR).
 */
public class CursorCurveConfigurationProperties extends Property {
	
	public final static String IMAGE = "IMAGE";
	public final static String[] SHAPES = shapes();
	
	public final static String shapeRegExpValues = "^(" + String.join("|", SHAPES) + ")$";
	public final static String shapeAvailableValues = String.join(":", SHAPES);
	
	public static final CursorCurveConfigurationProperties SHAPE = new CursorCurveConfigurationProperties("CursorCurveConfigurationProperties.SHAPE", DocometreMessages.CursorShape_Label, DocometreMessages.CursorShape_Tooltip, shapeRegExpValues, shapeAvailableValues);
	public static final CursorCurveConfigurationProperties SIZE = new CursorCurveConfigurationProperties("CursorCurveConfigurationProperties.SIZE", DocometreMessages.CursorSize_Label, DocometreMessages.CursorSize_Tooltip, "^[1-9][0-9]{0,2}$");
	public static final CursorCurveConfigurationProperties OUTLINE_COLOR = new CursorCurveConfigurationProperties("CursorCurveConfigurationProperties.OUTLINE_COLOR", DocometreMessages.CursorOutlineColor_Label, DocometreMessages.CursorOutlineColor_Tooltip, CurveConfigurationProperties.COLOR.getRegExp());
	public static final CursorCurveConfigurationProperties OUTLINE_WIDTH = new CursorCurveConfigurationProperties("CursorCurveConfigurationProperties.OUTLINE_WIDTH", DocometreMessages.CursorOutlineWidth_Label, DocometreMessages.CursorOutlineWidth_Tooltip, "^(([1-9][0-9]?)|(0))(?:\\.[0-9]+)?$");
	public static final CursorCurveConfigurationProperties IMAGE_FILE = new CursorCurveConfigurationProperties("CursorCurveConfigurationProperties.IMAGE_FILE", DocometreMessages.CursorImage_Label, DocometreMessages.CursorImage_Tooltip, "^.*$");
	
	/*
	 * Geometric shapes of the chart library, then IMAGE
	 */
	private static String[] shapes() {
		CursorShape[] cursorShapes = CursorShape.values();
		String[] shapes = new String[cursorShapes.length + 1];
		for (int i = 0; i < cursorShapes.length; i++) shapes[i] = cursorShapes[i].name();
		shapes[cursorShapes.length] = IMAGE;
		return shapes;
	}
	
	public static void populateProperties(CursorCurveConfiguration cursorCurveConfiguration){
		cursorCurveConfiguration.setProperty(SHAPE, CursorShape.CIRCLE.name());
		cursorCurveConfiguration.setProperty(SIZE, "15");
		cursorCurveConfiguration.setProperty(OUTLINE_COLOR, "RGB {255, 255, 255}");
		cursorCurveConfiguration.setProperty(OUTLINE_WIDTH, "0");
		cursorCurveConfiguration.setProperty(IMAGE_FILE, "");
	}
	
	public static CursorCurveConfiguration clone(CursorCurveConfiguration cursorCurveConfiguration) {
		CursorCurveConfiguration newCursorCurveConfiguration = new CursorCurveConfiguration(cursorCurveConfiguration.getXChannel(), cursorCurveConfiguration.getYChannel());
		CurveConfigurationProperties.clone(cursorCurveConfiguration, newCursorCurveConfiguration);
		newCursorCurveConfiguration.setProperty(XYCurveConfigurationProperties.X_CHANNEL_NAME, new String(cursorCurveConfiguration.getProperty(XYCurveConfigurationProperties.X_CHANNEL_NAME)));
		newCursorCurveConfiguration.setProperty(XYCurveConfigurationProperties.Y_CHANNEL_NAME, new String(cursorCurveConfiguration.getProperty(XYCurveConfigurationProperties.Y_CHANNEL_NAME)));
		newCursorCurveConfiguration.setProperty(SHAPE, new String(cursorCurveConfiguration.getProperty(SHAPE)));
		newCursorCurveConfiguration.setProperty(SIZE, new String(cursorCurveConfiguration.getProperty(SIZE)));
		newCursorCurveConfiguration.setProperty(OUTLINE_COLOR, new String(cursorCurveConfiguration.getProperty(OUTLINE_COLOR)));
		newCursorCurveConfiguration.setProperty(OUTLINE_WIDTH, new String(cursorCurveConfiguration.getProperty(OUTLINE_WIDTH)));
		newCursorCurveConfiguration.setProperty(IMAGE_FILE, new String(cursorCurveConfiguration.getProperty(IMAGE_FILE)));
		return newCursorCurveConfiguration;
	}
	
	/*
	 * Outline color, "RGB {r, g, b}" (white by default)
	 */
	public static Color getOutlineColor(CursorCurveConfiguration cursorCurveConfiguration) {
		return ChartConfigurationProperties.getColor(cursorCurveConfiguration, OUTLINE_COLOR);
	}
	
	/*
	 * Image file: absolute path, or path relative to the workspace (null if there is none)
	 */
	public static File getImageFile(CursorCurveConfiguration cursorCurveConfiguration) {
		return resolveImageFile(cursorCurveConfiguration.getProperty(IMAGE_FILE));
	}
	
	/*
	 * File of a value of IMAGE_FILE (null if it is empty)
	 */
	public static File resolveImageFile(String path) {
		if(path == null || path.trim().isEmpty()) return null;
		File file = new File(path.trim());
		if(file.isAbsolute()) return file;
		return new File(getWorkspacePath(), path.trim());
	}
	
	/*
	 * Value of IMAGE_FILE for a file chosen by the user: relative to the workspace if it is inside it
	 * (the experiments remain valid when the workspace is moved), absolute otherwise
	 */
	public static String toImageFileProperty(String path) {
		File workspace = new File(getWorkspacePath()).getAbsoluteFile();
		File file = new File(path).getAbsoluteFile();
		String workspacePath = workspace.getPath() + File.separator;
		if(file.getPath().startsWith(workspacePath)) return file.getPath().substring(workspacePath.length());
		return file.getPath();
	}
	
	private static String getWorkspacePath() {
		return new File(Platform.getInstanceLocation().getURL().getPath()).getPath();
	}
	
	private CursorCurveConfigurationProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private CursorCurveConfigurationProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}

}
