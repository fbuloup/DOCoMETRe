package fr.univamu.ism.docometre.dialogs;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;

public class CompileDialog extends TitleAreaDialog {
	
	protected Object[] selectedExperiments;

	public CompileDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE); 
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(DocometreMessages.CompileDialog_ShellTitle);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(DocometreMessages.CompileDialog_Title);
		setMessage(DocometreMessages.CompileDialog_Message);
		setTitleImage(Activator.getImage(IImageKeys.BUILD_WIZBAN));
		Composite container = (Composite) super.createDialogArea(parent);
		
		ListViewer experimentsListViewer = new ListViewer(container);
		experimentsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		experimentsListViewer.setContentProvider(new ArrayContentProvider());
		experimentsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if(element instanceof IResource) return GetResourceLabelDelegate.getLabel((IResource)element);
				return null;
			}
		});
		experimentsListViewer.setComparator(new ViewerComparator());
		experimentsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection selection = (StructuredSelection) experimentsListViewer.getSelection();
				selectedExperiments = selection.toArray();
			}
		});
		
		IResource[] experiments = ResourceProperties.getAllTypedResources(ResourceType.EXPERIMENT, ResourcesPlugin.getWorkspace().getRoot(), null);
		experimentsListViewer.setInput(experiments);
		
		return container;
	}
	
	
	
	public IFile[] getProcessesToCompile() {
		if(selectedExperiments == null) return new IFile[0];
		ArrayList<IResource> processes = new ArrayList<>();
		for (Object experimentObject : selectedExperiments) {
			IContainer experiment = (IContainer)experimentObject;
			IResource[] resources = ResourceProperties.getAllTypedResources(ResourceType.PROCESS, experiment, null);
			processes.addAll(Arrays.asList(resources));
		}
		return processes.toArray(new IFile[processes.size()]);
	}

}
