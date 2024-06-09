package org.eclipse.nebula.visualization.widgets.examples;
/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.widgets.figures.GaugeFigure;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


/**
 * A live updated Gauge Example.
 * @author Xihui Chen
 *
 */
public class GaugeExample {
	
	private static class GaugeContainer extends Canvas {

		public GaugeContainer(Composite parent, int style) {
			super(parent, style);
		}
		
		@Override
		public Point computeSize(int wHint, int hHint, boolean changed) {
			int size = Math.min(getParent().getBounds().width, getParent().getBounds().height);
			return super.computeSize(size, size, changed);
		}
		
	}
	
	private static int counter = 0;
	public static void main(String[] args) {
		final Shell shell = new Shell();
		shell.setSize(600, 800);
		shell.open();
		shell.setBackground(XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
	    
	    shell.setLayout(new GridLayout());
	    GaugeContainer gaugeContainer = new GaugeContainer(shell, SWT.BORDER);
	    gaugeContainer.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
//	    GridData gd = (GridData)gaugeContainer.getLayoutData();
//	    gd.minimumHeight = SWT.DEFAULT;
//	    gd.minimumWidth = SWT.DEFAULT;
//	    gd.heightHint = 500;
//	    gd.widthHint = 500;
	    
	    //use LightweightSystem to create the bridge between SWT and draw2D
		final LightweightSystem lws = new LightweightSystem(gaugeContainer);		
		
		//Create Gauge
		final GaugeFigure gaugeFigure = new GaugeFigure();
		
		//Init gauge
		gaugeFigure.setBackgroundColor(
				XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
		gaugeFigure.setForegroundColor(
				XYGraphMediaFactory.getInstance().getColor(255, 255, 255));
		
		gaugeFigure.setRange(-100, 100);
		gaugeFigure.setLoLevel(-50);
		gaugeFigure.setLoloLevel(-80);
		gaugeFigure.setHiLevel(60);
		gaugeFigure.setHihiLevel(80);
		gaugeFigure.setMajorTickMarkStepHint(50);
		
		lws.setContents(gaugeFigure);		
		shell.pack();
		//Update the gauge in another thread.
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				Display.getDefault().asyncExec(new Runnable() {					
					@Override
					public void run() {
						gaugeFigure.setValue(Math.sin(counter++/10.0)*100);			
//						lws.getUpdateManager().addInvalidFigure(gaugeFigure);
						lws.getUpdateManager().performUpdate();
					}
				});
			}
		}, 100, 100, TimeUnit.MILLISECONDS);		
		
	    Display display = Display.getDefault();
	    while (!shell.isDisposed()) {
	      if (!display.readAndDispatch())
	        display.sleep();
	    }
	    future.cancel(true);
	    scheduler.shutdown();
	   
	}
}
