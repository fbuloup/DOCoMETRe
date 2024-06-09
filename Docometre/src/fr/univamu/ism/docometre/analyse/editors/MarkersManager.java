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

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtchart.internal.ChartLayout;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.IPlotArea;
import org.eclipse.swtchart.ISeries;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.datamodel.ChannelsContainer;
import fr.univamu.ism.docometre.analyse.datamodel.XYChart;

public final class MarkersManager extends MouseAdapter implements ICustomPaintListener {

	private IMarkersManager containerEditor; 
	private String markersGroupLabel;
	private double[] selectedMarker;

	public MarkersManager(IMarkersManager containerEditor) {
		this.containerEditor = containerEditor;
		containerEditor.getChart().getPlotArea().addMouseListener(this);
		((IPlotArea)containerEditor.getChart().getPlotArea()).addCustomPaintListener(this);
	}
	
	public void setSelectedMarker(double[] selectedMarker) {
		this.selectedMarker = selectedMarker;
	}
	
	private boolean isEqualToSelectedMarker(double[] marker) {
		if(selectedMarker == null) return false;
		return (selectedMarker[0] == marker[0]) && (selectedMarker[1] == marker[1]) && (selectedMarker[2] == marker[2]);
	}
	
	@Override
	public void mouseDoubleClick(MouseEvent mouseEvent) {
		if (markersGroupLabel != null && !markersGroupLabel.equals("") && !(containerEditor.getChart().getCurrentSeries() == null)) {
			ISeries series = containerEditor.getChart().getCurrentSeries();
			String fullSignalName = series.getId().replaceAll("\\.\\d+$", "");
			int trialNumber = Integer.parseInt(series.getId().replaceAll("^\\w+\\.\\w+\\.\\w+\\.", ""));
			Event event = new Event();
			event.x = mouseEvent.x;
			event.y = mouseEvent.y;
			double[] coordinates = containerEditor.getChart().getMarkerCoordinates(event);
			if(coordinates.length == 2) {
				try {
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
								channelsContainer.setUpdateChannelsCache(true);
								containerEditor.updateMarkersGroup(markersGroupLabel, trialNumber, coordinates[0], coordinates[1]);
								containerEditor.getChart().redraw();
							}
						}
					}
					
				} catch (CoreException e) {
					Activator.logErrorMessageWithCause(e);
					e.printStackTrace();
				}
			} else Activator.logWarningMessage(DocometreMessages.activateShowCursorMessage);
		}
	}

	@Override
	public boolean drawBehindSeries() {
		return false;
	}
	
	private void paintMarkersForSignalOrCategoryEditor(PaintEvent event) {
		try {
			ISeries[] seriesSet = containerEditor.getChart().getSeriesSet().getSeries();
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
										
										boolean isSelectedMarker =  isEqualToSelectedMarker(markers[i]);
										
										int index = containerEditor.getChart().getAxisSet().getXAxes()[0].getPixelCoordinate(markers[i][1]) + 1;
										
										Color oldForegroundColor = event.gc.getForeground();
										int oldLineWidth = event.gc.getLineWidth();
										int oldLineStyle = event.gc.getLineStyle();
										event.gc.setForeground(((ILineSeries)series).getLineColor());
										event.gc.setLineWidth(3);
										event.gc.setLineStyle(isSelectedMarker?SWT.LINE_SOLID:SWT.LINE_DOT);
										event.gc.drawLine(index, 0, index, containerEditor.getChart().getPlotArea().getClientArea().height);
										event.gc.drawText(markersGroupLabel, index + 3, containerEditor.getChart().getPlotArea().getClientArea().height - 15);
										event.gc.setForeground(oldForegroundColor);
										event.gc.setLineWidth(oldLineWidth);
										event.gc.setLineStyle(oldLineStyle);
										
										GC gc = new GC(containerEditor.getChart());
										int W2 = containerEditor.getChart().getAxisSet().getYAxes()[0].getTick().getBounds().width;
										W2 += ChartLayout.MARGIN;
										gc.setForeground(((ILineSeries)series).getLineColor());
										gc.setLineWidth(3);
										gc.setLineStyle(isSelectedMarker?SWT.LINE_SOLID:SWT.LINE_DOT);
										gc.drawLine(index + W2, 0, index + W2, containerEditor.getChart().getPlotArea().getClientArea().height + 15);
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
	
	private void displayXYMarkers(ISeries series, Channel xSignal, Channel ySignal, boolean displayXMarkers, int trialNumber, int frontCut, int endCut, PaintEvent event) {
		XYChart xyChartData = ((XYChartEditor)containerEditor).getChartData();
		int delta = xyChartData.getMarkersSize();
		double sf = MathEngineFactory.getMathEngine().getSampleFrequency(xSignal);
		String[] markersGroupsLabels = new String[0]; 
		if(displayXMarkers) markersGroupsLabels = MathEngineFactory.getMathEngine().getMarkersGroupsLabels(xSignal);
		else markersGroupsLabels = MathEngineFactory.getMathEngine().getMarkersGroupsLabels(ySignal);
		for (String markersGroupLabel : markersGroupsLabels) {
			double[][] markers = new double[0][0];
			if(displayXMarkers) markers = MathEngineFactory.getMathEngine().getMarkers(markersGroupLabel, xSignal);
			else markers = MathEngineFactory.getMathEngine().getMarkers(markersGroupLabel, ySignal);
			for (int i = 0; i < markers.length; i++) {
				if(((int)markers[i][0]) == trialNumber) {
					int xIndex = 0;
					int yIndex = 0;
					int sampleIndex = (int) (sf * markers[i][1]);
					if(sampleIndex <= frontCut || sampleIndex >= endCut) continue;
					if(displayXMarkers) {
						xIndex = containerEditor.getChart().getAxisSet().getXAxes()[0].getPixelCoordinate(markers[i][2]);
						double[] values = MathEngineFactory.getMathEngine().getYValuesForSignal(ySignal, trialNumber);
						int baseFrontCut = MathEngineFactory.getMathEngine().getFrontCut(ySignal, trialNumber);
						yIndex = containerEditor.getChart().getAxisSet().getYAxes()[0].getPixelCoordinate(values[sampleIndex - baseFrontCut]);
					} else {
						yIndex = containerEditor.getChart().getAxisSet().getYAxes()[0].getPixelCoordinate(markers[i][2]);
						double[] values = MathEngineFactory.getMathEngine().getYValuesForSignal(xSignal, trialNumber);
						int baseFrontCut = MathEngineFactory.getMathEngine().getFrontCut(xSignal, trialNumber);
						xIndex = containerEditor.getChart().getAxisSet().getXAxes()[0].getPixelCoordinate(values[sampleIndex - baseFrontCut]);
					}
					
					Color oldForegroundColor = event.gc.getForeground();
					Color oldBackgroundColor = event.gc.getBackground();
					int oldLineWidth = event.gc.getLineWidth();
					int oldLineStyle = event.gc.getLineStyle();
					event.gc.setBackground(DocometreApplication.getColor(DocometreApplication.WHITE));
					event.gc.fillOval(xIndex - delta + 1, yIndex - delta + 1, 2*delta, 2*delta);
					event.gc.setForeground(((ILineSeries)series).getLineColor());
					event.gc.drawOval(xIndex - delta, yIndex - delta, 2*delta + 1, 2*delta + 1);
					
					event.gc.setBackground(oldBackgroundColor);
					if(xyChartData.isShowMarkersLabels()) {
						event.gc.drawText(markersGroupLabel, xIndex + delta, yIndex + delta);
					}
					event.gc.setForeground(oldForegroundColor);
					event.gc.setLineWidth(oldLineWidth);
					event.gc.setLineStyle(oldLineStyle);
					
				}
			}
		}
	}
	
	private void paintMarkersForXYChartEditor(PaintEvent event) {
		XYChart xyChartData = ((XYChartEditor)containerEditor).getChartData();
		if(!xyChartData.isShowMarkers()) return;
		for (String seriesID : xyChartData.getSeriesIDsPrefixes()) {
			Channel[] signals = xyChartData.getXYChannels(seriesID);
			if(signals == null) return;//May occurred when subject is not loaded 
			Channel xSignal = signals[0];
			Channel ySignal = signals[1];
			
			List<Integer> trialsNumbers = xyChartData.getSelectedTrialsNumbers();
			for (Integer trialNumber : trialsNumbers) {
				ISeries series = containerEditor.getChart().getSeriesSet().getSeries(seriesID + "." + trialNumber);
				// Display xSignal markers
				displayXYMarkers(series, xSignal, ySignal, true, trialNumber, xyChartData.getFrontCut(), xyChartData.getEndCut(), event);
				// Display ySignal markers
				displayXYMarkers(series, xSignal, ySignal, false, trialNumber, xyChartData.getFrontCut(),xyChartData.getEndCut(),  event);
			}
		}
	}

	@Override
	public void paintControl(PaintEvent event) {
		if(containerEditor instanceof SignalContainerEditor || containerEditor instanceof CategoryContainerEditor) paintMarkersForSignalOrCategoryEditor(event);
		if(containerEditor instanceof XYChartEditor) paintMarkersForXYChartEditor(event);
	}

	public void setMarkersGroupLabel(String markersGroupLabel) {
		this.markersGroupLabel = markersGroupLabel;
	}
	
	public String getMarkersGroupLabel() {
		return markersGroupLabel;
	}

}
