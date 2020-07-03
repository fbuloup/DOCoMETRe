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
package fr.univamu.ism.docometre.analyse.matlabeditor;

import java.util.ArrayList;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordPatternRule;

import fr.univamu.ism.docometre.editors.WordsDetector;

public class MatlabRulesPartitionScanner extends RuleBasedPartitionScanner {
	
	public static final String FUNCTIONS = "FUNCTIONS"; 
	public static final String COMMENT = "COMMENT"; 
	public static final String RESERVED_WORDS = "RESERVED_WORDS"; 
	
	public static final String[] PARTITIONS = new String[]{IDocument.DEFAULT_CONTENT_TYPE, COMMENT, FUNCTIONS, RESERVED_WORDS};
	
	public MatlabRulesPartitionScanner() {
		
		IToken functionToken = new Token(FUNCTIONS);
	    IToken commentToken = new Token(COMMENT);
	    IToken reservedWordsToken = new Token(RESERVED_WORDS);
	    
	    ArrayList<IPredicateRule> rules = new ArrayList<IPredicateRule>();
		rules.add(new EndOfLineRule("%", commentToken));
		rules.add(new EndOfLineRule("%%", commentToken));

		for(int i = 0; i < MatlabCodeScanner.FUNCTIONS.length; i++) {
			String function = MatlabCodeScanner.FUNCTIONS[i];
			System.out.println(function);
			String start = "";
			String end = "";
			if(function.length() == 2) {
				start = function.substring(0, 1);
				end = function.substring(1, 2);
			} else if(function.length() == 3) {
				start = function.substring(0, 1);
				end = function.substring(1, 3);
			} else {
				start = function.substring(0, 2);
				end = function.substring(function.length() - 2, function.length());
			}
			rules.add(new WordPatternRule(new WordsDetector(new String[] { function + " " }), " " + start, end + " ", functionToken));
			rules.add(new WordPatternRule(new WordsDetector(new String[] { function + "(" }), " " + start, end + "(", functionToken));
			rules.add(new WordPatternRule(new WordsDetector(new String[] { function + " " }), "\n" + start, end + " ", functionToken));
			rules.add(new WordPatternRule(new WordsDetector(new String[] { function + "(" }), "\n" + start, end + "(", functionToken));
			
			rules.add(new WordPatternRule(new WordsDetector(new String[] { function + "\n" }), "" + start, end + "\n", functionToken));
			rules.add(new WordPatternRule(new WordsDetector(new String[] { function + "\n" }), "\n" + start, end + "\n", functionToken));
			
		}
		
		for(int i = 0; i < MatlabCodeScanner.RESERVED_WORDS.length; i++) {
			String reservedWord = MatlabCodeScanner.RESERVED_WORDS[i];
			String start = "";
			String end = "";
			if(reservedWord.length() == 2) {
				start = reservedWord.substring(0, 1);
				end = reservedWord.substring(1, 2);
			} else if(reservedWord.length() == 3) {
				start = reservedWord.substring(0, 1);
				end = reservedWord.substring(1, 3);
			} else {
				start = reservedWord.substring(0, 1);
				end = reservedWord.substring(reservedWord.length() - 2, reservedWord.length() - 1);
			}
			rules.add(new WordPatternRule(new WordsDetector(new String[] { reservedWord + " " }), " " + start, end + " ", reservedWordsToken));
			rules.add(new WordPatternRule(new WordsDetector(new String[] { reservedWord + "(" }), " " + start, end + "(", reservedWordsToken));
			rules.add(new WordPatternRule(new WordsDetector(new String[] { reservedWord + " " }), "\n" + start, end + " ", reservedWordsToken));
			rules.add(new WordPatternRule(new WordsDetector(new String[] { reservedWord + "(" }), "\n" + start, end + "(", reservedWordsToken));
			
			rules.add(new WordPatternRule(new WordsDetector(new String[] { reservedWord + "\n" }), "" + start, end + "\n", reservedWordsToken));
			rules.add(new WordPatternRule(new WordsDetector(new String[] { reservedWord + "\n" }), "\n" + start, end + "\n", reservedWordsToken));
		}
		
		
		
	    setPredicateRules(rules.toArray(new IPredicateRule[rules.size()]));
	}

}
