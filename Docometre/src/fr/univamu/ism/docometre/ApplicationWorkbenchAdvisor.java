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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

	public String getInitialWindowPerspectiveId() {
		return DesignPerspective.ID;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
		boolean showTraditionalTabs = Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS);
		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS, showTraditionalTabs);
		configurer.setSaveAndRestore(true);
	}
	
	@Override
	public void postStartup() {
		PreferenceManager preferenceManager = PlatformUI.getWorkbench().getPreferenceManager( );
		preferenceManager.remove( "org.eclipse.equinox.security.ui.category" ); //$NON-NLS-1$
		preferenceManager.remove( "org.eclipse.equinox.security.ui.storage" ); //$NON-NLS-1$
		preferenceManager.remove( "org.eclipse.help.ui.browsersPreferencePage" ); //$NON-NLS-1$
		
		try {
			if(Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.REDIRECT_STD_ERR_OUT_TO_FILE)) {
				String filePath = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.STD_ERR_OUT_FILE);
				File consoleFile = new File(filePath);
				boolean deleted = true;
				if(consoleFile.exists()) deleted = consoleFile.delete();
				if(!deleted) throw new IOException("Impossible to delete Console file : " + filePath);
				if(consoleFile.createNewFile()) {
					System.out.println("Redirect std and err outputs to file : " + filePath);
					PrintStream pst = new PrintStream(filePath);
					System.setOut(pst);
					System.setErr(pst);
					SimpleDateFormat now = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
					System.out.println("This is std and err log file for DOCoMETRe session : " + now.format(new Date()));
				}
			}	
		
		} catch (IOException e) {
			e.printStackTrace();
		}  
		
		if(!Boolean.getBoolean("DEV")) {
			// We are not in dev mode
			// Get first launch flag
			IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
			boolean firstLaunch = !preferenceStore.contains(GeneralPreferenceConstants.FIRST_LAUNCH) || preferenceStore.getBoolean(GeneralPreferenceConstants.FIRST_LAUNCH);
			if(firstLaunch) {
				System.out.println("This is first launch");
				// Set prefs at first launch
				IEclipsePreferences defaults = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
				System.out.println("Setting prefs for LIBRARIES_ABSOLUTE_PATH, MATLAB_SCRIPT_LOCATION etc.");
				preferenceStore.putValue(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), defaults.get(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), ""));
				preferenceStore.putValue(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(),  defaults.get(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey(), ""));
				preferenceStore.putValue(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION, defaults.get(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION, ""));
				preferenceStore.putValue(GeneralPreferenceConstants.PYTHON_SCRIPTS_LOCATION, defaults.get(GeneralPreferenceConstants.PYTHON_SCRIPTS_LOCATION, ""));
				preferenceStore.putValue(GeneralPreferenceConstants.FIRST_LAUNCH, "false");
				
			}
		}
		
	}
	
	@Override
	public boolean preShutdown() {
		try {
			IProgressMonitor monitor = new NullProgressMonitor();
			ResourcesPlugin.getWorkspace().save(true, monitor);
			final Shell shell = getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow().getShell();
			if(MathEngineFactory.getMathEngine().isStarted()) {
				shell.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						MessageDialog.openInformation(shell, DocometreMessages.CloseMathEngineDialogTitle, DocometreMessages.CloseMathEngineDialogMessage);
					}
				});
				return false;
			} else {
				IPath workspaceLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation();
				IPath lockPath = workspaceLocation.append(".metadata").append(".lock");
				if(lockPath.toFile().exists()) {
					lockPath.toFile().delete();
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return super.preShutdown();
	}
}
