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
package fr.univamu.ism.docometre.dacqsystems.functions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.processeditor.ArduinoUnoInitializeSegmentEditor;
import fr.univamu.ism.docometre.editors.AbstractScriptSegmentEditor;
import fr.univamu.ism.docometre.editors.ResourceEditorInput;
import fr.univamu.ism.docometre.scripteditor.actions.AssignFunctionAction;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.docometre.scripteditor.editparts.BlockEditPart;

public class ArduinoUnoFunctionsMenuFactory {
	
	private static String SEPARATOR = "SEPARATOR";
	private static String CUSTOMER_FUNCTIONS_MENU = "CUSTOMER_FUNCTIONS_MENU";
	
	public static String[] ArduinoUnoFunctionsFiles = new String[] {AnalogInputFunction.functionFileName, 
																	AnalogWaitFunction.functionFileName,
																	DigitalWaitFunction.functionFileName,
		    														SEPARATOR,
																	DigitalOutputFunction.functionFileName,
																	DigitalInputFunction.functionFileName,
																	SEPARATOR,
																    ExpressionFunction.functionFileName,
																    SEPARATOR,
																    TerminateProcessFunction.functionFileName};
	
	public static String[] ArduinoUnoFunctionsClasses = new String[] {AnalogInputFunction.class.getName(),
																	  AnalogWaitFunction.class.getName(),
																	  DigitalWaitFunction.class.getName(),
																	  null,
																	  DigitalOutputFunction.class.getName(),
																	  DigitalInputFunction.class.getName(),
																	  null,
																	  ExpressionFunction.class.getName(),
																	  null,
																	  TerminateProcessFunction.class.getName()};
	
	public static void populateMenu(AbstractScriptSegmentEditor scriptSegmentEditor, BlockEditPart blockEditPart,
			IMenuManager functionMenuManager) {
		// Retrieve functions properties absolute path
		// Functions folder must be near ADWinIncludesFiles folder
		Process process = (Process) ((ResourceEditorInput) scriptSegmentEditor.getEditorInput()).getObject();
		int i = 0;
		for (String functionFilePath : ArduinoUnoFunctionsFiles) {
			if (functionFilePath.equals(SEPARATOR)) {
				functionMenuManager.add(new Separator());
			} else {
				String menuTitle = FunctionFactory.getProperty(process, functionFilePath, FunctionFactory.MENU_TITLE);
				String menuTooltip = FunctionFactory.getProperty(process, functionFilePath,
						FunctionFactory.DESCRIPTION);
				if (menuTitle != null && menuTooltip != null) {
					AssignFunctionAction assignFunctionAction = new AssignFunctionAction(scriptSegmentEditor,
							blockEditPart, menuTitle, menuTooltip, ArduinoUnoFunctionsClasses[i]);
					assignFunctionAction.setLazyEnablementCalculation(false);
					if(ArduinoUnoFunctionsClasses[i].equals(AnalogWaitFunction.class.getName())) assignFunctionAction.setEnabled(scriptSegmentEditor instanceof ArduinoUnoInitializeSegmentEditor);
					if(ArduinoUnoFunctionsClasses[i].equals(DigitalWaitFunction.class.getName())) assignFunctionAction.setEnabled(scriptSegmentEditor instanceof ArduinoUnoInitializeSegmentEditor);
					functionMenuManager.add(assignFunctionAction);
				}
			}
			i++;
		}
		// Retrieve customer functions
		String[] customerFunctions = FunctionFactory.getCustomerFunctions(process);
		if (customerFunctions.length > 0) {
			functionMenuManager.add(new Separator());
			MenuManager customerFunctionsMenuManager = new MenuManager(DocometreMessages.CustomerFunctionsMenuLabel, CUSTOMER_FUNCTIONS_MENU);
			for (String customerFunction : customerFunctions) {
				String menuTitle = FunctionFactory.getProperty(process, customerFunction, FunctionFactory.MENU_TITLE, true);
				String menuTooltip = FunctionFactory.getProperty(process, customerFunction, FunctionFactory.DESCRIPTION, true);
				if (menuTitle != null && menuTooltip != null) {
					AssignFunctionAction assignFunctionAction = new AssignFunctionAction(scriptSegmentEditor, blockEditPart, menuTitle, menuTooltip, customerFunction);
					assignFunctionAction.setLazyEnablementCalculation(false);
					customerFunctionsMenuManager.add(assignFunctionAction);
				}
			}
			functionMenuManager.add(customerFunctionsMenuManager);
		}
	}

}
