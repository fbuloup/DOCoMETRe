package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtchart.internal.ChartLayout;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.IPlotArea;
import org.eclipse.swtchart.ISeries;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;

public final class MarkersManager extends MouseAdapter implements ICustomPaintListener {

	private SignalContainerEditor signalContainerEditor; 
	private String markersGroupLabel;

	public MarkersManager(SignalContainerEditor signalContainerEditor) {
		this.signalContainerEditor = signalContainerEditor;
		signalContainerEditor.getChart().getPlotArea().addMouseListener(this);
		((IPlotArea)signalContainerEditor.getChart().getPlotArea()).addCustomPaintListener(this);
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent mouseEvent) {
		if (markersGroupLabel != null && !markersGroupLabel.equals("") && !(signalContainerEditor.getChart().getCurrentSeries() == null)) {
			ISeries series = signalContainerEditor.getChart().getCurrentSeries();
			String fullSignalName = series.getId().replaceAll("\\.\\d+$", "");
			int trialNumber = Integer.parseInt(series.getId().replaceAll("^\\w+\\.\\w+\\.\\w+\\.", ""));
			Event event = new Event();
			event.x = mouseEvent.x;
			event.y = mouseEvent.y;
			double[] coordinates = signalContainerEditor.getChart().getMarkerCoordinates(event);
			if(coordinates.length == 2) {
				try {
					Activator.logInfoMessage("Add marker " + markersGroupLabel + " at index " + mouseEvent.x + " for : " + fullSignalName + " trial number " + trialNumber, getClass());
					Activator.logInfoMessage("With coordinates x " + coordinates[0] + " and y " + coordinates[1], getClass());
					String fullSubjectName = fullSignalName.replaceAll("\\.\\w+$", "");
					fullSubjectName = fullSubjectName.replaceAll("\\.", "/");
					IResource subject = ResourcesPlugin.getWorkspace().getRoot().findMember(fullSubjectName);
					if(subject != null) {
						Object objectSession = subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
						if(objectSession != null && objectSession instanceof ChannelsContainer) {
							ChannelsContainer channelsContainer = (ChannelsContainer)objectSession;
							Channel signal = channelsContainer.getChannelFromName(fullSignalName);
							if(signal != null) {
								MathEngineFactory.getMathEngine().addMarker(markersGroupLabel, trialNumber, coordinates[0], coordinates[1], signal);
								signalContainerEditor.updateMarkersGroup(markersGroupLabel);
								signalContainerEditor.getChart().redraw();
							}
						}
					}
					
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean drawBehindSeries() {
		return false;
	}

	@Override
	public void paintControl(PaintEvent event) {
		try {
			ISeries[] seriesSet = signalContainerEditor.getChart().getSeriesSet().getSeries();
			for (ISeries series : seriesSet) {
				int trialNumber = Integer.parseInt(series.getId().replaceAll("^\\w+\\.\\w+\\.\\w+\\.", ""));
				String fullSignalName = series.getId().replaceAll("\\.\\d+$", "");
				String fullSubjectName = fullSignalName.replaceAll("\\.\\w+$", "");
				fullSubjectName = fullSubjectName.replaceAll("\\.", "/");
				IResource subject = ResourcesPlugin.getWorkspace().getRoot().findMember(fullSubjectName);
				if(subject != null) {
					Object objectSession = subject.getSessionProperty(ResourceProperties.CHANNELS_LIST_QN);
					if(objectSession != null && objectSession instanceof ChannelsContainer) {
						ChannelsContainer channelsContainer = (ChannelsContainer)objectSession;
						Channel signal = channelsContainer.getChannelFromName(fullSignalName);
						if(signal != null) {
							String[] markersGroupsLabel = MathEngineFactory.getMathEngine().getMarkersGroupsLabels(signal);
							for (String markersGroupLabel : markersGroupsLabel) {
								double[][] markers = MathEngineFactory.getMathEngine().getMarkers(markersGroupLabel, signal);
								for (int i = 0; i < markers.length; i++) {
									if(((int)markers[i][0]) == trialNumber) {
										
										int index = signalContainerEditor.getChart().getAxisSet().getXAxes()[0].getPixelCoordinate(markers[i][1]) + 1;
										
										event.gc.setForeground(((ILineSeries)series).getLineColor());
										event.gc.setLineWidth(3);
										event.gc.setLineStyle(SWT.LINE_DOT);
										event.gc.drawLine(index, 0, index, signalContainerEditor.getChart().getPlotArea().getClientArea().height);
										event.gc.drawText(markersGroupLabel, index + 3, signalContainerEditor.getChart().getPlotArea().getClientArea().height - 15);
										
										GC gc = new GC(signalContainerEditor.getChart());
										int W2 = signalContainerEditor.getChart().getAxisSet().getYAxes()[0].getTick().getBounds().width;
										W2 += ChartLayout.MARGIN;
										gc.setForeground(((ILineSeries)series).getLineColor());
										gc.setLineWidth(3);
										gc.setLineStyle(SWT.LINE_DOT);
										gc.drawLine(index + W2, 0, index + W2, signalContainerEditor.getChart().getPlotArea().getClientArea().height + 15);
										gc.dispose();
										
									}
								}
							}
							
						}
					}
					
					
				}
			}
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
	}
	
	public void setMarkersGroupLabel(String markersGroupLabel) {
		this.markersGroupLabel = markersGroupLabel;
	}
	
	public String getMarkersGroupLabel() {
		return markersGroupLabel;
	}

}
