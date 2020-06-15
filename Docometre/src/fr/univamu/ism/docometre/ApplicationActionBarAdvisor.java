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
package fr.univamu.ism.docometre;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import fr.univamu.ism.docometre.actions.CompileProcessAction;
import fr.univamu.ism.docometre.actions.CopyResourcesAction;
import fr.univamu.ism.docometre.actions.DeleteResourcesAction;
import fr.univamu.ism.docometre.actions.EditDescriptionAction;
import fr.univamu.ism.docometre.actions.EditSessionAction;
import fr.univamu.ism.docometre.actions.GotoEndAction;
import fr.univamu.ism.docometre.actions.GotoStartAction;
import fr.univamu.ism.docometre.actions.NewDACQConfigurationAction;
import fr.univamu.ism.docometre.actions.NewExperimentAction;
import fr.univamu.ism.docometre.actions.NewFolderAction;
import fr.univamu.ism.docometre.actions.NewProcessAction;
import fr.univamu.ism.docometre.actions.NewSessionAction;
import fr.univamu.ism.docometre.actions.NewSubjectAction;
import fr.univamu.ism.docometre.actions.NewTrialAction;
import fr.univamu.ism.docometre.actions.NextAction;
import fr.univamu.ism.docometre.actions.OpenEditorAction;
import fr.univamu.ism.docometre.actions.OpenProcessWithSystemEditorAction;
import fr.univamu.ism.docometre.actions.PasteResourcesAction;
import fr.univamu.ism.docometre.actions.PreviousAction;
import fr.univamu.ism.docometre.actions.RefreshResourceAction;
import fr.univamu.ism.docometre.actions.RenameResourceAction;
import fr.univamu.ism.docometre.actions.SelectItemToRunAction;
import fr.univamu.ism.docometre.actions.SetDACQConfigurationAsDefaultAction;
import fr.univamu.ism.docometre.actions.ShowDescriptionViewAction;
import fr.univamu.ism.docometre.actions.ShowExperimentsViewAction;
import fr.univamu.ism.docometre.actions.ShowMessagesViewAction;
import fr.univamu.ism.docometre.actions.ShowProgressViewAction;
import fr.univamu.ism.docometre.actions.TestJZY3DAction;
import fr.univamu.ism.docometre.actions.TestOpenGLAction;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.editors.ChartContributionItem;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {
    
	private IWorkbenchAction undoAction;
	private IWorkbenchAction redoAction;
	private IWorkbenchAction deleteAction;
	private IWorkbenchAction copyAction;
	private IWorkbenchAction pasteAction;
	private IWorkbenchAction renameAction;
	private IWorkbenchAction refreshAction;
	private IWorkbenchAction selectAllAction;
	
	public static ShowExperimentsViewAction showExperimentsViewAction;
	public static ShowMessagesViewAction showMessagesViewAction;
	public static ShowDescriptionViewAction showDescriptionViewAction;
	public static ShowProgressViewAction showProgressViewAction;
	public static DeleteResourcesAction deleteResourcesAction;
	public static CopyResourcesAction copyResourcesAction;
	public static PasteResourcesAction pasteResourcesAction;
	public static RenameResourceAction renameResourceAction;
	public static NewExperimentAction newExperimentAction;
	public static NewDACQConfigurationAction newDAQConfigurationAction;
	public static NewFolderAction newFolderAction;
	public static NewProcessAction newProcessAction;
	public static NewSessionAction newSessionAction;
	public static NewSubjectAction newSubjectAction;
	public static NewTrialAction newTrialAction;
	public static OpenEditorAction openEditorAction;
	public static OpenProcessWithSystemEditorAction openProcessWithSystemEditorAction;
	public static CompileProcessAction compileProcessAction;
	public static SetDACQConfigurationAsDefaultAction setDACQConfigurationAsDefaultAction;
	public static EditDescriptionAction editDescriptionAction;
	public static IWorkbenchAction aboutAction;
	public static NextAction nextAction;
	public static PreviousAction previousAction;
	public static GotoEndAction gotoEndAction;
	public static GotoStartAction gotoStartAction;
	public static SelectItemToRunAction selectItemToRunAction;
	public static EditSessionAction editSessionAction;
	public static RefreshResourceAction refreshResourceAction;
	
	public static TestOpenGLAction testOpenGLAction;
	public static TestJZY3DAction testJZY3DAction;
	
	public static WorkloadTimeContributionItem workloadTimeContributionItem;
	public static PausePendingContributionItem pausePendingContributionItem;
	public static ChartContributionItem cursorContributionItem;
	public static ChartContributionItem markerContributionItem;
	public static ChartContributionItem deltaContributionItem;
	public static SelectedExprimentContributionItem selectedExprimentContributionItem;
	
	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
        super(configurer);
    }

	@Override
    protected void makeActions(IWorkbenchWindow window) {
		/*Globals actions*/
    	undoAction = ActionFactory.UNDO.create(window);
    	register(undoAction);
    	redoAction = ActionFactory.REDO.create(window);
    	register(redoAction);
    	deleteAction = ActionFactory.DELETE.create(window);
    	register(deleteAction);
    	copyAction = ActionFactory.COPY.create(window);
    	register(copyAction);
    	pasteAction = ActionFactory.PASTE.create(window);
    	register(pasteAction);
    	renameAction = ActionFactory.RENAME.create(window);
    	register(renameAction);
    	refreshAction = ActionFactory.REFRESH.create(window);
    	register(refreshAction);
    	selectAllAction = ActionFactory.SELECT_ALL.create(window);
    	register(selectAllAction);
    	
    	/*Experiments view actions*/
    	showExperimentsViewAction = new ShowExperimentsViewAction();
    	register(showExperimentsViewAction);
    	deleteResourcesAction = new DeleteResourcesAction(window);
    	register(deleteResourcesAction);
    	copyResourcesAction = new CopyResourcesAction(window);
    	register(copyResourcesAction);
    	pasteResourcesAction = new PasteResourcesAction(window, copyResourcesAction);
    	register(pasteResourcesAction);
    	renameResourceAction = new RenameResourceAction(window);
    	register(renameResourceAction);
    	refreshResourceAction = new RefreshResourceAction(window);
    	register(refreshResourceAction);
    	newExperimentAction = new NewExperimentAction();
    	register(newExperimentAction);
    	newDAQConfigurationAction = new NewDACQConfigurationAction(window);
    	register(newDAQConfigurationAction);
    	newFolderAction = new NewFolderAction(window);
    	register(newFolderAction);
    	newProcessAction = new NewProcessAction(window);
    	register(newProcessAction);
    	newSessionAction = new NewSessionAction(window);
    	register(newSessionAction);
    	newSubjectAction = new NewSubjectAction(window);
    	register(newSubjectAction);
    	newTrialAction = new NewTrialAction(window);
    	register(newTrialAction);
    	openEditorAction = new OpenEditorAction(window);
    	register(openEditorAction);
    	openProcessWithSystemEditorAction = new OpenProcessWithSystemEditorAction(window);
    	register(openProcessWithSystemEditorAction);
    	compileProcessAction = new CompileProcessAction(window);
    	register(compileProcessAction);
    	setDACQConfigurationAsDefaultAction = new SetDACQConfigurationAsDefaultAction(window);
    	register(setDACQConfigurationAsDefaultAction);
    	editDescriptionAction = new EditDescriptionAction(window);
    	register(editDescriptionAction);
    	editSessionAction = new EditSessionAction(window);
    	register(editSessionAction);
    	
    	/*Messages View actions*/
    	showMessagesViewAction = new ShowMessagesViewAction();
    	register(showMessagesViewAction);
    	
    	/*Description View actions*/
    	showDescriptionViewAction = new ShowDescriptionViewAction();
    	register(showDescriptionViewAction);
    	
    	/*Progress View actions*/
    	showProgressViewAction = new ShowProgressViewAction();
    	register(showProgressViewAction);
    	
    	/* Experiment scheduler Actions */
    	nextAction = new NextAction();
    	register(nextAction);
    	previousAction = new PreviousAction();
    	register(previousAction);
    	gotoEndAction = new GotoEndAction();
    	register(gotoEndAction);
    	gotoStartAction= new GotoStartAction();
    	register(gotoStartAction);
    	selectItemToRunAction = new SelectItemToRunAction();
    	register(selectItemToRunAction);
    	
    	
    	testOpenGLAction = new TestOpenGLAction();
    	register(testOpenGLAction);
    	
    	testJZY3DAction = new TestJZY3DAction();
    	register(testJZY3DAction);
	}
	
	@Override
	protected void fillStatusLine(IStatusLineManager statusLine) {
		
		deltaContributionItem = new ChartContributionItem("deltaContributionItem");
		statusLine.appendToGroup(StatusLineManager.MIDDLE_GROUP, deltaContributionItem);
		
		markerContributionItem = new ChartContributionItem("markerContributionItem");
		statusLine.appendToGroup(StatusLineManager.MIDDLE_GROUP, markerContributionItem);
		
		cursorContributionItem = new ChartContributionItem("cursorContributionItem");
		statusLine.appendToGroup(StatusLineManager.MIDDLE_GROUP, cursorContributionItem);
		
		pausePendingContributionItem = new PausePendingContributionItem("PausePendingContributionItem");
		statusLine.appendToGroup(StatusLineManager.MIDDLE_GROUP, pausePendingContributionItem);
		
		workloadTimeContributionItem = new WorkloadTimeContributionItem("WorkloadTimeContributionItem");
		statusLine.appendToGroup(StatusLineManager.MIDDLE_GROUP, workloadTimeContributionItem);
		
		selectedExprimentContributionItem = new SelectedExprimentContributionItem("selectedExprimentContributionItem");
		statusLine.appendToGroup(StatusLineManager.BEGIN_GROUP, selectedExprimentContributionItem);
		
		super.fillStatusLine(statusLine);
	}
	
}
