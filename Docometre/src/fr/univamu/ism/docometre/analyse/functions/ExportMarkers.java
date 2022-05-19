package fr.univamu.ism.docometre.analyse.functions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;
import fr.univamu.ism.docometre.analyse.SelectedExprimentContributionItem;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;
import fr.univamu.ism.docometre.analyse.editors.ChannelsContentProvider;
import fr.univamu.ism.docometre.dacqsystems.functions.GenericFunction;
import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.Script;

public class ExportMarkers extends GenericFunction {
	
	private static final long serialVersionUID = 1L;
	
	public static final String functionFileName = "EXPORT_MARKERS.FUN";

	private static final String markersListKey = "markersList";
	private static final String separatorKey = "separator";
	private static final String destinationKey = "destination";

	private CheckboxTableViewer markersCheckboxTableViewer;
	
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
		separatorCombo.setText(getProperty(separatorKey, ";"));
		separatorCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(separatorKey, separatorCombo.getText());
			}
		});
		
		Composite destinationFolderContainer = new Composite(paramContainer, SWT.NORMAL);
		destinationFolderContainer.setLayout(new GridLayout(3, false));
		destinationFolderContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Label destinationFolderLabel = new Label(destinationFolderContainer, SWT.NORMAL);
		destinationFolderLabel.setText("Desitnation : ");
		destinationFolderLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		Text destinationFolderText = new Text(destinationFolderContainer, SWT.BORDER | SWT.READ_ONLY);
		destinationFolderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		destinationFolderText.setText(getProperty(destinationKey, ""));
		destinationFolderText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getTransientProperties().put(destinationKey, destinationFolderText.getText());
			}
		});
		
		Button destinationFolderButton = new Button(destinationFolderContainer, SWT.FLAT);
		destinationFolderButton.setText(DocometreMessages.Browse);
		destinationFolderButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		destinationFolderButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				String response = dd.open();
				if(response != null) {
					destinationFolderText.setText(response);
				}
			}
		});
		
		markersCheckboxTableViewer.setInput(SelectedExprimentContributionItem.selectedExperiment);
		filterText.setText(".*");
		String markersList = getProperty(markersListKey, "");
		String[] markers = markersList.split(";");
		TableItem[] tableItems = markersCheckboxTableViewer.getTable().getItems();
		for (TableItem tableItem : tableItems) {
			for (String marker : markers) {
				boolean checked = tableItem.getText().equals(marker);
				tableItem.setChecked(checked);
				if(checked) break;
			}
		}
		markersCheckboxTableViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
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
		
		boolean execute = true;
		if(objects == null) execute = false;
		if(objects[0] == null) execute = false;
		if(!(objects[0] instanceof IResource)) execute = false;
		if(!ResourceType.isSubject(((IResource)objects[0]))) execute = false;
		
		if(!execute) {
			if(MathEngineFactory.isMatlab()) return "% Error in ExportMarkers function : subject is not provide";
			if(MathEngineFactory.isPython()) return "# Error in ExportMarkers function : subject is not provide";
		}
		
		IResource currentSubject = (IResource)objects[0];
		String projectNameRegExp = currentSubject.getProject().getName();
		String subjectNameRegExp = currentSubject.getName();
		String replaceRegExp = projectNameRegExp + "\\.\\w+\\.";
		String replaceByRegExp = projectNameRegExp + "." + subjectNameRegExp +".";
		
		String code = "";

		String markersList = getProperty(markersListKey, "");
		String separator = getProperty(separatorKey, ";");
		String destination = getProperty(destinationKey, ";") + File.separator;
		
		if(MathEngineFactory.isPython()) {
			if(!"".equals(markersList)) {
				IContainer experiment = (IContainer) SelectedExprimentContributionItem.selectedExperiment;
				String[] markers = markersList.split(";");
				Set<String> filesNames = new HashSet<>();
				for (String marker : markers) {
					String[] segments = marker.split("\\.");
					String fileName = destination + segments[0] + "." + segments[1] + ".markers.csv";
					filesNames.add(fileName);
				}
				code = code + "import os;\n";
				code = code + "import numpy;\n";
				code = code + "# Delete existing files\n";
				for (String fileName : filesNames) {
					fileName = fileName.replaceAll(replaceRegExp, replaceByRegExp);
					code = code + "if os.path.exists('" + fileName + "'):\n";
					code = code + "\tos.remove('" + fileName + "');\n";
				}
				code = code + "# Create and add categories to files\n";
				for (String fileName : filesNames) {
					fileName = fileName.replaceAll(replaceRegExp, replaceByRegExp);
					code = code + "with open('" + fileName + "', 'w', encoding='utf-8') as dataFile:\n";
					String[] fileNameSplitted = fileName.split("\\.");
					String subjectName = fileNameSplitted[fileNameSplitted.length - 3];
					IResource subject = experiment.findMember(subjectName);
					Channel[] categories = MathEngineFactory.getMathEngine().getCategories(subject);
					for (Channel channel : categories) {
						String criteria = MathEngineFactory.getMathEngine().getCriteriaForCategory(channel);
						Integer[] trialsList = MathEngineFactory.getMathEngine().getTrialsListForCategory(channel);
						int[] trialsListInt = Arrays.asList(trialsList).stream().mapToInt(Integer::intValue).toArray();
						String[] strArray = Arrays.stream(trialsListInt).mapToObj(String::valueOf).toArray(String[]::new);
						String trialsListString = String.join(";", strArray);
						code = code + "\tdataFile.write('" + criteria + ";" + trialsListString + "');\n";
						code = code + "\tdataFile.write('\\n');\n";
					}
				}
				code = code + "# Append data\n";
				for (String marker : markers) {
					marker = marker.replaceAll(replaceRegExp, replaceByRegExp);
					String[] segments = marker.split("\\.");
					String fileName = destination + segments[0] + "." + segments[1] + ".markers.csv";
					//Channel channel = MathEngineFactory.getMathEngine().getChannelFromName(experiment, marker);
					String[] markerSplitted = marker.split("_");
					code = code + "# " + markerSplitted[markerSplitted.length - 1] + "\n";
					code = code + "with open('" + fileName + "', 'a', encoding='utf-8') as dataFile:\n";
					code = code + "\tdataFile.write('MarkerLabel;" + markerSplitted[markerSplitted.length - 1] + "');\n";
					code = code + "\tdataFile.write('\\n');\n";
					code = code + "\tnumpy.savetxt(dataFile, docometre.experiments['" + marker + "_Values'], delimiter=\"" + separator + "\")" + ";\n";
				}
			} else code = "# Nothing to export !";
		}
		
		if(MathEngineFactory.isMatlab()) {
			if(!"".equals(markersList)) {
				IContainer experiment = (IContainer) SelectedExprimentContributionItem.selectedExperiment;
				String[] markers = markersList.split(";");
				Set<String> filesNames = new HashSet<>();
				for (String marker : markers) {
					String[] segments = marker.split("\\.");
					String fileName = destination + segments[0] + "." + segments[1] + ".markers.csv";
					filesNames.add(fileName);
				}
				code = code + "% Create and add categories to files (delete files if exists)\n";
				for (String fileName : filesNames) {
					fileName = fileName.replaceAll(replaceRegExp, replaceByRegExp);
					code = code + "dataFile = fopen('" + fileName + "','w' , 'n', 'UTF-8');\n";
					String[] fileNameSplitted = fileName.split("\\.");
					String subjectName = fileNameSplitted[fileNameSplitted.length - 3];
					IResource subject = experiment.findMember(subjectName);
					Channel[] categories = MathEngineFactory.getMathEngine().getCategories(subject);
					for (Channel channel : categories) {
						String criteria = MathEngineFactory.getMathEngine().getCriteriaForCategory(channel);
						Integer[] trialsList = MathEngineFactory.getMathEngine().getTrialsListForCategory(channel);
						int[] trialsListInt = Arrays.asList(trialsList).stream().mapToInt(Integer::intValue).toArray();
						String[] strArray = Arrays.stream(trialsListInt).mapToObj(String::valueOf).toArray(String[]::new);
						String trialsListString = String.join(";", strArray);
						code = code + "fprintf(dataFile, '" + criteria + ";" + trialsListString + "');\n";
						code = code + "fprintf(dataFile, '\\n');\n";
					}
					code = code + "fclose(dataFile);\n";
				}
				code = code + "% Append data\n";
				for (String marker : markers) {
					marker = marker.replaceAll(replaceRegExp, replaceByRegExp);
					String[] segments = marker.split("\\.");
					String fileName = destination + segments[0] + "." + segments[1] + ".markers.csv";
					String[] markerSplitted = marker.split("_");
					code = code + "% " + markerSplitted[markerSplitted.length - 1] + "\n";
					code = code + "dataFile = fopen('" + fileName + "', 'a', 'n', 'UTF-8');\n";
					code = code + "fprintf(dataFile, 'MarkerLabel;" + markerSplitted[markerSplitted.length - 1] + "');\n";
					code = code + "fprintf(dataFile, '\\n');\n";
					code = code + "fclose(dataFile);\n";
					code = code + "dlmwrite('" + fileName + "', " + marker + "_Values ,'delimiter', ';', '-append');\n";
				}
			} else code = "% Nothing to export !";
		} 
		return code + "\n";
	}
	
	@Override
	public Block clone() {
		ExportMarkers function = new ExportMarkers();
		super.clone(function);
		return function;
	}

}
