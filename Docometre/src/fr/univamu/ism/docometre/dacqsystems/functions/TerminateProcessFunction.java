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

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoProcess;
import fr.univamu.ism.docometre.dialogs.ConditionalBlockArea;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Operator;
import fr.univamu.ism.process.ScriptSegmentType;

public final class TerminateProcessFunction  extends GenericFunction implements ModifyListener, ISelectionChangedListener {
	
	public static final String functionFileName = "TERMINATE_PROCESS.FUN";
	
	private static final long serialVersionUID = 1L;
	
	private static final String leftOperandKey = "leftOperandKey";
	private static final String operatorKey = "operatorKey";
	private static final String rightOperandKey = "rightOperandKey";
	
	@Override
	public String getFunctionFileName() {
		return functionFileName;
	}
	
	@Override
	public Object getGUI(Object titleAreaDialog, Object parent, Object context) {
		if(!(context instanceof Process)) return null;
		Composite container  = (Composite) parent;
		Composite paramContainer = new Composite(container, SWT.NORMAL);
		paramContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		paramContainer.setLayout(new GridLayout(2, false));
		
		Label infoLabel = new Label(paramContainer, SWT.NONE);
		infoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		infoLabel.setText(DocometreMessages.When);

		String leftOperand = getProperty(leftOperandKey, "");
		String operator = getProperty(operatorKey, Operator.IS_EQUAL_TO.getValue());
		String rightOperand = getProperty(rightOperandKey, "");
		
		Process process = (Process) context;
		DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
		
		ConditionalBlockArea conditionalBlockArea = new ConditionalBlockArea(paramContainer, SWT.NONE, leftOperand, rightOperand, Operator.getOperator(operator), dacqConfiguration.getProposal());
		conditionalBlockArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		conditionalBlockArea.addLeftOperandModifyListener(this);
		conditionalBlockArea.addRightOperandModifyListener(this);
		conditionalBlockArea.addOperatorModifyListener(this);
		getTransientProperties().put(operatorKey, operator);
		
		addCommentField(paramContainer, 1, context);
		
		return paramContainer;
	}
	
	@Override
	public String getCode(Object context, Object step) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
		String code = "";
		Process process = (Process) context;
		if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
			if(process instanceof ADWinProcess) {
				code = code + "\nREM Terminate Process Function\n\n";
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, FUNCTION_CODE);
				String leftOperand  = getProperty(leftOperandKey, "");
				String operator  = getProperty(operatorKey, "");
				String rightOperand  = getProperty(rightOperandKey, "");
				temporaryCode = temporaryCode.replaceAll(leftOperandKey, leftOperand).replaceAll(operatorKey, operator).replaceAll(rightOperandKey, rightOperand);
				code = code + temporaryCode + "\n\n";
			}
			if(process instanceof ArduinoUnoProcess) {
				String indent = (step == ScriptSegmentType.FINALIZE) ? UNO_FINALIZE_INDENT : UNO_DEFAULT_INDENT;
				code = code + "\n" + indent + "// Terminate Process Function\n";
				String temporaryCode = FunctionFactory.getProperty(process, functionFileName, FUNCTION_CODE);
				String leftOperand  = getProperty(leftOperandKey, "");
				String operator  = getProperty(operatorKey, " == ");
				if(Operator.IS_EQUAL_TO.getValue().equals(operator)) operator = " == ";
				if(Operator.IS_NOT_EQUAL_TO.getValue().equals(operator)) operator = " != ";
				String rightOperand  = getProperty(rightOperandKey, "");
				temporaryCode = indent + temporaryCode.replaceAll(leftOperandKey, leftOperand).replaceAll(operatorKey, operator).replaceAll(rightOperandKey, rightOperand);
				temporaryCode = temporaryCode.replaceAll("\n", "\n" + indent);
				code = code + temporaryCode + "\n";
			}
		}
		return code;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Operator operator = (Operator) ((IStructuredSelection)event.getSelection()).getFirstElement();
		getTransientProperties().put(operatorKey, operator.getValue());
	}

	@Override
	public void modifyText(ModifyEvent event) {
		Text text = (Text) event.widget;
		String key = leftOperandKey;
		if(((String)text.getData()).equals("rightOperandText")) key = rightOperandKey;
		getTransientProperties().put(key, text.getText());
		
	}
	
	@Override
	public Block clone() {
		TerminateProcessFunction function = new TerminateProcessFunction();
		super.clone(function);
		return function;
	}
}
