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
package fr.univamu.ism.docometre.analyse.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoProcess;
import fr.univamu.ism.docometre.dacqsystems.functions.DocometreContentProposalProvider;
import fr.univamu.ism.docometre.dacqsystems.functions.GenericFunction;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.ScriptSegmentType;

public final class ExpressionFunction extends GenericFunction {
	
	public static final String functionFileName = "EXPRESSION.FUN";
	
	private static final long serialVersionUID = 1L;
	
	private static final String expressionKey = "expression";

	private transient ListViewer expressionsListViewer;

	private transient SourceViewer sourceViewer;
	
	@Override
	public String getFunctionFileName() {
		return functionFileName;
	}

	@Override
	public Object getGUI(Object titleAreaDialog, Object parent, Object context) {
		if(!(context instanceof Process)) return null;
		Composite container  = (Composite) parent;
		
		TabFolder tabFolder = new TabFolder(container, SWT.BORDER);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TabItem expressionsTabItem = new TabItem(tabFolder, SWT.BORDER);
		expressionsTabItem.setText(DocometreMessages.expressionsListTitle);
		TabItem sourceTabItem = new TabItem(tabFolder, SWT.BORDER);
		sourceTabItem.setText(DocometreMessages.sourceCodeTitle);
		
		Composite expressionsContainer = createExpressionsTabItem(tabFolder, context);
		Composite sourceContainer = createSourceTabItem(tabFolder, context);
		
		expressionsTabItem.setControl(expressionsContainer);
		sourceTabItem.setControl(sourceContainer);
		
		Composite commentContainer = new Composite(container, SWT.NORMAL);
		commentContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		commentContainer.setLayout(new GridLayout(3, false));
		
		// Default tabitem is expressions 
		tabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(tabFolder.getSelectionIndex() == 0) {
					// Switch from source to expressions => update expressions
					expressionsListViewer.setInput(getExpressionsFromSource());
				} else {
					// Switch from expressions to source => update source
					String[] expressionsList = expressionsListViewer.getList().getItems();
					StringBuffer stringBuffer = new StringBuffer();
					for (String expression : expressionsList) {
						stringBuffer.append(expression + "\n");
					}
					sourceViewer.getDocument().set(stringBuffer.toString());
				}
			}
		});
		
		addCommentField(commentContainer, 2, context);
		
		return tabFolder;
	}
	
	private String[] getExpressionsFromSource() {
		ArrayList<String> expressions = new ArrayList<String>();
		int nbLines = sourceViewer.getTextWidget().getLineCount();
		for (int i = 0; i <nbLines; i++) {
			String line = sourceViewer.getTextWidget().getLine(i);
			if(!"".equals(line)) expressions.add(line);
		}
		return expressions.toArray(new String[expressions.size()]);
	}
	
	private Composite createSourceTabItem(TabFolder tabFolder, Object context) {
		Composite sourceContainer = new Composite(tabFolder, SWT.NORMAL);
		sourceContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sourceContainer.setLayout(new GridLayout(1, false));
		
		sourceViewer = new SourceViewer(sourceContainer, null, SWT.BORDER);
		sourceViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sourceViewer.setDocument(new Document());
		
		sourceViewer.getDocument().addDocumentListener(new IDocumentListener() {
			@Override
			public void documentChanged(DocumentEvent event) {
				applyChangesToTransientProperties(getExpressionsFromSource());
			}
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		});
		return sourceContainer;
	}

	private Composite createExpressionsTabItem(TabFolder tabFolder, Object context) {
		Composite expressionsContainer = new Composite(tabFolder, SWT.NORMAL);
		expressionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		expressionsContainer.setLayout(new GridLayout(3, false));
		
		Label expressionLabel = new Label(expressionsContainer, SWT.NORMAL);
		expressionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		expressionLabel.setText("Expression : ");
		
		Text expressionText = new Text(expressionsContainer, SWT.BORDER);
		Button validateButton = new Button(expressionsContainer, SWT.FLAT);
		Composite expressionsListContainer = new Composite(expressionsContainer, SWT.NORMAL);
		expressionsListViewer = new ListViewer(expressionsListContainer, SWT.SINGLE | SWT.BORDER);
		Composite buttonsContainer = new Composite(expressionsListContainer, SWT.NORMAL);
		Button deleteButton = new Button(buttonsContainer, SWT.FLAT);
		Button upButton = new Button(buttonsContainer, SWT.FLAT);
		Button downButton = new Button(buttonsContainer, SWT.FLAT);
		
		expressionsListContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		expressionsListContainer.setLayout(new GridLayout(2, false));
		
		expressionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		expressionText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateButton.setEnabled(expressionText.getText().equals("")?false:true);
			}
		});
		ControlDecoration expressionCD = new ControlDecoration(expressionText, SWT.TOP | SWT.LEFT);
		expressionCD.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
		expressionCD.setDescriptionText(DocometreMessages.UseCtrlSpaceProposal);
		expressionCD.setShowOnlyOnFocus(true);
		expressionCD.setMarginWidth(5);
		try {
			Process process = (Process) context;
			DACQConfiguration dacqConfiguration = process.getDACQConfiguration();
			KeyStroke keyStroke = KeyStroke.getInstance("CTRL+SPACE");
			DocometreContentProposalProvider proposalProvider = new DocometreContentProposalProvider(dacqConfiguration.getProposal(), expressionText);
			proposalProvider.setFiltering(true);
			
			ContentProposalAdapter leftProposalAdapter = new ContentProposalAdapter(expressionText, new TextContentAdapter(), proposalProvider, keyStroke, null);
			leftProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
			leftProposalAdapter.addContentProposalListener(proposalProvider);
		} catch (ParseException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		
		
		
		validateButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		validateButton.setText("Add");
		validateButton.setData("ADD");
		validateButton.setEnabled(false);
		validateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArrayList<String> expressions = new ArrayList<>(Arrays.asList(expressionsListViewer.getList().getItems()));
				if(validateButton.getData().equals("ADD")) {
					expressions.add(expressionText.getText());
				} else {
					String expression = expressionText.getText();
					int index = expressionsListViewer.getList().getSelectionIndex();
					expressions.set(index, expression);
				}
				expressionsListViewer.setInput(expressions.toArray());
				applyChangesToTransientProperties(expressionsListViewer.getList().getItems());
				expressionsListViewer.setSelection(StructuredSelection.EMPTY);
				expressionText.setText("");
				validateButton.setData("ADD");
				validateButton.setEnabled(false);
			}
		});
		
		expressionsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		expressionsListViewer.setContentProvider(ArrayContentProvider.getInstance());
		expressionsListViewer.setLabelProvider(new LabelProvider());
		
		String expressions = getProperty(expressionKey, "");
		String[] expressionsArray = expressions.split("\\n");
		expressionsListViewer.setInput(expressions.equals("")?null:expressionsArray);
		
		expressionsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = ((IStructuredSelection)event.getSelection());
				validateButton.setText("Add");
				validateButton.setData("ADD");
				expressionText.setText("");
				deleteButton.setEnabled(false);
				upButton.setEnabled(false);
				downButton.setEnabled(false);
				if(!selection.isEmpty()) {
					validateButton.setText("Modify");
					validateButton.setData("MODIFY");
					expressionText.setText((String) selection.getFirstElement());
					deleteButton.setEnabled(true);
					upButton.setEnabled(expressionsListViewer.getList().getSelectionIndex() > 0);
					downButton.setEnabled(expressionsListViewer.getList().getSelectionIndex() < expressionsListViewer.getList().getItemCount() - 1);
				}
				validateButton.setEnabled(false);
				expressionsContainer.layout();
			}
		});
		
		buttonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		buttonsContainer.setLayout(new GridLayout());
		
		deleteButton.setLayoutData(new GridData());
		deleteButton.setEnabled(false);
		deleteButton.setImage(Activator.getSharedImage(ISharedImages.IMG_TOOL_DELETE));
		deleteButton.setToolTipText(DocometreMessages.DeleteAction_Text);
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArrayList<String> expressions = new ArrayList<>(Arrays.asList(expressionsListViewer.getList().getItems()));
				int index = expressionsListViewer.getList().getSelectionIndex();
				expressions.remove(index);
				expressionsListViewer.setInput(expressions.toArray());
				applyChangesToTransientProperties(expressionsListViewer.getList().getItems());
				expressionsListViewer.setSelection(StructuredSelection.EMPTY);
			}
		});
		
		upButton.setLayoutData(new GridData());
		upButton.setEnabled(false);
		upButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		upButton.setToolTipText(DocometreMessages.Up_Label);
		upButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArrayList<String> expressions = new ArrayList<>(Arrays.asList(expressionsListViewer.getList().getItems()));
				int index = expressionsListViewer.getList().getSelectionIndex();
				ISelection selection = expressionsListViewer.getSelection();
				Collections.swap(expressions, index, index - 1);
				expressionsListViewer.setInput(expressions.toArray());
				applyChangesToTransientProperties(expressionsListViewer.getList().getItems());
				expressionsListViewer.setSelection(selection);
			}
		});
		
		downButton.setLayoutData(new GridData());
		downButton.setEnabled(false);
		downButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		downButton.setToolTipText(DocometreMessages.Down_Label);
		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArrayList<String> expressions = new ArrayList<>(Arrays.asList(expressionsListViewer.getList().getItems()));
				int index = expressionsListViewer.getList().getSelectionIndex();
				ISelection selection = expressionsListViewer.getSelection();
				Collections.swap(expressions, index, index + 1);
				expressionsListViewer.setInput(expressions.toArray());
				applyChangesToTransientProperties(expressionsListViewer.getList().getItems());
				expressionsListViewer.setSelection(selection);
			}
		});
		
		return expressionsContainer;
		
	}

	@Override
	public String getCode(Object context, Object step, Object...objects) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
		String code = "";
		Process process = (Process) context;
		if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
			if(process instanceof ADWinProcess) {
					code = code + "\nREM Expression Function\n\n";
					String temporaryCode = FunctionFactory.getProperty(process, functionFileName, FUNCTION_CODE);
					String expressions  = getProperty(expressionKey, "");
					temporaryCode = temporaryCode.replaceAll(expressionKey, expressions);
					code = code + temporaryCode + "\n\n";
			}
			if(process instanceof ArduinoUnoProcess) {
					String indent = (step == ScriptSegmentType.FINALIZE) ? UNO_FINALIZE_INDENT : UNO_DEFAULT_INDENT;
					code = code + "\n" + indent + "// Expression Function\n";
					String temporaryCode = FunctionFactory.getProperty(process, functionFileName, FUNCTION_CODE);
					String expressions  = getProperty(expressionKey, "");
					expressions = expressions.replaceAll("\n", ";\n" + indent);
					expressions = expressions + ";";
					temporaryCode = indent + temporaryCode.replaceAll(expressionKey, expressions);
					code = code + temporaryCode  + "\n";
			}
		}
		return code;
	}
	
	private void applyChangesToTransientProperties(String[] items) {
		String expressionsString = "";
		int i = 1;
		for (String item : items) {
			expressionsString = expressionsString + item;
			if(i < items.length) expressionsString = expressionsString + "\n";
			i++;
		}
		getTransientProperties().put(expressionKey, expressionsString);
	}
	
	@Override
	public Block clone() {
		ExpressionFunction function = new ExpressionFunction();
		super.clone(function);
		return function;
	}
	
	@Override
	public String getDescription(Object process) {
		return getProperty(expressionKey, "");
	}

}
