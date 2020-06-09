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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import fr.univamu.ism.docometre.dacqsystems.adwin.ui.processeditor.ADWinProcessEditor;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.processeditor.ArduinoUnoProcessEditor;
import fr.univamu.ism.docometre.editors.DataEditor;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.process.ScriptSegment;

/*
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	
	// The plug-in ID
	public static final String PLUGIN_ID = "Docometre"; //$NON-NLS-1$
	
	public static final String NEW_LINE = System.getProperty("line.separator");

	// The shared instance
	private static Activator plugin;
	
	// Files extensions definitions
	public static String daqFileExtension = ".daqconf";
	public static String processFileExtension = ".process";
	public static String logFileExtension = ".log";
	public static String samplesFileExtension = ".samples";
	public static String parametersFileExtension = ".params";
	public static String adwFileExtension = ".adw";
	
	// System types
	public static String ADWIN_SYSTEM = "ADWin";
	//public static String NI_600X_SYSTEM = "NI 600X";
	public static String ARDUINO_UNO_SYSTEM = "Arduino UNO";
	public static String[] SYSTEMS = new String[]{ADWIN_SYSTEM, ARDUINO_UNO_SYSTEM/*, NI_600X_SYSTEM*/};
	
	/*
	 * The constructor
	 */
	public Activator() {
	}
	
	public String getVersion() {
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		Version version = bundle.getVersion();
		return version.getQualifier();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;			
		Logger.getRootLogger().getLoggerRepository().resetConfiguration();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/*
	 * Returns the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/*
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		Optional<ImageDescriptor> option = ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, path);
		if(option.isPresent()) return option.get(); 
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		return sharedImages.getImageDescriptor(path);
	}
	
	/*
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 */
	public static ImageDescriptor getImageDescriptor(String pluginID, String path) {
		Optional<ImageDescriptor> option = ResourceLocator.imageDescriptorFromBundle(pluginID, path);
		if(option.isPresent()) return option.get(); 
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		return sharedImages.getImageDescriptor(path);
	}
	
	/*
	 * Return the image for the image file at the given
	 * plug-in relative path
	 */
	public static Image getImage(String path) {
		return getImageDescriptor(path).createImage();
	}

	/*
	 * Log messages (info, error or warning)
	 */
	public static void logErrorMessage(String message) {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message);
		getDefault().getLog().log(status);
	}
	
	/*
	 * Log messages (info, error or warning)
	 */
	public static void logErrorMessageWithCause(Exception exception) {
		logErrorMessage(getLogErrorMessageWithCause(exception));
	}
	
	public static String getLogErrorMessageWithCause(Exception exception) {
		String message = "" + exception + "\n";
		if(exception.getMessage() != null) message = message + exception.getMessage() + "\n";
		StackTraceElement[] stackElements = exception.getStackTrace();
		for (StackTraceElement stackTraceElement : stackElements) {
			message = message + stackTraceElement.toString() + "\n";
		}
		if(exception.getCause() != null && exception.getCause().getMessage() != null) message = message + exception.getCause().getMessage();
		return message;
	}
	
	/*
	 * Log messages (info, error or warning)
	 */
	public static void logWarningMessage(String message) {
		IStatus status = new Status(IStatus.WARNING, PLUGIN_ID, message);
		getDefault().getLog().log(status);
//		if(PlatformUI.isWorkbenchRunning())
//		MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "WARNING", message);
	}
	
	/*
	 * Log messages (info, error or warning)
	 */
	public static void logInfoMessage(String message, @SuppressWarnings("rawtypes") Class ID) {
		SimpleDateFormat now = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
		IStatus status = new Status(IStatus.INFO, ID.getSimpleName() + " - " + now.format(new Date()), message);
		getDefault().getLog().log(status);
	}
	
	//////////////////////////////////////////////////////
	// Editors functions
	/*
	 * Close editor associated with resource
	 */
	public static void closeEditor(Object object) {
		IEditorReference[] editorReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		for (int i = 0; i < editorReferences.length; i++) {
			try {
				ResourceEditorInput resourceEditorInput = ((ResourceEditorInput)editorReferences[i].getEditorInput());
				if(resourceEditorInput.isEditing(object)) {
					if(resourceEditorInput.removeEditedObject(object)) {
						if(editorReferences[i].getEditor(false) instanceof DataEditor) ((DataEditor)editorReferences[i].getEditor(false)).removeTrace(object);
					}
					if(resourceEditorInput.canCloseEditor()) editorReferences[i].getEditor(false).getSite().getPage().closeEditor(editorReferences[i].getEditor(false), false);
					else if(editorReferences[i].getEditor(false) instanceof PartNameRefresher) ((PartNameRefresher)editorReferences[i].getEditor(false)).refreshPartName();
				}
			} catch (PartInitException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			}
		}
	}
	
	
	/*
	 * 
	 */
	public static void activateScriptSegmentEditor(ScriptSegment scriptSegment) {
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if(activeEditor instanceof ADWinProcessEditor) {
			((ADWinProcessEditor)activeEditor).activateSegmentProcessEditor(scriptSegment);
		}
		if(activeEditor instanceof ArduinoUnoProcessEditor) {
			((ArduinoUnoProcessEditor)activeEditor).activateSegmentProcessEditor(scriptSegment);
		}
	}
	
	public static double max(double[] values) {
		double max = Double.NEGATIVE_INFINITY;
		for (double value : values) {
			max = Math.max(max, value);
		}
		return max;
	}
	
	public static double min(double[] values) {
		double min = Double.POSITIVE_INFINITY;
		for (double value : values) {
			min = Math.min(min, value);
		}
		return min;
	}
	

}
