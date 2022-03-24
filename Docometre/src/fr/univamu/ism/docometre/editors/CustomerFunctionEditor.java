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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.dialogs.FindDialog;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class CustomerFunctionEditor extends EditorPart implements PartNameRefresher {
	
	public static String ID = "Docometre.CustomerFunctionEditor";
	
	private IFile customerFunction;
	private boolean dirty;
	private SourceViewer sourceViewer;

	private Document document;

	private PartListenerAdapter partListenerAdapter;


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
		try {
			
			List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(customerFunction.getLocation().toOSString()), StandardCharsets.UTF_8);
			StringBuffer content = new StringBuffer();
			for (String line : lines) {
				content.append(line);
				if(!line.equals(lines.get(lines.size() - 1))) content.append("\n");
			}
			
			document = new Document();
			CompositeRuler lineAnnotationRuler = new CompositeRuler();
			lineAnnotationRuler.addDecorator(0,new LineNumberRulerColumn());
			sourceViewer = new SourceViewer(parent, lineAnnotationRuler, null, false, SWT.V_SCROLL | SWT.H_SCROLL);
			sourceViewer.setDocument(document);
			document.set(content.toString());
			sourceViewer.configure(new MySourceViewerConfiguration());
			
			
//			KeyStroke keyStroke = KeyStroke.getInstance("Ctrl+Space");
			
			
//			sourceViewer.setUndoManager(textViewerUndoManager);
			
//			sourceViewer.configure(new SourceViewerConfiguration());

			sourceViewer.addTextListener(new ITextListener() {
				@Override
				public void textChanged(TextEvent event) {
					setDirty(true);
				}
			});
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
					} else if(((event.stateMask & SWT.MOD1) == SWT.MOD1) && event.keyCode == 'd') {
						try {
							IRegion region = document.getLineInformationOfOffset(sourceViewer.getSelectedRange().x);
							if(region.getOffset() > 0) document.replace(region.getOffset() - 1, region.getLength() + 1, "");
							else document.replace(region.getOffset(), region.getLength() + 1, "");
						} catch (BadLocationException e) {
							e.printStackTrace();
							Activator.logErrorMessageWithCause(e);
						}
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
					if(partRef.getPart(false) == CustomerFunctionEditor.this) {
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
					if(partRef.getPart(false) == CustomerFunctionEditor.this) FindDialog.getInstance().setTextViewer(sourceViewer);
				}
			};
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
			
			sourceViewer.getTextWidget().setCaretOffset(document.getLength());
			sourceViewer.appendVerifyKeyListener(new VerifyKeyListener() {
					      public void verifyKey(VerifyEvent event) {

					      // Check for Ctrl+Spacebar
					      if (event.stateMask == SWT.CTRL && event.keyCode == ' ') {

					        // Check if source viewer is able to perform operation
					        if (sourceViewer.canDoOperation(SourceViewer.CONTENTASSIST_PROPOSALS))

					          // Perform operation
					          sourceViewer.doOperation(SourceViewer.CONTENTASSIST_PROPOSALS);

					        // Veto this key press to avoid further processing
					        event.doit = false;
					      }
					   }
					});
			
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
	}

	@Override
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
	}
	
	@Override
	public void refreshPartName() {
		setPartName(GetResourceLabelDelegate.getLabel(customerFunction));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}
	
	private class MySourceViewerConfiguration extends SourceViewerConfiguration {
		
		@Override
		public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
			ContentAssistant contentAssistant = new ContentAssistant();
//			contentAssistant.enableAutoActivation(true);
//			contentAssistant.enableAutoInsert(true);
//			contentAssistant.enableCompletionProposalTriggerChars(false);
			contentAssistant.addContentAssistProcessor(new CustomerFunctionCompletionProcessor(), IDocument.DEFAULT_CONTENT_TYPE);
			contentAssistant.install(sourceViewer);
			return contentAssistant;
		}
		@Override
		public IUndoManager getUndoManager(ISourceViewer sourceViewer) {
			TextViewerUndoManager textViewerUndoManager = new TextViewerUndoManager(Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.PREF_UNDO_LIMIT));
			return textViewerUndoManager;
		}
		
	}
 	
}
