package fr.univamu.ism.docometre.analyse.editors.functioneditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dialogs.FindDialog;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;

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
			if(customerFunction.getLocationURI() == null) Files.write(Paths.get(customerFunction.getFullPath().toPortableString()), text.getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
			else Files.write(Paths.get(customerFunction.getLocationURI()), text.getBytes(), StandardOpenOption.TRUNCATE_EXISTING , StandardOpenOption.WRITE);
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
		lineAnnotationRuler.addDecorator(0,new LineNumberRulerColumn());
		sourceViewer = new SourceViewer(parent, lineAnnotationRuler, null, false, SWT.V_SCROLL | SWT.H_SCROLL);
		sourceViewer.setDocument(document);
		sourceViewer.setEditable(false);
		
		String content = "";
		if(customerFunction.getLocation() != null) {
			content = CustomerFunctionReader.read(customerFunction.getLocation().toOSString());
			sourceViewer.setEditable(FunctionFactory.isCustomerFunction(Path.of(customerFunction.getLocation().toPortableString())));
		}
		else {
			content = CustomerFunctionReader.read(customerFunction.getFullPath().toPortableString());
			sourceViewer.setEditable(FunctionFactory.isCustomerFunction(Path.of(customerFunction.getFullPath().toPortableString())));
		}
		
		boolean devMode = Boolean.valueOf(System.getProperty("DEV"));
		if(devMode) sourceViewer.setEditable(true);
		
		document.set(content);
		sourceViewer.configure(new CustomerFunctionSourceViewerConfiguration());
		
		CustomerFunctionSourceViewerListeners.addSourceViewerListeners(sourceViewer, this);
		
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
	public void refreshPartName() {
		setPartName(GetResourceLabelDelegate.getLabel(customerFunction));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}
 	
}
