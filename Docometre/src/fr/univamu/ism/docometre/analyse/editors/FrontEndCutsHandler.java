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
	private XYChartEditor xyChartEditor;
	private String key;
	private XYChart xyChartData;

	public FrontEndCutsHandler(Spinner spinner, XYChartEditor xyChartEditor) {
		this.xyChartEditor = xyChartEditor;
		this.spinner = spinner;
		this.key = (String) spinner.getData();
		this.xyChartData = xyChartEditor.getXYChartData();
	}

	@Override
	public void keyTraversed(TraverseEvent e) {
		if(e.detail == SWT.TRAVERSE_RETURN) {
			if(key.equals("frontCut")) {
				int fc = spinner.getSelection();
				int ec = xyChartData.getEndCut();
				if(fc < ec) {
					if(spinner.getMinimum() <= fc) {
						xyChartEditor.updateFrontEndCutsChartHandler();
					}
				} else {
					spinner.setSelection(ec - 1);
					xyChartEditor.updateFrontEndCutsChartHandler();
				}
			}
			if(key.equals("endCut")) {
				int ec = spinner.getSelection();
				int fc = xyChartData.getFrontCut();
				if(ec > fc) {
					if(spinner.getMaximum() >= ec) {
						xyChartEditor.updateFrontEndCutsChartHandler();
					}
				} else {
					spinner.setSelection(fc + 1);
					xyChartEditor.updateFrontEndCutsChartHandler();
				}
			}
			xyChartEditor.setDirty(true);
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
				xyChartEditor.updateFrontEndCutsChartHandler();
				xyChartEditor.setDirty(true);
			} 
		}
		if(key.equals("endCut")) {
			value2 = xyChartData.getFrontCut();
			if(value1 > value2) {
				spinner.setSelection(value1);
				xyChartEditor.updateFrontEndCutsChartHandler();
				xyChartEditor.setDirty(true);
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
					xyChartEditor.updateFrontEndCutsChartHandler();
				}
			} else {
				xyChartEditor.updateFrontEndCutsChartHandler();
				spinner.setSelection(value2 - 1);
			}
		}
		if(key.equals("endCut")) {
			value2 = xyChartData.getFrontCut();
			if(value1 > value2) {
				xyChartEditor.updateFrontEndCutsChartHandler();
			} else spinner.setSelection(value2 + 1);
		}
		spinner.getParent().getParent().setFocus();
		xyChartEditor.setDirty(true);
	}

}
