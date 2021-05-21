package fr.univamu.ism.docometre.analyse.datamodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;

public class XYChart extends AbstractElement {

	private static final long serialVersionUID = 1L;
	
	private transient Map<String, Channel[]> xyChannelsMap;
	private Set<String> seriesIDs;
	private List<Integer> selectedTrialsNumbers;
	private double xMax;
	private double xMin;
	private double yMax;
	private double yMin;
	private boolean autoScale;
	private int frontCut;
	private int endCut;
	private boolean showMarkers;
	private boolean showMarkersLabels;
	private int markersSize;
	
	public XYChart() {
		seriesIDs = new HashSet<>();
		selectedTrialsNumbers = new ArrayList<>();
		autoScale = false;
		xMax = 10;
		xMin = -10;
		yMax = 10;
		yMin = -10;
		frontCut = -1;
		endCut = -1;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initializeObservers() {
	}

	public double getxMax() {
		return xMax;
	}

	public void setxMax(double xMax) {
		this.xMax = xMax;
	}

	public double getxMin() {
		return xMin;
	}

	public void setxMin(double xMin) {
		this.xMin = xMin;
	}

	public double getyMax() {
		return yMax;
	}

	public void setyMax(double yMax) {
		this.yMax = yMax;
	}

	public double getyMin() {
		return yMin;
	}

	public void setyMin(double yMin) {
		this.yMin = yMin;
	}

	public boolean isAutoScale() {
		return autoScale;
	}

	public void setAutoScale(boolean autoScale) {
		this.autoScale = autoScale;
	}
	
	public boolean isShowMarkers() {
		return showMarkers;
	}

	public void setShowMarkers(boolean showMarkers) {
		this.showMarkers = showMarkers;
	}

	public boolean isShowMarkersLabels() {
		return showMarkersLabels;
	}

	public void setShowMarkersLabels(boolean showMarkersLabels) {
		this.showMarkersLabels = showMarkersLabels;
	}
	
	public int getMarkersSize() {
		return markersSize == 0 ? 3 : markersSize;
	}

	public void setMarkersSize(int markersSize) {
		this.markersSize = markersSize;
	}

	public int getFrontCut() {
		return frontCut;
	}

	public void setFrontCut(int frontCut) {
		this.frontCut = frontCut;
	}

	public int getEndCut() {
		return endCut;
	}

	public void setEndCut(int endCut) {
		this.endCut = endCut;
	}
	
	public void setRange(double xMin, double xMax, double yMin, double yMax) {
		setxMin(xMin);
		setxMax(xMax);
		setyMin(yMin);
		setyMax(yMax);
	}
	
	public String[] getSeriesIDsPrefixes() {
		return seriesIDs.toArray(new String[seriesIDs.size()]);
	}
	
	public void addCurve(Channel xChannel, Channel yChannel) {
		String key = yChannel.getFullName() + "(" + xChannel.getFullName() + ")";
		seriesIDs.add(key);
		xyChannelsMap.put(key, new Channel[] {xChannel, yChannel});
	}
	
	public void removeCurve(String key) {
		seriesIDs.remove(key);
		xyChannelsMap.remove(key);
	}
	
	public int getNbCurves() {
		return seriesIDs.size();
	}
	
	public Collection<Channel[]> getXYChannels() {
		return xyChannelsMap.values();
	}
	
	public Set<String> getCurvesIDs() {
		return xyChannelsMap.keySet();
	}
	
	public Channel[] getXYChannels(String key) {
		return xyChannelsMap.get(key);
	}

	public void setSelectedTrialsNumbers(List<Integer> selectedTrialsNumbers) {
		this.selectedTrialsNumbers = selectedTrialsNumbers;
	}
	
	public List<Integer> getSelectedTrialsNumbers() {
		if(selectedTrialsNumbers == null) return new ArrayList<>();
		return selectedTrialsNumbers;
	}

	public boolean initialize() {
		if(xyChannelsMap != null) xyChannelsMap.clear();
		xyChannelsMap = new HashMap<>();
		if(!MathEngineFactory.getMathEngine().isStarted()) {
			Activator.logErrorMessage(DocometreMessages.PleaseStartMathEngineFirst);
			return false;
		}
		for (String seriesID : getSeriesIDsPrefixes()) {
			
			//pro.sub.chann(pro.sub.chann)
			
			String yFullChannelName = seriesID.split("\\(")[0];
			String xFullChannelName = seriesID.split("\\(")[1].replaceAll("\\)", "");
			
			String xProjectName = xFullChannelName.split("\\.")[0];
			String xSubjectName = xFullChannelName.split("\\.")[1];
			IProject xProject = ResourcesPlugin.getWorkspace().getRoot().getProject(xProjectName);
			IResource xSubject = xProject.findMember(xSubjectName);
			
			String yProjectName = yFullChannelName.split("\\.")[0];
			String ySubjectName = yFullChannelName.split("\\.")[1];
			IProject yProject = ResourcesPlugin.getWorkspace().getRoot().getProject(yProjectName);
			IResource ySubject = yProject.findMember(ySubjectName);
			
			
			if(MathEngineFactory.getMathEngine().isSubjectLoaded(xSubject) && MathEngineFactory.getMathEngine().isSubjectLoaded(ySubject)) {
				Channel xChannel = MathEngineFactory.getMathEngine().getChannelWithName(xSubject, xFullChannelName.split("\\.")[2]);
				Channel yChannel = MathEngineFactory.getMathEngine().getChannelWithName(ySubject, yFullChannelName.split("\\.")[2]);
				if(xChannel == null || yChannel == null) {
					if(xChannel == null) {
						String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, xFullChannelName);
						Activator.logErrorMessage(message);
					}
					if(yChannel == null) {
						String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, yFullChannelName);
						Activator.logErrorMessage(message);
					}
					return false;
				}
				xyChannelsMap.put(seriesID, new Channel[] {xChannel, yChannel});
			} else {
				if(!MathEngineFactory.getMathEngine().isSubjectLoaded(xSubject)) {
					String message = NLS.bind(DocometreMessages.SubjectNotLoaded, xSubject.getFullPath());
					Activator.logErrorMessage(message);
				}
				if(!MathEngineFactory.getMathEngine().isSubjectLoaded(ySubject) && !xSubject.equals(ySubject)) {
					String message = NLS.bind(DocometreMessages.SubjectNotLoaded, ySubject.getFullPath());
					Activator.logErrorMessage(message);
				}
				return false;
			}
		}
		return true;
	}
	
	public boolean contains(IResource subject) {
		for (String seriesID : getSeriesIDsPrefixes()) {
			String subjectName = seriesID.split("\\.")[1];
			if(subject.getName().equals(subjectName)) return true;
			subjectName = seriesID.split("\\.")[3];
			if(subject.getName().equals(subjectName)) return true;
		}
		return false;
		
	}

}
