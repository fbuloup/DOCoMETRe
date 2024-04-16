package fr.univamu.ism.docometre.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;

public class ParametersTableEditor extends EditorPart {
	
	private class ParameterEditingSupport extends EditingSupport {
		
		private TextCellEditor textCellEditor;
		private TableViewerColumn tableViewerColumn;

		public ParameterEditingSupport(TableViewer tableViewer, TableViewerColumn tableViewerColumn) {
			super(tableViewer);
			textCellEditor = new TextCellEditor(tableViewer.getTable());
			this.tableViewerColumn = tableViewerColumn;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return textCellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			int lineNumber = (int)element;
			int columnNumber = (int)tableViewerColumn.getColumn().getData();
			String parameterValue = "";
	    	try {
				int length = parametersEditor.getDocument().getLineLength(lineNumber);
				int offset = parametersEditor.getDocument().getLineOffset(lineNumber);
				String line = parametersEditor.getDocument().get(offset, length);
				String[] parameters = line.split(";");
				parameterValue = parameters[columnNumber-1].trim();
			} catch (BadLocationException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
	    	return parameterValue;
		}

		@Override
		protected void setValue(Object element, Object value) {
			int lineNumber = (int)element;
			int columnNumber = (int)tableViewerColumn.getColumn().getData();
			String parameterValue = (String) value;
			try {
				int length = parametersEditor.getDocument().getLineLength(lineNumber);
				int offset = parametersEditor.getDocument().getLineOffset(lineNumber);
				String line = parametersEditor.getDocument().get(offset, length);
				String[] parameters = line.split(";");
				parameters[columnNumber-1] = parameterValue;
				line = String.join(";", parameters);
				parametersEditor.getDocument().replace(offset, length, line);
				update();
			} catch (BadLocationException e) {
				Activator.logErrorMessageWithCause(e);
				e.printStackTrace();
			}
		}
		
	}
	
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
			parametersTableViewer.getTable().setHeaderVisible(true);
			parametersTableViewer.getTable().setLinesVisible(true);
			parametersTableViewer.setContentProvider(ArrayContentProvider.getInstance());
			int length = parametersEditor.getDocument().getLineLength(0);
			String firstLine = parametersEditor.getDocument().get(0, length);
			String[] parametersString = firstLine.split(";");
			for (int i = 0; i < parametersString.length; i++) {
				parametersString[i] = parametersString[i].trim();
			}
			TableViewerColumn tableViewerFirstColumn = new TableViewerColumn(parametersTableViewer, SWT.NONE);
			tableViewerFirstColumn.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return super.getText(element);
				}
			});
			tableViewerFirstColumn.getColumn().pack();
			for (int i = 0; i < parametersString.length; i++) {
				TableViewerColumn tableViewerColumn = new TableViewerColumn(parametersTableViewer, SWT.CENTER);
				tableViewerColumn.getColumn().setWidth(200);
				tableViewerColumn.getColumn().setData(i+1);
				tableViewerColumn.getColumn().setText(parametersString[i]);
				tableViewerColumn.setEditingSupport(new ParameterEditingSupport(parametersTableViewer, tableViewerColumn));
				tableViewerColumn.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						int lineNumber = (int)element;
						int columnNumber = (int)tableViewerColumn.getColumn().getData();
						String parameterValue = "";
				    	try {
							int length = parametersEditor.getDocument().getLineLength(lineNumber);
							int offset = parametersEditor.getDocument().getLineOffset(lineNumber);
							String line = parametersEditor.getDocument().get(offset, length);
							String[] parameters = line.split(";");
							parameterValue = parameters[columnNumber-1].trim();
						} catch (BadLocationException e) {
							Activator.logErrorMessageWithCause(e);
							e.printStackTrace();
						}
				    	return parameterValue;
					}
				});
				
			}
			update();
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
