package fr.univamu.ism.docometre.analyse.wizard;

import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;

public class MergeSubjectsWizardPage extends WizardPage {

	protected MergeSubjectsWizardPage() {
		super("MergeSubjectsWizardPage");
		setTitle(DocometreMessages.MergeSubjectsWizardPageTitle);
		setDescription(DocometreMessages.MergeSubjectsWizardPageDescription);
		setImageDescriptor(Activator.getImageDescriptor(IImageKeys.MERGE_WIZARD));
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, true));
		
		Label labelFrom = new Label(container, SWT.NONE);
		labelFrom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		labelFrom.setText(DocometreMessages.MergeSubjectsFromLabel);
		
		Label labelTo = new Label(container, SWT.NONE);
		labelTo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		labelTo.setText(DocometreMessages.MergeSubjectsToLabel);
		
		Composite subjectsContainer = new Composite(container, SWT.NONE);
		subjectsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		subjectsContainer.setLayout(new GridLayout(4, false));
		
		ListViewer fromListViewer = new ListViewer(subjectsContainer, SWT.BORDER);
		fromListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite fromButtonsContainer = new Composite(subjectsContainer, SWT.NONE);
		fromButtonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		fromButtonsContainer.setLayout(new FillLayout(SWT.VERTICAL));
		
		Button fromUpButton = new Button(fromButtonsContainer, SWT.FLAT);
		fromUpButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		fromUpButton.setToolTipText(DocometreMessages.Up_Label);
		
		Button fromDownButton = new Button(fromButtonsContainer, SWT.FLAT);
		fromDownButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		fromDownButton.setToolTipText(DocometreMessages.Down_Label);
		
		Button fromAddButton = new Button(fromButtonsContainer, SWT.FLAT);
		fromAddButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		fromAddButton.setToolTipText(DocometreMessages.Add_Tooltip);
		
		Button fromDeleteButton = new Button(fromButtonsContainer, SWT.FLAT);
		fromDeleteButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		fromDeleteButton.setToolTipText(DocometreMessages.Delete_Tooltip);
		
		ListViewer ToListViewer = new ListViewer(subjectsContainer, SWT.BORDER);
		ToListViewer.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite ToButtonsContainer = new Composite(subjectsContainer, SWT.NONE);
		ToButtonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		ToButtonsContainer.setLayout(new FillLayout(SWT.VERTICAL));
		
		Button ToUpButton = new Button(ToButtonsContainer, SWT.FLAT);
		ToUpButton.setImage(Activator.getImage(IImageKeys.UP_ICON));
		ToUpButton.setToolTipText(DocometreMessages.Up_Label);
		
		Button toDownButton = new Button(ToButtonsContainer, SWT.FLAT);
		toDownButton.setImage(Activator.getImage(IImageKeys.DOWN_ICON));
		toDownButton.setToolTipText(DocometreMessages.Down_Label);
		
		Button toAddButton = new Button(ToButtonsContainer, SWT.FLAT);
		toAddButton.setImage(Activator.getImage(IImageKeys.ADD_ICON));
		toAddButton.setToolTipText(DocometreMessages.Add_Tooltip);
		
		Button toDeleteButton = new Button(ToButtonsContainer, SWT.FLAT);
		toDeleteButton.setImage(Activator.getImage(IImageKeys.DELETE_ICON));
		toDeleteButton.setToolTipText(DocometreMessages.Delete_Tooltip);

		setControl(container);

	}

}
