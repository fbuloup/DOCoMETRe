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

public class ChannelEditor extends EditorPart {
	
	public static String ID = "Docometre.ChannelEditor";
	
	private static boolean debugSignal = false;
	private static boolean debugCategory = false;
	
	private Channel channel;
	private Composite container;

	public ChannelEditor() {
		
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}
	
	protected Channel getchannel() {
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
		if(channel == null) return Activator.getImageDescriptor("org.eclipse.ui", "icons/full/etool16/help_contents.png").createImage();
		if(channel.isCategory()) return Activator.getImage(IImageKeys.CATEGORY_ICON);
		return Activator.getImage(IImageKeys.SIGNAL_ICON);
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		if(debugSignal || channel != null && channel.isSignal()) {
			container = new SignalContainerEditor(parent, SWT.BORDER, this);
		}
		if(debugCategory || channel != null && channel.isCategory()) {
			container = new CategoryContainerEditor(parent, SWT.BORDER, this);
		}
	}

	@Override
	public void setFocus() {
		container.setFocus();
	}
	
	@Override
	public String getPartName() {
		
		/*>>>>>>>>*//*for debug, to remove*/
		if(channel == null) return "Part name for debug only!";
		/*>>>>>>>>*//*for debug, to remove*/
		
		return channel.getFullName();
	}

	public void gotNextTrial() {
		// TODO Auto-generated method stub
		
	}

	public void gotoPreviousTrial() {
		// TODO Auto-generated method stub
		
	}
	
}
