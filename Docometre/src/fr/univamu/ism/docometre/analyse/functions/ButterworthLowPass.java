package fr.univamu.ism.docometre.analyse.functions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.dacqsystems.functions.GenericFunction;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Script;

public class ButterworthLowPass extends GenericFunction {
	
	private static final long serialVersionUID = 1L;
	
	public static final String functionFileName = "BUTTERWORTH_LOWPASS.FUN";
	
	private static final String orderKey = "order";
	private static final String cutOffFrequencyKey = "cutOffFrequency";
	private static final String inputSignalKey = "inputSignal";
	private static final String outputSignalSuffixKey = "outputSignal";
	
	@Override
	public String getFunctionFileName() {
		return functionFileName;
	}
	
	@Override
	public Object getGUI(Object titleAreaDialog, Object parent, Object context) {
		if(!(context instanceof Script)) return null;
		
		Composite container  = (Composite) parent;
		Composite paramContainer = new Composite(container, SWT.NORMAL);
		paramContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		paramContainer.setLayout(new GridLayout(2, false));
		
		Label orderLabel = new Label(paramContainer, SWT.NONE);
		orderLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		orderLabel.setText(FunctionsMessages.OrderLabel);
		Spinner orderSpinner = new Spinner(paramContainer, SWT.BORDER);
		orderSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String value  = getProperty(orderKey, "2");
		orderSpinner.setValues(Integer.valueOf(value), 1, 100, 0, 1, 10);
		orderSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(orderKey, orderSpinner.getText());
			}
		});
		
		Label cutOffLabel = new Label(paramContainer, SWT.NONE);
		cutOffLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		cutOffLabel.setText(FunctionsMessages.CutoffFrequencyLabel);
		Text cutOffFrequencyText = new Text(paramContainer, SWT.BORDER);
		cutOffFrequencyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(cutOffFrequencyKey, "5");
		cutOffFrequencyText.setText(value);
		cutOffFrequencyText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(cutOffFrequencyKey, cutOffFrequencyText.getText());
			}
		});
		
		Label inputSignalLabel = new Label(paramContainer, SWT.NONE);
		inputSignalLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		inputSignalLabel.setText(FunctionsMessages.InputSignalLabel);
		Text inputSignalText = new Text(paramContainer, SWT.BORDER);
		inputSignalText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(inputSignalKey, "");
		inputSignalText.setText(value);
		inputSignalText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(inputSignalKey, inputSignalText.getText());
			}
		});
		
		Label outputSignalSuffixLabel = new Label(paramContainer, SWT.NONE);
		outputSignalSuffixLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		outputSignalSuffixLabel.setText(FunctionsMessages.OutputSignalSuffixLabel);
		Text outputSignalSuffixText = new Text(paramContainer, SWT.BORDER);
		outputSignalSuffixText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		value  = getProperty(outputSignalSuffixKey, "BLP");
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
	
	
	@Override
	public String getCode(Object context, Object step) {
		String code = FunctionFactory.getProperty(context, functionFileName, FUNCTION_CODE);
		
		String order = getProperty(orderKey, "");
		String cutOffFrequency = getProperty(cutOffFrequencyKey, "");
		String inputSignal = getProperty(inputSignalKey, "");
		String outputSignal = inputSignal + getProperty(outputSignalSuffixKey, "");
		code = code.replaceAll(orderKey, order).replaceAll(cutOffFrequencyKey, cutOffFrequency);
		code = code.replaceAll(inputSignalKey, inputSignal).replaceAll(outputSignalSuffixKey, outputSignal);
		
		return code + "\n";
	}
	
	@Override
	public Block clone() {
		ButterworthLowPass function = new ButterworthLowPass();
		super.clone(function);
		return function;
	}

}
