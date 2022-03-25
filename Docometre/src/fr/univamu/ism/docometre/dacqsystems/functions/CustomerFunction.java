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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

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
import fr.univamu.ism.process.Block;
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
		if(!(context instanceof Process || context instanceof Script)) return null;
		//Process process = (Process)context;
		
		Composite container  = (Composite) parent;
		Composite paramContainer = new Composite(container, SWT.BORDER);
		paramContainer.setLayout(new GridLayout(2, false));
		GridLayout gl = (GridLayout) paramContainer.getLayout();
		gl.horizontalSpacing = 15;
		
		try {
			String nbParamsString =  FunctionFactory.getProperty(context, functionFileName, PARAMETERS_NUMBER_KEY, true);
			int nbParams = Integer.valueOf(nbParamsString);
			for (int i = 1; i <= nbParams; i++) {
				String parameterDefinition = FunctionFactory.getProperty(context, functionFileName, PARAMETER_KEY + String.valueOf(i), true);
				if(!parameterDefinition.equals("")) {
					Properties properties = new Properties();
					String[] parameterSegments = parameterDefinition.split(",");
					for (int j = 0; j < parameterSegments.length; j++) {
						String segment = parameterSegments[j];
						String[] keyValue = segment.split("=");
						properties.put(keyValue[0].trim(), keyValue[1].trim());
					}
					// Create parameter
					createParameterWidget(paramContainer, properties, context);
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
	
	private void createTextWidget(String type, String regExp, Composite paramContainer, Label parameterLabel, String key, Object context) {
		String initialValue = "";
		initialValue = type.replaceAll("TEXT", "");
		initialValue = initialValue.replaceAll("\\[", "");
		initialValue = initialValue.replaceAll("\\]", "");
		initialValue = initialValue.strip();
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
		if(context instanceof Process) {
			try {
				DACQConfiguration dacqConfiguration = ((Process)context).getDACQConfiguration();
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
		}
		
		if(!"".equals(regExp)) {
			text.setData(REGEXP, regExp);
			text.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					validateParametersInputs();
				}
			});
		}
	}
	
	private void createFileOrFolderWidget(String fileOrFolder, boolean editable, Composite paramContainer, Label parameterLabel, String key, Object context) {
		Composite innerContainer = new Composite(paramContainer, SWT.NORMAL);
		innerContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		innerContainer.setLayout(gl);
		
		Text text = new Text(innerContainer, SWT.BORDER);
		text.setEditable(editable);
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
		text.setText(getProperty(key, ""));
		
		Button browseButton = new Button(innerContainer, SWT.FLAT);
		browseButton.setText(DocometreMessages.Browse);
		browseButton.setData(fileOrFolder);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String fileOrFolder = (String) browseButton.getData();
				if("FILE".equals(fileOrFolder)) {
					FileDialog fileDialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
					String filePath = fileDialog.open();
					if(filePath != null) text.setText(filePath);
				}
				if("FOLDER".equals(fileOrFolder)) {
					DirectoryDialog directoryDialog = new DirectoryDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
					String directoryPath = directoryDialog.open();
					if(directoryPath != null) text.setText(directoryPath);
				}
			}
		});
	}
	
	private void createComboWidget(String type, String values, Composite paramContainer, Label parameterLabel, String key, Object context) {
		String initialValue = "";
		initialValue = type.replaceAll("COMBO", "");
		initialValue = initialValue.replaceAll("\\[", "");
		initialValue = initialValue.replaceAll("\\]", "");
		initialValue = initialValue.strip();
		initialValue = initialValue.replaceAll("^\"", "");
		initialValue = initialValue.replaceAll("\"$", "");
		initialValue = getProperty(key, initialValue);
		String[] arrayValues = values.replaceAll("^\\(", "").replaceAll("\\)$", "").split(";");
		for (int i = 0; i < arrayValues.length; i++) {
			arrayValues[i] = arrayValues[i].strip();
			arrayValues[i] = arrayValues[i].replaceAll("^\"", "");
			arrayValues[i] = arrayValues[i].replaceAll("\"$", "");
		}
		Combo combo = new Combo(paramContainer, SWT.BORDER | SWT.READ_ONLY);
		combo.setData(LABEL, parameterLabel);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		combo.setData(key);
		combo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				getTransientProperties().put((String)combo.getData(), combo.getText());
			}
		});
		combo.setItems(arrayValues);
		combo.select(combo.indexOf(initialValue));
	}

	private void createParameterWidget(Composite paramContainer, Properties properties, Object context) {
		String label = getProperty(properties, LABEL_KEY);
		label = label.replaceAll("^\"", "");
		label = label.replaceAll("\"$", "");
		String key = getProperty(properties, NAME_KEY);
		String type = getProperty(properties, TYPE_KEY);
		String[] typeRegExp = type.split(":");
		Label parameterLabel = new Label(paramContainer, SWT.NORMAL);
		parameterLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		parameterLabel.setText(label);
		
		if(typeRegExp[0] != null && typeRegExp[0].matches("^TEXT(\\[.*\\])?$"))
			createTextWidget(typeRegExp[0], typeRegExp.length>1?typeRegExp[1]:"", paramContainer, parameterLabel, key, context);
		
		if(typeRegExp[0] != null && typeRegExp[0].matches("^FILE$")) {
			boolean editable = false;
			if(typeRegExp.length > 1) editable = "EDITABLE".equalsIgnoreCase(typeRegExp[1]);
			createFileOrFolderWidget("FILE", editable, paramContainer, parameterLabel, key, context);
		}
			
		
		if(typeRegExp[0] != null && typeRegExp[0].matches("^FOLDER$")) {
			boolean editable = false;
			if(typeRegExp.length > 1) editable = "EDITABLE".equalsIgnoreCase(typeRegExp[1]);
			createFileOrFolderWidget("FOLDER", editable, paramContainer, parameterLabel, key, context);
		}
		
		if(typeRegExp[0] != null && typeRegExp[0].matches("^COMBO(\\[.+\\]){1}$"))
			if(typeRegExp[1] != null && typeRegExp[1].matches("^\\(.+\\)$"))
			createComboWidget(typeRegExp[0], typeRegExp[1], paramContainer, parameterLabel, key, context);
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
	
	private String parseCode(Object context, String keyCode, String code) {
		String temporaryCode = FunctionFactory.getProperty(context, functionFileName, keyCode.toUpperCase(), true);
		if(temporaryCode != null && !"".equals(temporaryCode)) {
			String comment = "\n";
			if(context instanceof Process) {
				DACQConfiguration dacqConfiguration = ((Process)context).getDACQConfiguration();
				if(dacqConfiguration instanceof ADWinDACQConfiguration) comment += "REM";
				if(dacqConfiguration instanceof ArduinoUnoDACQConfiguration) comment += "//";
			}
			if(context instanceof Script) { 
				if(MathEngineFactory.isMatlab()) comment += "%";
				if(MathEngineFactory.isPython()) comment += "#";
			}
			temporaryCode = comment + " Customer Function : " + getName(context) + "\n" + temporaryCode;
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
		if(context instanceof Process) {
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
				code = parseCode(process, key, code);
				// Get any source code on specific key 
				key = key + "_" + systemType + "_" + cpuType;
				code = parseCode(process, key.toUpperCase(), code);
				
			}
			if(process instanceof ArduinoUnoProcess) {
				// Get any source code on generic key
				String key = step.toString().toUpperCase();
				if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
					key = FUNCTION_CODE;
				} 
				code = parseCode(process, key.toUpperCase(), code);
			}
		}
		if(context instanceof Script) {
			code = parseCode(context, FUNCTION_CODE.toUpperCase(), code);
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
	
	
	@Override
	public Block clone() {
		CustomerFunction function = new CustomerFunction();
		function.functionFileName = functionFileName;
		clone(function);
		return function;
	}
	
	
}
