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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

/**
 * Collapsible group in the style of the Nebula PGroup, whose header bar can contain any widget : 
 * <pre>
 *  ╭──────────────────────────────────────────────────────────╮
 *  │ Title                       [tool bar widgets...]  [◄]  │   collapsed
 *  ╰──────────────────────────────────────────────────────────╯
 *  ╭──────────────────────────────────────────────────────────╮
 *  │ Title                       [tool bar widgets...]  [▼]  │   expanded
 *  │                                                          │
 *  │ body                                                     │
 *  ╰──────────────────────────────────────────────────────────╯
 * </pre>
 * Widgets of the header bar are created with {@link #getToolBar()} as parent (they are laid out from left 
 * to right, vertically centered, RowData can be used). The content is created with {@link #getBody()} as 
 * parent, whose layout is a GridLayout with one column by default. Clicking the toggle, the title or 
 * any free area of the header expands or collapses the group. The images of the toggle are set with 
 * {@link #setToggleImages(Image, Image)}. The layout of this composite must not be changed.
 */
public class CollapsibleGroup extends Composite {
	
	// Horizontal and vertical paddings between the rounded border of the header and its content
	private static final int HEADER_H_PADDING = 12;
	private static final int HEADER_V_PADDING = 8;
	// Diameter of the arcs of the corners of the header
	private static final int HEADER_ARC = 14;
	// Space between the border and the left, right and bottom sides of the body : the body must not hide
	// the rounded corners of the border
	private static final int BODY_INSET = HEADER_ARC/2;
	// Colors of the border, for dark and light backgrounds
	private static final RGB DARK_BACKGROUND_BORDER_RGB = new RGB(200, 200, 200);
	private static final RGB LIGHT_BACKGROUND_BORDER_RGB = new RGB(160, 160, 160);
	
	private Composite header;
	private Label titleLabel;
	private Composite toolBar;
	private Label toggle;
	// Images of the toggle when collapsed and expanded (text if null)
	private Image collapsedImage;
	private Image expandedImage;
	private Composite body;
	private boolean expanded;
	// Height of the header including its border, updated by the layout
	private int headerHeight;
	private Color darkBackgroundBorderColor;
	private Color lightBackgroundBorderColor;
	// Bold font of the title, disposed with the group
	private Font titleFont;
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
				int bodyWHint = wHint == SWT.DEFAULT ? SWT.DEFAULT : Math.max(0, wHint - 2*BODY_INSET);
				Point bodySize = body.computeSize(bodyWHint, SWT.DEFAULT, flushCache);
				width = Math.max(width, bodySize.x + 2*BODY_INSET);
				height += bodySize.y + BODY_INSET;
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
			if(expanded) body.setBounds(clientArea.x + BODY_INSET, clientArea.y + headerHeight, Math.max(0, clientArea.width - 2*BODY_INSET), Math.max(0, clientArea.height - headerHeight - BODY_INSET));
		}
		
	}
	
	public CollapsibleGroup(Composite parent, int style) {
		super(parent, style);
		setBackgroundMode(SWT.INHERIT_DEFAULT);
		super.setLayout(new CollapsibleGroupLayout());
		
		darkBackgroundBorderColor = new Color(getDisplay(), DARK_BACKGROUND_BORDER_RGB);
		lightBackgroundBorderColor = new Color(getDisplay(), LIGHT_BACKGROUND_BORDER_RGB);
		
		// Header : title, tool bar and expand/collapse toggle
		header = new Composite(this, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.horizontalSpacing = 12;
		header.setLayout(gl);
		
		titleLabel = new Label(header, SWT.NONE);
		titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		// Title in bold
		FontData[] fontData = titleLabel.getFont().getFontData();
		for (FontData fd : fontData) fd.setStyle(fd.getStyle() | SWT.BOLD);
		titleFont = new Font(getDisplay(), fontData);
		titleLabel.setFont(titleFont);
		
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
		
		toggle = new Label(header, SWT.NONE);
		toggle.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		toggle.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		updateToggle();
		
		// Body : content, visible when expanded
		body = new Composite(this, SWT.NONE);
		gl = new GridLayout();
		// With the insets of the body : 10 pixels from the border
		gl.marginWidth = 10 - BODY_INSET;
		gl.marginHeight = 0;
		gl.marginTop = 2;
		gl.marginBottom = 10 - BODY_INSET;
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
			titleFont.dispose();
		});
	}
	
	/*
	 * Rounded border around the header, or around the header and the body when expanded
	 */
	private void paintBorders(GC gc) {
		Rectangle clientArea = getClientArea();
		if(clientArea.width <= 1 || clientArea.height <= 1) return;
		gc.setAntialias(SWT.ON);
		gc.setLineWidth(1);
		gc.setForeground(getBorderColor());
		int height = expanded ? clientArea.height : headerHeight;
		gc.drawRoundRectangle(clientArea.x, clientArea.y, clientArea.width - 1, height - 1, HEADER_ARC, HEADER_ARC);
	}
	
	/*
	 * Image of the toggle depending on the state, or text if there is no image
	 */
	private void updateToggle() {
		Image image = expanded ? expandedImage : collapsedImage;
		if(image != null) toggle.setImage(image);
		else toggle.setText(expanded ? "\u25BC" : "\u25C4");
		header.layout(new Control[] {toggle});
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
	
	/**
	 * Images of the expand/collapse toggle
	 * @param collapsedImage shown when the group is collapsed (click to expand)
	 * @param expandedImage shown when the group is expanded (click to collapse)
	 */
	public void setToggleImages(Image collapsedImage, Image expandedImage) {
		checkWidget();
		this.collapsedImage = collapsedImage;
		this.expandedImage = expandedImage;
		updateToggle();
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
		updateToggle();
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
