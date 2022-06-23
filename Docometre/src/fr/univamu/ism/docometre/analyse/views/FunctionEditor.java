package fr.univamu.ism.docometre.analyse.views;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinDACQConfigurationProperties;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoDACQConfigurationProperties;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class FunctionEditor extends ApplicationWindow {
	
	private static String FUNCTIONS_EDITOR_WIDTH = "FUNCTIONS_EDITOR_WIDTH";
	private static String FUNCTIONS_EDITOR_HEIGHT = "FUNCTIONS_EDITOR_HEIGHT";
	private static String FUNCTIONS_EDITOR_TOP = "FUNCTIONS_EDITOR_TOP";
	private static String FUNCTIONS_EDITOR_LEFT = "FUNCTIONS_EDITOR_LEFT";
	
	private CTabFolder tabFolder;
	private TreeViewer functionsTreeViewer;
	private SashForm sashForm;
	private CTabFolder functionsTabFolder;

	public FunctionEditor(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected void configureShell(Shell shell) {
		// SWT.SHADOW_OUT SWT.VERTICAL SWT.RIGHT
		addToolBar(SWT.FLAT | SWT.WRAP | SWT.HORIZONTAL);
		addMenuBar();
		addStatusLine();
		super.configureShell(shell);
		setStatus("Hello !");
	}
	
	@Override
	protected void initializeBounds() {
		super.initializeBounds();
		IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		int width = preferenceStore.getInt(FUNCTIONS_EDITOR_WIDTH);
		int height = preferenceStore.getInt(FUNCTIONS_EDITOR_HEIGHT);
		int top = preferenceStore.getInt(FUNCTIONS_EDITOR_TOP);
		int left = preferenceStore.getInt(FUNCTIONS_EDITOR_LEFT);
		if(width == 0 || height == 0) getShell().setMaximized(true);
		else getShell().setBounds(left, top, width, height);
		getShell().addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
				preferenceStore.setValue(FUNCTIONS_EDITOR_WIDTH, FunctionEditor.this.getShell().getBounds().width);
				preferenceStore.setValue(FUNCTIONS_EDITOR_HEIGHT, FunctionEditor.this.getShell().getBounds().height);
			}
		});
		getShell().addListener(SWT.Move, new Listener() {
			@Override
			public void handleEvent(Event event) {
				IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
				preferenceStore.setValue(FUNCTIONS_EDITOR_TOP, FunctionEditor.this.getShell().getBounds().y);
				preferenceStore.setValue(FUNCTIONS_EDITOR_LEFT, FunctionEditor.this.getShell().getBounds().x);
			}
		});
	}
	
	@Override
	protected Control createContents(Composite parent) {
		getShell().setText("DOCoMETRe " + Activator.getDefault().getVersion() + " - " + DocometreMessages.FuntionEditorShellTitle);
		
		Composite container = new Composite(parent, SWT.NONE);
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = 2;
		fillLayout.marginWidth = 2;
		container.setLayout(fillLayout);
		
		sashForm = new SashForm(container, SWT.HORIZONTAL);
		
		functionsTabFolder = new CTabFolder(sashForm, SWT.TOP | SWT.FLAT);
		functionsTabFolder.setMaximizeVisible(true);
		functionsTabFolder.setMinimizeVisible(false);
		functionsTabFolder.setBorderVisible(true);
		functionsTabFolder.setSimple(false);
		functionsTabFolder.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if(functionsTabFolder.getMaximized()) FunctionEditor.this.restoreFunctions();
				else FunctionEditor.this.maximizeFunctions();
			}
		});
		functionsTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			@Override
			public void maximize(CTabFolderEvent event) {
				FunctionEditor.this.maximizeFunctions();
			}
			@Override
			public void restore(CTabFolderEvent event) {
				FunctionEditor.this.restoreFunctions();
			}
		});
		ColorRegistry reg = JFaceResources.getColorRegistry();
		Color c1 = reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_START"); //$NON-NLS-1$
		Color c2 = reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_END"); //$NON-NLS-1$
		functionsTabFolder.setSelectionBackground(new Color[] { c1, c2 }, new int[] { 100 }, true);
		functionsTabFolder.setSelectionForeground(reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_TEXT_COLOR")); //$NON-NLS-1$
		CTabItem functionsTabItem = new CTabItem(functionsTabFolder, SWT.NONE);
		functionsTabItem.setText(DocometreMessages.FunctionsListTitle);
		functionsTabFolder.setSelection(functionsTabItem);
		
		functionsTreeViewer = new TreeViewer(functionsTabFolder, SWT.BORDER);
		functionsTreeViewer.setContentProvider(new FunctionsEditorTreeContentProvider());
		ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
		functionsTreeViewer.setLabelProvider(new DecoratingLabelProvider(new FunctionsEditorLabelProvider(), decorator));
		functionsTreeViewer.setComparator(new ViewerComparator());
		functionsTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ITreeSelection treeSelection = (ITreeSelection) event.getSelection();
				if(treeSelection.getFirstElement() instanceof Path) {
					Path path = (Path) treeSelection.getFirstElement();
					CTabItem tabItem = new CTabItem(tabFolder, SWT.CLOSE);
					
					StringBuffer content = new StringBuffer();
					try {
						List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(path.toString()), StandardCharsets.UTF_8);
						int i = 0;
						for (String line : lines) {
							line  = line.replaceAll("\\\\n\\\\", "");
							line  = line.replaceAll("\\\\t", "");
							content.append(line);
							if(i != lines.size() - 1) content.append("\n");
							i++;
						}
					} catch (IOException e1) {
						Activator.getLogErrorMessageWithCause(e1);
						e1.printStackTrace();
					}
					
					Document document = new Document();
					CompositeRuler lineAnnotationRuler = new CompositeRuler();
					lineAnnotationRuler.addDecorator(0,new LineNumberRulerColumn());
					SourceViewer functionSourceViewer = new SourceViewer(tabFolder, lineAnnotationRuler, SWT.V_SCROLL | SWT.H_SCROLL);
					functionSourceViewer.setEditable(false);
					
					functionSourceViewer.setDocument(document);
					document.set(content.toString());
					
					
					
					tabItem.setText(functionsTreeViewer.getTree().getSelection()[0].getText());
					tabItem.setToolTipText(path.toString());
					tabItem.addDisposeListener(new DisposeListener() {
						@Override
						public void widgetDisposed(DisposeEvent e) {
							if(tabFolder.getItemCount() == 0 && tabFolder.getMaximized()) FunctionEditor.this.restoreEditor();   
						}
					});
					tabFolder.setSelection(tabItem);
					tabItem.setControl(functionSourceViewer.getControl());
					tabFolder.setFocus();
				} else {
					Object element = treeSelection.getFirstElement();
					functionsTreeViewer.setExpandedState(element, !functionsTreeViewer.getExpandedState(element));
				}
			}
		});
		functionsTabItem.setControl(functionsTreeViewer.getTree());
		
		tabFolder = new CTabFolder(sashForm, SWT.TOP | SWT.FLAT);
		tabFolder.setSimple(false);
		 c1 = reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_START"); //$NON-NLS-1$
		 c2 = reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_END"); //$NON-NLS-1$
		tabFolder.setSelectionBackground(new Color[] { c1, c2 }, new int[] { 100 }, true);
		tabFolder.setSelectionForeground(reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_TEXT_COLOR")); //$NON-NLS-1$
		tabFolder.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if(tabFolder.getMaximized()) FunctionEditor.this.restoreEditor();
				else FunctionEditor.this.maximizeEditor();
			}
		});
		tabFolder.setBorderVisible(true);
		tabFolder.setMaximizeVisible(true);
		tabFolder.setMinimizeVisible(false);
		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			@Override
			public void maximize(CTabFolderEvent event) {
				FunctionEditor.this.maximizeEditor();
			}
			@Override
			public void restore(CTabFolderEvent event) {
				FunctionEditor.this.restoreEditor();
			}
		});
		
		functionsTreeViewer.setInput(createModel());

        sashForm.setSashWidth(4);
		sashForm.setWeights(new int[] {25,75});
		
		return container;
	}
	
	private void maximizeFunctions() {
		functionsTabFolder.setMaximized(true);
		tabFolder.setVisible(false);
		sashForm.requestLayout();
	}
	
	private void restoreFunctions() {
		if(functionsTabFolder.getMaximized()) {
			functionsTabFolder.setMaximized(false);
			tabFolder.setVisible(true);
			sashForm.requestLayout();
		}
	}
	
	private void maximizeEditor() {
		tabFolder.setMaximized(true);
		functionsTabFolder.setVisible(false);
		sashForm.requestLayout();
	}
	
	private void restoreEditor() {
		if(tabFolder.getMaximized()) {
			tabFolder.setMaximized(false);
			functionsTabFolder.setVisible(true);
			sashForm.requestLayout();
		}
	}
	
	private Object createModel() {
		String matlabScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATLAB_SCRIPTS_LOCATION);
		String matlabUserScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.MATLAB_USER_SCRIPTS_LOCATION);
		
		String pythonScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.PYTHON_SCRIPTS_LOCATION);
		String pythonUserScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.PYTHON_USER_SCRIPTS_LOCATION);
		
		String adwinScriptLocation = Activator.getDefault().getPreferenceStore().getString(ADWinDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey());
		String adwinUserScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.ADWIN_USER_LIBRARIES_ABSOLUTE_PATH);
		
		String arduinoUnoScriptLocation = Activator.getDefault().getPreferenceStore().getString(ArduinoUnoDACQConfigurationProperties.LIBRARIES_ABSOLUTE_PATH.getKey());
		String arduinoUnoUserScriptLocation = Activator.getDefault().getPreferenceStore().getString(GeneralPreferenceConstants.ARDUINO_USER_LIBRARIES_ABSOLUTE_PATH);

		BidiMap<String, Path> functionsHashMap = new DualHashBidiMap<>();
		
		if(Path.of(adwinUserScriptLocation).toFile().exists())
		functionsHashMap.put(DocometreMessages.ADwinUserFunctions, Path.of(adwinUserScriptLocation));
		functionsHashMap.put(DocometreMessages.ADwinFunctions, Path.of(adwinScriptLocation).getParent().resolve("ADWinFunctions"));
		
		if(Path.of(arduinoUnoUserScriptLocation).toFile().exists())
		functionsHashMap.put(DocometreMessages.ArduinoUnoUserFunctions, Path.of(arduinoUnoUserScriptLocation));
		functionsHashMap.put(DocometreMessages.ArduinoUnoFunctions, Path.of(arduinoUnoScriptLocation).getParent().resolve("ArduinoUnoFunctions"));
		
		if(Path.of(pythonUserScriptLocation).toFile().exists())
		functionsHashMap.put(DocometreMessages.PythonUserFunctions, Path.of(pythonUserScriptLocation));
		functionsHashMap.put(DocometreMessages.PythonFunctions, Path.of(pythonScriptLocation).getParent().resolve("PythonFunctions"));
		
		if(Path.of(matlabUserScriptLocation).toFile().exists())
		functionsHashMap.put(DocometreMessages.MatlabUserFunctions, Path.of(matlabUserScriptLocation));
		functionsHashMap.put(DocometreMessages.MatlabFunctions, Path.of(matlabScriptLocation).getParent().resolve("MatlabFunctions"));
		
		return functionsHashMap;
	}

	@Override
	public boolean close() {
		// TODO ...
		return super.close();
	}

}
