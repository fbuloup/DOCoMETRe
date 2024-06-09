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

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.analyse.datamodel.XYChart;
import fr.univamu.ism.docometre.analyse.datamodel.XYZChart;

public class RangeHandler implements TraverseListener {
	
	private Text rangeText;
	private XYChart xyChartData;
	private Chart2D3DBehaviour chartBehaviour;

	public RangeHandler(Text rangeText, Chart2D3DBehaviour chartBehaviour) {
		this.chartBehaviour = chartBehaviour;
		this.rangeText = rangeText;
		xyChartData = chartBehaviour.getChartData();
	}

	@Override
	public void keyTraversed(TraverseEvent e) {
		if(e.detail == SWT.TRAVERSE_RETURN) {
			String valueString = rangeText.getText();
			String key = (String) rangeText.getData();
			if(Pattern.matches("^(-)?\\d+(\\.\\d*)?$", valueString)) {
				double value1 = Double.parseDouble(rangeText.getText());
				double value2 = value1;
				if(key.equals("xMin")) {
					value2 = xyChartData.getxMax();
					if(value2 <= value1) return;
					xyChartData.setxMin(value1);
				}
				if(key.equals("xMax")) {
					value2 = xyChartData.getxMin();
					if(value2 >= value1) return;
					xyChartData.setxMax(value1);
				}
				if(key.equals("yMin")) {
					value2 = xyChartData.getyMax();
					if(value2 <= value1) return;
					xyChartData.setyMin(value1);
				}
				if(key.equals("yMax")) {
					value2 = xyChartData.getyMin();
					if(value2 >= value1) return;
					xyChartData.setyMax(value1);
				}
				if(key.equals("zMin")) {
					value2 = ((XYZChart)xyChartData).getzMax();
					if(value2 <= value1) return;
					((XYZChart)xyChartData).setzMin(value1);
				}
				if(key.equals("zMax")) {
					value2 = ((XYZChart)xyChartData).getzMin();
					if(value2 >= value1) return;
					((XYZChart)xyChartData).setzMax(value1);
				}
				
				if(key.equals("xMin") || key.equals("xMax")) {
					chartBehaviour.updateXAxisRange(xyChartData.getxMin(), xyChartData.getxMax());
				}
				if(key.equals("yMin") || key.equals("yMax")) {
					chartBehaviour.updateYAxisRange(xyChartData.getyMin(), xyChartData.getyMax());
				}
				if(key.equals("zMin") || key.equals("zMax")) {
					chartBehaviour.updateZAxisRange(((XYZChart)xyChartData).getzMin(), ((XYZChart)xyChartData).getzMax());
				}
				chartBehaviour.redraw();
				chartBehaviour.setDirty(true);
			} else {
				if(key.equals("xMin")) rangeText.setText(Double.toString(xyChartData.getxMin()));
				if(key.equals("xMax")) rangeText.setText(Double.toString(xyChartData.getxMax()));
				if(key.equals("yMin")) rangeText.setText(Double.toString(xyChartData.getyMin()));
				if(key.equals("yMax")) rangeText.setText(Double.toString(xyChartData.getyMax()));
			}
		}

	}

}
