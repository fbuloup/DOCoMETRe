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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [13/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

/**
 * Proposes words beginning with the identifier being typed before the cursor : 
 * first the names (e.g. channels and variables of the DACQ configuration), then the language keywords,
 * each group in alphabetical order.
 * The prefix is compared without case, the inserted word keeps its own case.
 */
public class WordsCompletionProcessor implements IContentAssistProcessor {
	
	private String[] names;
	private Image namesImage;
	private String[] keywords;
	private Image keywordsImage;
	
	/**
	 * @param names first proposed words (may be null)
	 * @param namesImage image of names proposals (may be null)
	 * @param keywords then proposed words (may be null)
	 * @param keywordsImage image of keywords proposals (may be null)
	 */
	public WordsCompletionProcessor(String[] names, Image namesImage, String[] keywords, Image keywordsImage) {
		this.names = sort(names);
		this.namesImage = namesImage;
		this.keywords = sort(keywords);
		this.keywordsImage = keywordsImage;
	}
	
	private static String[] sort(String[] words) {
		if(words == null) return new String[0];
		String[] sortedWords = Arrays.stream(words).filter(word -> word != null).toArray(String[]::new);
		Arrays.sort(sortedWords, String.CASE_INSENSITIVE_ORDER);
		return sortedWords;
	}
	
	/*
	 * Identifier being typed : letters, digits and '_' before the cursor
	 */
	public static String getPrefix(IDocument document, int offset) {
		int start = offset;
		try {
			while(start > 0 && IdentifierDetector.isIdentifierPart(document.getChar(start - 1))) start--;
			return document.get(start, offset - start);
		} catch (BadLocationException e) {
			return "";
		}
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		String prefix = getPrefix(viewer.getDocument(), offset);
		String upperCasePrefix = prefix.toUpperCase(Locale.ROOT);
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		Set<String> proposedWords = new LinkedHashSet<String>();
		addProposals(names, namesImage, prefix, upperCasePrefix, offset, proposals, proposedWords);
		addProposals(keywords, keywordsImage, prefix, upperCasePrefix, offset, proposals, proposedWords);
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}
	
	private void addProposals(String[] words, Image image, String prefix, String upperCasePrefix, int offset, List<ICompletionProposal> proposals, Set<String> proposedWords) {
		for (String word : words) {
			if(word == null || word.isEmpty() || proposedWords.contains(word)) continue;
			if(!word.toUpperCase(Locale.ROOT).startsWith(upperCasePrefix)) continue;
			proposedWords.add(word);
			proposals.add(new CompletionProposal(word, offset - prefix.length(), prefix.length(), word.length(), image, word, null, null));
		}
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
