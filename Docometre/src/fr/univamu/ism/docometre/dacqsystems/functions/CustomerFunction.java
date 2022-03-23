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
package fr.univamu.ism.docometre.dacqsystems.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoProcess;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegmentType;

public class CustomerFunction extends GenericFunction {
	
	private static final long serialVersionUID = 1L;
	
	private static final String REGEXP = "REGEXP";
	private static final String LABEL = "LABEL";
	private static final String CONTROL_DECORATION = "CONTROL_DECORATION";
	
	private static final String countrySuffix = "_" + Locale.getDefault().getCountry();
	private static final String PARAMETERS_NUMBER_KEY = "PARAMETERS_NUMBER";
	private static final String PARAMETER_KEY = "PARAMETER_";
	private static final String NAME_KEY = "NAME";
	private static final String TYPE_KEY = "TYPE";
	private static final String LABEL_KEY = "LABEL";
	
	transient private TitleAreaDialog titleAreaDialog;
	transient private ArrayList<Text> textParametersArray;
	
	private String functionFileName;
	
	private String getProperty(Properties properties, String key) {
		if(properties.containsKey(key + countrySuffix)) return properties.getProperty(key + countrySuffix);
		else return properties.getProperty(key);
	}
	
	public void setFunctionFileName(String functionFileName) {
		this.functionFileName = functionFileName;
	}
	
	@Override
	public String getFunctionFileName() {
		return functionFileName;
	}
	
	@Override
	public Object getGUI(Object titleAreaDialog, Object parent, Object context) {
		this.titleAreaDialog = (TitleAreaDialog) titleAreaDialog;
		textParametersArray = new ArrayList<Text>(0);
		if(!(context instanceof Process)) return null;
		Process process = (Process)context;
		
		Composite container  = (Composite) parent;
		Composite paramContainer = new Composite(container, SWT.BORDER);
		paramContainer.setLayout(new GridLayout(2, false));
		GridLayout gl = (GridLayout) paramContainer.getLayout();
		gl.horizontalSpacing = 15;
		
		try {
			String nbParamsString =  FunctionFactory.getProperty(process, functionFileName, PARAMETERS_NUMBER_KEY, true);
			int nbParams = Integer.valueOf(nbParamsString);
			for (int i = 1; i <= nbParams; i++) {
				String parameterDefinition = FunctionFactory.getProperty(process, functionFileName, PARAMETER_KEY + String.valueOf(i), true);
				if(!parameterDefinition.equals("")) {
					Properties properties = new Properties();
					String[] parameterSegments = parameterDefinition.split(",");
					for (int j = 0; j < parameterSegments.length; j++) {
						String segment = parameterSegments[j];
						String[] keyValue = segment.split("=");
						properties.put(keyValue[0].trim(), keyValue[1].trim());
					}
					// Create parameter
					createParamterWidget(paramContainer, properties, process);
				}
			}
		} catch (NumberFormatException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		
		addCommentField(paramContainer, 1, context);
		
		this.titleAreaDialog.getShell().addShellListener(new ShellAdapter() {
			@Override
			public void shellActivated(ShellEvent e) {
				validateParametersInputs();
			}
		});
		
		
		
		return paramContainer;
	}

	private void createParamterWidget(Composite paramContainer, Properties properties, Process process) {
		String label = getProperty(properties, LABEL_KEY);
		label = label.replaceAll("^\"", "");
		label = label.replaceAll("\"$", "");
		String key = getProperty(properties, NAME_KEY);
		String type = getProperty(properties, TYPE_KEY);
		String[] typeRegExp = type.split(":");
		Label parameterLabel = new Label(paramContainer, SWT.NORMAL);
		parameterLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		parameterLabel.setText(label);
		
		if(typeRegExp[0] != null && typeRegExp[0].matches("^TEXT(\\[.*\\])?$")) {
			String initialValue = "";
			initialValue = typeRegExp[0].replaceAll("TEXT", "");
			initialValue = initialValue.replaceAll("\\[", "");
			initialValue = initialValue.replaceAll("\\]", "");
			initialValue = initialValue.replaceAll("^\"", "");
			initialValue = initialValue.replaceAll("\"$", "");
			Text text = new Text(paramContainer, SWT.BORDER);
			text.setData(LABEL, parameterLabel);
			textParametersArray.add(text);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			text.setData(key);
			text.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					getTransientProperties().put((String)text.getData(), text.getText());
				}
			});
			text.setText(getProperty(key, initialValue));
			ControlDecoration textCD = new ControlDecoration(text, SWT.TOP | SWT.LEFT);
			textCD.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
			textCD.setDescriptionText(DocometreMessages.UseCtrlSpaceProposal);
			textCD.setShowOnlyOnFocus(true);
			textCD.setMarginWidth(5);
			try {
				DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
				KeyStroke keyStroke = KeyStroke.getInstance("CTRL+SPACE");
				DocometreContentProposalProvider proposalProvider = new DocometreContentProposalProvider(dacqConfiguration.getProposal(), text);
				proposalProvider.setFiltering(true);
				ContentProposalAdapter leftProposalAdapter = new ContentProposalAdapter(text, new TextContentAdapter(), proposalProvider, keyStroke, null);
				leftProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
				leftProposalAdapter.addContentProposalListener(proposalProvider);
			} catch (ParseException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
			if(typeRegExp.length > 1 ) {
				text.setData(REGEXP, typeRegExp[1]);
				text.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent event) {
						validateParametersInputs();
					}
				});
			}
		}
		
	}
	
	private void validateParametersInputs() {
		titleAreaDialog.setErrorMessage(null);
		for (Text textParameter : textParametersArray) {
			Object regExp = textParameter.getData("REGEXP");
			Object textControlDecoration = textParameter.getData(CONTROL_DECORATION);
			if(textControlDecoration != null) {
				((ControlDecoration)textControlDecoration).hide();
				((ControlDecoration)textControlDecoration).dispose();
				textParameter.setData(CONTROL_DECORATION, null);
			}
			if(regExp != null) {
				Pattern pattern = Pattern.compile((String)regExp);
				Matcher matcher = pattern.matcher(textParameter.getText());
				if(!matcher.matches()) {
					String errorMessage = NLS.bind(DocometreMessages.IsNotAValidValue, textParameter.getText());
					if(titleAreaDialog.getErrorMessage() == null) {
						Label label = (Label) textParameter.getData("LABEL");
						String titleAreaDialogErrorMessage = label.getText() + " " + errorMessage;
						titleAreaDialog.setErrorMessage(titleAreaDialogErrorMessage);
					}
					ControlDecoration textCD = new ControlDecoration(textParameter, SWT.BOTTOM | SWT.LEFT);
					textCD.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
					textCD.setDescriptionText(errorMessage);
					textCD.setMarginWidth(5);
					textParameter.setData(CONTROL_DECORATION, textCD);
				} 
			}
		}
	}
	
	private String parseCode(Process process, String keyCode, String code, Object context) {
		String temporaryCode = FunctionFactory.getProperty(process, functionFileName, keyCode.toUpperCase(), true);
		if(temporaryCode != null && !"".equals(temporaryCode)) {
			String comment = "\n";
			if(context instanceof Process) {
				DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
				if(dacqConfiguration instanceof ADWinDACQConfiguration) comment += "REM";
				if(dacqConfiguration instanceof ArduinoUnoDACQConfiguration) comment += "//";
			}
			if(context instanceof Script) { 
				if(MathEngineFactory.isMatlab()) comment += "%";
				if(MathEngineFactory.isPython()) comment += "#";
			}
			temporaryCode = comment + " Customer Function : " + getName(context) + "\n\n" + temporaryCode;
			HashMap<String, String> properties = getProperties();
			Set<String> propertiesKeys = properties.keySet();
			String hashCode = String.valueOf(hashCode());
			temporaryCode = temporaryCode.replaceAll("HashCode", hashCode);
			for (Iterator<String> propertiesKeysIterator = propertiesKeys.iterator(); propertiesKeysIterator.hasNext();) {
				String propertyKey = propertiesKeysIterator.next();
				String propertyKeyRegExp = "\\b" + propertyKey + "\\b";
				String propertyValue = properties.get(propertyKey);
				temporaryCode = temporaryCode.replaceAll(propertyKeyRegExp, propertyValue);
			}
		}
		return (temporaryCode != null && !"".equals(temporaryCode)) ? code + temporaryCode + "\n\n" : code;
	}
	
	@Override
	public String getCode(Object context, Object step) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
		String code = "";
		Process process = (Process) context;
		DACQConfiguration dacqConfiguration =  process.getDACQConfiguration();
		if(process instanceof ADWinProcess) {
			String systemType = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
			String cpuType = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
			String key = step.toString().toUpperCase();
			if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
				key = FUNCTION_CODE;
			} 
			// Get any source code on generic key
			code = parseCode(process, key, code, context);
			// Get any source code on specific key 
			key = key + "_" + systemType + "_" + cpuType;
			code = parseCode(process, key.toUpperCase(), code, context);
			
		}
		if(process instanceof ArduinoUnoProcess) {
			// Get any source code on generic key
			String key = step.toString().toUpperCase();
			if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
				key = FUNCTION_CODE;
			} 
			code = parseCode(process, key.toUpperCase(), code, context);
		}
		return code;
	}
	
	@Override
	public String getTitle(Object process) {
		if(!(process instanceof Process || process instanceof Script)) return "";
		return FunctionFactory.getProperty(process, getFunctionFileName(), FunctionFactory.MENU_TITLE, true);
	}
	
	@Override
	public String getDescription(Object process) {
		if(!(process instanceof Process || process instanceof Script)) return "";
		return FunctionFactory.getProperty(process, getFunctionFileName(), FunctionFactory.DESCRIPTION, true);
	}
	

}
