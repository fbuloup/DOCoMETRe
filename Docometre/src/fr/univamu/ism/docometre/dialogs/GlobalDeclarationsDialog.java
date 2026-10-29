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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.scripteditor.actions.GlobalDeclarationsClipboard;
import fr.univamu.ism.docometre.scripteditor.actions.GlobalDeclarationsClipboard.PastedDeclarations;
import fr.univamu.ism.process.GlobalDeclarations.Constant;
import fr.univamu.ism.process.GlobalDeclarations.Content;
import fr.univamu.ism.process.GlobalDeclarations.Library;
import fr.univamu.ism.process.GlobalDeclarations.Subroutine;

/**
 * Edit global libraries, constants and subroutines of a process. 
 * All or part of them can be copied in order to be pasted in another process or in an external editor, 
 * and source code text copied in an external editor can be pasted.
 */
public class GlobalDeclarationsDialog extends TitleAreaDialog {
	
	private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
	// Constants already defined in generated code (pi for ADBasic and Arduino, PI for Arduino)
	private static final String[] PREDEFINED_NAMES = new String[] {"pi", "PI"};
	// C library : <file> or "file"
	private static final Pattern C_LIBRARY_PATTERN = Pattern.compile("<[^<>]+>|\"[^\"]+\"");
	
	private class LibraryEditingSupport extends EditingSupport {
		
		private int column;
		private TextCellEditor cellEditor;

		public LibraryEditingSupport(TableViewer viewer, int column) {
			super(viewer);
			this.column = column;
			cellEditor = new TextCellEditor(viewer.getTable());
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return cellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			return getColumnText((Library) element, column);
		}

		@Override
		protected void setValue(Object element, Object value) {
			Library library = (Library) element;
			if(value.equals(getColumnText(library, column))) return;
			String file = column == 0 ? ((String) value).trim() : library.getFile();
			String comment = column == 1 ? (String) value : library.getComment();
			libraries.set(indexOf(libraries, library), new Library(file, comment));
			librariesViewer.refresh();
			validate();
		}
		
	}
	
	private class ConstantEditingSupport extends EditingSupport {
		
		private int column;
		private TextCellEditor cellEditor;

		public ConstantEditingSupport(TableViewer viewer, int column) {
			super(viewer);
			this.column = column;
			cellEditor = new TextCellEditor(viewer.getTable());
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return cellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			return getColumnText((Constant) element, column);
		}

		@Override
		protected void setValue(Object element, Object value) {
			Constant constant = (Constant) element;
			if(value.equals(getColumnText(constant, column))) return;
			String name = column == 0 ? (String) value : constant.getName();
			String constantValue = column == 1 ? (String) value : constant.getValue();
			String comment = column == 2 ? (String) value : constant.getComment();
			constants.set(indexOf(constants, constant), new Constant(name.trim(), constantValue.trim(), comment));
			constantsViewer.refresh();
			validate();
		}
		
	}
	
	private Object context;
	private List<String> pasteReport;
	private boolean caseSensitive;
	private boolean adbasic;
	private Set<String> reservedNames = new HashSet<>();
	private List<Library> libraries;
	private List<Constant> constants;
	private List<Subroutine> subroutines;
	private Content content;
	
	private TableViewer librariesViewer;
	private TableViewer constantsViewer;
	private ListViewer subroutinesViewer;
	private Text nameText;
	private Text parametersText;
	private Text bodyText;
	private Label signatureLabel;
	private Label endLabel;
	private boolean updatingSubroutineFields;
	private boolean modifyingSubroutine;

	public GlobalDeclarationsDialog(Shell parentShell, Object context, Content content) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		libraries = content.getLibraries();
		constants = content.getConstants();
		subroutines = content.getSubroutines();
		this.context = context;
		caseSensitive = GlobalDeclarationsClipboard.isCaseSensitive(context);
		adbasic = context instanceof ADWinProcess;
		List<String> names = new ArrayList<>(Arrays.asList(PREDEFINED_NAMES));
		if(context instanceof Process && ((Process)context).getDACQConfiguration() != null) 
			names.addAll(Arrays.asList(((Process)context).getDACQConfiguration().getProposal()));
		for (String name : names) reservedNames.add(getKey(name));
	}
	
	@Override
	public void create() {
		super.create();
		getShell().setText(DocometreMessages.GlobalDeclarationsDialogTitle);
		setTitle(DocometreMessages.GlobalDeclarationsDialogTitle);
		setMessage(DocometreMessages.GlobalDeclarationsDialogMessage, IMessageProvider.INFORMATION);
		setTitleImage(Activator.getImage(IImageKeys.CONFIGURE_FUNCTION_WIZBAN));
		if(pasteReport != null) showPasteReport(true, pasteReport);
		validate();
	}
	
	/**
	 * Dialog is opened to review declarations read from pasted source code text
	 * @param ignoredLines lines of pasted text that are neither constants nor subroutines
	 */
	public void setPasteReport(List<String> ignoredLines) {
		pasteReport = ignoredLines;
	}
	
	private void showPasteReport(boolean fromText, List<String> ignoredLines) {
		if(!ignoredLines.isEmpty()) setMessage(NLS.bind(DocometreMessages.GlobalDeclarationsIgnoredLines, ignoredLines.size(), String.join(" ; ", ignoredLines)), IMessageProvider.WARNING);
		else if(fromText) setMessage(DocometreMessages.GlobalDeclarationsPastedText, IMessageProvider.INFORMATION);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		TabFolder tabFolder = new TabFolder(area, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TabItem librariesTabItem = new TabItem(tabFolder, SWT.NONE);
		librariesTabItem.setText(DocometreMessages.GlobalDeclarationsLibrariesTab);
		librariesTabItem.setImage(Activator.getImage(IImageKeys.LIBRARY_ICON));
		librariesTabItem.setControl(createLibrariesArea(tabFolder));
		
		TabItem constantsTabItem = new TabItem(tabFolder, SWT.NONE);
		constantsTabItem.setText(DocometreMessages.GlobalDeclarationsConstantsTab);
		constantsTabItem.setImage(Activator.getImage(IImageKeys.CONSTANT_ICON));
		constantsTabItem.setControl(createConstantsArea(tabFolder));
		
		TabItem subroutinesTabItem = new TabItem(tabFolder, SWT.NONE);
		subroutinesTabItem.setText(DocometreMessages.GlobalDeclarationsSubroutinesTab);
		subroutinesTabItem.setImage(Activator.getImage(IImageKeys.SUBROUTINE_ICON));
		subroutinesTabItem.setControl(createSubroutinesArea(tabFolder));
		
		return area;
	}
	
	private Control createLibrariesArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		
		librariesViewer = new TableViewer(container, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		Table table = librariesViewer.getTable();
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = 600;
		gridData.heightHint = 250;
		table.setLayoutData(gridData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		String[] titles = new String[] {DocometreMessages.GlobalDeclarationsFileColumn, DocometreMessages.GlobalDeclarationsCommentColumn};
		int[] widths = new int[] {250, 330};
		for (int i = 0; i < titles.length; i++) {
			final int column = i;
			TableViewerColumn viewerColumn = new TableViewerColumn(librariesViewer, SWT.NONE);
			viewerColumn.getColumn().setText(titles[i]);
			viewerColumn.getColumn().setWidth(widths[i]);
			viewerColumn.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return getColumnText((Library) element, column);
				}
			});
			viewerColumn.setEditingSupport(new LibraryEditingSupport(librariesViewer, column));
		}
		librariesViewer.setContentProvider(ArrayContentProvider.getInstance());
		librariesViewer.setInput(libraries);
		
		Composite buttonsContainer = createButtonsContainer(container);
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsAddButton, () -> {
			Library library = new Library(getUniqueLibrary(), "");
			libraries.add(library);
			librariesViewer.refresh();
			librariesViewer.setSelection(new StructuredSelection(library));
			librariesViewer.editElement(library, 0);
			validate();
		});
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsRemoveButton, () -> {
			for (Object object : librariesViewer.getStructuredSelection().toList()) libraries.remove(indexOf(libraries, object));
			librariesViewer.refresh();
			validate();
		});
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsCopyButton, () -> {
			List<Library> selectedLibraries = new ArrayList<>();
			for (Object object : librariesViewer.getStructuredSelection().toList()) selectedLibraries.add((Library) object);
			copy(new Content(selectedLibraries, null, null));
		});
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsPasteButton, () -> paste());
		return container;
	}
	
	private Control createConstantsArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		
		constantsViewer = new TableViewer(container, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		Table table = constantsViewer.getTable();
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = 600;
		gridData.heightHint = 250;
		table.setLayoutData(gridData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		String[] titles = new String[] {DocometreMessages.GlobalDeclarationsNameColumn, DocometreMessages.GlobalDeclarationsValueColumn, DocometreMessages.GlobalDeclarationsCommentColumn};
		int[] widths = new int[] {150, 150, 280};
		for (int i = 0; i < titles.length; i++) {
			final int column = i;
			TableViewerColumn viewerColumn = new TableViewerColumn(constantsViewer, SWT.NONE);
			viewerColumn.getColumn().setText(titles[i]);
			viewerColumn.getColumn().setWidth(widths[i]);
			viewerColumn.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return getColumnText((Constant) element, column);
				}
			});
			viewerColumn.setEditingSupport(new ConstantEditingSupport(constantsViewer, column));
		}
		constantsViewer.setContentProvider(ArrayContentProvider.getInstance());
		constantsViewer.setInput(constants);
		
		Composite buttonsContainer = createButtonsContainer(container);
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsAddButton, () -> {
			Constant constant = new Constant(getUniqueName("CONSTANT"), "0", "");
			constants.add(constant);
			constantsViewer.refresh();
			constantsViewer.setSelection(new StructuredSelection(constant));
			constantsViewer.editElement(constant, 0);
			validate();
		});
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsRemoveButton, () -> {
			for (Object object : constantsViewer.getStructuredSelection().toList()) constants.remove(indexOf(constants, object));
			constantsViewer.refresh();
			validate();
		});
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsCopyButton, () -> {
			List<Constant> selectedConstants = new ArrayList<>();
			for (Object object : constantsViewer.getStructuredSelection().toList()) selectedConstants.add((Constant) object);
			copy(new Content(selectedConstants, null));
		});
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsPasteButton, () -> paste());
		return container;
	}
	
	private Control createSubroutinesArea(Composite parent) {
		SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
		
		Composite listContainer = new Composite(sashForm, SWT.NONE);
		listContainer.setLayout(new GridLayout(2, false));
		subroutinesViewer = new ListViewer(listContainer, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		subroutinesViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		subroutinesViewer.setContentProvider(ArrayContentProvider.getInstance());
		subroutinesViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Subroutine)element).getName();
			}
		});
		subroutinesViewer.setInput(subroutines);
		subroutinesViewer.addSelectionChangedListener(event -> {
			if(!modifyingSubroutine) updateSubroutineFields();
		});
		
		Composite buttonsContainer = createButtonsContainer(listContainer);
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsAddButton, () -> {
			Subroutine subroutine = new Subroutine(getUniqueName("subroutine"), "", "");
			subroutines.add(subroutine);
			subroutinesViewer.refresh();
			subroutinesViewer.setSelection(new StructuredSelection(subroutine));
			nameText.setFocus();
			nameText.selectAll();
			validate();
		});
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsRemoveButton, () -> {
			for (Object object : subroutinesViewer.getStructuredSelection().toList()) subroutines.remove(indexOf(subroutines, object));
			subroutinesViewer.refresh();
			updateSubroutineFields();
			validate();
		});
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsCopyButton, () -> {
			List<Subroutine> selectedSubroutines = new ArrayList<>();
			for (Object object : subroutinesViewer.getStructuredSelection().toList()) selectedSubroutines.add((Subroutine) object);
			copy(new Content(null, selectedSubroutines));
		});
		createButton(buttonsContainer, DocometreMessages.GlobalDeclarationsPasteButton, () -> paste());
		
		Composite fieldsContainer = new Composite(sashForm, SWT.NONE);
		fieldsContainer.setLayout(new GridLayout(2, false));
		Label nameLabel = new Label(fieldsContainer, SWT.NONE);
		nameLabel.setText(DocometreMessages.GlobalDeclarationsNameColumn);
		nameText = new Text(fieldsContainer, SWT.BORDER);
		nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Label parametersLabel = new Label(fieldsContainer, SWT.NONE);
		parametersLabel.setText(DocometreMessages.GlobalDeclarationsParametersLabel);
		parametersText = new Text(fieldsContainer, SWT.BORDER);
		parametersText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		signatureLabel = new Label(fieldsContainer, SWT.NONE);
		signatureLabel.setFont(JFaceResources.getTextFont());
		signatureLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		bodyText = new Text(fieldsContainer, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		bodyText.setFont(JFaceResources.getTextFont());
		// Tab key inserts a tabulation in body instead of moving focus
		bodyText.addTraverseListener(event -> {
			if(event.detail == SWT.TRAVERSE_TAB_NEXT || event.detail == SWT.TRAVERSE_TAB_PREVIOUS) event.doit = false;
		});
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gridData.widthHint = 450;
		gridData.heightHint = 220;
		bodyText.setLayoutData(gridData);
		endLabel = new Label(fieldsContainer, SWT.NONE);
		endLabel.setFont(JFaceResources.getTextFont());
		endLabel.setText(adbasic ? "ENDSUB" : "}");
		endLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		nameText.addModifyListener(event -> subroutineFieldsModified());
		parametersText.addModifyListener(event -> subroutineFieldsModified());
		bodyText.addModifyListener(event -> subroutineFieldsModified());
		
		sashForm.setWeights(new int[] {2, 5});
		updateSubroutineFields();
		return sashForm;
	}
	
	private Composite createButtonsContainer(Composite parent) {
		Composite buttonsContainer = new Composite(parent, SWT.NONE);
		buttonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		GridLayout gridLayout = new GridLayout(1, true);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		buttonsContainer.setLayout(gridLayout);
		return buttonsContainer;
	}
	
	private Button createButton(Composite parent, String text, Runnable runnable) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.addListener(SWT.Selection, event -> runnable.run());
		return button;
	}
	
	/*
	 * Fill fields with selected subroutine, fields are disabled when there is not exactly one selected subroutine
	 */
	private void updateSubroutineFields() {
		IStructuredSelection selection = subroutinesViewer.getStructuredSelection();
		Subroutine subroutine = selection.size() == 1 ? (Subroutine) selection.getFirstElement() : null;
		updatingSubroutineFields = true;
		nameText.setText(subroutine == null ? "" : subroutine.getName());
		parametersText.setText(subroutine == null ? "" : subroutine.getParameters());
		bodyText.setText(subroutine == null ? "" : subroutine.getBody());
		updatingSubroutineFields = false;
		nameText.setEnabled(subroutine != null);
		parametersText.setEnabled(subroutine != null);
		bodyText.setEnabled(subroutine != null);
		updateSignature();
	}
	
	private void subroutineFieldsModified() {
		if(updatingSubroutineFields) return;
		IStructuredSelection selection = subroutinesViewer.getStructuredSelection();
		if(selection.size() != 1) return;
		Subroutine subroutine = new Subroutine(nameText.getText().trim(), parametersText.getText().trim(), bodyText.getText());
		subroutines.set(indexOf(subroutines, selection.getFirstElement()), subroutine);
		// Keep fields as they are while user is typing
		modifyingSubroutine = true;
		subroutinesViewer.refresh();
		subroutinesViewer.setSelection(new StructuredSelection(subroutine));
		modifyingSubroutine = false;
		updateSignature();
		validate();
	}
	
	private void updateSignature() {
		String name = nameText.getText().trim();
		String parameters = parametersText.getText().trim();
		if(adbasic) signatureLabel.setText("SUB " + name + "(" + parameters + ")");
		else signatureLabel.setText("void " + name + "(" + parameters + ") {");
	}
	
	private void copy(Content selectedContent) {
		if(selectedContent.isEmpty()) return;
		GlobalDeclarationsClipboard.copy(selectedContent, context);
	}
	
	/*
	 * Paste declarations copied in DOCoMETRe or source code text copied in an external editor.
	 * Paste buttons are always enabled : system clipboard may change in another application at any time.
	 */
	private void paste() {
		PastedDeclarations pastedDeclarations = GlobalDeclarationsClipboard.getPastedDeclarations(context);
		if(pastedDeclarations == null) {
			setMessage(DocometreMessages.GlobalDeclarationsNothingToPaste, IMessageProvider.WARNING);
			return;
		}
		Content mergedContent = GlobalDeclarationsClipboard.merge(getShell(), new Content(libraries, constants, subroutines), pastedDeclarations.getContent(), caseSensitive);
		if(mergedContent == null) return;
		showPasteReport(pastedDeclarations.isFromText(), pastedDeclarations.getIgnoredLines());
		libraries.clear();
		libraries.addAll(mergedContent.getLibraries());
		constants.clear();
		constants.addAll(mergedContent.getConstants());
		subroutines.clear();
		subroutines.addAll(mergedContent.getSubroutines());
		librariesViewer.refresh();
		constantsViewer.refresh();
		subroutinesViewer.refresh();
		updateSubroutineFields();
		validate();
	}
	
	
	/*
	 * Check libraries, names and values, OK button is enabled only if there is no error
	 */
	private void validate() {
		String errorMessage = null;
		Set<String> files = new HashSet<>();
		for (Library library : libraries) {
			if(errorMessage == null && library.getFile().isEmpty()) errorMessage = DocometreMessages.GlobalDeclarationsEmptyLibrary;
			if(errorMessage == null && !adbasic && !C_LIBRARY_PATTERN.matcher(library.getFile()).matches()) errorMessage = NLS.bind(DocometreMessages.GlobalDeclarationsInvalidLibrary, library.getFile());
			if(errorMessage == null && !files.add(getKey(library.getFile()))) errorMessage = NLS.bind(DocometreMessages.GlobalDeclarationsDuplicateLibrary, library.getFile());
		}
		Set<String> names = new HashSet<>();
		for (Constant constant : constants) {
			if(errorMessage == null) errorMessage = checkName(constant.getName(), names);
			if(errorMessage == null && constant.getValue().isEmpty()) errorMessage = NLS.bind(DocometreMessages.GlobalDeclarationsEmptyValue, constant.getName());
		}
		for (Subroutine subroutine : subroutines) {
			if(errorMessage == null) errorMessage = checkName(subroutine.getName(), names);
		}
		setErrorMessage(errorMessage);
		Button okButton = getButton(IDialogConstants.OK_ID);
		if(okButton != null) okButton.setEnabled(errorMessage == null);
	}
	
	private String checkName(String name, Set<String> names) {
		if(name.isEmpty()) return DocometreMessages.GlobalDeclarationsEmptyName;
		if(!NAME_PATTERN.matcher(name).matches()) return NLS.bind(DocometreMessages.GlobalDeclarationsInvalidName, name);
		if(reservedNames.contains(getKey(name))) return NLS.bind(DocometreMessages.GlobalDeclarationsReservedName, name);
		if(!names.add(getKey(name))) return NLS.bind(DocometreMessages.GlobalDeclarationsDuplicateName, name);
		return null;
	}
	
	/*
	 * ADBasic names are not case sensitive
	 */
	private String getKey(String name) {
		return caseSensitive ? name : name.toLowerCase(Locale.ROOT);
	}
	
	private String getUniqueName(String prefix) {
		Set<String> names = new HashSet<>();
		for (Constant constant : constants) names.add(getKey(constant.getName()));
		for (Subroutine subroutine : subroutines) names.add(getKey(subroutine.getName()));
		int i = 1;
		while(names.contains(getKey(prefix + i)) || reservedNames.contains(getKey(prefix + i))) i++;
		return prefix + i;
	}
	
	private String getUniqueLibrary() {
		Set<String> files = new HashSet<>();
		for (Library library : libraries) files.add(getKey(library.getFile()));
		int i = 1;
		while(files.contains(getKey(getLibraryFile(i)))) i++;
		return getLibraryFile(i);
	}
	
	private String getLibraryFile(int i) {
		return adbasic ? "library" + i + ".inc" : "<library" + i + ".h>";
	}
	
	private static String getColumnText(Library library, int column) {
		if(column == 0) return library.getFile();
		return library.getComment();
	}
	
	private static String getColumnText(Constant constant, int column) {
		if(column == 0) return constant.getName();
		if(column == 1) return constant.getValue();
		return constant.getComment();
	}
	
	/*
	 * Index of an element by identity : two declarations may be equal
	 */
	private static int indexOf(List<?> list, Object element) {
		for (int i = 0; i < list.size(); i++) if(list.get(i) == element) return i;
		return -1;
	}
	
	@Override
	protected void okPressed() {
		librariesViewer.applyEditorValue();
		constantsViewer.applyEditorValue();
		validate();
		if(getErrorMessage() != null) return;
		content = new Content(libraries, constants, subroutines);
		super.okPressed();
	}
	
	/**
	 * @return libraries, constants and subroutines after OK has been pressed
	 */
	public Content getContent() {
		return content;
	}

}
