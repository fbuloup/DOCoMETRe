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
package fr.univamu.ism.docometre.views;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.WorkbenchJob;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ThemeColors;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class MessagesView extends ViewPart implements ILogListener, IDocumentListener {
	
	private static Color WARNING_COLOR = JFaceResources.getResources().createColor(new RGB(204,102,0));
	private static Color ERROR_COLOR = JFaceResources.getResources().createColor(new RGB(250, 0, 0));
	
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
	
	public static String ID = "Docometre.MessagesView";
	
	private TextViewer messagesViewer;

	private boolean autoScroll = true ;

	public MessagesView() {
		Activator.getDefault().getLog().addLogListener(this);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		Activator.getDefault().getLog().removeLogListener(this);
	}

	@Override
	public void createPartControl(Composite parent) {
		int styles = SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.READ_ONLY;
		messagesViewer = new SourceViewer(parent, null, styles);
		messagesViewer.setDocument(new Document());
		messagesViewer.getDocument().addDocumentListener(this);
		getViewSite().getActionBars().getToolBarManager().add(new ClearConsoleAction());
		getViewSite().getActionBars().getToolBarManager().add(new ScrollLockAction());
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				Activator.getDefault().getLog().removeLogListener(CachedLogger.getInstance());
				IStatus cachedStatus = CachedLogger.getInstance().get();
				while (cachedStatus != null) {
					putMessage(cachedStatus);
					if(Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.REDIRECT_STD_ERR_OUT_TO_FILE)) {
						System.out.println(cachedStatus.getMessage());
					}
					cachedStatus = CachedLogger.getInstance().get();
				}
			}
		});
	}

	@Override
	public void setFocus() {
		messagesViewer.getControl().setFocus();
	}

	@Override
	public void logging(IStatus status, String plugin) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				putMessage(status);
			}
		});
		
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		// TODO Auto-generated method stub
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		if(autoScroll) revealJob.schedule(50);
	}
	
	private void putMessage(IStatus status) {
		String message = status.getPlugin() + " : " + status.getMessage();
		int start = messagesViewer.getTextWidget().getCharCount();
		if(start > 0) {
			message = "\n" + message;
		}
		messagesViewer.getTextWidget().append(message);
		
		StyleRange styleRange = new StyleRange();
		styleRange.start = start;
		styleRange.length = message.length();
		styleRange.background = ThemeColors.getBackgroundColor();
		
		if(status.getSeverity() == IStatus.INFO) styleRange.fontStyle = SWT.NORMAL;
//			if(status.getSeverity() == IStatus.WARNING) styleRange.fontStyle = SWT.BOLD;
//			if(status.getSeverity() == IStatus.ERROR) styleRange.fontStyle = SWT.BOLD;
		if(status.getSeverity() == IStatus.INFO) styleRange.foreground = ThemeColors.getForegroundColor();
		if(status.getSeverity() == IStatus.WARNING) styleRange.foreground = WARNING_COLOR;
		if(status.getSeverity() == IStatus.ERROR) styleRange.foreground = ERROR_COLOR;
		
		messagesViewer.getTextWidget().setStyleRange(styleRange);
	}

}



