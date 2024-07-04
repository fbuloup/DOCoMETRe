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
package fr.univamu.ism.nswtchart;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class SWTXYChartTest {

	private static Shell shell;

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
		shell = new Shell (display);
		shell.setMaximized(true);
		shell.setText("XY SWT Test");
		shell.setLayout(new FillLayout());

		/* Create the first chart */
		XYSWTChart chart = new XYSWTChart(shell, SWT.DOUBLE_BUFFERED, "Arial", SWT.BOLD | SWT.ITALIC, 8);
		chart.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		

		double fe = 1000;
		int D = 16;
		int NMAX = (int)(D*fe);
		double[] xValues = new double[NMAX];
		double[] yValues = new double[NMAX];
		for (int i = 0; i < yValues.length; i++) {
			xValues[i] = i / fe;
			yValues[i] = 3*Math.sin(2*Math.PI*1*xValues[i]);
		}
		
		/* Create two series in this first chart */
		chart.createSerie(xValues, yValues, "serie1", display.getSystemColor(SWT.COLOR_GREEN), 1);
		
		
		xValues = new double[NMAX];
		yValues = new double[NMAX];
		for (int i = 0; i < yValues.length; i++) {
			xValues[i] = i / fe;
			yValues[i] = 3*Math.sin(2*Math.PI*2*xValues[i]);
		}
		
		/* Create two series in this first chart */
		chart.createSerie(xValues, yValues, "serie2", display.getSystemColor(SWT.COLOR_BLUE), 3);

		/* Open the shell to display charts */
		shell.open ();

		if(displayLoop) {
			/* SWT display loop */
			while (!shell.isDisposed ()) {
				if (!display.readAndDispatch ()) display.sleep ();
			}
			display.dispose ();
		}

	}

}