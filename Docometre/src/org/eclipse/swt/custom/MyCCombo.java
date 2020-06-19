package org.eclipse.swt.custom;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class MyCCombo extends Composite {

	private Text text;
	private Button button;

	public MyCCombo(Composite parent, int style) {
		super(parent, SWT.NO_FOCUS | ((style & SWT.BORDER) > 0 ? (style ^ SWT.BORDER) : style) );
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		setLayout(gl);
		
		
		int textStyle = SWT.SINGLE;
		if ((style & SWT.READ_ONLY) != 0) textStyle |= SWT.READ_ONLY;
		if ((style & SWT.FLAT) != 0) textStyle |= SWT.FLAT;
		if ((style & SWT.BORDER) != 0) textStyle |= SWT.BORDER;
		textStyle |= style & (SWT.LEAD | SWT.CENTER | SWT.TRAIL);
		text = new Text(this, textStyle);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		int arrowStyle = SWT.ARROW | SWT.DOWN;
		if ((style & SWT.FLAT) != 0) arrowStyle |= SWT.FLAT;
		button = new Button(this, arrowStyle);
		button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
	}
	

}
