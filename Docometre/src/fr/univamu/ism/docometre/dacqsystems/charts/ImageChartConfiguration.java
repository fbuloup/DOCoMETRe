package fr.univamu.ism.docometre.dacqsystems.charts;

import java.nio.FloatBuffer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Hyperlink;

import fr.univamu.ism.docometre.dacqsystems.ExperimentScheduler;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.docometre.editors.ModulePage.ModuleSectionPart;

public class ImageChartConfiguration extends ChartConfiguration {
	
	private static final long serialVersionUID = 1L;

	transient private Text imagesFileNameText;

	public ImageChartConfiguration() {
		super(ChartTypes.IMAGE_CHART);
		ImageChartConfigurationProperties.populateProperties(this);
	}

	@Override
	public void update(FloatBuffer floatBuffer, String channelID) {

	}

	@Override
	public CurveConfiguration[] createCurvesConfiguration(IStructuredSelection selection) {
		return null;
	}

	@Override
	public void populateChartConfigurationContainer(Composite container, ChartsConfigurationPage page, ModuleSectionPart generalConfigurationSectionPart) {
		container.setLayout(new GridLayout(2, false));
		
		page.createLabel(container, ImageChartConfigurationProperties.IMAGES_FILE_NAME.getLabel(), ImageChartConfigurationProperties.IMAGES_FILE_NAME.getTooltip());
		imagesFileNameText = page.createText(container, getProperty(ImageChartConfigurationProperties.IMAGES_FILE_NAME), SWT.NONE, 1, 1);
		imagesFileNameText.addModifyListener(page.getGeneralConfigurationModifyListener());
		imagesFileNameText.addModifyListener(new ModifyPropertyHandler(ImageChartConfigurationProperties.IMAGES_FILE_NAME, ImageChartConfiguration.this, imagesFileNameText, ImageChartConfigurationProperties.IMAGES_FILE_NAME.getRegExp(), "", false, (ResourceEditor)page.getEditor()));

	}
	
	/*
	 * Helper method to update widget associated with specific key
	 */
	private void updateWidget(Control widget, ImageChartConfigurationProperties propertyKey) {
		String value = getProperty(propertyKey);
		Listener[] listeners = widget.getListeners(SWT.Modify);
		for (Listener listener : listeners) widget.removeListener(SWT.Modify, listener);
		if(widget instanceof Text) {
			Text text = (Text) widget;
			if(!text.getText().equals(value)) text.setText(value);
		}
		if(widget instanceof Hyperlink) ((Hyperlink)widget).setText(value);
		if(widget instanceof Combo) ((Combo)widget).select(((Combo)widget).indexOf(value));
		if(widget instanceof Button) ((Button)widget).setSelection(Boolean.valueOf(value));
		widget.setFocus();
		for (Listener listener : listeners) widget.addListener(SWT.Modify , listener);
	}

	@Override
	public void update(Property property, Object newValue, Object oldValue) {
		if(!(property instanceof ImageChartConfigurationProperties)) return;
		if(property == ImageChartConfigurationProperties.IMAGES_FILE_NAME)
			updateWidget(imagesFileNameText, (ImageChartConfigurationProperties)property);

	}

	@Override
	public void createChart(Composite container) {
		Object object = container.getData("process");
		if(object != null && object instanceof Process) {
			Process process = (Process)object;
			IPath wsPath = new Path(Platform.getInstanceLocation().getURL().getPath());
			String directoryName = wsPath.toOSString() + process.getOutputFolder().getFullPath().toOSString();
			String imageFileName = getProperty(ImageChartConfigurationProperties.IMAGES_FILE_NAME);
			System.out.print("Must read images file name : " + imageFileName + " in folder " + directoryName + " at line number " + ExperimentScheduler.getInstance().getCurrentTrialNumber());
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return ImageChartConfigurationProperties.clone(this);
	}

	@Override
	public void initializeObservers() {
		// TODO Auto-generated method stub

	}

}
