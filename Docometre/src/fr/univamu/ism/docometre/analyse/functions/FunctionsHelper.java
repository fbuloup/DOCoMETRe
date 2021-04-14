package fr.univamu.ism.docometre.analyse.functions;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.editors.ChannelsContentProvider;
import fr.univamu.ism.docometre.preferences.MathEnginePreferencesConstants;

public final class FunctionsHelper {
	
	public static String createTrialsListHelper(String trialsList) {
		String mathEngine = Activator.getDefault().getPreferenceStore().getString(MathEnginePreferencesConstants.MATH_ENGINE);
		trialsList = trialsList.replaceAll(";", ",");
		trialsList = trialsList.replaceAll("-", ":");
		if(MathEnginePreferencesConstants.MATH_ENGINE_PYTHON.equals(mathEngine)) {
			Pattern pattern = Pattern.compile("^(?!([ \\d]*-){2})\\d+(?: *[-,:;] *\\d+)*$");
			Matcher matcher = pattern.matcher(trialsList);
	        if(matcher.matches()) {
	        	String[] rangesString = trialsList.split(",");
	        	for (int i = 0; i < rangesString.length; i++) {
					if(rangesString[i].contains(":")) {
						String[] numbers = rangesString[i].split(":");
						numbers[0] = String.valueOf(Integer.parseInt(numbers[0]) - 1);
						rangesString[i] = numbers[0] + ":" + numbers[1];
					} else {
						rangesString[i] = String.valueOf(Integer.parseInt(rangesString[i]) - 1);
					}
				}
	        	
	        	trialsList = String.join(",", rangesString);
	        	return trialsList;
	        }
	        pattern = Pattern.compile("^\\d+$");
			matcher = pattern.matcher(trialsList);
			if(matcher.matches()) {
				return String.valueOf(Integer.valueOf(trialsList) - 1);
			}
		}
		return trialsList;
	}
	
	public static LabelProvider createTextProvider() {
		LabelProvider textProvider = LabelProvider.createTextProvider(new Function<Object, String>() {
			@Override
			public String apply(Object t) {
				if(!(t instanceof Channel)) return null;
				Channel channel = (Channel)t;
				return channel.getFullName();
			}
		});
		return textProvider;
	}
	
	public static Button addChannelsSelectionButton(Composite parent, String text, ComboViewer comboViewer, ChannelsContentProvider channelsContentProvider) {
		Button button = new Button(parent, SWT.NORMAL);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(parent.getShell(), FunctionsHelper.createTextProvider());
				elementListSelectionDialog.setMultipleSelection(false);
				elementListSelectionDialog.setElements(channelsContentProvider.getElements(SelectedExprimentContributionItem.selectedExperiment));
				if(elementListSelectionDialog.open() == Dialog.OK) {
					StructuredSelection structuredSelection = new StructuredSelection(elementListSelectionDialog.getFirstResult());
					comboViewer.setSelection(structuredSelection);
				}
			}
		});
		return button;
	}
	
}
