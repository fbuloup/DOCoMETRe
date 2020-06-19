package fr.univamu.ism.docometre.analyse.editors;

import java.util.stream.IntStream;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class SignalContainerEditor extends Composite implements ISelectionChangedListener {
	
	private static String[] graphicalSymbols = new String[] {"\u25A1", "\u25C7", "\u25B3", "\u25CB", "\u2606", "+"};
	
	private ChannelEditor channelEditor;
	private ListViewer trialsListViewer;

	public SignalContainerEditor(Composite parent, int style, ChannelEditor channelEditor) {
		super(parent, style);
		this.channelEditor = channelEditor;
		setLayout(new GridLayout(4, true));
		GridLayout gl = (GridLayout)getLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 5;
		
		ChannelEditorWidgetsFactory.createChart(this, 3);
		
		trialsListViewer = new ListViewer(this, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		trialsListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		int nbTrials = MathEngineFactory.getMathEngine().getTrialsNumber(channelEditor.getchannel());
		Integer[] trials = IntStream.rangeClosed(1, nbTrials).boxed().toArray(Integer[]::new);
		trialsListViewer.setContentProvider(new ArrayContentProvider());
		trialsListViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String trial = super.getText(element);
				return DocometreMessages.Trial + trial;
			}
		});
		trialsListViewer.setInput(trials);
		trialsListViewer.addSelectionChangedListener(this);
		
		ChannelEditorWidgetsFactory.createSeparator(this, true, false, 4, SWT.HORIZONTAL);
		createGeneralInfoGroup();
		createTrialsGroup();
		createFieldsGroup();
		createMarkersGroup();
	}
	
	private void createMarkersGroup() {
		Group markersGroupsGroup = ChannelEditorWidgetsFactory.createGroup(this, DocometreMessages.MarkersGroupTitle);
		markersGroupsGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.GroupNameLabel, SWT.LEFT, false);
		Composite deleteMarkersGroupContainer = new Composite(markersGroupsGroup, SWT.NORMAL);
		deleteMarkersGroupContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		deleteMarkersGroupContainer.setLayout(gl);
		ChannelEditorWidgetsFactory.createCombo(deleteMarkersGroupContainer, SWT.FILL, true);
		Button deleteMarkersGroupButton = new Button(deleteMarkersGroupContainer, SWT.FLAT);
		deleteMarkersGroupButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		deleteMarkersGroupButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		deleteMarkersGroupButton.setToolTipText(DocometreMessages.DeleteSelectedMarkersGroupTooltip);
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.TrialNumberLabel, SWT.LEFT, false);
		Composite deleteMarkerTrialContainer = new Composite(markersGroupsGroup, SWT.NORMAL);
		deleteMarkerTrialContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout gl2 = new GridLayout(2, false);
		gl2.horizontalSpacing = 0;
		gl2.verticalSpacing = 0;
		gl2.marginHeight = 0;
		gl2.marginWidth = 0;
		deleteMarkerTrialContainer.setLayout(gl2);
		ChannelEditorWidgetsFactory.createSpinner(deleteMarkerTrialContainer, SWT.FILL, true);
		Button deleteMarkerTrialButton = new Button(deleteMarkerTrialContainer, SWT.FLAT);
		deleteMarkerTrialButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		deleteMarkerTrialButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		deleteMarkerTrialButton.setToolTipText(DocometreMessages.DeleteSelectedMarkerTrialTooltip);
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.GraphicalSymbolLabel, SWT.LEFT, false);
		ComboViewer graphicalComboViewer = ChannelEditorWidgetsFactory.createCombo(markersGroupsGroup, SWT.FILL, true);
		graphicalComboViewer.setContentProvider(new ArrayContentProvider());
		graphicalComboViewer.setLabelProvider(new LabelProvider());
		graphicalComboViewer.setInput(graphicalSymbols);
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.XMarkerValueLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.NotAvailable_Label, SWT.LEFT, true);
		
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.YMarkerValueLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(markersGroupsGroup, DocometreMessages.NotAvailable_Label, SWT.LEFT, true);
	}
	
	private void createFieldsGroup() {
		Group fieldsGroup = ChannelEditorWidgetsFactory.createGroup(this, DocometreMessages.FieldsGroupTitle);
		fieldsGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(fieldsGroup, DocometreMessages.FieldsNameLabel, SWT.LEFT, false);
		Composite deletFieldContainer = new Composite(fieldsGroup, SWT.NORMAL);
		deletFieldContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		deletFieldContainer.setLayout(gl);
		ChannelEditorWidgetsFactory.createCombo(deletFieldContainer, SWT.FILL, true);
		Button deleteFieldButton = new Button(deletFieldContainer, SWT.FLAT);
		deleteFieldButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		deleteFieldButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		deleteFieldButton.setToolTipText(DocometreMessages.DeleteSelectedFieldTooltip);
		
		ChannelEditorWidgetsFactory.createLabel(fieldsGroup, DocometreMessages.TrialNumberLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createSpinner(fieldsGroup, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(fieldsGroup, DocometreMessages.ValueLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(fieldsGroup, DocometreMessages.NotAvailable_Label, SWT.LEFT, true);
	}

	private void createTrialsGroup() {
		Group trialsGroup = ChannelEditorWidgetsFactory.createGroup(this, DocometreMessages.TrialsGroupLabel);
		trialsGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.Trial, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createSpinner(trialsGroup, SWT.FILL, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.FrontCutLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.LEFT, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.EndCutLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.LEFT, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.SamplesNumberLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.LEFT, true);
		
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.DurationLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(trialsGroup, DocometreMessages.NotAvailable_Label, SWT.LEFT, true);
	}

	private void createGeneralInfoGroup() {
		Group infosGroup = ChannelEditorWidgetsFactory.createGroup(this, DocometreMessages.GeneralInfoGroupLabel);
		infosGroup.setLayout(new GridLayout(2, false));
		
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.SignalNameLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.NotAvailable_Label, SWT.LEFT, true);
		
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.FrequencyLabel, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.NotAvailable_Label, SWT.LEFT, true);
		
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.TrialsNumberLabel2, SWT.LEFT, false);
		ChannelEditorWidgetsFactory.createLabel(infosGroup, DocometreMessages.NotAvailable_Label, SWT.LEFT, true);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

}
