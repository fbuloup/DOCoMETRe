package fr.univamu.ism.docometre.analyse.handlers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.analyse.editors.ChannelEditor;
import fr.univamu.ism.docometre.analyse.editors.Chart2D3DBehaviour;
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;

public final class UpdateWorkbenchDelegate {
	
	public static void update() {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					// Set subject as modified
					IResource[] modifiedSubjects = MathEngineFactory.getMathEngine().getCreatedOrModifiedSubjects();
					if(modifiedSubjects.length > 0) {
						for (IResource modifiedSubject : modifiedSubjects) {
							ResourceProperties.setSubjectModified(modifiedSubject, true);
							try {
								if(modifiedSubject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) != null && modifiedSubject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN) instanceof ChannelsContainer) {
									ChannelsContainer channelsContainer = (ChannelsContainer)modifiedSubject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
									channelsContainer.setUpdateChannelsCache(true);
								}
							} catch (CoreException e) {
								Activator.logErrorMessageWithCause(e);
								e.printStackTrace();
							} finally {
								ExperimentsView.refresh(modifiedSubject, null);
								SubjectsView.refresh(modifiedSubject, null);
							}
						}
						IEditorReference[] editorsRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
						for (IEditorReference editorRef : editorsRefs) {
							IEditorPart editor = editorRef.getEditor(false);
							if(editor instanceof ChannelEditor) {
								((ChannelEditor)editor).update();
							}
							if(editor instanceof Chart2D3DBehaviour) {
								((Chart2D3DBehaviour)editor).redraw();
							}
						}
					}
				}
			});
	}
	
	public static void updateCharts() {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				IEditorReference[] editorsRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
				for (IEditorReference editorRef : editorsRefs) {
					IEditorPart editor = editorRef.getEditor(false);
					if(editor instanceof ChannelEditor) {
						((ChannelEditor)editor).update();
					}
					if(editor instanceof Chart2D3DBehaviour) {
						((Chart2D3DBehaviour)editor).redraw();
					}
				}
			}
		});
	}

}
