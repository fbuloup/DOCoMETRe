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
import java.util.Arrays;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinRS232Module;
import fr.univamu.ism.docometre.editors.ModulePage;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class AddRS232ChannelsDialogHandler extends SelectionAdapter implements ISelectionChangedListener {
	
	private Shell shell;
	private IOperationHistory operationHistory;
	private DACQConfiguration dacqConfiguration;
	private IUndoContext undoContext;
	private ListViewer channelsListViewer;
	private ArrayList<Channel> selectedChannelsList = new ArrayList<Channel>();
	private ModulePage modulePage;
	
	private class AddChannelsDialogOperation extends AbstractOperation {
		
		private Channel[] fromChannels;
		private ArrayList<Channel> createdChannels = new ArrayList<>(0);
		private ModulePage modulePage;
		private ADWinRS232Module module;

		public AddChannelsDialogOperation(Channel[] channels, ModulePage modulePage, IUndoContext undoContext) {
			super(ADWinMessages.AddChannelsOperation_Label);
			this.fromChannels = channels;
			this.modulePage = modulePage;
			this.module = (ADWinRS232Module) modulePage.getModule();
			addContext(undoContext);
			int tranferNumber = module.getChannelsNumber() + 1;
			for (Channel fromChannel : fromChannels) {
				Channel channel = module.createChannel(fromChannel, tranferNumber++);
				channel.addObserver(module);
				createdChannels.add(channel);
				module.removeChannel(channel);
			}
		}

		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			for (Channel channel : createdChannels) module.addChannel(channel);
			return Status.OK_STATUS;
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			setFocus();
			return execute(monitor, info);
		}

		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			for (Channel channel : createdChannels) module.removeChannel(channel);
			setFocus();
			return Status.OK_STATUS;
		}
		
		private void setFocus() {
			ResourceEditor activeEditor = (ResourceEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			activeEditor.setActivePage(modulePage.getId());
		}
		
	}

	private class AddChannelDialog extends TitleAreaDialog {

		public AddChannelDialog(Shell parentShell) {
			super(parentShell);
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(ADWinMessages.AddChannelsDialog_ShellTitle);
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			setTitle(ADWinMessages.AddChannelsDialog_Title);
			setMessage(ADWinMessages.AddChannelsDialog_Message);
			setTitleImage(Activator.getImage(IImageKeys.MODULE_WIZBAN));
			Composite container = (Composite) super.createDialogArea(parent);
			
			channelsListViewer = new ListViewer(container);
			channelsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			channelsListViewer.setContentProvider(new ArrayContentProvider());
			channelsListViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((Channel)element).getProperty(ChannelProperties.NAME);
				}
			});
			
			ArrayList<Channel> channelsArrayList = new ArrayList<>(0);
			channelsArrayList.addAll(Arrays.asList(dacqConfiguration.getChannels()));
//			channelsArrayList.addAll(Arrays.asList(((ADWinDACQConfiguration)dacqConfiguration).getVariables()));
//			Module[] modules = dacqConfiguration.getModules();
//			for (Module module : modules) {
//				boolean getChannels = !(module instanceof ADWinRS232Module && module.getProperty(ADWinRS232ModuleProperties.SYSTEM_TYPE).equals(ADWinRS232ModuleProperties.ICE_SYSTEM_TYPE));
//				if(getChannels) {
//					channelsArrayList.addAll(Arrays.asList(module.getChannels()));
//				}
//			}
			// Remove channels already used by this module
			Channel[] moduleChannels = modulePage.getModule().getChannels();
			for (Channel moduleChannel : moduleChannels) {
				int i = 0;
				while (i < channelsArrayList.size()) {
					Channel channel = channelsArrayList.get(i);
					String moduleChannelName = moduleChannel.getProperty(ChannelProperties.NAME);
					String channelName = channel.getProperty(ChannelProperties.NAME);
					if(moduleChannelName.equals(channelName)) channelsArrayList.remove(i);
					else i++;
				}
			}
			
			channelsListViewer.setInput(channelsArrayList.toArray(new Channel[channelsArrayList.size()]));
			channelsListViewer.addSelectionChangedListener(AddRS232ChannelsDialogHandler.this);
			channelsListViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					buttonPressed(IDialogConstants.OK_ID); 
				}
			});
			
			return container;
		}
		
	}
	
	public AddRS232ChannelsDialogHandler(Shell shell, ResourceEditor editor, ModulePage modulePage) {
		this.shell = shell;
		operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
		this.dacqConfiguration = modulePage.getModule().getDACQConfiguration();
		this.undoContext = editor.getUndoContext();
		this.modulePage = modulePage;
	}
	
	@Override
	public void widgetSelected(SelectionEvent event) {
		AddChannelDialog addChannelDialog = new AddChannelDialog(shell);
		if(addChannelDialog.open() == Dialog.OK) {
			try {
				operationHistory.execute(new AddChannelsDialogOperation(selectedChannelsList.toArray(new Channel[selectedChannelsList.size()]), modulePage, undoContext), null, null);
			} catch (ExecutionException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		selectedChannelsList.clear();
		StructuredSelection selection = (StructuredSelection)channelsListViewer.getSelection();
		selectedChannelsList.addAll(selection.toList());
	}


}
