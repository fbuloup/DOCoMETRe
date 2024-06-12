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

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.FontUtil;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.nrtswtchart.RTSWTXYChart;
import fr.univamu.ism.nrtswtchart.RTSWTXYSerie;
import fr.univamu.ism.docometre.editors.ModulePage.ModuleSectionPart;

public class XYChartConfiguration extends ChartConfiguration {

	private static final long serialVersionUID = 1L;
	
	transient private Button autoScaleButton;
	transient private Text xAmpMaxText;
	transient private Text xAmpMinText;
	transient private Text yAmpMaxText;
	transient private Text yAmpMinText;
	transient private Combo fontCombo;
	transient private Button fontBoldButton;
	transient private Button fontItalicButton;
	transient private Text fontSizeText;

	public XYChartConfiguration() {
		super(ChartTypes.XY_CHART);
		XYChartConfigurationProperties.populateProperties(this);
	}

	@Override
	public void update(FloatBuffer floatBuffer, String channelID) {
	}

	@Override
	public CurveConfiguration[] createCurvesConfiguration(IStructuredSelection selection) {
		Object[] elements = selection.toArray();
		XYCurveConfiguration xyCurveConfiguration = new XYCurveConfiguration((Channel)elements[0], (Channel)elements[1]);
		addCurveConfiguration(xyCurveConfiguration);
		return new CurveConfiguration[] {xyCurveConfiguration};
	}

	@Override
	public void populateChartConfigurationContainer(Composite container, ChartsConfigurationPage page, ModuleSectionPart generalConfigurationSectionPart) {
		container.setLayout(new GridLayout(6, false));
		
		autoScaleButton = page.createButton(container, DocometreMessages.AutoScale_Title, SWT.CHECK, 6, 1);
		String value = getProperty(XYChartConfigurationProperties.AUTO_SCALE);
		boolean autoScale = Boolean.valueOf(value);
		autoScaleButton.setSelection(autoScale);
		autoScaleButton.addSelectionListener(new ModifyPropertyHandler(XYChartConfigurationProperties.AUTO_SCALE, XYChartConfiguration.this, autoScaleButton, XYChartConfigurationProperties.AUTO_SCALE.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.xMaxAmplitude_Title, DocometreMessages.xMaxAmplitude_Tooltip);
		xAmpMaxText = page.createText(container, getProperty(XYChartConfigurationProperties.X_MAX), SWT.NONE, 5, 1); 
		xAmpMaxText.addModifyListener(page.getGeneralConfigurationModifyListener());
		xAmpMaxText.addModifyListener(new ModifyPropertyHandler(XYChartConfigurationProperties.X_MAX, this, xAmpMaxText, XYChartConfigurationProperties.X_MAX.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.xMinAmplitude_Title, DocometreMessages.xMinAmplitude_Tooltip);
		xAmpMinText = page.createText(container, getProperty(XYChartConfigurationProperties.X_MIN), SWT.NONE, 5, 1); 
		xAmpMinText.addModifyListener(page.getGeneralConfigurationModifyListener());
		xAmpMinText.addModifyListener(new ModifyPropertyHandler(XYChartConfigurationProperties.X_MIN, this, xAmpMinText, XYChartConfigurationProperties.X_MIN.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.yMaxAmplitude_Title, DocometreMessages.yMaxAmplitude_Tooltip);
		yAmpMaxText = page.createText(container, getProperty(XYChartConfigurationProperties.Y_MAX), SWT.NONE, 5, 1); 
		yAmpMaxText.addModifyListener(page.getGeneralConfigurationModifyListener());
		yAmpMaxText.addModifyListener(new ModifyPropertyHandler(XYChartConfigurationProperties.Y_MAX, this, yAmpMaxText, XYChartConfigurationProperties.Y_MAX.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.yMinAmplitude_Title, DocometreMessages.yMinAmplitude_Tooltip);
		yAmpMinText = page.createText(container, getProperty(XYChartConfigurationProperties.Y_MIN), SWT.NONE, 5, 1); 
		yAmpMinText.addModifyListener(page.getGeneralConfigurationModifyListener());
		yAmpMinText.addModifyListener(new ModifyPropertyHandler(XYChartConfigurationProperties.Y_MIN, this, yAmpMinText, XYChartConfigurationProperties.Y_MIN.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		autoScaleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				generalConfigurationSectionPart.markDirty();
				xAmpMaxText.setEnabled(!autoScaleButton.getSelection());
				xAmpMinText.setEnabled(!autoScaleButton.getSelection());
				yAmpMaxText.setEnabled(!autoScaleButton.getSelection());
				yAmpMinText.setEnabled(!autoScaleButton.getSelection());
			}
		});
		
		page.createLabel(container, DocometreMessages.Font_Title, DocometreMessages.Font_Tooltip);
		value = getProperty(XYChartConfigurationProperties.FONT);
		value = value==null?FontUtil.getDefaultFontName():value;
		fontCombo = page.createCombo(container, XYChartConfigurationProperties.FONT.getAvailableValues(), value, 1, 1);
		fontCombo.addModifyListener(page.getGeneralConfigurationModifyListener());
		fontCombo.addModifyListener(new ModifyPropertyHandler(XYChartConfigurationProperties.FONT, this, fontCombo, XYChartConfigurationProperties.FONT.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		value = getProperty(XYChartConfigurationProperties.FONT_BOLD);
		boolean fontBold = Boolean.valueOf(value);
		fontBoldButton = page.createButton(container, "Bold", SWT.CHECK, 1, 1);
		fontBoldButton.setSelection(fontBold);
		fontBoldButton.addSelectionListener(new ModifyPropertyHandler(XYChartConfigurationProperties.FONT_BOLD, this, fontBoldButton, XYChartConfigurationProperties.FONT_BOLD.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		value = getProperty(XYChartConfigurationProperties.FONT_ITALIC);
		boolean fontItalic = Boolean.valueOf(value);
		fontItalicButton = page.createButton(container, "Italic", SWT.CHECK, 1, 1);
		fontItalicButton.setSelection(fontItalic);
		fontItalicButton.addSelectionListener(new ModifyPropertyHandler(XYChartConfigurationProperties.FONT_ITALIC, this, fontItalicButton, XYChartConfigurationProperties.FONT_ITALIC.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.Size_Title, DocometreMessages.Size_Tooltip);
		fontSizeText = page.createText(container, getProperty(XYChartConfigurationProperties.FONT_SIZE), SWT.NONE, 1, 1);
		fontSizeText.addModifyListener(page.getGeneralConfigurationModifyListener());
		fontSizeText.addModifyListener(new ModifyPropertyHandler(XYChartConfigurationProperties.FONT_SIZE, this, fontSizeText, XYChartConfigurationProperties.FONT_SIZE.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Control widget, XYChartConfigurationProperties propertyKey) {
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

	@Override
	public void update(Property property, Object newValue, Object oldValue) {
		if(!(property instanceof XYChartConfigurationProperties)) return;
		if(property == XYChartConfigurationProperties.AUTO_SCALE) {
			updateWidget(autoScaleButton, (XYChartConfigurationProperties)property);

			yAmpMaxText.setEnabled(!autoScaleButton.getSelection());
			yAmpMinText.setEnabled(!autoScaleButton.getSelection());
			xAmpMaxText.setEnabled(!autoScaleButton.getSelection());
			xAmpMinText.setEnabled(!autoScaleButton.getSelection());
		}
		if(property == XYChartConfigurationProperties.Y_MAX)
			updateWidget(yAmpMaxText, (XYChartConfigurationProperties)property);
		if(property == XYChartConfigurationProperties.Y_MIN)
			updateWidget(yAmpMinText, (XYChartConfigurationProperties)property);
		if(property == XYChartConfigurationProperties.X_MAX)
			updateWidget(xAmpMaxText, (XYChartConfigurationProperties)property);
		if(property == XYChartConfigurationProperties.X_MIN)
			updateWidget(xAmpMinText, (XYChartConfigurationProperties)property);
		if(property == XYChartConfigurationProperties.FONT)
			updateWidget(fontCombo, (XYChartConfigurationProperties)property);
		if(property == XYChartConfigurationProperties.FONT_SIZE)
			updateWidget(fontSizeText, (XYChartConfigurationProperties)property);
	}

	@Override
	public void createChart(Composite chartContainer) {
		// Clean series
		for (CurveConfiguration curveConfiguration : curvesConfigurations) {
			XYCurveConfiguration xyCurveConfiguration = (XYCurveConfiguration)curveConfiguration;
			xyCurveConfiguration.setSerie(null);
		}
		// Create chart
		String value = getProperty(ChartConfigurationProperties.HORIZONTAL_SPANNING);
		int hSpan = Integer.parseInt(value);
		value = getProperty(ChartConfigurationProperties.VERTICAL_SPANNING);
		int vSpan = Integer.parseInt(value);
		value = getProperty(XYChartConfigurationProperties.AUTO_SCALE);
		boolean autoscale = Boolean.parseBoolean(value);
		value = getProperty(XYChartConfigurationProperties.X_MAX);
		double xMax = Double.parseDouble(value);
		value = getProperty(XYChartConfigurationProperties.X_MIN);
		double xMin = Double.parseDouble(value);
		value = getProperty(XYChartConfigurationProperties.Y_MAX);
		double yMax = Double.parseDouble(value);
		value = getProperty(XYChartConfigurationProperties.Y_MIN);
		double yMin = Double.parseDouble(value);
		String fontName = getProperty(XYChartConfigurationProperties.FONT);
		if(fontName == null) fontName = FontUtil.getDefaultFontName();
		value = getProperty(XYChartConfigurationProperties.FONT_BOLD);
		boolean bold = Boolean.parseBoolean(value);
		value = getProperty(XYChartConfigurationProperties.FONT_ITALIC);
		boolean italic = Boolean.parseBoolean(value);
		int fontStyle = (bold?SWT.BOLD:SWT.NORMAL) | (italic?SWT.ITALIC:SWT.NORMAL);
		value = getProperty(XYChartConfigurationProperties.FONT_SIZE);
		if(value == null || "".equals(value)) value = "12";
		int fontSize = Integer.parseInt(value);
		
		RTSWTXYChart rtswtxyChart = new RTSWTXYChart(chartContainer, SWT.DOUBLE_BUFFERED, fontName, fontStyle, fontSize);
		rtswtxyChart.getChart().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, hSpan, vSpan));
		rtswtxyChart.setAutoScale(autoscale);
		rtswtxyChart.setxMax(xMax);
		rtswtxyChart.setxMin(xMin);
		rtswtxyChart.setyMax(yMax);
		rtswtxyChart.setyMin(yMin);
		rtswtxyChart.setGridVisibility(true);
		rtswtxyChart.setLegendVisibility(true);
		rtswtxyChart.setLegendPosition(SWT.BOTTOM);
		
		// Create curves
		for (CurveConfiguration curveConfiguration : curvesConfigurations) {
			XYCurveConfiguration xyCurveConfiguration = (XYCurveConfiguration)curveConfiguration;
			String xSerieID = xyCurveConfiguration.getXChannel().getID();
			String ySerieID = xyCurveConfiguration.getYChannel().getID();
			Color serieColor = CurveConfigurationProperties.getColor(xyCurveConfiguration);
			
//			int serieStyle = CurveConfigurationProperties.getStyle(xyCurveConfiguration);
//			int serieWidth = Integer.parseInt(xyCurveConfiguration.getProperty(CurveConfigurationProperties.WIDTH));
//			BasicStroke stroke = new BasicStroke(serieWidth);
//			int lineWidth = serieWidth + 13;
//			int emptyWidth = serieWidth + 3;
//			if(serieStyle == SWT.LINE_DASH) 
//				stroke = new BasicStroke(serieWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] {lineWidth, emptyWidth}, 0.0f);
//			if(serieStyle == SWT.LINE_DOT) 
//				stroke = new BasicStroke(serieWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] {emptyWidth, emptyWidth}, 0.0f);
//			if(serieStyle == SWT.LINE_DASHDOT) 
//				stroke = new BasicStroke(serieWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] {lineWidth, serieWidth, emptyWidth, serieWidth}, 0.0f);
//			if(serieStyle == SWT.LINE_DASHDOTDOT) 
//				stroke = new BasicStroke(serieWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1.0f, new float[] {lineWidth, emptyWidth, emptyWidth, emptyWidth, emptyWidth, emptyWidth}, 0.0f);
	
			
			RTSWTXYSerie rtswtSerie = rtswtxyChart.createSerie(ySerieID + "(" + xSerieID + ")", serieColor);
			xyCurveConfiguration.setSerie(rtswtSerie);
		}

	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return XYChartConfigurationProperties.clone(this);
	}

	@Override
	public void initializeObservers() {
		// TODO Auto-generated method stub

	}

}
