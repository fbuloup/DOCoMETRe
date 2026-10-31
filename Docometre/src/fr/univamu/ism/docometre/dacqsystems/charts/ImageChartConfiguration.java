package fr.univamu.ism.docometre.dacqsystems.charts;

import java.io.File;
import java.nio.FloatBuffer;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Hyperlink;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.DocometreSharedColors;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.dacqsystems.ExperimentScheduler;
import fr.univamu.ism.docometre.dacqsystems.ModifyPropertyHandler;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.Property;
import fr.univamu.ism.docometre.editors.ResourceEditor;
import fr.univamu.ism.docometre.editors.ModulePage.ModuleSectionPart;

public class ImageChartConfiguration extends ChartConfiguration {
	
	private static final long serialVersionUID = 1L;

	transient private Text imagesFileNameText;
	transient private Canvas imageCanvas;
	transient private Image image;
	transient private String errorMessage;

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
		if(!(object instanceof Process)) return;
		Process process = (Process)object;
		
		String value = getProperty(ChartConfigurationProperties.HORIZONTAL_SPANNING);
		int hSpan = Integer.parseInt(value);
		value = getProperty(ChartConfigurationProperties.VERTICAL_SPANNING);
		int vSpan = Integer.parseInt(value);
		
		imageCanvas = new Canvas(container, SWT.DOUBLE_BUFFERED);
		imageCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, hSpan, vSpan));
		// Same colors as the other real time charts (see MeterChartConfiguration)
		imageCanvas.setData("org.eclipse.e4.ui.css.id", "DocometreMeterChart");
		imageCanvas.setBackground(DocometreSharedColors.getInstance().getColor(new RGB(25, 28, 33)));
		imageCanvas.setForeground(DocometreSharedColors.getInstance().getColor(new RGB(217, 217, 229)));
		
		image = null;
		errorMessage = null;
		try {
			image = loadImage(process, container.getDisplay());
		} catch (Exception e) {
			errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
			Activator.logErrorMessage(errorMessage);
		}
		
		imageCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				paintImage(event.gc);
			}
		});
		imageCanvas.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent event) {
				if(image != null && !image.isDisposed()) image.dispose();
				image = null;
			}
		});
	}
	
	/*
	 * Get image path from images file in session folder and load this image.
	 * Line N of images file gives the image of trial N. If there are less lines
	 * than trials, last line is used. Empty lines are ignored.
	 */
	private Image loadImage(Process process, Display display) throws Exception {
		IFolder outputFolder = process.getOutputFolder();
		// When running a trial, output folder is the trial folder and its parent the session folder.
		// When testing a process, there is no session : use output folder parent instead.
		IContainer sessionFolder = outputFolder.getParent();
		File sessionDirectory = sessionFolder.getLocation().toFile();
		String imagesFileName = getProperty(ImageChartConfigurationProperties.IMAGES_FILE_NAME);
		File imagesFile = new File(sessionDirectory, imagesFileName);
		if(!imagesFile.isFile()) throw new Exception(NLS.bind(DocometreMessages.ImageChartFileNotFound, imagesFileName, sessionDirectory.getAbsolutePath()));
		
		List<String> lines = new ArrayList<>();
		List<String> fileLines;
		try {
			fileLines = Files.readAllLines(imagesFile.toPath(), StandardCharsets.UTF_8);
		} catch (MalformedInputException e) {
			// Not an UTF-8 file : probably edited with a Latin-1 editor
			fileLines = Files.readAllLines(imagesFile.toPath(), StandardCharsets.ISO_8859_1);
		}
		for (String line : fileLines) {
			line = line.trim();
			if(!line.isEmpty()) lines.add(line);
		}
		if(lines.isEmpty()) throw new Exception(NLS.bind(DocometreMessages.ImageChartFileEmpty, imagesFile.getAbsolutePath()));
		
		int trialNumber = ResourceType.isTrial(outputFolder) ? ExperimentScheduler.getInstance().getCurrentTrialNumber() : 1;
		int lineIndex = Math.min(Math.max(trialNumber, 1), lines.size()) - 1;
		
		// Image path may be absolute or relative to session folder
		File imageFile = new File(lines.get(lineIndex));
		if(!imageFile.isAbsolute()) imageFile = new File(sessionDirectory, lines.get(lineIndex));
		if(!imageFile.isFile()) throw new Exception(NLS.bind(DocometreMessages.ImageChartImageNotFound, new Object[] {imageFile.getAbsolutePath(), lineIndex + 1, imagesFile.getAbsolutePath()}));
		try {
			return new Image(display, imageFile.getAbsolutePath());
		} catch (SWTException e) {
			throw new Exception(NLS.bind(DocometreMessages.ImageChartImageLoadError, imageFile.getAbsolutePath(), e.getMessage()));
		}
	}
	
	/*
	 * Draw image centered and scaled to fit canvas while keeping its aspect ratio,
	 * or error message if image could not be loaded
	 */
	private void paintImage(GC gc) {
		Rectangle area = imageCanvas.getClientArea();
		if(image == null || image.isDisposed()) {
			if(errorMessage == null) return;
			gc.setForeground(imageCanvas.getDisplay().getSystemColor(SWT.COLOR_RED));
			gc.drawText(errorMessage, area.x + 5, area.y + 5, true);
			return;
		}
		Rectangle bounds = image.getBounds();
		if(area.width <= 0 || area.height <= 0 || bounds.width <= 0 || bounds.height <= 0) return;
		double scale = Math.min((double)area.width / bounds.width, (double)area.height / bounds.height);
		int width = (int) Math.round(bounds.width * scale);
		int height = (int) Math.round(bounds.height * scale);
		int x = area.x + (area.width - width) / 2;
		int y = area.y + (area.height - height) / 2;
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		gc.drawImage(image, 0, 0, bounds.width, bounds.height, x, y, width, height);
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
