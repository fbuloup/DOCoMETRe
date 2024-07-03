package fr.univamu.ism.nswtchart;

import java.awt.geom.Point2D;

public class Window {
		
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
			return "xMin : " + topLeft.x + " - yMax : " + topLeft.y + " - xMax : " + bottomRight.x + " - yMin : " + bottomRight.y;
		}

		public void reorder() {
			double xMin = getXMin();
			double xMax = getXMax();
			double yMin = getYMin();
			double yMax = getYMax();
			if(xMin > xMax) {
				topLeft.x = xMax;
				bottomRight.x = xMin;
 			}
			if(yMin > yMax) {
				topLeft.y = yMin;
				bottomRight.y = yMax;
 			}
		}
		
		public boolean areSameConers() {
			return topLeft.x == bottomRight.x || topLeft.y == bottomRight.y;
		}
		
		public double getDx() {
			return getXMax() - getXMin();
		}
		
		public double getDy() {
			return getYMax() - getYMin();
		}

		public void zoom(int count) {
			// TODO Auto-generated method stub
			
		}
		
	}