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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [12/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Platform;

/*
 * Display backend of GTK on Linux.
 * In a Wayland session (Ubuntu 22.04 and later, Fedora, WSL2 with WSLg...), GTK uses its Wayland
 * backend by default. But JOGL, used by OpenGL charts (real time charts, XYZ charts), only supports
 * X11 with SWT : the JVM crashes (SIGSEGV in XDisplayString) as soon as such a chart is created.
 * The GTK backend is chosen when GTK is initialized, by the launcher (splash screen) before the JVM
 * starts, and the environment can't be given to the launcher relaunch (EXIT_RELAUNCH) : the JVM is
 * a child process of the launcher. So, in a Wayland session, DOCoMETRe starts a new launcher with
 * GDK_BACKEND=x11 in its environment and exits : GTK then uses Xwayland, the X11 server of the
 * Wayland session. The new launcher waits for the end of this process (configuration area).
 */
public final class LinuxDisplayBackend {
	
	private static final String GDK_BACKEND = "GDK_BACKEND";
	private static final String X11 = "x11";
	private static final String WAYLAND_DISPLAY = "WAYLAND_DISPLAY";
	private static final String DISPLAY = "DISPLAY";
	
	// Properties set by the launcher
	private static final String PROP_LAUNCHER = "eclipse.launcher";
	private static final String PROP_COMMANDS = "eclipse.commands";
	private static final String NEW_LINE = "\n";
	private static final String CMD_VMARGS = "-vmargs";
	private static final String CMD_SHOWSPLASH = "-showsplash";
	// Arguments added by the launcher (with a value, or without) : the new launcher adds them again
	private static final Set<String> LAUNCHER_ARGUMENTS_WITH_VALUE = Set.of("-os", "-ws", "-arch", "-launcher", "-name", 
			"--launcher.library", "-startup", "-exitdata", "-vm");
	private static final Set<String> LAUNCHER_ARGUMENTS = Set.of("--launcher.overrideVmargs", "--launcher.appendVmargs");
	
	private LinuxDisplayBackend() {
	}
	
	/*
	 * In a Wayland session on Linux, starts a new DOCoMETRe launcher with GDK_BACKEND=x11 : returns true, 
	 * the application must then exit. Returns false on other systems, in an X11 session, when 
	 * GDK_BACKEND=x11 is already set (relaunched process) or when the relaunch is not possible.
	 */
	public static boolean relaunchWithX11Backend() {
		if(!Platform.OS_LINUX.equals(Platform.getOS())) return false;
		if(isEmpty(System.getenv(WAYLAND_DISPLAY))) return false;
		String gdkBackend = System.getenv(GDK_BACKEND);
		if(gdkBackend != null && gdkBackend.startsWith(X11)) return false;
		String message = "Wayland session : OpenGL charts need the X11 backend of GTK.";
		// Without X11 server (Xwayland) in the session, GTK couldn't open any window
		if(isEmpty(System.getenv(DISPLAY))) {
			Activator.logInfoMessage(message + " No X11 server (DISPLAY) in this session : OpenGL charts are not available.", LinuxDisplayBackend.class);
			return false;
		}
		String launcher = System.getProperty(PROP_LAUNCHER);
		String commands = System.getProperty(PROP_COMMANDS);
		if(launcher == null || commands == null) {
			Activator.logInfoMessage(message + " Launch DOCoMETRe with GDK_BACKEND=x11 environment variable.", LinuxDisplayBackend.class);
			return false;
		}
		// The new launcher is started when this process has exited
		List<String> command = new ArrayList<>();
		command.add("/bin/sh");
		command.add("-c");
		command.add("while kill -0 " + ProcessHandle.current().pid() + " 2>/dev/null; do sleep 0.2; done; exec \"$0\" \"$@\"");
		command.add(launcher);
		command.addAll(getUserArguments(commands));
		ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();
		processBuilder.environment().put(GDK_BACKEND, X11);
		try {
			processBuilder.start();
		} catch (Exception e) {
			Activator.logInfoMessage(message + " Unable to relaunch DOCoMETRe (" + e.getMessage() + ") : launch it with GDK_BACKEND=x11 environment variable.", LinuxDisplayBackend.class);
			return false;
		}
		Activator.logInfoMessage(message + " Relaunch DOCoMETRe with GDK_BACKEND=x11", LinuxDisplayBackend.class);
		return true;
	}
	
	/*
	 * Arguments given to the launcher by the user (e.g. -nl en), without the arguments added by 
	 * the launcher itself (from DOCoMETRe.ini) and the VM arguments
	 */
	static List<String> getUserArguments(String commands) {
		List<String> arguments = new ArrayList<>();
		String[] currentArguments = commands.split(NEW_LINE);
		for (int i = 0; i < currentArguments.length; i++) {
			String argument = currentArguments[i];
			if(argument.isEmpty()) continue;
			if(CMD_VMARGS.equals(argument)) break;
			if(LAUNCHER_ARGUMENTS_WITH_VALUE.contains(argument)) {
				i++;
				continue;
			}
			if(LAUNCHER_ARGUMENTS.contains(argument)) continue;
			if(CMD_SHOWSPLASH.equals(argument)) {
				// Optional splash path
				if(i + 1 < currentArguments.length && !currentArguments[i + 1].startsWith("-")) i++;
				continue;
			}
			arguments.add(argument);
		}
		return arguments;
	}
	
	private static boolean isEmpty(String value) {
		return value == null || value.isEmpty();
	}

}
