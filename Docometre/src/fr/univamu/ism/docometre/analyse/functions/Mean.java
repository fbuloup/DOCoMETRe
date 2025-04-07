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
package fr.univamu.ism.docometre.analyse.functions;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.editors.ChannelsContentProvider;
import fr.univamu.ism.docometre.dacqsystems.functions.GenericFunction;
import fr.univamu.ism.docometre.dialogs.FunctionalBlockConfigurationDialog;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Script;

public class Mean extends GenericFunction {
	
	private static final long serialVersionUID = 1L;
	
	public static final String functionFileName = "MEAN.FUN";
	
	private static final String inputSignalKey = "inputSignal";
	private static final String fromInputSignalKey = "fromInputSignal";
	private static final String toInputSignalKey = "toInputSignal";
	
	transient private FunctionalBlockConfigurationDialog functionalBlockConfigurationDialog;
	
	@Override
	public String getFunctionFileName() {
		return functionFileName;
	}
	
	@Override
	public Object getGUI(Object functionalBlockConfigurationDialog, Object parent, Object context) {
		if(!(context instanceof Script)) return null;
		
		Composite container  = (Composite) parent;
		Composite paramContainer = new Composite(container, SWT.NORMAL);
		paramContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		paramContainer.setLayout(new GridLayout(3, false));
		
		this.functionalBlockConfigurationDialog = (FunctionalBlockConfigurationDialog) functionalBlockConfigurationDialog;
		
		if(!checkPreBuildGUI(this.functionalBlockConfigurationDialog, paramContainer, 2, context)) {
			return paramContainer;
		}
		
		// Input
		Label inputSignalLabel = new Label(paramContainer, SWT.NONE);
		inputSignalLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		inputSignalLabel.setText(FunctionsMessages.InputSignalLabel);
		
		ComboViewer inputSignalComboViewer = new ComboViewer(paramContainer, SWT.BORDER);
		inputSignalComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String value  = getProperty(inputSignalKey, "");
		ChannelsContentProvider channelsContentProvider = new ChannelsContentProvider(true, false, false, false, false, false, false);
		inputSignalComboViewer.getCombo().setText(value);
		inputSignalComboViewer.setContentProvider(channelsContentProvider);
		inputSignalComboViewer.setLabelProvider(FunctionsHelper.createTextProvider());
		inputSignalComboViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		Channel channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, getProperty(inputSignalKey, ""));
		if(channel != null) inputSignalComboViewer.setSelection(new StructuredSelection(channel));
		else {
			String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, value);
			Activator.logErrorMessage(message);
			this.functionalBlockConfigurationDialog.setErrorMessage(DocometreMessages.FunctionalBlockConfigurationDialogBlockingMessage);
		}
		inputSignalComboViewer.getCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Mean.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				getTransientProperties().put(inputSignalKey, inputSignalComboViewer.getCombo().getText());
			}
		});
		FunctionsHelper.addChannelsSelectionButton(paramContainer, "...", inputSignalComboViewer, channelsContentProvider);
		
		// From
		Label fromLabel = new Label(paramContainer, SWT.NONE);
		fromLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		fromLabel.setText(FunctionsMessages.FromLabel);
		
		ComboViewer fromComboViewer = new ComboViewer(paramContainer, SWT.BORDER);
		fromComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(fromInputSignalKey, "");
		fromComboViewer.getCombo().setText(value);
		ChannelsContentProvider channelsContentProviderFromTo = new ChannelsContentProvider(false, false, false, false, false, true, true);
		fromComboViewer.setContentProvider(channelsContentProviderFromTo);
		fromComboViewer.setLabelProvider(FunctionsHelper.createTextProvider());
		fromComboViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, getProperty(fromInputSignalKey, ""));
		if(channel != null) fromComboViewer.setSelection(new StructuredSelection(channel));
		else {
			String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, value);
			Activator.logErrorMessage(message);
			this.functionalBlockConfigurationDialog.setErrorMessage(DocometreMessages.FunctionalBlockConfigurationDialogBlockingMessage);
		}
		fromComboViewer.getCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Mean.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				getTransientProperties().put(fromInputSignalKey, fromComboViewer.getCombo().getText());
			}
		});
		FunctionsHelper.addChannelsSelectionButton(paramContainer, "...", fromComboViewer, channelsContentProviderFromTo);
		
		// To
		Label toLabel = new Label(paramContainer, SWT.NONE);
		toLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		toLabel.setText(FunctionsMessages.ToLabel);
		
		ComboViewer toComboViewer = new ComboViewer(paramContainer, SWT.BORDER);
		toComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(toInputSignalKey, "");
		toComboViewer.getCombo().setText(value);
		toComboViewer.setContentProvider(new ChannelsContentProvider(false, false, false, false, false, true, true));
		toComboViewer.setLabelProvider(FunctionsHelper.createTextProvider());
		toComboViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, getProperty(toInputSignalKey, ""));
		if(channel != null) toComboViewer.setSelection(new StructuredSelection(channel));
		else {
			String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, value);
			Activator.logErrorMessage(message);
			this.functionalBlockConfigurationDialog.setErrorMessage(DocometreMessages.FunctionalBlockConfigurationDialogBlockingMessage);
		}
		toComboViewer.getCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Mean.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				getTransientProperties().put(toInputSignalKey, toComboViewer.getCombo().getText());
			}
		});
		FunctionsHelper.addChannelsSelectionButton(paramContainer, "...", toComboViewer, channelsContentProviderFromTo);
		
		addCommentField(paramContainer, 2, context);
		
		return paramContainer;
	}
	
	@Override
	public String getCode(Object context, Object step, Object...objects) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
				
		String code = FunctionFactory.getProperty(context, functionFileName, FUNCTION_CODE);
		
		String inputSignal = getProperty(inputSignalKey, "");
		String fromInputSignal = getProperty(fromInputSignalKey, "");
		String toInputSignal = getProperty(toInputSignalKey, "");
		code = code.replaceAll(inputSignalKey, inputSignal).replaceAll(fromInputSignalKey, fromInputSignal).replaceAll(toInputSignalKey, toInputSignal);
		
		return code + "\n";
	}
	
	@Override
	public Block clone() {
		Mean function = new Mean();
		super.clone(function);
		return function;
	}

}
