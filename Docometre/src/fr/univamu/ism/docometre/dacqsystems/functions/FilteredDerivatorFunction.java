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
import java.util.Arrays;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinCodeSegmentProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.ScriptSegmentType;

public class FilteredDerivatorFunction extends GenericFunction {
	
	public static final String functionFileName = "FILTERED_DERIVATOR.FUN";
	
	private static final long serialVersionUID = 1L;

	private static final String inputChannelNameKey = "inputChannelName";
	private static final String outputChannelNameKey = "outputChannelName";
	private static final String aValueKey = "aValue";
	private static final String fcValueKey = "fcValue";
	private static final String feValueKey = "feValue";
	private static final String sampleFrequencyRatioKey = "sampleFrequencyRatio";
	
	@Override
	public String getFunctionFileName() {
		return functionFileName;
	}
	
	@Override
	public Object getGUI(Object titleAreaDialog, Object parent, Object context) {
		if (!(context instanceof Process)) return null;
		Composite container = (Composite) parent;
		Composite paramContainer = new Composite(container, SWT.NORMAL);
		paramContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		paramContainer.setLayout(new GridLayout(2, false));

		Canvas canvas = new Canvas(paramContainer, SWT.NONE);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		((GridData)canvas.getLayoutData()).heightHint = 50;
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				System.out.println("Paint event");
				Image image = Activator.getImage(IImageKeys.FILTERED_DERIVATOR_WIZBAN);
				double w1 = image.getBounds().width;
				double h1 = image.getBounds().height;
				double w2 = canvas.getBounds().width;
				double h2 = canvas.getBounds().height;
				double scale = Math.min(w2/w1, h2/h1);
				double newWidth = (int) (image.getBounds().width*scale);
				double newHeight = (int) (image.getBounds().height*scale);
				double withPosition = (int) (w2/2 - newWidth/2);
				double heightPosition = (int) (h2/2 - newHeight/2);
				e.gc.setAntialias(SWT.ON);
				e.gc.setInterpolation(SWT.HIGH);
				e.gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, (int)withPosition, (int)heightPosition, (int)newWidth, (int)newHeight);
				image.dispose(); 
			}
		});
		
		Label inputLabel = new Label(paramContainer, SWT.NONE);
		inputLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		inputLabel.setText(DocometreMessages.InputLabel);
		ComboViewer inputChannelComboViewer = new ComboViewer(paramContainer);
		inputChannelComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		inputChannelComboViewer.setContentProvider(new ArrayContentProvider());
		inputChannelComboViewer.setLabelProvider(new LabelProvider());
		inputChannelComboViewer.setComparator(new ViewerComparator());
		ArrayList<Channel> channels = new ArrayList<>();
		Process process = (Process) context;
		DACQConfiguration dacqConfiguration = (DACQConfiguration) process.getDACQConfiguration();
		channels.addAll(Arrays.asList(dacqConfiguration.getChannels()));
		inputChannelComboViewer.setInput(channels);
		inputChannelComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if(inputChannelComboViewer.getCombo().getSelectionIndex() > -1) 
					getTransientProperties().put(inputChannelNameKey, inputChannelComboViewer.getCombo().getText());
			}
		});
		String value  = getProperty(inputChannelNameKey, "");
		inputChannelComboViewer.getCombo().select(inputChannelComboViewer.getCombo().indexOf(value));
		
		Label outputLabel = new Label(paramContainer, SWT.NONE);
		outputLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		outputLabel.setText(DocometreMessages.OutputLabel);
		ComboViewer outputChannelComboViewer = new ComboViewer(paramContainer);
		outputChannelComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		outputChannelComboViewer.setContentProvider(new ArrayContentProvider());
		outputChannelComboViewer.setLabelProvider(new LabelProvider());
		outputChannelComboViewer.setComparator(new ViewerComparator());
		outputChannelComboViewer.setInput(channels);
		outputChannelComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if(outputChannelComboViewer.getCombo().getSelectionIndex() > -1) 
					getTransientProperties().put(outputChannelNameKey, outputChannelComboViewer.getCombo().getText());
					int sf = 0;
					if(dacqConfiguration instanceof ADWinDACQConfiguration) sf = Integer.parseInt(dacqConfiguration.getProperty(ADWinDACQConfigurationProperties.GLOBAL_FREQUENCY));
					if(dacqConfiguration instanceof ArduinoUnoDACQConfiguration) sf = Integer.parseInt(dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.GLOBAL_FREQUENCY));
					Channel channel = (Channel) outputChannelComboViewer.getStructuredSelection().getFirstElement();
					int ratio = (int)( sf / Double.parseDouble(channel.getProperty(ChannelProperties.SAMPLE_FREQUENCY)));
					getTransientProperties().put(sampleFrequencyRatioKey, Integer.toString(ratio));
			}
		});
		value  = getProperty(outputChannelNameKey, "");
		outputChannelComboViewer.getCombo().select(outputChannelComboViewer.getCombo().indexOf(value));
		
		Label aLabel = new Label(paramContainer, SWT.NONE);
		aLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		aLabel.setText(DocometreMessages.aValueLabel);
		Text aText = new Text(paramContainer, SWT.BORDER);
		aText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		aText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(aValueKey, aText.getText());
			}
		});
		value  = getProperty(aValueKey, "0");
		aText.setText(value);
		
		Label fcLabel = new Label(paramContainer, SWT.NONE);
		fcLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		fcLabel.setText(DocometreMessages.fcValueLabel);
		Text fcText = new Text(paramContainer, SWT.BORDER);
		fcText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fcText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(fcValueKey, fcText.getText());
			}
		});
		value  = getProperty(fcValueKey, "0");
		fcText.setText(value);
		
		Label feLabel = new Label(paramContainer, SWT.NONE);
		feLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		feLabel.setText(DocometreMessages.feValueLabel);
		Text feText = new Text(paramContainer, SWT.BORDER);
		feText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		feText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(feValueKey, feText.getText());
			}
		});
		value  = getProperty(feValueKey, "0");
		feText.setText(value);
		
		addCommentField(paramContainer, 1, context);

		return paramContainer;

	}
	
	@Override
	public String getCode(Object context, Object step) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
		String code = "";
		Process process = (Process) context;
		if(process instanceof ADWinProcess) {
			if(step == ADWinCodeSegmentProperties.DECLARATION) {
				code = code + "\nREM Filtered derivator declaration\n";
				String key = ADWinCodeSegmentProperties.DECLARATION.toString();
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, key.toUpperCase());
				String hashCode = String.valueOf(hashCode());
				code = code + temporaryCode.replaceAll("HashCode", hashCode);
			}
			if(step == ADWinCodeSegmentProperties.INITIALIZATION) {
				code = code + "\nREM Filtered derivator initialization\n";
				String key = ADWinCodeSegmentProperties.INITIALIZATION.toString();
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, key.toUpperCase());
				String hashCode = String.valueOf(hashCode());
				String aValue = getProperty(aValueKey, "0");
				String fcValue = getProperty(fcValueKey, "0");
				String feValue = getProperty(feValueKey, "1");
				String sampleFrequencyRatio  = getProperty(sampleFrequencyRatioKey, "1");
				temporaryCode = temporaryCode.replaceAll(fcValueKey, fcValue).replaceAll(feValueKey, feValue).replaceAll(aValueKey, aValue).replaceAll("HashCode", hashCode).replaceAll(sampleFrequencyRatioKey, sampleFrequencyRatio);
				code = code + temporaryCode + "\n\n";
			}
			if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
				// Récupérer la bonne propriété dans le fichier functionFileName en fonction du bon device : Gold ou Pro ET du bon CPU : I ou II
				// FUNCTION_CODE_GOLD_I ou FUNCTION_CODE_GOLD_II ou FUNCTION_CODE_PRO_I ou FUNCTION_CODE_PRO_II
				code = code + "\nREM Filtered derivator Function\n\n";
				String key = FUNCTION_CODE;
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, key.toUpperCase());
				String inputChannelName = getProperty(inputChannelNameKey, "");;
				String outputChannelName = getProperty(outputChannelNameKey, "");;
				String hashCode = String.valueOf(hashCode());
				String aValue = getProperty(aValueKey, "0");
				String sampleFrequencyRatio  = getProperty(sampleFrequencyRatioKey, "1");
				temporaryCode = temporaryCode.replaceAll(inputChannelNameKey, inputChannelName).replaceAll(outputChannelNameKey, outputChannelName).replaceAll("HashCode", hashCode);
				temporaryCode = temporaryCode.replaceAll(aValueKey, aValue).replaceAll(sampleFrequencyRatioKey, sampleFrequencyRatio);
				code = code + temporaryCode + "\n\n";
			}
		}
		return code;
	}
	
	@Override
	public Block clone() {
		FilteredDerivatorFunction function = new FilteredDerivatorFunction();
		super.clone(function);
		return function;
	}


}
