package fr.univamu.ism.docometre.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import fr.univamu.ism.docometre.DocometreMessages;

public class MathEnginePrefrencePage extends PreferencePage implements IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NORMAL);
		container.setLayout(new FillLayout());
		Label label = new Label(container, SWT.NORMAL);
		label.setText(DocometreMessages.RootPreferencePage_Label);
		
		return container;
	}

}
