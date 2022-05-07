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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class ChannelEditor extends EditorPart implements TrialsEditor {
	
	public static String ID = "Docometre.ChannelEditor";
	
	private Channel channel;
	private Composite container;

	public ChannelEditor() {
		
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}
	
	protected Channel getChannel() {
		return channel;
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		channel = (Channel)object;
	}
	
	@Override
	public Image getTitleImage() {
		if(channel == null) return Activator.getImage("org.eclipse.ui", "icons/full/etool16/help_contents.png");
		if(channel.isCategory()) return Activator.getImage(IImageKeys.CATEGORY_ICON);
		return Activator.getImage(IImageKeys.SIGNAL_ICON);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		if(channel != null && channel.isSignal()) {
			container = new SignalContainerEditor(parent, SWT.BORDER, this);
		}
		if(channel != null && channel.isCategory()) {
			container = new CategoryContainerEditor(parent, SWT.BORDER, this);
		}
	}

	@Override
	public void setFocus() {
		container.setFocus();
	}
	
	@Override
	public String getPartName() {
		return channel.getFullName();
	}

	@Override
	public void gotoNextTrial() {
		if(container instanceof TrialNavigator)
			((TrialNavigator)container).gotoNextTrial();
		
	}

	@Override
	public void gotoPreviousTrial() {
		if(container instanceof TrialNavigator)
			((TrialNavigator)container).gotoPreviousTrial();
	}
	
	public void update() {
		if(getChannel().isModified() && getChannel().isSignal()) {
			((SignalContainerEditor)container).update();
		}
	}

	public void setShowCursor(boolean showCursor) {
		if(container instanceof SignalContainerEditor) ((SignalContainerEditor)container).getChart().setShowCursor(showCursor);
	}

	public void setShowMarker(boolean showMarker) {
		if(container instanceof SignalContainerEditor) ((SignalContainerEditor)container).getChart().setShowMarker(showMarker);
	}
	
}
