package fr.univamu.ism.docometre.analyse.wizard;

import org.eclipse.core.text.StringMatcher;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.analyse.datamodel.Channel;

public class SelectChannelsWizardPage extends WizardPage {
	
	private Channel selectedChannel;
	private Channel[] availableChannels;
	private String filter = "";
	private ListViewer listViewer;
	private Text filterText;

	protected SelectChannelsWizardPage(String pageName, Channel[] availableChannels, String title, String message) {
		super(pageName);
		setTitle(title);
		setDescription(message);
		this.availableChannels = availableChannels;
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NORMAL);
		container.setLayout(new GridLayout());
		
		filterText = new Text(container, SWT.BORDER);
		filterText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		filterText.setFont(parent.getFont());
		filterText.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				filter = filterText.getText();
				listViewer.refresh();
				if(getNextPage() != null) ((SelectChannelsWizardPage)getNextPage()).setFilterText(filter);
			}
		});
		filterText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					listViewer.getList().setFocus();
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {
			}
		});

		LabelProvider labelProvider = new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Channel)element).getFullName();
			}
		};
		listViewer = new ListViewer(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE);
		listViewer.setContentProvider(new ArrayContentProvider());
		listViewer.setLabelProvider(labelProvider);
		listViewer.setComparator(new ViewerComparator());
		listViewer.setFilters(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if(filter == null || "".equals(filter)) return true;
				StringMatcher stringMatcher = new StringMatcher(filter, false, false);
				return stringMatcher.match(((Channel)element).getFullName());
			}
		});
		listViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		listViewer.getList().setFont(parent.getFont());
		listViewer.setInput(availableChannels);
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object[] selection = ((StructuredSelection)listViewer.getSelection()).toArray();
				selectedChannel = null;
				if(selection.length == 1) selectedChannel = (Channel) selection[0];
				getContainer().updateButtons();
			}
		});
		listViewer.getList().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				getContainer().updateButtons();
				if(getNextPage() != null) getContainer().showPage(getNextPage());
			}
		});
		setControl(container);
	}
	
	@Override
	public boolean isPageComplete() {
		return selectedChannel != null;
	}
	
	public String getFilter() {
		return filter;
	}
	
	public void setFilterText(String filter) {
		filterText.setText(filter);
	}
	
	public Channel getChannel() {
		return selectedChannel;
	}

}
