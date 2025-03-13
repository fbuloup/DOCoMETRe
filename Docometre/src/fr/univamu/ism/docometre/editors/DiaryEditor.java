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
package fr.univamu.ism.docometre.editors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.PartListenerAdapter;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.annotations.DocometreAnnotationAccesExtension;
import fr.univamu.ism.docometre.annotations.DocometreSharedTextColors;
import fr.univamu.ism.docometre.annotations.ErrorAnnotation;
import fr.univamu.ism.docometre.annotations.WarningAnnotation;
import fr.univamu.ism.docometre.dacqsystems.DocometreBuilder;
import fr.univamu.ism.docometre.dialogs.FindDialog;

public class DiaryEditor extends EditorPart implements PartNameRefresher {
	
	public static String ID = "Docometre.DiaryEditor";
	private SourceViewer sourceViewer;
	private PartListenerAdapter partListenerAdapter;
	private ArrayList<Font> fontsArrayList = new ArrayList<>();

	public DiaryEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		IResource resource = ((IResource)((ResourceEditorInput)input).getObject());
		setPartName(GetResourceLabelDelegate.getLabel(resource));
//		setTitleToolTip(getEditorInput().getToolTipText());
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		try {
			
			IResource diary = ((IResource)((ResourceEditorInput)getEditorInput()).getObject());
			
			Document document = new Document();
			
			// Composite holding lines numbers and annotation column 
			CompositeRuler lineAnnotationRuler = new CompositeRuler();
			AnnotationModel annotationModel = new AnnotationModel();
			annotationModel.connect(document);
			lineAnnotationRuler.setModel(annotationModel);
			// Lines numbers column
			LineNumberRulerColumn lineNumberRulerColumn = new LineNumberRulerColumn();
			lineNumberRulerColumn.setForeground(PlatformUI.createDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
			lineAnnotationRuler.addDecorator(0,lineNumberRulerColumn);
			// Annotation column
			DocometreAnnotationAccesExtension docometreAnnotationAccesExtension = new DocometreAnnotationAccesExtension();
			AnnotationRulerColumn arc = new AnnotationRulerColumn(annotationModel, 13, docometreAnnotationAccesExtension);
			DefaultAnnotationHover defaultAnnotationHover = new DefaultAnnotationHover();
			arc.setHover(defaultAnnotationHover);
			arc.addAnnotationType(ErrorAnnotation.TYPE_ERROR);
			lineAnnotationRuler.addDecorator(1, arc);
			
			// Overview column
			OverviewRuler overviewRuler = new OverviewRuler(docometreAnnotationAccesExtension, 13, DocometreSharedTextColors.getInstance());
			overviewRuler.addAnnotationType(ErrorAnnotation.TYPE_ERROR); 
			overviewRuler.addAnnotationType(WarningAnnotation.TYPE_WARNING);
			overviewRuler.addHeaderAnnotationType(ErrorAnnotation.TYPE_ERROR);
			overviewRuler.addHeaderAnnotationType(WarningAnnotation.TYPE_WARNING);
			overviewRuler.setAnnotationTypeColor(ErrorAnnotation.TYPE_ERROR,   DocometreSharedTextColors.getInstance().getColor(new RGB(255, 0, 0))); 
			overviewRuler.setAnnotationTypeLayer(ErrorAnnotation.TYPE_ERROR, IAnnotationAccessExtension.DEFAULT_LAYER); 
			overviewRuler.setAnnotationTypeColor(WarningAnnotation.TYPE_WARNING,   DocometreSharedTextColors.getInstance().getColor(new RGB(255, 255, 0))); 
			overviewRuler.setAnnotationTypeLayer(WarningAnnotation.TYPE_WARNING, IAnnotationAccessExtension.DEFAULT_LAYER); 
			
			sourceViewer = new SourceViewer(parent, lineAnnotationRuler, overviewRuler, true, SWT.V_SCROLL | SWT.H_SCROLL);
			sourceViewer.getTextWidget().setEditable(false);
			sourceViewer.setDocument(document, annotationModel, -1, -1);
			sourceViewer.configure(DiarySourceViewerConfigurationFactory.getSourceViewerConfiguration(diary));
			sourceViewer.getTextWidget().addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent event) {
					if(((event.stateMask & SWT.MOD1) == SWT.MOD1) && event.keyCode == 'f') {
						FindDialog.getInstance().setTextViewer(sourceViewer);
						FindDialog.getInstance().open();
					}
					if(((event.stateMask & SWT.MOD1) == SWT.MOD1) && event.keyCode == '=') {
						 Font font = sourceViewer.getTextWidget().getFont();
						 Font newFont = new Font(font.getDevice(), font.getFontData()[0].getName(), font.getFontData()[0].getHeight() + 1, font.getFontData()[0].getStyle());
						 sourceViewer.getTextWidget().setFont(newFont);
						 fontsArrayList.add(newFont);
					 }
					 if(((event.stateMask & SWT.MOD1) == SWT.MOD1) && event.keyCode == '-') {
						 Font font = sourceViewer.getTextWidget().getFont();
						 Font newFont = new Font(font.getDevice(), font.getFontData()[0].getName(), font.getFontData()[0].getHeight() - 1, font.getFontData()[0].getStyle());
						 sourceViewer.getTextWidget().setFont(newFont);
						 fontsArrayList.add(newFont);
					 }
				}
			});
			sourceViewer.getTextWidget().addCaretListener(new CaretListener() {
				@Override
				public void caretMoved(CaretEvent event) {
					FindDialog.getInstance().resetOffset(sourceViewer, event.caretOffset);
				}
			});
			
			String system = ResourceProperties.getSystemPersistentProperty(diary);
			
			if(system == null) {
				IResource process = ResourceProperties.getAssociatedProcess(diary.getParent());
				IResource dacq = ResourceProperties.getAssociatedDACQConfiguration(process);
				system = ResourceProperties.getSystemPersistentProperty(dacq);
				ResourceProperties.setSystemPersistentProperty(diary, system);
			}
			
			if(Activator.ADWIN_SYSTEM.equals(system)) {
				DiaryStatisticsComputer diaryStatComputer = DiaryStatisticsComputerFactory.getStatisticsComputer(diary);
				
				IFile logFile = (IFile) ((ResourceEditorInput)getEditorInput()).getObject();
				List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(logFile.getLocation().toOSString()), StandardCharsets.UTF_8);
				StringBuffer content = new StringBuffer();
				for (String line : lines) {
					line = diaryStatComputer.computeValue(line);
					content.append(line);
					if(!line.equals(lines.get(lines.size() - 1))) content.append("\n");
				}
				document.set(content.toString());
				
				try {
					IMarker[] markers = logFile.findMarkers(DocometreBuilder.MARKER_ID, true,IResource.DEPTH_INFINITE);
					for (IMarker marker : markers) {
						Annotation annotation = null;
						String line = (String) marker.getAttribute(IMarker.MESSAGE);
						if (marker.getAttribute(IMarker.SEVERITY).equals(IMarker.SEVERITY_ERROR)) annotation = new ErrorAnnotation(marker, line);
						if (marker.getAttribute(IMarker.SEVERITY).equals(IMarker.SEVERITY_WARNING)) annotation = new WarningAnnotation(marker, line);
						int lineNumber = (int) marker.getAttribute(IMarker.LINE_NUMBER);
						int lineOffset = document.getLineOffset(lineNumber - 1);
						String lengthLine = line.replaceAll("^(.|\\n)*line:\\s", "");
						lengthLine = lengthLine.replaceAll("\\sfile:(.*)$", "");
						annotationModel.addAnnotation(annotation, new Position(lineOffset, lengthLine.length()));
					}
				} catch (Exception e) {
					e.printStackTrace();
					Activator.logErrorMessageWithCause(e);
				}
			}
			
			if(Activator.ARDUINO_UNO_SYSTEM.equals(system)) {
				IFile logFile = (IFile) ((ResourceEditorInput)getEditorInput()).getObject();
				List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(logFile.getLocation().toOSString()), StandardCharsets.UTF_8);
				StringBuffer content = new StringBuffer();
				for (String line : lines) {
					content.append(line);
					if(!line.equals(lines.get(lines.size() - 1))) content.append("\n");
				}
				document.set(content.toString());
			}
			
			partListenerAdapter = new PartListenerAdapter() {
				@Override
				public void partClosed(IWorkbenchPartReference partRef) {
					if(partRef.getPart(false) == DiaryEditor.this) {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListenerAdapter);
					}
				}
				
				@Override
				public void partActivated(IWorkbenchPartReference partRef) {
					update(partRef);
				}
				
				@Override
				public void partBroughtToTop(IWorkbenchPartReference partRef) {
					update(partRef);
				}
				
				private void update(IWorkbenchPartReference partRef) {
					if(partRef.getPart(false) == DiaryEditor.this) FindDialog.getInstance().setTextViewer(sourceViewer);
				}
			};
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListenerAdapter);
			
			
		} catch (IOException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}

	@Override
	public void dispose() {
		for (Font font : fontsArrayList) font.dispose();
		super.dispose();
	}

	@Override
	public void setFocus() {
		sourceViewer.getTextWidget().setFocus();
	}

	@Override
	public void refreshPartName() {
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
//		IResource resource = ObjectsController.getResourceForObject(object);
		setPartName(GetResourceLabelDelegate.getLabel((IResource) object));
		setTitleToolTip(getEditorInput().getToolTipText());
		firePropertyChange(PROP_TITLE);
	}

}
