package fr.univamu.ism.docometre.chartxyz.editors;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

import fr.univamu.ism.docometre.chartxyz.AbstractGLChart;

public final class ChartPartListener {
	
	public static void install(IWorkbenchPart part, AbstractGLChart<?,?> chart) {
		IPartListener2 partListener2 = new IPartListener2() {
			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == part && chart != null && !chart.isDisposed()) chart.stopAnimator();
			}
			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if(partRef.getPart(false) == part && chart != null && !chart.isDisposed()) chart.startAnimator();
			}
		};
		part.getSite().getPage().addPartListener(partListener2);
		chart.getControl().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				part.getSite().getPage().removePartListener(partListener2);
			}
		});
	}

}
