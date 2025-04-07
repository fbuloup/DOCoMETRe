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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.GetResourceLabelDelegate;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;

public class ExperimentsLabelProvider implements ILabelProvider {

	public void addListener(ILabelProviderListener listener) {
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
	}

	public Image getImage(Object element) {
		if(!(element instanceof IResource)) return null;
		IResource resource = (IResource) element;
		if(resource instanceof IProject && !((IProject) resource).isOpen()) return null;
		if(ResourceType.isExperiment(resource)) return Activator.getImage(IImageKeys.EXPERIMENT_ICON);
		if(ResourceType.isFolder(resource)) return Activator.getImage(IImageKeys.FOLDER_ICON);
		if(ResourceType.isSubject(resource)) return Activator.getImage(IImageKeys.SUBJECT_ICON);
		if(ResourceType.isSession(resource)) return Activator.getImage(IImageKeys.SESSION_ICON);
		if(ResourceType.isDACQConfiguration(resource)) return Activator.getImage(IImageKeys.DACQ_CONFIGURATION_ICON);
		if(ResourceType.isProcess(resource)) return Activator.getImage(IImageKeys.PROCESS_ICON);
		if(ResourceType.isTrial(resource) && ResourceProperties.isTrialDone((IFolder) resource)) return Activator.getImage(IImageKeys.DONE_ICON);
		if(ResourceType.isTrial(resource) && !ResourceProperties.isTrialDone((IFolder) resource)) return Activator.getImage(IImageKeys.UNDONE_ICON);
		if(ResourceType.isLog(resource)) return Activator.getImage(IImageKeys.DIARY_ICON);
		if(ResourceType.isSamples(resource)) return Activator.getImage(IImageKeys.SAMPLES_ICON);
		if(ResourceType.isADWDataFile(resource)) return Activator.getImage(IImageKeys.SAMPLES_ICON);
		if(ResourceType.isOptitrack_Type_1(resource)) return Activator.getImage(IImageKeys.SAMPLES_ICON);
		if(ResourceType.isColumnDataFile(resource)) return Activator.getImage(IImageKeys.SAMPLES_ICON);
		if(ResourceType.isParameters(resource)) return Activator.getImage(IImageKeys.PARAMETERS_FILE_ICON);
		if(ResourceType.isProcessTest(resource)) return Activator.getImage(IImageKeys.FOLDER_ICON);
		if(ResourceType.isDataProcessing(resource)) return Activator.getImage(IImageKeys.DATA_PROCESSING_ICON);
		if(ResourceType.isBatchDataProcessing(resource)) return Activator.getImage(IImageKeys.BATCH_DATA_PROCESSING_ICON);
		if(ResourceType.isNumpyFile(resource)) return Activator.getImage(IImageKeys.NUMPY_ICON_2);
		if(ResourceType.isSaveFile(resource)) return Activator.getImage(IImageKeys.LOAD_UNLOAD_ICON);
		if(ResourceType.isXYChart(resource)) return Activator.getImage(IImageKeys.XYChart_ICON);
		if(ResourceType.isXYZChart(resource)) return Activator.getImage(IImageKeys.XYZChart_ICON);
		if(ResourceType.isCustomerFunction(resource)) return Activator.getImage(IImageKeys.CUSTOMER_FUNCTION_ICON);
		return Activator.getSharedImage(ISharedImages.IMG_LCL_LINKTO_HELP);//"icons/full/etool16/help_contents.png"); // this is IWorkbenchGraphicConstants.IMG_ETOOL_HELP_CONTENTS from org.eclipse.ui
	}

	public String getText(Object element) {
		if(element instanceof IResource) return GetResourceLabelDelegate.getLabel((IResource)element);
		return null;
	}

}
