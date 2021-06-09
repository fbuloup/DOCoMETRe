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
