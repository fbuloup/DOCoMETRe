package fr.univamu.ism.docometre.analyse.functions;

import java.util.function.Function;

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
	private static final String fromKey = "from";
	private static final String toKey = "to";
	
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
		
		// Input
		Label inputSignalLabel = new Label(paramContainer, SWT.NONE);
		inputSignalLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		inputSignalLabel.setText(FunctionsMessages.InputSignalLabel);
		
		ComboViewer inputSignalComboViewer = new ComboViewer(paramContainer, SWT.BORDER);
		inputSignalComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String value  = getProperty(inputSignalKey, "");
		inputSignalComboViewer.getCombo().setText(value);
		inputSignalComboViewer.setContentProvider(new ChannelsContentProvider(true, false, false, false, false, false, false));
		inputSignalComboViewer.setLabelProvider(LabelProvider.createTextProvider(new Function<Object, String>() {
			@Override
			public String apply(Object t) {
				if(!(t instanceof Channel)) return null;
				Channel channel = (Channel)t;
				return channel.getFullName();
			}
		}));
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
		
		// From
		Label fromLabel = new Label(paramContainer, SWT.NONE);
		fromLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		fromLabel.setText(FunctionsMessages.FromLabel);
		
		ComboViewer fromComboViewer = new ComboViewer(paramContainer, SWT.BORDER);
		fromComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(fromKey, "");
		fromComboViewer.getCombo().setText(value);
		fromComboViewer.setContentProvider(new ChannelsContentProvider(false, false, false, true, false, true, true));
		fromComboViewer.setLabelProvider(LabelProvider.createTextProvider(new Function<Object, String>() {
			@Override
			public String apply(Object t) {
				if(!(t instanceof Channel)) return null;
				Channel channel = (Channel)t;
				return channel.getFullName();
			}
		}));
		fromComboViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, getProperty(fromKey, ""));
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
				getTransientProperties().put(fromKey, fromComboViewer.getCombo().getText());
			}
		});
		
		// To
		Label toLabel = new Label(paramContainer, SWT.NONE);
		toLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		toLabel.setText(FunctionsMessages.ToLabel);
		
		ComboViewer toComboViewer = new ComboViewer(paramContainer, SWT.BORDER);
		toComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(toKey, "");
		toComboViewer.getCombo().setText(value);
		toComboViewer.setContentProvider(new ChannelsContentProvider(false, false, false, true, false, true, true));
		toComboViewer.setLabelProvider(LabelProvider.createTextProvider(new Function<Object, String>() {
			@Override
			public String apply(Object t) {
				if(!(t instanceof Channel)) return null;
				Channel channel = (Channel)t;
				return channel.getFullName();
			}
		}));
		toComboViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		channel = MathEngineFactory.getMathEngine().getChannelFromName(SelectedExprimentContributionItem.selectedExperiment, getProperty(toKey, ""));
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
				getTransientProperties().put(toKey, toComboViewer.getCombo().getText());
			}
		});
		
		addCommentField(paramContainer, 1, context);
		
		return paramContainer;
	}
	
	@Override
	public String getCode(Object context, Object step) {
		String code = FunctionFactory.getProperty(context, functionFileName, FUNCTION_CODE);
		
		String inputSignal = getProperty(inputSignalKey, "");
		code = code.replaceAll(inputSignalKey, inputSignal);
		
		return code + "\n";
	}
	
	@Override
	public Block clone() {
		Mean function = new Mean();
		super.clone(function);
		return function;
	}

}
