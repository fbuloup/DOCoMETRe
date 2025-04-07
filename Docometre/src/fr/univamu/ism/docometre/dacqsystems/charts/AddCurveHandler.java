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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dialogs.AddCurveDialog;
import fr.univamu.ism.docometre.editors.ModulePage;

public class AddCurveHandler extends SelectionAdapter implements ISelectionChangedListener {

	private ChartConfiguration selectedChartConfiguration;
	private IOperationHistory operationHistory;
	private ObjectUndoContext undoContext;
	private ModulePage modulePage;
	private DACQConfiguration dacqConfiguration;
	private ArrayList<Double> reservedReferenceValues = new ArrayList<>();; 

	public AddCurveHandler(ModulePage modulePage, DACQConfiguration dacqConfiguration, ObjectUndoContext undoContext) {
		operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
		this.undoContext = undoContext;
		this.modulePage = modulePage;
		this.dacqConfiguration = dacqConfiguration;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void widgetSelected(SelectionEvent event) {
		try {
			reservedReferenceValues.clear();
			for (CurveConfiguration curveConfiguration : selectedChartConfiguration.curvesConfigurations) {
				if(curveConfiguration instanceof OscilloCurveConfiguration) {
					Channel channel = ((OscilloCurveConfiguration)curveConfiguration).getChannel();
					if(channel instanceof HorizontalReferenceChannel) {
						reservedReferenceValues.add(((HorizontalReferenceChannel)channel).getValue());
					}
				}
			}
			AddCurveDialog addCurveDialog = new AddCurveDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), dacqConfiguration, selectedChartConfiguration);
			if(addCurveDialog.open() == Window.OK) {
				IStructuredSelection selection = addCurveDialog.getSelection();
				List<Channel> selectedChannels = new ArrayList<Channel>(selection.toList());
				List<Channel> selectedChannelsToRemove = new ArrayList<>();
				selectedChannels.forEach(new Consumer<Channel>() {
					public void accept(final Channel channel) {
						if(channel instanceof HorizontalReferenceChannel) {
							IInputValidator inputValidator = new IInputValidator() {
								@Override
								public String isValid(String newText) {
									if(!Pattern.matches("^(-)?\\d+(\\.\\d*)?$", newText)) return NLS.bind(DocometreMessages.IsNotAValidValue, newText);
									if(reservedReferenceValues.contains(Double.parseDouble(newText))) return DocometreMessages.ErrorValueAlreadyUsed;
									return null;
								}
							};
							InputDialog inputDialog = new InputDialog(Display.getDefault().getActiveShell(), DocometreMessages.ReferenceValueDialogTitle, DocometreMessages.ReferenceValueDialogMessage, "0", inputValidator);
							if(inputDialog.open() == Window.OK) ((HorizontalReferenceChannel)channel).setValue(inputDialog.getValue());
							else selectedChannelsToRemove.add(channel);
						}
					};
				});
				selectedChannels.removeAll(selectedChannelsToRemove);
				if(selectedChannels.size() > 0) {
					selection = new StructuredSelection(selectedChannels.toArray()); 
					operationHistory.execute(new AddCurveOperation(modulePage, DocometreMessages.AddCurveOperation_Label, selectedChartConfiguration, selection, undoContext), null, null);
				}
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		
		IStructuredSelection selection = (IStructuredSelection)event.getSelection();
		if(selection.getFirstElement() instanceof ChartConfiguration) {
			if(selection.size() == 1) {
				selectedChartConfiguration = (ChartConfiguration) selection.getFirstElement();
			} else {
				selectedChartConfiguration = null;
				reservedReferenceValues.clear();
			}
			
		}
	}

}
