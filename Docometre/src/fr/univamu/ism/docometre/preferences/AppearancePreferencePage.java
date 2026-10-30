package fr.univamu.ism.docometre.preferences;

import java.util.List;

import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.DocometreMessages;

/*
 * Theme selection (light, dark...) with public IThemeEngine API
 */
public class AppearancePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private IThemeEngine themeEngine;
	private List<ITheme> themes;
	private Combo themeCombo;

	@Override
	public void init(IWorkbench workbench) {
		setDescription(DocometreMessages.AppearancePreferences_Description);
		themeEngine = workbench.getService(IThemeEngine.class);
		noDefaultButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label themeLabel = new Label(container, SWT.NONE);
		themeLabel.setText(DocometreMessages.AppearancePreferences_Theme);
		themeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		themeCombo = new Combo(container, SWT.READ_ONLY);
		themeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		if(themeEngine == null) {
			themeCombo.setEnabled(false);
			return container;
		}

		themes = themeEngine.getThemes();
		ITheme activeTheme = themeEngine.getActiveTheme();
		for (ITheme theme : themes) {
			themeCombo.add(theme.getLabel());
			if(activeTheme != null && activeTheme.getId().equals(theme.getId())) themeCombo.select(themeCombo.getItemCount() - 1);
		}

		return container;
	}

	@Override
	public boolean performOk() {
		if(themeEngine != null && themeCombo.getSelectionIndex() >= 0) {
			ITheme theme = themes.get(themeCombo.getSelectionIndex());
			ITheme activeTheme = themeEngine.getActiveTheme();
			if(activeTheme == null || !activeTheme.getId().equals(theme.getId())) {
				// Saved in preferences, used at next startup
				themeEngine.setTheme(theme, true);
				// Colors, icons and native widgets are set at startup : restart is needed
				if(MessageDialog.openQuestion(getShell(), DocometreMessages.AppearancePreferences_RestartTitle, DocometreMessages.AppearancePreferences_RestartQuestion)) {
					getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							PlatformUI.getWorkbench().restart();
						}
					});
				}
			}
		}
		return super.performOk();
	}

}
