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
package fr.univamu.ism.docometre;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.equinox.app.IApplicationContext;

/*
 * User interface language : system language or a language chosen in appearance preferences.
 * Language is set by the framework at startup (-nl argument) : Eclipse and DOCoMETRe messages, 
 * as well as plugin.xml translations, are loaded with this language. So, when chosen language 
 * is not current one, DOCoMETRe is relaunched with the right -nl argument.
 * Chosen language is saved with workspaces data as it must be known before workspace selection.
 */
public final class Languages {
	
	public static final String SYSTEM = "SYSTEM";
	
	// Language of default messages files (messages.properties)
	private static final String DEFAULT_LANGUAGE = "en";
	// Translated messages files : messages_xx.properties or messages_xx_YY.properties
	private static final Pattern MESSAGES_FILE_PATTERN = Pattern.compile("messages_([a-z]{2,3})(_\\w+)?\\.properties");
	
	// Properties used by launcher to relaunch DOCoMETRe (see org.eclipse.ui.internal.Workbench.setRestartArguments)
	private static final String PROP_VM = "eclipse.vm";
	private static final String PROP_VMARGS = "eclipse.vmargs";
	private static final String PROP_COMMANDS = "eclipse.commands";
	private static final String CMD_VMARGS = "-vmargs";
	private static final String CMD_NL = "-nl";
	private static final String NEW_LINE = "\n";
	
	private static boolean relaunchRequested;
	
	/*
	 * Available languages are default messages language and languages of all messages_xx.properties files
	 */
	public static String[] getAvailableLanguages() {
		TreeSet<String> languages = new TreeSet<>();
		languages.add(DEFAULT_LANGUAGE);
		Enumeration<URL> entries = Activator.getDefault().getBundle().findEntries("/", "messages_*.properties", true);
		while (entries != null && entries.hasMoreElements()) {
			String path = entries.nextElement().getPath();
			Matcher matcher = MESSAGES_FILE_PATTERN.matcher(path.substring(path.lastIndexOf('/') + 1));
			if(matcher.matches()) languages.add(matcher.group(1));
		}
		return languages.toArray(new String[languages.size()]);
	}
	
	/*
	 * Language name in this language (e.g. English, Français)
	 */
	public static String getLanguageName(String language) {
		Locale locale = Locale.of(language);
		String name = locale.getDisplayLanguage(locale);
		if(name.isEmpty()) return language;
		return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
	}
	
	/*
	 * Language chosen by user : SYSTEM or language code
	 */
	public static String getPreferredLanguage() {
		return ChooseWorkspaceData.getInstance().getLanguage();
	}
	
	public static void setPreferredLanguage(String language) {
		ChooseWorkspaceData.getInstance().setLanguage(language);
		ChooseWorkspaceData.getInstance().save();
	}
	
	/*
	 * At startup, if a language is chosen and is not current one, returns command line to relaunch 
	 * DOCoMETRe with this language. Returns null if no relaunch is needed or possible.
	 */
	public static String getStartupRelaunchCommandLine() {
		String language = getPreferredLanguage();
		if(SYSTEM.equals(language)) return null;
		if(language.equals(Locale.getDefault().getLanguage())) return null;
		// Already launched with this language : don't relaunch again
		if(language.equals(getLanguageArgument())) return null;
		String commandLine = getRelaunchCommandLine(language);
		if(commandLine == null) Activator.logInfoMessage("Unable to relaunch DOCoMETRe with language \"" + language + "\" : use -nl " + language + " program argument", Languages.class);
		else Activator.logInfoMessage("Relaunch DOCoMETRe with language \"" + language + "\"", Languages.class);
		return commandLine;
	}
	
	/*
	 * Prepare relaunch with new language when workbench restarts. 
	 * Returns false if relaunch is not possible (e.g. when launched from Eclipse IDE) : 
	 * workbench then simply restarts.
	 */
	public static boolean requestRelaunch(String language) {
		String commandLine = getRelaunchCommandLine(language);
		if(commandLine == null) return false;
		System.setProperty(IApplicationContext.EXIT_DATA_PROPERTY, commandLine);
		relaunchRequested = true;
		return true;
	}
	
	public static boolean isRelaunchRequested() {
		return relaunchRequested;
	}
	
	/*
	 * Value of -nl program argument, null if none
	 */
	private static String getLanguageArgument() {
		String commands = System.getProperty(PROP_COMMANDS);
		if(commands == null) return null;
		String[] arguments = commands.split(NEW_LINE);
		for (int i = 0; i < arguments.length - 1; i++) {
			if(CMD_NL.equals(arguments[i])) return Locale.forLanguageTag(arguments[i + 1].replace('_', '-')).getLanguage();
		}
		return null;
	}
	
	/*
	 * Current command line where -nl argument is replaced by new language (removed if SYSTEM).
	 * Returns null if launcher information is not available.
	 */
	private static String getRelaunchCommandLine(String language) {
		String vm = System.getProperty(PROP_VM);
		String commands = System.getProperty(PROP_COMMANDS);
		if(vm == null || commands == null) return null;
		
		StringBuilder commandLine = new StringBuilder(512);
		commandLine.append(vm);
		commandLine.append(NEW_LINE);
		
		// append the vmargs and commands
		String vmargs = System.getProperty(PROP_VMARGS);
		if(vmargs != null && !vmargs.isEmpty() && !vmargs.endsWith(NEW_LINE)) vmargs = vmargs + NEW_LINE;
		if(vmargs != null) commandLine.append(vmargs);
		
		List<String> arguments = new ArrayList<>();
		if(!SYSTEM.equals(language)) {
			arguments.add(CMD_NL);
			arguments.add(language);
		}
		String[] currentArguments = commands.split(NEW_LINE);
		for (int i = 0; i < currentArguments.length; i++) {
			if(CMD_NL.equals(currentArguments[i])) {
				// Skip current -nl argument and its value
				i++;
				continue;
			}
			arguments.add(currentArguments[i]);
		}
		for (String argument : arguments) {
			commandLine.append(argument);
			commandLine.append(NEW_LINE);
		}
		
		// put the vmargs back at the very end (the eclipse.commands property already contains the -vm arg)
		if(vmargs != null) {
			commandLine.append(CMD_VMARGS);
			commandLine.append(NEW_LINE);
			commandLine.append(vmargs);
		}
		
		return commandLine.toString();
	}

}
