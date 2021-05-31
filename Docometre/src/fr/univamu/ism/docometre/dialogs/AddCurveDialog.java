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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.charts.ChartConfiguration;
import fr.univamu.ism.docometre.dacqsystems.charts.CurveConfiguration;
import fr.univamu.ism.docometre.dacqsystems.charts.MeterChartConfiguration;
import fr.univamu.ism.docometre.dacqsystems.charts.HorizontalReferenceChannel;
import fr.univamu.ism.docometre.dacqsystems.charts.OneChannelCurve;
import fr.univamu.ism.docometre.dacqsystems.charts.OscilloChartConfiguration;
import fr.univamu.ism.docometre.dacqsystems.charts.XYChartConfiguration;
import fr.univamu.ism.docometre.dacqsystems.charts.XYCurveConfiguration;
import fr.univamu.ism.docometre.dacqsystems.charts.XYCurveConfigurationProperties;

public class AddCurveDialog extends TitleAreaDialog {
	
	private DACQConfiguration dacqConfiguration;
	private ChartConfiguration chartConfiguration;
	
	private IStructuredSelection selection;

	private IStructuredSelection xSelection;
	private IStructuredSelection ySelection;

	public AddCurveDialog(Shell parentShell, DACQConfiguration dacqConfiguration, ChartConfiguration chartConfiguration) {
		super(parentShell);
		setShellStyle(getShellStyle()); 
		this.dacqConfiguration = dacqConfiguration;
		this.chartConfiguration = chartConfiguration;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(DocometreMessages.AddCurvesDialog_ShellTitle);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(DocometreMessages.AddCurvesDialog_Title);
		setMessage(DocometreMessages.AddCurvesDialog_Message);
		setTitleImage(Activator.getImageDescriptor(IImageKeys.MODULE_WIZBAN).createImage());
		Composite container = (Composite) super.createDialogArea(parent);
		// Create curve configuration depending on its type
		createCurveConfigurationArea(container);
		return container;
	}

	private void createCurveConfigurationArea(Composite parent) {
		if(chartConfiguration instanceof OscilloChartConfiguration || chartConfiguration instanceof MeterChartConfiguration) {
			// If it's an oscillo or meter chart
			ListViewer channelsListViewer = new ListViewer(parent);
			channelsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			channelsListViewer.setContentProvider(new ArrayContentProvider());
			channelsListViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((Channel)element).getProperty(ChannelProperties.NAME);
				}
			});
			channelsListViewer.setComparator(new ViewerComparator());
			// Get all transfered channels
			ArrayList<Channel> channels = new ArrayList<Channel>(0);
			if(chartConfiguration instanceof OscilloChartConfiguration) {
				channels.add(new HorizontalReferenceChannel());
				channels.addAll(Arrays.asList(dacqConfiguration.getTransferedChannels()));
			}
			if(chartConfiguration instanceof MeterChartConfiguration && chartConfiguration.getCurvesConfiguration().length == 0) 
				channels.addAll(Arrays.asList(dacqConfiguration.getTransferedChannels()));
				
			// Remove the one already used 
			CurveConfiguration[] curvesConfigurations = chartConfiguration.getCurvesConfiguration();
			for (CurveConfiguration curveConfiguration : curvesConfigurations) {
				Channel currentChannel = ((OneChannelCurve)curveConfiguration).getChannel();
				channels.remove(currentChannel);
			}
			channelsListViewer.setInput(channels.toArray(new Channel[channels.size()]));
			channelsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					selection = (IStructuredSelection) channelsListViewer.getSelection();
				}
			});
			channelsListViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					buttonPressed(IDialogConstants.OK_ID); 
				}
			});
		}
		if(chartConfiguration instanceof XYChartConfiguration) {
			Composite container = new Composite(parent, SWT.NORMAL);
			container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			container.setLayout(new GridLayout(2, true));
			
			Label xLabel = new Label(container, SWT.NORMAL);
			xLabel.setText(XYCurveConfigurationProperties.X_CHANNEL_NAME.getLabel() + " :");
			xLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Label yLabel = new Label(container, SWT.NORMAL);
			yLabel.setText(XYCurveConfigurationProperties.Y_CHANNEL_NAME.getLabel() + " :");
			yLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			ListViewer xChannelsListViewer = new ListViewer(container, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			xChannelsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			xChannelsListViewer.setContentProvider(new ArrayContentProvider());
			xChannelsListViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((Channel)element).getProperty(ChannelProperties.NAME);
				}
			});
			xChannelsListViewer.setComparator(new ViewerComparator());
			
			ListViewer yChannelsListViewer = new ListViewer(container, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			yChannelsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			yChannelsListViewer.setContentProvider(new ArrayContentProvider());
			yChannelsListViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((Channel)element).getProperty(ChannelProperties.NAME);
				}
			});
			yChannelsListViewer.setComparator(new ViewerComparator());
			
			// Get all transfered channels
			ArrayList<Channel> channels = new ArrayList<Channel>(0);
			channels.addAll(Arrays.asList(dacqConfiguration.getTransferedChannels()));
			// Remove the one already used 
			CurveConfiguration[] curvesConfigurations = chartConfiguration.getCurvesConfiguration();
			for (CurveConfiguration curveConfiguration : curvesConfigurations) {
				Channel xCurrentChannel = ((XYCurveConfiguration)curveConfiguration).getXChannel();
				Channel yCurrentChannel = ((XYCurveConfiguration)curveConfiguration).getYChannel();
				channels.remove(xCurrentChannel);
				channels.remove(yCurrentChannel);
			}
			
			xChannelsListViewer.setInput(channels.toArray(new Channel[channels.size()]));
			xChannelsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					xSelection = (IStructuredSelection) xChannelsListViewer.getSelection();
				}
			});
			xChannelsListViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					if(xSelection != null && ySelection != null)
					buttonPressed(IDialogConstants.OK_ID); 
				}
			});
			
			yChannelsListViewer.setInput(channels.toArray(new Channel[channels.size()]));
			yChannelsListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					ySelection = (IStructuredSelection) yChannelsListViewer.getSelection();
				}
			});
			yChannelsListViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					if(xSelection != null && ySelection != null)
					buttonPressed(IDialogConstants.OK_ID); 
				}
			});
		}
	}
	
	public IStructuredSelection getSelection() {
		if(chartConfiguration instanceof XYChartConfiguration) {
			selection = new StructuredSelection(new Object[] {xSelection.getFirstElement(), ySelection.getFirstElement()});
		}
		return selection;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

}
