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
package fr.univamu.ism.docometre.history.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;

import fr.univamu.ism.docometre.history.ui.HistoryOperations;
import fr.univamu.ism.docometre.history.ui.SnapshotsView;

/**
 * Handler of an experiment history command : the experiment is the one of the selected
 * resource or, when snapshots view is active, the one shown in this view.
 */
public abstract class AbstractHistoryHandler extends AbstractHandler {
	
	@Override
	public void setEnabled(Object evaluationContext) {
		IProject experiment = null;
		if(evaluationContext instanceof IEvaluationContext) {
			IEvaluationContext context = (IEvaluationContext) evaluationContext;
			experiment = HistoryOperations.getExperiment(context.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME));
			Object part = context.getVariable(ISources.ACTIVE_PART_NAME);
			if(experiment == null && part instanceof SnapshotsView) experiment = ((SnapshotsView)part).getExperiment();
		}
		setBaseEnabled(isEnabled(experiment));
	}
	
	protected IProject getExperiment(ExecutionEvent event) {
		IProject experiment = HistoryOperations.getExperiment(HandlerUtil.getCurrentSelection(event));
		if(experiment == null && HandlerUtil.getActivePart(event) instanceof SnapshotsView) experiment = ((SnapshotsView)HandlerUtil.getActivePart(event)).getExperiment();
		return experiment;
	}
	
	/*
	 * Return true if command can be executed on experiment (which may be null)
	 */
	protected abstract boolean isEnabled(IProject experiment);

}
