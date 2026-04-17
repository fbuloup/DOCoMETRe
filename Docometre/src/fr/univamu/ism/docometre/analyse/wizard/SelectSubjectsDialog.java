package fr.univamu.ism.docometre.analyse.wizard;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;

public class SelectSubjectsDialog extends TitleAreaDialog {
	
	private String[] subjects;
	private ArrayList<String> selectedSubjects = new ArrayList<String>(0);
	private Object[] curentSelectedSubjects;

	public SelectSubjectsDialog(Shell parentShell, String[] subjects, Object[] curentSelectedSubjects) {
		super(parentShell);
		this.subjects = subjects;
		this.curentSelectedSubjects = curentSelectedSubjects;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(DocometreMessages.MergeSubjectsDialogTitle);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(DocometreMessages.MergeSubjectsDialogTitle);
		setMessage(DocometreMessages.MergeSubjectsSelectionDescription);
		setTitleImage(Activator.getImage(IImageKeys.MERGE_WIZARD));
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		ListViewer subjectsListViewer = new ListViewer(dialogArea, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		subjectsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		subjectsListViewer.setComparator(new MergeSubjectsViewerComparator());
		subjectsListViewer.setContentProvider(ArrayContentProvider.getInstance());
		subjectsListViewer.setLabelProvider(new LabelProvider());
		subjectsListViewer.setInput(subjects);
		subjectsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object[] elements = subjectsListViewer.getStructuredSelection().toArray();
				selectedSubjects.clear();
				for (int i = 0; i < elements.length; i++) {
					selectedSubjects.add((String) elements[i]);
				}
			}
		});
		StructuredSelection selection = new StructuredSelection(curentSelectedSubjects);
		subjectsListViewer.setSelection(selection);
		return dialogArea;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	public ArrayList<String> getSelection() {
		return selectedSubjects;
	}

}
