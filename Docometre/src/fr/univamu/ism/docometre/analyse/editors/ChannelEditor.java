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
	
}
