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

import java.util.ArrayList;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnInModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoAnOutModule;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDigInOutModule;

public class DeleteModulesHandler extends SelectionAdapter implements ISelectionChangedListener {
	
		private StructuredSelection selectedModules;
		private Shell shell;
		private IOperationHistory operationHistory;
		private DACQConfiguration daqConfiguration;
		private IUndoContext undoContext;
		
		public DeleteModulesHandler(Shell shell, DACQConfiguration daqConfiguration, IUndoContext undoContext) {
			this.shell = shell;
			this.daqConfiguration = daqConfiguration;
			operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
			this.undoContext = undoContext;
		}

		@Override
		public void widgetSelected(SelectionEvent event) {
			if(selectedModules == null) return;
			if(daqConfiguration instanceof ArduinoUnoDACQConfiguration) {
				ArrayList<Object> modules = new ArrayList<>();
				Object[] modulesArray = selectedModules.toArray();
				for (Object module : modulesArray) {
					boolean keep = !((module instanceof ArduinoUnoAnInModule) || (module instanceof ArduinoUnoAnOutModule) || (module instanceof ArduinoUnoDigInOutModule));
					if(keep) modules.add(module);
				}
				selectedModules = new StructuredSelection(modules.toArray());
			}
			if(!selectedModules.isEmpty()) {
				if(MessageDialog.openQuestion(shell, ADWinMessages.DeleteModuleDialog_Title, ADWinMessages.DeleteModuleDialog_Message)) {
					try {
						operationHistory.execute(new DeleteModulesOperation(ADWinMessages.DeleteModulesOperation_Label, daqConfiguration, selectedModules, undoContext), null, null);
					} catch (ExecutionException e) {
						e.printStackTrace();
						Activator.logErrorMessageWithCause(e);
					}
				}
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			selectedModules = (StructuredSelection)event.getSelection();
		}
	}
