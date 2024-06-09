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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.eclipse.swt.graphics.Rectangle;

import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public final class ChooseWorkspaceData {
	
	private static ChooseWorkspaceData chooseWorkspaceData = null;
	private String propertiesFilePath = System.getProperty("user.dir") + File.separator + "workspaceData.properties"; 
	private boolean showWorkspaceDialog = true;
	private int maxRecentWorkspaces = 5;
	private String[] recentWorkspaces = new String[]{""};
	private String recentWorkspacesString = "";
	private String selectedWorkspace = "";
	private Properties workspaceDataProperties;
	private int workspaceDialogXPosition = 0;
	private int workspaceDialogYPosition = 0;
	private int workspaceDialogWidth = 0;
	private int workspaceDialogHeight = 0;
	
	private ChooseWorkspaceData() {
		workspaceDataProperties = new Properties();
		try(FileInputStream workspaceDataPropertiesFile = new FileInputStream(propertiesFilePath)) {
			workspaceDataProperties.load(workspaceDataPropertiesFile);
			maxRecentWorkspaces = Integer.parseInt((String) workspaceDataProperties.get(GeneralPreferenceConstants.MAX_RECENT_WORKSPACES));
			showWorkspaceDialog = Boolean.parseBoolean((String) workspaceDataProperties.get(GeneralPreferenceConstants.SHOW_WORKSPACE_SELECTION_DIALOG));
			recentWorkspacesString = (String) workspaceDataProperties.get(GeneralPreferenceConstants.RECENT_WORKSPACES);
			recentWorkspaces = recentWorkspacesString.split(",");
			selectedWorkspace =  (String) workspaceDataProperties.get(GeneralPreferenceConstants.SELECTED_WORKSPACE);
			if( workspaceDataProperties.get(GeneralPreferenceConstants.WORKSPACE_DIALOG_X_POSITION) != null) {
				workspaceDialogXPosition = Integer.parseInt((String) workspaceDataProperties.get(GeneralPreferenceConstants.WORKSPACE_DIALOG_X_POSITION));
				workspaceDialogYPosition = Integer.parseInt((String) workspaceDataProperties.get(GeneralPreferenceConstants.WORKSPACE_DIALOG_Y_POSITION));
				workspaceDialogWidth = Integer.parseInt((String) workspaceDataProperties.get(GeneralPreferenceConstants.WORKSPACE_DIALOG_WIDTH));
				workspaceDialogHeight = Integer.parseInt((String) workspaceDataProperties.get(GeneralPreferenceConstants.WORKSPACE_DIALOG_HEIGHT));
			}
		} catch (FileNotFoundException e) {
			save();
		} catch (Exception e) {
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	public static ChooseWorkspaceData getInstance() {
		if(chooseWorkspaceData == null) chooseWorkspaceData = new ChooseWorkspaceData();
		return chooseWorkspaceData;
	}
	
	public int getMaxRecentWorkspaces() {
		return maxRecentWorkspaces;
	}
	
	public void setMaxRecentWorkspace(int maxRecentWorkspaces) {
		this.maxRecentWorkspaces = maxRecentWorkspaces;
	}

	public String getSelection() {
		return this.selectedWorkspace;
	}

	public void setSelection(String selectedWorkspace) {
		if(selectedWorkspace == null || selectedWorkspace.equals("")) return;
		this.selectedWorkspace = selectedWorkspace;
		if(!recentWorkspacesString.contains(selectedWorkspace)) {
			recentWorkspacesString = selectedWorkspace + ',' + recentWorkspacesString;
			recentWorkspacesString = recentWorkspacesString.replaceAll(",$", "");
			recentWorkspaces = recentWorkspacesString.split(",");
			if(recentWorkspaces.length > maxRecentWorkspaces) {
				recentWorkspaces = Arrays.copyOfRange(recentWorkspaces, 0, maxRecentWorkspaces - 1);
			}
		}
	}
	
	public String[] getRecentWorkspaces() {
		return this.recentWorkspaces;
	}
	
	public boolean getShowDialog() {
		return showWorkspaceDialog;
	}

	public void setShowDialog(boolean showWorkspaceDialog) {
		this.showWorkspaceDialog = showWorkspaceDialog;
	}

	public void save() {
		workspaceDataProperties.put(GeneralPreferenceConstants.MAX_RECENT_WORKSPACES, String.valueOf(maxRecentWorkspaces));
		workspaceDataProperties.put(GeneralPreferenceConstants.RECENT_WORKSPACES, encodeWorkspacesString(recentWorkspaces));
		workspaceDataProperties.put(GeneralPreferenceConstants.SELECTED_WORKSPACE, selectedWorkspace);
		workspaceDataProperties.put(GeneralPreferenceConstants.SHOW_WORKSPACE_SELECTION_DIALOG, String.valueOf(getShowDialog()));
		workspaceDataProperties.put(GeneralPreferenceConstants.WORKSPACE_DIALOG_X_POSITION, String.valueOf(workspaceDialogXPosition));
		workspaceDataProperties.put(GeneralPreferenceConstants.WORKSPACE_DIALOG_Y_POSITION, String.valueOf(workspaceDialogYPosition));
		workspaceDataProperties.put(GeneralPreferenceConstants.WORKSPACE_DIALOG_WIDTH, String.valueOf(workspaceDialogWidth));
		workspaceDataProperties.put(GeneralPreferenceConstants.WORKSPACE_DIALOG_HEIGHT, String.valueOf(workspaceDialogHeight));
		try {
			FileOutputStream workspaceDataPropertiesFile = new FileOutputStream(propertiesFilePath);
			workspaceDataProperties.store(workspaceDataPropertiesFile, "Workspaces data properties for DOCoMETRe");
		} catch (IOException e) {
			Activator.logErrorMessageWithCause(e);
		}
		
	}
	
	private String encodeWorkspacesString(String[] recentWorkspaces) {
		recentWorkspacesString = "";
		for (int i = 0; i < recentWorkspaces.length; i++) {
			recentWorkspacesString = recentWorkspacesString + recentWorkspaces[i] + ",";
		}
		return recentWorkspacesString.replaceAll(",$", "");
	}

	public void setWorkspaceDialogPosition(int x, int y, int width, int height) {
		this.workspaceDialogXPosition = x;
		this.workspaceDialogYPosition = y;
		this.workspaceDialogWidth = width;
		this.workspaceDialogHeight = height;
		
	}
	
	public Rectangle getWorkspaceDialogPosition() {
		return new Rectangle(workspaceDialogXPosition, workspaceDialogYPosition, workspaceDialogWidth, workspaceDialogHeight);
	}

}
