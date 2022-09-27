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

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
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

public class ReplaceSamples extends GenericFunction {
	
	private static final long serialVersionUID = 1L;
	
	public static final String functionFileName = "REPLACE_SAMPLES.FUN";
	
	private static final String inputSignalKey = "inputSignal";
	private static final String fromIndexKey = "fromIndex";
	private static final String toIndexKey = "toIndex";
	private static final String replacementIndexKey = "replacementIndex";
	private static final String outputSignalSuffixKey = "outputSignal";
	private static final String trialsListKey = "trialsList";
	
	transient private FunctionalBlockConfigurationDialog functionalBlockConfigurationDialog;
	transient private Spinner fromIndexSpinner;
	transient private Spinner toIndexSpinner;
	transient private Spinner replacementIndexSpinner;
	transient private Channel channel;
	
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

		// Trials list
		Label trialsListLabel = new Label(paramContainer, SWT.NONE);
		trialsListLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		trialsListLabel.setText(FunctionsMessages.TrialsList);
		Text trialsListText = new Text(paramContainer, SWT.BORDER);
		trialsListText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String value  = getProperty(trialsListKey, "1:10,15,20:25");
		trialsListText.setText(value);
		trialsListText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				ReplaceSamples.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				boolean putValue = true;
				String regExp = "^(?!([ \\d]*-){2})\\d+(?: *[-,:;] *\\d+)*$";
				Pattern pattern = Pattern.compile(regExp);
				Matcher matcher = pattern.matcher(trialsListText.getText());
				putValue = matcher.matches();
				if(putValue) getTransientProperties().put(trialsListKey, trialsListText.getText());
				else {
					String message = NLS.bind(FunctionsMessages.TrialsListNotValidLabel, trialsListText.getText());
					ReplaceSamples.this.functionalBlockConfigurationDialog.setErrorMessage(message);
				}
			}
		});
		
		// Input Signal
		Label inputSignalLabel = new Label(paramContainer, SWT.NONE);
		inputSignalLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		inputSignalLabel.setText(FunctionsMessages.InputSignalLabel);
		ComboViewer inputSignalComboViewer = new ComboViewer(paramContainer, SWT.BORDER);
		inputSignalComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(inputSignalKey, "");
		inputSignalComboViewer.getCombo().setText(value);
		inputSignalComboViewer.setContentProvider(new ChannelsContentProvider(true, false, false, false, false, false, false));
		inputSignalComboViewer.setLabelProvider(LabelProvider.createTextProvider(new Function<Object, String>() {
			@Override
			public String apply(Object t) {
				if(!(t instanceof Channel)) return null;
				Channel channel = (Channel)t;
				if(channel.isSignal()) return channel.getFullName();
				return null;
			}
		}));
		inputSignalComboViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, getProperty(inputSignalKey, ""));
		if(channel != null) inputSignalComboViewer.setSelection(new StructuredSelection(channel));
		else {
			String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, value);
			Activator.logErrorMessage(message);
			this.functionalBlockConfigurationDialog.setErrorMessage(DocometreMessages.FunctionalBlockConfigurationDialogBlockingMessage);
		}
		inputSignalComboViewer.getCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				ReplaceSamples.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				getTransientProperties().put(inputSignalKey, inputSignalComboViewer.getCombo().getText());
				channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, getProperty(inputSignalKey, ""));
				ReplaceSamples.this.updateValues();
			}
		});
		
		// From index
		Label fromIndexLabel = new Label(paramContainer, SWT.NONE);
		fromIndexLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		fromIndexLabel.setText(FunctionsMessages.FromIndexLabel);
		fromIndexSpinner = new Spinner(paramContainer, SWT.BORDER);
		fromIndexSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fromIndexSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				updateFromIndex(e.count);
			}
		});
		fromIndexSpinner.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				updateFromIndex(0);
			}
		});
		
		// To index
		Label toIndexLabel = new Label(paramContainer, SWT.NONE);
		toIndexLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		toIndexLabel.setText(FunctionsMessages.ToIndexLabel);
		toIndexSpinner = new Spinner(paramContainer, SWT.BORDER);
		toIndexSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		toIndexSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				updateToIndex(e.count);
			}
		});
		toIndexSpinner.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				updateToIndex(0);
			}
		});
		
		// Replacement index
		Label replacementIndexLabel = new Label(paramContainer, SWT.NONE);
		replacementIndexLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		replacementIndexLabel.setText(FunctionsMessages.ReplacementIndexLabel);
		replacementIndexSpinner = new Spinner(paramContainer, SWT.BORDER);
		replacementIndexSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		replacementIndexSpinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				updateReplacementIndex(e.count);
			}
		});
		replacementIndexSpinner.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				updateReplacementIndex(0);
			}
		});
		
		updateValues();
		
		fromIndexSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				ReplaceSamples.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				getTransientProperties().put(fromIndexKey, fromIndexSpinner.getText());
			}
		});
		
		toIndexSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				ReplaceSamples.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				getTransientProperties().put(toIndexKey, toIndexSpinner.getText());
			}
		});
		
		replacementIndexSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				ReplaceSamples.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				getTransientProperties().put(replacementIndexKey, replacementIndexSpinner.getText());
			}
		});
		
		// Output signal
		Label outputSignalSuffixLabel = new Label(paramContainer, SWT.NONE);
		outputSignalSuffixLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		outputSignalSuffixLabel.setText(FunctionsMessages.OutputSignalSuffixLabel);
		Text outputSignalSuffixText = new Text(paramContainer, SWT.BORDER);
		outputSignalSuffixText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(outputSignalSuffixKey, "Rep");
		outputSignalSuffixText.setText(value);
		outputSignalSuffixText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(outputSignalSuffixKey, outputSignalSuffixText.getText());
			}
		});
		
		addCommentField(paramContainer, 1, context);
		
		return paramContainer;
	}
	
	private void updateFromIndex(int count) {
		int newValue = fromIndexSpinner.getSelection() + count;
		if(toIndexSpinner.getSelection() > newValue) 
			if(newValue > 0 && newValue <= fromIndexSpinner.getMaximum())
				fromIndexSpinner.setSelection(newValue);
	}
	
	private void updateToIndex(int count) {
		int newValue = toIndexSpinner.getSelection() + count;
		if(fromIndexSpinner.getSelection() < newValue) 
			if(newValue > 0 && newValue <= toIndexSpinner.getMaximum())
				toIndexSpinner.setSelection(newValue);
	}
	
	private void updateReplacementIndex(int count) {
		int newValue = replacementIndexSpinner.getSelection() + count;
		if(newValue > 0 && newValue <= replacementIndexSpinner.getMaximum())
			replacementIndexSpinner.setSelection(newValue);
		
	}
	
	private void updateValues() {
		int maximumIndex = 3;
		if(channel != null) maximumIndex = MathEngineFactory.getMathEngine().getSamplesNumber(channel, 1);
		String fromIndexValue  = getProperty(fromIndexKey, "1");
		String toIndexValue  = getProperty(toIndexKey, "2");
		String replamcementIndexValue  = getProperty(replacementIndexKey, "3");
		fromIndexSpinner.setValues(Integer.parseInt(fromIndexValue), 1, maximumIndex, 0, 1, 10);
		toIndexSpinner.setValues(Integer.parseInt(toIndexValue), 1, maximumIndex, 0, 1, 10);
		replacementIndexSpinner.setValues(Integer.parseInt(replamcementIndexValue), 1, maximumIndex, 0, 1, 10);
	}
	
	@Override
	public String getCode(Object context, Object step, Object...objects) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
		
		String code = FunctionFactory.getProperty(context, functionFileName, FUNCTION_CODE);
		
		String trialsList = getProperty(trialsListKey, "");
		trialsList = FunctionsHelper.createTrialsListHelper(trialsList);
		String inputSignal = getProperty(inputSignalKey, "");
		String fromIndex = getProperty(fromIndexKey, "");
		String toIndex = getProperty(toIndexKey, "");
		String replacementIndex = getProperty(replacementIndexKey, "");
		String outputSignal = inputSignal + getProperty(outputSignalSuffixKey, "MDir");
		
		code = code.replaceAll(trialsListKey, trialsList).replaceAll(inputSignalKey, inputSignal);
		code = code.replaceAll(fromIndexKey, fromIndex).replaceAll(toIndexKey, toIndex).replaceAll(replacementIndexKey, replacementIndex);
		code = code.replaceAll(outputSignalSuffixKey, outputSignal);
		
		return code + "\n";
	}
	
	@Override
	public Block clone() {
		Maximum function = new Maximum();
		super.clone(function);
		return function;
	}

}
