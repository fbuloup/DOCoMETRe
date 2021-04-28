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
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.docometre.editors.ModulePage.ModuleSectionPart;
import fr.univamu.ism.rtswtchart.RTSWTChartFonts;
import fr.univamu.ism.rtswtchart.RTSWTOscilloChart;
import fr.univamu.ism.rtswtchart.RTSWTOscilloSerie;

public class OscilloChartConfiguration extends ChartConfiguration {

	transient private Text timeWidthText;
	transient private Button autoScaleButton;
	transient private Text ampMaxText;
	transient private Text ampMinText;
	transient private Combo fontCombo;
	transient private Button displayCurrentValuesButton;

	public OscilloChartConfiguration() {
		super(ChartTypes.OSCILLO_CHART);
		OscilloChartConfigurationProperties.populateProperties(this);
	}

	private static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return OscilloChartConfigurationProperties.clone(this);
	}
	
	@Override
	public void initializeObservers() {
		// TODO Auto-generated method stub
	}
	
	public void populateChartConfigurationContainer(Composite container, ChartsConfigurationPage page, ModuleSectionPart generalConfigurationSectionPart) {
		container.setLayout(new GridLayout(2, false));
		
		page.createLabel(container, DocometreMessages.TimeWidth_Title, DocometreMessages.TimeWidth_Tooltip);
		timeWidthText = page.createText(container, getProperty(OscilloChartConfigurationProperties.TIME_WIDTH), SWT.NONE, 1, 1);
		timeWidthText.addModifyListener(page.getGeneralConfigurationModifyListener());
		timeWidthText.addModifyListener(new ModifyPropertyHandler(OscilloChartConfigurationProperties.TIME_WIDTH, this, timeWidthText, OscilloChartConfigurationProperties.TIME_WIDTH.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		autoScaleButton = page.createButton(container, DocometreMessages.AutoScale_Title, SWT.CHECK, 2, 1);
		String value = getProperty(OscilloChartConfigurationProperties.AUTO_SCALE);
		boolean autoScale = Boolean.valueOf(value);
		autoScaleButton.setSelection(autoScale);
		autoScaleButton.addSelectionListener(new ModifyPropertyHandler(OscilloChartConfigurationProperties.AUTO_SCALE, OscilloChartConfiguration.this, autoScaleButton, OscilloChartConfigurationProperties.AUTO_SCALE.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.MaxAmplitude_Title, DocometreMessages.MaxAmplitude_Tooltip);
		ampMaxText = page.createText(container, getProperty(OscilloChartConfigurationProperties.Y_MAX), SWT.NONE, 1, 1); 
		ampMaxText.addModifyListener(page.getGeneralConfigurationModifyListener());
		ampMaxText.addModifyListener(new ModifyPropertyHandler(OscilloChartConfigurationProperties.Y_MAX, this, ampMaxText, OscilloChartConfigurationProperties.Y_MAX.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.MinAmplitude_Title, DocometreMessages.MinAmplitude_Tooltip);
		ampMinText = page.createText(container, getProperty(OscilloChartConfigurationProperties.Y_MIN), SWT.NONE, 1, 1); 
		ampMinText.addModifyListener(page.getGeneralConfigurationModifyListener());
		ampMinText.addModifyListener(new ModifyPropertyHandler(OscilloChartConfigurationProperties.Y_MIN, this, ampMinText, OscilloChartConfigurationProperties.Y_MIN.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		autoScaleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				generalConfigurationSectionPart.markDirty();
				ampMaxText.setEnabled(!autoScaleButton.getSelection());
				ampMinText.setEnabled(!autoScaleButton.getSelection());
			}
		});
		
		page.createLabel(container, DocometreMessages.Font_Title, DocometreMessages.Font_Tooltip);
		value = getProperty(OscilloChartConfigurationProperties.FONT);
		value = value==null?RTSWTChartFonts.BITMAP_HELVETICA_10.getLabel():value;
		fontCombo = page.createCombo(container, OscilloChartConfigurationProperties.FONT.getAvailableValues(), value, 1, 1);
		fontCombo.addModifyListener(page.getGeneralConfigurationModifyListener());
		fontCombo.addModifyListener(new ModifyPropertyHandler(OscilloChartConfigurationProperties.FONT, this, fontCombo, OscilloChartConfigurationProperties.FONT.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		displayCurrentValuesButton = page.createButton(container, DocometreMessages.DisplayValues_Title, SWT.CHECK, 2, 1);
		value = getProperty(OscilloChartConfigurationProperties.DISPLAY_CURRENT_VALUES);
		boolean displayCurrentValues = Boolean.valueOf(value);
		displayCurrentValuesButton.setSelection(displayCurrentValues);
		displayCurrentValuesButton.addSelectionListener(new ModifyPropertyHandler(OscilloChartConfigurationProperties.DISPLAY_CURRENT_VALUES, OscilloChartConfiguration.this, displayCurrentValuesButton, OscilloChartConfigurationProperties.DISPLAY_CURRENT_VALUES.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Control widget, OscilloChartConfigurationProperties propertyKey) {
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
		if(!(property instanceof OscilloChartConfigurationProperties)) return;
		if(property == OscilloChartConfigurationProperties.AUTO_SCALE) {
			updateWidget(autoScaleButton, (OscilloChartConfigurationProperties)property);
			ampMaxText.setEnabled(!autoScaleButton.getSelection());
			ampMinText.setEnabled(!autoScaleButton.getSelection());
		}
			
		if(property == OscilloChartConfigurationProperties.TIME_WIDTH)
			updateWidget(timeWidthText, (OscilloChartConfigurationProperties)property);
		if(property == OscilloChartConfigurationProperties.Y_MAX)
			updateWidget(ampMaxText, (OscilloChartConfigurationProperties)property);
		if(property == OscilloChartConfigurationProperties.Y_MIN)
			updateWidget(ampMinText, (OscilloChartConfigurationProperties)property);
		if(property == OscilloChartConfigurationProperties.DISPLAY_CURRENT_VALUES)
			updateWidget(displayCurrentValuesButton, (OscilloChartConfigurationProperties)property);
		if(property == OscilloChartConfigurationProperties.FONT)
			updateWidget(fontCombo, (OscilloChartConfigurationProperties)property);
	}
	
	public CurveConfiguration[] createCurvesConfiguration(IStructuredSelection selection) {
		Object[] objects = selection.toArray();
		CurveConfiguration[] newCurvesConfigurations = new CurveConfiguration[objects.length];
		int i = 0;
		for (Object object : objects) {
			if(object instanceof Channel) {
				Channel channel = (Channel) object;
				OscilloCurveConfiguration oscilloCurveConfiguration = new OscilloCurveConfiguration(channel);
				oscilloCurveConfiguration.setProperty(OscilloCurveConfigurationProperties.CHANNEL_NAME, channel.getProperty(ChannelProperties.NAME));
				oscilloCurveConfiguration.setProperty(CurveConfigurationProperties.STYLE, CurveConfigurationProperties.SOLID);
				oscilloCurveConfiguration.setProperty(CurveConfigurationProperties.WIDTH, "1");
				addCurveConfiguration(oscilloCurveConfiguration);
				newCurvesConfigurations[i] = oscilloCurveConfiguration;
				i++;
			}
		}
		
		return newCurvesConfigurations;
	}

	@Override
	public void update(FloatBuffer floatBuffer, String channelID) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createChart(Composite container) {
		// Clean series
		for (CurveConfiguration curveConfiguration : curvesConfigurations) {
			OscilloCurveConfiguration oscilloCurveConfiguration = (OscilloCurveConfiguration)curveConfiguration;
			oscilloCurveConfiguration.setSerie(null);
		}
		// Create chart
		String value = getProperty(ChartConfigurationProperties.HORIZONTAL_SPANNING);
		int hSpan = Integer.parseInt(value);
		value = getProperty(ChartConfigurationProperties.VERTICAL_SPANNING);
		int vSpan = Integer.parseInt(value);
		value = getProperty(OscilloChartConfigurationProperties.AUTO_SCALE);
		boolean autoscale = Boolean.parseBoolean(value);
		value = getProperty(OscilloChartConfigurationProperties.TIME_WIDTH);
		double timeWidth = Double.parseDouble(value);
		value = getProperty(OscilloChartConfigurationProperties.Y_MAX);
		double yMax = Double.parseDouble(value);
		value = getProperty(OscilloChartConfigurationProperties.Y_MIN);
		double yMin = Double.parseDouble(value);
		String font = getProperty(OscilloChartConfigurationProperties.FONT);
		if(font == null) font = RTSWTChartFonts.BITMAP_HELVETICA_10.getLabel();
		value = getProperty(OscilloChartConfigurationProperties.DISPLAY_CURRENT_VALUES);
		boolean displayCurrentValues = Boolean.parseBoolean(value);
		RTSWTOscilloChart oscilloChart = new RTSWTOscilloChart(container, SWT.NORMAL, RTSWTChartFonts.getFont(font), timeWidth, yMin, yMax, autoscale, SWT.ON, SWT.HIGH);
		oscilloChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, hSpan, vSpan));
		// Create curves
		for (CurveConfiguration curveConfiguration : curvesConfigurations) {
			OscilloCurveConfiguration oscilloCurveConfiguration = (OscilloCurveConfiguration)curveConfiguration;
			
			String serieID = oscilloCurveConfiguration.getChannel().getID();
			Color serieColor = CurveConfigurationProperties.getColor(oscilloCurveConfiguration);
			int serieStyle = CurveConfigurationProperties.getStyle(oscilloCurveConfiguration);
			int serieWidth = Integer.parseInt(oscilloCurveConfiguration.getProperty(CurveConfigurationProperties.WIDTH));
			RTSWTOscilloSerie oscilloSerie = oscilloChart.createSerie(serieID, serieColor, serieStyle, serieWidth);
			oscilloSerie.setShowCurrentValue(displayCurrentValues);
			// Set serie to update in configuration
			oscilloCurveConfiguration.setSerie(oscilloSerie);
		}
	}

}
