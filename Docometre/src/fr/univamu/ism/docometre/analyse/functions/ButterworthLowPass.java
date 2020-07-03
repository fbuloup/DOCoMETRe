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
		orderLabel.setText("Order");
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
		cutOffLabel.setText("Cutoff");
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
		
		addCommentField(paramContainer, 1, context);
		
		return paramContainer;
	}
	
	
	@Override
	public String getCode(Object context, Object step) {
		String code = FunctionFactory.getProperty(context, functionFileName, FUNCTION_CODE);
		
		String order = getProperty(orderKey, "");
		String cutOffFrequency = getProperty(cutOffFrequencyKey, "");
		code = code.replaceAll(orderKey, order).replaceAll(cutOffFrequencyKey, cutOffFrequency);
		
		return code + "\n";
	}
	
	@Override
	public Block clone() {
		ButterworthLowPass function = new ButterworthLowPass();
		super.clone(function);
		return function;
	}

}
