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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [11/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.editors;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.ImagesFile;
import fr.univamu.ism.docometre.dacqsystems.ImagesFile.ImageLine;
import fr.univamu.ism.docometre.widgets.ImageCanvas;

/**
 * Images page of the images files editor : one row per image line of the file (comment and
 * empty lines are not shown), with the trial number, the path of the image (red if the image
 * is not found) and the comment of the line. The selected image is previewed.
 * Rows are read from, and changes written to, the document of the editor.
 */
public class ImagesTableEditor extends EditorPart implements TextFileEditorPage {
	
	// Folder of the last added images
	private static String lastImagesFolder;
	
	private ImagesEditor imagesEditor;
	private TableViewer imagesTableViewer;
	private ImageCanvas previewCanvas;
	private Label previewLabel;
	private Button removeButton;
	private Button moveUpButton;
	private Button moveDownButton;
	// Image lines of the document, rows of the table
	private List<ImageLine> imageLines = new ArrayList<>();
	
	/*
	 * Edition of the path or of the comment of an image line
	 */
	private class ImageLineEditingSupport extends EditingSupport {
		
		private TextCellEditor textCellEditor;
		private boolean comment;

		public ImageLineEditingSupport(TableViewer tableViewer, boolean comment) {
			super(tableViewer);
			textCellEditor = new TextCellEditor(tableViewer.getTable());
			this.comment = comment;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return textCellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			return comment ? getComment((ImageLine)element) : ((ImageLine)element).getPath();
		}

		@Override
		protected void setValue(Object element, Object value) {
			ImageLine imageLine = (ImageLine)element;
			String newValue = ((String)value).trim();
			if(newValue.equals(getValue(element))) return;
			// An image line without path would become a comment or an empty line
			if(!comment && newValue.isEmpty()) return;
			// A path can't contain a comment
			if(!comment && newValue.contains("#")) return;
			int lineIndex = imageLine.getLineNumber() - 1;
			String line = getLine(lineIndex);
			line = comment ? ImagesFile.setComment(line, newValue) : ImagesFile.setPath(line, newValue);
			replaceLine(lineIndex, line);
			update(new int[] {lineIndex});
		}
		
	}

	public ImagesTableEditor(ImagesEditor imagesEditor) {
		this.imagesEditor = imagesEditor;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
		
		Composite tableContainer = new Composite(sashForm, SWT.NONE);
		tableContainer.setLayout(new GridLayout(4, false));
		
		imagesTableViewer = new TableViewer(tableContainer, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		imagesTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		imagesTableViewer.getTable().setHeaderVisible(true);
		imagesTableViewer.getTable().setLinesVisible(true);
		imagesTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		// Tooltip of the image column : absolute path of the image
		ColumnViewerToolTipSupport.enableFor(imagesTableViewer);
		
		TableViewerColumn trialColumn = new TableViewerColumn(imagesTableViewer, SWT.CENTER);
		trialColumn.getColumn().setText(DocometreMessages.ImagesEditor_TrialColumn);
		trialColumn.getColumn().setWidth(70);
		trialColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				int trialNumber = imageLines.indexOf(element) + 1;
				// Last line : image of the remaining trials
				if(trialNumber == imageLines.size()) return "≥ " + trialNumber;
				return String.valueOf(trialNumber);
			}
		});
		
		TableViewerColumn imageColumn = new TableViewerColumn(imagesTableViewer, SWT.NONE);
		imageColumn.getColumn().setText(DocometreMessages.ImagesEditor_ImageColumn);
		imageColumn.getColumn().setWidth(350);
		imageColumn.setEditingSupport(new ImageLineEditingSupport(imagesTableViewer, false));
		imageColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((ImageLine)element).getPath();
			}
			@Override
			public Color getForeground(Object element) {
				if(getImageFile((ImageLine)element).isFile()) return null;
				return imagesTableViewer.getTable().getDisplay().getSystemColor(SWT.COLOR_RED);
			}
			@Override
			public String getToolTipText(Object element) {
				return getImageFile((ImageLine)element).getAbsolutePath();
			}
		});
		
		TableViewerColumn commentColumn = new TableViewerColumn(imagesTableViewer, SWT.NONE);
		commentColumn.getColumn().setText(DocometreMessages.ImagesEditor_CommentColumn);
		commentColumn.getColumn().setWidth(250);
		commentColumn.setEditingSupport(new ImageLineEditingSupport(imagesTableViewer, true));
		commentColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return getComment((ImageLine)element);
			}
		});
		
		imagesTableViewer.addSelectionChangedListener(event -> {
			updatePreview();
			updateButtons();
		});
		
		Button addButton = new Button(tableContainer, SWT.PUSH);
		addButton.setText(DocometreMessages.ImagesEditor_AddImages);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				addImages();
			}
		});
		removeButton = new Button(tableContainer, SWT.PUSH);
		removeButton.setText(DocometreMessages.ImagesEditor_Remove);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				removeImages();
			}
		});
		moveUpButton = new Button(tableContainer, SWT.PUSH);
		moveUpButton.setText(DocometreMessages.ImagesEditor_MoveUp);
		moveUpButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				moveImage(-1);
			}
		});
		moveDownButton = new Button(tableContainer, SWT.PUSH);
		moveDownButton.setText(DocometreMessages.ImagesEditor_MoveDown);
		moveDownButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				moveImage(1);
			}
		});
		
		Composite previewContainer = new Composite(sashForm, SWT.NONE);
		previewContainer.setLayout(new GridLayout());
		previewLabel = new Label(previewContainer, SWT.WRAP);
		previewLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		previewCanvas = new ImageCanvas(previewContainer, SWT.BORDER);
		previewCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		sashForm.setWeights(new int[] {60, 40});
		
		update();
	}
	
	@Override
	public void setFocus() {
		imagesTableViewer.getTable().setFocus();
	}
	
	@Override
	public void update() {
		update(null);
	}
	
	/*
	 * Reads image lines from the document and selects the rows of these lines indexes (null : selection kept if possible)
	 */
	private void update(int[] selectedLinesIndexes) {
		if(imagesTableViewer == null || imagesTableViewer.getTable().isDisposed()) return;
		List<Integer> selectedLines = new ArrayList<>();
		if(selectedLinesIndexes != null) for (int lineIndex : selectedLinesIndexes) selectedLines.add(lineIndex + 1);
		else for (ImageLine imageLine : getSelectedImageLines()) selectedLines.add(imageLine.getLineNumber());
		IDocument document = imagesEditor.getDocument();
		List<String> lines = new ArrayList<>();
		for (int i = 0; i < document.getNumberOfLines(); i++) lines.add(getLine(i));
		imageLines = ImagesFile.getImageLines(lines);
		imagesTableViewer.setInput(imageLines);
		List<ImageLine> selection = new ArrayList<>();
		for (ImageLine imageLine : imageLines) if(selectedLines.contains(imageLine.getLineNumber())) selection.add(imageLine);
		imagesTableViewer.setSelection(new StructuredSelection(selection), true);
		updatePreview();
		updateButtons();
	}
	
	private List<ImageLine> getSelectedImageLines() {
		List<ImageLine> selectedImageLines = new ArrayList<>();
		for (Object object : ((IStructuredSelection)imagesTableViewer.getSelection()).toList()) selectedImageLines.add((ImageLine) object);
		selectedImageLines.sort(Comparator.comparingInt(ImageLine::getLineNumber));
		return selectedImageLines;
	}
	
	private void updateButtons() {
		List<ImageLine> selectedImageLines = getSelectedImageLines();
		removeButton.setEnabled(!selectedImageLines.isEmpty());
		boolean oneSelected = selectedImageLines.size() == 1;
		int index = oneSelected ? imageLines.indexOf(selectedImageLines.get(0)) : -1;
		moveUpButton.setEnabled(oneSelected && index > 0);
		moveDownButton.setEnabled(oneSelected && index < imageLines.size() - 1);
	}
	
	/*
	 * Preview of the first selected image
	 */
	private void updatePreview() {
		List<ImageLine> selectedImageLines = getSelectedImageLines();
		if(selectedImageLines.isEmpty()) {
			previewLabel.setText("");
			previewCanvas.setImage(null);
			return;
		}
		File imageFile = getImageFile(selectedImageLines.get(0));
		previewLabel.setText(imageFile.getAbsolutePath());
		previewLabel.getParent().layout();
		if(!imageFile.isFile()) {
			previewCanvas.setMessage(NLS.bind(DocometreMessages.ImagesEditor_ImageNotFound, imageFile.getAbsolutePath()));
			return;
		}
		try {
			previewCanvas.setImage(new Image(previewCanvas.getDisplay(), imageFile.getAbsolutePath()));
		} catch (SWTException e) {
			previewCanvas.setMessage(NLS.bind(DocometreMessages.ImagesEditor_ImageLoadError, imageFile.getAbsolutePath(), e.getMessage()));
		}
	}
	
	/*
	 * Adds images chosen by the user after the last selected image, at the end of the file if there is no selection
	 */
	private void addImages() {
		File workspaceFolder = getWorkspaceFolder();
		FileDialog fileDialog = new FileDialog(getSite().getShell(), SWT.OPEN | SWT.MULTI);
		fileDialog.setFilterExtensions(ImagesFile.IMAGES_EXTENSIONS);
		fileDialog.setFilterPath(lastImagesFolder != null ? lastImagesFolder : imagesEditor.getFile().getProject().getLocation().toOSString());
		if(fileDialog.open() == null) return;
		lastImagesFolder = fileDialog.getFilterPath();
		String[] fileNames = fileDialog.getFileNames();
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < fileNames.length; i++) {
			if(i > 0) text.append("\n");
			text.append(ImagesFile.getPath(new File(lastImagesFolder, fileNames[i]), workspaceFolder));
		}
		try {
			IDocument document = imagesEditor.getDocument();
			List<ImageLine> selectedImageLines = getSelectedImageLines();
			int firstNewLineIndex;
			if(!selectedImageLines.isEmpty()) {
				int lineIndex = selectedImageLines.get(selectedImageLines.size() - 1).getLineNumber() - 1;
				IRegion region = document.getLineInformation(lineIndex);
				document.replace(region.getOffset() + region.getLength(), 0, "\n" + text);
				firstNewLineIndex = lineIndex + 1;
			} else {
				String content = document.get();
				if(content.isEmpty()) {
					document.set(text.toString());
					firstNewLineIndex = 0;
				} else if(content.endsWith("\n") || content.endsWith("\r")) {
					firstNewLineIndex = document.getNumberOfLines() - 1;
					document.replace(document.getLength(), 0, text.toString());
				} else {
					firstNewLineIndex = document.getNumberOfLines();
					document.replace(document.getLength(), 0, "\n" + text);
				}
			}
			int[] newLinesIndexes = new int[fileNames.length];
			for (int i = 0; i < newLinesIndexes.length; i++) newLinesIndexes[i] = firstNewLineIndex + i;
			update(newLinesIndexes);
		} catch (BadLocationException e) {
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	/*
	 * Removes the lines of the selected images (their comments are removed too)
	 */
	private void removeImages() {
		List<ImageLine> selectedImageLines = getSelectedImageLines();
		try {
			IDocument document = imagesEditor.getDocument();
			for (int i = selectedImageLines.size() - 1; i >= 0; i--) {
				int lineIndex = selectedImageLines.get(i).getLineNumber() - 1;
				IRegion region = document.getLineInformation(lineIndex);
				String delimiter = document.getLineDelimiter(lineIndex);
				if(delimiter != null) document.replace(region.getOffset(), region.getLength() + delimiter.length(), "");
				else if(lineIndex > 0) {
					// Last line : the delimiter of the previous line is removed
					int previousDelimiterLength = document.getLineDelimiter(lineIndex - 1).length();
					document.replace(region.getOffset() - previousDelimiterLength, region.getLength() + previousDelimiterLength, "");
				} else document.replace(region.getOffset(), region.getLength(), "");
			}
			update(new int[0]);
		} catch (BadLocationException e) {
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	/*
	 * Exchanges the line of the selected image with the line of the previous (direction -1) or next (direction 1) image
	 */
	private void moveImage(int direction) {
		List<ImageLine> selectedImageLines = getSelectedImageLines();
		if(selectedImageLines.size() != 1) return;
		int index = imageLines.indexOf(selectedImageLines.get(0));
		int otherIndex = index + direction;
		if(otherIndex < 0 || otherIndex >= imageLines.size()) return;
		int lineIndex = imageLines.get(index).getLineNumber() - 1;
		int otherLineIndex = imageLines.get(otherIndex).getLineNumber() - 1;
		String line = getLine(lineIndex);
		String otherLine = getLine(otherLineIndex);
		// Replace the last line first : offsets of the first one are not changed
		if(lineIndex > otherLineIndex) {
			replaceLine(lineIndex, otherLine);
			replaceLine(otherLineIndex, line);
		} else {
			replaceLine(otherLineIndex, line);
			replaceLine(lineIndex, otherLine);
		}
		update(new int[] {otherLineIndex});
	}
	
	/*
	 * Text of a line of the document, without its delimiter
	 */
	private String getLine(int lineIndex) {
		try {
			IRegion region = imagesEditor.getDocument().getLineInformation(lineIndex);
			return imagesEditor.getDocument().get(region.getOffset(), region.getLength());
		} catch (BadLocationException e) {
			Activator.logErrorMessageWithCause(e);
			return "";
		}
	}
	
	/*
	 * Replaces the text of a line of the document, its delimiter is kept
	 */
	private void replaceLine(int lineIndex, String text) {
		try {
			IRegion region = imagesEditor.getDocument().getLineInformation(lineIndex);
			imagesEditor.getDocument().replace(region.getOffset(), region.getLength(), text);
		} catch (BadLocationException e) {
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	private String getComment(ImageLine imageLine) {
		return ImagesFile.getComment(getLine(imageLine.getLineNumber() - 1)).replaceFirst("^#", "").trim();
	}
	
	private static File getWorkspaceFolder() {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
	}
	
	private static File getImageFile(ImageLine imageLine) {
		return ImagesFile.getImageFile(imageLine.getPath(), getWorkspaceFolder());
	}

}
