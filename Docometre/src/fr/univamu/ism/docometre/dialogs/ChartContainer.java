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
package fr.univamu.ism.docometre.dialogs;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.charts.ChartConfiguration;
import fr.univamu.ism.docometre.dacqsystems.charts.ChartConfigurationProperties;

public class ChartContainer extends Composite {

	private static List<Control> children;
	
//	private ChartConfiguration clonedChartConfiguration;
	private ChartConfiguration chartConfiguration;
	private int chartNumber;
	private ConfigureChartsLayoutDialog configureChartsLayoutDialog;

	public ChartContainer(Composite parent, int style, ChartConfiguration chartConfiguration, int chartNumber, ConfigureChartsLayoutDialog configureChartsLayoutDialog) {
		super(parent, style);
		this.configureChartsLayoutDialog = configureChartsLayoutDialog;
		this.chartConfiguration = chartConfiguration;
		this.chartNumber = chartNumber;
		int horizontalSpan = Integer.parseInt(this.chartConfiguration.getProperty(ChartConfigurationProperties.HORIZONTAL_SPANNING));
		int verticalSpan = Integer.parseInt(this.chartConfiguration.getProperty(ChartConfigurationProperties.VERTICAL_SPANNING));
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, horizontalSpan, verticalSpan));
		setLayout(new GridLayout(2, false));
		updateChildren();
		// Create move left/right tool bar
		createMoveToolbar();
		// Create chart area
		createChartArea();
		// Create horizontal span tool bar
		createHSpanToolbar();
		// Create vertical span tool bar
		createVSpanToolbar();
	}
	
	private void updateChildren() {
		children = Arrays.asList(ChartContainer.this.getParent().getChildren());
	}

	private void createVSpanToolbar() {
		Composite chartToolbar = new Composite(this, SWT.NONE);
		chartToolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 32, 1));
		chartToolbar.setLayout(new GridLayout(3,false));
		
		Button reduceVSpanButton = new Button(chartToolbar, SWT.PUSH | SWT.FLAT);
		reduceVSpanButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		reduceVSpanButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, true));
		reduceVSpanButton.setToolTipText(DocometreMessages.DecreaseVSpan_Tooltip);
		
		Button expandVSpanButton = new Button(chartToolbar, SWT.PUSH | SWT.FLAT);
		expandVSpanButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		expandVSpanButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		expandVSpanButton.setToolTipText(DocometreMessages.IncreaseVSpan_Tooltip);
		
		Label spanValueLabel = new Label(chartToolbar, SWT.NONE);
		spanValueLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		spanValueLabel.setText("" + ((GridData)getLayoutData()).verticalSpan);
		
		reduceVSpanButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				GridData gd = (GridData)ChartContainer.this.getLayoutData();
				gd.verticalSpan = gd.verticalSpan - 1;
				if(gd.verticalSpan == 0) gd.verticalSpan = 1;
				ChartContainer.this.getParent().layout();
				spanValueLabel.setText("" + gd.verticalSpan);
				chartConfiguration.setProperty(ChartConfigurationProperties.VERTICAL_SPANNING, String.valueOf(gd.verticalSpan));
				ChartContainer.this.configureChartsLayoutDialog.setModified();
			}
		});
		
		expandVSpanButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				GridData gd = (GridData)ChartContainer.this.getLayoutData();
				gd.verticalSpan = gd.verticalSpan + 1;
				ChartContainer.this.getParent().layout();
				spanValueLabel.setText("" + gd.verticalSpan);
				chartConfiguration.setProperty(ChartConfigurationProperties.VERTICAL_SPANNING, String.valueOf(gd.verticalSpan));
				ChartContainer.this.configureChartsLayoutDialog.setModified();
			}
		});
		
	}

	private void createHSpanToolbar() {
		Composite chartToolbar = new Composite(this, SWT.NONE);
		chartToolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		chartToolbar.setLayout(new GridLayout(1,true));
		
		Button reduceHSpanButton = new Button(chartToolbar, SWT.PUSH | SWT.FLAT);
		reduceHSpanButton.setImage(Activator.getImage(IImageKeys.LEFT_ICON));
		reduceHSpanButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));
		reduceHSpanButton.setToolTipText(DocometreMessages.DecreaseHSpan_Tooltip);
		
		Button expandHSpanButton = new Button(chartToolbar, SWT.PUSH | SWT.FLAT);
		expandHSpanButton.setImage(Activator.getImage(IImageKeys.RIGHT_ICON));
		expandHSpanButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		expandHSpanButton.setToolTipText(DocometreMessages.IncreaseHSpan_Tooltip);
		
		Label spanValueLabel = new Label(chartToolbar, SWT.NONE);
		spanValueLabel.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
		spanValueLabel.setText("" + ((GridData)getLayoutData()).horizontalSpan);
		
		reduceHSpanButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				GridData gd = (GridData)ChartContainer.this.getLayoutData();
				gd.horizontalSpan = gd.horizontalSpan - 1;
				if(gd.horizontalSpan == 0) gd.horizontalSpan = 1;
				ChartContainer.this.getParent().layout();
				spanValueLabel.setText("" + gd.horizontalSpan);
				chartConfiguration.setProperty(ChartConfigurationProperties.HORIZONTAL_SPANNING, String.valueOf(gd.horizontalSpan));
				ChartContainer.this.configureChartsLayoutDialog.setModified();
			}
		});
		
		expandHSpanButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				GridData gd = (GridData)ChartContainer.this.getLayoutData();
				gd.horizontalSpan = gd.horizontalSpan + 1;
				if(gd.horizontalSpan > ConfigureChartsLayoutDialog.chartsLayoutColumnsNumber) gd.horizontalSpan = ConfigureChartsLayoutDialog.chartsLayoutColumnsNumber;
				ChartContainer.this.getParent().layout();
				spanValueLabel.setText("" + gd.horizontalSpan);
				chartConfiguration.setProperty(ChartConfigurationProperties.HORIZONTAL_SPANNING, String.valueOf(gd.horizontalSpan));
				ChartContainer.this.configureChartsLayoutDialog.setModified();
			}
		});
		
	}

	private void createChartArea() {
		Composite chartArea = new Composite(this, SWT.NONE);
		chartArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chartArea.setLayout(new GridLayout());
		Label label = new Label(chartArea, SWT.NONE);
		label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		label.setText("" + (chartNumber + 1) + ". " + chartConfiguration.getLabel());
	}

	private void createMoveToolbar() {
		Composite chartToolbar = new Composite(this, SWT.NONE);
		chartToolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		chartToolbar.setLayout(new GridLayout(2,true));
		
		Button moveLeftButton = new Button(chartToolbar, SWT.PUSH | SWT.FLAT);
		moveLeftButton.setImage(Activator.getImage(IImageKeys.LEFT_ICON));
		moveLeftButton.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		moveLeftButton.setToolTipText(DocometreMessages.MoveChartLeft_Tooltip);
		moveLeftButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateChildren();
				int currentIndex = ChartContainer.children.indexOf(ChartContainer.this);
				if(currentIndex > 0) {
					Control control = ChartContainer.children.get(currentIndex - 1);
					ChartContainer.this.moveAbove(control);
					ChartContainer.this.getParent().layout();
					updateChildren();
					ChartContainer.this.configureChartsLayoutDialog.setModified();
				}
			}
		});
		
		Button moveRightButton = new Button(chartToolbar, SWT.PUSH | SWT.FLAT);
		moveRightButton.setImage(Activator.getImage(IImageKeys.RIGHT_ICON));
		moveRightButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		moveRightButton.setToolTipText(DocometreMessages.MoveChartRight_Tooltip);
		moveRightButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateChildren();
				int currentIndex = ChartContainer.children.indexOf(ChartContainer.this);
				if(currentIndex < ChartContainer.children.size() - 1) {
					Control control = ChartContainer.children.get(currentIndex + 1);
					ChartContainer.this.moveBelow(control);
					ChartContainer.this.getParent().layout();
					updateChildren();
					ChartContainer.this.configureChartsLayoutDialog.setModified();
				}
			}
		});
	}
	
	public ChartConfiguration getChartConfiguration() {
		return chartConfiguration;
	}

}
