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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Module;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinCodeSegmentProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinModuleProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinRS232Module;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoProcess;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Operator;
import fr.univamu.ism.process.ScriptSegmentType;

public class JVLMAC141ReadRegisterValueFunction extends GenericFunction {
	
	public static final String functionFileName = "JVL_MAC141_READ_REGISTER_VALUE.FUN";
	
	private static final long serialVersionUID = 1L;
	
	private static final String moduleNumberKey = "moduleNumber";
	private static final String portNumberKey = "portNumber";
	private static final String registerTypeKey = "registerType";
	private static final String channelNameKey = "channelName";
	
	@Override
	public String getFunctionFileName() {
		return functionFileName;
	}
	
	@Override
	public Object getGUI(Object titleAreaDialog, Object parent, Object context) {
		if(!(context instanceof Process)) return null;
		
		Process process = (Process) context;
		DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
		
		boolean rs232ModuleFound = false;
		Module[] modules = dacqConfiguration.getModules();
		for (Module module : modules) {
			if(module instanceof ADWinRS232Module) {
				rs232ModuleFound = true;
				break;
			}
		}
		
		if(!rs232ModuleFound) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), DocometreMessages.SerialPortDialogTitle, DocometreMessages.NoSerialPortDefinedLabel);
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
		
		Label registerTypeLabel = new Label(paramContainer, SWT.NORMAL);
		registerTypeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		registerTypeLabel.setText(DocometreMessages.RegisterTypeLabel);
		
		ComboViewer registerTypeComboViewer = new ComboViewer(paramContainer);
		registerTypeComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		registerTypeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		registerTypeComboViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if(element instanceof Operator) return ((Operator)element).getValue();
				return super.getText(element);
			}
		});
		if(dacqConfiguration instanceof ADWinDACQConfiguration) registerTypeComboViewer.setInput(JVLMAC141_REGISTERS.registerTypes);
		registerTypeComboViewer.getCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(registerTypeKey, registerTypeComboViewer.getCombo().getText());
			}
		});
		
		value  = getProperty(registerTypeKey, JVLMAC141_REGISTERS.registerTypes[0]);
		index = JVLMAC141_REGISTERS.findRegisterIndex(value);
		registerTypeComboViewer.getCombo().select(index);
		
		Label channelNameLabel = new Label(paramContainer, SWT.NORMAL);
		channelNameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		channelNameLabel.setText(DocometreMessages.RegisterValueLabel);
		
		Text channelNameText = new Text(paramContainer, SWT.BORDER);
		channelNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		channelNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(channelNameKey, channelNameText.getText());
//				validateButton.setEnabled(expressionText.getText().equals("")?false:true);
			}
		});
		channelNameText.setText(getProperty(channelNameKey, ""));
		ControlDecoration expressionCD = new ControlDecoration(channelNameText, SWT.TOP | SWT.LEFT);
		expressionCD.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
		expressionCD.setDescriptionText(DocometreMessages.UseCtrlSpaceProposal);
		expressionCD.setShowOnlyOnFocus(true);
		expressionCD.setMarginWidth(5);
		try {
			Process adwinProcess = (Process) context;
			DACQConfiguration adwinDacqConfiguration = adwinProcess.getDACQConfiguration();
			KeyStroke keyStroke = KeyStroke.getInstance("CTRL+SPACE");
			DocometreContentProposalProvider proposalProvider = new DocometreContentProposalProvider(adwinDacqConfiguration.getProposal(), channelNameText);
			proposalProvider.setFiltering(true);
			
			ContentProposalAdapter leftProposalAdapter = new ContentProposalAdapter(channelNameText, new TextContentAdapter(), proposalProvider, keyStroke, null);
			leftProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
			leftProposalAdapter.addContentProposalListener(proposalProvider);
		} catch (ParseException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		
		
		addCommentField(paramContainer, 1, context);
		
		return paramContainer;
		
	}
	
	@Override
	public String getCode(Object context, Object step) {
		// TODO Auto-generated method stub
		
		
		
		String code = "";
		Process process = (Process) context;
//		DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
		if(process instanceof ADWinProcess) {
			if(step == ADWinCodeSegmentProperties.DECLARATION) {
				code = code + "\nREM JVL Mac 141 Servo motor Read Register Value Function Declaration\n";
				String key = ADWinCodeSegmentProperties.DECLARATION.toString();
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, key.toUpperCase());
				String hashCode = String.valueOf(hashCode());
				code = code + temporaryCode.replaceAll("HashCode", hashCode);
			}
			if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
				code = code + "\nREM JVL Mac 141 Servo motor Read Register Value Function\n\n";
				ADWinDACQConfiguration dacqConfiguration = (ADWinDACQConfiguration) process.getDACQConfiguration();
				String systemType = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.SYSTEM_TYPE);
				String cpuType = dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.CPU_TYPE);
				String key = FUNCTION_CODE + "_" + systemType + "_" + cpuType;
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, key.toUpperCase());
				String moduleNumber = getProperty(moduleNumberKey, "1");
				String portNumber = getProperty(portNumberKey, "1");
				String registerType = getProperty(registerTypeKey, JVLMAC141_REGISTERS.registerTypes[0]);
				int index = JVLMAC141_REGISTERS.findRegisterIndex(registerType);
				registerType = JVLMAC141_REGISTERS.registerAdresses[index];
				String channelName = getProperty(channelNameKey, "");
				String hashCode = String.valueOf(hashCode());
				
				temporaryCode = temporaryCode.replaceAll(moduleNumberKey, moduleNumber).replaceAll(portNumberKey, portNumber).replaceAll("HashCode", hashCode);
				temporaryCode = temporaryCode.replaceAll(registerTypeKey, registerType).replaceAll(channelNameKey, channelName);
				code = code + temporaryCode + "\n\n";
				
			}
		}
		if(process instanceof ArduinoUnoProcess) {
			
			code = code + "\n// JVL Mac 141 Servo motor Read Register Value function NOT YET IMPLEMENTED FOR ARDUINO UNO\n";
			
		}
		return code;
	}
	
	@Override
	public Block clone() {
		JVLMAC141ReadRegisterValueFunction function = new JVLMAC141ReadRegisterValueFunction();
		super.clone(function);
		return function;
	}

}
