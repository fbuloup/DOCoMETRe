package fr.univamu.ism.docometre.analyse.editors;

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtchart.Range;
import org.eclipse.swtchart.extensions.charts.InteractiveChart;

import fr.univamu.ism.docometre.analyse.datamodel.XYChart;

public class RangeHandler implements TraverseListener {
	
	private Text rangeText;
	private XYChart xyChartData;
	private InteractiveChart chart;
	private XYChartEditor xyChartEditor;

	public RangeHandler(Text rangeText, XYChartEditor xyChartEditor) {
		this.xyChartEditor = xyChartEditor;
		this.rangeText = rangeText;
		xyChartData = xyChartEditor.getXYChartData();
		chart = xyChartEditor.getChart();
	}

	@Override
	public void keyTraversed(TraverseEvent e) {
		if(e.detail == SWT.TRAVERSE_RETURN) {
			String valueString = rangeText.getText();
			String key = (String) rangeText.getData();
			if(Pattern.matches("^(-)?\\d+(\\.\\d*)?$", valueString)) {
				double value1 = Double.parseDouble(rangeText.getText());
				double value2 = value1;
				Range range = new Range(value1, value2);
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
				
				if(key.equals("xMin") || key.equals("xMax")) {
					range = new Range(xyChartData.getxMin(), xyChartData.getxMax());
					chart.getAxisSet().getXAxis(0).setRange(range);
				}
				if(key.equals("yMin") || key.equals("yMax")) {
					range = new Range(xyChartData.getyMin(), xyChartData.getyMax());
					chart.getAxisSet().getYAxis(0).setRange(range);
				}
				chart.redraw();
				xyChartEditor.setDirty(true);
			} else {
				if(key.equals("xMin")) rangeText.setText(Double.toString(xyChartData.getxMin()));
				if(key.equals("xMax")) rangeText.setText(Double.toString(xyChartData.getxMax()));
				if(key.equals("yMin")) rangeText.setText(Double.toString(xyChartData.getyMin()));
				if(key.equals("yMax")) rangeText.setText(Double.toString(xyChartData.getyMax()));
			}
		}

	}

}
