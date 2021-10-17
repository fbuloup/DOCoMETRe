package fr.univamu.ism.docometre.analyse.editors;

import java.awt.BasicStroke;
import java.awt.Color;

import org.jfree.chart3d.Chart3D;
import org.jfree.chart3d.Chart3DFactory;
import org.jfree.chart3d.Chart3DPanel;
import org.jfree.chart3d.axis.NumberAxis3D;
import org.jfree.chart3d.data.xyz.XYZSeriesCollection;
import org.jfree.chart3d.plot.XYZPlot;
import org.jfree.chart3d.table.StandardRectanglePainter;

public final class Orson3DChartFactory {
	
	public static Chart3DPanel create3DChart() {
		Chart3D chart3d = Chart3DFactory.createXYZLineChart(null, null, new XYZSeriesCollection<>(), "x", "y", "z");
		chart3d.setBackground(new StandardRectanglePainter(Color.BLACK));
		chart3d.setChartBoxColor(Color.DARK_GRAY);
		
		((XYZPlot)chart3d.getPlot()).getXAxis().setLabelColor(Color.WHITE);
		((XYZPlot)chart3d.getPlot()).getYAxis().setLabelColor(Color.WHITE);
		((XYZPlot)chart3d.getPlot()).getZAxis().setLabelColor(Color.WHITE);
		((XYZPlot)chart3d.getPlot()).getXAxis().setTickLabelColor(Color.WHITE);
		((XYZPlot)chart3d.getPlot()).getYAxis().setTickLabelColor(Color.WHITE);
		((XYZPlot)chart3d.getPlot()).getZAxis().setTickLabelColor(Color.WHITE);
		
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getXAxis()).setLineColor(Color.WHITE);
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getYAxis()).setLineColor(Color.WHITE);
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getZAxis()).setLineColor(Color.WHITE);
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getXAxis()).setLineStroke(new BasicStroke(0.5f));
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getYAxis()).setLineStroke(new BasicStroke(0.5f));
		((NumberAxis3D)((XYZPlot)chart3d.getPlot()).getZAxis()).setLineStroke(new BasicStroke(0.5f));
		
		Chart3DPanel chart3DPanel = new Chart3DPanel(chart3d);
		return chart3DPanel;
	}

}
