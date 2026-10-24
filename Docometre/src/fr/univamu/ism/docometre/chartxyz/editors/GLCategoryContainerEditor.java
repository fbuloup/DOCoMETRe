package fr.univamu.ism.docometre.chartxyz.editors;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.analyse.editors.TrialNavigator;

public class GLCategoryContainerEditor extends Composite implements ISelectionChangedListener, TrialNavigator {
	
	private GLChannelEditor channelEditor;
	
	public GLCategoryContainerEditor(Composite parent, int style, GLChannelEditor channelEditor) {
		super(parent, style);
		this.channelEditor = channelEditor;
	}

	@Override
	public void gotoNextTrial() {
		// TODO Auto-generated method stub

	}

	@Override
	public void gotoPreviousTrial() {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// TODO Auto-generated method stub

	}

}
