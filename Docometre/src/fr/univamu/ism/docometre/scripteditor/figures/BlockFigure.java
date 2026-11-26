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

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfiguration;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;
import fr.univamu.ism.docometre.scripteditor.figures.FunctionCodeLines.Language;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Comment;
import fr.univamu.ism.process.DoBlock;
import fr.univamu.ism.process.Function;
import fr.univamu.ism.process.IfBlock;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegment;
import fr.univamu.ism.process.ScriptSegmentType;

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
	
	// Text displayed by a deactivated function
	private static final String COMMENTED = "COMMENTED";
	
	private BlockEditPart blockEditPart;
	// One label per displayed line : a function displays the first lines of its code, other blocks their name
	protected List<Label> labels = new ArrayList<>();
	private RoundedRectangle roundedRectangle;

	public BlockFigure(BlockEditPart blockEditPart) {
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
		
		updateLabel();
		setToolTipFigure(null);
	}
	
	/*
	 * Tooltip is set on the figure holding the labels : it is displayed whatever the line under the mouse
	 */
	private void setToolTipFigure(String message) {
		if(message == null) {
			roundedRectangle.setToolTip(null);
			if(blockEditPart.getModel() instanceof Function) {
				String tooltip = ((Function)blockEditPart.getModel()).getDescription(getContext());
				if(tooltip != null && !tooltip.equals("")) roundedRectangle.setToolTip(new TooltipFigure(tooltip));
			}
		} else {
			TooltipFigure tooltipFigure = new TooltipFigure(message);
			roundedRectangle.setToolTip(tooltipFigure);
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
		// Status icon is displayed in the first label
		Label label = labels.isEmpty() ? null : labels.get(0);
		if(label != null) label.setIcon(null);
		setToolTipFigure(null);
		// Code generation status (warning) and compilation status (errors and warnings located in this block)
		IStatus status = blockEditPart.getModel().getStatus();
		IStatus compilationStatus = blockEditPart.getModel().getCompilationStatus();
		if(status != null || compilationStatus != null) {
			boolean error = (status != null && status.getSeverity() == IStatus.ERROR) || (compilationStatus != null && compilationStatus.getSeverity() == IStatus.ERROR);
			Image image = Activator.getImage(error ? IImageKeys.ERROR_ANNOTATION_ICON : IImageKeys.WARNING_ANNOTATION_ICON);
			if(label != null) {
				if(image != null && !image.isDisposed()) label.setIcon(image);
				label.setIconAlignment(PositionConstants.LEFT);
			}
			String message = status != null ? status.getMessage() : "";
			if(compilationStatus != null) message = message + (message.isEmpty() ? "" : "\n") + compilationStatus.getMessage();
			setToolTipFigure(message);
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
	
	/*
	 * Returns the object edited : ADwin or Arduino Uno process for acquisition, script for data processing
	 */
	private Object getContext() {
		ResourceEditorInput resourceEditorInput = (ResourceEditorInput) ((DefaultEditDomain) blockEditPart.getRoot().getViewer().getEditDomain()).getEditorPart().getEditorInput();
		return resourceEditorInput.getObject();
	}
	
	/**
	 * Updates displayed lines : 
	 * - a deactivated function displays COMMENTED,
	 * - an activated function displays the first lines of its code, without comments (see FunctionCodeLines),
	 * - other blocks display their name.
	 */
	public void updateLabel() {
		Object context = getContext();
		Block block = blockEditPart.getModel();
		List<String> lines = new ArrayList<>();
		if(block instanceof Function) lines = getFunctionLines((Function) block, context);
		else lines.add(block.getName(context));
		setLines(lines);
	}
	
	private List<String> getFunctionLines(Function function, Object context) {
		List<String> lines = new ArrayList<>();
		if(!function.isActivated()) {
			lines.add(COMMENTED);
			return lines;
		}
		try {
			String code = function.getCode(context, getStep());
			lines = FunctionCodeLines.getDisplayedLines(code, getLanguage(context));
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
		}
		// No code to display (e.g. function not yet assigned or not available for this system) : display function title
		if(lines.isEmpty()) {
			String title = function.getTitle(context);
			lines.add(title == null || title.isEmpty() ? function.getName(context) : title);
		}
		return lines;
	}
	
	/*
	 * Code generation step is the type of the segment holding the block
	 */
	private ScriptSegmentType getStep() {
		Object segment = blockEditPart.getParent() != null ? blockEditPart.getParent().getModel() : null;
		if(segment instanceof ScriptSegment) return ((ScriptSegment) segment).getScriptSegmentType();
		return ScriptSegmentType.LOOP;
	}
	
	private Language getLanguage(Object context) {
		if(context instanceof Process) {
			DACQConfiguration dacqConfiguration = ((Process) context).getDACQConfiguration();
			if(dacqConfiguration instanceof ADWinDACQConfiguration) return Language.ADBASIC;
			if(dacqConfiguration instanceof ArduinoUnoDACQConfiguration) return Language.ARDUINO;
		}
		if(context instanceof Script) {
			if(MathEngineFactory.isMatlab()) return Language.MATLAB;
			if(MathEngineFactory.isPython()) return Language.PYTHON;
		}
		return Language.UNKNOWN;
	}
	
	/*
	 * Creates one label per line, lines group being vertically centered
	 */
	private void setLines(List<String> lines) {
		if(lines.size() != labels.size()) {
			for (Label label : labels) roundedRectangle.remove(label);
			labels.clear();
			for (int i = 0; i < lines.size(); i++) {
				Label label = new Label();
				label.setForegroundColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
				roundedRectangle.add(label);
				roundedRectangle.getLayoutManager().setConstraint(label, getLabelLayoutData(i, lines.size()));
				labels.add(label);
			}
		}
		for (int i = 0; i < lines.size(); i++) labels.get(i).setText(lines.get(i));
	}
	
	/*
	 * Extra vertical space is shared between first and last labels : above first one and below last one
	 */
	private GridData getLabelLayoutData(int index, int nbLabels) {
		if(nbLabels == 1) return new GridData(GridData.FILL_BOTH);
		if(index == 0) return new GridData(SWT.FILL, SWT.END, true, true);
		if(index == nbLabels - 1) return new GridData(SWT.FILL, SWT.BEGINNING, true, true);
		return new GridData(SWT.FILL, SWT.CENTER, true, false);
	}
	
}
