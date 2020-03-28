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
package fr.univamu.ism.docometre.dialogs;

import java.util.HashMap;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.DocometreMessages;

public class FindDialog extends org.eclipse.jface.dialogs.Dialog {
	
	private static String lastQuery = "";
	private static Rectangle lastShellBounds = null;

	private TextViewer textViewer;
	private Text textToFindText;
	private int offset;
	private HashMap<Object, Integer> offsets = new HashMap<>();
	private Button wholeWordButton;
	private Button searchBackButton;
	private Button caseSensitiveButton;

	private static FindDialog findDialog;
	
	public static FindDialog getInstance() {
		if(findDialog == null) {
			findDialog = new FindDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		}
		return findDialog;
	}
	
	private FindDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public void resetOffset(TextViewer textViewer, int offset) {
		offsets.put(textViewer, offset);
		this.offset = offset;
	}
	
	@Override
	public boolean close() {
		findDialog = null;
		offsets.clear();
		return super.close();
	}
	
	public void setTextViewer(TextViewer textViewer) {
		this.textViewer = textViewer;
		offset = offsets.get(textViewer) == null ? 0 : offsets.get(textViewer);
	}
	
	@Override
	protected void initializeBounds() {
		if(lastShellBounds == null) super.initializeBounds();
		else getShell().setBounds(getConstrainedShellBounds(new Rectangle(lastShellBounds.x, lastShellBounds.y, lastShellBounds.width, lastShellBounds.height)));
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
		setBlockOnOpen(false);
	}

	@Override
	protected Control createContents(Composite parent) {
		getShell().addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				lastShellBounds = getShell().getBounds();
			}
		});
		getShell().addListener(SWT.Move, new Listener() {
			@Override
			public void handleEvent(Event event) {
				lastShellBounds = getShell().getBounds();
			}
		});
		getShell().setText(DocometreMessages.FindDialogShellTitle);
		Control container = super.createContents(parent);
		Composite dialogArea = (Composite) getDialogArea();
		dialogArea.setLayout(new GridLayout(2, false));

		Label textToFindLabel = new Label(dialogArea, SWT.NONE);
		textToFindLabel.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		textToFindLabel.setText(DocometreMessages.FindDialogLabelTitle);

		textToFindText = new Text(dialogArea, SWT.BORDER);
		textToFindText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		textToFindText.setText(lastQuery);
		
		wholeWordButton = new Button(dialogArea, SWT.CHECK);
		wholeWordButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 2, 1));
		wholeWordButton.setText(DocometreMessages.FindDialogWholeWordButtonTitle);
		
		caseSensitiveButton = new Button(dialogArea, SWT.CHECK);
		caseSensitiveButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 2, 1));
		caseSensitiveButton.setText(DocometreMessages.FindDialogCaseSensitiveButtonTitle);
		
		searchBackButton = new Button(dialogArea, SWT.CHECK);
		searchBackButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false, 2, 1));
		searchBackButton.setText(DocometreMessages.FindDialogSearchBackwardButtonTitle);
		searchBackButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (offset > -1) {
					if(searchBackButton.getSelection()) offset -= textToFindText.getText().length();
					else offset += textToFindText.getText().length()+1;
				}
			}
		});
		
		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		Button findButton = createButton(parent, IDialogConstants.CLIENT_ID + 1, DocometreMessages.FindDialogButtonTitle, true);
		findButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(textViewer != null && !textViewer.getTextWidget().isDisposed()) {
					lastQuery = textToFindText.getText();
					offset = textViewer.getFindReplaceTarget().findAndSelect(offset, textToFindText.getText(), !searchBackButton.getSelection(), caseSensitiveButton.getSelection(), wholeWordButton.getSelection());
					if (offset == -1) PlatformUI.getWorkbench().getDisplay().beep();
					else {
						if(searchBackButton.getSelection()) offset -= textToFindText.getText().length();
						else offset += textToFindText.getText().length();
					}
					offsets.put(textViewer, offset);
				}
			}
		});
	}

}
