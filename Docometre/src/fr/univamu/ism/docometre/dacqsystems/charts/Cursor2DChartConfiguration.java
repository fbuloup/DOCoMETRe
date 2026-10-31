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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.chartxyz.CursorShape;
import fr.univamu.ism.docometre.chartxyz.CursorStyle;
import fr.univamu.ism.docometre.chartxyz.Curve2DData;
import fr.univamu.ism.docometre.chartxyz.GLCursorChart2D;
import fr.univamu.ism.docometre.chartxyz.macos.NewtCursorChart2D;
import fr.univamu.ism.docometre.chartxyz.windows.CursorChart2D;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;

/*
 * XY chart without history: only the last value received by each curve is displayed, as a symbol
 * (shape, size, fill and outline colors, or PNG/JPEG image) configured for each curve.
 * Same chart properties as the XY chart, except the history size. Requires the OpenGL V2 charts;
 * otherwise, a standard XY chart is displayed.
 */
public class Cursor2DChartConfiguration extends XYChartConfiguration {

	private static final long serialVersionUID = 1L;

	public Cursor2DChartConfiguration() {
		super(ChartTypes.CURSOR_2D_CHART);
	}

	@Override
	protected boolean hasHistorySize() {
		return false;
	}

	@Override
	public CurveConfiguration[] createCurvesConfiguration(IStructuredSelection selection) {
		Object[] elements = selection.toArray();
		CursorCurveConfiguration cursorCurveConfiguration = new CursorCurveConfiguration((Channel)elements[0], (Channel)elements[1]);
		addCurveConfiguration(cursorCurveConfiguration);
		return new CurveConfiguration[] {cursorCurveConfiguration};
	}

	@Override
	public void createChart(Composite chartContainer) {
		if(!Activator.useOpenGLV2()) {
			Activator.logWarningMessage(DocometreMessages.Cursor2DChartOpenGLV1Warning);
			super.createChart(chartContainer);
			return;
		}
		// Clean series
		for (CurveConfiguration curveConfiguration : curvesConfigurations) {
			((XYCurveConfiguration)curveConfiguration).setSerie(null);
		}
		// Create chart
		int hSpan = Integer.parseInt(getProperty(ChartConfigurationProperties.HORIZONTAL_SPANNING));
		int vSpan = Integer.parseInt(getProperty(ChartConfigurationProperties.VERTICAL_SPANNING));
		boolean autoscale = Boolean.parseBoolean(getProperty(XYChartConfigurationProperties.AUTO_SCALE));
		double xMax = Double.parseDouble(getProperty(XYChartConfigurationProperties.X_MAX));
		double xMin = Double.parseDouble(getProperty(XYChartConfigurationProperties.X_MIN));
		double yMax = Double.parseDouble(getProperty(XYChartConfigurationProperties.Y_MAX));
		double yMin = Double.parseDouble(getProperty(XYChartConfigurationProperties.Y_MIN));
		
		GLCursorChart2D chart = null;
		if (Platform.OS_MACOSX.equals(Platform.getOS())) chart = new NewtCursorChart2D(chartContainer, xMin, xMax, yMin, yMax, new Curve2DData[0]);
		else chart = new CursorChart2D(chartContainer, xMin, xMax, yMin, yMax, new Curve2DData[0]);
		chart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, hSpan, vSpan));
		chart.setAutoScale(autoscale);
		chart.setGridVisibility(true);
		chart.setLegendVisibility(true);
		chart.setLegendPosition(SWT.BOTTOM);
		// Real-time display: no markers, neither with the mouse nor in the context menu
		chart.setMarkersInteractive(false);
		chart.setMarkersMenuItemsVisible(false);
		
		// Create curves
		for (CurveConfiguration curveConfiguration : curvesConfigurations) {
			CursorCurveConfiguration cursorCurveConfiguration = (CursorCurveConfiguration)curveConfiguration;
			String xSerieID = cursorCurveConfiguration.getXChannel().getID();
			String ySerieID = cursorCurveConfiguration.getYChannel().getID();
			String sfxString = cursorCurveConfiguration.getXChannel().getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			String sfyString = cursorCurveConfiguration.getYChannel().getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			double sfx = Double.parseDouble(sfxString);
			double sfy = Double.parseDouble(sfyString);
			if(sfx != sfy) {
				Activator.logErrorMessage(NLS.bind(DocometreMessages.ChannelSampleFrequency, xSerieID, sfxString));
				Activator.logErrorMessage(NLS.bind(DocometreMessages.ChannelSampleFrequency, ySerieID, sfyString));
				Activator.logErrorMessage(DocometreMessages.Cursor2DChartDifferentFrequencies);
				continue;
			}
			Color color = CurveConfigurationProperties.getColor(cursorCurveConfiguration);
			Curve2DData curve = new Curve2DData(ySerieID + "(" + xSerieID + ")", sfx);
			curve.setColor(color.getRed(), color.getGreen(), color.getBlue());
			chart.addCurve(curve);
			chart.setCursorStyle(curve, createCursorStyle(cursorCurveConfiguration));
			cursorCurveConfiguration.setSerie(curve);
		}
	}
	
	/*
	 * Symbol of a curve: the image if the shape is IMAGE (a disk if it cannot be read),
	 * otherwise the shape filled with the color of the curve, outlined if the outline width is not zero
	 */
	public static CursorStyle createCursorStyle(CursorCurveConfiguration cursorCurveConfiguration) {
		float size = parseFloat(cursorCurveConfiguration.getProperty(CursorCurveConfigurationProperties.SIZE), CursorStyle.DEFAULT_SIZE);
		size = Math.max(1, Math.min(CursorStyle.MAX_SIZE, size));
		String shapeName = cursorCurveConfiguration.getProperty(CursorCurveConfigurationProperties.SHAPE);
		if(CursorCurveConfigurationProperties.IMAGE.equals(shapeName)) {
			File file = CursorCurveConfigurationProperties.getImageFile(cursorCurveConfiguration);
			if(file != null) {
				try {
					return CursorStyle.image(file.getPath(), size);
				} catch (IllegalArgumentException e) {
					Activator.logErrorMessage(NLS.bind(DocometreMessages.CursorImageUnreadable, file.getPath(), e.getMessage()));
				}
			} else Activator.logErrorMessage(NLS.bind(DocometreMessages.CursorNoImageFile, cursorCurveConfiguration.getYChannel().getID() + "(" + cursorCurveConfiguration.getXChannel().getID() + ")"));
			shapeName = CursorShape.CIRCLE.name();
		}
		CursorShape shape = CursorShape.CIRCLE;
		try {
			if(shapeName != null) shape = CursorShape.valueOf(shapeName);
		} catch (IllegalArgumentException e) {
			Activator.logErrorMessage(NLS.bind(DocometreMessages.CursorUnknownShape, shapeName));
		}
		CursorStyle style = CursorStyle.shape(shape, size);
		float outlineWidth = parseFloat(cursorCurveConfiguration.getProperty(CursorCurveConfigurationProperties.OUTLINE_WIDTH), 0);
		if(outlineWidth > 0) {
			Color outlineColor = CursorCurveConfigurationProperties.getOutlineColor(cursorCurveConfiguration);
			style = style.withOutline(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), outlineWidth);
		}
		return style;
	}
	
	private static float parseFloat(String value, float defaultValue) {
		try {
			return (value == null) ? defaultValue : Float.parseFloat(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return XYChartConfigurationProperties.copy(this, new Cursor2DChartConfiguration());
	}

}
