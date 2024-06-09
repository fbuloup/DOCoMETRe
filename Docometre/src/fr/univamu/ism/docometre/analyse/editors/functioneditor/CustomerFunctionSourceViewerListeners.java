package fr.univamu.ism.docometre.analyse.editors.functioneditor;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.VerifyEvent;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dialogs.FindDialog;

public final class CustomerFunctionSourceViewerListeners {
	
	public static void addSourceViewerListeners(SourceViewer functionSourceViewer, CustomerFunctionEditor editor) {
		functionSourceViewer.addTextListener(new ITextListener() {
			@Override
			public void textChanged(TextEvent event) {
				editor.setDirty(true);
			}
		});
		functionSourceViewer.getTextWidget().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if(((event.stateMask & SWT.MOD1) == SWT.MOD1) && event.keyCode == 'f') {
					FindDialog.getInstance().setTextViewer(functionSourceViewer);
					FindDialog.getInstance().open();
				} else if(isRedoKeyPress(event)) {
					functionSourceViewer.doOperation(ITextOperationTarget.REDO);
				} else if(isUndoKeyPress(event)) {
					functionSourceViewer.doOperation(ITextOperationTarget.UNDO);
				} else if(((event.stateMask & SWT.MOD1) == SWT.MOD1) && event.keyCode == 'd') {
					try {
						IRegion region = functionSourceViewer.getDocument().getLineInformationOfOffset(functionSourceViewer.getSelectedRange().x);
						if(region.getOffset() > 0) functionSourceViewer.getDocument().replace(region.getOffset() - 1, region.getLength() + 1, "");
						else functionSourceViewer.getDocument().replace(region.getOffset(), region.getLength() + 1, "");
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
		functionSourceViewer.getTextWidget().addCaretListener(new CaretListener() {
			@Override
			public void caretMoved(CaretEvent event) {
				FindDialog.getInstance().resetOffset(functionSourceViewer, event.caretOffset);
			}
		});
		
		functionSourceViewer.appendVerifyKeyListener(new VerifyKeyListener() {
		      public void verifyKey(VerifyEvent event) {
		      if (event.stateMask == SWT.CTRL && event.keyCode == ' ') {
		        if (functionSourceViewer.canDoOperation(SourceViewer.CONTENTASSIST_PROPOSALS))
		        	functionSourceViewer.doOperation(SourceViewer.CONTENTASSIST_PROPOSALS);
		        event.doit = false;
		      }
		   }
		});
	}

}
