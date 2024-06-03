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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [01/06/2024]
 ******************************************************************************/
package fr.univamu.ism.nrtswtchart;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class RTSWTOscilloChartTest {
	
	private static Random random = new Random();
	private static RTSWTOscilloSerie rtswtSerie1;
	private static RTSWTOscilloSerie rtswtSerie2;
	private static RTSWTOscilloSerie rtswtSerie3;
	private static RTSWTOscilloSerie rtswtSerie4;
	
	private static class GenerateData extends TimerTask {
		private double previousTime = 0;
		@Override
		public void run() {
			
			double saveprevioustime = previousTime;
			int nbSamples = 1 + random.nextInt(1000);
			final Double[] x = new Double[nbSamples];
			final Double[] y = new Double[nbSamples];
			for (int i = 0; i < y.length; i++) {
				x[i] = previousTime + i/1000.0;
				y[i] = 15*Math.sin(2*Math.PI*1*x[i]) + (random.nextDouble() - 0.5)*2;
//				y[i] = y[i]*1E200;/* Just to have big big numbers */
			}
			previousTime = x[x.length - 1];
			rtswtSerie1.addPoints(x, y);
			
			previousTime = saveprevioustime;
			for (int i = 0; i < y.length; i++) {
				x[i] = previousTime + i/1000.0;
				y[i] = 3*Math.sin(2*Math.PI*1*x[i] + Math.PI/3) + (random.nextDouble() - 0.5)*2;
//				y[i] = y[i]*1E200;/* Just to have big big numbers */
			}
			previousTime = x[x.length - 1];
			rtswtSerie2.addPoints(x, y);
			
			previousTime = saveprevioustime;
			for (int i = 0; i < y.length; i++) {
				x[i] = previousTime + i/1000.0;
				y[i] = 0.5*Math.sin(2*Math.PI*1*x[i] + 2*Math.PI/3) + (random.nextDouble() - 0.5)*2;
//				y[i] = y[i]*1E200;/* Just to have big big numbers */
			}
			previousTime = x[x.length - 1];
			rtswtSerie3.addPoints(x, y);
			
			previousTime = saveprevioustime;
			for (int i = 0; i < y.length; i++) {
				x[i] = previousTime + i/1000.0;
				y[i] = 1*Math.sin(2*Math.PI*1*x[i] + Math.PI) + (random.nextDouble() - 0.5)*2;
//				y[i] = y[i]*1E200;/* Just to have big big numbers */
			}
			previousTime = x[x.length - 1];
			rtswtSerie4.addPoints(x, y);
		}
	}

	public static void main(String[] args) {
		
		boolean displayLoop = true;
		
		if(args.length > 0) {
			int i = 0;
			while (i < args.length) {
				if(args[i].equalsIgnoreCase("-displayLoop")) {
					displayLoop = Boolean.parseBoolean(args[i+1]);
					i++;
				}
				i++;
			}
		}
		
		/*Retrieve default display and create new shell*/
		Display display = Display.getDefault();
		if(display == null) display = new Display();
		Shell shell = new Shell (display);
		shell.setMaximized(true);
		shell.setText("RTSWT Test");
		shell.setLayout(new FillLayout());
		
		RTSWTOscilloChart rtswtChart = new RTSWTOscilloChart(shell, SWT.DOUBLE_BUFFERED, null,  SWT.BOLD | SWT.NORMAL, 14);
		rtswtChart.setWindowTimeWidth(10);
		rtswtChart.setAutoScale(false);
		rtswtChart.setGridVisibility(true);
		rtswtChart.setLegendVisibility(true);
		rtswtChart.setLegendPosition(SWT.BOTTOM);
		rtswtSerie1 = rtswtChart.createSerie("Serie1", display.getSystemColor(SWT.COLOR_GREEN));
		rtswtSerie2 = rtswtChart.createSerie("Serie2", display.getSystemColor(SWT.COLOR_RED));
		rtswtSerie3 = rtswtChart.createSerie("Serie3", display.getSystemColor(SWT.COLOR_CYAN));
		rtswtSerie4 = rtswtChart.createSerie("Serie2", display.getSystemColor(SWT.COLOR_DARK_MAGENTA));
		
		rtswtChart.createSerie(RTSWTOscilloSerie.HORIZONTAL_REFERENCE + "[3]", display.getSystemColor(SWT.COLOR_WHITE));
		
		shell.open();
		
		Timer timer1 = new Timer();
		timer1.schedule(new GenerateData(), 100, 10);
		
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				/* Stop timers, dispose display and exit */
				timer1.cancel();
//				timer2.cancel();
				System.out.println(rtswtChart.getMeanDrawTime());
			}
		});
		
		if(displayLoop) {
			/* SWT display loop */
			while (!shell.isDisposed ()) {
				if (!display.readAndDispatch ()) display.sleep ();
			}
			display.dispose ();
		}

	}

}
