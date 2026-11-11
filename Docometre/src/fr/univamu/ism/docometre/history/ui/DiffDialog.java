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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [04/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.history.ui;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ThemeColors;
import fr.univamu.ism.docometre.history.SnapshotRepository;
import fr.univamu.ism.docometre.history.SnapshotRepository.FileDiff;

/**
 * Show differences of a file (unified diff) : added lines in green, removed lines in red.
 */
public class DiffDialog extends Dialog {
	
	private final String title;
	private final String subtitle;
	private final FileDiff fileDiff;

	public DiffDialog(Shell parentShell, String title, String subtitle, FileDiff fileDiff) {
		super(parentShell);
		this.title = title;
		this.subtitle = subtitle;
		this.fileDiff = fileDiff;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Label subtitleLabel = new Label(area, SWT.NONE);
		subtitleLabel.setText(subtitle);
		subtitleLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(subtitleLabel);
		if(fileDiff.result() != FileDiff.Result.DIFFERENCES) {
			Label messageLabel = new Label(area, SWT.WRAP);
			messageLabel.setText(getMessage(fileDiff.result()));
			GridDataFactory.fillDefaults().grab(true, true).hint(500, SWT.DEFAULT).applyTo(messageLabel);
			return area;
		}
		StyledText styledText = new StyledText(area, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		styledText.setFont(JFaceResources.getTextFont());
		styledText.setText(fileDiff.text());
		boolean dark = ThemeColors.isDarkTheme();
		Color addedColor = dark ? new Color(110, 200, 140) : new Color(0, 128, 40);
		Color removedColor = dark ? new Color(240, 125, 125) : new Color(180, 30, 30);
		Color hunkColor = dark ? new Color(130, 170, 230) : new Color(40, 80, 160);
		ArrayList<StyleRange> styleRanges = new ArrayList<>();
		String text = styledText.getText();
		int offset = 0;
		for (String line : text.split("\n", -1)) {
			Color color = null;
			if(line.startsWith("+")) color = addedColor;
			else if(line.startsWith("-")) color = removedColor;
			else if(line.startsWith("@@")) color = hunkColor;
			if(color != null && !line.isEmpty()) styleRanges.add(new StyleRange(offset, line.length(), color, null));
			offset += line.length() + 1;
		}
		styledText.setStyleRanges(styleRanges.toArray(new StyleRange[styleRanges.size()]));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(styledText);
		return area;
	}
	
	private static String getMessage(FileDiff.Result result) {
		switch (result) {
		case IDENTICAL:
			return DocometreMessages.History_DiffIdentical;
		case BINARY:
			return DocometreMessages.History_DiffBinary;
		default:
			return NLS.bind(DocometreMessages.History_DiffTooLarge, SnapshotRepository.MAXIMUM_DIFF_SIZE / (1024 * 1024));
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if(buttonId == IDialogConstants.CLOSE_ID) close();
		else super.buttonPressed(buttonId);
	}
	
	@Override
	protected Point getInitialSize() {
		return fileDiff.result() == FileDiff.Result.DIFFERENCES ? new Point(800, 600) : super.getInitialSize();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

}
