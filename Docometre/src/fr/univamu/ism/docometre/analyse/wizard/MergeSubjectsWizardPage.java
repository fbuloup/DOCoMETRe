package fr.univamu.ism.docometre.analyse.wizard;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class MergeSubjectsWizardPage extends WizardPage {
	
	private ArrayList<String> selectedSubjectsFrom = new ArrayList<String>(0);
	private ArrayList<String> selectedSubjectsTo = new ArrayList<String>(0);
	private String[] availableSubjects;
	
	protected MergeSubjectsWizardPage() {
		super("MergeSubjectsWizardPage");
		setTitle(DocometreMessages.MergeSubjectsWizardPageTitle);
		setDescription(DocometreMessages.MergeSubjectsWizardPageDescription);
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.MERGE_WIZARD));
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, true));
		
		Label labelFrom = new Label(container, SWT.NONE);
		labelFrom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		labelFrom.setText(DocometreMessages.MergeSubjectsFromLabel);
		
		Label labelTo = new Label(container, SWT.NONE);
		labelTo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		labelTo.setText(DocometreMessages.MergeSubjectsToLabel);
		
		Composite subjectsContainer = new Composite(container, SWT.NONE);
		subjectsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		subjectsContainer.setLayout(new GridLayout(4, false));
		
		ListViewer fromListViewer = new ListViewer(subjectsContainer, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		fromListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite fromButtonsContainer = new Composite(subjectsContainer, SWT.NONE);
		fromButtonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		fromButtonsContainer.setLayout(new FillLayout(SWT.VERTICAL));
		
		Button fromUpButton = new Button(fromButtonsContainer, SWT.FLAT);
		fromUpButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		fromUpButton.setToolTipText(DocometreMessages.Up_Label);
		
		Button fromDownButton = new Button(fromButtonsContainer, SWT.FLAT);
		fromDownButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		fromDownButton.setToolTipText(DocometreMessages.Down_Label);
		
		Button fromAddButton = new Button(fromButtonsContainer, SWT.FLAT);
		fromAddButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		fromAddButton.setToolTipText(DocometreMessages.Add_Tooltip);
		
		Button fromDeleteButton = new Button(fromButtonsContainer, SWT.FLAT);
		fromDeleteButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		fromDeleteButton.setToolTipText(DocometreMessages.Delete_Tooltip);
		
		ListViewer toListViewer = new ListViewer(subjectsContainer, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		toListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite toButtonsContainer = new Composite(subjectsContainer, SWT.NONE);
		toButtonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		toButtonsContainer.setLayout(new FillLayout(SWT.VERTICAL));
		
		Button toUpButton = new Button(toButtonsContainer, SWT.FLAT);
		toUpButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		toUpButton.setToolTipText(DocometreMessages.Up_Label);
		
		Button toDownButton = new Button(toButtonsContainer, SWT.FLAT);
		toDownButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		toDownButton.setToolTipText(DocometreMessages.Down_Label);
		
		Button toAddButton = new Button(toButtonsContainer, SWT.FLAT);
		toAddButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		toAddButton.setToolTipText(DocometreMessages.Add_Tooltip);
		
		Button toDeleteButton = new Button(toButtonsContainer, SWT.FLAT);
		toDeleteButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		toDeleteButton.setToolTipText(DocometreMessages.Delete_Tooltip);
		
		fromListViewer.setContentProvider(ArrayContentProvider.getInstance());
		fromListViewer.setLabelProvider(new LabelProvider());
		toListViewer.setContentProvider(ArrayContentProvider.getInstance());
		toListViewer.setLabelProvider(new LabelProvider());
		
		if(MathEngineFactory.getMathEngine().isStarted()) {
			availableSubjects = MathEngineFactory.getMathEngine().getLoadedSubjects();
		}
		
		SelectionAdapter addSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object[] selectedSubjects = fromListViewer.getList().getItems();
				if(e.widget == toAddButton) selectedSubjects = toListViewer.getList().getItems();
				SelectSubjectsDialog selectSubjectsDialog = new SelectSubjectsDialog(Display.getDefault().getActiveShell(), availableSubjects, selectedSubjects);
				if(selectSubjectsDialog.open() == Window.OK) {
					if(e.widget == fromAddButton) {
						fromListViewer.setInput(selectSubjectsDialog.getSelection());
						selectedSubjectsFrom = selectSubjectsDialog.getSelection();
					}
					if(e.widget == toAddButton) {
						toListViewer.setInput(selectSubjectsDialog.getSelection());
						selectedSubjectsTo = selectSubjectsDialog.getSelection();
					}
				}
				setPageComplete(fromListViewer.getList().getItems().length == toListViewer.getList().getItems().length);
			}
		};
		fromAddButton.addSelectionListener(addSelectionAdapter);
		toAddButton.addSelectionListener(addSelectionAdapter);
		
		SelectionAdapter deleteSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(e.widget == fromDeleteButton) {
					selectedSubjectsFrom.removeAll(Arrays.asList(fromListViewer.getStructuredSelection().toArray()));
					fromListViewer.setInput(selectedSubjectsFrom.toArray());
				}
				if(e.widget == toDeleteButton) {
					selectedSubjectsTo.removeAll(Arrays.asList(toListViewer.getStructuredSelection().toArray()));
					toListViewer.setInput(selectedSubjectsTo.toArray());
				}
				setPageComplete(fromListViewer.getList().getItems().length == toListViewer.getList().getItems().length);
			}
		};
		fromDeleteButton.addSelectionListener(deleteSelectionAdapter);
		toDeleteButton.addSelectionListener(deleteSelectionAdapter);
		
		SelectionAdapter upSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(e.widget == fromUpButton) {
					Object[] selectedItems = fromListViewer.getStructuredSelection().toArray();
					for (Object item : selectedItems) 
						if(fromListViewer.getList().indexOf((String) item) == 0) return;
					for (Object item : selectedItems) {
						int index = selectedSubjectsFrom.indexOf(item);
						selectedSubjectsFrom.remove(index);
						index--;
						selectedSubjectsFrom.add(index, (String) item);
					}
					fromListViewer.refresh();
				}
				if(e.widget == toUpButton) {
					Object[] selectedItems = toListViewer.getStructuredSelection().toArray();
					for (Object item : selectedItems) 
						if(toListViewer.getList().indexOf((String) item) == 0) return;
					for (Object item : selectedItems) {
						int index = selectedSubjectsTo.indexOf(item);
						selectedSubjectsTo.remove(index);
						index--;
						selectedSubjectsTo.add(index, (String) item);
					}
					toListViewer.refresh();
				}	
			}
		};
		fromUpButton.addSelectionListener(upSelectionAdapter);
		toUpButton.addSelectionListener(upSelectionAdapter);
		
		SelectionAdapter downSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(e.widget == fromDownButton) {
					int nbItems = fromListViewer.getList().getItems().length;
					Object[] selectedItems = fromListViewer.getStructuredSelection().toArray();
					for (Object item : selectedItems) 
						if(fromListViewer.getList().indexOf((String) item) == nbItems - 1) return;
					for (int i = selectedItems.length - 1; i >= 0 ; i--) {
						int index = selectedSubjectsFrom.indexOf(selectedItems[i]);
						String item = selectedSubjectsFrom.remove(index);
						index++;
						selectedSubjectsFrom.add(index, (String) item);
					}
					fromListViewer.refresh();
				}
				if(e.widget == toDownButton) {
					int nbItems = toListViewer.getList().getItems().length;
					Object[] selectedItems = toListViewer.getStructuredSelection().toArray();
					for (Object item : selectedItems) 
						if(toListViewer.getList().indexOf((String) item) == nbItems - 1) return;
					for (int i = selectedItems.length - 1; i >= 0 ; i--) {
						int index = selectedSubjectsTo.indexOf(selectedItems[i]);
						String item = selectedSubjectsTo.remove(index);
						index++;
						selectedSubjectsTo.add(index, (String) item);
					}
					toListViewer.refresh();
				}	
			}
		};
		fromDownButton.addSelectionListener(downSelectionAdapter);
		toDownButton.addSelectionListener(downSelectionAdapter);
		
		setControl(container);
		
	}
	
	public String[] getSelectedSubjectFrom() {
		return selectedSubjectsFrom.toArray(new String[selectedSubjectsFrom.size()]);
	}
	
	public String[] getSelectedSubjectTo() {
		return selectedSubjectsTo.toArray(new String[selectedSubjectsTo.size()]);
	}

}
