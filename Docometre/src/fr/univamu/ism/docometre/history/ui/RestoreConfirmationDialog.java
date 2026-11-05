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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [04/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.history.ui;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.history.FileChange;

/**
 * Confirmation of a restoration : what will be done and which files will be changed.
 */
public class RestoreConfirmationDialog extends MessageDialog {
	
	private final List<String> steps;
	private final List<FileChange> changes;
	private final String modifiedSubjects;
	private Button saveSubjectsButton;
	private boolean saveSubjects = true;

	/*
	 * modifiedSubjects : names of loaded subjects which have unsaved modifications, null if none
	 */
	public RestoreConfirmationDialog(Shell parentShell, String question, boolean warning, List<String> steps, List<FileChange> changes, String modifiedSubjects) {
		super(parentShell, DocometreMessages.History_RestoreTitle, null, question, warning ? WARNING : QUESTION, 1, DocometreMessages.History_RestoreButton, IDialogConstants.CANCEL_LABEL);
		this.steps = steps;
		this.changes = changes;
		this.modifiedSubjects = modifiedSubjects;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	@Override
	protected Control createCustomArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		int width = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		
		for (String step : steps) {
			Label bulletLabel = new Label(container, SWT.NONE);
			bulletLabel.setText("•");
			GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(bulletLabel);
			Label stepLabel = new Label(container, SWT.WRAP);
			stepLabel.setText(step);
			GridDataFactory.fillDefaults().grab(true, false).hint(width, SWT.DEFAULT).applyTo(stepLabel);
		}
		
		if(modifiedSubjects != null) {
			saveSubjectsButton = new Button(container, SWT.CHECK);
			saveSubjectsButton.setText(NLS.bind(DocometreMessages.History_RestoreSaveSubjects, modifiedSubjects));
			saveSubjectsButton.setSelection(true);
			GridDataFactory.fillDefaults().span(2, 1).indent(0, 5).applyTo(saveSubjectsButton);
		}
		
		Label filesLabel = new Label(container, SWT.NONE);
		filesLabel.setText(DocometreMessages.History_RestoreFiles);
		GridDataFactory.fillDefaults().span(2, 1).indent(0, 5).applyTo(filesLabel);
		
		TableViewer tableViewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		TableViewerColumn kindColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
		kindColumn.getColumn().setText(DocometreMessages.SnapshotsView_Change);
		kindColumn.getColumn().setWidth(100);
		kindColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return getRestoreKindLabel(((FileChange)element).kind());
			}
		});
		TableViewerColumn pathColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
		pathColumn.getColumn().setText(DocometreMessages.SnapshotsView_File);
		pathColumn.getColumn().setWidth(400);
		pathColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return SnapshotsView.getPathLabel(((FileChange)element).path());
			}
		});
		tableViewer.setInput(changes);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).hint(width, 8 * tableViewer.getTable().getItemHeight()).applyTo(tableViewer.getControl());
		return container;
	}
	
	/*
	 * Restoration point of view : added files are recreated, modified ones replaced, deleted ones removed
	 */
	private static String getRestoreKindLabel(FileChange.Kind kind) {
		switch (kind) {
		case ADDED:
			return DocometreMessages.FileChange_ADDED;
		case DELETED:
			return DocometreMessages.FileChange_DELETED;
		default:
			return DocometreMessages.FileChange_MODIFIED;
		}
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if(saveSubjectsButton != null) saveSubjects = saveSubjectsButton.getSelection();
		super.buttonPressed(buttonId);
	}
	
	/*
	 * True if modified subjects must be saved before being unloaded
	 */
	public boolean isSaveSubjects() {
		return saveSubjects;
	}

}
