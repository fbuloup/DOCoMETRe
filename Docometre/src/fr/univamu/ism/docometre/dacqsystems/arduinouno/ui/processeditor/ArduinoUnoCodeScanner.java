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
package fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.processeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;

import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.ThemeColors;
import fr.univamu.ism.docometre.editors.WordsDetector;

public class ArduinoUnoCodeScanner extends RuleBasedScanner {
	
	private static String[] INCLUDE = {"#include"};
	private static String[] DEFINE = {"#ifndef", "#ifdef", "#endif"};
	private static String[] SEGMENTS = {"setup()", "loop()", "finalize()"};
	public static String[] RESERVED_WORDS = {"double", "for", "while", "do", "void", "auto", "signed", "const", "extern", "register", "unsigned", "int", "float", "char", "double", "long", "break", "return", "continue",
												"if", "else", "switch", "case", "default", "sizeof", "struct", "typedef", "union", "volatile", "byte", "bool", "sprintf", "Serial", "println", "print", "begin",
												"available", "read", "micros", "String", "wdt_enable", "exit"};
	
	public static RuleBasedScanner getIncludeScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules = new ArrayList<IRule>(0);
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLUE), 
													ThemeColors.getBackgroundColor(), 
													SWT.NORMAL, 
													DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		WordRule wordRule = new WordRule(new WordsDetector(INCLUDE), token);
		rules.add(wordRule);
		ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		
		return ruleBasedScanner;
	}
	
	public static RuleBasedScanner getDefineScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules = new ArrayList<IRule>(0);
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.ORANGE), 
													ThemeColors.getBackgroundColor(), 
													SWT.NORMAL, 
													DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		WordRule wordRule = new WordRule(new WordsDetector(DEFINE), token);
		rules.add(wordRule);
		ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		
		return ruleBasedScanner;
	}
	
	public static RuleBasedScanner getSegmentScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules = new ArrayList<IRule>(0);
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.RED), 
													ThemeColors.getBackgroundColor(), 
													SWT.NORMAL, 
													DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		WordRule wordRule = new WordRule(new WordsDetector(SEGMENTS), token);
		rules.add(wordRule);
		ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		
		return ruleBasedScanner;
	}
	
	public static RuleBasedScanner getReservedWordsScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules = new ArrayList<IRule>(0);
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.MAROON), 
													ThemeColors.getBackgroundColor(), 
													SWT.NORMAL, 
													DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		WordRule wordRule = new WordRule(new WordsDetector(RESERVED_WORDS), token);
		rules.add(wordRule);
		ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		
		return ruleBasedScanner;
	}
	
	public static RuleBasedScanner getCommentScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules = new ArrayList<IRule>(0);
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.GREEN), 
													ThemeColors.getBackgroundColor(), 
													SWT.NORMAL, 
													DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		IRule commentRule1 = new EndOfLineRule("//", token);
		IRule commentRule2 = new MultiLineRule("/*", "*/", token, (char) 0, true);
		rules.add(commentRule1);
		rules.add(commentRule2);
		ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		
		return ruleBasedScanner;
	}

}
