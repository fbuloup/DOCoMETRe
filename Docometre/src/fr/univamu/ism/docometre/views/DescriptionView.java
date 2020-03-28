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

import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import fr.univamu.ism.docometre.ApplicationActionBarAdvisor;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;

public class DescriptionView extends ViewPart implements ISelectionListener, IPartListener {
	
	private static int COMPLETION_HEIGHT = 15;
	
	// Colors IDs
	private static final String BACKGROUND_COLOR = "BACKGROUND_COLOR";
	private static final String BORDER_COLOR = "BORDER_COLOR";
	private static final String MARK_TRIAL_COLOR = "MARK_TRIAL_COLOR";
	
	static {
		JFaceResources.getColorRegistry().put(BORDER_COLOR, new RGB(139,0,0));
		JFaceResources.getColorRegistry().put(BACKGROUND_COLOR, new RGB(255, 255, 255));
		JFaceResources.getColorRegistry().put(MARK_TRIAL_COLOR, new RGB(139,0,0));
	}

	class Completion extends Canvas implements PaintListener {
		private IResource resource;

		public Completion(Composite container, int style, IResource resource) {
			super(container, style);
			addPaintListener(this);
			this.resource = resource;
		}

		@Override
		public void paintControl(PaintEvent e) {
			
			// Get total nb trials and done trials
			String toolTip = "";
			int nbTrials = ResourceProperties.getTotalNumberOfTrials(resource);
			if(nbTrials == 0) toolTip = "No trials";
			int[] undoneTrials = ResourceProperties.getUndoneTrialsNumbers(resource);
			Arrays.sort(undoneTrials);
			
			// Create image and tool tip
			Rectangle rectangle = new Rectangle(0, 0, nbTrials==0?100:nbTrials, COMPLETION_HEIGHT);
			Image image = new Image(getDisplay(), rectangle);
			GC gc = new GC(image);
			gc.setBackground(JFaceResources.getColorRegistry().get(BACKGROUND_COLOR));
			gc.fillRectangle(0, 0, image.getBounds().width , image.getBounds().height );
			gc.setForeground(JFaceResources.getColorRegistry().get(MARK_TRIAL_COLOR));
			int lastUndoneTrial = 0;
			
			boolean isFollowing = false;
			if(undoneTrials.length > 0) {
				for (int i = 0; i < undoneTrials.length; i++) {
					// Construct tool tip
					if(lastUndoneTrial == 0) {
						lastUndoneTrial = undoneTrials[i];
						toolTip = DocometreMessages.UndoneTrials + String.valueOf(undoneTrials[i]);
					} else {
						if(undoneTrials[i] - 1 == lastUndoneTrial) {
							isFollowing = true;
							lastUndoneTrial = undoneTrials[i];
							if(i == undoneTrials.length - 1) toolTip = toolTip + "-" + String.valueOf(lastUndoneTrial);
						} else {
							if(isFollowing) {
								toolTip = toolTip + "-" + String.valueOf(lastUndoneTrial);
								isFollowing = false;
							} 
							toolTip = toolTip + "," + undoneTrials[i];
							lastUndoneTrial = undoneTrials[i];
						}
					}
					// Mark undone trial
					gc.drawLine(undoneTrials[i] - 1, 0, undoneTrials[i] - 1, image.getBounds().height - 1);
				}
			}
			
			// Copy image in canvas
			e.gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 1, 0, getClientArea().width - 2, getClientArea().height);
			image.dispose();
			
			// Draw black round rectangle
			e.gc.setForeground(JFaceResources.getColorRegistry().get(BORDER_COLOR));
			e.gc.setLineWidth(1);
			e.gc.drawRoundRectangle(0, 0, getClientArea().width - 1, getClientArea().height - 1, 6, 6);

			setToolTipText(toolTip);
		}
		
		
	}
	
	public static String ID = "Docometre.DescriptionView";

	private StyledText descriptionStyledText;
	private CLabel selectedResourceLabel;

	private IResource selectedResource;
	private Completion completion;

	private Composite parentContainer;

	/*
	 * Refresh description view 
	 */
	public static void refresh() {
		DescriptionView descriptionView = (DescriptionView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(DescriptionView.ID);
		if(descriptionView != null) descriptionView.refreshDescription();
	}
	
	public DescriptionView() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(this);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		parent.setLayout(gridLayout);
		
		parentContainer = parent;
		
		int styles = SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.WRAP;
		selectedResourceLabel = new CLabel(parent, SWT.NONE);
		selectedResourceLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		descriptionStyledText = new StyledText(parent, styles);
		descriptionStyledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		descriptionStyledText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				//ApplicationActionBarAdvisor.editDescriptionAction.setChecked(ApplicationActionBarAdvisor.editDescriptionAction.isEnabled());
			}
		});
		descriptionStyledText.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent event) {
				ApplicationActionBarAdvisor.editDescriptionAction.setChecked(false);
				ApplicationActionBarAdvisor.editDescriptionAction.run();
			}
			@Override
			public void focusGained(FocusEvent event) {
				descriptionStyledText.setEditable(selectedResource != null);
				ApplicationActionBarAdvisor.editDescriptionAction.setChecked(ApplicationActionBarAdvisor.editDescriptionAction.isEnabled() && selectedResource != null);
			}
		});
		
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if(activePage != null) {
			ExperimentsView experimentsView = (ExperimentsView) activePage.findView(ExperimentsView.ID);
			if(experimentsView != null) selectionChanged(experimentsView, experimentsView.getSelection());
		} 
		getViewSite().getActionBars().getToolBarManager().add(ApplicationActionBarAdvisor.editDescriptionAction);
	}

	@Override
	public void setFocus() {
		descriptionStyledText.setFocus();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		selectedResource = null;
		if(ApplicationActionBarAdvisor.editDescriptionAction.isChecked()) ApplicationActionBarAdvisor.editDescriptionAction.setChecked(false);
		selectedResourceLabel.setText(DocometreMessages.SelectedResourceNone);
		descriptionStyledText.setText("");
		if(completion != null && !completion.isDisposed()) completion.dispose();
		if(!selection.isEmpty()) {
			selectedResource = (IResource)((IStructuredSelection)selection).getFirstElement();
			if(ResourceType.isSubject(selectedResource) || ResourceType.isSession(selectedResource)) {
				completion = new Completion(parentContainer, SWT.NONE, selectedResource);
				completion.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				completion.moveAbove(descriptionStyledText);
				((GridData)completion.getLayoutData()).heightHint = COMPLETION_HEIGHT;
			}
			selectedResourceLabel.setText(DocometreMessages.SelectedResource + GetResourceLabelDelegate.getLabel(selectedResource));
			refreshDescription();
		}
		parentContainer.layout(true);
	}
	
	public String getDescription() {
		return descriptionStyledText.getText();
	}

	public void partActivated(IWorkbenchPart part) {
	}

	public void partBroughtToTop(IWorkbenchPart part) {
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		if(part instanceof DescriptionView || part instanceof ExperimentsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().removeSelectionListener(ExperimentsView.ID, this);
		if(part instanceof DescriptionView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(this);
	}

	public void partDeactivated(IWorkbenchPart part) {
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
		if(part instanceof DescriptionView || part instanceof ExperimentsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(ExperimentsView.ID, this);
		if(part instanceof ExperimentsView) {
			selectedResourceLabel.setText(DocometreMessages.SelectedResourceNone);
			descriptionStyledText.setText("");
		}
	}

	public void refreshDescription() {
		if(selectedResource == null || !selectedResource.exists()) {
			descriptionStyledText.setText("");
			return;
		}
		String value = ResourceProperties.getDescriptionPersistentProperty(selectedResource);
		if(value != null ) descriptionStyledText.setText(value);
		else descriptionStyledText.setText("");
	}
	
}
