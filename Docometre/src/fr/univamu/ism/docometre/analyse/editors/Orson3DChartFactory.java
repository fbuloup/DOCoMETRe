package fr.univamu.ism.docometre.analyse.editors;

import java.awt.BasicStroke;
import java.awt.Color;

import org.jfree.chart3d.Chart3D;
import org.jfree.chart3d.Chart3DFactory;
import org.jfree.chart3d.Chart3DPanel;
import org.jfree.chart3d.axis.NumberAxis3D;
import org.jfree.chart3d.data.xyz.XYZSeriesCollection;
import org.jfree.chart3d.plot.XYZPlot;
import org.jfree.chart3d.style.StandardChartStyle;
import org.jfree.chart3d.table.StandardRectanglePainter;

public final class Orson3DChartFactory {
	
	public static Chart3DPanel create3DChart() {
		Chart3D chart3d = Chart3DFactory.createXYZLineChart(null, null, new XYZSeriesCollection<>(), "x", "y", "z");
		
		((StandardChartStyle)chart3d.getStyle()).setAxisLabelColor(Color.WHITE);
//		((XYZPlot)chart3d.getPlot()).getXAxis().setLabelColor(Color.WHITE);
//		((XYZPlot)chart3d.getPlot()).getYAxis().setLabelColor(Color.WHITE);
//		((XYZPlot)chart3d.getPlot()).getZAxis().setLabelColor(Color.WHITE);
		
		((StandardChartStyle)chart3d.getStyle()).setBackgroundPainter(new StandardRectanglePainter(Color.BLACK));
//		chart3d.setBackground(new StandardRectanglePainter(Color.BLACK));
		
		((StandardChartStyle)chart3d.getStyle()).setChartBoxColor(Color.WHITE);
//		chart3d.setChartBoxColor(Color.WHITE);
		
		((StandardChartStyle)chart3d.getStyle()).setAxisTickLabelColor(Color.WHITE);
//		((XYZPlot)chart3d.getPlot()).getXAxis().setTickLabelColor(Color.WHITE);
//		((XYZPlot)chart3d.getPlot()).getYAxis().setTickLabelColor(Color.WHITE);
//		((XYZPlot)chart3d.getPlot()).getZAxis().setTickLabelColor(Color.WHITE);
		
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getXAxis()).setLineColor(Color.RED);
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getYAxis()).setLineColor(Color.BLUE);
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getZAxis()).setLineColor(Color.GREEN);
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getXAxis()).setLineStroke(new BasicStroke(3));
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getYAxis()).setLineStroke(new BasicStroke(3));
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getZAxis()).setLineStroke(new BasicStroke(3));
		
		((StandardChartStyle)chart3d.getStyle()).setLegendItemBackgroundColor(Color.BLACK);
		((StandardChartStyle)chart3d.getStyle()).setLegendItemColor(Color.WHITE);
		
		Chart3DPanel chart3DPanel = new Chart3DPanel(chart3d);
		return chart3DPanel;
	}

}
