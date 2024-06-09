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
package fr.univamu.ism.docometre.analyse.views;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.WorkbenchJob;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.analyse.MathEngine;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class MathEngineCommandView extends ViewPart implements IDocumentListener {
	
	public static String ID = "Docometre.MathEngineCommandView";
	
	private Text commandText;
	private Button sendCommandButton;
	private TextViewer messagesViewer;
	private boolean autoScroll = true ;
	private ArrayList<String> cmdsArrayList = new ArrayList<String>(0);
	private int cmdIndex = 1;
	
	private class ScrollLockAction extends Action {
		public ScrollLockAction() {
			super(null, Action.AS_CHECK_BOX);
			setToolTipText(DocometreMessages.ScrollLockText);
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.SCROLL_LOCK_ICON));
		}
		@Override
		public void run() {
			autoScroll = !isChecked();
		}
	}
	
	private class ClearConsoleAction extends Action {
		public ClearConsoleAction() {
			setToolTipText(DocometreMessages.ClearMessagesText);
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.CLEAR_CONSOLE_ICON));
		}
		@Override
		public void run() {
			BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
				@Override
				public void run() {
					messagesViewer.getDocument().set("");
					messagesViewer.setSelectedRange(0, 0);
				}
			});
		}
	}
	
	private WorkbenchJob revealJob = new WorkbenchJob(DocometreMessages.RevealEODTitle) {//$NON-NLS-1$
        public IStatus runInUIThread(IProgressMonitor monitor) {
            StyledText textWidget = messagesViewer.getTextWidget();
            if (textWidget != null && !textWidget.isDisposed()) {
                int lineCount = textWidget.getLineCount();
                textWidget.setTopIndex(lineCount - 1);
            }
            return Status.OK_STATUS;
        }
    };

	public MathEngineCommandView() {
		cmdsArrayList.add("");
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(3, false));
		Label commandLabel = new Label(parent, SWT.NONE);
		commandLabel.setText(DocometreMessages.commandTitle);
		commandLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		commandText = new Text(parent, SWT.BORDER | SWT.SINGLE);
		commandText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		commandText.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.CR){
					sendCommandHandler();
				}
				if(e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.ARROW_UP) {
					if(e.keyCode == SWT.ARROW_UP) cmdIndex = (cmdIndex < cmdsArrayList.size()) ? cmdIndex + 1 : cmdIndex;	
					if(e.keyCode == SWT.ARROW_DOWN) cmdIndex = (cmdIndex > 1) ? cmdIndex - 1 : 1;
					String cmd = cmdsArrayList.get(cmdIndex-1);
					commandText.setText(cmd);
					commandText.setSelection(cmd.length());
					e.doit = false;
				}
				
			}
		});
		sendCommandButton = new Button(parent, SWT.FLAT);
		sendCommandButton.setText(DocometreMessages.sendButtonTitle);
		sendCommandButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		sendCommandButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sendCommandHandler();
			}
		});
		
		
		int styles = SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.READ_ONLY;
		messagesViewer = new SourceViewer(parent, null, styles);
		messagesViewer.setDocument(new Document());
		messagesViewer.getDocument().addDocumentListener(this);
		messagesViewer.getTextWidget().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		getViewSite().getActionBars().getToolBarManager().add(new ClearConsoleAction());
		getViewSite().getActionBars().getToolBarManager().add(new ScrollLockAction());
	}

	protected void sendCommandHandler() {
		if(commandText.getText().equals("")) return;
		MathEngine mathEngine = MathEngineFactory.getMathEngine();
		if(mathEngine.isStarted()) {
			try {
				String cmd = commandText.getText();
				String response = mathEngine.evaluate(cmd);
				messagesViewer.getTextWidget().append(response);
				messagesViewer.getTextWidget().append("\n");
				messagesViewer.getTextWidget().append("\n");
				commandText.setText("");
				cmdsArrayList.add(cmd);
				cmdIndex = cmdsArrayList.size();
			} catch (Exception e) {
				messagesViewer.getTextWidget().append(e.getMessage());
				messagesViewer.getTextWidget().append("\n");
				messagesViewer.getTextWidget().append("\n");
				e.printStackTrace();
			}
		} else messagesViewer.getTextWidget().append(DocometreMessages.PleaseStartMathEngineFirst + "\n");
		
	}

	@Override
	public void setFocus() {
		commandText.setFocus();
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		if(autoScroll) revealJob.schedule(50);
	}

}
