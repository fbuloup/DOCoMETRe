package fr.univamu.ism.rtswtchart;

import org.eclipse.swt.widgets.Composite;

public final class RTSWTChartsFactory {
	
	public static IRTSWTOscilloChart createPureSWTOscilloChart(Composite parent, int style, String fontName, int fontStyle, int fontSize) {
		IRTSWTOscilloChart chart =  new fr.univamu.ism.internal.nrtswtchart.RTSWTOscilloChart(parent, style, fontName, fontStyle, fontSize);
		return chart;
	}
	
	public static IRTSWTOscilloChart createOpenGLOscilloChart(Composite parent, int style, String fontName, int fontStyle, int fontSize) {
		IRTSWTOscilloChart chart = new fr.univamu.ism.internal.rtswtchart.RTSWTOscilloChart(parent, style, fontName, fontStyle, fontSize);
		return chart;
	}


}
