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

public class FindPeaks extends GenericFunction {
	
	private static final long serialVersionUID = 1L;
	
	public static final String functionFileName = "FIND_PEAKS.FUN";
	
	private static final String inputSignalKey = "inputSignal";
	private static final String inputMarker1Key = "inputMarker1";
	private static final String inputMarker2Key = "inputMarker2";
	private static final String markersGroupLabelKey = "markersGroupLabel";
	private static final String heightKey = "height";
	private static final String thresholdKey = "threshold";
	private static final String distanceKey = "distance";
	private static final String prominenceKey = "prominence";
	private static final String trialsListKey = "trialsList";
	
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
				FindPeaks.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				boolean putValue = true;
				String regExp = "^(?!([ \\d]*-){2})\\d+(?: *[-,:;] *\\d+)*$";
				Pattern pattern = Pattern.compile(regExp);
				Matcher matcher = pattern.matcher(trialsListText.getText());
				putValue = matcher.matches();
				if(putValue) getTransientProperties().put(trialsListKey, trialsListText.getText());
				else {
					String message = NLS.bind(FunctionsMessages.TrialsListNotValidLabel, trialsListText.getText());
					FindPeaks.this.functionalBlockConfigurationDialog.setErrorMessage(message);
				}
			}
		});
		
		// Input Signal 1
		Label inputSignal1Label = new Label(paramContainer, SWT.NONE);
		inputSignal1Label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		inputSignal1Label.setText(FunctionsMessages.InputSignalLabel);
		ComboViewer inputSignal1ComboViewer = new ComboViewer(paramContainer, SWT.BORDER);
		inputSignal1ComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(inputSignalKey, "");
		inputSignal1ComboViewer.getCombo().setText(value);
		inputSignal1ComboViewer.setContentProvider(new ChannelsContentProvider(true, false, false, false, false, false, false));
		inputSignal1ComboViewer.setLabelProvider(LabelProvider.createTextProvider(new Function<Object, String>() {
			@Override
			public String apply(Object t) {
				if(!(t instanceof Channel)) return null;
				Channel channel = (Channel)t;
				if(channel.isSignal()) return channel.getFullName();
				return null;
			}
		}));
		inputSignal1ComboViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		Channel channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, getProperty(inputSignalKey, ""));
		if(channel != null) inputSignal1ComboViewer.setSelection(new StructuredSelection(channel));
		else {
			String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, value);
			Activator.logErrorMessage(message);
			this.functionalBlockConfigurationDialog.setErrorMessage(DocometreMessages.FunctionalBlockConfigurationDialogBlockingMessage);
		}
		inputSignal1ComboViewer.getCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				FindPeaks.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				getTransientProperties().put(inputSignalKey, inputSignal1ComboViewer.getCombo().getText());
			}
		});
		
		// Input Marker 1 : from
		Label inputMarker1Label = new Label(paramContainer, SWT.NONE);
		inputMarker1Label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		inputMarker1Label.setText(FunctionsMessages.FromMarkerLabel);
		ComboViewer inputMarker1ComboViewer = new ComboViewer(paramContainer, SWT.BORDER);
		inputMarker1ComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(inputMarker1Key, "");
		inputMarker1ComboViewer.getCombo().setText(value);
		inputMarker1ComboViewer.setContentProvider(new ChannelsContentProvider(false, false, false, true, false, true, true));
		inputMarker1ComboViewer.setLabelProvider(FunctionsHelper.createTextProvider());
		inputMarker1ComboViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, getProperty(inputMarker1Key, ""));
		if(channel != null) inputMarker1ComboViewer.setSelection(new StructuredSelection(channel));
		else {
			String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, value);
			Activator.logErrorMessage(message);
			this.functionalBlockConfigurationDialog.setErrorMessage(DocometreMessages.FunctionalBlockConfigurationDialogBlockingMessage);
		}
		inputMarker1ComboViewer.getCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				FindPeaks.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				getTransientProperties().put(inputMarker1Key, inputMarker1ComboViewer.getCombo().getText());
			}
		});
		
		// Input Marker 2 : to
		Label inputMarker2Label = new Label(paramContainer, SWT.NONE);
		inputMarker2Label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		inputMarker2Label.setText(FunctionsMessages.ToMarkerLabel);
		ComboViewer inputMarker2ComboViewer = new ComboViewer(paramContainer, SWT.BORDER);
		inputMarker2ComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(inputMarker2Key, "");
		inputMarker2ComboViewer.getCombo().setText(value);
		inputMarker2ComboViewer.setContentProvider(new ChannelsContentProvider(false, false, false, true, false, true, true));
		inputMarker2ComboViewer.setLabelProvider(FunctionsHelper.createTextProvider());
		inputMarker2ComboViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, getProperty(inputMarker2Key, ""));
		if(channel != null) inputMarker2ComboViewer.setSelection(new StructuredSelection(channel));
		else {
			String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, value);
			Activator.logErrorMessage(message);
			this.functionalBlockConfigurationDialog.setErrorMessage(DocometreMessages.FunctionalBlockConfigurationDialogBlockingMessage);
		}
		inputMarker2ComboViewer.getCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				FindPeaks.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				getTransientProperties().put(inputMarker2Key, inputMarker2ComboViewer.getCombo().getText());
			}
		});
		
		// Markers group label 
		Label markersGroupLabelLabel = new Label(paramContainer, SWT.NONE);
		markersGroupLabelLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		markersGroupLabelLabel.setText(FunctionsMessages.MarkersGroupLabel);
		Text markersGroupLabelText = new Text(paramContainer, SWT.BORDER);
		markersGroupLabelText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(markersGroupLabelKey, "Maximum");
		markersGroupLabelText.setText(value);
		markersGroupLabelText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				FindPeaks.this.functionalBlockConfigurationDialog.setErrorMessage(null);
				boolean putValue = true;
				String regExp = "^[a-zA-Z][a-zA-Z0-9_]*$";
				Pattern pattern = Pattern.compile(regExp);
				Matcher matcher = pattern.matcher(markersGroupLabelText.getText());
				putValue = matcher.matches();
				if(putValue) getTransientProperties().put(markersGroupLabelKey, markersGroupLabelText.getText());
				else {
					String message = NLS.bind(FunctionsMessages.EndCutNotValidLabel, markersGroupLabelText.getText());
					FindPeaks.this.functionalBlockConfigurationDialog.setErrorMessage(message);
				}
			}
		});
		
		// Height
		Label heightLabel = new Label(paramContainer, SWT.NONE);
		heightLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		heightLabel.setText(DocometreMessages.heightLabel);
		Text heightText = new Text(paramContainer, SWT.BORDER);
		heightText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(heightKey, "None");
		heightText.setText(value);
		heightText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(heightKey, heightText.getText());
			}
		});
		
		// Threshold
		Label thresholdLabel = new Label(paramContainer, SWT.NONE);
		thresholdLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		thresholdLabel.setText(DocometreMessages.thresholdLabel);
		Text thresholdText = new Text(paramContainer, SWT.BORDER);
		thresholdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(thresholdKey, "None");
		thresholdText.setText(value);
		thresholdText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(thresholdKey, thresholdText.getText());
			}
		});
		
		// Distance
		Label distanceLabel = new Label(paramContainer, SWT.NONE);
		distanceLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		distanceLabel.setText(DocometreMessages.distanceLabel);
		Text distanceText = new Text(paramContainer, SWT.BORDER);
		distanceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(distanceKey, "None");
		distanceText.setText(value);
		distanceText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(distanceKey, distanceText.getText());
			}
		});
		
		// Prominence
		Label prominenceLabel = new Label(paramContainer, SWT.NONE);
		prominenceLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		prominenceLabel.setText(DocometreMessages.prominenceLabel);
		Text prominenceText = new Text(paramContainer, SWT.BORDER);
		prominenceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(prominenceKey, "None");
		prominenceText.setText(value);
		prominenceText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(prominenceKey, prominenceText.getText());
			}
		});
		
		addCommentField(paramContainer, 1, context);
		
		return paramContainer;
	}
	
	
	@Override
	public String getCode(Object context, Object step) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
		
		String code = FunctionFactory.getProperty(context, functionFileName, FUNCTION_CODE);
		
		String trialsList = getProperty(trialsListKey, "");
		trialsList = FunctionsHelper.createTrialsListHelper(trialsList);
		String inputSignal1 = getProperty(inputSignalKey, "");
		String inputMarker1 = getProperty(inputMarker1Key, "");
		String inputMarker2 = getProperty(inputMarker2Key, "");
		String markersGroupLabel = getProperty(markersGroupLabelKey, "Maxima");
		
		code = code.replaceAll(trialsListKey, trialsList).replaceAll(inputSignalKey, inputSignal1);
		code = code.replaceAll(inputMarker1Key, inputMarker1).replaceAll(inputMarker2Key, inputMarker2).replaceAll(markersGroupLabelKey, markersGroupLabel);
		
		String height = "";
		String threshold = "";
		String distance = "";
		String prominence = "";
		if(MathEngineFactory.isPython()) {
			height = "height=" + getProperty(heightKey, "None");
			threshold = "threshold=" + getProperty(thresholdKey, "None");
			distance = "distance=" + getProperty(distanceKey, "None");
			prominence = "prominence=" + getProperty(prominenceKey, "None");
			code = code.replaceAll(heightKey, height).replace(distanceKey, distance).replaceAll(thresholdKey, threshold).replaceAll(prominenceKey, prominence);
		}
		if(MathEngineFactory.isMatlab()) {
			if(getProperty(heightKey, "None").equals("None")) height = "";
			else height = "'MinPeakHeight'," + getProperty(heightKey, "0");
			if(getProperty(thresholdKey, "None").equals("None")) threshold = "";
			else threshold = ",'Threshold'," + getProperty(thresholdKey, "0");
			if(getProperty(distanceKey, "None").equals("None")) distance = "";
			else distance = ",'MinPeakDistance'," + getProperty(distanceKey, "0");
			if(getProperty(prominenceKey, "None").equals("None")) prominence = "";
			else prominence = ",'MinPeakProminence'," + getProperty(prominenceKey, "1");
			String value = "," + height + threshold + distance + prominence;
			value = value.equals(",")?"":value;
			String key = ", height, threshold, distance, prominence";
			code = code.replaceAll(key, value);
			code = code.replaceAll(",,", ",");
			
		}
		
		return code + "\n";
	}
	
	@Override
	public Block clone() {
		FindPeaks function = new FindPeaks();
		super.clone(function);
		return function;
	}

}
