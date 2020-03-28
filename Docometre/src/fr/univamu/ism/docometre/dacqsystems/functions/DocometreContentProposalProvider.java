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
package fr.univamu.ism.docometre.dacqsystems.functions;

import java.util.ArrayList;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.widgets.Text;

public class DocometreContentProposalProvider implements IContentProposalProvider, IContentProposalListener {
	
	
	private int startPosition;
	private int position;
	
	/*
	 * The proposals provided.
	 */
	private String[] proposals;

	/*
	 * The proposals mapped to IContentProposal. Cached for speed in the case
	 * where filtering is not used.
	 */
	private IContentProposal[] contentProposals;

	/*
	 * Boolean that tracks whether filtering is used.
	 */
	private boolean filterProposals = false;
	private Text expressionText;

	public DocometreContentProposalProvider(String[] proposals, Text expressionText) {
		super();
		this.proposals = proposals;
		this.expressionText = expressionText;
	}
	
	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		char[] chars =  contents.toCharArray();
		startPosition = 0;
		this.position = position;
		if(chars.length > 0)
			for (int i = position - 1; i >= 0; i--) {
				boolean found = chars[i] == '\u0020' || chars[i] == '*' || chars[i] == '+' || chars[i] == '-' || chars[i] == '/' || chars[i] == '(' || chars[i] == '=';
				if(found) {
					startPosition = i + 1;
					break;
				}
			}
		
		if (filterProposals) {
			ArrayList<ContentProposal> list = new ArrayList<>();
			String value = "";
			if(position > 0) value = contents.substring(startPosition, position);
			for (String proposal : proposals) {
				if(proposal.startsWith(value) || value.equals("")) list.add(new ContentProposal(proposal));
			}
			return list.toArray(new IContentProposal[list.size()]);
		}
		
		if (contentProposals == null) {
			contentProposals = new IContentProposal[proposals.length];
			for (int i = 0; i < proposals.length; i++) {
				contentProposals[i] = new ContentProposal(proposals[i]);
			}
		}
		return contentProposals;
		
	}
	
	/**
	 * Set the Strings to be used as content proposals.
	 *
	 * @param items
	 *            the Strings to be used as proposals.
	 */
	public void setProposals(String... items) {
		this.proposals = items;
		contentProposals = null;
	}
	
	/**
	 * Set the boolean that controls whether proposals are filtered according to
	 * the current field content.
	 *
	 * @param filterProposals
	 *            <code>true</code> if the proposals should be filtered to
	 *            show only those that match the current contents of the field,
	 *            and <code>false</code> if the proposals should remain the
	 *            same, ignoring the field content.
	 * @since 3.3
	 */
	public void setFiltering(boolean filterProposals) {
		this.filterProposals = filterProposals;
		// Clear any cached proposals.
		contentProposals = null;
	}

	@Override
	public void proposalAccepted(IContentProposal proposal) {
		String text = expressionText.getText();
		String newText = text.substring(0, startPosition) + text.substring(position, text.length());
		expressionText.setText(newText);
		expressionText.setSelection(position + proposal.getContent().length());
		
	}

}
