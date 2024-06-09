package fr.univamu.ism.docometre.analyse.functions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.editors.ChannelsContentProvider;
import fr.univamu.ism.docometre.dacqsystems.functions.GenericFunction;
import fr.univamu.ism.docometre.dialogs.FunctionalBlockConfigurationDialog;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Script;

public class ExportMarkers extends GenericFunction {
	
	private class RelativePathSelectionDialog extends Dialog {
		
		IResource selectedDestination;

		protected RelativePathSelectionDialog(Shell parentShell, String initialSelection) {
			super(parentShell);
			selectedDestination = ResourcesPlugin.getWorkspace().getRoot().findMember(initialSelection);
		}
		
		public IResource getSelectedDestination() {
			return selectedDestination;
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			Composite container = (Composite) super.createDialogArea(parent);
			TreeViewer foldersTreeViewer = new TreeViewer(container, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			foldersTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			foldersTreeViewer.setContentProvider(new ITreeContentProvider() {
				@Override
				public boolean hasChildren(Object element) {
					try {
						if(!(element instanceof IContainer)) return false;
						IResource[] members = ((IContainer)element).members();
						for (IResource member : members) {
							if(ResourceType.isExperiment(member) || ResourceType.isFolder(member)) return true;
						}
					} catch (CoreException e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
					}
					return false;
				}
				@Override
				public Object getParent(Object element) {
					if(element instanceof IResource) return ((IResource)element).getParent();
					return null;
				}
				@Override
				public Object[] getElements(Object inputElement) {
					try {
						ArrayList<IContainer> elements = new ArrayList<IContainer>();
						if(inputElement == ResourcesPlugin.getWorkspace().getRoot()) {
							IResource[] members = ResourcesPlugin.getWorkspace().getRoot().members();
							for (IResource member : members) {
								if(ResourceType.isExperiment(member) || ResourceType.isFolder(member)) elements.add((IContainer) member);
							}
						}
						return elements.toArray();
					} catch (CoreException e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
					}
					return new Object[0];
				}
				@Override
				public Object[] getChildren(Object parentElement) {
					try {
						ArrayList<IContainer> elements = new ArrayList<IContainer>();
						if(parentElement instanceof IContainer) {
							IResource[] members = ((IContainer)parentElement).members();
							for (IResource member : members) {
								if(ResourceType.isExperiment(member) || ResourceType.isFolder(member)) elements.add((IContainer) member);
							}
						}
						return elements.toArray();
					} catch (CoreException e) {
						Activator.logErrorMessageWithCause(e);
						e.printStackTrace();
					}
					return new Object[0];
				}
			});
			foldersTreeViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((IContainer)element).getName();
				}
			});
			foldersTreeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
			foldersTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					buttonPressed(OK);
				}
			});
			foldersTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					selectedDestination = (IResource) event.getStructuredSelection().getFirstElement();
				}
			});
			if(selectedDestination != null) foldersTreeViewer.setSelection(new StructuredSelection(selectedDestination));
			
			Label messageLabel = new Label(container, SWT.NORMAL);
			messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			messageLabel.setText(FunctionsMessages.DialogMessage);
			
			return container;
		}
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(FunctionsMessages.DialogTitle);
		}
		
		@Override
		protected boolean isResizable() {
			return true;
		}
		
	}
	
	private static final long serialVersionUID = 1L;
	
	public static final String functionFileName = "EXPORT_MARKERS.FUN";

	private static final String markersListKey = "markersList";
	private static final String dividerKey = "divider";
	private static final String destinationKey = "destination";
	private static final String fullPathFileNameKey = "fullPathFileName";
	private static final String fullSubjectNameKey = "fullSubjectName";
	private static final String cellExpressionKey = "cellExpression";
	private static final String relativePathKey = "relativePath";

	transient private CheckboxTableViewer markersCheckboxTableViewer;
	
	@Override
	public String getFunctionFileName() {
		return functionFileName;
	}
	
	@Override
	public Object getGUI(Object titleAreaDialog, Object parent, Object context) {
		if(!(context instanceof Script)) return null;
		
		Composite container  = (Composite) parent;
		Composite paramContainer = new Composite(container, SWT.NORMAL);
		paramContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		paramContainer.setLayout(new GridLayout());
		
		if(!checkPreBuildGUI((FunctionalBlockConfigurationDialog)titleAreaDialog, paramContainer, 2, context)) {
			return paramContainer;
		}
		
		Composite filterContainer = new Composite(paramContainer, SWT.NORMAL);
		filterContainer.setLayout(new GridLayout(2, false));
		filterContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label filterLabel = new Label(filterContainer, SWT.NORMAL);
		filterLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		filterLabel.setText(FunctionsMessages.FilterLabel);
		
		Text filterText = new Text(filterContainer, SWT.BORDER);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		markersCheckboxTableViewer = CheckboxTableViewer.newCheckList(paramContainer, SWT.BORDER);
		markersCheckboxTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		markersCheckboxTableViewer.setContentProvider(new ChannelsContentProvider(false, false, false, true, false, false, false));
		markersCheckboxTableViewer.setLabelProvider(FunctionsHelper.createTextProvider());
		markersCheckboxTableViewer.setComparator(new ViewerComparator());
		markersCheckboxTableViewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return Pattern.matches(filterText.getText(), ((Channel)element).getFullName());
			}
		});
		
		filterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				markersCheckboxTableViewer.refresh();
			}
		});
		
		Composite buttonsContainer = new Composite(paramContainer, SWT.NORMAL);
		buttonsContainer.setLayout(new GridLayout(2, true));
		buttonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Button unselectAllButton = new Button(buttonsContainer, SWT.FLAT);
		unselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		unselectAllButton.setText(FunctionsMessages.UnselectAll);
		unselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				markersCheckboxTableViewer.setAllChecked(false);
				TableItem[] items = markersCheckboxTableViewer.getTable().getItems();
				for (TableItem tableItem : items) {
					Event event = new Event();
					event.widget = markersCheckboxTableViewer.getTable();
					event.item = tableItem;
					event.doit = true;
					event.detail = SWT.CHECK;
					SelectionEvent selectionEvent = new SelectionEvent(event);
					markersCheckboxTableViewer.handleSelect(selectionEvent);
				}
				
			}
		});
		
		Button selectAllButton = new Button(buttonsContainer, SWT.FLAT);
		selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		selectAllButton.setText(FunctionsMessages.SelectAll);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				markersCheckboxTableViewer.setAllChecked(true);
				TableItem[] items = markersCheckboxTableViewer.getTable().getItems();
				for (TableItem tableItem : items) {
					Event event = new Event();
					event.widget = markersCheckboxTableViewer.getTable();
					event.item = tableItem;
					event.doit = true;
					event.detail = SWT.CHECK;
					SelectionEvent selectionEvent = new SelectionEvent(event);
					markersCheckboxTableViewer.handleSelect(selectionEvent);
				}
			}
		});
		
		Composite separatorContainer = new Composite(paramContainer, SWT.NORMAL);
		separatorContainer.setLayout(new GridLayout(2, false));
		separatorContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label separatorLabel = new Label(separatorContainer, SWT.NORMAL);
		separatorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		separatorLabel.setText(FunctionsMessages.SeparatorLabel);
		
		Combo separatorCombo = new Combo(separatorContainer, SWT.BORDER);
		separatorCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		separatorCombo.setItems(new String[] {";", ",", ":", "/", "-"});
		separatorCombo.setText(getProperty(dividerKey, ";"));
		separatorCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(dividerKey, separatorCombo.getText());
			}
		});
		
		Group group = new Group(paramContainer, SWT.NONE);
		group.setText(FunctionsMessages.Destination);
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group.setLayout(new GridLayout(2, false));
		
		Composite buttonsContainer2 = new Composite(group, SWT.NORMAL);
		buttonsContainer2.setLayout(new GridLayout(2, true));
		buttonsContainer2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		boolean relativePath = getProperty(relativePathKey, "true").equals("true") ? true:false;
		Button workspaceButton = new Button(buttonsContainer2, SWT.RADIO);
		workspaceButton.setText(FunctionsMessages.Relative);
		workspaceButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		workspaceButton.setSelection(relativePath);
		workspaceButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getTransientProperties().put(relativePathKey, workspaceButton.getSelection()?"true":"false");
			}
		});
		Button fileSystemButton = new Button(buttonsContainer2, SWT.RADIO);
		fileSystemButton.setText(FunctionsMessages.Absolute);
		fileSystemButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fileSystemButton.setSelection(!relativePath);
		fileSystemButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getTransientProperties().put(relativePathKey, fileSystemButton.getSelection()?"false":"true");
			}
		});
		
		Text destinationFolderText = new Text(group, SWT.BORDER | SWT.READ_ONLY);
		destinationFolderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		destinationFolderText.setText(getProperty(destinationKey, ""));
		destinationFolderText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(destinationKey, destinationFolderText.getText());
			}
		});
		
		Button destinationFolderButton = new Button(group, SWT.FLAT);
		destinationFolderButton.setText(DocometreMessages.Browse);
		destinationFolderButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		destinationFolderButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean relativePath = workspaceButton.getSelection();
				if(!relativePath) {
					DirectoryDialog dd = new DirectoryDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
					dd.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getFullPath().toPortableString());
					String response = dd.open();
					if(response != null) {
						destinationFolderText.setText(response);
					}
				} else {
					RelativePathSelectionDialog relativePathSelectionDialog = new RelativePathSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), destinationFolderText.getText());
					if(relativePathSelectionDialog.open() == Window.OK) {
						IResource selectedDestination = relativePathSelectionDialog.getSelectedDestination();
						destinationFolderText.setText(selectedDestination.getFullPath().toPortableString());
					}
				}
			}
		});
		
		markersCheckboxTableViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		filterText.setText(".*");
		String markersList = getProperty(markersListKey, "");
		String[] markers = markersList.split(";");
		TableItem[] tableItems = markersCheckboxTableViewer.getTable().getItems();
		for (String marker : markers) {
			boolean checked = false;
			for (TableItem tableItem : tableItems) {
				checked = tableItem.getText().equals(marker);
				if(checked) {
					tableItem.setChecked(checked);
					break;
				}
			}
			if(!checked) {
				String message = NLS.bind(DocometreMessages.ImpossibleToFindChannelTitle, marker);
				Activator.logErrorMessage(message);
				((FunctionalBlockConfigurationDialog)titleAreaDialog).setErrorMessage(DocometreMessages.FunctionalBlockConfigurationDialogBlockingMessage);
			}
		}
		markersCheckboxTableViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				((FunctionalBlockConfigurationDialog)titleAreaDialog).setErrorMessage(null);
				Object[] elements = markersCheckboxTableViewer.getCheckedElements();
				ArrayList<String> stringElements = new ArrayList<>();
				for (Object element : elements) {
					stringElements.add(((Channel)element).getFullName());
				}
				String value = String.join(";", stringElements);
				getTransientProperties().put(markersListKey, value);
			}
		});
		
		return paramContainer;
	}
	
	@Override
	public String getCode(Object context, Object step, Object...objects) {
		if(!isActivated()) return GenericFunction.getCommentedCode(this, context);
		
		String code = FunctionFactory.getProperty(context, functionFileName, FUNCTION_CODE);
		
		String markersList = getProperty(markersListKey, "");
		String divider = getProperty(dividerKey, ";");
		String destination = getProperty(destinationKey, "") + File.separator;
		boolean relativePath = getProperty(relativePathKey, "true").equals("true") ? true:false;
		if(relativePath) destination = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString() + destination + "/";
		
		String[] markers = markersList.split(";");
		Set<String> filesNames = new HashSet<>();
		Set<String> subjectsNames = new HashSet<>();
		for (String marker : markers) {
			if(!"".equals(marker)) {
				String[] segments = marker.split("\\.");
				String fileName = destination + segments[0] + "." + segments[1] + ".markers.csv";
				filesNames.add(fileName);
				subjectsNames.add(segments[0] + "." + segments[1]);
			}
		}
		
		if(filesNames.size() == 0) {
			if(MathEngineFactory.isMatlab()) return "% Nothing to export !";
			if(MathEngineFactory.isPython()) return "# Nothing to export !";
		}
		
		if(filesNames.size() > 1) {
			if(MathEngineFactory.isMatlab()) return "% Export error : cannot export more than one subject ! " + String.join(", ", markers);
			if(MathEngineFactory.isPython()) return "# Export error : cannot export more than one subject ! " + String.join(", ", markers);
		}
		
		String fileName = filesNames.toArray(new String[filesNames.size()])[0];
		String fullSubjectName = subjectsNames.toArray(new String[subjectsNames.size()])[0];
		
		String cellExpression = "";
		if(MathEngineFactory.isMatlab()) cellExpression = "{'" + String.join("','", markers) + "'}";
		if(MathEngineFactory.isPython()) cellExpression = "['" + String.join("','", markers) + "']";
		
		code  = code.replaceAll(fullPathFileNameKey, Matcher.quoteReplacement(fileName));
		code  = code.replaceAll(fullSubjectNameKey, fullSubjectName);
		code  = code.replaceAll(cellExpressionKey, cellExpression);
		code  = code.replaceAll(dividerKey, divider);
		
		return code + "\n";
	}
	
	@Override
	public Block clone() {
		ExportMarkers function = new ExportMarkers();
		super.clone(function);
		return function;
	}

}
