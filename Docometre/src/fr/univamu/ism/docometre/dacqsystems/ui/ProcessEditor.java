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
package fr.univamu.ism.docometre.dacqsystems.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackEventListener;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dialogs.FindDialog;
import fr.univamu.ism.docometre.editors.AbstractScriptSegmentEditor;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.editors.SchematicSourceLinker;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegment;
import fr.univamu.ism.process.ScriptSegmentCodeMap;
import fr.univamu.ism.process.ScriptSegmentType;

public class ProcessEditor extends MultiPageEditorPart implements IPageChangedListener, PartNameRefresher, SchematicSourceLinker/*, IResourceChangeListener*/ {
	
	protected ArrayList<AbstractScriptSegmentEditor> segmentEditors;
	private PartListenerAdapter partListenerAdapter;
	protected CommandStack commandStack;
	protected SourceEditor sourceEditor;
	// Source code page text, without the number of errors and warnings outside blocks
	private String sourceEditorPageText;
	// Compilation markers are linked to blocks again each time the diagram is modified (or undo/redo)
	private CommandStackEventListener commandStackEventListener = event -> {
		if(event.isPostChangeEvent()) updateBlocksCompilationMarkers();
	};
	
	private Process getProcess() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput)getEditorInput();
		return (Process) resourceEditorInput.getObject();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		
		if(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_ICON) == null) {
			JFaceResources.getImageRegistry().put(IImageKeys.PROCESS_ICON, Activator.getImage(IImageKeys.PROCESS_ICON));
		}
		
		if(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_ERROR_DECORATOR) == null) {
			JFaceResources.getImageRegistry().put(IImageKeys.PROCESS_ERROR_DECORATOR, new DecorationOverlayIcon(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_ICON), Activator.getImageDescriptor(IImageKeys.ERROR_ICON), IDecoration.BOTTOM_LEFT));
		}
		
		if(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_WARNING_DECORATOR) == null) {
			JFaceResources.getImageRegistry().put(IImageKeys.PROCESS_WARNING_DECORATOR, new DecorationOverlayIcon(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_ICON), Activator.getImageDescriptor(IImageKeys.WARNING_ICON), IDecoration.BOTTOM_LEFT));
		}
		
		IResource resource = ObjectsController.getResourceForObject(getProcess());
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		
		addPageChangedListener(this);
		
		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == ProcessEditor.this) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
					ProcessEditor.this.removePageChangedListener(ProcessEditor.this);
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
				IWorkbenchPart part = partRef.getPart(false);
				if(part == ProcessEditor.this) {
					if(getSelectedPage() instanceof AbstractScriptSegmentEditor)
						((AbstractScriptSegmentEditor)getSelectedPage()).updatePasteAction();
				}
				
			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
	}

	@Override
	protected void createPages() {
		commandStack = new CommandStack();
		commandStack.addCommandStackEventListener(commandStackEventListener);
		segmentEditors = new ArrayList<AbstractScriptSegmentEditor>(0);
		updateTitleImage();
	}
	
	public void refreshPartName() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		IResource resource = ObjectsController.getResourceForObject(object);
		setPartName(GetResourceLabelDelegate.getLabel(resource));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Object object = getProcess();
		IResource processFile = ObjectsController.getResourceForObject(object);
		ObjectsController.serialize((IFile) processFile, object);
		for (AbstractScriptSegmentEditor abstractScriptSegmentEditor : segmentEditors) {
			abstractScriptSegmentEditor.doSave(monitor);
		}
	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void activateSegmentProcessEditor(ScriptSegment scriptSegment) {
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.INITIALIZE)) if(getActivePage() != 0) setActivePage(0);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.LOOP)) if(getActivePage() != 1) setActivePage(1);
		if(scriptSegment.getScriptSegmentType().equals(ScriptSegmentType.FINALIZE)) if(getActivePage() != 2) setActivePage(2);
	}

	@Override
	public boolean showInSourceCode(Object modelElement) {
		if(sourceEditor == null) return false;
		// Regenerate source code and find corresponding lines before switching to source code page
		int[] lines = sourceEditor.getSourceLines(modelElement);
		if(lines == null) return false;
		if(getSelectedPage() != sourceEditor) setActiveEditor(sourceEditor);
		// Source code page activation may have regenerated code : select lines afterwards
		sourceEditor.selectLines(lines[0], lines[1]);
		return true;
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		for (AbstractScriptSegmentEditor abstractScriptSegmentEditor : segmentEditors) {
			if(event.getSelectedPage() == abstractScriptSegmentEditor) abstractScriptSegmentEditor.updatePasteAction();
		}
		if(event.getSelectedPage() == sourceEditor) {
			FindDialog.getInstance().setTextViewer(sourceEditor.getSourceViewer());
		}
		if(event.getSelectedPage() == sourceEditor)
			sourceEditor.updateMarkers();
		// Code may have changed (e.g. DACQ configuration modified)
		updateBlocksCompilationMarkers();
	}

	@Override
	public void dispose() {
		if(commandStack != null) commandStack.removeCommandStackEventListener(commandStackEventListener);
		super.dispose();
		ObjectsController.removeHandle(getProcess().getDACQConfiguration());
		ObjectsController.removeHandle(getProcess());
		getProcess().resetDACQConfiguration();
	}

	public void updateTitleImage() {
		try {
			IResource process = ObjectsController.getResourceForObject(getProcess());
			int severity = process.findMaxProblemSeverity(DocometreBuilder.MARKER_ID, true, IResource.DEPTH_INFINITE);
			if(severity == IMarker.SEVERITY_ERROR) setTitleImage(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_ERROR_DECORATOR));
			if(severity == IMarker.SEVERITY_WARNING) setTitleImage(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_WARNING_DECORATOR));
			if(severity == -1) setTitleImage(JFaceResources.getImageRegistry().get(IImageKeys.PROCESS_ICON));
			if(sourceEditor != null) {
				sourceEditor.updateMarkers();
			}
			updateBlocksCompilationMarkers();
		} catch (Exception e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		
	}
	
	/*
	 * Max severity : IMarker.SEVERITY_ERROR > IMarker.SEVERITY_WARNING > -1 (none)
	 */
	private static int maxSeverity(int severity1, int severity2) {
		return Math.max(severity1, severity2);
	}
	
	private static Image getSeverityImage(int severity) {
		if(severity == IMarker.SEVERITY_ERROR) return Activator.getImage(IImageKeys.ERROR_ANNOTATION_ICON);
		if(severity == IMarker.SEVERITY_WARNING) return Activator.getImage(IImageKeys.WARNING_ANNOTATION_ICON);
		return null;
	}
	
	private int getPageIndex(IEditorPart editor) {
		for (int i = 0; i < getPageCount(); i++) if(getEditor(i) == editor) return i;
		return -1;
	}
	
	/**
	 * Links compilation errors and warnings to the blocks generating the lines where they are located :
	 * these blocks display an error or warning icon, with messages in their tooltip. The tab of each segment
	 * page displays the max severity of its blocks. Errors and warnings located outside blocks (variables 
	 * declarations, modules code...) are counted in the tab of the source code page.
	 * Markers lines refer to the compiled code : they are linked to blocks only if the source code is
	 * unchanged since last compilation.
	 */
	public void updateBlocksCompilationMarkers() {
		if(sourceEditor == null || segmentEditors == null || segmentEditors.isEmpty()) return;
		try {
			Script script = getProcess().getScript();
			ScriptSegment[] scriptSegments = new ScriptSegment[] {script.getInitializeBlocksContainer(), script.getLoopBlocksContainer(), script.getFinalizeBlocksContainer()};
			for (ScriptSegment scriptSegment : scriptSegments) 
				for (Block block : scriptSegment.getBlocks()) block.setCompilationStatus(null);
			
			Map<Block, Integer> blocksSeverities = new LinkedHashMap<>();
			Map<Block, String> blocksMessages = new HashMap<>();
			Map<ScriptSegmentType, Integer> segmentsSeverities = new HashMap<>();
			int outsideBlocksMarkersNumber = 0;
			int outsideBlocksSeverity = -1;
			
			IResource resource = ObjectsController.getResourceForObject(getProcess());
			IMarker[] markers = resource == null || !resource.exists() ? new IMarker[0] : resource.findMarkers(DocometreBuilder.MARKER_ID, true, IResource.DEPTH_INFINITE);
			List<IMarker> compilationMarkers = new ArrayList<>();
			for (IMarker marker : markers) {
				// Code generation statuses are already displayed by their blocks
				if(marker.getAttribute(DocometreBuilder.CODE_GENERATION_MARKER_ATTRIBUTE, false)) continue;
				int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
				if(severity == IMarker.SEVERITY_ERROR || severity == IMarker.SEVERITY_WARNING) compilationMarkers.add(marker);
			}
			
			if(!compilationMarkers.isEmpty()) {
				// Regenerate source code and locate segments in it
				Map<ScriptSegmentCodeMap, Integer> segments = sourceEditor.locateSegments();
				int linesShift = ResourceProperties.getCompiledCodeLinesShift(resource, sourceEditor.getSourceCode());
				// Nothing is linked if source code has been modified since last compilation
				if(linesShift >= 0) {
					for (IMarker marker : compilationMarkers) {
						int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
						int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, -1);
						Block block = null;
						ScriptSegmentType scriptSegmentType = null;
						if(lineNumber >= 1) {
							int sourceLine = lineNumber - 1 + linesShift;
							for (Map.Entry<ScriptSegmentCodeMap, Integer> entry : segments.entrySet()) {
								int segmentLine = sourceLine - entry.getValue();
								if(segmentLine >= 0 && segmentLine < entry.getKey().getLinesNumber()) {
									block = entry.getKey().getBlockAtLine(segmentLine);
									scriptSegmentType = entry.getKey().getSegmentType();
									break;
								}
							}
						}
						if(block != null) {
							String message = marker.getAttribute(IMarker.MESSAGE, "");
							blocksSeverities.put(block, maxSeverity(blocksSeverities.getOrDefault(block, -1), severity));
							blocksMessages.merge(block, message, (message1, message2) -> message1 + "\n" + message2);
							segmentsSeverities.put(scriptSegmentType, maxSeverity(segmentsSeverities.getOrDefault(scriptSegmentType, -1), severity));
						} else {
							outsideBlocksMarkersNumber++;
							outsideBlocksSeverity = maxSeverity(outsideBlocksSeverity, severity);
						}
					}
				}
			}
			
			for (Map.Entry<Block, Integer> entry : blocksSeverities.entrySet()) {
				int severity = entry.getValue() == IMarker.SEVERITY_ERROR ? IStatus.ERROR : IStatus.WARNING;
				entry.getKey().setCompilationStatus(new Status(severity, Activator.PLUGIN_ID, blocksMessages.get(entry.getKey())));
			}
			
			// Segments pages : max severity of their blocks
			for (AbstractScriptSegmentEditor segmentEditor : segmentEditors) {
				int pageIndex = getPageIndex(segmentEditor);
				if(pageIndex >= 0) setPageImage(pageIndex, getSeverityImage(segmentsSeverities.getOrDefault(segmentEditor.getScriptSegmentType(), -1)));
				segmentEditor.refreshBlocksFigures();
			}
			
			// Source code page : errors and warnings outside blocks
			int pageIndex = getPageIndex(sourceEditor);
			if(pageIndex >= 0) {
				if(sourceEditorPageText == null) sourceEditorPageText = getPageText(pageIndex);
				String pageText = sourceEditorPageText;
				if(outsideBlocksMarkersNumber > 0) pageText = NLS.bind(DocometreMessages.OutsideBlocksMarkers, sourceEditorPageText, outsideBlocksMarkersNumber);
				setPageText(pageIndex, pageText);
				setPageImage(pageIndex, getSeverityImage(outsideBlocksSeverity));
			}
		} catch (Exception e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}
}
