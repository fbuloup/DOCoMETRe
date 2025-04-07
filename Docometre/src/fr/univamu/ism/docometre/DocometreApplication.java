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
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * This class controls all aspects of the application's execution
 */
public class DocometreApplication implements IApplication {
	
	public static final Integer EXIT_ERROR = Integer.valueOf(-1);
	
	public static String COURIER_NEW = "COURIER_NEW";
	public static String COURIER_NEW_BOLD = "COURIER_NEW_BOLD";
	
	public static String WHITE = "WHITE";
	public static String BLACK = "BLACK";
	public static String RED = "RED";
	public static String GREEN = "GREEN";
	public static String BLUE = "BLUE";
	public static String ORANGE = "ORANGE";
	public static String MAROON = "MAROON";
	
	public static String WORKSPACE_PATH;
	
	private static RGB[] rgbs = new RGB[] { 
			new RGB(0xFF, 0x00, 0x00),
			new RGB(0x00, 0xFF, 0x00),
			new RGB(0x00, 0x00, 0xFF),
			new RGB(0xFF, 0xFF, 0x00),
			new RGB(0x00, 0xFF, 0xFF),
			new RGB(0xFF, 0x00, 0xFF),
			new RGB(0xFF, 0x80, 0x00),
			new RGB(0x80, 0x80, 0x80),
			new RGB(0xA0, 0x60, 0x20),
			new RGB(0x60, 0xA0, 0x20),
			new RGB(0x20, 0x60, 0xA0),
			new RGB(0x6D, 0x1D, 0x73),
			new RGB(0xFF, 0xAA, 0x00),
			new RGB(0x99, 0x91, 0x26),
			new RGB(0xFF, 0x80, 0x91),
			new RGB(0x36, 0xB8, 0xD9)
		};
	private static int maxColors = rgbs.length;;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.
	 * IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		Display display = PlatformUI.createDisplay();
		
		JFaceResources.getFontRegistry().put(COURIER_NEW, new FontData[]{ new FontData("Courier New", 12, SWT.NORMAL)});
		JFaceResources.getFontRegistry().put(COURIER_NEW_BOLD, new FontData[]{ new FontData("Courier New", 12, SWT.BOLD)});
		
		JFaceResources.getColorRegistry().put(WHITE, new RGB(255, 255, 255));
		JFaceResources.getColorRegistry().put(BLACK, new RGB(0, 0, 0));
		JFaceResources.getColorRegistry().put(RED, new RGB(202, 0, 42));
		JFaceResources.getColorRegistry().put(GREEN, new RGB(61, 119, 91));
		JFaceResources.getColorRegistry().put(BLUE, new RGB(0, 0, 255));
		JFaceResources.getColorRegistry().put(ORANGE, new RGB(255, 102, 0));
		JFaceResources.getColorRegistry().put(MAROON, new RGB(122, 0, 0));
		
		for (int i = 0; i < rgbs.length; i++) {
			JFaceResources.getColorRegistry().put(String.valueOf(i), rgbs[i]);
		}
		
		boolean workspaceOK = false;
		String workspace = "";
		while(!workspaceOK) {
			workspaceOK = true;
			ChooseWorkspaceData chooseWorkspaceData = ChooseWorkspaceData.getInstance();
			workspace = chooseWorkspaceData.getSelection();
			if(workspace == null || workspace.equals("")) chooseWorkspaceData.setShowDialog(true);
			if(chooseWorkspaceData.getShowDialog()) {
				ChooseWorkspaceDialog chooseWorkspaceDialog = new ChooseWorkspaceDialog(chooseWorkspaceData);
				int response = chooseWorkspaceDialog.open();
				chooseWorkspaceData.save();
				if(response == Window.CANCEL) return IApplication.EXIT_OK;
				workspace = chooseWorkspaceData.getSelection();
			} 
			
			if(workspace == null || workspace.equals("")) {
				MessageDialog.openError(display.getActiveShell(), DocometreMessages.Error, DocometreMessages.WorkspaceNotSpecified);
				Activator.logInfoMessage(DocometreMessages.WorkspaceNotSpecified, DocometreApplication.class);
				workspaceOK = false;
			}
			
			File workspaceLocker = new File(workspace + File.separator + ".metadata" + File.separator +"workspace.locker");
			if(workspaceLocker.exists()) {
				String message = NLS.bind(DocometreMessages.WorkspaceAlreadyUsed, workspace);
				MessageDialog.openError(display.getActiveShell(), DocometreMessages.Error, message);
				Activator.logErrorMessage(message);
				workspaceOK = false;
			}
		}
		
		File metadataFolder = new File(workspace + File.separator + ".metadata" + File.separator);
		if(!metadataFolder.exists()) {
			if(!metadataFolder.mkdirs()) {
				MessageDialog.openError(display.getActiveShell(), DocometreMessages.Error,  DocometreMessages.UnableCreateMetadataFolder);
				return EXIT_ERROR;
			}
		}
		
		File workspaceLocker = new File(workspace + File.separator + ".metadata" + File.separator +"workspace.locker");
		if(!workspaceLocker.createNewFile()) MessageDialog.openError(display.getActiveShell(), DocometreMessages.Error,  DocometreMessages.UnableCreateLockerFile);
		
		Location instanceLocation = Platform.getInstanceLocation();
		instanceLocation.release();
		File workspaceFile = new File(workspace);
		URL workspaceURL = workspaceFile.toURI().toURL();
		instanceLocation.set(workspaceURL, true);
		WORKSPACE_PATH = workspace;
		try {
			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
			Activator.logInfoMessage("createAndRunWorkbench return code : " + returnCode, getClass());
			if (returnCode == PlatformUI.RETURN_RESTART)
				return IApplication.EXIT_RESTART;
			else
				return IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
//		Location instanceLocation = Platform.getInstanceLocation();
//		instanceLocation.release();
		if (!PlatformUI.isWorkbenchRunning())
			return;
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				if (!display.isDisposed())
					workbench.close();
			}
		});
	}
	
	public static Color getColor(Byte index) {
		if(index < 0) index = 0;
		index = (byte) (index % maxColors);
		Color color = JFaceResources.getColorRegistry().get(index.toString());
		return color;
	}
	
	public static Color getColor(String colorName) {
		return JFaceResources.getColorRegistry().get(colorName);
	}
	
	public static Color getColor(RGB rgb) {
		Color color = JFaceResources.getColorRegistry().get(rgb.toString());
		if(color == null) {
			JFaceResources.getColorRegistry().put(rgb.toString(), rgb);
			color = JFaceResources.getColorRegistry().get(rgb.toString());
		}
		return color;
	}

	public static Font getFont(String fontName) {
		return JFaceResources.getFontRegistry().get(fontName);
	}
	
}
