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

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.extensions.charts.InteractiveChart;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public final class ChannelEditorWidgetsFactory {
	
	private static Color colorWhite = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_WHITE);
	private static Color colorBlack = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK);
	
	public static Label createLabel(Composite parent, String text, int horizontalAlignment, boolean grabExcessHorizontal) {
		Label label = new Label(parent, SWT.NORMAL);
		label.setLayoutData(new GridData(horizontalAlignment, SWT.CENTER, grabExcessHorizontal, false));
		label.setText(text);
		return label;
	}
	
	public static Spinner createSpinner(Composite parent, int horizontalAlignment, boolean grabExcessHorizontal) {
		Spinner spinner = new Spinner(parent, SWT.BORDER | SWT.READ_ONLY);
		spinner.setLayoutData(new GridData(horizontalAlignment, SWT.CENTER, grabExcessHorizontal, false));
		spinner.setMinimum(0);
		spinner.setMaximum(0);
		spinner.setSelection(0);
		spinner.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				spinner.setSelection(spinner.getSelection() + e.count);
				
			}
		});
		return spinner;
	}
	
	public static ComboViewer createCombo(Composite parent, int horizontalAlignment, boolean grabExcessHorizontal) {
		ComboViewer comboViewer = new ComboViewer(parent, SWT.FLAT | SWT.BORDER | SWT.READ_ONLY);
		comboViewer.getCombo().setLayoutData(new GridData(horizontalAlignment, SWT.CENTER, grabExcessHorizontal, false));
		return comboViewer;
	}
	
	public static Group createGroup(Composite parent, String title) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setText(title);
		return group;
	}

	public static void createSeparator(Composite parent, boolean grabExcessHorizontal, boolean grabExcessVertical, int horizontalSpan, int style) {
		Label separator = new Label(parent, SWT.SEPARATOR | style);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, grabExcessHorizontal, grabExcessVertical, horizontalSpan, 1));
	}

	public static InteractiveChart createChart(Composite parent, int horizontalSpan) {
		InteractiveChart chart = new InteractiveChart(parent, SWT.BORDER);
		boolean showCursor = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SHOW_CURSOR);
		boolean showMarker = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SHOW_MARKER);
		chart.setShowCursor(showCursor);
		chart.setShowMarker(showMarker);
		chart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, horizontalSpan, 1));
		chart.setBackground(colorBlack);
		chart.getPlotArea().setBackground(colorBlack);
		chart.getLegend().setBackground(colorBlack);
		chart.getLegend().setForeground(colorWhite);
		IAxis[] axes = chart.getAxisSet().getAxes();
		for (IAxis axe : axes) {
			axe.getTick().setForeground(colorWhite);
		}
		chart.getTitle().setVisible(false);
		chart.getAxisSet().getXAxes()[0].getTitle().setForeground(colorWhite);
		chart.getAxisSet().getXAxes()[0].getTitle().setText("Time (s)");
		chart.getAxisSet().getYAxes()[0].getTitle().setVisible(false);
		chart.getLegend().setPosition(SWT.BOTTOM);
		chart.setSelectionRectangleColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
		return chart;
	}

}
