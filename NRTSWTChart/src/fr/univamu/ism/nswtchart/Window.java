package fr.univamu.ism.nswtchart;

import java.awt.geom.Point2D;

public class Window {
	
	public static Window fromString(String stringValues) {
		Window window = new Window(0, 0, 0, 0);
		double tx = 0, ty = 0, bx = 0, by = 0;
		try {
			String[] values = stringValues.split(" - ");
			if(values.length == 4) {
				for (int i = 0; i < 4; i++) {
					String[] currentValues = values[i].split(" : ");
					if(currentValues.length == 2) {
						double value = Double.parseDouble(currentValues[1]);
						if(i == 0) tx = value;
						else if(i == 1) ty = value;
						else if(i == 2) bx = value;
						else if(i == 3) by = value;
					}
				}
			}
			window.topLeft.x = tx;
			window.topLeft.y = ty;
			window.bottomRight.x = bx;
			window.bottomRight.y = by;
			return window;
		} finally {
			if(window.topLeft.equals(new Point2D.Double(0, 0)) && window.bottomRight.equals(new Point2D.Double(0, 0))) return null;
		}
		
	}

		
	private Point2D.Double topLeft;
	private Point2D.Double bottomRight;
	
	public Window(double xMin, double xMax, double yMin, double yMax) {
		topLeft = new Point2D.Double(xMin, yMax);
		bottomRight = new Point2D.Double(xMax, yMin);
	}
	
	public void setTopLeft(Point2D.Double topLeft) {
		this.topLeft = topLeft;
	}
	
	public void setBottomRight(Point2D.Double bottomRight) {
		this.bottomRight = bottomRight;
	}
	
	public double getXMin() {
		return topLeft.getX();
	}
	
	public double getXMax() {
		return bottomRight.getX();
	}
	
	public double getYMin() {
		return bottomRight.getY();
	}
	
	public double getYMax() {
		return topLeft.getY();
	}
	
	public double getWidth() {
		return getXMax() - getXMin();
	}
	
	public double getHeight() {
		return getYMax() - getYMin();
	}
	
	public boolean isInside(double x, double y) {
		if(x < getXMin() || x > getXMax()) return false;
		if(y < getYMin() || y > getYMax()) return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "topLeft.x : " + topLeft.x + " - topLeft.y : " + topLeft.y + " - bottomRight.x : " + bottomRight.x + " - bottomRight.y : " + bottomRight.y;
	}
	
	public double getDx() {
		return getXMax() - getXMin();
	}
	
	public double getDy() {
		return getYMax() - getYMin();
	}

	public void zoom(int count, double xValue, double yValue) {
		if(count > 0) {
			topLeft.x = (xValue + getXMin())/2;
			bottomRight.x = (xValue + getXMax())/2;
			bottomRight.y = (yValue + getYMin())/2;
			topLeft.y = (yValue + getYMax())/2;
		} else {
			topLeft.x = topLeft.x - Math.abs(xValue - getXMin())/2;
			topLeft.y = topLeft.y + Math.abs(yValue - getYMax())/2;
			bottomRight.x = bottomRight.x + Math.abs(xValue - getXMax())/2;
			bottomRight.y = bottomRight.y - Math.abs(yValue - getYMin())/2;
		}
	}

		
	}