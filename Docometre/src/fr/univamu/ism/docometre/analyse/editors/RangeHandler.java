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
