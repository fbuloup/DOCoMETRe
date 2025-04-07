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
package fr.univamu.ism.docometre.dacqsystems;

import java.io.File;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IMessageManager;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.charts.ChartConfigurationProperties;
import fr.univamu.ism.docometre.editors.ResourceEditor;

public class ModifyPropertyHandler extends SelectionAdapter implements ModifyListener {
	
		private Control control;
		private String regexp;
		private String errorMessage;
		private boolean fileOrFolder;
		private IMessageManager messageManager;
		private Property property;
		private IOperationHistory operationHistory;
		private AbstractElement element;
		private IUndoContext undoContext;
		
		public ModifyPropertyHandler(Property property, AbstractElement element, Control control, String regexp, String errorMessage, boolean fileOrFolder, ResourceEditor resourceEditor) {
			this.control = control;
			this.regexp = regexp;
			this.errorMessage = errorMessage;
			this.fileOrFolder= fileOrFolder;
			messageManager = resourceEditor.getActivePageInstance().getManagedForm().getMessageManager();
			this.property = property;
			operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
			this.undoContext = resourceEditor.getUndoContext();
			this.element = element;
		}
		
		@Override
		public void modifyText(ModifyEvent event) {
			boolean runOperation = true;
			messageManager.removeMessage(control, control);
			String text = "";
			if(control instanceof Text) text = ((Text)control).getText();
			if(control instanceof Combo) text = ((Combo)control).getText();
			if(!text.matches(regexp)) {
				String tempErrorMessage = errorMessage;
				if(fileOrFolder) tempErrorMessage = NLS.bind(errorMessage, text);
				messageManager.addMessage(control, tempErrorMessage, null, IMessageProvider.ERROR, control);
				runOperation= false;
			} else if(fileOrFolder) {
				File file = new File(text);
				if(!file.exists()) {
					String message = NLS.bind(DocometreMessages.ErrorFileFolderNotExists, text);
					messageManager.addMessage(control, message, null, IMessageProvider.ERROR, control);
					runOperation= false;
				}
			}
			if(runOperation) {
				boolean modifyRunned = false;
				if(control.getData("modifyRunned") != null) {
					modifyRunned = (boolean) control.getData("modifyRunned");
					if(modifyRunned) {
						control.setData("modifyRunned", false);
						return;
					}
				}
				else control.setData("modifyRunned", true);
				try {
					String label = NLS.bind(DocometreMessages.ModifyPropertyOperation_Label, property.getLabel() + " : " + element.toString());
					operationHistory.execute(new ModifyPropertyOperation(control, label, property, element, text, undoContext), null, null);
				} catch (ExecutionException e) {
					e.printStackTrace();
					Activator.logErrorMessageWithCause(e);
				} 

			}
		}
		
		@Override
		public void widgetSelected(SelectionEvent event) {
			String text = "";
			if(control instanceof Button) {
				Object object = (String) control.getData("isColorDialog");
				if(object != null && object instanceof String) {
					String needColorDialog = (String)object;
					if("yes".equalsIgnoreCase(needColorDialog)) {
						Color chartColor = ChartConfigurationProperties.getColor(element, property);
						ColorDialog colorDialog = new ColorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
						if(chartColor != null) colorDialog.setRGB(chartColor.getRGB());
						RGB rgbColor = colorDialog.open();
						if(rgbColor != null) text = rgbColor.toString();
					}
				} else text = String.valueOf(((Button)control).getSelection());
			}
			boolean modifyRunned = false;
			if(control.getData("modifyRunned") != null) {
				modifyRunned = (boolean) control.getData("modifyRunned");
				if(modifyRunned) {
					control.setData("modifyRunned", false);
					return;
				}
			}
			else control.setData("modifyRunned", true);
			try {
				String label = NLS.bind(DocometreMessages.ModifyPropertyOperation_Label, property.getLabel() + " : " + element.toString());
				operationHistory.execute(new ModifyPropertyOperation(control, label, property, element, text, undoContext), null, null);
			} catch (ExecutionException e) {
				e.printStackTrace();
				Activator.logErrorMessageWithCause(e);
			} 

		}
		
		public void setRegExp(String regExp) {
			this.regexp = regExp;
		}
	}
