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
package fr.univamu.ism.docometre.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.process.Function;

public class FunctionalBlockConfigurationDialog extends TitleAreaDialog {
	
	private Function function;
	private Object context;
//	private String blockingErrorMessage;
	
	public FunctionalBlockConfigurationDialog(Shell parentShell, Object context, Function function) {
		super(parentShell);
		setShellStyle(getShellStyle()); 
		this.function = function;
		this.context = context;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(DocometreMessages.FunctionConfigurationDialog);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(NLS.bind(DocometreMessages.FunctionConfigurationDialog_Title, function.getTitle(context)));
		setMessage(NLS.bind(DocometreMessages.FunctionConfigurationDialog_Message, function.getDescription(context)));
		setTitleImage(Activator.getImage(IImageKeys.CONFIGURE_FUNCTION_WIZBAN));
		
		Composite container = (Composite) super.createDialogArea(parent);
		Composite composite = (Composite)function.getGUI(this , container, context);
		if(composite != null) {
			// Build the separator line
			(new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR)).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		
		return container;
	}
	
	
	@Override
	public void setErrorMessage(String newErrorMessage) {
//		String errorMessage = newErrorMessage == null ? blockingErrorMessage : newErrorMessage;
		super.setErrorMessage(newErrorMessage);
		if(getButton(OK) != null) getButton(OK).setEnabled(newErrorMessage == null);
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		setErrorMessage(getErrorMessage());
		return control;
	}
	
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	public String getErrorMessage() {
		String errorMessage = super.getErrorMessage();
//		if(errorMessage == null) errorMessage = blockingErrorMessage;
		return errorMessage;
	}
	
//	public void setBlockingErrorMessage(String blockingErrorMessage) {
//		this.blockingErrorMessage = blockingErrorMessage;
//	}

}
