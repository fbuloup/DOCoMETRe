package fr.univamu.ism.docometre.dacqsystems.charts;

import java.io.File;
import java.nio.FloatBuffer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Hyperlink;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.DocometreSharedColors;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.ExperimentScheduler;
import fr.univamu.ism.docometre.dacqsystems.ImagesFile;
import fr.univamu.ism.docometre.dacqsystems.ImagesFile.ImageLine;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.docometre.editors.ModulePage.ModuleSectionPart;
import fr.univamu.ism.docometre.widgets.ImageCanvas;

public class ImageChartConfiguration extends ChartConfiguration {
	
	private static final long serialVersionUID = 1L;

	transient private Text imagesFileNameText;
	transient private ImageCanvas imageCanvas;

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
		
		Label explanationsLabel = page.getManagedForm().getToolkit().createLabel(container, DocometreMessages.ImageChartExplanations, SWT.WRAP);
		explanationsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)explanationsLabel.getLayoutData()).widthHint = 200;
		
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
		if(!(object instanceof Process)) return;
		Process process = (Process)object;
		
		String value = getProperty(ChartConfigurationProperties.HORIZONTAL_SPANNING);
		int hSpan = Integer.parseInt(value);
		value = getProperty(ChartConfigurationProperties.VERTICAL_SPANNING);
		int vSpan = Integer.parseInt(value);
		
		imageCanvas = new ImageCanvas(container, SWT.NONE);
		imageCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, hSpan, vSpan));
		// Same colors as the other real time charts (see MeterChartConfiguration)
		imageCanvas.setData("org.eclipse.e4.ui.css.id", "DocometreMeterChart");
		imageCanvas.setBackground(DocometreSharedColors.getInstance().getColor(new RGB(25, 28, 33)));
		imageCanvas.setForeground(DocometreSharedColors.getInstance().getColor(new RGB(217, 217, 229)));
		
		updateImage(process);
	}
	
	/*
	 * Replace current image by the one of current trial, or by an error message
	 */
	private void updateImage(Process process) {
		try {
			imageCanvas.setImage(loadImage(process, imageCanvas.getDisplay()));
		} catch (Exception e) {
			String errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
			Activator.logErrorMessage(errorMessage);
			imageCanvas.setMessage(errorMessage);
		}
	}
	
	/*
	 * Get image path from images file in session folder and load this image.
	 * See ImagesFile for the format of the images file.
	 */
	private Image loadImage(Process process, Display display) throws Exception {
		IFolder outputFolder = process.getOutputFolder();
		// When running a trial, output folder is the trial folder and its parent the session folder.
		// When testing a process, there is no session : use output folder parent instead.
		IContainer sessionFolder = outputFolder.getParent();
		File sessionDirectory = sessionFolder.getLocation().toFile();
		String imagesFileName = ImagesFile.getFileName(getProperty(ImageChartConfigurationProperties.IMAGES_FILE_NAME));
		File imagesFile = new File(sessionDirectory, imagesFileName);
		if(!imagesFile.isFile()) throw new Exception(NLS.bind(DocometreMessages.ImageChartFileNotFound, imagesFileName, sessionDirectory.getAbsolutePath()));
		
		int trialNumber = ResourceType.isTrial(outputFolder) ? ExperimentScheduler.getInstance().getCurrentTrialNumber() : 1;
		ImageLine imageLine = ImagesFile.getTrialImageLine(ImagesFile.getImageLines(ImagesFile.readLines(imagesFile.toPath())), trialNumber);
		if(imageLine == null) throw new Exception(NLS.bind(DocometreMessages.ImageChartFileEmpty, imagesFile.getAbsolutePath()));
		
		File imageFile = ImagesFile.getImageFile(imageLine.getPath(), ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile());
		if(!imageFile.isFile()) throw new Exception(NLS.bind(DocometreMessages.ImageChartImageNotFound, new Object[] {imageFile.getAbsolutePath(), imageLine.getLineNumber(), imagesFile.getAbsolutePath()}));
		try {
			return new Image(display, imageFile.getAbsolutePath());
		} catch (SWTException e) {
			throw new Exception(NLS.bind(DocometreMessages.ImageChartImageLoadError, imageFile.getAbsolutePath(), e.getMessage()));
		}
	}
	
	/*
	 * Canvas is kept : only the image of the new current trial is loaded.
	 */
	@Override
	public boolean resetChart(Composite container) {
		if(imageCanvas == null || imageCanvas.isDisposed()) return false;
		Object object = container.getData("process");
		if(!(object instanceof Process)) return false;
		updateImage((Process)object);
		return true;
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
