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

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.charts.ChartConfiguration;
import fr.univamu.ism.docometre.dacqsystems.charts.Charts;

public class ConfigureChartsLayoutDialog extends TitleAreaDialog {

	private DACQConfiguration dacqConfiguration;
	private Charts charts;
	protected static int chartsLayoutColumnsNumber = 1;
	private boolean modified;
	private Composite chartsContainer;

	public ConfigureChartsLayoutDialog(Shell parentShell, DACQConfiguration dacqConfiguration) {
		super(parentShell);
		this.dacqConfiguration = dacqConfiguration;
		try {
			charts = (Charts) dacqConfiguration.getCharts().clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		String value = dacqConfiguration.getProperty(DACQConfigurationProperties.CHARTS_LAYOUT_COLUMNS_NUMBER);
		if(value == null) Activator.logErrorMessage(DocometreMessages.ChartLayoutDialogErrorMessage_Title);
		else chartsLayoutColumnsNumber = Integer.parseInt(value);
	}
	
	@Override
	public void create() {
		super.create();
		getShell().setText(DocometreMessages.ConfigureChartsLayoutDialogShellTitle);
		setTitle(DocometreMessages.ConfigureChartsLayoutDialogTitle);
		setMessage(DocometreMessages.ConfigureChartsLayoutDialogMessage, IMessageProvider.INFORMATION);
	}
	
	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS | SWT.MAX | SWT.RESIZE | getDefaultOrientation());
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		// Toolbar
		Composite toolbar = new Composite(area, SWT.NONE);
		toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		toolbar.setLayout(new GridLayout(2,true));
		Button addColumnButton = new Button(toolbar, SWT.PUSH | SWT.FLAT);
		addColumnButton.setImage(Activator.getImage(IImageKeys.ADD_COLUMN_ICON));
		addColumnButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		addColumnButton.setToolTipText(DocometreMessages.AddColumn_Title);
		addColumnButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				chartsLayoutColumnsNumber++;
				chartsContainer.setLayout(new GridLayout(chartsLayoutColumnsNumber, true));
				chartsContainer.layout();
				modified = true;
			}
		});
		Button deleteColumnButton = new Button(toolbar, SWT.PUSH | SWT.FLAT);
		deleteColumnButton.setImage(Activator.getImage(IImageKeys.DELETE_COLUMN_ICON));
		deleteColumnButton.setToolTipText(DocometreMessages.DeleteColumn_Title);
		deleteColumnButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		deleteColumnButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				chartsLayoutColumnsNumber--;
				if(chartsLayoutColumnsNumber == 0) chartsLayoutColumnsNumber = 1;
				chartsContainer.setLayout(new GridLayout(chartsLayoutColumnsNumber, true));
				chartsContainer.layout();
				modified = true;
			}
		});
		
		// Build the separator line
		(new Label(area, SWT.HORIZONTAL | SWT.SEPARATOR)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// Charts container
		chartsContainer = new Composite(area, SWT.NONE);
		chartsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chartsContainer.setLayout(new GridLayout(chartsLayoutColumnsNumber, true));
		
		ChartConfiguration[] chartsConfigurations = charts.getChartsConfigurations();
		int i = 0;
		for (ChartConfiguration chartConfiguration : chartsConfigurations) {
			new ChartContainer(chartsContainer, SWT.BORDER, chartConfiguration, i++, this);
		}

		// Build the separator line
		(new Label(area, SWT.HORIZONTAL | SWT.SEPARATOR)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		return area;
	}
	
	@Override
	protected void okPressed() {
		if(modified) {
			dacqConfiguration.setProperty(DACQConfigurationProperties.CHARTS_LAYOUT_COLUMNS_NUMBER, Integer.toString(chartsLayoutColumnsNumber));
			Control[] controls = chartsContainer.getChildren();
			ChartConfiguration[] chartsConfigurations = new ChartConfiguration[controls.length];
			int i = 0;
			for (Control control : controls) {
				chartsConfigurations[i] = ((ChartContainer)control).getChartConfiguration();
				i++;
			}
			charts.setChartsConfigurations(chartsConfigurations);
			dacqConfiguration.setCharts(charts);
		}
		super.okPressed();
	}
	
	public void setModified() {
		modified = true;
	}
	
	public boolean getModified() {
		return modified;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
}
