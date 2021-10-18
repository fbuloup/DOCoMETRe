package fr.univamu.ism.docometre.analyse.datamodel;

import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swtchart.LineStyle;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ColorUtil;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class XYZChart extends XYChart {

	private static final long serialVersionUID = 1L;
	
	private double zMax;
	private double zMin;

	private RGB zAxisColor;
	private boolean zAxisVisible;
	private String zAxisGridStyle;
	private RGB zAxisGridColor;
	
	public XYZChart() {
		super();
		zMax = 10;
		zMin = -10;
	}
	
	public double getzMax() {
		return zMax;
	}

	public void setzMax(double zMax) {
		this.zMax = zMax;
	}
	
	public double getzMin() {
		return zMin;
	}

	public void setzMin(double zMin) {
		this.zMin = zMin;
	}
	
	public void setRange(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
		super.setRange(xMin, xMax, yMin, yMax);
		setzMin(xMin);
		setzMax(xMax);
	}
	
	public void addCurve(Channel xChannel, Channel yChannel, Channel zChannel) {
		String key = zChannel.getFullName() + "(" + xChannel.getFullName() + "," + yChannel.getFullName() + ")";
		seriesIDs.add(key);
		channelsMap.put(key, new Channel[] {xChannel, yChannel, zChannel});
	}
	
	public boolean initialize() {
		if(channelsMap != null) channelsMap.clear();
		channelsMap = new HashMap<>();
		if(!MathEngineFactory.getMathEngine().isStarted()) {
			Activator.logErrorMessage(DocometreMessages.PleaseStartMathEngineFirst);
			return false;
		}
		for (String seriesID : getSeriesIDsPrefixes()) {
			
			//pro.sub.chann(pro.sub.chann)
			
			String zFullChannelName = seriesID.split("\\(")[0];
			String yFullChannelName = seriesID.split("\\(")[1].split(",")[0];
			String xFullChannelName = seriesID.split("\\(")[1].split(",")[0].replaceAll("\\)", "");
			
			String xProjectName = xFullChannelName.split("\\.")[0];
			String xSubjectName = xFullChannelName.split("\\.")[1];
			IProject xProject = ResourcesPlugin.getWorkspace().getRoot().getProject(xProjectName);
			IResource xSubject = xProject.findMember(xSubjectName);
			
			String yProjectName = yFullChannelName.split("\\.")[0];
			String ySubjectName = yFullChannelName.split("\\.")[1];
			IProject yProject = ResourcesPlugin.getWorkspace().getRoot().getProject(yProjectName);
			IResource ySubject = yProject.findMember(ySubjectName);
			
			String zProjectName = zFullChannelName.split("\\.")[0];
			String zSubjectName = zFullChannelName.split("\\.")[1];
			IProject zProject = ResourcesPlugin.getWorkspace().getRoot().getProject(zProjectName);
			IResource zSubject = zProject.findMember(zSubjectName);
			
			
			if(MathEngineFactory.getMathEngine().isSubjectLoaded(xSubject) && MathEngineFactory.getMathEngine().isSubjectLoaded(ySubject) && MathEngineFactory.getMathEngine().isSubjectLoaded(zSubject)) {
				Channel xChannel = MathEngineFactory.getMathEngine().getChannelWithName(xSubject, xFullChannelName.split("\\.")[2]);
				Channel yChannel = MathEngineFactory.getMathEngine().getChannelWithName(ySubject, yFullChannelName.split("\\.")[2]);
				Channel zChannel = MathEngineFactory.getMathEngine().getChannelWithName(zSubject, zFullChannelName.split("\\.")[2]);
				if(xChannel == null || yChannel == null || zChannel == null) {
					if(xChannel == null) {
						String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, xFullChannelName);
						Activator.logErrorMessage(message);
					}
					if(yChannel == null) {
						String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, yFullChannelName);
						Activator.logErrorMessage(message);
					}
					if(zChannel == null) {
						String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, zFullChannelName);
						Activator.logErrorMessage(message);
					}
					return false;
				}
				channelsMap.put(seriesID, new Channel[] {xChannel, yChannel, zChannel});
			} else {
				if(!MathEngineFactory.getMathEngine().isSubjectLoaded(xSubject)) {
					String message = NLS.bind(DocometreMessages.SubjectNotLoaded, xSubject.getFullPath());
					Activator.logErrorMessage(message);
				}
				if(!MathEngineFactory.getMathEngine().isSubjectLoaded(ySubject) && !xSubject.equals(ySubject)) {
					String message = NLS.bind(DocometreMessages.SubjectNotLoaded, ySubject.getFullPath());
					Activator.logErrorMessage(message);
				}
				if(!MathEngineFactory.getMathEngine().isSubjectLoaded(zSubject) && (!xSubject.equals(zSubject) || !ySubject.equals(zSubject))) {
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
			subjectName = seriesID.split("\\.")[5];
			if(subject.getName().equals(subjectName)) return true;
		}
		return false;
		
	}

	public void setXZxisForeGroundColor(Color color) {
		zAxisColor = color.getRGB();
	}
	
	public Color getZAxisForeGroundColor() {
		if(zAxisColor == null) zAxisColor = new RGB(255, 255, 255);
		return ColorUtil.getColor(zAxisColor);
	}
	
	public void setZAxisVisibility(boolean visible) {
		zAxisVisible = visible;
	}
	
	public boolean isZAxisVisible() {
		return zAxisVisible;
	}
	
	public LineStyle getZAxisGridStyle() {
		return LineStyle.getLineStyle(zAxisGridStyle);
	}

	public void setZAxisGridStyle(String gridStyle) {
		this.zAxisGridStyle = gridStyle;
	}

	public Color getZAxisGridColor() {
		if(zAxisGridColor == null) zAxisGridColor = new RGB(255, 255, 255);
		return ColorUtil.getColor(zAxisGridColor);
	}

	public void setZAxisGridColor(Color color) {
		this.zAxisGridColor = color.getRGB();
	}
	
	public Channel[] getXYZChannels(String key) {
		return channelsMap.get(key);
	}
	
}
