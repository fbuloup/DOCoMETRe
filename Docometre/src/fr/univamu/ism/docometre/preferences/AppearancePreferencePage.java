package fr.univamu.ism.docometre.preferences;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
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
import org.osgi.service.prefs.BackingStoreException;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.Languages;
import fr.univamu.ism.docometre.ThemeColors;

/*
 * Theme selection : system, light or dark.
 * Eclipse theme engine API is not public : selected theme is saved in the preference
 * read by the theme engine at startup (no preference means follow system).
 * Language selection : system or one of available languages (see Languages).
 */
public class AppearancePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int SYSTEM = 0;
	private static final int LIGHT = 1;
	private static final int DARK = 2;

	private Combo themeCombo;
	private int initialSelection;
	private Combo languageCombo;
	// SYSTEM followed by available languages, in language combo order
	private String[] languages;
	private int initialLanguageSelection;

	@Override
	public void init(IWorkbench workbench) {
		setDescription(DocometreMessages.AppearancePreferences_Description);
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
		themeCombo.setItems(new String[] { DocometreMessages.AppearancePreferences_SystemTheme, DocometreMessages.AppearancePreferences_LightTheme, DocometreMessages.AppearancePreferences_DarkTheme });

		String themeId = getThemePreferences().get(ThemeColors.THEME_ID_KEY, null);
		if(themeId == null) initialSelection = SYSTEM;
		else initialSelection = themeId.contains("dark") ? DARK : LIGHT;
		themeCombo.select(initialSelection);
		
		Label languageLabel = new Label(container, SWT.NONE);
		languageLabel.setText(DocometreMessages.AppearancePreferences_Language);
		languageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		languageCombo = new Combo(container, SWT.READ_ONLY);
		languageCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String[] availableLanguages = Languages.getAvailableLanguages();
		languages = new String[availableLanguages.length + 1];
		languages[0] = Languages.SYSTEM;
		System.arraycopy(availableLanguages, 0, languages, 1, availableLanguages.length);
		languageCombo.add(DocometreMessages.AppearancePreferences_SystemLanguage);
		for (String language : availableLanguages) languageCombo.add(Languages.getLanguageName(language));
		
		// Preferred language may not be available anymore : system is then selected
		initialLanguageSelection = 0;
		String preferredLanguage = Languages.getPreferredLanguage();
		for (int i = 0; i < languages.length; i++) if(languages[i].equals(preferredLanguage)) initialLanguageSelection = i;
		languageCombo.select(initialLanguageSelection);

		return container;
	}

	@Override
	public boolean performOk() {
		boolean restart = false;
		int selection = themeCombo.getSelectionIndex();
		if(selection != initialSelection) {
			IEclipsePreferences themePreferences = getThemePreferences();
			if(selection == SYSTEM) themePreferences.remove(ThemeColors.THEME_ID_KEY);
			if(selection == LIGHT) themePreferences.put(ThemeColors.THEME_ID_KEY, getLightThemeId());
			if(selection == DARK) themePreferences.put(ThemeColors.THEME_ID_KEY, ThemeColors.DARK_THEME_ID);
			try {
				themePreferences.flush();
			} catch (BackingStoreException e) {
				Activator.logErrorMessageWithCause(e);
			}
			initialSelection = selection;
			// Theme is applied at startup : restart is needed
			restart = MessageDialog.openQuestion(getShell(), DocometreMessages.AppearancePreferences_RestartTitle, DocometreMessages.AppearancePreferences_RestartQuestion);
		}
		int languageSelection = languageCombo.getSelectionIndex();
		if(languageSelection != initialLanguageSelection) {
			String language = languages[languageSelection];
			Languages.setPreferredLanguage(language);
			initialLanguageSelection = languageSelection;
			// Language is applied at startup : relaunch with new language is needed
			if(!restart) restart = MessageDialog.openQuestion(getShell(), DocometreMessages.AppearancePreferences_LanguageRestartTitle, DocometreMessages.AppearancePreferences_LanguageRestartQuestion);
		}
		if(restart) {
			// Relaunch with preferred language (current -nl argument may come from a previous language choice)
			Languages.requestRelaunch(Languages.getPreferredLanguage());
			getShell().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					PlatformUI.getWorkbench().restart();
				}
			});
		}
		return super.performOk();
	}

	private IEclipsePreferences getThemePreferences() {
		return InstanceScope.INSTANCE.getNode(ThemeColors.THEME_PREFERENCES_NODE);
	}

	/*
	 * Light theme is product default theme (cssTheme property in plugin.xml)
	 */
	private String getLightThemeId() {
		IProduct product = Platform.getProduct();
		String cssTheme = product != null ? product.getProperty("cssTheme") : null;
		return cssTheme != null ? cssTheme : "org.eclipse.e4.ui.css.theme.e4_classic";
	}

}
