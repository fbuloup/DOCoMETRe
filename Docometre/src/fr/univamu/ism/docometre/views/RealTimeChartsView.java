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
package fr.univamu.ism.docometre.views;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import fr.univamu.ism.docometre.AcquirePerspective;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.ExperimentScheduler;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.charts.ChartConfiguration;
import fr.univamu.ism.docometre.handlers.RunStopHandler;

public class RealTimeChartsView extends ViewPart implements IPerspectiveListener {
	
	public static String ID = "Docometre.RealTimeChartsView";
	
	private Label experimentLabelValue;
	private Label subjectLabelValue;
	private Label sessionLabelValue;
	private Label trialLabelValue;
	private Label processLabelValue;
	private Label runItemLabelValue;

	private Composite stateContainer;

	private PartListenerAdapter partListenerAdapter;

	private Composite chartsContainer;

	public RealTimeChartsView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		
		container.getDisplay().addFilter(SWT.KeyUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				IPerspectiveDescriptor currentPerspective = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getPerspective();
				boolean checkPerspcetive = currentPerspective.getId().equals(AcquirePerspective.id);
				boolean isRunning = ExperimentScheduler.getInstance().isRunning();
				if(event.keyCode == SWT.ESC) {
					if(checkPerspcetive && isRunning) RunStopHandler.stop();
				}
				if((event.keyCode == SWT.PAUSE) || (((event.stateMask & SWT.COMMAND) != 0) && event.keyCode == 'p')) {
					if(checkPerspcetive && isRunning) {
						ExperimentScheduler.getInstance().stop(true);
						ApplicationActionBarAdvisor.pausePendingContributionItem.setText(DocometreMessages.PausePending);
					}
				}
			}
		});
		
		stateContainer = new Composite(container, SWT.NONE);
		stateContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		stateContainer.setLayout(new GridLayout(12, false));
		((GridLayout)stateContainer.getLayout()).horizontalSpacing = 0;
		((GridLayout)stateContainer.getLayout()).marginHeight = 0;
		((GridLayout)stateContainer.getLayout()).marginWidth = 0;
		((GridLayout)stateContainer.getLayout()).verticalSpacing = 0;
		
		Label runItemLabel = new Label(stateContainer, SWT.NONE);
		runItemLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		runItemLabel.setText(DocometreMessages.ItemToRun_Label);
		
		runItemLabelValue = new Label(stateContainer, SWT.NONE);
		runItemLabelValue.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		runItemLabelValue.setText(DocometreMessages.NotAvailable_Label);
		
		Label experimentLabel = new Label(stateContainer, SWT.NONE);
		experimentLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		experimentLabel.setText(DocometreMessages.Experiment_Label);
		
		experimentLabelValue = new Label(stateContainer, SWT.NONE);
		experimentLabelValue.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		experimentLabelValue.setText(DocometreMessages.NotAvailable_Label);
		
		Label subjectLabel = new Label(stateContainer, SWT.NONE);
		subjectLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		subjectLabel.setText(DocometreMessages.Subject_Label);
		
		subjectLabelValue = new Label(stateContainer, SWT.NONE);
		subjectLabelValue.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		subjectLabelValue.setText(DocometreMessages.NotAvailable_Label);
		
		Label sessionLabel = new Label(stateContainer, SWT.NONE);
		sessionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		sessionLabel.setText(DocometreMessages.Session_Label);
		
		sessionLabelValue = new Label(stateContainer, SWT.NONE);
		sessionLabelValue.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		sessionLabelValue.setText(DocometreMessages.NotAvailable_Label);
		
		Label trialLabel = new Label(stateContainer, SWT.NONE);
		trialLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		trialLabel.setText(DocometreMessages.Trial_Label);
		
		trialLabelValue = new Label(stateContainer, SWT.NONE);
		trialLabelValue.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		trialLabelValue.setText(DocometreMessages.NotAvailable_Label);
		
		Label processLabel = new Label(stateContainer, SWT.NONE);
		processLabel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		processLabel.setText(DocometreMessages.Process_Label);
		
		processLabelValue = new Label(stateContainer, SWT.NONE);
		processLabelValue.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		processLabelValue.setText(DocometreMessages.NotAvailable_Label);
		
		chartsContainer = new Composite(container, SWT.BORDER);
		chartsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		IResource resource = ExperimentsView.currentSelectedResource;
		
		if(resource != null) {
			updateItemToLaunch(resource);
			updateValues(resource);
		}
		
		ExperimentScheduler.getInstance().initialize();
		
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == RealTimeChartsView.this) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().removePerspectiveListener(RealTimeChartsView.this);
				}
			}
			
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				update(partRef);
			}
			
			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				update(partRef);
			}
			
			private void update(IWorkbenchPartReference partRef) {

			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
		
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);

	}
	
	public void updateValues(IResource resource) {
		if(resource == null) {
			experimentLabelValue.setText(DocometreMessages.NotAvailable_Label);
			subjectLabelValue.setText(DocometreMessages.NotAvailable_Label);
			sessionLabelValue.setText(DocometreMessages.NotAvailable_Label);
			trialLabelValue.setText(DocometreMessages.NotAvailable_Label);
			processLabelValue.setText(DocometreMessages.NotAvailable_Label);
			return;
		}
		
		if(ResourceType.isExperiment(resource)) experimentLabelValue.setText(resource.getName());
		if(ResourceType.isSubject(resource)) subjectLabelValue.setText(resource.getName());
		if(ResourceType.isSession(resource)) sessionLabelValue.setText(resource.getName());
		if(ResourceType.isTrial(resource)) {
			int nbTrials = ResourceProperties.getTrialsNumber(resource.getParent());
			trialLabelValue.setText(resource.getName() + " / " + nbTrials);
		}
		if(ResourceType.isProcess(resource)) {
			processLabelValue.setText(resource.getName().replaceAll(Activator.processFileExtension + "$", ""));
			IContainer parent = resource.getParent();
			while(!ResourceType.isExperiment(parent)) parent = parent.getParent();
			experimentLabelValue.setText(parent.getName());
		}

		stateContainer.layout(true);
		
	}
	
	@Override
	public void dispose() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().removePerspectiveListener(this);
		super.dispose();
	}

	@Override
	public void setFocus() {

	}

	public void updateItemToLaunch(IResource resource) {
		if(resource == null) {
			runItemLabelValue.setText(DocometreMessages.NotAvailable_Label);
			return;
		}
		boolean update = ResourceType.isProcess(resource);
		update = update || ResourceType.isSubject(resource);
		update = update || ResourceType.isSession(resource);
		update = update || ResourceType.isTrial(resource);
		if(update) runItemLabelValue.setText(resource.getFullPath().toOSString());
		
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		if(perspective.getId().equals(AcquirePerspective.id)) {
			IResource resource = ExperimentsView.currentSelectedResource;
			updateItemToLaunch(resource);
			ExperimentScheduler.getInstance().initialize();
			removeAllCharts();
		}
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		// TODO Auto-generated method stub
		
	}
	
	private void removeAllCharts() {
		Control[] children = chartsContainer.getChildren();
		for (Control control : children) control.dispose();
	}

	public void updateCharts(Process process) {
		chartsContainer.setVisible(false);
		removeAllCharts();
		DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
		String value = dacqConfiguration.getProperty(DACQConfigurationProperties.CHARTS_LAYOUT_COLUMNS_NUMBER);
		int chartsLayoutColumnsNumber = Integer.parseInt(value);
		chartsContainer.setLayout(new GridLayout(chartsLayoutColumnsNumber, true));
		((GridLayout)chartsContainer.getLayout()).horizontalSpacing = 1;
		((GridLayout)chartsContainer.getLayout()).verticalSpacing = 1;
		((GridLayout)chartsContainer.getLayout()).marginHeight = 0;
		((GridLayout)chartsContainer.getLayout()).marginWidth = 0;
		ChartConfiguration[] chartsConfigurations = dacqConfiguration.getCharts().getChartsConfigurations();
		chartsContainer.setData("process", process);
		for (ChartConfiguration chartConfiguration : chartsConfigurations) {
			chartConfiguration.createChart(chartsContainer);
		}
		chartsContainer.setVisible(true);
		chartsContainer.requestLayout();
		chartsContainer.setFocus();
	}

}
