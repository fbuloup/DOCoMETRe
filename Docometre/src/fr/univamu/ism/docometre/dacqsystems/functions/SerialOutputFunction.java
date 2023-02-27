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

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinCodeSegmentProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinRS232Module;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinVariable;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinVariableProperties;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.ScriptSegmentType;

public class SerialOutputFunction extends GenericFunction {
	
	public static final String functionFileName = "SERIAL_OUTPUT.FUN";
	
	private static final long serialVersionUID = 1L;
	
	private static final String moduleNumberKey = "moduleNumber";
	private static final String portNumberKey = "portNumber";
	private static final String asciiStringValueKey = "asciiStringValue";
	private static final String addCRKey = "add_CR";
	private static final String addLFKey = "add_LF";
	
	@Override
	public String getFunctionFileName() {
		return functionFileName;
	}
	
	@Override
	public Object getGUI(Object titleAreaDialog, Object parent, Object context) {
		if(!(context instanceof Process)) return null;
		
		Process process = (Process) context;
		DACQConfiguration dacqConfiguration = (DACQConfiguration) process.getDACQConfiguration();
		
		boolean rs232ModuleFound = false;
		Module[] modules = dacqConfiguration.getModules();
		for (Module module : modules) {
			if(module instanceof ADWinRS232Module) {
				rs232ModuleFound = true;
				break;
			}
		}
		
		if(!rs232ModuleFound) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "" , "");
			return null;
		}
		
		Composite container  = (Composite) parent;
		Composite paramContainer = new Composite(container, SWT.NORMAL);
		paramContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		paramContainer.setLayout(new GridLayout(2, false));

		if(process instanceof ADWinProcess) {
			if(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE).equals(ADWinDACQConfigurationProperties.PRO)) {
				Label moduleNumberLabel = new Label(paramContainer, SWT.NORMAL);
				moduleNumberLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
				moduleNumberLabel.setText(DocometreMessages.ModuleNumberLabel);
				
				ComboViewer moduleNumberComboViewer = new ComboViewer(paramContainer);
				moduleNumberComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				for (Module module : modules) {
					if(module instanceof ADWinRS232Module) {
						moduleNumberComboViewer.getCombo().add(module.getProperty(ADWinModuleProperties.MODULE_NUMBER));
					}
				}
				moduleNumberComboViewer.getCombo().addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if(moduleNumberComboViewer.getCombo().getSelectionIndex() > -1) 
							getTransientProperties().put(moduleNumberKey, moduleNumberComboViewer.getCombo().getText());
					}
				});
				String value  = getProperty(moduleNumberKey, "");
				int index = moduleNumberComboViewer.getCombo().indexOf(value);
				moduleNumberComboViewer.getCombo().select(index);
			}
		}
		
		
		Label portNumberLabel = new Label(paramContainer, SWT.NORMAL);
		portNumberLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		portNumberLabel.setText(DocometreMessages.PortNumberLabel);
		
		ComboViewer portNumberComboViewer = new ComboViewer(paramContainer);
		portNumberComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		portNumberComboViewer.getCombo().add("1");
		portNumberComboViewer.getCombo().add("2");
		portNumberComboViewer.getCombo().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(portNumberComboViewer.getCombo().getSelectionIndex() > -1) 
					getTransientProperties().put(portNumberKey, portNumberComboViewer.getCombo().getText());
			}
		});
		String value  = getProperty(portNumberKey, "");
		int index = portNumberComboViewer.getCombo().indexOf(value);
		portNumberComboViewer.getCombo().select(index);
		
		Label stringValueLabel = new Label(paramContainer, SWT.NORMAL);
		stringValueLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		stringValueLabel.setText(DocometreMessages.StringValueLabel);
		
		Text stringValueText = new Text(paramContainer, SWT.BORDER);
		stringValueText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		stringValueText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(asciiStringValueKey, stringValueText.getText());
			}
		});
		value  = getProperty(asciiStringValueKey, "");
		stringValueText.setText(value);
		
		
		ControlDecoration expressionCD = new ControlDecoration(stringValueText, SWT.TOP | SWT.LEFT);
		expressionCD.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
		expressionCD.setDescriptionText(DocometreMessages.UseCtrlSpaceProposal);
		expressionCD.setShowOnlyOnFocus(true);
		expressionCD.setMarginWidth(5);
		try {
			Process adwinProcess = (Process) context;
			DACQConfiguration adwinDacqConfiguration = adwinProcess.getDACQConfiguration();
			KeyStroke keyStroke = KeyStroke.getInstance("CTRL+SPACE");
			DocometreContentProposalProvider proposalProvider = new DocometreContentProposalProvider(adwinDacqConfiguration.getProposal(), stringValueText);
			proposalProvider.setFiltering(true);
			
			ContentProposalAdapter leftProposalAdapter = new ContentProposalAdapter(stringValueText, new TextContentAdapter(), proposalProvider, keyStroke, null);
			leftProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
			leftProposalAdapter.addContentProposalListener(proposalProvider);
		} catch (ParseException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		
		
		Label dummyLabel1 = new Label(paramContainer, SWT.NONE);
		dummyLabel1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		Button crButton = new Button(paramContainer, SWT.CHECK);
		crButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		crButton.setText(DocometreMessages.CRValueLabel); 
		boolean addCR  = Boolean.parseBoolean(getProperty(addCRKey, "false"));
		crButton.setSelection(addCR);
		crButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getTransientProperties().put(addCRKey, String.valueOf(crButton.getSelection()));
			}
		});
		
		Label dummyLabel2 = new Label(paramContainer, SWT.NONE);
		dummyLabel2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		Button lfButton = new Button(paramContainer, SWT.CHECK);
		lfButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		lfButton.setText(DocometreMessages.LFValueLabel); 
		boolean addLF  = Boolean.parseBoolean(getProperty(addLFKey, "false"));
		lfButton.setSelection(addLF);
		lfButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getTransientProperties().put(addLFKey, String.valueOf(lfButton.getSelection()));
			}
		});

		addCommentField(paramContainer, 1, context);
		
		return paramContainer;
	}
	
	
	
	@Override
	public String getCode(Object context, Object step, Object...objects) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
		String code = "";
		Process process = (Process) context;
		DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
		if(process instanceof ADWinProcess) {
			if(step == ADWinCodeSegmentProperties.DECLARATION) {
				code = code + "\nREM Serial output function declaration\n";
				String key = ADWinCodeSegmentProperties.DECLARATION.toString();
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, key.toUpperCase());
				String hashCode = String.valueOf(hashCode());
				temporaryCode = temporaryCode.replaceAll("HashCode", hashCode);
				String value = getProperty(asciiStringValueKey, "");
				if(!value.startsWith("\"")) {
					Channel[] variables = process.getDACQConfiguration().getVariables();
					ADWinVariable adWinVariable = null;
					boolean found = false;
					for (Channel variable : variables) {
						adWinVariable = (ADWinVariable)variable;
						String channelName = adWinVariable.getProperty(ChannelProperties.NAME);
						if(value.equals(channelName)) {
							found = true;
							break;
						}
					}
					if(found) {
						String size = adWinVariable.getProperty(ADWinVariableProperties.SIZE);
						code = code + temporaryCode.replaceAll("stringSize", size);
					} else Activator.logErrorMessage("Channel " + value + " not found !");
				} else code = code + temporaryCode.replaceAll("stringSize", String.valueOf(value.length()-2));
			}
			if(step == ADWinCodeSegmentProperties.INITIALIZATION) {
				code = code + "\nREM Serial output function initialization\n";
				String key = ADWinCodeSegmentProperties.INITIALIZATION.toString();
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, key.toUpperCase());
				String hashCode = String.valueOf(hashCode());
				temporaryCode = temporaryCode.replaceAll("HashCode", hashCode);
				boolean addCR = Boolean.parseBoolean(getProperty(addCRKey, "false"));
				code = code + temporaryCode.replaceAll(addCRKey, addCR?"1":"0");
				boolean addLF = Boolean.parseBoolean(getProperty(addLFKey, "false"));
				code = code.replaceAll(addLFKey, addLF?"1":"0");
			}
			if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
				// Récupérer la bonne propriété dans le fichier functionFileName en fonction du bon device : Gold ou Pro ET du bon CPU : I ou II
				// FUNCTION_CODE_GOLD_I ou FUNCTION_CODE_GOLD_II ou FUNCTION_CODE_PRO_I ou FUNCTION_CODE_PRO_II
				code = code + "\nREM Serial Output Function\n\n";
				String systemType = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
				String cpuType = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
				String key = FUNCTION_CODE + "_" + systemType + "_" + cpuType;
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, key.toUpperCase());
				String moduleNumber = getProperty(moduleNumberKey, "");
				String portNumber = getProperty(portNumberKey, "");
				String asciiStringValue = getProperty(asciiStringValueKey, "");
				String hashCode = String.valueOf(hashCode());
				temporaryCode = temporaryCode.replaceAll(moduleNumberKey, moduleNumber).replaceAll(portNumberKey, portNumber).replaceAll("HashCode", hashCode);
				temporaryCode = temporaryCode.replaceAll(asciiStringValueKey, asciiStringValue);
				code = code + temporaryCode + "\n\n";
			}
		}
		return code;
	}
	
	@Override
	public Block clone() {
		SerialOutputFunction function = new SerialOutputFunction();
		super.clone(function);
		return function;
	}

}
