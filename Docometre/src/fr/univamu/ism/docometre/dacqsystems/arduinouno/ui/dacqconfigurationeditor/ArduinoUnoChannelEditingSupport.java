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
package fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.dacqconfigurationeditor;

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
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.DACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyOperation;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.dacqsystems.PropertyObserver;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDigInOutChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnInChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoVariableProperties;
import fr.univamu.ism.docometre.dacqsystems.charts.CurveConfigurationProperties;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ArduinoUnoChannelEditingSupport extends EditingSupport implements PropertyObserver {
	
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
	private DACQConfiguration dacqConfiguration;
	private Module module;
	private IUndoContext undoContext;
	private PartListenerAdapter partListenerAdapter;

	public ArduinoUnoChannelEditingSupport(ColumnViewer viewer, Property property, DACQConfiguration dacqConfiguration, final ResourceEditor editor, Module module) {
		super(viewer);
		this.property = property;
		this.dacqConfiguration = dacqConfiguration;
		this.module = module;
		this.undoContext = editor.getUndoContext();
		
		if(property == ArduinoUnoChannelProperties.USED) {
			cellEditor = new CheckboxCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ArduinoUnoChannelProperties.USED.getRegExp()));
		}
//		if(propertyKey == ArduinoUnoChannelProperties.USED) {
//			cellEditor = new CheckboxCellEditor((Composite) viewer.getControl());
//			cellEditor.setValidator(new Validator(ArduinoUnoChannelProperties.USED.getRegExp()));
//		}
//		if(propertyKey == ArduinoUnoChannelProperties.USED) {
//			cellEditor = new CheckboxCellEditor((Composite) viewer.getControl());
//			cellEditor.setValidator(new Validator(ArduinoUnoChannelProperties.USED.getRegExp()));
//		}
		if(property == ChannelProperties.NAME) {
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ChannelProperties.NAME.getRegExp()));
		}
		if(property == ChannelProperties.SAMPLE_FREQUENCY) {
			this.dacqConfiguration.addObserver(this);
			partListenerAdapter = new PartListenerAdapter() {
				@Override
				public void partClosed(IWorkbenchPartReference partRef) {
					if(partRef.getPart(false) == editor) {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
						ArduinoUnoChannelEditingSupport.this.dacqConfiguration.removeObserver(ArduinoUnoChannelEditingSupport.this);
					}
				}
			};
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
			
			cellEditor = new ComboBoxViewerCellEditor((Composite) viewer.getControl(), SWT.READ_ONLY);
			((ComboBoxViewerCellEditor)cellEditor).setContentProvider(new ArrayContentProvider());
			((ComboBoxViewerCellEditor)cellEditor).getViewer().setComparer(new IElementComparer() {
				public int hashCode(Object element) {
					return element.hashCode();
				}
				public boolean equals(Object a, Object b) {
					String aString = a.toString();
					String bString = b.toString();
					return aString.equals(bString);
				}
			});
			((ComboBoxViewerCellEditor)cellEditor).setLabelProvider(new LabelProvider());
			((ComboBoxViewerCellEditor)cellEditor).setInput(dacqConfiguration.getAvailableFrequencies(dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY)));
			cellEditor.setValidator(new Validator(ChannelProperties.SAMPLE_FREQUENCY.getRegExp()));
		}
		if(property == ChannelProperties.CHANNEL_NUMBER) {
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ChannelProperties.CHANNEL_NUMBER.getRegExp()));
		}
		if(property == ChannelProperties.TRANSFER) {
			cellEditor = new CheckboxCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ChannelProperties.TRANSFER.getRegExp()));
		}
		if(property == ChannelProperties.AUTO_TRANSFER) {
			cellEditor = new CheckboxCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ChannelProperties.AUTO_TRANSFER.getRegExp()));
		}
		if(property == ChannelProperties.TRANSFER_NUMBER) {
			// It's not possible to modify this number, it is auto-generated 
		}
		if(property == ChannelProperties.RECORD) {
			cellEditor = new CheckboxCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ChannelProperties.RECORD.getRegExp()));
		}
		if(property == ChannelProperties.BUFFER_SIZE) {
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ChannelProperties.BUFFER_SIZE.getRegExp()));
		}
		if(property == ArduinoUnoVariableProperties.TYPE) {
			cellEditor = new ComboBoxViewerCellEditor((Composite) viewer.getControl(), SWT.READ_ONLY);
			((ComboBoxViewerCellEditor)cellEditor).setContentProvider(new ArrayContentProvider());
			((ComboBoxViewerCellEditor)cellEditor).setLabelProvider(new LabelProvider());
			((ComboBoxViewerCellEditor)cellEditor).setInput(ArduinoUnoVariableProperties.TYPES);
			cellEditor.setValidator(new Validator(ArduinoUnoVariableProperties.TYPE.getRegExp()));
		}
		if(property == ArduinoUnoVariableProperties.SIZE) {
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ArduinoUnoVariableProperties.SIZE.getRegExp()));
		}
		
		if(property == ArduinoUnoAnInChannelProperties.UNIT) {
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ArduinoUnoAnInChannelProperties.UNIT.getRegExp()));
		}
		if(property == ArduinoUnoAnInChannelProperties.UNIT_MAX) {
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ArduinoUnoAnInChannelProperties.UNIT_MAX.getRegExp()));
		}
		if(property == ArduinoUnoAnInChannelProperties.UNIT_MIN) {
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ArduinoUnoAnInChannelProperties.UNIT_MIN.getRegExp()));
		}
		if(property == ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX) {
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ArduinoUnoAnInChannelProperties.AMPLITUDE_MAX.getRegExp()));
		}
		if(property == ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN) {
			cellEditor = new TextCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ArduinoUnoAnInChannelProperties.AMPLITUDE_MIN.getRegExp()));
		}
		if(property == ArduinoUnoAnInChannelProperties.GAIN) {
			cellEditor = new ComboBoxViewerCellEditor((Composite) viewer.getControl(), SWT.READ_ONLY);
			((ComboBoxViewerCellEditor)cellEditor).setContentProvider(new ArrayContentProvider());
			((ComboBoxViewerCellEditor)cellEditor).setLabelProvider(new LabelProvider());
			((ComboBoxViewerCellEditor)cellEditor).setInput(ArduinoUnoAnInChannelProperties.GAINS);
			cellEditor.setValidator(new Validator(ArduinoUnoAnInChannelProperties.GAIN.getRegExp()));
			
		}
		/*
		if(property == ADWinAnOutChannelProperties.STIMULUS) {
			cellEditor = new CheckboxCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ADWinAnOutChannelProperties.STIMULUS.getRegExp()));
		}*/

		if(property ==ArduinoUnoDigInOutChannelProperties.IN_OUT) {
			cellEditor = new ComboBoxViewerCellEditor((Composite) viewer.getControl(), SWT.READ_ONLY);
			((ComboBoxViewerCellEditor)cellEditor).setContentProvider(new ArrayContentProvider());
			((ComboBoxViewerCellEditor)cellEditor).setLabelProvider(new LabelProvider());
			((ComboBoxViewerCellEditor)cellEditor).setInput(ArduinoUnoDigInOutChannelProperties.INPUT_OUTPUT);
			cellEditor.setValidator(new Validator(ArduinoUnoDigInOutChannelProperties.IN_OUT.getRegExp()));
		}/*
		if(property == ADWinDigInOutChannelProperties.STIMULUS) {
			cellEditor = new CheckboxCellEditor((Composite) viewer.getControl());
			cellEditor.setValidator(new Validator(ADWinDigInOutChannelProperties.STIMULUS.getRegExp()));
		}*/
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
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue, AbstractElement element) {
		if(property == ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY) {
			int newGlobalFrequency = Integer.parseInt((String)newValue);
			Double[] frequencies = new Double[100];
			for (int i = 0; i < frequencies.length; i++) {
				frequencies[i] = Double.valueOf(newGlobalFrequency) / Double.valueOf(i+1);
			}
			((ComboBoxViewerCellEditor)cellEditor).setInput(frequencies);
		}
		if(property == DACQConfigurationProperties.UPDATE_MODULE) {
			if(oldValue == module) dacqConfiguration.removeObserver(this); 
		}
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
		if(!(element instanceof Channel)) return null;
		if(getCellEditor(element) instanceof CheckboxCellEditor) {
			String value = ((Channel)element).getProperty(property);
			return value.equals("true") ? true : false;
		}
		return ((Channel)element).getProperty(property);
	}

	@Override
	protected void setValue(Object element, Object value) {
		if((value != null && getValue(element) == null) || !value.toString().equals(getValue(element).toString())) {
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
