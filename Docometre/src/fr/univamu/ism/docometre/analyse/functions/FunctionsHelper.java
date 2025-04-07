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
