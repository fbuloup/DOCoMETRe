package fr.univamu.ism.docometre.editors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.dialogs.FindDialog;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class ParametersRawEditor extends EditorPart {
	
	private SourceViewer sourceViewer;
	private PartListenerAdapter partListenerAdapter;
	private ArrayList<Font> fontsArrayList = new ArrayList<>();
	private ParametersEditor parametersEditor;
	private StringBuffer content;
	
	public ParametersRawEditor(ParametersEditor parametersEditor) {
		this.parametersEditor = parametersEditor;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		try {
			
			readParamsFile();
			
			Document document = parametersEditor.getDocument();
			CompositeRuler lineAnnotationRuler = new CompositeRuler();
			LineNumberRulerColumn lineNumberRulerColumn = new LineNumberRulerColumn();
			if(Display.isSystemDarkTheme()) {
				lineNumberRulerColumn.setForeground(PlatformUI.createDisplay().getSystemColor(SWT.COLOR_WHITE));
				lineNumberRulerColumn.setBackground(PlatformUI.createDisplay().getSystemColor(SWT.COLOR_BLACK));
			} else {
				lineNumberRulerColumn.setForeground(PlatformUI.createDisplay().getSystemColor(SWT.COLOR_BLACK));
				lineNumberRulerColumn.setBackground(PlatformUI.createDisplay().getSystemColor(SWT.COLOR_WHITE));
			}
			lineAnnotationRuler.addDecorator(0, lineNumberRulerColumn);
			sourceViewer = new SourceViewer(parent, lineAnnotationRuler, null, true, SWT.V_SCROLL | SWT.H_SCROLL);
			sourceViewer.setDocument(document);
			document.set(content.toString());
			
			TextViewerUndoManager textViewerUndoManager = new TextViewerUndoManager(Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.PREF_UNDO_LIMIT));
			sourceViewer.setUndoManager(textViewerUndoManager);
			textViewerUndoManager.connect(sourceViewer);
//			textViewerUndoManager.reset();
			
			sourceViewer.addTextListener(new ITextListener() {
				@Override
				public void textChanged(TextEvent event) {
					parametersEditor.setDirty(true);
				}
			});
			
			sourceViewer.getTextWidget().addKeyListener(new KeyAdapter() {
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
					if(((event.stateMask & SWT.MOD1) == SWT.MOD1) && event.keyCode == '=') {
						 Font font = sourceViewer.getTextWidget().getFont();
						 Font newFont = new Font(font.getDevice(), font.getFontData()[0].getName(), font.getFontData()[0].getHeight() + 1, font.getFontData()[0].getStyle());
						 sourceViewer.getTextWidget().setFont(newFont);
						 fontsArrayList.add(newFont);
					 }
					 if(((event.stateMask & SWT.MOD1) == SWT.MOD1) && event.keyCode == '-') {
						 Font font = sourceViewer.getTextWidget().getFont();
						 Font newFont = new Font(font.getDevice(), font.getFontData()[0].getName(), font.getFontData()[0].getHeight() - 1, font.getFontData()[0].getStyle());
						 sourceViewer.getTextWidget().setFont(newFont);
						 fontsArrayList.add(newFont);
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
			
			partListenerAdapter = new PartListenerAdapter() {
				@Override
				public void partClosed(IWorkbenchPartReference partRef) {
					if(partRef.getPart(false) == parametersEditor) {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
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
					if(partRef.getPart(false) == parametersEditor) FindDialog.getInstance().setTextViewer(sourceViewer);
				}
			};
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
			
			sourceViewer.getTextWidget().setCaretOffset(document.getLength());
			
		} catch (IOException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}

	}
	
	@Override
	public void dispose() {
		for (Font font : fontsArrayList) font.dispose();
		super.dispose();
	}

	@Override
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
	}

	public void update() {
//		try {
//			readParamsFile();
//			parametersEditor.getDocument().set(content.toString());
//		} catch (IOException e) {
//			Activator.logErrorMessageWithCause(e);
//			e.printStackTrace();
//		}
		
	}
	
	private void readParamsFile() throws IOException {
		IFile paramsFile = (IFile) ((ResourceEditorInput)getEditorInput()).getObject();
		List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(paramsFile.getLocation().toOSString()), StandardCharsets.UTF_8);
		content = new StringBuffer();
		int lineNumber = 1;
		for (String line : lines) {
			content.append(line);
			if(lineNumber < lines.size()) {
				content.append("\n");
				lineNumber++;
			}
		}
	}

}
