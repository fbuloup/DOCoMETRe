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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [26/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.widgets;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

/**
 * Collapsible group in the style of the Nebula PGroup, whose header bar can contain any widget : 
 * <pre>
 *  ╭──────────────────────────────────────────────────────────╮
 *  │ Title                       [tool bar widgets...]    &lt;&lt; │   collapsed
 *  ╰──────────────────────────────────────────────────────────╯
 *  ╭──────────────────────────────────────────────────────────╮
 *  │ Title                       [tool bar widgets...]    ∨∨ │   expanded
 *  ╰──────────────────────────────────────────────────────────╯
 *  ┌──────────────────────────────────────────────────────────┐
 *  │ body                                                     │
 *  └──────────────────────────────────────────────────────────┘
 * </pre>
 * Widgets of the header bar are created with {@link #getToolBar()} as parent (they are laid out from left 
 * to right, vertically centered, RowData can be used). The content is created with {@link #getBody()} as 
 * parent, whose layout is a GridLayout with one column by default. Clicking the toggle, the title or 
 * any free area of the header expands or collapses the group. The layout of this composite must not be changed.
 */
public class CollapsibleGroup extends Composite {
	
	// Horizontal and vertical paddings between the rounded border of the header and its content
	private static final int HEADER_H_PADDING = 12;
	private static final int HEADER_V_PADDING = 8;
	// Diameter of the arcs of the corners of the header
	private static final int HEADER_ARC = 14;
	// Size of the expand/collapse toggle
	private static final int TOGGLE_WIDTH = 28;
	private static final int TOGGLE_HEIGHT = 20;
	// Colors of the border, for dark and light backgrounds, and of the toggle
	private static final RGB DARK_BACKGROUND_BORDER_RGB = new RGB(200, 200, 200);
	private static final RGB LIGHT_BACKGROUND_BORDER_RGB = new RGB(160, 160, 160);
	private static final RGB TOGGLE_RGB = new RGB(33, 150, 243);
	
	private Composite header;
	private Label titleLabel;
	private Composite toolBar;
	private Canvas toggle;
	private Composite body;
	private boolean expanded;
	// Height of the header including its border, updated by the layout
	private int headerHeight;
	private Color darkBackgroundBorderColor;
	private Color lightBackgroundBorderColor;
	private Color toggleColor;
	private ArrayList<ExpandListener> expandListeners = new ArrayList<>();
	
	/*
	 * Lays out the header and, when the group is expanded, the body under it
	 */
	private final class CollapsibleGroupLayout extends Layout {
		
		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			int headerWHint = wHint == SWT.DEFAULT ? SWT.DEFAULT : Math.max(0, wHint - 2*HEADER_H_PADDING);
			Point headerSize = header.computeSize(headerWHint, SWT.DEFAULT, flushCache);
			int width = headerSize.x + 2*HEADER_H_PADDING;
			int height = headerSize.y + 2*HEADER_V_PADDING;
			if(expanded) {
				int bodyWHint = wHint == SWT.DEFAULT ? SWT.DEFAULT : Math.max(0, wHint - 2);
				Point bodySize = body.computeSize(bodyWHint, SWT.DEFAULT, flushCache);
				width = Math.max(width, bodySize.x + 2);
				height += bodySize.y + 2;
			}
			if(wHint != SWT.DEFAULT) width = wHint;
			if(hHint != SWT.DEFAULT) height = hHint;
			return new Point(width, height);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle clientArea = composite.getClientArea();
			int headerWidth = Math.max(0, clientArea.width - 2*HEADER_H_PADDING);
			int headerContentHeight = header.computeSize(headerWidth, SWT.DEFAULT, flushCache).y;
			headerHeight = headerContentHeight + 2*HEADER_V_PADDING;
			header.setBounds(clientArea.x + HEADER_H_PADDING, clientArea.y + HEADER_V_PADDING, headerWidth, headerContentHeight);
			if(expanded) body.setBounds(clientArea.x + 1, clientArea.y + headerHeight + 1, Math.max(0, clientArea.width - 2), Math.max(0, clientArea.height - headerHeight - 2));
		}
		
	}
	
	public CollapsibleGroup(Composite parent, int style) {
		super(parent, style);
		setBackgroundMode(SWT.INHERIT_DEFAULT);
		super.setLayout(new CollapsibleGroupLayout());
		
		darkBackgroundBorderColor = new Color(getDisplay(), DARK_BACKGROUND_BORDER_RGB);
		lightBackgroundBorderColor = new Color(getDisplay(), LIGHT_BACKGROUND_BORDER_RGB);
		toggleColor = new Color(getDisplay(), TOGGLE_RGB);
		
		// Header : title, tool bar and expand/collapse toggle
		header = new Composite(this, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.horizontalSpacing = 12;
		header.setLayout(gl);
		
		titleLabel = new Label(header, SWT.NONE);
		titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		toolBar = new Composite(header, SWT.NONE) {
			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {
				// No tool bar widget : no space (Composite would return a default size)
				if(getChildren().length == 0) return new Point(0, 0);
				return super.computeSize(wHint, hHint, changed);
			}
		};
		toolBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		RowLayout rl = new RowLayout(SWT.HORIZONTAL);
		rl.center = true;
		rl.wrap = false;
		rl.spacing = 10;
		rl.marginLeft = 0;
		rl.marginRight = 0;
		rl.marginTop = 0;
		rl.marginBottom = 0;
		toolBar.setLayout(rl);
		
		toggle = new Canvas(header, SWT.DOUBLE_BUFFERED);
		GridData gd = new GridData(SWT.END, SWT.CENTER, false, false);
		gd.widthHint = TOGGLE_WIDTH;
		gd.heightHint = TOGGLE_HEIGHT;
		toggle.setLayoutData(gd);
		toggle.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		toggle.addPaintListener(e -> paintToggle(e.gc));
		
		// Body : content, visible when expanded
		body = new Composite(this, SWT.NONE);
		gl = new GridLayout();
		gl.marginWidth = 10;
		gl.marginHeight = 10;
		body.setLayout(gl);
		body.setVisible(false);
		
		// Expand or collapse when clicking on toggle, title or free area of the header
		MouseListener toggleListener = new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if(e.button != 1) return;
				setExpanded(!expanded);
			}
		};
		toggle.addMouseListener(toggleListener);
		titleLabel.addMouseListener(toggleListener);
		header.addMouseListener(toggleListener);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if(e.button == 1 && e.y < headerHeight) setExpanded(!expanded);
			}
		});
		
		addPaintListener(e -> paintBorders(e.gc));
		addListener(SWT.Resize, e -> redraw());
		addDisposeListener(e -> {
			darkBackgroundBorderColor.dispose();
			lightBackgroundBorderColor.dispose();
			toggleColor.dispose();
		});
	}
	
	/*
	 * Rounded border around the header and, when expanded, rectangle around the body
	 */
	private void paintBorders(GC gc) {
		Rectangle clientArea = getClientArea();
		if(clientArea.width <= 1 || clientArea.height <= 1) return;
		gc.setAntialias(SWT.ON);
		gc.setLineWidth(1);
		gc.setForeground(getBorderColor());
		gc.drawRoundRectangle(clientArea.x, clientArea.y, clientArea.width - 1, headerHeight - 1, HEADER_ARC, HEADER_ARC);
		if(expanded) {
			gc.setAntialias(SWT.OFF);
			gc.drawRectangle(clientArea.x, clientArea.y + headerHeight, clientArea.width - 1, clientArea.height - headerHeight - 1);
		}
	}
	
	/*
	 * Two chevrons : pointing left when collapsed, pointing down when expanded
	 */
	private void paintToggle(GC gc) {
		Rectangle area = toggle.getClientArea();
		gc.setAntialias(SWT.ON);
		gc.setLineWidth(2);
		gc.setLineCap(SWT.CAP_ROUND);
		gc.setLineJoin(SWT.JOIN_ROUND);
		gc.setForeground(toggleColor);
		int cx = area.x + area.width/2;
		int cy = area.y + area.height/2;
		if(expanded) {
			int dx = 10, dy = 6;
			gc.drawPolyline(new int[] {cx - dx, cy - dy - 1, cx, cy - 1, cx + dx, cy - dy - 1});
			gc.drawPolyline(new int[] {cx - dx, cy + 1, cx, cy + dy + 1, cx + dx, cy + 1});
		} else {
			int dx = 6, dy = 8;
			gc.drawPolyline(new int[] {cx - 1, cy - dy, cx - dx - 1, cy, cx - 1, cy + dy});
			gc.drawPolyline(new int[] {cx + dx + 1, cy - dy, cx + 1, cy, cx + dx + 1, cy + dy});
		}
	}
	
	private Color getBorderColor() {
		RGB rgb = getBackground().getRGB();
		double luminance = 0.299*rgb.red + 0.587*rgb.green + 0.114*rgb.blue;
		return luminance < 128 ? darkBackgroundBorderColor : lightBackgroundBorderColor;
	}
	
	/**
	 * @return the composite in which the widgets of the header bar are created, laid out from left to right 
	 * (RowLayout) between the title and the toggle
	 */
	public Composite getToolBar() {
		checkWidget();
		return toolBar;
	}
	
	/**
	 * @return the composite in which the content of the group is created (GridLayout with one column by default)
	 */
	public Composite getBody() {
		checkWidget();
		return body;
	}
	
	/**
	 * @return the label of the title, e.g. to change its font or add a tooltip
	 */
	public Label getTitleLabel() {
		checkWidget();
		return titleLabel;
	}
	
	public void setText(String text) {
		checkWidget();
		titleLabel.setText(text == null ? "" : text);
		header.layout(new Control[] {titleLabel});
	}
	
	public String getText() {
		checkWidget();
		return titleLabel.getText();
	}
	
	public void setToggleToolTipText(String toolTipText) {
		checkWidget();
		toggle.setToolTipText(toolTipText);
	}
	
	public boolean getExpanded() {
		checkWidget();
		return expanded;
	}
	
	/**
	 * Expands or collapses the group, then notifies the expand listeners. The parent is
	 * asked to lay out again (see {@link Control#requestLayout()}).
	 */
	public void setExpanded(boolean expanded) {
		checkWidget();
		if(this.expanded == expanded) return;
		this.expanded = expanded;
		body.setVisible(expanded);
		toggle.redraw();
		layout(true);
		redraw();
		Event event = new Event();
		event.widget = this;
		event.display = getDisplay();
		ExpandEvent expandEvent = new ExpandEvent(event);
		for (ExpandListener expandListener : expandListeners.toArray(new ExpandListener[expandListeners.size()])) {
			if(expanded) expandListener.itemExpanded(expandEvent);
			else expandListener.itemCollapsed(expandEvent);
		}
		if(!isDisposed()) requestLayout();
	}
	
	/**
	 * Listener notified each time the group is expanded or collapsed (after the change)
	 */
	public void addExpandListener(ExpandListener expandListener) {
		checkWidget();
		if(!expandListeners.contains(expandListener)) expandListeners.add(expandListener);
	}
	
	public void removeExpandListener(ExpandListener expandListener) {
		checkWidget();
		expandListeners.remove(expandListener);
	}
	
}
