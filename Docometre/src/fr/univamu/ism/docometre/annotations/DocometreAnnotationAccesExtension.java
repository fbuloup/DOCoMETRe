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
package fr.univamu.ism.docometre.annotations;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.ImageUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;

public class DocometreAnnotationAccesExtension implements IAnnotationAccessExtension, IAnnotationAccess {
		
		private Image image;
		private MouseMoveListener mouseMoveListener;
		private Rectangle lastPosition;
		
		public DocometreAnnotationAccesExtension() {
		}

		@Override
		public String getTypeLabel(Annotation annotation) {
			return null;
		}

		@Override
		public int getLayer(Annotation annotation) {
			return 0;
		}

		@Override
		public void paint(Annotation annotation, GC gc, Canvas canvas, Rectangle bounds) {

			//if(image != null && !image.isDisposed()) image.dispose();
			
			switch (annotation.getType()) {
			case ErrorAnnotation.TYPE_ERROR:
				image = Activator.getImage(IImageKeys.ERROR_ANNOTATION_ICON);
				break;
			case WarningAnnotation.TYPE_WARNING:
				image = Activator.getImage(IImageKeys.WARNING_ANNOTATION_ICON);
				break;
			default :
				Activator.getSharedImage("icons/full/etool16/help_contents.png"); // this is IWorkbenchGraphicConstants.IMG_ETOOL_HELP_CONTENTS from org.eclipse.ui
				break;
			}
			
			if(mouseMoveListener == null) {
				mouseMoveListener = new MouseMoveListener() {
					@Override
					public void mouseMove(MouseEvent e) {
						canvas.setCursor(null);
						if(lastPosition == null) return;
						if(e.y >= lastPosition.y && e.y <= lastPosition.y + lastPosition.height) canvas.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
					}
				};
				canvas.addMouseMoveListener(mouseMoveListener);
			}
			
			
			if (image != null && gc != null && canvas!= null && !image.isDisposed() && !gc.isDisposed() && !canvas.isDisposed()) {
				lastPosition = bounds;
				ImageUtilities.drawImage(image, gc, canvas, bounds, SWT.CENTER, SWT.TOP);
			}
			
		}

		@Override
		public boolean isPaintable(Annotation annotation) {
			return true;
		}

		@Override
		public boolean isSubtype(Object annotationType, Object potentialSupertype) {
			return annotationType.equals(potentialSupertype);
		}

		@Override
		public Object[] getSupertypes(Object annotationType) {
			return null;
		}

		@Override
		public Object getType(Annotation annotation) {
			return annotation.getType();
		}

		@Override
		public boolean isMultiLine(Annotation annotation) {
			return true;
		}

		@Override
		public boolean isTemporary(Annotation annotation) {
			return false;
		}

		public void dispose() {
			if(image != null && !image.isDisposed()) image.dispose();
		}

		
	}
