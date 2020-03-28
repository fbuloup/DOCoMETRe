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
package fr.univamu.ism.docometre.dacqsystems.adwin.ui.dialogs;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinVariable;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinVariableProperties;

public class ParametersDialog extends TitleAreaDialog {
	
	private class ParameterValueEditingSupport extends EditingSupport {
		
		private class Validator implements ICellEditorValidator {
			private String regExp;
			public Validator() {
				this.regExp = "(^[+-]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+-]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)";
			}
			public String isValid(Object value) {
				if(value == null) return "error";
				if(value.toString().matches(regExp)) return null;
				return "error";
			}
		}
		

		private CellEditor cellEditor;
		
		public ParameterValueEditingSupport(ColumnViewer viewer) {
			super(viewer);
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator());
			
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return cellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			ADWinVariable parameter = (ADWinVariable) element;
	        String value = parameter.getProperty(ADWinVariableProperties.PARAMETER_VALUE);
	        if(value == null) {
	        		value = "0";
	        		parameter.setProperty(ADWinVariableProperties.PARAMETER_VALUE, value);
	        		
	        }
	        return value;
		}

		@Override
		protected void setValue(Object element, Object value) {
			((ADWinVariable)element).setProperty(ADWinVariableProperties.PARAMETER_VALUE, (String)value);
			getViewer().refresh(element);
		}
		
	}

	private ADWinVariable[] parameters;

	public ParametersDialog(Shell parentShell, ADWinVariable[] parameters) {
		super(parentShell);
		this.parameters = parameters;
	}
	
	@Override
	public void create() {
		super.create();
		getShell().setText(DocometreMessages.ParametersDialogShellTitle);
		setTitle(DocometreMessages.ParametersDialogTitle);
		setMessage(DocometreMessages.ParametersDialogMessage, IMessageProvider.INFORMATION);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		TableViewer parametersTableViewer = new TableViewer(area, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		TableViewerColumn parameterNameColumn = new TableViewerColumn(parametersTableViewer, SWT.NONE);
		parameterNameColumn.getColumn().setWidth(200);
		parameterNameColumn.getColumn().setText(DocometreMessages.ParametersDialogParamNameColumn);
		parameterNameColumn.setLabelProvider(new ColumnLabelProvider() {
		    @Override
		    public String getText(Object element) {
		        ADWinVariable variable = (ADWinVariable) element;
		        return variable.getProperty(ChannelProperties.NAME);
		    }
		});
		
		TableViewerColumn parameterValueColumn = new TableViewerColumn(parametersTableViewer, SWT.NONE);
		parameterValueColumn.getColumn().setWidth(200);
		parameterValueColumn.getColumn().setText(DocometreMessages.ParametersDialogParamValueColumn);
		parameterValueColumn.setLabelProvider(new ColumnLabelProvider() {
		    @Override
		    public String getText(Object element) {
		        ADWinVariable variable = (ADWinVariable) element;
		        String value = variable.getProperty(ADWinVariableProperties.PARAMETER_VALUE);
		        return (value == null)? "0":value;
		    }
		});
		parameterValueColumn.setEditingSupport(new ParameterValueEditingSupport(parameterValueColumn.getViewer()));

		// make lines and header visible
		final Table table = parametersTableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		parametersTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		parametersTableViewer.setInput(parameters);

		return area;
		
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

}
