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
package fr.univamu.ism.docometre.analyse.editors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.SubToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.part.EditorActionBarContributor;

import fr.univamu.ism.docometre.AcquirePerspective;
import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.XYZChart;
import fr.univamu.ism.docometre.analyse.wizard.SelectChannelsWizard;
import fr.univamu.ism.docometre.analyse.wizard.SelectChannelsWizard.ChannelsNumber;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class ChannelEditorActionBarContributor extends EditorActionBarContributor implements IPerspectiveListener {
	
	private class NextTrialAction extends Action {
		public NextTrialAction() {
			super("Next trial", Activator.getImageDescriptor(IImageKeys.NEXT_ICON));
		}
		
		@Override
		public void run() {
			boolean synchronize = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SYNCHRONIZE_CHARTS_WHEN_TRIAL_CHANGE);
			if(!synchronize) editor.gotoNextTrial();
			else {
				IEditorReference[] editorsRef = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
				for (IEditorReference editorReference : editorsRef) {
					if(editorReference.getEditor(false) instanceof TrialsEditor) ((TrialsEditor)editorReference.getEditor(false)).gotoNextTrial();
				}
			}
		}
	}
	
	private class PreviousTrialAction extends Action {
		public PreviousTrialAction() {
			super("Previous trial", Activator.getImageDescriptor(IImageKeys.PREVIOUS_ICON));
		}
		@Override
		public void run() {
			boolean synchronize = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SYNCHRONIZE_CHARTS_WHEN_TRIAL_CHANGE);
			if(!synchronize) editor.gotoPreviousTrial();
			else {
				IEditorReference[] editorsRef = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
				for (IEditorReference editorReference : editorsRef) {
					if(editorReference.getEditor(false) instanceof TrialsEditor) ((TrialsEditor)editorReference.getEditor(false)).gotoPreviousTrial();
				}
			}
		}
	}
	
	private class AddCurveAction extends Action {
		public AddCurveAction() {
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.ADD_ICON));
			setToolTipText(DocometreMessages.AddNewCurveToolTip);
		}
		@Override
		public void run() {
			if(!MathEngineFactory.getMathEngine().isStarted()) return;
			String[] loadedSubjects = MathEngineFactory.getMathEngine().getLoadedSubjects();
			if(loadedSubjects.length == 0) {
				MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DocometreMessages.AddCurvesDialog_ShellTitle, DocometreMessages.NoSubjectsLoadedMessage);
				return;
			}
			Set<Channel> signals = new HashSet<>();
			for (String loadedSubject : loadedSubjects) {
				IResource subject = ((IContainer)SelectedExprimentContributionItem.selectedExperiment).findMember(loadedSubject.split("\\.")[1]);
				signals.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getSignals(subject)));
			}
			
			ChannelsNumber channelsNumber = ChannelsNumber.ONE;
			if(chartEditor instanceof XYChartEditor) channelsNumber = ChannelsNumber.TWO;
			if(chartEditor instanceof XYZChartEditor) channelsNumber = ChannelsNumber.THREE;
			SelectChannelsWizard selectChannelsWizard = new SelectChannelsWizard(channelsNumber, signals.toArray(new Channel[signals.size()]));
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			WizardDialog selectChannelsWizardDialog = new WizardDialog(shell, selectChannelsWizard);
			if(selectChannelsWizardDialog.open() == Dialog.OK) {
				Channel[] channels = selectChannelsWizard.getSelectedChannels();
				Channel xSignal = (Channel) channels[0];
				Channel ySignal = (Channel) channels[0];
				Channel zSignal = (Channel) channels[0];
				if(channelsNumber == ChannelsNumber.TWO) {
					ySignal = (Channel) channels[1];
					chartEditor.getChartData().addCurve(xSignal, ySignal);
				}
				if(channelsNumber == ChannelsNumber.THREE) {
					ySignal = (Channel) channels[1];
					zSignal = (Channel) channels[2];
					((XYZChart)chartEditor.getChartData()).addCurve(xSignal, ySignal, zSignal);
				}
				chartEditor.refreshTrialsListFrontEndCuts();
				chartEditor.setDirty(true);
			}
		}
	}
	
	private class RemoveCurveAction extends Action {
		public RemoveCurveAction() {
			setImageDescriptor(Activator.getImageDescriptor(IImageKeys.REMOVE_ICON));
			setToolTipText(DocometreMessages.RemoveCurveToolTip);
		}
		@Override
		public void run() {
			if(!MathEngineFactory.getMathEngine().isStarted()) return;
			ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), new LabelProvider());
			elementListSelectionDialog.setTitle(DocometreMessages.CurvesSelectionDialogTitle);
			elementListSelectionDialog.setMessage(DocometreMessages.CurvesSelectionDialogMessage);
			elementListSelectionDialog.setMultipleSelection(true);
			elementListSelectionDialog.setElements(chartEditor.getChartData().getSeriesIDsPrefixes());
			if(elementListSelectionDialog.open() == Dialog.OK) {
				String[] selection = Arrays.asList(elementListSelectionDialog.getResult()).toArray(new String[elementListSelectionDialog.getResult().length]);
				String[] seriesIDs = chartEditor.getSeriesIDs();
				for (String seriesID : seriesIDs) {
					for (String item : selection) {
						if(seriesID.startsWith(item)) {
							chartEditor.removeSeries(seriesID);
							chartEditor.getChartData().removeCurve(item);
						}
					}
				}
				chartEditor.redraw();
				chartEditor.refreshTrialsListFrontEndCuts();
				chartEditor.setDirty(true);
			}
		}
	}

	private class UpAction extends Action {
		public UpAction() {
			super("Up", Activator.getImageDescriptor(IImageKeys.UP));
		}
		
		@Override
		public void run() {
			((XYZChartEditor)chartEditor).up();
		}
	}
	
	private class DownAction extends Action {
		public DownAction() {
			super("Down", Activator.getImageDescriptor(IImageKeys.DOWN));
		}
		
		@Override
		public void run() {
			((XYZChartEditor)chartEditor).down();
		}
	}
	
	private class RightAction extends Action {
		public RightAction() {
			super("Right", Activator.getImageDescriptor(IImageKeys.RIGHT));
		}
		
		@Override
		public void run() {
			((XYZChartEditor)chartEditor).right();
		}
	}
	
	private class LeftAction extends Action {
		public LeftAction() {
			super("Left", Activator.getImageDescriptor(IImageKeys.LEFT));
		}
		
		@Override
		public void run() {
			((XYZChartEditor)chartEditor).left();
		}
	}
	
//	private class ZoomInAction extends Action {
//	public ZoomInAction() {
//		super("Zoom In", Activator.getImageDescriptor(IImageKeys.ZOOM_IN2));
//	}
//	
//	@Override
//	public void run() {
//		((XYZChartEditor)chartEditor).zoomIn();
//	}
//}
//
//private class ZoomOutAction extends Action {
//	public ZoomOutAction() {
//		super("Zoom Out", Activator.getImageDescriptor(IImageKeys.ZOOM_OUT2));
//	}
//	
//	@Override
//	public void run() {
//		((XYZChartEditor)chartEditor).zoomOut();
//	}
//}
//
//private class ZoomFitAction extends Action {
//	public ZoomFitAction() {
//		super("Zoom to Fit", Activator.getImageDescriptor(IImageKeys.ZOOM_FIT));
//	}
//	
//	@Override
//	public void run() {
//		((XYZChartEditor)chartEditor).zoomToFit();
//	}
//}
	
//	private class TurnLeftAction extends Action {
//		public TurnLeftAction() {
//			super("Turn Left", Activator.getImageDescriptor(IImageKeys.TURN_LEFT));
//		}
//		
//		@Override
//		public void run() {
//			((XYZChartEditor)chartEditor).turnLeft();
//		}
//	}
//	
//	private class TurnRightAction extends Action {
//		public TurnRightAction() {
//			super("Turn Right", Activator.getImageDescriptor(IImageKeys.TURN_RIGHT));
//		}
//		
//		@Override
//		public void run() {
//			((XYZChartEditor)chartEditor).turnRight();
//		}
//	}
	
	private ActionContributionItem nextTrialActionContributionItem;
	private ActionContributionItem previousTrialActionContributionItem;
	private SubToolBarManager subToolBarManager;
	private TrialsEditor editor;
	private Chart2D3DBehaviour chartEditor;
	private ActionContributionItem addCurveActionContributionItem;
	private ActionContributionItem removeCurveActionContributionItem;
	private ActionContributionItem upActionContributionItem;
	private ActionContributionItem downActionContributionItem;
	private ActionContributionItem leftActionContributionItem;
	private ActionContributionItem rightActionContributionItem;
//	private ActionContributionItem zoomInActionContributionItem;
//	private ActionContributionItem zoomOutActionContributionItem;
//	private ActionContributionItem zoomFitActionContributionItem;
//	private ActionContributionItem turnLeftActionContributionItem;
//	private ActionContributionItem turnRightActionContributionItem;

	public ChannelEditorActionBarContributor() {
		nextTrialActionContributionItem = new ActionContributionItem(new NextTrialAction());
		previousTrialActionContributionItem = new ActionContributionItem(new PreviousTrialAction());
		addCurveActionContributionItem = new ActionContributionItem(new AddCurveAction());
		removeCurveActionContributionItem = new ActionContributionItem(new RemoveCurveAction());
		upActionContributionItem = new ActionContributionItem(new UpAction());
		downActionContributionItem = new ActionContributionItem(new DownAction());
		leftActionContributionItem = new ActionContributionItem(new LeftAction());
		rightActionContributionItem = new ActionContributionItem(new RightAction());
//		zoomInActionContributionItem = new ActionContributionItem(new ZoomInAction());
//		zoomOutActionContributionItem = new ActionContributionItem(new ZoomOutAction());
//		zoomFitActionContributionItem = new ActionContributionItem(new ZoomFitAction());
//		turnLeftActionContributionItem = new ActionContributionItem(new TurnLeftAction());
//		turnRightActionContributionItem = new ActionContributionItem(new TurnRightAction());
		
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
	}
	
	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		subToolBarManager = new SubToolBarManager(toolBarManager);
		
		subToolBarManager.add(previousTrialActionContributionItem);
		subToolBarManager.add(nextTrialActionContributionItem);
		subToolBarManager.add(new Separator());
		subToolBarManager.add(addCurveActionContributionItem);
		subToolBarManager.add(removeCurveActionContributionItem);
		subToolBarManager.add(new Separator());
		subToolBarManager.add(upActionContributionItem);
		subToolBarManager.add(downActionContributionItem);
		subToolBarManager.add(leftActionContributionItem);
		subToolBarManager.add(rightActionContributionItem);
//		subToolBarManager.add(zoomInActionContributionItem);
//		subToolBarManager.add(zoomOutActionContributionItem);
//		subToolBarManager.add(zoomFitActionContributionItem);
//		subToolBarManager.add(turnLeftActionContributionItem);
//		subToolBarManager.add(turnRightActionContributionItem);
		
	}
	
	@Override
	public void dispose() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().removePerspectiveListener(this);
		super.dispose();
	}
	
	@Override
	public void setActiveEditor(IEditorPart targetEditor) {
		editor = null;
		chartEditor = null;
		
		subToolBarManager.setVisible(false);
		
		nextTrialActionContributionItem.setVisible(false);
		previousTrialActionContributionItem.setVisible(false);
		
		addCurveActionContributionItem.setVisible(false);
		removeCurveActionContributionItem.setVisible(false);
		
		upActionContributionItem.setVisible(false);
		downActionContributionItem.setVisible(false);
		leftActionContributionItem.setVisible(false);
		rightActionContributionItem.setVisible(false);
//		zoomInActionContributionItem.setVisible(false);
//		zoomOutActionContributionItem.setVisible(false);
//		zoomFitActionContributionItem.setVisible(false);
//		turnLeftActionContributionItem.setVisible(false);
//		turnRightActionContributionItem.setVisible(false);
		
		if(!(targetEditor instanceof TrialsEditor)) return;
		
		subToolBarManager.setVisible(true);
		
		nextTrialActionContributionItem.setVisible(true);
		previousTrialActionContributionItem.setVisible(true);
		
		editor = (TrialsEditor) targetEditor;
		if(editor instanceof Chart2D3DBehaviour) {
			addCurveActionContributionItem.setVisible(true);
			removeCurveActionContributionItem.setVisible(true);
			chartEditor = (Chart2D3DBehaviour) targetEditor;
		}
		
		if(editor instanceof XYZChartEditor) {
			upActionContributionItem.setVisible(true);
			downActionContributionItem.setVisible(true);
			leftActionContributionItem.setVisible(true);
			rightActionContributionItem.setVisible(true);
//			zoomInActionContributionItem.setVisible(true);
//			zoomOutActionContributionItem.setVisible(true);
//			zoomFitActionContributionItem.setVisible(true);
//			turnLeftActionContributionItem.setVisible(true);
//			turnRightActionContributionItem.setVisible(true);
		}
		subToolBarManager.update(true);
		
		if(subToolBarManager.getParent() != null) {
			subToolBarManager.getParent().update(true);
		}
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		boolean actionsVisibility = !perspective.getId().equals(AcquirePerspective.id);
		
		nextTrialActionContributionItem.setVisible(actionsVisibility);
		previousTrialActionContributionItem.setVisible(actionsVisibility);
		addCurveActionContributionItem.setVisible(actionsVisibility);
		removeCurveActionContributionItem.setVisible(actionsVisibility);
		
		subToolBarManager.setVisible(actionsVisibility);
		
		nextTrialActionContributionItem.getAction().setEnabled(actionsVisibility);
		previousTrialActionContributionItem.getAction().setEnabled(actionsVisibility);
		addCurveActionContributionItem.getAction().setEnabled(actionsVisibility);
		removeCurveActionContributionItem.getAction().setEnabled(actionsVisibility);
		
		upActionContributionItem.setVisible(actionsVisibility);
		downActionContributionItem.setVisible(actionsVisibility);
		leftActionContributionItem.setVisible(actionsVisibility);
		rightActionContributionItem.setVisible(actionsVisibility);
//		zoomInActionContributionItem.setVisible(actionsVisibility);
//		zoomOutActionContributionItem.setVisible(actionsVisibility);
//		zoomFitActionContributionItem.setVisible(actionsVisibility);
//		turnLeftActionContributionItem.setVisible(actionsVisibility);
//		turnRightActionContributionItem.setVisible(actionsVisibility);
		
		subToolBarManager.update(true);
		
		if(subToolBarManager.getParent() != null) {
			subToolBarManager.getParent().update(true);
		}
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		// TODO Auto-generated method stub
		
	}

}
