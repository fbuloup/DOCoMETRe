package fr.univamu.ism.docometre.analyse.editors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.SubToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.LabelProvider;
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
			Set<Channel> signals = new HashSet<>();
			for (String loadedSubject : loadedSubjects) {
				IResource subject = ((IContainer)SelectedExprimentContributionItem.selectedExperiment).findMember(loadedSubject.split("\\.")[1]);
				signals.addAll(Arrays.asList(MathEngineFactory.getMathEngine().getSignals(subject)));
			}
			ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(xyChartEditor.getSite().getShell(), new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((Channel)element).getFullName();
				}
			});
			elementListSelectionDialog.setMultipleSelection(false);
			elementListSelectionDialog.setElements(signals.toArray(new Channel[signals.size()]));
			elementListSelectionDialog.setTitle(DocometreMessages.XAxisSelectionDialogTitle);
			elementListSelectionDialog.setMessage(DocometreMessages.XAxisSelectionDialogMessage);
			if(elementListSelectionDialog.open() == Dialog.OK) {
				Object[] selection = elementListSelectionDialog.getResult();
				Channel xSignal = (Channel) selection[0];
				elementListSelectionDialog.setTitle(DocometreMessages.YAxisSelectionDialogTitle);
				elementListSelectionDialog.setMessage(DocometreMessages.YAxisSelectionDialogMessage);
				if(elementListSelectionDialog.open() == Dialog.OK) {
					selection = elementListSelectionDialog.getResult();
					Channel ySignal = (Channel) selection[0];
					xyChartEditor.getXYChartData().addCurve(xSignal, ySignal);
					xyChartEditor.refreshTrialsListFrontEndCuts();
					xyChartEditor.setDirty(true);
				}
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
			ElementListSelectionDialog elementListSelectionDialog = new ElementListSelectionDialog(xyChartEditor.getSite().getShell(), new LabelProvider());
			elementListSelectionDialog.setTitle(DocometreMessages.CurvesSelectionDialogTitle);
			elementListSelectionDialog.setMessage(DocometreMessages.CurvesSelectionDialogMessage);
			elementListSelectionDialog.setMultipleSelection(true);
			elementListSelectionDialog.setElements(xyChartEditor.getXYChartData().getSeriesIDsPrefixes());
			if(elementListSelectionDialog.open() == Dialog.OK) {
				String[] selection = Arrays.asList(elementListSelectionDialog.getResult()).toArray(new String[elementListSelectionDialog.getResult().length]);
				String[] seriesIDs = xyChartEditor.getSeriesIDs();
				for (String seriesID : seriesIDs) {
					for (String item : selection) {
						if(seriesID.startsWith(item)) {
							xyChartEditor.getChart().removeSeries(seriesID);
							xyChartEditor.getXYChartData().removeCurve(item);
						}
					}
				}
				xyChartEditor.getChart().redraw();
				xyChartEditor.refreshTrialsListFrontEndCuts();
				xyChartEditor.setDirty(true);
			}
		}
	}

	private ActionContributionItem nextTrialActionContributionItem;
	private ActionContributionItem previousTrialActionContributionItem;
	private SubToolBarManager subToolBarManager;
	private TrialsEditor editor;
	private XYChartEditor xyChartEditor;
	private ActionContributionItem addCurveActionContributionItem;
	private ActionContributionItem removeCurveActionContributionItem;

	public ChannelEditorActionBarContributor() {
		nextTrialActionContributionItem = new ActionContributionItem(new NextTrialAction());
		previousTrialActionContributionItem = new ActionContributionItem(new PreviousTrialAction());
		addCurveActionContributionItem = new ActionContributionItem(new AddCurveAction());
		removeCurveActionContributionItem = new ActionContributionItem(new RemoveCurveAction());
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
	}
	
	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		subToolBarManager = new SubToolBarManager(toolBarManager);
		
		subToolBarManager.add(previousTrialActionContributionItem);
		subToolBarManager.add(nextTrialActionContributionItem);
		subToolBarManager.add(addCurveActionContributionItem);
		subToolBarManager.add(removeCurveActionContributionItem);
	}
	
	@Override
	public void dispose() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().removePerspectiveListener(this);
		super.dispose();
	}
	
	@Override
	public void setActiveEditor(IEditorPart targetEditor) {
		editor = null;
		xyChartEditor = null;
		
		subToolBarManager.setVisible(false);
		
		nextTrialActionContributionItem.setVisible(false);
		previousTrialActionContributionItem.setVisible(false);
		addCurveActionContributionItem.setVisible(false);
		removeCurveActionContributionItem.setVisible(false);
		
		if(!(targetEditor instanceof TrialsEditor)) return;
		
		subToolBarManager.setVisible(true);
		
		nextTrialActionContributionItem.setVisible(true);
		previousTrialActionContributionItem.setVisible(true);
		editor = (TrialsEditor) targetEditor;
		if(editor instanceof XYChartEditor) {
			addCurveActionContributionItem.setVisible(true);
			removeCurveActionContributionItem.setVisible(true);
			xyChartEditor = (XYChartEditor) targetEditor;
		}
		
		subToolBarManager.update(true);
		
		if(subToolBarManager.getParent() != null) {
			subToolBarManager.getParent().update(true);
		}
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		boolean actionsVisibility = !perspective.getId().equals(AcquirePerspective.ID);
		
		nextTrialActionContributionItem.setVisible(actionsVisibility);
		previousTrialActionContributionItem.setVisible(actionsVisibility);
		addCurveActionContributionItem.setVisible(actionsVisibility);
		removeCurveActionContributionItem.setVisible(actionsVisibility);
		
		subToolBarManager.setVisible(actionsVisibility);
		
		nextTrialActionContributionItem.getAction().setEnabled(actionsVisibility);
		previousTrialActionContributionItem.getAction().setEnabled(actionsVisibility);
		addCurveActionContributionItem.getAction().setEnabled(actionsVisibility);
		removeCurveActionContributionItem.getAction().setEnabled(actionsVisibility);
		
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
