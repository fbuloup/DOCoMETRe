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

import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.TextFlow;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;

/**
 * Figure of global declarations in INITIALIZE segment editor : 
 * numbers of libraries, constants and subroutines, each one with its icon
 */
public class GlobalDeclarationsFigure extends RoundedRectangle {
	
	private static final String GLOBAL_DECLARATIONS_COLOR = "GLOBAL_DECLARATIONS_COLOR";
	private static final String GLOBAL_DECLARATIONS_BORDER_COLOR = "GLOBAL_DECLARATIONS_BORDER_COLOR";
	
	static {
		JFaceResources.getColorRegistry().put(GLOBAL_DECLARATIONS_BORDER_COLOR, new RGB(70, 130, 110));
		JFaceResources.getColorRegistry().put(GLOBAL_DECLARATIONS_COLOR, new RGB(200, 230, 215));
	}
	
	private Label librariesLabel;
	private Label constantsLabel;
	private Label subroutinesLabel;

	public GlobalDeclarationsFigure() {
		setCornerDimensions(new Dimension(15, 15));
		setBackgroundColor(JFaceResources.getColorRegistry().get(GLOBAL_DECLARATIONS_BORDER_COLOR));
		
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.marginHeight = 2;
		gridLayout.marginWidth = 2;
		gridLayout.verticalSpacing = 0;
		setLayoutManager(gridLayout);
		
		RoundedRectangle roundedRectangle = new RoundedRectangle();
		roundedRectangle.setCornerDimensions(new Dimension(10, 10));
		roundedRectangle.setBackgroundColor(JFaceResources.getColorRegistry().get(GLOBAL_DECLARATIONS_COLOR));
		roundedRectangle.setForegroundColor(JFaceResources.getColorRegistry().get(GLOBAL_DECLARATIONS_COLOR));
		getLayoutManager().setConstraint(roundedRectangle, new GridData(GridData.FILL_BOTH));
		add(roundedRectangle);
		
		GridLayout innerGridLayout = new GridLayout(3, true);
		innerGridLayout.marginHeight = 0;
		innerGridLayout.marginWidth = 6;
		innerGridLayout.horizontalSpacing = 10;
		roundedRectangle.setLayoutManager(innerGridLayout);
		
		librariesLabel = createLabel(roundedRectangle, IImageKeys.LIBRARY_ICON);
		constantsLabel = createLabel(roundedRectangle, IImageKeys.CONSTANT_ICON);
		subroutinesLabel = createLabel(roundedRectangle, IImageKeys.SUBROUTINE_ICON);
	}
	
	private static Label createLabel(IFigure parent, String imageKey) {
		Label label = new Label("0", Activator.getImage(imageKey));
		label.setForegroundColor(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		label.setLabelAlignment(PositionConstants.LEFT);
		label.setIconTextGap(4);
		parent.add(label);
		parent.getLayoutManager().setConstraint(label, new GridData(GridData.FILL, GridData.CENTER, true, true));
		return label;
	}
	
	public void update(int nbLibraries, int nbConstants, int nbSubroutines, String toolTip) {
		librariesLabel.setText(String.valueOf(nbLibraries));
		constantsLabel.setText(String.valueOf(nbConstants));
		subroutinesLabel.setText(String.valueOf(nbSubroutines));
		FlowPage toolTipFigure = new FlowPage();
		toolTipFigure.setOpaque(true);
		toolTipFigure.setBorder(new MarginBorder(0, 2, 1, 0));
		TextFlow textFlow = new TextFlow();
		textFlow.setText(toolTip);
		toolTipFigure.add(textFlow);
		setToolTip(toolTipFigure);
	}

}
