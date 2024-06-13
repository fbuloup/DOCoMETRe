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

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.widgets.figures.TankFigure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Hyperlink;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.docometre.editors.ModulePage.ModuleSectionPart;

public class MeterChartConfiguration extends ChartConfiguration {
	
	private static final long serialVersionUID = 1L;
	
	transient private Text rangeMaxText;
	transient private Text rangeMinText;
	transient private Button showLowButton;
	transient private Text levelLowText;
	transient private Button showLowLowButton;
	transient private Text levelLowLowText;
	transient private Button showHighButton;
	transient private Text levelHighText;
	transient private Button showHighHighButton;
	transient private Text levelHighHighText;

	transient private Canvas meterContainer;

	public MeterChartConfiguration(ChartTypes chartType) {
		super(chartType);
		MeterChartConfigurationProperties.populateProperties(this);
	}

	@Override
	public void update(FloatBuffer floatBuffer, String channelID) {
	}

	@Override
	public void populateChartConfigurationContainer(Composite container, ChartsConfigurationPage page, ModuleSectionPart generalConfigurationSectionPart) {
		container.setLayout(new GridLayout(2, false));
		
		// Range
		page.createLabel(container, DocometreMessages.rangeMaxAmplitude_Title, DocometreMessages.rangeMaxAmplitude_Tooltip);
		rangeMaxText = page.createText(container, getProperty(MeterChartConfigurationProperties.RANGE_MAX), SWT.NONE, 1, 1); 
		rangeMaxText.addModifyListener(page.getGeneralConfigurationModifyListener());
		rangeMaxText.addModifyListener(new ModifyPropertyHandler(MeterChartConfigurationProperties.RANGE_MAX, MeterChartConfiguration.this, rangeMaxText, MeterChartConfigurationProperties.RANGE_MAX.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.rangeMinAmplitude_Title, DocometreMessages.rangeMinAmplitude_Tooltip);
		rangeMinText = page.createText(container, getProperty(MeterChartConfigurationProperties.RANGE_MIN), SWT.NONE, 1, 1); 
		rangeMinText.addModifyListener(page.getGeneralConfigurationModifyListener());
		rangeMinText.addModifyListener(new ModifyPropertyHandler(MeterChartConfigurationProperties.RANGE_MIN, MeterChartConfiguration.this, rangeMinText, MeterChartConfigurationProperties.RANGE_MIN.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		// Levels High
		showHighHighButton = page.createButton(container, DocometreMessages.ShowHighHigh_Title, SWT.CHECK, 2, 1);
		String stringValue = getProperty(MeterChartConfigurationProperties.SHOW_HIGH_HIGH);
		boolean value = Boolean.valueOf(stringValue);
		showHighHighButton.setSelection(value);
		showHighHighButton.addSelectionListener(new ModifyPropertyHandler(MeterChartConfigurationProperties.SHOW_HIGH_HIGH, MeterChartConfiguration.this, showHighHighButton, MeterChartConfigurationProperties.SHOW_HIGH_HIGH.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
	
		page.createLabel(container, DocometreMessages.LevelHighHigh_Title, DocometreMessages.LevelHighHigh_Tooltip);
		levelHighHighText = page.createText(container, getProperty(MeterChartConfigurationProperties.LEVEL_HIGH_HIGH), SWT.NONE, 1, 1); 
		levelHighHighText.addModifyListener(page.getGeneralConfigurationModifyListener());
		levelHighHighText.addModifyListener(new ModifyPropertyHandler(MeterChartConfigurationProperties.LEVEL_HIGH_HIGH, MeterChartConfiguration.this, levelHighHighText, MeterChartConfigurationProperties.LEVEL_HIGH_HIGH.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		showHighButton = page.createButton(container, DocometreMessages.ShowHigh_Title, SWT.CHECK, 2, 1);
		stringValue = getProperty(MeterChartConfigurationProperties.SHOW_HIGH);
		value = Boolean.valueOf(stringValue);
		showHighButton.setSelection(value);
		showHighButton.addSelectionListener(new ModifyPropertyHandler(MeterChartConfigurationProperties.SHOW_HIGH, MeterChartConfiguration.this, showHighButton, MeterChartConfigurationProperties.SHOW_HIGH.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.LevelHigh_Title, DocometreMessages.LevelHigh_Tooltip);
		levelHighText = page.createText(container, getProperty(MeterChartConfigurationProperties.LEVEL_HIGH), SWT.NONE, 1, 1); 
		levelHighText.addModifyListener(page.getGeneralConfigurationModifyListener());
		levelHighText.addModifyListener(new ModifyPropertyHandler(MeterChartConfigurationProperties.LEVEL_HIGH, MeterChartConfiguration.this, levelHighText, MeterChartConfigurationProperties.LEVEL_HIGH.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		// Levels low
		showLowButton = page.createButton(container, DocometreMessages.ShowLow_Title, SWT.CHECK, 2, 1);
		stringValue = getProperty(MeterChartConfigurationProperties.SHOW_LOW);
		value = Boolean.valueOf(stringValue);
		showLowButton.setSelection(value);
		showLowButton.addSelectionListener(new ModifyPropertyHandler(MeterChartConfigurationProperties.SHOW_LOW, MeterChartConfiguration.this, showLowButton, MeterChartConfigurationProperties.SHOW_LOW.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		page.createLabel(container, DocometreMessages.LevelLow_Title, DocometreMessages.LevelLow_Tooltip);
		levelLowText = page.createText(container, getProperty(MeterChartConfigurationProperties.LEVEL_LOW), SWT.NONE, 1, 1); 
		levelLowText.addModifyListener(page.getGeneralConfigurationModifyListener());
		levelLowText.addModifyListener(new ModifyPropertyHandler(MeterChartConfigurationProperties.LEVEL_LOW, MeterChartConfiguration.this, levelLowText, MeterChartConfigurationProperties.LEVEL_LOW.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
		showLowLowButton = page.createButton(container, DocometreMessages.ShowLowLow_Title, SWT.CHECK, 2, 1);
		stringValue = getProperty(MeterChartConfigurationProperties.SHOW_LOW_LOW);
		value = Boolean.valueOf(stringValue);
		showLowLowButton.setSelection(value);
		showLowLowButton.addSelectionListener(new ModifyPropertyHandler(MeterChartConfigurationProperties.SHOW_LOW_LOW, MeterChartConfiguration.this, showLowLowButton, MeterChartConfigurationProperties.SHOW_LOW_LOW.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
	
		page.createLabel(container, DocometreMessages.LevelLowLow_Title, DocometreMessages.LevelLowLow_Tooltip);
		levelLowLowText = page.createText(container, getProperty(MeterChartConfigurationProperties.LEVEL_LOW_LOW), SWT.NONE, 1, 1); 
		levelLowLowText.addModifyListener(page.getGeneralConfigurationModifyListener());
		levelLowLowText.addModifyListener(new ModifyPropertyHandler(MeterChartConfigurationProperties.LEVEL_LOW_LOW, this, levelLowLowText, MeterChartConfigurationProperties.LEVEL_LOW_LOW.getRegExp(), "", false, (ResourceEditor)page.getEditor()));
		
	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Control widget, MeterChartConfigurationProperties propertyKey) {
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
		if(property == MeterChartConfigurationProperties.RANGE_MAX)
			updateWidget(rangeMaxText, (MeterChartConfigurationProperties)property);
		if(property == MeterChartConfigurationProperties.RANGE_MIN)
			updateWidget(rangeMinText, (MeterChartConfigurationProperties)property);
		if(property == MeterChartConfigurationProperties.SHOW_HIGH_HIGH) {
			updateWidget(showHighHighButton, (MeterChartConfigurationProperties)property);
			levelHighHighText.setEnabled(!showHighHighButton.getSelection());
		}
		if(property == MeterChartConfigurationProperties.SHOW_HIGH) {
			updateWidget(showHighButton, (MeterChartConfigurationProperties)property);
			levelHighText.setEnabled(!showHighButton.getSelection());
		}
		if(property == MeterChartConfigurationProperties.SHOW_LOW) {
			updateWidget(showLowButton, (MeterChartConfigurationProperties)property);
			levelLowText.setEnabled(!showLowButton.getSelection());
		}
		if(property == MeterChartConfigurationProperties.SHOW_LOW_LOW) {
			updateWidget(showLowLowButton, (MeterChartConfigurationProperties)property);
			levelLowLowText.setEnabled(!showLowLowButton.getSelection());
		}
	}
	
	@Override
	public CurveConfiguration[] createCurvesConfiguration(IStructuredSelection selection) {
		Object element = selection.getFirstElement();
		Channel channel = (Channel) element;
		MeterCurveConfiguration meterCurveConfiguration = new MeterCurveConfiguration(channel);
		meterCurveConfiguration.setProperty(MeterCurveConfigurationProperties.CHANNEL_NAME, channel.getProperty(ChannelProperties.NAME));
		addCurveConfiguration(meterCurveConfiguration);
		return new CurveConfiguration[] {meterCurveConfiguration};
	}

	@Override
	public void createChart(Composite container) {
		// Create chart
		
		MeterCurveConfiguration[] meterCurvesConfigurations = curvesConfigurations.toArray(new MeterCurveConfiguration[curvesConfigurations.size()]);
		if(meterCurvesConfigurations.length != 1) return;
		
		String value = getProperty(ChartConfigurationProperties.HORIZONTAL_SPANNING);
		int hSpan = Integer.parseInt(value);
		value = getProperty(ChartConfigurationProperties.VERTICAL_SPANNING);
		int vSpan = Integer.parseInt(value);

		value = getProperty(MeterChartConfigurationProperties.RANGE_MAX);
		double rangeMax = Double.parseDouble(value);
		value = getProperty(MeterChartConfigurationProperties.RANGE_MIN);
		double rangeMin = Double.parseDouble(value);
		
		value = getProperty(MeterChartConfigurationProperties.SHOW_HIGH_HIGH);
		boolean showHighHigh = Boolean.parseBoolean(value);
		value = getProperty(MeterChartConfigurationProperties.SHOW_HIGH);
		boolean showHigh = Boolean.parseBoolean(value);
		value = getProperty(MeterChartConfigurationProperties.SHOW_LOW);
		boolean showLow = Boolean.parseBoolean(value);
		value = getProperty(MeterChartConfigurationProperties.SHOW_LOW_LOW);
		boolean showLowLow = Boolean.parseBoolean(value);
		value = getProperty(MeterChartConfigurationProperties.LEVEL_HIGH_HIGH);
		double levelHighHigh = Double.parseDouble(value);
		value = getProperty(MeterChartConfigurationProperties.LEVEL_HIGH);
		double levelHigh = Double.parseDouble(value);
		value = getProperty(MeterChartConfigurationProperties.LEVEL_LOW);
		double levelLow = Double.parseDouble(value);
		value = getProperty(MeterChartConfigurationProperties.LEVEL_LOW_LOW);
		double levelLowLow = Double.parseDouble(value);
		
		meterContainer = new Canvas(container, SWT.BORDER);
		meterContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, hSpan, vSpan));
		meterContainer.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		
		LightweightSystem lws = new LightweightSystem(meterContainer);
		
		Figure meterFigure = null;
		
		if(getChartType().equals(ChartTypes.GAUGE_CHART)) {
			meterFigure = new GaugeFigure();
			((GaugeFigure)meterFigure).setRange(rangeMin, rangeMax);
			((GaugeFigure)meterFigure).setShowHihi(showHighHigh);
			((GaugeFigure)meterFigure).setShowHi(showHigh);
			((GaugeFigure)meterFigure).setShowLo(showLow);
			((GaugeFigure)meterFigure).setShowLolo(showLowLow);
			((GaugeFigure)meterFigure).setLoloLevel(levelLowLow);
			((GaugeFigure)meterFigure).setLoLevel(levelLow);
			((GaugeFigure)meterFigure).setHihiLevel(levelHighHigh);
			((GaugeFigure)meterFigure).setHiLevel(levelHigh);
			meterFigure.setBackgroundColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
			meterFigure.setForegroundColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
			((GaugeFigure)meterFigure).setTitle(meterCurvesConfigurations[0].getChannel().getID());
		}
		if(getChartType().equals(ChartTypes.TANK_CHART)) {
			meterFigure = new TankFigure();
			((TankFigure)meterFigure).setRange(rangeMin, rangeMax);
			((TankFigure)meterFigure).setShowHihi(showHighHigh);
			((TankFigure)meterFigure).setShowHi(showHigh);
			((TankFigure)meterFigure).setShowLo(showLow);
			((TankFigure)meterFigure).setShowLolo(showLowLow);
			((TankFigure)meterFigure).setLoloLevel(levelLowLow);
			((TankFigure)meterFigure).setLoLevel(levelLow);
			((TankFigure)meterFigure).setHihiLevel(levelHighHigh);
			((TankFigure)meterFigure).setHiLevel(levelHigh);
			meterFigure.setBackgroundColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
			meterFigure.setForegroundColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE));
			((TankFigure)meterFigure).setTitle(meterCurvesConfigurations[0].getChannel().getID());
		}
		
		meterCurvesConfigurations[0].setChart(meterFigure);
		meterCurvesConfigurations[0].setLigthWeightSystem(lws);
		
		lws.setControl(meterContainer);
		lws.setContents(meterFigure);
	}
	
	public void setVisible(boolean visible) {
		if(meterContainer != null && !meterContainer.isDisposed())meterContainer.setVisible(visible);
	}
	
	public boolean isVisible() {
		if(meterContainer != null && !meterContainer.isDisposed()) return meterContainer.isVisible();
		return false;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return MeterChartConfigurationProperties.clone(this);
	}

	@Override
	public void initializeObservers() {
		// TODO Auto-generated method stub

	}

}
