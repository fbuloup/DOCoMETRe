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

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.functions.DocometreContentProposalProvider;
import fr.univamu.ism.process.Operator;

public class ConditionalBlockArea extends Composite {

	private Text leftOperandText;
	private ComboViewer operatorComboViewer;
	private Text rightOperandText;

	public ConditionalBlockArea(Composite container, int style, String leftOperand, String rightOperand, Object operator, String[] proposal) {
		super(container, style);
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setLayout(new GridLayout(3, false));
		((GridLayout)getLayout()).marginLeft = 5;
		((GridLayout)getLayout()).marginRight = 5;
		
		leftOperandText = new Text(this, SWT.BORDER);
		leftOperandText.setData("leftOperandText");
		leftOperandText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		leftOperandText.setText(leftOperand);
		ControlDecoration leftOperandCD = new ControlDecoration(leftOperandText, SWT.TOP | SWT.LEFT);
		leftOperandCD.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
		leftOperandCD.setDescriptionText(DocometreMessages.UseCtrlSpaceProposal);
		leftOperandCD.setShowOnlyOnFocus(true);
		leftOperandCD.setMarginWidth(5);
		
		operatorComboViewer = new ComboViewer(this);
		operatorComboViewer.getCombo().setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		operatorComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		operatorComboViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if(element instanceof Operator) return ((Operator)element).getValue();
				return super.getText(element);
			}
		});
		operatorComboViewer.setInput(Operator.getValues());
		operatorComboViewer.setSelection(new StructuredSelection(operator));
		
		rightOperandText = new Text(this, SWT.BORDER);
		rightOperandText.setData("rightOperandText");
		rightOperandText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		rightOperandText.setText(rightOperand);
		ControlDecoration rightOperandCD = new ControlDecoration(rightOperandText, SWT.TOP | SWT.RIGHT);
		rightOperandCD.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
		rightOperandCD.setDescriptionText(DocometreMessages.UseCtrlSpaceProposal);
		rightOperandCD.setShowOnlyOnFocus(true);
		rightOperandCD.setMarginWidth(5);
		
		try {
			KeyStroke keyStroke = KeyStroke.getInstance("CTRL+SPACE");
			DocometreContentProposalProvider leftProposalProvider = new DocometreContentProposalProvider(proposal, leftOperandText);
			leftProposalProvider.setFiltering(true);
			
			DocometreContentProposalProvider rightProposalProvider = new DocometreContentProposalProvider(proposal, rightOperandText);
			rightProposalProvider.setFiltering(true);
			
			ContentProposalAdapter leftProposalAdapter = new ContentProposalAdapter(leftOperandText, new TextContentAdapter(), leftProposalProvider, keyStroke, null);
			leftProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
			leftProposalAdapter.addContentProposalListener(leftProposalProvider);
			
			ContentProposalAdapter rightProposalAdapter = new ContentProposalAdapter(rightOperandText, new TextContentAdapter(), rightProposalProvider, keyStroke, null);
			rightProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
			rightProposalAdapter.addContentProposalListener(rightProposalProvider);
			
		} catch (ParseException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
	}
	
	public String getLeftOperand() {
		return leftOperandText.getText();
	}
	
	public Operator getOperand() {
		return (Operator) ((IStructuredSelection)operatorComboViewer.getSelection()).getFirstElement();
	}
	
	public String getRightOperand() {
		return rightOperandText.getText();
	}
	
	public void addLeftOperandModifyListener(ModifyListener listener) {
		leftOperandText.addModifyListener(listener);
	}
	
	public void addOperatorModifyListener(ISelectionChangedListener listener) {
		operatorComboViewer.addSelectionChangedListener(listener);
	}
	
	public void addRightOperandModifyListener(ModifyListener listener) {
		rightOperandText.addModifyListener(listener);
	}

}
