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
package fr.univamu.ism.docometre.analyse.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.map.HashedMap;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.analyse.datamodel.XYChart;
import fr.univamu.ism.docometre.analyse.editors.ChannelEditor;
import fr.univamu.ism.docometre.analyse.editors.XYChartEditor;
import fr.univamu.ism.docometre.analyse.editors.XYZChartEditor;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.preferences.MathEnginePreferencesConstants;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class LoadUnloadSubjectsHandler extends AbstractHandler implements ISelectionListener {
	
	private class SaveSubjectsDialog extends Dialog {

		private HashedMap<IResource, Boolean> mustSaveSubjects;

		protected SaveSubjectsDialog(Shell parentShell, HashedMap<IResource, Boolean> mustSaveSubjects) {
			super(parentShell);
			this.mustSaveSubjects = mustSaveSubjects;
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			Composite container = (Composite) super.createDialogArea(parent);
			Label label = new Label(container, SWT.NORMAL);
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			label.setText(DocometreMessages.SelectSubjectsToSave);
			CheckboxTableViewer subjectsListViewer = CheckboxTableViewer.newCheckList(container, SWT.NORMAL);
			subjectsListViewer.setContentProvider(new ArrayContentProvider());
			subjectsListViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if(element instanceof IResource) {
						return ((IResource)element).getFullPath().toPortableString();
					}
					return super.getText(element);
				}
			});
			subjectsListViewer.setInput(selectedSubjects.toArray());
			subjectsListViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			subjectsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					mustSaveSubjects.clear();
					Object[] selectionArray = subjectsListViewer.getCheckedElements();
					for (Object subject : selectionArray) {
						mustSaveSubjects.put((IResource) subject, true);
					}
				}
			});
			
			return container;
		}
		
		@Override
		protected boolean isResizable() {
			return true;
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(DocometreMessages.SelectSubjectsToSaveDialogTitle);
		}
		
		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			getButton(IDialogConstants.OK_ID).setText(DocometreMessages.Terminate);
		}
	}
	
	private List<IResource> selectedSubjects = new ArrayList<>();
	private boolean cancel;
	private static LoadUnloadSubjectsHandler loadUnloadSubjectsHandler;

	public static LoadUnloadSubjectsHandler getInstance() {
		return loadUnloadSubjectsHandler;
	}
	
	public LoadUnloadSubjectsHandler() {
		if(loadUnloadSubjectsHandler == null) {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(this);
			loadUnloadSubjectsHandler = this;
		}
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(!MathEngineFactory.getMathEngine().isStarted()) {
			Activator.logInfoMessage(DocometreMessages.PleaseStartMathEngineFirst, getClass());
			return null;
		}
		cancel = false;
		boolean cancelModified = false;
		HashedMap<IResource, Boolean> mustSaveSubjects = new HashedMap<>(); 
		for (IResource subject : selectedSubjects) {
			boolean loaded = MathEngineFactory.getMathEngine().isSubjectLoaded(subject);
			if(loaded) {
				boolean modified = ResourceProperties.isSubjectModified(subject);
				if(modified) mustSaveSubjects.put(subject, false);
			}
		}
		
		if(mustSaveSubjects.size() > 0) {
			SaveSubjectsDialog saveSubjectsDialog = new SaveSubjectsDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), mustSaveSubjects);
			int response = saveSubjectsDialog.open();
			if(response == Window.CANCEL) {
				mustSaveSubjects.clear();
				return true;
			}
		}
		
		for (IResource subject : selectedSubjects) {
			boolean loaded = MathEngineFactory.getMathEngine().isSubjectLoaded(subject);
			String loadUnloadName = subject.getFullPath().segment(0) + "." + subject.getFullPath().segment(1);
			if(loaded) {
				if(ResourceProperties.isSubjectModified(subject)) {
					if(mustSaveSubjects.get(subject) != null && mustSaveSubjects.get(subject)) {
						saveSubject(subject);
					}
				}
				closeEditors(subject);
				unloadSubject(subject, loadUnloadName);
			} else loadSubject(subject, loadUnloadName);
			
			
			if(cancel) break;
		}
		for (IResource subject : selectedSubjects) {
			try {
				if(subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
					ChannelsContainer channelsContainer = (ChannelsContainer)subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
					channelsContainer.setUpdateChannelsCache(true);
				}
			ExperimentsView.refresh(subject, null);
			SubjectsView.refresh(subject, null);
			} catch (CoreException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		SaveModifiedSubjectsHandler.refresh();
		return cancel || cancelModified;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		selectedSubjects.clear();
		if(!(selection instanceof StructuredSelection)) return;
		StructuredSelection structuredSelection = (StructuredSelection)selection;
		for (Object element : structuredSelection) {
			if(element instanceof IResource) {
				IResource resource = (IResource)element;
				if(ResourceType.isSubject(resource)) selectedSubjects.add(resource);
			}
		}
		setBaseEnabled(selectedSubjects.size() > 0);
	}
	
	public void resetSelection(List<IResource> selectedSubjects) {
		this.selectedSubjects = selectedSubjects;
	}
	
	private void saveSubject(IResource subject) {
		ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		try {
			progressMonitorDialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(DocometreMessages.SavingSubject + "\"" + subject.getFullPath().toString() + "\". " + DocometreMessages.PleaseWait, IProgressMonitor.UNKNOWN);
					Activator.logInfoMessage(DocometreMessages.SavingSubject + "\"" + subject.getFullPath().toString() + "\". ", LoadUnloadSubjectsHandler.this.getClass());
					MathEngineFactory.getMathEngine().saveSubject(subject);
					Activator.logInfoMessage(DocometreMessages.Done, LoadUnloadSubjectsHandler.this.getClass());
					cancel = monitor.isCanceled();
					monitor.done();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
			cancel = true;
		}
	}
	
	private void closeEditors(IResource subject) {
		IEditorReference[] editorsReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		for (IEditorReference editorReference : editorsReferences) {
			if(editorReference.getId().equals(ChannelEditor.ID)) {
				try {
					Object object = ((ResourceEditorInput)editorReference.getEditorInput()).getObject();
					Channel channel = (Channel)object;
					if(channel.getParent().equals(subject)) {
						editorReference.getEditor(false).getSite().getPage().closeEditor(editorReference.getEditor(false), true);
					}
				} catch (PartInitException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
			if(editorReference.getId().equals(XYChartEditor.ID) || editorReference.getId().equals(XYZChartEditor.ID)) {
				try {
					Object object = ((ResourceEditorInput)editorReference.getEditorInput()).getObject();
					XYChart xyChart = (XYChart)object;
					if(xyChart.contains(subject)) { 
						editorReference.getEditor(false).getSite().getPage().closeEditor(editorReference.getEditor(false), true);
					}
				} catch (PartInitException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
		}
	}
	
	private void unloadSubject(IResource subject, String loadUnloadName) {
		ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		try {
			progressMonitorDialog.run(true, selectedSubjects.size()>1, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask(DocometreMessages.UnloadingSubject + "\"" + loadUnloadName + "\". " + DocometreMessages.PleaseWait, IProgressMonitor.UNKNOWN);
						Activator.logInfoMessage(DocometreMessages.UnloadingSubject + "\"" + loadUnloadName + "\". ", LoadUnloadSubjectsHandler.this.getClass());
						MathEngineFactory.getMathEngine().unload(subject);
						Activator.logInfoMessage(DocometreMessages.Done, LoadUnloadSubjectsHandler.this.getClass());
						cancel = monitor.isCanceled();
						monitor.done();
						subject.setSessionProperty(ResourceProperties.SUBJECT_MODIFIED_QN, false);
						subject.setSessionProperty(ResourceProperties.CHANNELS_LIST_QN, null);
					} catch (CoreException e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	private void loadSubject(IResource subject, String loadUnloadName) {
		ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		try {
			progressMonitorDialog.run(true, selectedSubjects.size()>1, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(DocometreMessages.LoadingSubject+ "\"" + loadUnloadName + "\". " + DocometreMessages.PleaseWait, IProgressMonitor.UNKNOWN);
					Activator.logInfoMessage(DocometreMessages.LoadingSubject + "\"" + loadUnloadName + "\". ", LoadUnloadSubjectsHandler.this.getClass());
					boolean loadFromSavedFile = Activator.getDefault().getPreferenceStore().getBoolean(MathEnginePreferencesConstants.ALWAYS_LOAD_FROM_SAVED_DATA);
					MathEngineFactory.getMathEngine().load(subject, loadFromSavedFile);
					Activator.logInfoMessage(DocometreMessages.Done, LoadUnloadSubjectsHandler.this.getClass());
					cancel = monitor.isCanceled();
					monitor.done();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
}
