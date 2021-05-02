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
package fr.univamu.ism.docometre.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Spinner;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class OrganizeSessionWizardPage extends WizardPage {
	
	public static String PageName = "OrganizeSessionWizardPage";
	
	private enum DistributionMethod {
		SELECTED_TRIALS_LIST,
		SELECTED_TRIALS_LIST_RANDOM_1,
		SELECTED_TRIALS_LIST_RANDOM_2,
		PACKET_TRIALS_LIST,
		PACKET_TRIALS_LIST_RANDOM,
	}
	
	class ProcessLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			IResource resource = (IResource)element;
			String name = resource.getName().replaceAll(Activator.processFileExtension, "");
			name = name + " (" + resource.getParent().getFullPath().toOSString() + ")";
			Integer occurences = selectedProcessesOccurences.get(resource);
			if(occurences != null) name = name + " (" + String.valueOf(occurences) + ")";
			return name;
		}
	}
	
	private ArrayList<IResource> selectedProcesses = new ArrayList<IResource>();
	private HashMap<IResource, Integer> selectedProcessesOccurences = new HashMap<IResource, Integer>();
	private ArrayList<IResource> availableProcesses = new ArrayList<IResource>();
	private ListViewer availableProcessesListViewer;
	private ListViewer selectedProcessesListViewer;
	private DistributionMethod distributionMethod = DistributionMethod.SELECTED_TRIALS_LIST;
	private String trialsList;
	private int packetSize = 1;
	
	private ArrayList<Integer> trialsNumbers = new ArrayList<Integer>();
	// Trial number / associated Process
	private HashMap<Integer, IResource> resultAssociation = new HashMap<Integer, IResource>();
	private Random random;
	private ListViewer resultListViewer;

	public OrganizeSessionWizardPage() {
		super(PageName);
		setTitle(DocometreMessages.OrganizeSessionWizardPageTitle);
		setDescription(DocometreMessages.OrganizeSessionWizardPageDescription);
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.ORGANIZE_SESSION_WIZBAN));
	}

	@Override
	public void createControl(Composite parent) {
		random = new Random();
		
		Composite mainContainer = new Composite(parent, SWT.NONE);

		setControl(mainContainer);
		GridLayout gl_mainContainer = new GridLayout(2, true);
		gl_mainContainer.marginWidth = 0;
		gl_mainContainer.marginHeight = 0;
		mainContainer.setLayout(gl_mainContainer);
		
		// Left container
		Composite leftContainer = new Composite(mainContainer, SWT.NONE);
		leftContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		leftContainer.setLayout(new GridLayout(2, false));
		
		// Available processes
		Label availabelProcessesLabel = new Label(leftContainer, SWT.NONE);
		availabelProcessesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		availabelProcessesLabel.setText(DocometreMessages.OrganizeSessionWizardPage_availabelProcessesLabel);
		availableProcessesListViewer = new ListViewer(leftContainer, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		availableProcessesListViewer.setContentProvider(new ArrayContentProvider());
		availableProcessesListViewer.setLabelProvider(new ProcessLabelProvider());
		availableProcessesListViewer.setComparator(new ViewerComparator());
		List availableProcessesList = availableProcessesListViewer.getList();
		availableProcessesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
		IResource[] elements = ResourceProperties.getAllTypedResources(ResourceType.PROCESS, ((NewResourceWizard)getWizard()).getParentResource().getProject(), null);
		for (IResource item : elements) {
			availableProcesses.add(item);
		}
		availableProcessesListViewer.setInput(availableProcesses);
		Button addButton = new Button(leftContainer, SWT.FLAT);
		addButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		addButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		addButton.setToolTipText(DocometreMessages.OrganizeSessionWizardPage_addButton);
		
		Button addAllButton = new Button(leftContainer, SWT.FLAT);
		addAllButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		addAllButton.setImage(Activator.getImage(IImageKeys.ADD_ALL_ICON));
		addAllButton.setToolTipText(DocometreMessages.OrganizeSessionWizardPage_addAllButton);
		
		// Selected processes
		Label selectedProcessesLabel = new Label(leftContainer, SWT.NONE);
		selectedProcessesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		selectedProcessesLabel.setText(DocometreMessages.OrganizeSessionWizardPage_selectedProcessesLabel);
		selectedProcessesListViewer = new ListViewer(leftContainer, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		List selectedProcessesList = selectedProcessesListViewer.getList();
		selectedProcessesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4));
		selectedProcessesListViewer.setContentProvider(new ArrayContentProvider());
		selectedProcessesListViewer.setLabelProvider(new ProcessLabelProvider());
		selectedProcessesListViewer.setInput(selectedProcesses);

		Button removeButton = new Button(leftContainer, SWT.FLAT);
		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		removeButton.setImage(Activator.getImage(IImageKeys.REMOVE_ICON));
		removeButton.setToolTipText(DocometreMessages.OrganizeSessionWizardPage_removeButton);
		
		Button removeAllButton = new Button(leftContainer, SWT.FLAT);
		removeAllButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		removeAllButton.setImage(Activator.getImage(IImageKeys.REMOVE_ALL_ICON));
		removeAllButton.setToolTipText(DocometreMessages.OrganizeSessionWizardPage_removeAllButton);
		
		Button moveUpButton = new Button(leftContainer, SWT.FLAT);
		moveUpButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		moveUpButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		moveUpButton.setToolTipText(DocometreMessages.Up_Label);
		moveUpButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object element = selectedProcessesListViewer.getStructuredSelection().getFirstElement();
				int index = selectedProcesses.indexOf(element);
				if(index >= 1) {
					selectedProcessesListViewer.setSelection(new StructuredSelection(element));
					selectedProcesses.add(index - 1, (IResource) element);
					selectedProcesses.remove(index + 1);
					selectedProcessesListViewer.refresh();
				}
			}
		});
		
		Button moveDownButton = new Button(leftContainer, SWT.FLAT);
		moveDownButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		moveDownButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		moveDownButton.setToolTipText(DocometreMessages.Down_Label);
		moveDownButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object element = selectedProcessesListViewer.getStructuredSelection().getFirstElement();
				int index = selectedProcesses.indexOf(element);
				if(index < selectedProcesses.size() - 1 && index > -1) {
					selectedProcessesListViewer.setSelection(new StructuredSelection(element));
					selectedProcesses.add(index + 2, (IResource) element);
					selectedProcesses.remove(index);
					selectedProcessesListViewer.refresh();
				}
			}
		});
		
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object[] selection = availableProcessesListViewer.getStructuredSelection().toArray();
				addProcessesHandler(selection);
			}
		});
		
		addAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				availableProcessesListViewer.getList().selectAll();
				Object[] selection = availableProcessesListViewer.getStructuredSelection().toArray();
				addProcessesHandler(selection);
			}
		});
		
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object[] selection = selectedProcessesListViewer.getStructuredSelection().toArray();
				removeProcessesHandler(selection);
			}
		});
		
		removeAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedProcessesListViewer.getList().selectAll();
				Object[] selection = selectedProcessesListViewer.getStructuredSelection().toArray();
				removeProcessesHandler(selection);
			}
		});
		
		// Right container
		Composite rightContainer = new Composite(mainContainer, SWT.NONE);
		rightContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gl_rightContainer = new GridLayout(2, false);
		gl_rightContainer.marginBottom = 5;
		gl_rightContainer.marginHeight = 0;
		gl_rightContainer.marginWidth = 0;
		gl_rightContainer.horizontalSpacing = 0;
		rightContainer.setLayout(gl_rightContainer);
		
		CTabFolder selectDistributionMethodTabFolder = new CTabFolder(rightContainer, SWT.BORDER);
		selectDistributionMethodTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		selectDistributionMethodTabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		
		CTabItem selectedTrialsTabItem = new CTabItem(selectDistributionMethodTabFolder, SWT.NONE);
		selectedTrialsTabItem.setText(DocometreMessages.OrganizeSessionWizardPage_selectedTrialsTabItem);
		
		Composite selectedTrialsContainer = new Composite(selectDistributionMethodTabFolder, SWT.NONE);
		selectedTrialsTabItem.setControl(selectedTrialsContainer);
		selectedTrialsContainer.setLayout(new GridLayout(2, false));
		
		Label selectedTrialsLabel = new Label(selectedTrialsContainer, SWT.NONE);
		selectedTrialsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		selectedTrialsLabel.setText(DocometreMessages.OrganizeSessionWizardPage_selectedTrialsLabel);
		
		Text trialsListText = new Text(selectedTrialsContainer, SWT.BORDER);
		trialsListText.setText(DocometreMessages.OrganizeSessionWizardPage_trialsListText);
		trialsList = trialsListText.getText();
		trialsListText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		trialsListText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setErrorMessage(null);
				trialsList = trialsListText.getText();
				Pattern pattern = Pattern.compile("^(?!([ \\d]*-){2})\\d+(?: *[-,:;] *\\d+)*$");
				Matcher matcher = pattern.matcher(trialsList);
				if(!matcher.matches()) {
					setErrorMessage(DocometreMessages.BadTrialsList);
				}
			}
		});
		
		Group selectedTrialsGroup = new Group(selectedTrialsContainer, SWT.NONE);
		selectedTrialsGroup.setLayout(new GridLayout(1, false));
		selectedTrialsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		selectedTrialsGroup.setText(DocometreMessages.OrganizeSessionWizardPage_selectedTrialsGroup);
		
		Button selectedProcessesListButton = new Button(selectedTrialsGroup, SWT.RADIO);
		selectedProcessesListButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		selectedProcessesListButton.setSelection(true);
		selectedProcessesListButton.setText(DocometreMessages.OrganizeSessionWizardPage_selectedProcessesListButton);
		
		Button randomDistributionButton1 = new Button(selectedTrialsGroup, SWT.RADIO);
		randomDistributionButton1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		randomDistributionButton1.setText(DocometreMessages.OrganizeSessionWizardPage_randomDistributionButton1);
		
		Button randomDistributionButton2 = new Button(selectedTrialsGroup, SWT.RADIO);
		randomDistributionButton2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		randomDistributionButton2.setText(DocometreMessages.OrganizeSessionWizardPage_randomDistributionButton2);
		
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(selectedProcessesListButton.getSelection()) distributionMethod = DistributionMethod.SELECTED_TRIALS_LIST;
				if(randomDistributionButton1.getSelection()) distributionMethod = DistributionMethod.SELECTED_TRIALS_LIST_RANDOM_1;
				if(randomDistributionButton2.getSelection()) distributionMethod = DistributionMethod.SELECTED_TRIALS_LIST_RANDOM_2;
			}
		};
		
		selectedProcessesListButton.addSelectionListener(selectionAdapter);
		randomDistributionButton1.addSelectionListener(selectionAdapter);
		randomDistributionButton2.addSelectionListener(selectionAdapter);
		
		CTabItem packTrialsTabItem = new CTabItem(selectDistributionMethodTabFolder, SWT.NONE);
		packTrialsTabItem.setText(DocometreMessages.OrganizeSessionWizardPage_packTrialsTabItem);
		
		Composite packTrialsContainer = new Composite(selectDistributionMethodTabFolder, SWT.NONE);
		packTrialsTabItem.setControl(packTrialsContainer);
		packTrialsContainer.setLayout(new GridLayout(2, false));
		
		Label packTrialsSizeLabel = new Label(packTrialsContainer, SWT.NONE);
		packTrialsSizeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		packTrialsSizeLabel.setText(DocometreMessages.OrganizeSessionWizardPage_packTrialsSizeLabel);
		
//		Text packTrialsSizeText = new Text(packTrialsContainer, SWT.BORDER);
//		packTrialsSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		packTrialsSizeText.setText(DocometreMessages.OrganizeSessionWizardPage_packTrialsSizeText);
		
		Spinner packTrialsSizeSpinner = new Spinner(packTrialsContainer, SWT.BORDER);
		packTrialsSizeSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		packTrialsSizeSpinner.setMinimum(1);
		packTrialsSizeSpinner.setMaximum(100000);
		packTrialsSizeSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setErrorMessage(null);
				packetSize = 0;
				String packetSizeString = packTrialsSizeSpinner.getText();
				Pattern pattern = Pattern.compile("^\\d+$");
				Matcher matcher = pattern.matcher(packetSizeString);
				if(!matcher.matches()) {
					setErrorMessage(DocometreMessages.BadPacketTrialsSize);
				} else packetSize = packTrialsSizeSpinner.getSelection();
			}
		});
		packTrialsSizeSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				packTrialsSizeSpinner.setSelection(packTrialsSizeSpinner.getSelection() + e.count);
			}
		});
		
		Group packTrialsGroup = new Group(packTrialsContainer, SWT.NONE);
		packTrialsGroup.setLayout(new GridLayout(1, false));
		packTrialsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true, 2, 1));
		packTrialsGroup.setText(DocometreMessages.OrganizeSessionWizardPage_packTrialsGroup);
		
		Button selectedProcessesListButton2 = new Button(packTrialsGroup, SWT.RADIO);
		selectedProcessesListButton2.setSelection(true);
		selectedProcessesListButton2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		selectedProcessesListButton2.setText(DocometreMessages.OrganizeSessionWizardPage_selectedProcessesListButton2);
		
		Button randomDistributionButton3 = new Button(packTrialsGroup, SWT.RADIO);
		randomDistributionButton3.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		randomDistributionButton3.setText(DocometreMessages.OrganizeSessionWizardPage_randomDistributionButton3);
		
		selectedProcessesListButton2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(selectedProcessesListButton2.getSelection()) distributionMethod = DistributionMethod.PACKET_TRIALS_LIST;
				if(randomDistributionButton3.getSelection()) distributionMethod = DistributionMethod.PACKET_TRIALS_LIST_RANDOM;
			}
		});
		
		selectDistributionMethodTabFolder.setSelection(selectedTrialsTabItem);
		
		Label resultLabel = new Label(rightContainer, SWT.NONE);
		resultLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		resultLabel.setText(DocometreMessages.OrganizeSessionWizardPage_resultLabel);
		
		selectDistributionMethodTabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(e.item == selectedTrialsTabItem) {
					if(selectedProcessesListButton.getSelection()) distributionMethod = DistributionMethod.SELECTED_TRIALS_LIST;
					if(randomDistributionButton1.getSelection()) distributionMethod = DistributionMethod.SELECTED_TRIALS_LIST_RANDOM_1;
					if(randomDistributionButton2.getSelection()) distributionMethod = DistributionMethod.SELECTED_TRIALS_LIST_RANDOM_2;
				}
				if(e.item == packTrialsTabItem) {
					if(selectedProcessesListButton2.getSelection()) distributionMethod = DistributionMethod.PACKET_TRIALS_LIST;
					if(randomDistributionButton3.getSelection()) distributionMethod = DistributionMethod.PACKET_TRIALS_LIST_RANDOM;
				}
			}
		});
		
		Button applyButton = new Button(rightContainer, SWT.FLAT);
		applyButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		applyButton.setImage(Activator.getImage(IImageKeys.APPLY_ICON));
		applyButton.setToolTipText(DocometreMessages.OrganizeSessionWizardPage_applyButton);
		
		resultListViewer = new ListViewer(rightContainer, SWT.BORDER | SWT.V_SCROLL);
		resultListViewer.setContentProvider(new ArrayContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if(inputElement instanceof HashMap) return resultAssociation.entrySet().toArray();
				return super.getElements(inputElement);
			}
		});
		resultListViewer.setLabelProvider(new LabelProvider() {
			@SuppressWarnings("rawtypes")
			@Override
			public String getText(Object element) {
				if(element instanceof Entry) {
					Integer trialNumber = (Integer)((Entry)element).getKey();
					IResource resource = (IResource)((Entry)element).getValue();
					return DocometreMessages.Trial + trialNumber + " : " + resource.getName().replaceAll(Activator.processFileExtension + "$", "");
				}
				return super.getText(element);
			}
		});
		List resultList = resultListViewer.getList();
		resultList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		GridData layout = (GridData)resultList.getLayoutData();
		layout.minimumHeight = 200;
		
		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				applyHandler();
			}
		});
		resultListViewer.setInput(resultAssociation);
	}

	private void addProcessesHandler(Object[] selection) {
		removeAddProcesses(true, selection);
	}
	
	private void removeProcessesHandler(Object[] selection) {
		removeAddProcesses(false, selection);
	}
	
	private void removeAddProcesses(boolean add, Object[] selection) {
		for (Object item : selection) {
			if(add) {
				availableProcesses.remove(item);
				selectedProcesses.add((IResource) item);
			} else {
				selectedProcesses.remove(item);
				availableProcesses.add((IResource) item);
			}
		}
		selectedProcessesListViewer.refresh();
		availableProcessesListViewer.refresh();
	}
	
	protected void applyHandler() {
		try {
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			pmd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(DocometreMessages.ApplyWait, 100);
					resultAssociation.clear();
					selectedProcessesOccurences.clear();
					System.out.println("distributionMethod : " + distributionMethod);
					System.out.println("trialsList : " + trialsList);
					System.out.println("packetSize : " + packetSize);

					monitor.subTask(DocometreMessages.BuildTrialsList);
					buildTrialsList(monitor);
					monitor.worked(20);
					if(distributionMethod.equals(DistributionMethod.SELECTED_TRIALS_LIST)) {
						applySelectedTrialsList(monitor);
					}
					if(distributionMethod.equals(DistributionMethod.SELECTED_TRIALS_LIST_RANDOM_1)) {
						applySelectedTrialsListRandom1(monitor);
					}
					if(distributionMethod.equals(DistributionMethod.SELECTED_TRIALS_LIST_RANDOM_2)) {
						applySelectedTrialsListRandom2(monitor);
					}
					if(distributionMethod.equals(DistributionMethod.PACKET_TRIALS_LIST))
						applyPackedTrialsList(monitor);
					if(distributionMethod.equals(DistributionMethod.PACKET_TRIALS_LIST_RANDOM))
						applyPackedTrialsListRandom(monitor);
					monitor.worked(70);
					monitor.subTask(DocometreMessages.RefreshViewers);
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							resultListViewer.refresh();
							selectedProcessesListViewer.refresh();
							availableProcessesListViewer.refresh();
						}
					});

					monitor.done();
					
					OrganizeSessionWizardPage.this.getShell().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							((WizardPage)OrganizeSessionWizardPage.this.getPreviousPage()).setPageComplete(true);
							OrganizeSessionWizardPage.this.setPageComplete(true);
							
						}
					});
					
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		} 
	}

	private void buildTrialsList(IProgressMonitor monitor) {
		trialsNumbers.clear();
		trialsList = trialsList.replaceAll(";", ",");
		trialsList = trialsList.replaceAll(":", "-");
		String[] trialsListArray = trialsList.split(",");
		for (String trials : trialsListArray) {
			if(trials.contains("-")) {
				int trialMin = Integer.valueOf(trials.split("-")[0]);
				int trialMax = Integer.valueOf(trials.split("-")[1]);
				if(trialMax >= trialMax) {
					for(int i = trialMin; i <= trialMax; i++) trialsNumbers.add(i);
				}
			} else {
				int trial =  Integer.valueOf(trials);
				trialsNumbers.add(trial);
			}
			if(monitor.isCanceled()) throw new OperationCanceledException();
		}
	}

	private void applyPackedTrialsListRandom(IProgressMonitor monitor) {
		int index = 0;
		int nbTrials = ((NewResourceWizard)getWizard()).getTrialsNumber();
		double mean = 1f*nbTrials / (1f*selectedProcesses.size());
		while (index < nbTrials) {
			int selectedProcessNumber = random.nextInt(selectedProcesses.size());
			Integer occurence = selectedProcessesOccurences.get(selectedProcesses.get(selectedProcessNumber));
			if(occurence == null || occurence <= mean) {
				for (int i = 0; i < packetSize; i++) {
					if(index < nbTrials) {
						monitor.subTask(DocometreMessages.ChooseProcess + DocometreMessages.Trial + index + 1);
						putAssociation(index + 1, selectedProcesses.get(selectedProcessNumber));
						index++;
					}
				}
			}
			if(monitor.isCanceled()) throw new OperationCanceledException();
		}
		
	}

	private void applyPackedTrialsList(IProgressMonitor monitor) {
		if(selectedProcesses.size() > 0) {
			int selectedProcessNumber = 0; 
			int nbTrials = ((NewResourceWizard)getWizard()).getTrialsNumber();
			for (int trialNumber = 0; trialNumber < nbTrials; trialNumber++) {
				selectedProcessNumber = trialNumber / packetSize;
				selectedProcessNumber = selectedProcessNumber % selectedProcesses.size();
				String message = NLS.bind(DocometreMessages.AssociatingProcess, selectedProcesses.get(selectedProcessNumber).getName().replaceAll(Activator.processFileExtension + "$", ""), String.valueOf(trialNumber + 1));
				monitor.subTask(message);
				putAssociation(trialNumber + 1, selectedProcesses.get(selectedProcessNumber));
				if(monitor.isCanceled()) throw new OperationCanceledException();
			}
		}	
	}

	private void applySelectedTrialsListRandom2(IProgressMonitor monitor) {
		if(trialsNumbers.size() > 0 && selectedProcesses.size() > 0) {
			double mean = 1f*trialsNumbers.size() / (1f*selectedProcesses.size());
			int index = 0;
			long t0 = System.currentTimeMillis();
			redo : while (index < trialsNumbers.size()) {
				Integer trialNumber = trialsNumbers.get(index);
				monitor.subTask(DocometreMessages.ChooseProcess + DocometreMessages.Trial + trialNumber);
				int selectedProcessNumber = random.nextInt(selectedProcesses.size());
				Integer occurence = selectedProcessesOccurences.get(selectedProcesses.get(selectedProcessNumber));
				if(System.currentTimeMillis() - t0 > 500) {
					System.out.println("Time out at " + index + " - Redo"); 
					index = 0;
					t0 = System.currentTimeMillis();
					resultAssociation.clear();
					selectedProcessesOccurences.clear();
					continue redo;
				}
				if(occurence == null || occurence < mean) {
					if(index == 0 || !resultAssociation.get(trialsNumbers.get(index - 1)).equals(selectedProcesses.get(selectedProcessNumber))) {
						putAssociation(trialNumber, selectedProcesses.get(selectedProcessNumber));
						index++;
						t0 = System.currentTimeMillis();
					}
				}
				if(monitor.isCanceled()) throw new OperationCanceledException();
			}
		}
		
//		procedure TAppliqueRepartEssaisDLGForm.appliquerAleat2(listeEssai : array of integer;taillePaquet : integer);//Appliquer aleat2
//		var i,numST,nbEssais,n,m,o : integer;
//		    mean : double;
//			occurences : array of integer;
//		label goAgain;
//		begin
//			if high(listeEssai) > -1 then//taille paquet=1
//		  begin
//
//				nbEssais := 0;
//				for i := 0 to high(listeEssai)
//				do if listeEssai[i] > -1 then inc(nbEssais);
//				mean := nbEssais/STSelectListBox.Count;
//		goAgain:
//				setLength(occurences,STSelectListBox.Count);
//				i := 0;
//				repeat
//					if listeEssai[i] > -1 then
//					begin
//						numST := random(STSelectListBox.Count);
//						if occurences[numST] < mean then
//						begin
//							if (i>0)then
//							begin
//								if (STSelectListBox.Items[numST] <> DocDOCOMETRE.SESSIONS[numSession].ESSAIS[listeEssai[i-1]].NOMST) then
//								begin
//									DocDOCOMETRE.SESSIONS[numSession].ESSAIS[listeEssai[i]].NOMST := STSelectListBox.Items[numST];
//									inc(occurences[numST]);
//									inc(i);
//								end else
//								begin
//									n := 0;
//									o := 0;
//									for m := 0 to STSelectListBox.Count - 1
//									do if occurences[m] >= mean then inc(n) else o := m;
//									if n = STSelectListBox.Count - 1 then
//									begin
//										if i < nbEssais then
//										begin
//											
//											DocDOCOMETRE.SESSIONS[numSession].ESSAIS[listeEssai[i]].NOMST := STSelectListBox.Items[o];
//											inc(occurences[o]);
//											inc(i);
//										end;
//									end;
//								end;
//							end else
//							begin
//								DocDOCOMETRE.SESSIONS[numSession].ESSAIS[listeEssai[i]].NOMST := STSelectListBox.Items[numST];
//								inc(occurences[numST]);
//								inc(i);
//							end;
//						end;
//					end;
//				until i = nbEssais;
//				setLength(occurences,0);
//				if DocDOCOMETRE.SESSIONS[numSession].ESSAIS[listeEssai[i-1]].NOMST = DocDOCOMETRE.SESSIONS[numSession].ESSAIS[listeEssai[i-2]].NOMST
//				then goto goAgain;
//			end else
//			begin
//				{ds ce cas il n'y a pas de repartition par paquet}
//			end;
//		end;
		
	}

	private void applySelectedTrialsListRandom1(IProgressMonitor monitor) {
		if(trialsNumbers.size() > 0 && selectedProcesses.size() > 0) {
			double mean = 1f*trialsNumbers.size() / (1f*selectedProcesses.size());
			int index = 0;
			while (index < trialsNumbers.size()) {
				Integer trialNumber = trialsNumbers.get(index);
				monitor.subTask(DocometreMessages.ChooseProcess + DocometreMessages.Trial + trialNumber);
				int selectedProcessNumber = random.nextInt(selectedProcesses.size());
				Integer occurence = selectedProcessesOccurences.get(selectedProcesses.get(selectedProcessNumber));
				if(occurence == null || occurence < mean ) {
					putAssociation(trialNumber, selectedProcesses.get(selectedProcessNumber));
					index++;
				}
				if(monitor.isCanceled()) throw new OperationCanceledException();
			}
		}
		
		//procedure TAppliqueRepartEssaisDLGForm.appliquerAleat1(listeEssai : array of integer;taillePaquet : integer);//Appliquer aleat1
		//var i,numST,nbEssais,n : integer;
//		    mean : double;
//		    occurences : array of integer;
		//begin
		//  if high(listeEssai) > -1 then//taille paquet=1
		//  begin
//		    nbEssais := 0;
//		    for i := 0 to high(listeEssai)
//		    do if listeEssai[i] > -1 then inc(nbEssais);
//		    mean := nbEssais/STSelectListBox.Count;
//		    setLength(occurences,STSelectListBox.Count);
//		    i := 0;
//		    repeat
//		      if listeEssai[i] > -1 then
//		      begin
//		        numST := random(STSelectListBox.Count);
//		        if occurences[numST] < mean then
//		        begin
//		          DocDOCOMETRE.SESSIONS[numSession].ESSAIS[listeEssai[i]].NOMST := STSelectListBox.Items[numST];
//		          inc(occurences[numST]);
//		          inc(i);
//		        end;
//		      end;
//		    until i = nbEssais;
//		    setLength(occurences,0);
		//  end else
		//  begin
//		    mean := DocDOCOMETRE.SESSIONS[numSession].ESSAIS.Count/STSelectListBox.Count;
//		    setLength(occurences,STSelectListBox.Count);
//		    i := 0;
//		    repeat
//		      numST := random(STSelectListBox.Count);
//					n := 0;
//					repeat
//						if occurences[numST] < mean then
//						begin
//							DocDOCOMETRE.SESSIONS[numSession].ESSAIS[i].NOMST := 
		//STSelectListBox.Items[numST];
//							inc(i);
//							inc(occurences[numST]);
//						end;
//						inc(n);
//					until n = taillePaquet;
//				until i = DocDOCOMETRE.SESSIONS[numSession].ESSAIS.Count;
//			end;
		//end;
		
	}

	private void applySelectedTrialsList(IProgressMonitor monitor) {
		if(trialsNumbers.size() > 0 && selectedProcesses.size() > 0) {
			int selectedProcessNumber = 0;
			for (Integer trialNumber: trialsNumbers) {
				String message = NLS.bind(DocometreMessages.AssociatingProcess, selectedProcesses.get(selectedProcessNumber).getName().replaceAll(Activator.processFileExtension + "$", ""), trialNumber.toString());
				monitor.subTask(message);
				putAssociation(trialNumber, selectedProcesses.get(selectedProcessNumber));
				selectedProcessNumber++;
				selectedProcessNumber = selectedProcessNumber % selectedProcesses.size();
				if(monitor.isCanceled()) throw new OperationCanceledException();
			}
		}	
//		if high(listeEssai) > -1 then//taille paquet=1
//		  begin
//		    numST := 0;
//		    for i := 0 to high(listeEssai) do
//		    begin
//		      if listeEssai[i] > -1 then
//		      begin
//		        DocDOCOMETRE.SESSIONS[numSession].ESSAIS[listeEssai[i]].NOMST := STSelectListBox.Items[numST];
//		        inc(numST);
//		        numST := numST mod STSelectListBox.Count;
//		        //if numST = STSelectListBox.Count then numST :=0;
//		      end;
//		    end;
//		  end else
//		  begin
//		    numST := 0;
//		    i := 0;
//		    repeat
//		      DocDOCOMETRE.SESSIONS[numSession].ESSAIS[i].NOMST := STSelectListBox.Items[numST];
//		      inc(i);
//		      numST := (i - (taillePaquet*STSelectListBox.Count)*(i div (taillePaquet*STSelectListBox.Count))) div taillePaquet;
//		    until i = DocDOCOMETRE.SESSIONS[numSession].ESSAIS.Count;
//		  end;
	}

	private void putAssociation(Integer trialNumber, IResource process) {
		resultAssociation.put(trialNumber, process);
		Integer nbOccurences = selectedProcessesOccurences.get(process);
		if(nbOccurences != null) nbOccurences++;
		else nbOccurences = 1;
		selectedProcessesOccurences.put(process, nbOccurences);
		
	}

	public HashMap<Integer, IResource> getResultAssociation() {
		return resultAssociation;
	}
	
}
