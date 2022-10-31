package fr.univamu.ism.docometre.analyse.editors.functioneditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
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
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dialogs.FindDialog;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;

public class CustomerFunctionEditor extends EditorPart implements PartNameRefresher {
	
	public static String ID = "Docometre.CustomerFunctionEditor";
	
	private boolean dirty;
	private SourceViewer sourceViewer;
	private Document document;
	private PartListenerAdapter partListenerAdapter;
	private ArrayList<Font> fontsArrayList = new ArrayList<>();

	public CustomerFunctionEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			Object object = ((ResourceEditorInput)getEditorInput()).getObject();
			Path functionPath = null;
			if(object instanceof IFile) {
				IFile customerFunction = (IFile) ((ResourceEditorInput)getEditorInput()).getObject();
				functionPath = Path.of(customerFunction.getLocation().toOSString());
			} else functionPath = (Path)object;
			
			
			StringBuffer stringBuffer = new StringBuffer();
			int nbLines = document.getNumberOfLines();
			for (int i = 0; i < nbLines; i++) {
				try {
					IRegion lineInfo = document.getLineInformation(i);
					String line = document.get(lineInfo.getOffset(), lineInfo.getLength());
					if(line.endsWith("\\")) {
						line = line.replaceAll("\\\\", "\\\\n\\\\");
					}
					stringBuffer.append(line.replaceAll("\t", "\\\\t"));
					stringBuffer.append("\n");
				} catch (BadLocationException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
			String text = stringBuffer.toString();
			Files.write(functionPath, text.getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
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
		setPartName(((ResourceEditorInput)input).getName());
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty(boolean dirty) {
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
		CompositeRuler lineAnnotationRuler = new CompositeRuler();
		LineNumberRulerColumn lineNumberRulerColumn = new LineNumberRulerColumn();
		lineAnnotationRuler.addDecorator(0, lineNumberRulerColumn);
		sourceViewer = new SourceViewer(parent, lineAnnotationRuler, null, false, SWT.V_SCROLL | SWT.H_SCROLL);
		sourceViewer.setDocument(document);
		sourceViewer.setEditable(false);
		
		Object customerFunction = ((ResourceEditorInput)getEditorInput()).getObject();
		String content = "";
		if(customerFunction instanceof IResource) {
			content = CustomerFunctionReader.read( ((IResource)customerFunction).getLocation().toOSString());
			sourceViewer.setEditable(FunctionFactory.isCustomerFunction(Path.of(((IResource)customerFunction).getLocation().toPortableString())));
		}
		else {
			content = CustomerFunctionReader.read(((Path)customerFunction).toFile().getAbsolutePath());
			sourceViewer.setEditable(FunctionFactory.isCustomerFunction((Path)customerFunction));
		}
		
		boolean devMode = Boolean.valueOf(System.getProperty("DEV"));
		if(devMode) sourceViewer.setEditable(true);
		
		document.set(content);
		sourceViewer.configure(new CustomerFunctionSourceViewerConfiguration());
		
		CustomerFunctionSourceViewerListeners.addSourceViewerListeners(sourceViewer, this);
		// Must add this listener in CustomerFunctionEditor because new fonts must be deleted from CustomerFunctionEditor
		sourceViewer.getTextWidget().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				 if(((event.stateMask & SWT.CTRL) == SWT.CTRL) && (event.keyCode == '=' || event.keyCode == 16777259)) {
					 Font font = sourceViewer.getTextWidget().getFont();
					 Font newFont = new Font(font.getDevice(), font.getFontData()[0].getName(), font.getFontData()[0].getHeight() + 1, font.getFontData()[0].getStyle());
					 sourceViewer.getTextWidget().setFont(newFont);
					 lineNumberRulerColumn.setFont(newFont);
					 sourceViewer.getTextWidget().getParent().layout(true);
					 fontsArrayList.add(newFont);
				 }
				 if(((event.stateMask & SWT.CTRL) == SWT.CTRL) && (event.keyCode == '-' || event.keyCode == 16777261)) {
					 Font font = sourceViewer.getTextWidget().getFont();
					 Font newFont = new Font(font.getDevice(), font.getFontData()[0].getName(), font.getFontData()[0].getHeight() - 1, font.getFontData()[0].getStyle());
					 sourceViewer.getTextWidget().setFont(newFont);
					 lineNumberRulerColumn.setFont(newFont);
					 sourceViewer.getTextWidget().getParent().layout(true);
					 fontsArrayList.add(newFont);
				 }
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
		
		Object editedObject = ((ResourceEditorInput)getEditorInput()).getObject();
		if(editedObject instanceof IFile) {
			IFile editedFile = (IFile)editedObject;
			if(!ResourceType.isCustomerFunction(editedFile)) setTitleImage(Activator.getImage(IImageKeys.FUNCTION_ICON));
		}
	}

	@Override
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
	}
	
	@Override
	public void dispose() {
		for (Font font : fontsArrayList) font.dispose();
		super.dispose();
	}
	
	@Override
	public void refreshPartName() {
		setPartName(getEditorInput().getName());
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}
 	
}
