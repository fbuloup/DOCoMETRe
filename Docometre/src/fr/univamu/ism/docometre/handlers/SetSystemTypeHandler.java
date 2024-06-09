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
package fr.univamu.ism.docometre.handlers;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class SetSystemTypeHandler extends AbstractHandler implements IElementUpdater  {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String parameterValue = event.getParameter("Docometre.system.type.parameter");
		if(parameterValue != null) {
			IWorkbenchPart part = HandlerUtil.getActivePart(event);
			if(part instanceof ExperimentsView) {
				ExperimentsView experimentsView = (ExperimentsView)part;
				IStructuredSelection selection = (IStructuredSelection)experimentsView.getSelection();
				Object[] elements = selection.toArray();
				for (Object element : elements) {
					IResource resource = (IResource)element;
					ResourceProperties.setSystemPersistentProperty(resource, parameterValue);
					ExperimentsView.refresh(resource.getParent(), new IResource[] {resource});
				}
			}
		}
		return null;
	}

	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		IViewPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ExperimentsView.ID);
		if(part != null && part instanceof ExperimentsView) {
			String parameterValue = (String) parameters.get("Docometre.system.type.parameter");
			ExperimentsView experimentsView = (ExperimentsView)part;
			IResource resource = (IResource) ((IStructuredSelection)experimentsView.getSelection()).getFirstElement();
			String systemType = ResourceProperties.getSystemPersistentProperty(resource);
			element.setChecked(parameterValue.equalsIgnoreCase(systemType));
		}
	}
	
	

}
