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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swtchart.LineStyle;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.dacqsystems.AbstractElement;

public class XYChart extends AbstractElement {

	private static final long serialVersionUID = 1L;
	
	protected transient Map<String, Channel[]> channelsMap;
	protected Set<String> seriesIDs;
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
	
	private RGB backGroundRGBColor;
	private RGB plotAreaBackGroundRGBColor;
	
	private RGB legendBackGroundRGBColor;
	private RGB legendForeGroundRGBColor;
	private boolean legendVisible;
	
	private boolean xAxisVisible;
	private boolean yAxisVisible;
	private RGB xAxisColor;
	private RGB yAxisColor;
	
	private String xAxisGridStyle;
	private String yAxisGridStyle;
	private RGB xAxisGridColor;
	private RGB yAxisGridColor;
	private boolean useSameColorForSameCategory;
	
	public XYChart() {
		seriesIDs = new HashSet<>();
		selectedTrialsNumbers = new ArrayList<>();
		autoScale = true;
		xMax = 10;
		xMin = -10;
		yMax = 10;
		yMin = -10;
		frontCut = -1;
		endCut = -1;
		backGroundRGBColor = new RGB(0, 0, 0);
		plotAreaBackGroundRGBColor = new RGB(0, 0, 0);
		legendBackGroundRGBColor = new RGB(0, 0, 0);
		legendForeGroundRGBColor = new RGB(255,  255,  255);
		legendVisible= true;
		xAxisColor = new RGB(255,  255,  255);
		yAxisColor = new RGB(255,  255,  255);
		xAxisVisible = true;
		yAxisVisible = true;
		useSameColorForSameCategory = true;
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
	
	public boolean isUseSameColorForSameCategory() {
		return useSameColorForSameCategory;
	}

	public void setUseSameColorForSameCategory(boolean useSameColorForSameCategory) {
		this.useSameColorForSameCategory = useSameColorForSameCategory;
	}

	public String[] getSeriesIDsPrefixes() {
		return seriesIDs.toArray(new String[seriesIDs.size()]);
	}
	
	public void addCurve(Channel xChannel, Channel yChannel) {
		String key = yChannel.getFullName() + "(" + xChannel.getFullName() + ")";
		seriesIDs.add(key);
		channelsMap.put(key, new Channel[] {xChannel, yChannel});
	}
	
	public void removeCurve(String key) {
		seriesIDs.remove(key);
		channelsMap.remove(key);
		if(seriesIDs.isEmpty()) selectedTrialsNumbers = new ArrayList<Integer>();
	}
	
	public int getNbCurves() {
		return seriesIDs.size();
	}
	
	public Collection<Channel[]> getChannels() {
		return channelsMap.values();
	}
	
	public Set<String> getCurvesIDs() {
		return channelsMap.keySet();
	}
	
	public Channel[] getXYChannels(String key) {
		return channelsMap.get(key);
	}

	public void setSelectedTrialsNumbers(List<Integer> selectedTrialsNumbers) {
		this.selectedTrialsNumbers = selectedTrialsNumbers;
	}
	
	public List<Integer> getSelectedTrialsNumbers() {
		if(selectedTrialsNumbers == null) return new ArrayList<>();
		return selectedTrialsNumbers;
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
				channelsMap.put(seriesID, new Channel[] {xChannel, yChannel});
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

	public void setBackGroundColor(Color background) {
		backGroundRGBColor = background.getRGB();
	}
	
	public Color getBackGroundColor() {
		if(backGroundRGBColor == null) backGroundRGBColor = new RGB(0, 0, 0);
		return DocometreApplication.getColor(backGroundRGBColor);
	}

	public void setPlotAreaBackGroundColor(Color backgroundInPlotArea) {
		plotAreaBackGroundRGBColor = backgroundInPlotArea.getRGB();
	}
	
	public Color getPlotAreaBackGroundColor() {
		if(plotAreaBackGroundRGBColor == null) plotAreaBackGroundRGBColor = new RGB(0, 0, 0);
		return DocometreApplication.getColor(plotAreaBackGroundRGBColor);
	}
	
	public void setLegendBackGroundColor(Color color) {
		legendBackGroundRGBColor = color.getRGB();
	}
	
	public Color getLegendBackGroundColor() {
		if(legendBackGroundRGBColor == null) legendBackGroundRGBColor = new RGB(0, 0, 0);
		return DocometreApplication.getColor(legendBackGroundRGBColor);
	}
	
	public void setLegendForeGroundColor(Color color) {
		legendForeGroundRGBColor = color.getRGB();
	}
	
	public Color getLegendForeGroundColor() {
		if(legendForeGroundRGBColor == null) legendForeGroundRGBColor = new RGB(255, 255, 255);
		return DocometreApplication.getColor(legendForeGroundRGBColor);
	}
	
	public boolean isLegendVisible() {
		return legendVisible;
	}
	
	public void setLegendVisible(boolean visible) {
		legendVisible = visible;
	}
	
	public void setXAxisForeGroundColor(Color color) {
		xAxisColor = color.getRGB();
	}
	
	public Color getXAxisForeGroundColor() {
		if(xAxisColor == null) xAxisColor = new RGB(255, 255, 255);
		return DocometreApplication.getColor(xAxisColor);
	}
	
	public void setYAxisForeGroundColor(Color color) {
		yAxisColor = color.getRGB();
	}
	
	public Color getYAxisForeGroundColor() {
		if(yAxisColor == null) yAxisColor = new RGB(255, 255, 255);
		return DocometreApplication.getColor(yAxisColor);
	}
	
	public void setXAxisVisibility(boolean visible) {
		xAxisVisible = visible;
	}
	
	public boolean isXAxisVisible() {
		return xAxisVisible;
	}
	
	public void setYAxisVisibility(boolean visible) {
		yAxisVisible = visible;
	}
	
	public boolean isYAxisVisible() {
		return yAxisVisible;
	}

	public LineStyle getXAxisGridStyle() {
		return LineStyle.getLineStyle(xAxisGridStyle);
	}

	public void setXAxisGridStyle(String gridStyle) {
		this.xAxisGridStyle = gridStyle;
	}

	public Color getXAxisGridColor() {
		if(xAxisGridColor == null) xAxisGridColor = new RGB(255, 255, 255);
		return DocometreApplication.getColor(xAxisGridColor);
	}

	public void setXAxisGridColor(Color color) {
		this.xAxisGridColor = color.getRGB();
	}
	
	public LineStyle getYAxisGridStyle() {
		return LineStyle.getLineStyle(yAxisGridStyle);
	}

	public void setYAxisGridStyle(String gridStyle) {
		this.yAxisGridStyle = gridStyle;
	}

	public Color getYAxisGridColor() {
		if(yAxisGridColor == null) yAxisGridColor = new RGB(255, 255, 255);
		return DocometreApplication.getColor(yAxisGridColor);
	}

	public void setYAxisGridColor(Color color) {
		this.yAxisGridColor = color.getRGB();
	}
	
	

}
