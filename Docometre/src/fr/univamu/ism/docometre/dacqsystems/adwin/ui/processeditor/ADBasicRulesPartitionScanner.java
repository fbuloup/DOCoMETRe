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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordPatternRule;

import fr.univamu.ism.docometre.editors.WordsDetector;

public class ADBasicRulesPartitionScanner extends RuleBasedPartitionScanner {
	
	public static final String IMPORT = "IMPORT"; 
	public static final String COMMENT = "COMMENT"; 
	public static final String INCLUDE = "INCLUDE"; 
	public static final String DEFINE = "DEFINE"; 
	public static final String DECLARE = "DECLARE"; 
	public static final String SEGMENT = "SEGMENT"; 
	public static final String PAR_FPAR = "PAR_FPAR"; 
	public static final String RESERVED_WORDS = "RESERVED_WORDS"; 
	
	public static final String[] PARTITIONS = new String[]{IDocument.DEFAULT_CONTENT_TYPE, COMMENT, IMPORT, INCLUDE, DEFINE, SEGMENT, DECLARE, PAR_FPAR, RESERVED_WORDS};
	
	public ADBasicRulesPartitionScanner() {
		
		IToken importToken = new Token(IMPORT);
	    IToken commentToken = new Token(COMMENT);
		IToken includeToken = new Token(INCLUDE);
		IToken defineToken = new Token(DEFINE);
	    IToken declareToken = new Token(DECLARE);
	    IToken segmentToken = new Token(SEGMENT);
	    IToken parFparToken = new Token(PAR_FPAR);
	    IToken reservedWordsToken = new Token(RESERVED_WORDS);
	    
	    ArrayList<IPredicateRule> rules = new ArrayList<IPredicateRule>();
	    for (int i = 1; i <= 80; i++) {
			rules.add(new WordPatternRule(new WordsDetector(new String[]{"PAR_" + i}),"PA" ,"_" + i , parFparToken));
			rules.add(new WordPatternRule(new WordsDetector(new String[]{"FPAR_" + i}),"FPA" ,"_" + i , parFparToken));
		}
		rules.add(new EndOfLineRule("REM", commentToken));
		rules.add(new EndOfLineRule(":REM", commentToken));
		rules.add(new EndOfLineRule("'", commentToken));
		rules.add(new EndOfLineRule("IMPORT", importToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "IMPORT" }), "IMPORT", "IMPORT" + Character.SPACE_SEPARATOR, defineToken));
		rules.add(new EndOfLineRule("#INCLUDE", includeToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "#DEFINE" }), "#D", "E", defineToken));
		rules.add(new EndOfLineRule("DATA_", defineToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "DIM" }), "D", "M", declareToken));
		rules.add(new EndOfLineRule("AS SHORT", declareToken));
		rules.add(new EndOfLineRule("AS INTEGER", declareToken));
		rules.add(new EndOfLineRule("AS LONG", declareToken));
		rules.add(new EndOfLineRule("AS FLOAT", declareToken));
		rules.add(new EndOfLineRule("AS STRING", declareToken));
		rules.add(new EndOfLineRule("LOWINIT:", segmentToken));
		rules.add(new EndOfLineRule("INIT:", segmentToken));
		rules.add(new EndOfLineRule("EVENT:", segmentToken));
		rules.add(new EndOfLineRule("FINISH:", segmentToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "FIFO_CLEAR" }), "\nFIF", "EAR", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "FIFO_CLEAR" }), "\tFIF", "EAR", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "FIFO_EMPTY" }), "FIF", "PTY", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "GLOBAL_DELAY" }), "\nGLOB", "ELAY", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "INC" }), "\nIN", "C", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "INC" }), "\tIN", "C", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "IF" }), "\nI", "F", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "IF" }), "\tI", "F", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "THEN" }), " TH", "EN", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "ELSE" }), "\nEL", "SE", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "ELSE" }), "\tEL", "SE", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "ENDIF" }), "\nEN", "IF", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "ENDIF" }), "\tEN", "IF", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "DO" }), "\nD", "O", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "DO" }), "\tD", "O", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "UNTIL" }), "\nUN", "IL", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "UNTIL" }), "\tUN", "IL", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "FOR" }), "\nFO", "R", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "FOR" }), "\tFO", "R", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "TO" }), " T", "O", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "NEXT" }), "\nNE", "XT", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "NEXT" }), "\tNE", "XT", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "SHIFT_RIGHT" }), "SH", "HT", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "OR" }), "O", "R", reservedWordsToken));
		rules.add(new WordPatternRule(new WordsDetector(new String[] { "AND" }), "AN", "D", reservedWordsToken));
		

		rules.add(new WordPatternRule(new WordsDetector(new String[] { "for" }), "\nfo", "r", reservedWordsToken));
		
	    setPredicateRules(rules.toArray(new IPredicateRule[rules.size()]));
	}

}
