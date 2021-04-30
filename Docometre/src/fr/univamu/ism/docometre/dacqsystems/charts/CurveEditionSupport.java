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
package fr.univamu.ism.docometre.dacqsystems.charts;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColorCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.PropertyObserver;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyOperation;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class CurveEditionSupport extends EditingSupport implements PropertyObserver {
	
	private class Validator implements ICellEditorValidator {
		private String regExp;
		public Validator(String regExp) {
			this.regExp = regExp;
		}
		public String isValid(Object value) {
			if(value == null) return "error";
			if(value.toString().matches(regExp)) return null;
			return "error";
		}
	}
	
	private Property property;
	private CellEditor cellEditor;
//	private DACQConfiguration dacqConfiguration;
//	private Module module;
	private IUndoContext undoContext;
//	private PartListenerAdapter partListenerAdapter;

	public CurveEditionSupport(ColumnViewer viewer, Property property, DACQConfiguration dacqConfiguration, final ResourceEditor editor, Module module) {
		super(viewer);
		this.property = property;
//		this.dacqConfiguration = dacqConfiguration;
//		this.module = module;
		this.undoContext = editor.getUndoContext();
		
		if(property == CurveConfigurationProperties.COLOR) {
			cellEditor = new ColorCellEditor((Composite) viewer.getControl());
//			cellEditor.setValidator(new Validator(CurveConfigurationProperties.COLOR.getRegExp()));
		}
		if(property == CurveConfigurationProperties.STYLE) {
			cellEditor = new ComboBoxViewerCellEditor((Composite) viewer.getControl(), SWT.READ_ONLY);
			((ComboBoxViewerCellEditor)cellEditor).setContentProvider(new ArrayContentProvider());
			((ComboBoxViewerCellEditor)cellEditor).setLabelProvider(new LabelProvider());
			((ComboBoxViewerCellEditor)cellEditor).setInput(CurveConfigurationProperties.STYLES);
			cellEditor.setValidator(new Validator(CurveConfigurationProperties.STYLE.getRegExp()));
		}
		if(property == CurveConfigurationProperties.WIDTH) {
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(CurveConfigurationProperties.WIDTH.getRegExp()));
		}
		if(property == OscilloCurveConfigurationProperties.DISPLAY_CURRENT_VALUES) {
			cellEditor = new CheckboxCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(OscilloCurveConfigurationProperties.DISPLAY_CURRENT_VALUES.getRegExp()));
		}
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		// TODO Auto-generated method stub

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
		if(!(element instanceof CurveConfiguration) || !(element instanceof OscilloCurveConfiguration)) return null;
		if(getCellEditor(element) instanceof ColorCellEditor) {
			RGB rgbValue = new RGB(255, 255, 255);
			if(element instanceof CurveConfiguration) rgbValue = CurveConfigurationProperties.getColor((CurveConfiguration)element).getRGB();
			return rgbValue;
		}
		if(getCellEditor(element) instanceof CheckboxCellEditor) {
			String value = "false";
			if(element instanceof OscilloCurveConfiguration) value = ((OscilloCurveConfiguration)element).getProperty(property);
			return "true".equals(value) ? true : false;
		}
		return ((CurveConfiguration)element).getProperty(property);
	}

	@Override
	protected void setValue(Object element, Object value) {
		if(!value.toString().equals(getValue(element).toString())) {
			try {
				String label = NLS.bind(DocometreMessages.ModifyPropertyOperation_Label, property.getLabel() + " : " + element.toString());
				IOperationHistory operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
				ModifyPropertyOperation modifyPropertyOperation = new ModifyPropertyOperation(getViewer().getControl(), label, property, (AbstractElement)element, value.toString(), undoContext);
				getViewer().getControl().setData(Integer.toString(modifyPropertyOperation.hashCode()), ((TableViewer)getViewer()).getTable().getSelectionIndex());
				operationHistory.execute(modifyPropertyOperation, null, null);
			} catch (ExecutionException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			} 
			getViewer().refresh(element);
		}
	}

}
