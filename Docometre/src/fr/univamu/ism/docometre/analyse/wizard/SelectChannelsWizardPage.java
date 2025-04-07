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
