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

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dialogs.DialogSelectionHandler;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Operator;
import fr.univamu.ism.process.ScriptSegmentType;

public class StimulusFunction extends GenericFunction {

	public static final String functionFileName = "STIMULUS.FUN";
	
	private static final long serialVersionUID = 1L;
	
	public static final String absolutePathToFileKey = "absolutePathToFile";
	public static final String outputKey = "output";
	public static final String frequencyRatioKey = "frequencyRatio";
	public static final String transferNumberKey = "transferNumber";
	
	private transient TitleAreaDialog titleAreaDialog;
	
	@Override
	public String getFunctionFileName() {
		return functionFileName;
	}
	
	@Override
	public Object getGUI(Object titleAreaDialog, Object parent, Object context) {
		if(!(context instanceof Process)) return null;
		
		this.titleAreaDialog = (TitleAreaDialog) titleAreaDialog;
		Process process = (Process) context;
		DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
		
		Composite container  = (Composite) parent;
		Composite paramContainer = new Composite(container, SWT.NORMAL);
		paramContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		paramContainer.setLayout(new GridLayout(3, false));
		
		Label fileLabel = new Label(paramContainer, SWT.NORMAL);
		fileLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		fileLabel.setText(DocometreMessages.PathToFileLabel);
		fileLabel.setToolTipText(DocometreMessages.PathToFileTooltip);
		
		Text pathText = new Text(paramContainer, SWT.READ_ONLY | SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		pathText.setText(getProperty(absolutePathToFileKey, ""));
		pathText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(absolutePathToFileKey, pathText.getText());
			}
		});
		
		Button browseButton = new Button(paramContainer, SWT.FLAT);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		browseButton.setText(DocometreMessages.Browse);
		browseButton.addSelectionListener(new DialogSelectionHandler(pathText, false, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()));
		
		Label channelLabel = new Label(paramContainer, SWT.NORMAL);
		channelLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		channelLabel.setText(DocometreMessages.ChannelName_Label);
		
		ComboViewer channelComboViewer = new ComboViewer(paramContainer, SWT.BORDER | SWT.READ_ONLY);
		channelComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		channelComboViewer.getCombo().addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				StimulusFunction.this.titleAreaDialog.setErrorMessage(null);
				String[] input = (String[])channelComboViewer.getInput();
				if(input == null) return;
				if(!Arrays.asList(input).contains(channelComboViewer.getCombo().getText())) {
					Pattern pattern = Pattern.compile("[-]?\\d+(\\.\\d+)?");
					Matcher matcher = pattern.matcher(channelComboViewer.getCombo().getText());
					if(!matcher.matches()) {
						String message = NLS.bind(DocometreMessages.FloatValueNotValidLabel, channelComboViewer.getCombo().getText());
						StimulusFunction.this.titleAreaDialog.setErrorMessage(message);
					}
				}
			}
		});
		channelComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		channelComboViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if(element instanceof Operator) return ((Operator)element).getValue();
				return super.getText(element);
			}
		});
		if(dacqConfiguration instanceof ADWinDACQConfiguration) channelComboViewer.setInput(dacqConfiguration.getOutputsProposal());
		channelComboViewer.getCombo().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(outputKey, channelComboViewer.getCombo().getText());
			}
		});
		String value  = getProperty(outputKey, "");
		channelComboViewer.getCombo().setText(value);
		
		addCommentField(paramContainer, 2, context);
		
		return paramContainer;
	}
	
	
	
	@Override
	public String getCode(Object context, Object step) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
		String code = "";
		Process process = (Process) context;
		
		if(process instanceof ADWinProcess) {
			if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
				
				String output = getProperty(outputKey, "");
				String transferNumber = "0";
				DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
				float globalFrequency = Float.parseFloat(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY));
				float frequency = globalFrequency;
				Channel[] channels = dacqConfiguration.getChannels();
				for (Channel channel : channels) {
					if(channel.getProperty(ChannelProperties.NAME).equals(output)) {
						frequency = Float.parseFloat(channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY));
						transferNumber = channel.getProperty(ChannelProperties.TRANSFER_NUMBER);
					}
				}
				int frequencyRatio = (int)(globalFrequency / frequency);
				String absolutePathToFile = getProperty(absolutePathToFileKey, "");
				
				code = code + "\nREM Stimulus Function\n\n";
				String key = FUNCTION_CODE;
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, key.toUpperCase());
				code = temporaryCode.replaceAll(outputKey, output).replaceAll(absolutePathToFileKey, absolutePathToFile);
				code = code.replaceAll(frequencyRatioKey, String.valueOf(frequencyRatio));
				code = code.replaceAll(transferNumberKey, transferNumber);
				code = code + "\n\n";
			}
		}
		return code;
	}
	
	@Override
	public Block clone() {
		StimulusFunction function = new StimulusFunction();
		super.clone(function);
		return function;
	}
	
}
