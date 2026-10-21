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

import java.nio.FloatBuffer;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Hyperlink;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.FontUtil;
import fr.univamu.ism.docometre.chartxyz.Curve3DData;
import fr.univamu.ism.docometre.chartxyz.IRTSWTXYZChart;
import fr.univamu.ism.docometre.chartxyz.IRTSWTXYZSerie;
import fr.univamu.ism.docometre.chartxyz.LineStyle;
import fr.univamu.ism.docometre.chartxyz.macos.NewtChart3D;
import fr.univamu.ism.docometre.chartxyz.windows.Chart3D;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.docometre.editors.ModulePage.ModuleSectionPart;

public class XYZChartConfiguration extends ChartConfiguration {

	private static final long serialVersionUID = 1L;
	
	transient private Button autoScaleButton;
	transient private Text xAmpMaxText;
	transient private Text xAmpMinText;
	transient private Text yAmpMaxText;
	transient private Text yAmpMinText;
	transient private Text zAmpMaxText;
	transient private Text zAmpMinText;
	transient private Combo fontCombo;
	transient private Button fontBoldButton;
	transient private Button fontItalicButton;
	transient private Text fontSizeText;
	transient private Text historySizeText;
	transient private Button chartColorButton;

	public XYZChartConfiguration() {
		super(ChartTypes.XYZ_CHART);
		XYZChartConfigurationProperties.populateProperties(this);
	}

	@Override
	public void update(FloatBuffer floatBuffer, String channelID) {
	}

	@Override
	public CurveConfiguration[] createCurvesConfiguration(IStructuredSelection selection) {
		Object[] elements = selection.toArray();
		XYZCurveConfiguration xyzCurveConfiguration = new XYZCurveConfiguration((Channel)elements[0], (Channel)elements[1], (Channel)elements[2]);
		addCurveConfiguration(xyzCurveConfiguration);
		return new CurveConfiguration[] {xyzCurveConfiguration};
	}

	@Override
	public void populateChartConfigurationContainer(Composite container, ChartsConfigurationPage page, ModuleSectionPart generalConfigurationSectionPart) {
		container.setLayout(new GridLayout(7, false));
		
		autoScaleButton = page.createButton(container, DocometreMessages.AutoScale_Title, SWT.CHECK, 7, 1);
		String value = getProperty(XYZChartConfigurationProperties.AUTO_SCALE);
		boolean autoScale = Boolean.valueOf(value);
		autoScaleButton.setSelection(autoScale);
		autoScaleButton.addSelectionListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.AUTO_SCALE, XYZChartConfiguration.this, autoScaleButton, XYZChartConfigurationProperties.AUTO_SCALE.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.xMaxAmplitude_Title, DocometreMessages.xMaxAmplitude_Tooltip);
		xAmpMaxText = page.createText(container, getProperty(XYZChartConfigurationProperties.X_MAX), SWT.NONE, 6, 1); 
		xAmpMaxText.addModifyListener(page.getGeneralConfigurationModifyListener());
		xAmpMaxText.addModifyListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.X_MAX, this, xAmpMaxText, XYZChartConfigurationProperties.X_MAX.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.xMinAmplitude_Title, DocometreMessages.xMinAmplitude_Tooltip);
		xAmpMinText = page.createText(container, getProperty(XYZChartConfigurationProperties.X_MIN), SWT.NONE, 6, 1); 
		xAmpMinText.addModifyListener(page.getGeneralConfigurationModifyListener());
		xAmpMinText.addModifyListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.X_MIN, this, xAmpMinText, XYZChartConfigurationProperties.X_MIN.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.yMaxAmplitude_Title, DocometreMessages.yMaxAmplitude_Tooltip);
		yAmpMaxText = page.createText(container, getProperty(XYZChartConfigurationProperties.Y_MAX), SWT.NONE, 6, 1); 
		yAmpMaxText.addModifyListener(page.getGeneralConfigurationModifyListener());
		yAmpMaxText.addModifyListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.Y_MAX, this, yAmpMaxText, XYZChartConfigurationProperties.Y_MAX.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.yMinAmplitude_Title, DocometreMessages.yMinAmplitude_Tooltip);
		yAmpMinText = page.createText(container, getProperty(XYZChartConfigurationProperties.Y_MIN), SWT.NONE, 6, 1); 
		yAmpMinText.addModifyListener(page.getGeneralConfigurationModifyListener());
		yAmpMinText.addModifyListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.Y_MIN, this, yAmpMinText, XYZChartConfigurationProperties.Y_MIN.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.zMaxAmplitude_Title, DocometreMessages.zMaxAmplitude_Tooltip);
		zAmpMaxText = page.createText(container, getProperty(XYZChartConfigurationProperties.Z_MAX), SWT.NONE, 6, 1); 
		zAmpMaxText.addModifyListener(page.getGeneralConfigurationModifyListener());
		zAmpMaxText.addModifyListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.Z_MAX, this, zAmpMaxText, XYZChartConfigurationProperties.Z_MAX.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.zMinAmplitude_Title, DocometreMessages.zMinAmplitude_Tooltip);
		zAmpMinText = page.createText(container, getProperty(XYZChartConfigurationProperties.Z_MIN), SWT.NONE, 6, 1); 
		zAmpMinText.addModifyListener(page.getGeneralConfigurationModifyListener());
		zAmpMinText.addModifyListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.Z_MIN, this, zAmpMinText, XYZChartConfigurationProperties.Z_MIN.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		autoScaleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				generalConfigurationSectionPart.markDirty();
				xAmpMaxText.setEnabled(!autoScaleButton.getSelection());
				xAmpMinText.setEnabled(!autoScaleButton.getSelection());
				yAmpMaxText.setEnabled(!autoScaleButton.getSelection());
				yAmpMinText.setEnabled(!autoScaleButton.getSelection());
				zAmpMaxText.setEnabled(!autoScaleButton.getSelection());
				zAmpMinText.setEnabled(!autoScaleButton.getSelection());
			}
		});
		
		page.createLabel(container, DocometreMessages.Font_Title, DocometreMessages.Font_Tooltip);
		value = getProperty(XYZChartConfigurationProperties.FONT);
		value = value==null?FontUtil.getDefaultFontName():value;
		fontCombo = page.createCombo(container, XYZChartConfigurationProperties.FONT.getAvailableValues(), value, 1, 1);
		fontCombo.addModifyListener(page.getGeneralConfigurationModifyListener());
		fontCombo.addModifyListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.FONT, this, fontCombo, XYZChartConfigurationProperties.FONT.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		value = getProperty(XYZChartConfigurationProperties.FONT_BOLD);
		boolean fontBold = Boolean.valueOf(value);
		fontBoldButton = page.createButton(container, "Bold", SWT.CHECK, 1, 1);
		fontBoldButton.setSelection(fontBold);
		fontBoldButton.addSelectionListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.FONT_BOLD, this, fontBoldButton, XYZChartConfigurationProperties.FONT_BOLD.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		fontBoldButton.setEnabled(!Activator.useOpenGLV1());
		
		value = getProperty(XYZChartConfigurationProperties.FONT_ITALIC);
		boolean fontItalic = Boolean.valueOf(value);
		fontItalicButton = page.createButton(container, "Italic", SWT.CHECK, 1, 1);
		fontItalicButton.setSelection(fontItalic);
		fontItalicButton.addSelectionListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.FONT_ITALIC, this, fontItalicButton, XYZChartConfigurationProperties.FONT_ITALIC.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		fontItalicButton.setEnabled(!Activator.useOpenGLV1());
		
		page.createLabel(container, DocometreMessages.Size_Title, DocometreMessages.Size_Tooltip);
		fontSizeText = page.createText(container, getProperty(XYZChartConfigurationProperties.FONT_SIZE), SWT.NONE, 1, 1);
		fontSizeText.addModifyListener(page.getGeneralConfigurationModifyListener());
		fontSizeText.addModifyListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.FONT_SIZE, this, fontSizeText, XYZChartConfigurationProperties.FONT_SIZE.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		fontSizeText.setEnabled(!Activator.useOpenGLV1());
		
		Color chartColor = ChartConfigurationProperties.getColor(this, ChartConfigurationProperties.COLOR);
		chartColorButton = page.createColorDialogButton(container, "...", SWT.PUSH | SWT.FLAT, 1, 1);
		chartColorButton.setToolTipText(DocometreMessages.Color_Tooltip);
		chartColorButton.setBackground(chartColor);
		chartColorButton.addSelectionListener(new ModifyPropertyHandler(ChartConfigurationProperties.COLOR, this, chartColorButton, ChartConfigurationProperties.COLOR.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.HistorySize_Title, DocometreMessages.HistorySize_Tooltip);
		historySizeText = page.createText(container, getProperty(XYZChartConfigurationProperties.HISTORY_SIZE), SWT.NONE, 6, 1);
		historySizeText.addModifyListener(page.getGeneralConfigurationModifyListener());
		historySizeText.addModifyListener(new ModifyPropertyHandler(XYZChartConfigurationProperties.HISTORY_SIZE, this, historySizeText, XYZChartConfigurationProperties.HISTORY_SIZE.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Control widget, XYZChartConfigurationProperties propertyKey) {
		String value = getProperty(propertyKey);
		Listener[] listeners = widget.getListeners(SWT.Modify);
		for (Listener listener : listeners) widget.removeListener(SWT.Modify, listener);
		if(widget instanceof Text) {
			Text text = (Text) widget;
			if(!text.getText().equals(value)) text.setText(value);
		}
		if(widget instanceof Hyperlink) ((Hyperlink)widget).setText(value);
		if(widget instanceof Combo) ((Combo)widget).select(((Combo)widget).indexOf(value));
		if(widget instanceof Button) ((Button)widget).setSelection(Boolean.valueOf(value));
		widget.setFocus();
		for (Listener listener : listeners) widget.addListener(SWT.Modify , listener);
	}
	
	private void updateColordialogButton() {
		Listener[] listeners = chartColorButton.getListeners(SWT.Modify);
		for (Listener listener : listeners) chartColorButton.removeListener(SWT.Modify, listener);
		Color chartColor = ChartConfigurationProperties.getColor(this, ChartConfigurationProperties.COLOR);
		chartColorButton.setBackground(chartColor);
		for (Listener listener : listeners) chartColorButton.addListener(SWT.Modify , listener);
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue) {
		boolean validate = (property instanceof XYZChartConfigurationProperties) || (property instanceof ChartConfigurationProperties);
		if(!validate) return;
		if(property == XYZChartConfigurationProperties.AUTO_SCALE) {
			updateWidget(autoScaleButton, (XYZChartConfigurationProperties)property);

			yAmpMaxText.setEnabled(!autoScaleButton.getSelection());
			yAmpMinText.setEnabled(!autoScaleButton.getSelection());
			xAmpMaxText.setEnabled(!autoScaleButton.getSelection());
			xAmpMinText.setEnabled(!autoScaleButton.getSelection());
			zAmpMaxText.setEnabled(!autoScaleButton.getSelection());
			zAmpMinText.setEnabled(!autoScaleButton.getSelection());
		}
		if(property == XYZChartConfigurationProperties.Z_MAX)
			updateWidget(zAmpMaxText, (XYZChartConfigurationProperties)property);
		if(property == XYZChartConfigurationProperties.Z_MIN)
			updateWidget(zAmpMinText, (XYZChartConfigurationProperties)property);
		if(property == XYZChartConfigurationProperties.Y_MAX)
			updateWidget(yAmpMaxText, (XYZChartConfigurationProperties)property);
		if(property == XYZChartConfigurationProperties.Y_MIN)
			updateWidget(yAmpMinText, (XYZChartConfigurationProperties)property);
		if(property == XYZChartConfigurationProperties.X_MAX)
			updateWidget(xAmpMaxText, (XYZChartConfigurationProperties)property);
		if(property == XYZChartConfigurationProperties.X_MIN)
			updateWidget(xAmpMinText, (XYZChartConfigurationProperties)property);
		if(property == XYZChartConfigurationProperties.FONT)
			updateWidget(fontCombo, (XYZChartConfigurationProperties)property);
		if(property == XYZChartConfigurationProperties.FONT_SIZE)
			updateWidget(fontSizeText, (XYZChartConfigurationProperties)property);
		if(property == XYZChartConfigurationProperties.HISTORY_SIZE)
			updateWidget(historySizeText, (XYZChartConfigurationProperties)property);
		if(property == ChartConfigurationProperties.COLOR)
			updateColordialogButton();
	}

	@Override
	public void createChart(Composite chartContainer) {
		// Clean series
		for (CurveConfiguration curveConfiguration : curvesConfigurations) {
			XYZCurveConfiguration xyzCurveConfiguration = (XYZCurveConfiguration)curveConfiguration;
			xyzCurveConfiguration.setSerie(null);
		}
		// Create chart
		String value = getProperty(ChartConfigurationProperties.HORIZONTAL_SPANNING);
		int hSpan = Integer.parseInt(value);
		value = getProperty(ChartConfigurationProperties.VERTICAL_SPANNING);
		int vSpan = Integer.parseInt(value);
		value = getProperty(XYZChartConfigurationProperties.AUTO_SCALE);
		boolean autoscale = Boolean.parseBoolean(value);
		value = getProperty(XYZChartConfigurationProperties.X_MAX);
		double xMax = Double.parseDouble(value);
		value = getProperty(XYZChartConfigurationProperties.X_MIN);
		double xMin = Double.parseDouble(value);
		value = getProperty(XYZChartConfigurationProperties.Y_MAX);
		double yMax = Double.parseDouble(value);
		value = getProperty(XYZChartConfigurationProperties.Y_MIN);
		double yMin = Double.parseDouble(value);
		value = getProperty(XYZChartConfigurationProperties.Z_MAX);
		double zMax = Double.parseDouble(value);
		value = getProperty(XYZChartConfigurationProperties.Z_MIN);
		double zMin = Double.parseDouble(value);
//		String fontName = getProperty(XYZChartConfigurationProperties.FONT);
//		if(fontName == null) fontName = FontUtil.getDefaultFontName();
//		value = getProperty(XYZChartConfigurationProperties.FONT_BOLD);
//		boolean bold = Boolean.parseBoolean(value);
//		value = getProperty(XYZChartConfigurationProperties.FONT_ITALIC);
//		boolean italic = Boolean.parseBoolean(value);
//		int fontStyle = (bold?SWT.BOLD:SWT.NORMAL) | (italic?SWT.ITALIC:SWT.NORMAL);
//		value = getProperty(XYZChartConfigurationProperties.FONT_SIZE);
//		if(value == null || "".equals(value)) value = "12";
//		int fontSize = Integer.parseInt(value);
		value = getProperty(XYZChartConfigurationProperties.HISTORY_SIZE);
		if(value == null || "".equals(value)) value = "1";
		double historySize = Double.parseDouble(value);
//		Color chartColor = ChartConfigurationProperties.getColor(this, ChartConfigurationProperties.COLOR);
		
		IRTSWTXYZChart rtswtxyzChart = null;
		if(Activator.useOpenGLV2()) {
			// There is no XYZ chart except in SWT OpenGL V2
			String os = Platform.getOS();
			if (Platform.OS_WIN32.equals(os)) {
				rtswtxyzChart = new Chart3D(chartContainer, xMin, xMax, yMin, yMax, zMin, zMax, new Curve3DData[0]);
			} else if (Platform.OS_LINUX.equals(os)) {
				rtswtxyzChart = new Chart3D(chartContainer, xMin, xMax, yMin, yMax, zMin, zMax, new Curve3DData[0]);
			} else if (Platform.OS_MACOSX.equals(os)) {
				rtswtxyzChart = new NewtChart3D(chartContainer, xMin, xMax, yMin, yMax, zMin, zMax, new Curve3DData[0]);
			}
		}
		else {
			Text text = new Text(chartContainer, SWT.WRAP);
			text.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
			text.setText(DocometreMessages.ImpossibleToCreate3DChartMessage);
			text.setBackground(chartContainer.getBackground());
			text.setForeground(JFaceColors.getErrorText(null));
			Activator.logErrorMessage("Impossible to use 3D chart without SWT + OpenGL v2 library !");
			return;
		}
		rtswtxyzChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, hSpan, vSpan));
		rtswtxyzChart.setAutoScale(autoscale);
		rtswtxyzChart.setGridVisibility(true);
//		rtswtxyzChart.setLegendFont(fontName, fontSize, fontStyle);
//		rtswtxyzChart.setScaleFont(fontName, fontSize, fontStyle);
		rtswtxyzChart.setLegendVisibility(true);
		rtswtxyzChart.setHistorySize(historySize);
		// Create curves
		for (CurveConfiguration curveConfiguration : curvesConfigurations) {
			XYZCurveConfiguration xyzCurveConfiguration = (XYZCurveConfiguration)curveConfiguration;
			String xSerieID = xyzCurveConfiguration.getXChannel().getID();
			String ySerieID = xyzCurveConfiguration.getYChannel().getID();
			String zSerieID = xyzCurveConfiguration.getZChannel().getID();
			Color serieColor = CurveConfigurationProperties.getColor(xyzCurveConfiguration);
			int thickness = Integer.parseInt(xyzCurveConfiguration.getProperty(CurveConfigurationProperties.WIDTH));
			String sfxString = xyzCurveConfiguration.getXChannel().getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			String sfyString = xyzCurveConfiguration.getYChannel().getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			String sfzString = xyzCurveConfiguration.getZChannel().getProperty(ChannelProperties.SAMPLE_FREQUENCY);
			if(!sfxString.equals(sfyString) || !sfxString.equals(sfzString) || !sfyString.equals(sfzString)) {
				Activator.logErrorMessage("Sample frequency for channel " + xSerieID + " is " + sfxString + "Hz");
				Activator.logErrorMessage("Sample frequency for channel " + ySerieID + " is " + sfyString + "Hz");
				Activator.logErrorMessage("Sample frequency for channel " + zSerieID + " is " + sfzString + "Hz");
				Activator.logErrorMessage("Channels with different sample frequencies cannot be displayed in XYZ chart !");
				continue;
			}
			
			int serieStyle = CurveConfigurationProperties.getStyle(xyzCurveConfiguration);
			LineStyle lineStyle = LineStyle.SOLID;
			if(serieStyle == SWT.LINE_DASH) lineStyle = LineStyle.DASH;
			if(serieStyle == SWT.LINE_DOT) lineStyle = LineStyle.DASH_DOT;
			if(serieStyle == SWT.LINE_DASHDOT) lineStyle = LineStyle.DASH_DOT;
			if(serieStyle == SWT.LINE_DASHDOTDOT) lineStyle = LineStyle.DASH_DASH_DOT;
	
			double sfx = Double.parseDouble(sfxString);
			IRTSWTXYZSerie rtswtSerie = new Curve3DData(sfx);
			((Curve3DData)rtswtSerie).setName(zSerieID + "(" + xSerieID + ", " + ySerieID + ")");
			((Curve3DData)rtswtSerie).setLineWidth(thickness);
			((Curve3DData)rtswtSerie).setLineStyle(lineStyle);
			((Curve3DData)rtswtSerie).setColor(serieColor.getRed()/255,serieColor.getGreen()/255,serieColor.getBlue()/255);
			
			String os = Platform.getOS();
			if (Platform.OS_WIN32.equals(os)) {
				((Chart3D)rtswtxyzChart).addCurve((Curve3DData)rtswtSerie);
			} else if (Platform.OS_LINUX.equals(os)) {
				((Chart3D)rtswtxyzChart).addCurve((Curve3DData)rtswtSerie);
			} else if (Platform.OS_MACOSX.equals(os)) {
				((NewtChart3D)rtswtxyzChart).addCurve((Curve3DData)rtswtSerie);
			}
			
			xyzCurveConfiguration.setSerie(rtswtSerie);
		}

	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return XYZChartConfigurationProperties.clone(this);
	}

	@Override
	public void initializeObservers() {
	}

}
