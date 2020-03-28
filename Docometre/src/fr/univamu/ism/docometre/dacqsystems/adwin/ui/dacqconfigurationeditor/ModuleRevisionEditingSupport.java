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
package fr.univamu.ism.docometre.dacqsystems.adwin.ui.dacqconfigurationeditor;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinCANModule;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinRS232Module;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ModuleRevisionEditingSupport extends EditingSupport {
	
	private ComboBoxViewerCellEditor moduleRevisionEditor;
	private IUndoContext undoContext;
	private DACQConfiguration daqConfiguration;

	public ModuleRevisionEditingSupport(ColumnViewer viewer, DACQConfiguration daqConfiguration, ResourceEditor editor) {
		super(viewer);
		this.daqConfiguration = daqConfiguration;
		moduleRevisionEditor = new ComboBoxViewerCellEditor((Composite) viewer.getControl(), SWT.READ_ONLY);
		moduleRevisionEditor.setContentProvider(new ArrayContentProvider());
		moduleRevisionEditor.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return (String) element;
			}
		});
		moduleRevisionEditor.setInput(ADWinModuleProperties.REVISION.getAvailableValues());
		this.undoContext = editor.getUndoContext();
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return moduleRevisionEditor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		return ((Module) element).getProperty(ADWinModuleProperties.REVISION);
	}

	@Override
	protected void setValue(Object element, Object value) {
		boolean runOperation = true; 
		// When current module is not CAN or SERIAL, it cannot have module number that already exists
		String className = element.getClass().getCanonicalName();
		String CANModuleClassName = ADWinCANModule.class.getCanonicalName();
		String RS232ModuleClassName = ADWinRS232Module.class.getCanonicalName();
		if(!className.equals(CANModuleClassName) && !className.equals(RS232ModuleClassName)) {
			Module[] modules = daqConfiguration.getModules();
			for (Module module : modules) {
				if(module.getClass().toString().equals(element.getClass().toString()) && module != element) {
					String moduleRevision = module.getProperty(ADWinModuleProperties.REVISION);
					runOperation = runOperation && !((String)value).equals(moduleRevision);
					if(!runOperation) break;
				}
			}
		}
		
		if(!((String)value).equals(getValue(element)) && runOperation) {
			try {
				String label = NLS.bind(DocometreMessages.ModifyPropertyOperation_Label, ADWinModuleProperties.REVISION.getLabel() + " : " + element.toString());
				IOperationHistory operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
				ModifyModulePropertyOperation modifyModulePropertyOperation = new ModifyModulePropertyOperation(getViewer().getControl(), label, ADWinModuleProperties.REVISION, (String) value, (Module) element, undoContext);
				getViewer().getControl().setData(Integer.toString(modifyModulePropertyOperation.hashCode()), ((TableViewer)getViewer()).getTable().getSelectionIndex());
				operationHistory.execute(modifyModulePropertyOperation, null, null);
			} catch (ExecutionException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}
	}

}
