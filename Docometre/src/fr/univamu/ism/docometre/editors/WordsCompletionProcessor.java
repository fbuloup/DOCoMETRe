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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

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
 * Proposes words beginning with the identifier being typed before the cursor. Words are 
 * given by groups (e.g. channels of the DACQ configuration or of the experiment, then 
 * language keywords), proposed in this order, each group in alphabetical order.
 * The prefix is compared without case, the inserted word keeps its own case.
 * 
 * Extra prefix characters (e.g. '.' for channels full names "Experiment.Subject.Signal") 
 * may be accepted in the typed prefix : a word containing one of them is compared to 
 * the whole prefix, other words (keywords) to the part after the last one ("np.ze" : "zeros").
 */
public class WordsCompletionProcessor implements IContentAssistProcessor {
	
	private static class WordsGroup {
		private Supplier<String[]> words;
		private Image image;
		private WordsGroup(Supplier<String[]> words, Image image) {
			this.words = words;
			this.image = image;
		}
	}
	
	private List<WordsGroup> groups = new ArrayList<WordsGroup>();
	private String extraPrefixCharacters = "";
	
	public WordsCompletionProcessor() {
	}
	
	/**
	 * @param names first proposed words (may be null)
	 * @param namesImage image of names proposals (may be null)
	 * @param keywords then proposed words (may be null)
	 * @param keywordsImage image of keywords proposals (may be null)
	 */
	public WordsCompletionProcessor(String[] names, Image namesImage, String[] keywords, Image keywordsImage) {
		addWords(names, namesImage);
		addWords(keywords, keywordsImage);
	}
	
	/**
	 * @param extraPrefixCharacters characters also accepted in the typed prefix, e.g. "." for channels full names
	 */
	public WordsCompletionProcessor setExtraPrefixCharacters(String extraPrefixCharacters) {
		this.extraPrefixCharacters = extraPrefixCharacters != null ? extraPrefixCharacters : "";
		return this;
	}
	
	/**
	 * Adds a group of words (may be null), proposed after the previous groups
	 */
	public WordsCompletionProcessor addWords(String[] words, Image image) {
		return addWords(() -> words, image);
	}
	
	/**
	 * Adds a group of words computed at each proposals request (e.g. channels of the loaded subjects)
	 */
	public WordsCompletionProcessor addWords(Supplier<String[]> words, Image image) {
		groups.add(new WordsGroup(words, image));
		return this;
	}
	
	/*
	 * Identifier being typed : letters, digits, '_' and extra prefix characters before the cursor
	 */
	public static String getPrefix(IDocument document, int offset, String extraPrefixCharacters) {
		int start = offset;
		try {
			while(start > 0) {
				char c = document.getChar(start - 1);
				if(!IdentifierDetector.isIdentifierPart(c) && extraPrefixCharacters.indexOf(c) == -1) break;
				start--;
			}
			return document.get(start, offset - start);
		} catch (BadLocationException e) {
			return "";
		}
	}
	
	private boolean containsExtraPrefixCharacter(String word) {
		for (char c : extraPrefixCharacters.toCharArray()) if(word.indexOf(c) != -1) return true;
		return false;
	}
	
	private static String[] sort(String[] words) {
		if(words == null) return new String[0];
		String[] sortedWords = Arrays.stream(words).filter(word -> word != null && !word.isEmpty()).toArray(String[]::new);
		Arrays.sort(sortedWords, String.CASE_INSENSITIVE_ORDER);
		return sortedWords;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		String prefix = getPrefix(viewer.getDocument(), offset, extraPrefixCharacters);
		// Part of the prefix after the last extra prefix character
		String lastPart = prefix;
		for (char c : extraPrefixCharacters.toCharArray()) {
			int index = lastPart.lastIndexOf(c);
			if(index != -1) lastPart = lastPart.substring(index + 1);
		}
		List<String[]> groupsWords = new ArrayList<String[]>();
		for (WordsGroup group : groups) groupsWords.add(sort(group.words.get()));
		// A prefix such as "Experiment.Subject.s" matching full names : words without extra 
		// prefix characters (keywords) are not proposed ("np.ze" still proposes "zeros")
		boolean fullNameMatches = false;
		if(!prefix.equals(lastPart)) {
			for (String[] words : groupsWords) 
				for (String word : words) 
					if(containsExtraPrefixCharacter(word) && startsWithIgnoreCase(word, prefix)) fullNameMatches = true;
		}
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		Set<String> proposedWords = new HashSet<String>();
		for (int i = 0; i < groups.size(); i++) {
			for (String word : groupsWords.get(i)) {
				if(proposedWords.contains(word)) continue;
				boolean fullName = containsExtraPrefixCharacter(word);
				if(!fullName && fullNameMatches) continue;
				String comparedPrefix = fullName ? prefix : lastPart;
				if(!startsWithIgnoreCase(word, comparedPrefix)) continue;
				proposedWords.add(word);
				proposals.add(new CompletionProposal(word, offset - comparedPrefix.length(), comparedPrefix.length(), word.length(), groups.get(i).image, word, null, null));
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}
	
	private static boolean startsWithIgnoreCase(String word, String prefix) {
		return word.toUpperCase(Locale.ROOT).startsWith(prefix.toUpperCase(Locale.ROOT));
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
