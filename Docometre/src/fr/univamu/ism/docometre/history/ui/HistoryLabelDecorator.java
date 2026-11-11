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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.history.GitResources;
import fr.univamu.ism.docometre.history.ui.HistoryStatusCache.ExperimentStatus;

/**
 * Decorations of historized experiments :
 *  - "> " prefix on experiment, folders and files changed since the last snapshot,
 *  - last snapshot number and number of changes after experiment name.
 * Statuses are read from HistoryStatusCache, decorations are updated when they are computed.
 */
public class HistoryLabelDecorator implements ILightweightLabelDecorator {
	
	public static final String ID = "Docometre.HistoryDecorator";
	public static final String CHANGED_PREFIX = "> ";

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if(!(element instanceof IResource)) return;
		IResource resource = (IResource) element;
		IProject project;
		IPath projectRelativePath;
		try {
			project = resource.getProject();
			projectRelativePath = resource.getProjectRelativePath();
		} catch (RuntimeException e) {
			// e.g. channels which are not workspace resources
			return;
		}
		if(project == null || projectRelativePath == null || !project.isOpen() || GitResources.isGitMetadata(resource)) return;
		ExperimentStatus status = HistoryStatusCache.getInstance().getStatus(project);
		if(status == null || !status.historized()) return;
		if(status.isChanged(projectRelativePath.toPortableString())) decoration.addPrefix(CHANGED_PREFIX);
		if(resource instanceof IProject) decoration.addSuffix(getSuffix(status));
	}
	
	private static String getSuffix(ExperimentStatus status) {
		if(status.lastSnapshot() == null) return DocometreMessages.HistoryDecorator_NoSnapshot;
		if(status.changesNumber() == 0) return NLS.bind(DocometreMessages.HistoryDecorator_Snapshot, status.lastSnapshot().number());
		return NLS.bind(DocometreMessages.HistoryDecorator_SnapshotAndChanges, status.lastSnapshot().number(), status.changesNumber());
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

}
