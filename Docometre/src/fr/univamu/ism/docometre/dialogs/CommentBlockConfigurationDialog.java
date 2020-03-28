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

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.process.Comment;

public class CommentBlockConfigurationDialog extends TitleAreaDialog {
	
	private Comment comment;
	private String commentString;
	private Text commentText;

	public CommentBlockConfigurationDialog(Shell parentShell, Comment comment) {
		super(parentShell);
		setShellStyle(getShellStyle()); 
		this.comment = comment;
	}
	
	@Override
	public void create() {
		super.create();
		getShell().setText(DocometreMessages.BlockDialogShellTitle);
		setTitle(DocometreMessages.CommentBlockConfigurationTitle);
		setMessage(DocometreMessages.CommentBlockConfigurationMessage, IMessageProvider.INFORMATION);
		setTitleImage(Activator.getImageDescriptor(IImageKeys.COMMENT_WIZBAN).createImage());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout(1, false));
		
		Label commentLabel = new Label(container, SWT.NONE);
		commentLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		commentLabel.setText("Comment : ");
		
		commentText = new Text(container, SWT.MULTI | SWT.BORDER);
		commentText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		commentText.setText(comment.getComment());
		
		return area;
	}
	
	@Override
	protected void okPressed() {
		commentString = commentText.getText();
		super.okPressed();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	public String getComment() {
		return commentString;
	}
	
	
	
}
