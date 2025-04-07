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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import fr.univamu.ism.docometre.consoles.DocometreConsole;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
        configurer.setTitle("DOCoMETRe " + Activator.getDefault().getVersion() + " - " + ChooseWorkspaceData.getInstance().getSelection()); //$NON-NLS-1$
    }

    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }
    
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(400, 300));
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setShowProgressIndicator(true);
        configurer.setShowPerspectiveBar(true);
        DocometreConsole.createInstance();
		Activator.logInfoMessage(DocometreMessages.CurrentWorkspace + DocometreApplication.WORKSPACE_PATH, DocometreApplication.class);
        Activator.logInfoMessage("Locale : " + Locale.getDefault(),  getClass());
		Activator.logInfoMessage(DocometreMessages.RuntimeFolder + System.getProperty("user.dir"), getClass());
        try {
			if(Activator.getDefault().getPreferenceStore().getBoolean(GeneralPreferenceConstants.REDIRECT_STD_ERR_OUT_TO_FILE)) {
				String filePath = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.STD_ERR_OUT_FILE);
				File consoleFile = new File(filePath);
				FileOutputStream consoleFileOutputStream = new FileOutputStream(consoleFile, true);
				PrintStream pst = new PrintStream(consoleFileOutputStream, true);
				System.setOut(pst);
				System.setErr(pst);
				SimpleDateFormat now = new SimpleDateFormat("HH:mm:ss.SSS YYYY-MM-dd", Locale.getDefault());
				// Let these System.outs be !
				System.out.println("\n****************************************************");
				System.out.println("This is std and err log file for DOCoMETRe session : " + now.format(new Date()));
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}  
    }

	@Override
    public void postWindowOpen() {
		int historyLimit = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.PREF_UNDO_LIMIT);
		PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().setLimit(IOperationHistory.GLOBAL_UNDO_CONTEXT, historyLimit);
		UndoRedoUserApprover promptingUserApprover = new UndoRedoUserApprover(IOperationHistory.GLOBAL_UNDO_CONTEXT);
		PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().addOperationApprover(promptingUserApprover);
    }
	
}
