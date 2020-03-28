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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ObjectsController;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.annotations.DocometreAnnotationAccesExtension;
import fr.univamu.ism.docometre.annotations.DocometreSharedTextColors;
import fr.univamu.ism.docometre.annotations.ErrorAnnotation;
import fr.univamu.ism.docometre.annotations.WarningAnnotation;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dialogs.FindDialog;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class SourceEditor extends EditorPart implements IResourceChangeListener {

	// Colors IDs
	private static final String COLOR_BLACK = "COLOR_BLACK";
	private static final String COLOR_DARK_GREY = "COLOR_DARK_GREY";

	static {
		JFaceResources.getColorRegistry().put(COLOR_BLACK, new RGB(0, 0, 0));
		JFaceResources.getColorRegistry().put(COLOR_DARK_GREY, new RGB(127, 127, 127));
	}

	private DefaultEditDomain editDomain;
	private ProcessEditor processEditor;
	private ActionRegistry actionRegistry;
	protected Document document;
	protected AnnotationModel annotationModel;
	private DocometreAnnotationAccesExtension docometreAnnotationAccesExtension;
	protected SourceViewer sourceViewer;
	private PartListenerAdapter partListenerAdapter;

	public SourceEditor(CommandStack commandStack, ProcessEditor processEditor) {
			this.processEditor = processEditor;
			editDomain = new DefaultEditDomain(this);
			editDomain.setCommandStack(commandStack);
		}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta == null)
			return;
		try {
			delta.accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							IResource process = ObjectsController.getResourceForObject(getProcess());
							if (delta.getResource().equals(process)) {
								updateMarkers();
							}
						}
					});
					return true;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);

		actionRegistry = new ActionRegistry();

		IAction action;

		action = new UndoAction(this);
		actionRegistry.registerAction(action);
//			getStackActions().add(action.getId());

		action = new RedoAction(this);
		actionRegistry.registerAction(action);
//			getStackActions().add(action.getId());

	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	public SourceViewer getSourceViewer() {
		return sourceViewer;
	}

	@Override
	public void createPartControl(Composite parent) {
		document = new Document();
		Composite sourceContainer = new Composite(parent, SWT.BORDER);
		sourceContainer.setLayout(new FillLayout());
		// Composite holding lines numbers and annotation column
		CompositeRuler lineAnnotationRuler = new CompositeRuler();
		annotationModel = new AnnotationModel();
		annotationModel.connect(document);
		lineAnnotationRuler.setModel(annotationModel);
		// Lines numbers column
		LineNumberRulerColumn lineNumberRulerColumn = new LineNumberRulerColumn();
		lineNumberRulerColumn.setForeground(JFaceResources.getColorRegistry().get(COLOR_DARK_GREY));
		lineAnnotationRuler.addDecorator(0, lineNumberRulerColumn);
		// Annotation column
		docometreAnnotationAccesExtension = new DocometreAnnotationAccesExtension();
		AnnotationRulerColumn arc = new AnnotationRulerColumn(annotationModel, 16, docometreAnnotationAccesExtension);
		DefaultAnnotationHover defaultAnnotationHover = new DefaultAnnotationHover();
		arc.setHover(defaultAnnotationHover);
		arc.addAnnotationType(ErrorAnnotation.TYPE_ERROR);
		arc.addAnnotationType(WarningAnnotation.TYPE_WARNING);
		lineAnnotationRuler.addDecorator(0, arc);
		// Overview column
		OverviewRuler overviewRuler = new OverviewRuler(docometreAnnotationAccesExtension, 13,
				DocometreSharedTextColors.getInstance());
		overviewRuler.addAnnotationType(ErrorAnnotation.TYPE_ERROR);
		overviewRuler.addAnnotationType(WarningAnnotation.TYPE_WARNING);
		overviewRuler.addHeaderAnnotationType(ErrorAnnotation.TYPE_ERROR);
		overviewRuler.addHeaderAnnotationType(WarningAnnotation.TYPE_WARNING);
		overviewRuler.setAnnotationTypeColor(ErrorAnnotation.TYPE_ERROR,
				DocometreSharedTextColors.getInstance().getColor(new RGB(255, 0, 0)));
		overviewRuler.setAnnotationTypeLayer(ErrorAnnotation.TYPE_ERROR, IAnnotationAccessExtension.DEFAULT_LAYER);
		overviewRuler.setAnnotationTypeColor(WarningAnnotation.TYPE_WARNING,
				DocometreSharedTextColors.getInstance().getColor(new RGB(255, 255, 0)));
		overviewRuler.setAnnotationTypeLayer(WarningAnnotation.TYPE_WARNING, IAnnotationAccessExtension.DEFAULT_LAYER);

		sourceViewer = new SourceViewer(sourceContainer, lineAnnotationRuler, overviewRuler, true,
				SWT.V_SCROLL | SWT.H_SCROLL);
		sourceViewer.getTextWidget().setEditable(false);

		sourceViewer.setDocument(document, annotationModel, -1, -1);
		
//		FastPartitioner adbasicFastPartitioner = new FastPartitioner(new ADBasicRulesPartitionScanner(), ADBasicRulesPartitionScanner.PARTITIONS);
//		document.setDocumentPartitioner(adbasicFastPartitioner);
//		adbasicFastPartitioner.connect(document);
//		sourceViewer.configure(new ADBasicSourceViewerConfiguration());

		sourceViewer.getTextWidget().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent event) {
			}

			@Override
			public void keyPressed(KeyEvent event) {
				if (((event.stateMask & SWT.MOD1) == SWT.MOD1) && event.keyCode == 'f') {
					FindDialog.getInstance().setTextViewer(sourceViewer);
					FindDialog.getInstance().open();
				} else if(event.keyCode == SWT.F5) {
					try {
						Process process = getProcess();
						update(process.getCode(null));
					} catch (Exception e) {
						e.printStackTrace();
						Activator.logErrorMessageWithCause(e);
					}
				}
			}
		});
		sourceViewer.getTextWidget().addCaretListener(new CaretListener() {
			@Override
			public void caretMoved(CaretEvent event) {
				FindDialog.getInstance().resetOffset(sourceViewer, event.caretOffset);
			}
		});

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

		partListenerAdapter = new PartListenerAdapter() {
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == processEditor) {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.removePartListener(partListenerAdapter);
					ResourcesPlugin.getWorkspace().removeResourceChangeListener(SourceEditor.this);
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
				if (partRef.getPart(false) == processEditor)
					FindDialog.getInstance().setTextViewer(getSourceViewer());
			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
		try {
			update(getProcess().getCode(null));
		} catch (Exception e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}

		updateMarkers();

	}

	private Process getProcess() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput) getEditorInput();
		return (Process) resourceEditorInput.getObject();
	}

	@Override
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
	}

	protected void update(String code) {
//			System.out.println("update document...");
		sourceViewer.getDocument().set(code);
//			for (int i = 0; i < sourceViewer.getDocument().getLength(); i++) {
//				ITypedRegion typedRegion = sourceViewer.getDocument().getDocumentPartitioner().getPartition(i);			
//				System.out.println(i + " = " + typedRegion.getType());
//			}
	}

	protected void updateMarkers() {
		try {
			update(getProcess().getCode(null));
			IResource process = ObjectsController.getResourceForObject(getProcess());
			IMarker[] markers = process.findMarkers(DocometreBuilder.MARKER_ID, true, IResource.DEPTH_INFINITE);
			annotationModel.removeAllAnnotations();
			for (IMarker marker : markers) {
				Annotation annotation = null;
				String line = (String) marker.getAttribute(IMarker.MESSAGE);
				Object severity = marker.getAttribute(IMarker.SEVERITY);
				Object lineNumber = marker.getAttribute(IMarker.LINE_NUMBER);
				if (line != null && severity != null && lineNumber != null) {
					if (severity.equals(IMarker.SEVERITY_ERROR))
						annotation = new ErrorAnnotation(marker, line);
					if (severity.equals(IMarker.SEVERITY_WARNING))
						annotation = new WarningAnnotation(marker, line);
//						int lineNumber = (int) marker.getAttribute(IMarker.LINE_NUMBER);
					int lineOffset = document.getLineOffset((int) lineNumber - 1);
					String lengthLine = line.replaceAll("^(.|\\n)*line:\\s", "");
					lengthLine = lengthLine.replaceAll("\\sfile:(.*)$", "");
					annotationModel.addAnnotation(annotation, new Position(lineOffset, lengthLine.length()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == CommandStack.class)
			return (T) editDomain.getCommandStack();
		if (adapter == ActionRegistry.class)
			return (T) actionRegistry;
		return super.getAdapter(adapter);
	}
	
	@Override
	public void dispose() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput)getEditorInput();
		ObjectsController.removeHandle(resourceEditorInput.getObject());
		docometreAnnotationAccesExtension.dispose();
		super.dispose();
	}

}
