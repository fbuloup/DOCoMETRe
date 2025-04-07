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

import java.util.Iterator;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinVariable;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class DeleteChannelsOperation extends AbstractOperation  {

	private StructuredSelection selectedChannels;
	private Module[] modules;
	private ADWinDACQConfiguration daqConfiguration;
	private boolean isVariable;
	private boolean isModuleChannel;
	private ModulePage modulePage;

	public DeleteChannelsOperation(ModulePage modulePage, String label, ADWinDACQConfiguration daqConfiguration, StructuredSelection selectedChannels, IUndoContext undoContext) {
		super(label);
		this.daqConfiguration = daqConfiguration;
		this.selectedChannels = selectedChannels;
		this.modulePage = modulePage;
		modules = new Module[selectedChannels.size()];
		addContext(undoContext);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		isVariable = false;
		isModuleChannel = false;
		for (int i = 0; i < selectedChannels.size(); i++) {
			Channel channel = (Channel) selectedChannels.toArray()[i];
			if (channel instanceof ADWinVariable) {
				isVariable = true;
				ADWinVariable variable = (ADWinVariable) channel;
				daqConfiguration.removeVariable(variable);
				variable.getDACQConfiguration().updateChannelsTransferNumber();
			} else {
				isModuleChannel = true;
				Module module = channel.getModule();
				module.removeChannel(channel);
				modules[i] = module;
				module.getDACQConfiguration().updateChannelsTransferNumber();
			}
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		setFocus();
		return execute(monitor, info);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		setFocus();
		if(isVariable) {
			for (Iterator variablesIterator = selectedChannels.iterator(); variablesIterator.hasNext();) {
				ADWinVariable variable = (ADWinVariable) variablesIterator.next();
				daqConfiguration.addVariable(variable);
				variable.getDACQConfiguration().updateChannelsTransferNumber();
			}
		}
		if(isModuleChannel) {
			for (int i = 0; i < selectedChannels.size(); i++) {
				Channel channel = (Channel) selectedChannels.toArray()[i];
				Module module = modules[i];
				module.addChannel(channel);
				module.getDACQConfiguration().updateChannelsTransferNumber();
			}
		}
		return Status.OK_STATUS;
	}
	
	private void setFocus() {
		ResourceEditor activeEditor = (ResourceEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		activeEditor.setActivePage(modulePage.getId());
	}

	
}
