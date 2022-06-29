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
package fr.univamu.ism.docometre.editors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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

public class ParametersEditor extends EditorPart implements PartNameRefresher {
	
	public static String ID = "Docometre.ParametersEditor";
	
	private SourceViewer sourceViewer;
	private boolean dirty;
	private Document document;
	private PartListenerAdapter partListenerAdapter;
	private ArrayList<Font> fontsArrayList = new ArrayList<>();
	
	public ParametersEditor() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			IResource paramsFile = ((IResource)((ResourceEditorInput)getEditorInput()).getObject());
			Files.write(Paths.get(paramsFile.getLocationURI()), document.get().getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
			setDirty(false);
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		IResource resource = ((IResource)((ResourceEditorInput)input).getObject());
		setPartName(GetResourceLabelDelegate.getLabel(resource));
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	private void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void createPartControl(Composite parent) {
		try {
			
			IFile paramsFile = (IFile) ((ResourceEditorInput)getEditorInput()).getObject();
			List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(paramsFile.getLocation().toOSString()), StandardCharsets.UTF_8);
			StringBuffer content = new StringBuffer();
			int lineNumber = 1;
			for (String line : lines) {
				content.append(line);
				if(lineNumber < lines.size()) {
					content.append("\n");
					lineNumber++;
				}
			}
			
			document = new Document();
			CompositeRuler lineAnnotationRuler = new CompositeRuler();
			lineAnnotationRuler.addDecorator(0, new LineNumberRulerColumn());
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
					setDirty(true);
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
					if(partRef.getPart(false) == ParametersEditor.this) {
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
					if(partRef.getPart(false) == ParametersEditor.this) FindDialog.getInstance().setTextViewer(sourceViewer);
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

	@Override
	public void refreshPartName() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		setPartName(GetResourceLabelDelegate.getLabel((IResource) object));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}

}
