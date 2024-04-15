package fr.univamu.ism.docometre.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;

public class ParametersTableEditor extends EditorPart {
	
	private ParametersEditor parametersEditor;
	private TableViewer parametersTableViewer;

	public ParametersTableEditor(ParametersEditor parametersEditor) {
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
			parametersTableViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
			parametersTableViewer.setContentProvider(ArrayContentProvider.getInstance());
			parametersTableViewer.setLabelProvider(new ITableLabelProvider() {
				@Override
				public void removeListener(ILabelProviderListener listener) {
				}
				@Override
				public boolean isLabelProperty(Object element, String property) {
					return false;
				}
				@Override
				public void dispose() {
				}
				@Override
				public void addListener(ILabelProviderListener listener) {
				}
				@Override
				public String getColumnText(Object element, int columnIndex) {
					if(!(element instanceof Integer)) return "";
			    	int lineNumber = (int)element;
			    	String parameterValue = "";
			    	try {
						int length = parametersEditor.getDocument().getLineLength(lineNumber);
						int offset = parametersEditor.getDocument().getLineOffset(lineNumber);
						String line = parametersEditor.getDocument().get(offset, length);
						String[] parameters = line.split(";");
						parameterValue = parameters[columnIndex].trim();
					} catch (BadLocationException e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
					}
			    	return parameterValue;
				}
				@Override
				public Image getColumnImage(Object element, int columnIndex) {
					return null;
				}
			});
			int length = parametersEditor.getDocument().getLineLength(0);
			String firstLine = parametersEditor.getDocument().get(0, length);
			String[] parametersString = firstLine.split(";");
			for (int i = 0; i < parametersString.length; i++) {
				parametersString[i] = parametersString[i].trim();
			}
			for (int i = 0; i < parametersString.length; i++) {
				TableColumn tableColumn = new TableColumn(parametersTableViewer.getTable(), SWT.NONE);
				tableColumn.setWidth(200);
				tableColumn.setText(parametersString[i]);
			}
			
			Integer[] linesNumber = new Integer[parametersEditor.getDocument().getNumberOfLines() - 1];
			for (int i = 0; i < linesNumber.length; i++) {
				linesNumber[i] = i+1;
			}
			parametersTableViewer.setInput(linesNumber);
			parametersTableViewer.getTable().setHeaderVisible(true);
			parametersTableViewer.getTable().setLinesVisible(true);
			parametersTableViewer.refresh();
		} catch (BadLocationException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
		

	}

	@Override
	public void setFocus() {
		parametersTableViewer.getTable().setFocus();
	}

	public void update() {
		Integer[] linesNumber = new Integer[parametersEditor.getDocument().getNumberOfLines() - 1];
		for (int i = 0; i < linesNumber.length; i++) {
			linesNumber[i] = i+1;
		}
		parametersTableViewer.setInput(linesNumber);
		
	}

}
