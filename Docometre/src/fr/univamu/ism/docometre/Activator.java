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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import fr.univamu.ism.docometre.analyse.editors.DataProcessEditor;
import fr.univamu.ism.docometre.consoles.DocometreConsole;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.processeditor.ADWinProcessEditor;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.processeditor.ArduinoUnoProcessEditor;
import fr.univamu.ism.docometre.editors.DataEditor;
import fr.univamu.ism.docometre.editors.PartNameRefresher;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.views.CachedLogger;
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
	public static final String daqFileExtension = ".daqconf";
	public static final String processFileExtension = ".process";
	public static final String logFileExtension = ".log";
	public static final String samplesFileExtension = ".samples";
	public static final String parametersFileExtension = ".params";
	public static final String adwFileExtension = ".adw";
	public static final String dataProcessingFileExtension = ".dataprocess";
	public static final String batchDataProcessingFileExtension = ".batchdataprocess";
	public static final String xyChartFileExtension = ".chart";
	public static final String xyzChartFileExtension = ".chart3D";
	public static final String customerFunctionFileExtension = ".FUN";
	
	// System types
	public static final String ADWIN_SYSTEM = "ADWin";
	//public static String NI_600X_SYSTEM = "NI 600X";
	public static final String ARDUINO_UNO_SYSTEM = "Arduino UNO";
	public static final String[] SYSTEMS = new String[]{ADWIN_SYSTEM, ARDUINO_UNO_SYSTEM/*, NI_600X_SYSTEM*/};
	
	private static MessageConsole messageConsole; 
	
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
		Activator.getDefault().getLog().addLogListener(CachedLogger.getInstance());
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
		return getImageDescriptor(PLUGIN_ID, path);
	}
	
	/*
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 */
	public static ImageDescriptor getImageDescriptor(String pluginID, String path) {
		Bundle bundle = Platform.getBundle(pluginID);
        URL url = FileLocator.find(bundle, new Path(path), null);
        ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);
		return imageDescriptor;
	}
	
	/*
	 * Return the image for the image file at the given
	 * plug-in relative path
	 */
	public static Image getImage(String pluginID, String path) {
		Image image = plugin.getImageRegistry().get(path);
		if(image == null || image.isDisposed()) {
			plugin.getImageRegistry().remove(path);
            ImageDescriptor imageDescriptor = getImageDescriptor(pluginID, path);
			plugin.getImageRegistry().put(path, imageDescriptor);
			image = plugin.getImageRegistry().get(path);
		}
		return image;
	}
	
	/*
	 * Return the shared image for the image file at the given
	 * plug-in relative path
	 */
	public static Image getSharedImage(String path) {
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		return sharedImages.getImage(path);
	}
	
	/*
	 * Return the shared image descriptor for the image file at the given
	 * plug-in relative path
	 */
	public static ImageDescriptor getSharedImageDescriptor(String path) {
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		return sharedImages.getImageDescriptor(path);
	}
	
	/*
	 * Return the image for the image file at the given
	 * plug-in relative path
	 */
	public static Image getImage(String path) {
		return getImage(PLUGIN_ID, path);
	}
	
	public static Font getBoldFont(String  symbolicName) {
		return JFaceResources.getFontRegistry().getBold(symbolicName);
	}
	
	public static Font getItalicFont(String  symbolicName) {
		return JFaceResources.getFontRegistry().getItalic(symbolicName);
	}

	/*
	 * Log messages (info, error or warning)
	 */
	public static void logErrorMessage(String message) {
		IStatus status = new Status(IStatus.ERROR, "Error", message);
		getDefault().getLog().log(status);
	}
	
	/*
	 * Log messages (info, error or warning)
	 */
	public static void logErrorMessageWithCause(Exception exception) {
		logErrorMessage(getLogErrorMessageWithCause(exception));
	}
	
	private static void getMultiStatusMessages(StringBuffer message, IStatus status) {
		message.append(status.getMessage() + "\n");
		if(status.getException() != null) {
			//message.append(status.getException().getMessage());
			if(status.getException() instanceof CoreException) {
				getMultiStatusMessages(message, ((CoreException) status.getException()).getStatus());
			} else {
				message.append(status.getException().getMessage());
				if(status.getException().getCause() !=  null ) getMultiCauseMessages(message, status.getException().getCause());
			}
		}
		
		if(status.isMultiStatus()) {
			IStatus[] childrenStatuses = status.getChildren();
			for (IStatus childrenStatus : childrenStatuses) {
				getMultiStatusMessages(message, childrenStatus);
			}
		}
	}
	
	private static void getMultiCauseMessages(StringBuffer message, Throwable throwable) {
		if(throwable.toString() != null && !"".equals(throwable.toString())) message.append(throwable.toString() + "\n");
		if(throwable.getCause() != null) getMultiCauseMessages(message, throwable.getCause());
	}
	
	public static String getLogErrorMessageWithCause(Exception exception) {
		StringBuffer message = new StringBuffer();
		if(exception instanceof CoreException) {
			getMultiStatusMessages(message, ((CoreException) exception).getStatus());
		} else {
			if(exception.getMessage() != null) message.append(exception.toString() + "\n");
			if(exception.getCause() !=  null ) getMultiCauseMessages(message, exception.getCause());
		}
		
		StackTraceElement[] stackElements = exception.getStackTrace();
		if(stackElements != null) message.append("Stack trace :\n");
		for (StackTraceElement stackTraceElement : stackElements) {
			message.append(stackTraceElement.toString() + "\n");
		}
		
		return message.toString();
	}
	
	/*
	 * Log messages (info, error or warning)
	 */
	public static void logWarningMessage(String message) {
		IStatus status = new Status(IStatus.WARNING, "Warning", message);
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
				if(!(editorReferences[i].getEditorInput() instanceof ResourceEditorInput)) {
					if(editorReferences[i].getEditor(false) != null) editorReferences[i].getEditor(false).getSite().getPage().closeEditor(editorReferences[i].getEditor(false), false);
					continue;
				}
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
	
	
	public static IWorkbenchPart getEditor(Object object, String editorID) {
		IWorkbenchPart foundWorkbenchPart = null;
		try {
			IEditorReference[] editors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findEditors(null, editorID, IWorkbenchPage.MATCH_ID);
			for (IEditorReference editorReference : editors) {
				ResourceEditorInput editorInput = (ResourceEditorInput)editorReference.getEditorInput();
				if(editorInput.isEditing(object)) {
					foundWorkbenchPart = editorReference.getPart(false);
					break;
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		return foundWorkbenchPart;
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
		if(activeEditor instanceof DataProcessEditor) {
			((DataProcessEditor)activeEditor).activateSegmentProcessEditor();
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
	
	public static void beep(Exception e) {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
		logErrorMessageWithCause(e);
		e.printStackTrace();
	}
	
	public static MessageConsole getMessageConsole() {
		createMessageConsole();
		return messageConsole;
	}
	
	public static void createMessageConsole() {
		if (messageConsole == null)  {
			messageConsole = new DocometreConsole("Messages", IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_16.name(), true);
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { messageConsole });
		}
	}
	

}
