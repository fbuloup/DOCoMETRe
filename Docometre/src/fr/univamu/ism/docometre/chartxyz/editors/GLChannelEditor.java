package fr.univamu.ism.docometre.chartxyz.editors;

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
import fr.univamu.ism.docometre.analyse.datamodel.XYChart;
import fr.univamu.ism.docometre.analyse.editors.Chart2D3DBehaviour;
import fr.univamu.ism.docometre.analyse.editors.TrialNavigator;
import fr.univamu.ism.docometre.analyse.editors.TrialsEditor;
import fr.univamu.ism.docometre.chartxyz.AbstractGLChart;
import fr.univamu.ism.docometre.chartxyz.GLChart1D;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;

public class GLChannelEditor extends EditorPart implements TrialsEditor, Chart2D3DBehaviour {
	
	public static String ID = "Docometre.GLChannelEditor";
	
	private Channel channel;
	private Composite container;
	
	@Override
	public Image getTitleImage() {
		if(channel == null) return Activator.getImage("org.eclipse.ui", "icons/full/etool16/help_contents.png");
		if(channel.isCategory()) return Activator.getImage(IImageKeys.CATEGORY_ICON);
		return Activator.getImage(IImageKeys.SIGNAL_ICON);
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setInput(input);
		setSite(site);
		Object object = ((ResourceEditorInput)getEditorInput()).getObject();
		channel = (Channel)object;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		if(channel != null && channel.isSignal()) {
			container = new GLSignalContainerEditor(parent, SWT.NONE, this);
			ChartPartListener.install(this, (AbstractGLChart<?, ?>) ((GLSignalContainerEditor)container).getChart());
		}
		if(channel != null && channel.isCategory()) {
			container = new GLCategoryContainerEditor(parent, SWT.NONE, this);
			ChartPartListener.install(this, (AbstractGLChart<?, ?>) ((GLCategoryContainerEditor)container).getChart());
		}
	}
	
	@Override
	public String getPartName() {
		return channel.getFullName();
	}
	
	@Override
	public void setFocus() {
		container.setFocus();
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
	
	@Override
	public void update() {
		if(channel.isModified() && channel.isSignal()) {
			if(container instanceof GLSignalContainerEditor) ((GLSignalContainerEditor)container).update();
			if(container instanceof GLCategoryContainerEditor) ((GLCategoryContainerEditor)container).update();
		}
	}

	@Override
	public void setDirty(boolean dirty) {
	}

	@Override
	public void refreshTrialsListFrontEndCutsCategories() {
	}

	@Override
	public XYChart getChartData() {
		return null;
	}

	@Override
	public String[] getSeriesIDs() {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected Channel getChannel() {
		return channel;
	}

	@Override
	public void redraw() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateFrontEndCutsChartHandler() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateXAxisRange(double min, double max) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateYAxisRange(double min, double max) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateZAxisRange(double min, double max) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSeries(String seriesID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void left() {
		if(container instanceof GLSignalContainerEditor) {
			GLSignalContainerEditor signalEditor = (GLSignalContainerEditor)container;
			GLChart1D chart = (GLChart1D) signalEditor.getChart();
			chart.getRenderer().pan(-0.1f, 0);
		}
		if(container instanceof GLCategoryContainerEditor) {
			GLCategoryContainerEditor categoryEditor = (GLCategoryContainerEditor)container;
			GLChart1D chart = (GLChart1D) categoryEditor.getChart();
			chart.getRenderer().pan(-0.1f, 0);
		}
	}

	@Override
	public void right() {
		if(container instanceof GLSignalContainerEditor) {
			GLSignalContainerEditor signalEditor = (GLSignalContainerEditor)container;
			GLChart1D chart = (GLChart1D) signalEditor.getChart();
			chart.getRenderer().pan(0.1f, 0);
		}
		if(container instanceof GLCategoryContainerEditor) {
			GLCategoryContainerEditor categoryEditor = (GLCategoryContainerEditor)container;
			GLChart1D chart = (GLChart1D) categoryEditor.getChart();
			chart.getRenderer().pan(0.1f, 0);
		}
	}

	@Override
	public void up() {
		if(container instanceof GLSignalContainerEditor) {
			GLSignalContainerEditor signalEditor = (GLSignalContainerEditor)container;
			GLChart1D chart = (GLChart1D) signalEditor.getChart();
			chart.getRenderer().pan(0, -0.1f);
		}
		if(container instanceof GLCategoryContainerEditor) {
			GLCategoryContainerEditor categoryEditor = (GLCategoryContainerEditor)container;
			GLChart1D chart = (GLChart1D) categoryEditor.getChart();
			chart.getRenderer().pan(0, -0.1f);
		}
	}

	@Override
	public void down() {
		if(container instanceof GLSignalContainerEditor) {
			GLSignalContainerEditor signalEditor = (GLSignalContainerEditor)container;
			GLChart1D chart = (GLChart1D) signalEditor.getChart();
			chart.getRenderer().pan(0, 0.1f);
		}
		if(container instanceof GLCategoryContainerEditor) {
			GLCategoryContainerEditor categoryEditor = (GLCategoryContainerEditor)container;
			GLChart1D chart = (GLChart1D) categoryEditor.getChart();
			chart.getRenderer().pan(0, 0.1f);
		}
	}

	@Override
	public void zoomIn() {
		if(container instanceof GLSignalContainerEditor) {
			GLSignalContainerEditor signalEditor = (GLSignalContainerEditor)container;
			GLChart1D chart = (GLChart1D) signalEditor.getChart();
			chart.getRenderer().zoom(1f);
		}
		if(container instanceof GLCategoryContainerEditor) {
			GLCategoryContainerEditor categoryEditor = (GLCategoryContainerEditor)container;
			GLChart1D chart = (GLChart1D) categoryEditor.getChart();
			chart.getRenderer().zoom(1f);
		}
	}

	@Override
	public void zoomOut() {
		if(container instanceof GLSignalContainerEditor) {
			GLSignalContainerEditor signalEditor = (GLSignalContainerEditor)container;
			GLChart1D chart = (GLChart1D) signalEditor.getChart();
			chart.getRenderer().zoom(-1f);
		}
		if(container instanceof GLCategoryContainerEditor) {
			GLCategoryContainerEditor categoryEditor = (GLCategoryContainerEditor)container;
			GLChart1D chart = (GLChart1D) categoryEditor.getChart();
			chart.getRenderer().zoom(-1f);
		}
	}

	@Override
	public void zoomToFit() {
		if(container instanceof GLSignalContainerEditor) {
			GLSignalContainerEditor signalEditor = (GLSignalContainerEditor)container;
			GLChart1D chart = (GLChart1D) signalEditor.getChart();
			chart.getRenderer().resetView();
		}
		if(container instanceof GLCategoryContainerEditor) {
			GLCategoryContainerEditor categoryEditor = (GLCategoryContainerEditor)container;
			GLChart1D chart = (GLChart1D) categoryEditor.getChart();
			chart.getRenderer().resetView();
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

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

}
