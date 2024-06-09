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

import java.util.ArrayList;
import java.util.function.Function;


import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

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

public class ChangeSampleFrequency extends GenericFunction {
	
	private static final long serialVersionUID = 1L;
	
	public static final String functionFileName = "CHANGE_SAMPLE_FREQUENCY.FUN";
	
	private static final String inputSignalsKey = "inputSignals";
	private static final String sampleFrequencyKey = "sampleFrequency";
	
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
		paramContainer.setLayout(new GridLayout(2, false));
		
		this.functionalBlockConfigurationDialog = (FunctionalBlockConfigurationDialog) functionalBlockConfigurationDialog;
		
		if(!checkPreBuildGUI(this.functionalBlockConfigurationDialog, paramContainer, 2, context)) {
			return paramContainer;
		}
		
		Label inputSignalLabel = new Label(paramContainer, SWT.NONE);
		inputSignalLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		inputSignalLabel.setText(FunctionsMessages.InputSignalsLabel);
		
		ListViewer inputSignalsListViewer = new ListViewer(paramContainer, SWT.BORDER | SWT.MULTI);
		inputSignalsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		inputSignalsListViewer.setContentProvider(new ChannelsContentProvider(true, false, false, false, false, false, false));
		inputSignalsListViewer.setLabelProvider(LabelProvider.createTextProvider(new Function<Object, String>() {
			@Override
			public String apply(Object t) {
				if(!(t instanceof Channel)) return null;
				Channel channel = (Channel)t;
				if(channel.isSignal()) return channel.getFullName();
				return null;
			}
		}));
		inputSignalsListViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		
		String[] selectedChannels = getProperty(inputSignalsKey, "").split(";");
		ArrayList<Channel> channels = new ArrayList<>();
		for (String selectedChannel : selectedChannels) {
			Channel channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, selectedChannel);
			if(channel != null) channels.add(channel);
			else {
				if(!"".equals(selectedChannel)) {
					String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, selectedChannel);
					Activator.logErrorMessage(message);
					this.functionalBlockConfigurationDialog.setErrorMessage(DocometreMessages.FunctionalBlockConfigurationDialogBlockingMessage);
				}
			}
		}
		StructuredSelection structuredSelection = new StructuredSelection(channels);
		inputSignalsListViewer.setSelection(structuredSelection);
		
		inputSignalsListViewer.getList().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String value = "";
				ChangeSampleFrequency.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				IStructuredSelection selection = inputSignalsListViewer.getStructuredSelection();
				Object[] selectedChannels = selection.toArray();
				for(int i = 0; i < selectedChannels.length; i++) {
					Channel channel = (Channel)selectedChannels[i];
					value = value + channel.getFullName();
					if(i < selectedChannels.length - 1) value = value + ";"; 
				}
				getTransientProperties().put(inputSignalsKey, value);
			}
		});
		
		Label sampleFrequencyLabel = new Label(paramContainer, SWT.NONE);
		sampleFrequencyLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		sampleFrequencyLabel.setText(FunctionsMessages.NewSampleFrequencyValueLabel);
		Text sampleFrequencyText = new Text(paramContainer, SWT.BORDER);
		sampleFrequencyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String value  = getProperty(sampleFrequencyKey, "100");
		sampleFrequencyText.setText(value);
		sampleFrequencyText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(sampleFrequencyKey, sampleFrequencyText.getText());
			}
		});
		
		addCommentField(paramContainer, 1, context);
		
		return paramContainer;
	}
	
	
	@Override
	public String getCode(Object context, Object step, Object...objects) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
		
		String rootCode = FunctionFactory.getProperty(context, functionFileName, FUNCTION_CODE);
		String code = ""; 
		
		String[] channels = getProperty(inputSignalsKey, "").split(";");
		String newSampleFrequency = getProperty(sampleFrequencyKey, "100");
		
		for (String channel : channels) {
			code = code + rootCode.replaceAll("inputSignal", channel).replaceAll("sampleFrequency", newSampleFrequency);
			code = code + "\n";
		}
		return code + "\n";
	}
	
	@Override
	public Block clone() {
		ChangeSampleFrequency function = new ChangeSampleFrequency();
		super.clone(function);
		return function;
	}

}
