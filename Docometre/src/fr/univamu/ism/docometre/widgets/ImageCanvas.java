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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [11/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Canvas which displays an image centered and scaled to fit, keeping its proportions,
 * or a message in red (error message for example). The image given to this canvas is
 * disposed by it (when replaced or when the canvas is disposed).
 * Used by image charts and by the preview of the images files editor.
 */
public class ImageCanvas extends Canvas {
	
	private Image image;
	private String message;

	public ImageCanvas(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);
		addPaintListener(event -> paint(event.gc));
		addDisposeListener(event -> disposeImage());
	}
	
	/**
	 * Displays this image, previous image is disposed
	 * @param image the image to display, disposed by this canvas, null for no image
	 */
	public void setImage(Image image) {
		disposeImage();
		this.image = image;
		this.message = null;
		redraw();
	}
	
	/**
	 * Displays this message in red instead of the image, previous image is disposed
	 * @param message the message to display, null for no message
	 */
	public void setMessage(String message) {
		disposeImage();
		this.message = message;
		redraw();
	}
	
	private void disposeImage() {
		if(image != null && !image.isDisposed()) image.dispose();
		image = null;
	}
	
	private void paint(GC gc) {
		Rectangle area = getClientArea();
		if(area.width <= 0 || area.height <= 0) return;
		if(image == null || image.isDisposed()) {
			if(message == null || message.isEmpty()) return;
			// Message wrapped to the width of the canvas
			TextLayout textLayout = new TextLayout(getDisplay());
			textLayout.setText(message);
			textLayout.setFont(getFont());
			textLayout.setWidth(Math.max(1, area.width - 10));
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
			textLayout.draw(gc, area.x + 5, area.y + 5);
			textLayout.dispose();
			return;
		}
		Rectangle bounds = image.getBounds();
		if(bounds.width <= 0 || bounds.height <= 0) return;
		double scale = Math.min((double)area.width / bounds.width, (double)area.height / bounds.height);
		int width = (int) Math.round(bounds.width * scale);
		int height = (int) Math.round(bounds.height * scale);
		int x = area.x + (area.width - width) / 2;
		int y = area.y + (area.height - height) / 2;
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		gc.drawImage(image, 0, 0, bounds.width, bounds.height, x, y, width, height);
	}

}
