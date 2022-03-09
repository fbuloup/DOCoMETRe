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
package fr.univamu.ism.docometre.analyse.datamodel;

import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swtchart.LineStyle;
import org.jzy3d.maths.Coord3d;

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
	private Coord3d viewPoint;
	
	
	public XYZChart() {
		super();
		zMax = 10;
		zMin = -10;
		viewPoint = Coord3d.ORIGIN;
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
		setzMin(zMin);
		setzMax(zMax);
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
			String xFullChannelName = seriesID.split("\\(")[1].split(",")[0];
			String yFullChannelName = seriesID.split("\\(")[1].split(",")[1].replaceAll("\\)", "");
			
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
	
	public void setViewPoint(Coord3d viewPoint) {
		this.viewPoint = viewPoint;
	}
	
	public Coord3d getViewPoint() {
		return viewPoint;
	}
	
}
