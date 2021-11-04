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

public class ChannelEditorActionBarContributor extends EditorActionBarContributor implements IPerspectiveListener {
	
	private class NextTrialAction extends Action {
		public NextTrialAction() {
			super("Next trial", Activator.getImageDescriptor(IImageKeys.NEXT_ICON));
		}
		
		@Override
		public void run() {
			editor.gotoNextTrial();
		}
	}
	
	private class PreviousTrialAction extends Action {
		public PreviousTrialAction() {
			super("Previous trial", Activator.getImageDescriptor(IImageKeys.PREVIOUS_ICON));
		}
		@Override
		public void run() {
			editor.gotoPreviousTrial();
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
