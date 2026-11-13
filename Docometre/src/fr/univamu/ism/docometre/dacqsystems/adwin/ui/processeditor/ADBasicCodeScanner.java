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
package fr.univamu.ism.docometre.dacqsystems.adwin.ui.processeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.NumberRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;

import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.ThemeColors;
import fr.univamu.ism.docometre.editors.IdentifierDetector;
import fr.univamu.ism.docometre.editors.WholeWordRule;

public final class ADBasicCodeScanner extends RuleBasedScanner {
	
	private static String[] IMPORT = {"IMPORT"};
	private static String[] INCLUDE = {"#INCLUDE"};
	private static String[] DEFINE = {"#DEFINE", "#IF", "#ELSEIF", "#ELSE", "#ENDIF"};
	private static String[] DECLARE = {"DIM", "AS", "LONG", "FLOAT", "SHORT", "INTEGER", "STRING", "FIFO"};
	private static String[] SEGMENTS = {"LOWINIT:", "INIT:", "EVENT:", "FINISH:"};
	private static String[] RESERVED_WORDS = {"FIFO_CLEAR", "FIFO_EMPTY", "FIFO_FULL", "IF", "ELSE", "ELSEIF", "ENDIF", "THEN", "INC", "DEC", 
											  "GLOBAL_DELAY", "GLOBALDELAY", "PROCESSDELAY", "DO", "UNTIL", "FOR", "TO", "DOWNTO", "STEP", "NEXT", "EXIT",
											  "SELECTCASE", "CASE", "CASEELSE", "ENDSELECT", "SUB", "ENDSUB", "FUNCTION", "ENDFUNCTION", "RETURN",
											  "SHIFT_RIGHT", "SHIFT_LEFT", "OR", "AND", "XOR", "NOT", "NOP"};
	private static int NB_PAR_FPAR = 80;
	private static int NB_DATA = 200;
	
	private static IToken createToken(String color, String font, ADBasicSourceEditor adBasicSourceEditor) {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(color), 
													ThemeColors.getBackgroundColor(), 
													SWT.NORMAL, 
													DocometreApplication.getFont(font, adBasicSourceEditor.getFontSize()));
		return new Token(attribute);
	}
	
	private static void addWords(WordRule wordRule, String[] words, IToken token) {
		for (String word : words) wordRule.addWord(word, token);
	}
	
	/*
	 * Scanner for the default partition : one case insensitive WordRule reads complete 
	 * identifiers (and #directives) and colors them only if they are keywords. 
	 * Digits inside identifiers (ean1, Acquisition_ean1...) are consumed by the WordRule, 
	 * so the NumberRule only colors real numbers.
	 */
	public static RuleBasedScanner getCodeScanner(ADBasicSourceEditor adBasicSourceEditor) {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules = new ArrayList<IRule>();
		
		IToken segmentToken = createToken(DocometreApplication.RED, DocometreApplication.COURIER_NEW_BOLD, adBasicSourceEditor);
		for (String segment : SEGMENTS) rules.add(new WholeWordRule(segment, segmentToken, true, false));
		
		WordRule wordRule = new WordRule(new IdentifierDetector("#"), new Token(null), true);
		IToken blueBoldToken = createToken(DocometreApplication.BLUE, DocometreApplication.COURIER_NEW_BOLD, adBasicSourceEditor);
		addWords(wordRule, DEFINE, blueBoldToken);
		for (int i = 1; i <= NB_PAR_FPAR; i++) {
			wordRule.addWord("PAR_" + i, blueBoldToken);
			wordRule.addWord("FPAR_" + i, blueBoldToken);
		}
		for (int i = 1; i <= NB_DATA; i++) wordRule.addWord("DATA_" + i, blueBoldToken);
		addWords(wordRule, DECLARE, createToken(DocometreApplication.ORANGE, DocometreApplication.COURIER_NEW_BOLD, adBasicSourceEditor));
		addWords(wordRule, RESERVED_WORDS, createToken(DocometreApplication.ORANGE, DocometreApplication.COURIER_NEW, adBasicSourceEditor));
		rules.add(wordRule);
		
		rules.add(new NumberRule(createToken(DocometreApplication.BLUE, DocometreApplication.COURIER_NEW, adBasicSourceEditor)));
		
		ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		return ruleBasedScanner;
	}
	
	/*
	 * IMPORT and #INCLUDE lines : only the keyword is colored, not the file path.
	 */
	private static RuleBasedScanner getDirectiveScanner(String[] directive, ADBasicSourceEditor adBasicSourceEditor) {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules = new ArrayList<IRule>();
		WordRule wordRule = new WordRule(new IdentifierDetector("#"), new Token(null), true);
		addWords(wordRule, directive, createToken(DocometreApplication.BLUE, DocometreApplication.COURIER_NEW_BOLD, adBasicSourceEditor));
		rules.add(wordRule);
		for (IRule commentRule : getCommentRules(adBasicSourceEditor)) rules.add(commentRule);
		ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		return ruleBasedScanner;
	}
	
	public static RuleBasedScanner getImportScanner(ADBasicSourceEditor adBasicSourceEditor) {
		return getDirectiveScanner(IMPORT, adBasicSourceEditor);
	}
	
	public static RuleBasedScanner getIncludeScanner(ADBasicSourceEditor adBasicSourceEditor) {
		return getDirectiveScanner(INCLUDE, adBasicSourceEditor);
	}
	
	public static RuleBasedScanner getStringScanner(ADBasicSourceEditor adBasicSourceEditor) {
		// Strings keep the default color : this scanner only prevents keywords from being colored inside them
		return new RuleBasedScanner();
	}
	
	private static IRule[] getCommentRules(ADBasicSourceEditor adBasicSourceEditor) {
		IToken token = createToken(DocometreApplication.GREEN, DocometreApplication.COURIER_NEW_BOLD, adBasicSourceEditor);
		return new IRule[] {new EndOfLineRule("'", token), new WholeWordRule("REM", token, true, true)};
	}
	
	public static RuleBasedScanner getCommentScanner(ADBasicSourceEditor adBasicSourceEditor) {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		ruleBasedScanner.setRules(getCommentRules(adBasicSourceEditor));
		return ruleBasedScanner;
	}

}
