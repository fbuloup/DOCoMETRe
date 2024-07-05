package fr.univamu.ism.nswtchart;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.swt.graphics.Point;

public class ClearCursorsThread extends Thread {
	
	private boolean terminate = false;
	private ConcurrentLinkedQueue<Point> cursorPositions = new ConcurrentLinkedQueue<Point>();
	private XYSWTChart xyswtChart;
	
	public ClearCursorsThread(XYSWTChart xyswtChart) {
		this.xyswtChart = xyswtChart;
	}
	
	@Override
	public void run() {
		while (!terminate) {
			Point cursorPosition = cursorPositions.poll();
			if(cursorPosition != null) {
				
			}
		}
	}
	
	public void terminate() {
		terminate = true;
	}
	
	public void addCursorPosition(Point cursorPosition) {
		cursorPositions.add(cursorPosition);
	}

}
