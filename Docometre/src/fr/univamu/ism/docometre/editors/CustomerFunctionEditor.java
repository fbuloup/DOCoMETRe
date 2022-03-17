package fr.univamu.ism.docometre.editors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.dialogs.FindDialog;

public class CustomerFunctionEditor extends EditorPart {
	
	public static String ID = "Docometre.CustomerFunctionEditor";
	
	private static final String COLOR_DARK_GREY = "COLOR_DARK_GREY";
	
	private IFile customerFunction;
	private boolean dirty;
	private SourceViewer sourceViewer;

	private Document document;


	public CustomerFunctionEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			Files.write(Paths.get(customerFunction.getLocationURI()), document.get().getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
			setDirty(false);
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public void doSaveAs() {
		// Not allowed
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		customerFunction = (IFile) ((ResourceEditorInput)input).getObject();
		setPartName(GetResourceLabelDelegate.getLabel(customerFunction));
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}
	
	private void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		document = new Document();
		
		// Composite holding lines numbers and annotation column 
		CompositeRuler lineAnnotationRuler = new CompositeRuler();
		AnnotationModel annotationModel = new AnnotationModel();
		annotationModel.connect(document);
		lineAnnotationRuler.setModel(annotationModel);

		// Lines numbers column
		LineNumberRulerColumn lineNumberRulerColumn = new LineNumberRulerColumn();
		lineNumberRulerColumn.setForeground(JFaceResources.getColorRegistry().get(COLOR_DARK_GREY));
		lineAnnotationRuler.addDecorator(0,lineNumberRulerColumn);
		
		sourceViewer = new SourceViewer(parent, lineAnnotationRuler, null, false, SWT.V_SCROLL | SWT.H_SCROLL);
		sourceViewer.getTextWidget().setEditable(true);
		sourceViewer.setDocument(document, annotationModel, -1, -1);
		sourceViewer.configure(new SourceViewerConfiguration());
		sourceViewer.getTextWidget().addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent event) {
			}
			
			@Override
			public void keyPressed(KeyEvent event) {
				if(((event.stateMask & SWT.MOD1) == SWT.MOD1) && event.keyCode == 'f') {
					FindDialog.getInstance().setTextViewer(sourceViewer);
					FindDialog.getInstance().open();
				} else if(isRedoKeyPress(event)) {
					sourceViewer.doOperation(ITextOperationTarget.REDO);
				} else if(isUndoKeyPress(event)) {
					sourceViewer.doOperation(ITextOperationTarget.UNDO);
				}
			}
			
			private boolean isRedoKeyPress(KeyEvent e) {
				boolean redo = false;
				if(Platform.getOS().equals(Platform.OS_MACOSX)) {
					boolean stateMask = ((e.stateMask & (SWT.MOD1 | SWT.SHIFT)) == (SWT.MOD1 | SWT.SHIFT));
					redo = stateMask && ((e.keyCode == 'z') || (e.keyCode == 'Z'));
				} else redo = ((e.stateMask & SWT.MOD1) == SWT.MOD1) && ((e.keyCode == 'y') || (e.keyCode == 'Y'));
				return redo;
			}

			private boolean isUndoKeyPress(KeyEvent e) {
				boolean stateMask = (e.stateMask & SWT.MOD1) == SWT.MOD1;
				boolean undo = stateMask && ((e.keyCode == 'z') || (e.keyCode == 'Z'));
				return undo;
			}
		});
		sourceViewer.getTextWidget().addCaretListener(new CaretListener() {
			@Override
			public void caretMoved(CaretEvent event) {
				FindDialog.getInstance().resetOffset(sourceViewer, event.caretOffset);
			}
		});
		sourceViewer.addTextListener(new ITextListener() {
			@Override
			public void textChanged(TextEvent event) {
				setDirty(true);
			}
		});
		
		TextViewerUndoManager textViewerUndoManager = new TextViewerUndoManager(10);
		textViewerUndoManager.connect(sourceViewer);
		
		try {
			List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(customerFunction.getLocation().toOSString()), StandardCharsets.UTF_8);
			StringBuffer content = new StringBuffer();
			for (String line : lines) {
				content.append(line);
				if(!line.equals(lines.get(lines.size() - 1))) content.append("\n");
			}
			document.set(content.toString());
			setDirty(false);
			textViewerUndoManager.reset();
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
	}

	@Override
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
	}

}
