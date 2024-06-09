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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Spinner;

import fr.univamu.ism.docometre.analyse.datamodel.XYChart;

public class FrontEndCutsHandler extends SelectionAdapter implements TraverseListener, MouseWheelListener {
	
	private Spinner spinner;
	private Chart2D3DBehaviour chartBehaviour;
	private String key;
	private XYChart xyChartData;

	public FrontEndCutsHandler(Spinner spinner, Chart2D3DBehaviour chartBehaviour) {
		this.chartBehaviour = chartBehaviour;
		this.spinner = spinner;
		this.key = (String) spinner.getData();
		this.xyChartData = chartBehaviour.getChartData();
	}

	@Override
	public void keyTraversed(TraverseEvent e) {
		if(e.detail == SWT.TRAVERSE_RETURN) {
			if(key.equals("frontCut")) {
				int fc = spinner.getSelection();
				int ec = xyChartData.getEndCut();
				if(fc < ec) {
					if(spinner.getMinimum() <= fc) {
						chartBehaviour.updateFrontEndCutsChartHandler();
					}
				} else {
					spinner.setSelection(ec - 1);
					chartBehaviour.updateFrontEndCutsChartHandler();
				}
			}
			if(key.equals("endCut")) {
				int ec = spinner.getSelection();
				int fc = xyChartData.getFrontCut();
				if(ec > fc) {
					if(spinner.getMaximum() >= ec) {
						chartBehaviour.updateFrontEndCutsChartHandler();
					}
				} else {
					spinner.setSelection(fc + 1);
					chartBehaviour.updateFrontEndCutsChartHandler();
				}
			}
			chartBehaviour.setDirty(true);
		}
	}

	@Override
	public void mouseScrolled(MouseEvent e) {
		int value1 = spinner.getSelection() + e.count;
		int value2 = value1;
		if(key.equals("frontCut")) {
			value2 = xyChartData.getEndCut();
			if(value1 < value2) {
				spinner.setSelection(value1);
				chartBehaviour.updateFrontEndCutsChartHandler();
				chartBehaviour.setDirty(true);
			} 
		}
		if(key.equals("endCut")) {
			value2 = xyChartData.getFrontCut();
			if(value1 > value2) {
				spinner.setSelection(value1);
				chartBehaviour.updateFrontEndCutsChartHandler();
				chartBehaviour.setDirty(true);
			} 
		}
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if(e.stateMask != SWT.BUTTON1) return;
		int value1 = spinner.getSelection();
		int value2 = value1;
		if(key.equals("frontCut")) {
			value2 = xyChartData.getEndCut();
			if(value1 < value2) {
				if(spinner.getMinimum() <= value1) {
					chartBehaviour.updateFrontEndCutsChartHandler();
				}
			} else {
				chartBehaviour.updateFrontEndCutsChartHandler();
				spinner.setSelection(value2 - 1);
			}
		}
		if(key.equals("endCut")) {
			value2 = xyChartData.getFrontCut();
			if(value1 > value2) {
				chartBehaviour.updateFrontEndCutsChartHandler();
			} else spinner.setSelection(value2 + 1);
		}
		spinner.getParent().getParent().setFocus();
		chartBehaviour.setDirty(true);
	}

}
