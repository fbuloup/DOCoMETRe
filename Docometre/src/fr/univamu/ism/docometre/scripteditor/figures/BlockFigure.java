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
package fr.univamu.ism.docometre.scripteditor.figures;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.TextFlow;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Comment;
import fr.univamu.ism.process.DoBlock;
import fr.univamu.ism.process.Function;
import fr.univamu.ism.process.IfBlock;

public class BlockFigure extends RoundedRectangle {
	
	private class TooltipFigure extends FlowPage {
		
		private TextFlow message;

		public TooltipFigure(String text) {
	        setOpaque(true);
	        setBorder(new MarginBorder(0, 2, 1, 0));
	        message = new TextFlow();
	        message.setText(text);
	        add(message);
	    }
		
		@Override
	    public Dimension getPreferredSize(int w, int h) {
	        Dimension d = super.getPreferredSize(-1, -1);
	        if (d.width > 1000)
	            d = super.getPreferredSize(1000, -1);
	        return d;
	    }
		
	}

	// Colors IDs
	private static final String IF_COLOR = "IF_COLOR";
	private static final String DO_COLOR = "DO_COLOR";
	private static final String FUNCTION_COLOR = "FUNCTION_COLOR";
	private static final String COMMENT_COLOR = "COMMENT_COLOR";
	private static final String DEACTIVATED_COLOR = "DEACTIVATED_COLOR";
	
	private static final String IF_BORDER_COLOR = "IF_BORDER_COLOR";
	private static final String DO_BORDER_COLOR = "DO_BORDER_COLOR";
	private static final String FUNCTION_BORDER_COLOR = "FUNCTION_BORDER_COLOR";
	private static final String COMMENT_BORDER_COLOR = "COMMENT_BORDER_COLOR";
	private static final String DEACTIVATED_BORDER_COLOR = "DEACTIVATED_BORDER_COLOR";

	static {
		JFaceResources.getColorRegistry().put(IF_BORDER_COLOR, new RGB(25, 60, 180));
		JFaceResources.getColorRegistry().put(DO_BORDER_COLOR, new RGB(255, 69, 0));
		JFaceResources.getColorRegistry().put(FUNCTION_BORDER_COLOR, new RGB(218, 165, 32));
		JFaceResources.getColorRegistry().put(COMMENT_BORDER_COLOR, new RGB(255, 140, 0));
		JFaceResources.getColorRegistry().put(DEACTIVATED_COLOR, new RGB(50,205,50));

		JFaceResources.getColorRegistry().put(IF_COLOR, new RGB(179, 192, 239));
		JFaceResources.getColorRegistry().put(DO_COLOR, new RGB(241, 157, 122));
		JFaceResources.getColorRegistry().put(FUNCTION_COLOR, new RGB(235, 215, 162));
		JFaceResources.getColorRegistry().put(COMMENT_COLOR, new RGB(254, 216, 177));
		JFaceResources.getColorRegistry().put(DEACTIVATED_BORDER_COLOR, new RGB(34,139,34));
	}
	
	private BlockEditPart blockEditPart;
	protected Label label;
	private RoundedRectangle roundedRectangle;

	public BlockFigure(String title, BlockEditPart blockEditPart) {
		this.blockEditPart = blockEditPart;

		setCornerDimensions(new Dimension(15, 15));
		setBackgroundColor(getBorderColor());
		
		GridLayout gd1 = new GridLayout(1, false);
		gd1.horizontalSpacing = 0;
		gd1.marginHeight = 2;
		gd1.marginWidth = 2;
		gd1.verticalSpacing = 0;
		setLayoutManager(gd1);
		
		roundedRectangle = new RoundedRectangle();
		roundedRectangle.setCornerDimensions(new Dimension(10, 10));
		roundedRectangle.setBackgroundColor(getColor());
		roundedRectangle.setForegroundColor(getColor());
		getLayoutManager().setConstraint(roundedRectangle, new GridData(GridData.FILL_BOTH));
		add(roundedRectangle);

		GridLayout gd2 = new GridLayout(1, false);
		gd2.horizontalSpacing = 0;
		gd2.marginHeight = 0;
		gd2.marginWidth = 0;
		gd2.verticalSpacing = 0;
		roundedRectangle.setLayoutManager(gd2);
		
		label = new Label(title);
		setToolTipFigure(null);
		label.setForegroundColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		roundedRectangle.add(label);
		roundedRectangle.getLayoutManager().setConstraint(label, new GridData(GridData.FILL_BOTH));
	}
	
	private void setToolTipFigure(String message) {
		if(message == null) {
			label.setToolTip(null);
			if(blockEditPart.getModel() instanceof Function) {
				ResourceEditorInput resourceEditorInput = (ResourceEditorInput) ((DefaultEditDomain) blockEditPart.getRoot().getViewer().getEditDomain()).getEditorPart().getEditorInput();
				Object object = resourceEditorInput.getObject();
				String tooltip = ((Function)blockEditPart.getModel()).getDescription(object);
				if(tooltip != null && !tooltip.equals("")) label.setToolTip(new TooltipFigure(tooltip));
			}
		} else {
			TooltipFigure tooltipFigure = new TooltipFigure(message);
			label.setToolTip(tooltipFigure);
		}
	}
	
	protected BlockEditPart getBlockEditPart() {
		return blockEditPart;
	}
	
	@Override
	public void paintFigure(Graphics graphics) {
		setBackgroundColor(getBorderColor());
		roundedRectangle.setBackgroundColor(getColor());
		roundedRectangle.setForegroundColor(getColor());
		
		fillShape(graphics);
		if(label.getIcon() != null && !label.getIcon().isDisposed()) label.getIcon().dispose();
		label.setIcon(null);
		setToolTipFigure(null);
		IStatus status = blockEditPart.getModel().getStatus();
		if(status != null) {
			label.setIcon(Activator.getImage(IImageKeys.WARNING_ANNOTATION_ICON));
			label.setIconAlignment(PositionConstants.LEFT);
			setToolTipFigure(status.getMessage());
		}
	}
	
	private Color getColor() {
		if(!getBlockEditPart().getModel().isActivated()) return JFaceResources.getColorRegistry().get(DEACTIVATED_COLOR);
		if (getBlockEditPart().getModel() instanceof IfBlock) return JFaceResources.getColorRegistry().get(IF_COLOR);
		if (getBlockEditPart().getModel() instanceof DoBlock) return JFaceResources.getColorRegistry().get(DO_COLOR);
		if (getBlockEditPart().getModel() instanceof Comment) return JFaceResources.getColorRegistry().get(COMMENT_COLOR);
		return JFaceResources.getColorRegistry().get(FUNCTION_COLOR);
	} 
	
	private Color getBorderColor() {
		if(!getBlockEditPart().getModel().isActivated()) return JFaceResources.getColorRegistry().get(DEACTIVATED_BORDER_COLOR);
		if (getBlockEditPart().getModel() instanceof IfBlock) return JFaceResources.getColorRegistry().get(IF_BORDER_COLOR);
		if (getBlockEditPart().getModel() instanceof DoBlock) return JFaceResources.getColorRegistry().get(DO_BORDER_COLOR);
		if (getBlockEditPart().getModel() instanceof Comment) return JFaceResources.getColorRegistry().get(COMMENT_BORDER_COLOR);
		return JFaceResources.getColorRegistry().get(FUNCTION_BORDER_COLOR);
	} 
	
	public void updateLabel() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput) ((DefaultEditDomain) blockEditPart.getRoot().getViewer().getEditDomain()).getEditorPart().getEditorInput();
		Object object = resourceEditorInput.getObject();
		label.setText(((Block)blockEditPart.getModel()).getName(object));
	}
	
}
