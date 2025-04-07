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
package fr.univamu.ism.docometre.analyse.pythoneditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.NumberRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;

import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.ThemeColors;
import fr.univamu.ism.docometre.analyse.matlabeditor.FloatingPointNumberRule;

public class PythonCodeScanner extends RuleBasedScanner {
	
	public static String[] FUNCTIONS = {
			"abs", "delattr", "hash", "memoryview", "set", "all", "dict", "help", 
			"min", "setattr", "any", "dir", "hex", "next", "slice", "ascii", "divmod", 
			"id", "object", "sorted", "bin", "enumerate", "input", "oct", "staticmethod", 
			"bool", "eval", "int", "open", "str", "breakpoint", "exec", "isinstance", 
			"ord", "sum", "bytearray", "filter", "issubclass", "pow", "super", "bytes", 
			"float", "iter", "print", "tuple", "callable", "format", "len", "property", 
			"type", "chr", "frozenset", "list", "range", "vars", "classmethod", "getattr", 
			"locals", "repr", "zip", "compile", "globals", "map", "reversed", "__import__", 
			"complex", "hasattr", "max", "round"};
	
	public static String[] DOCOMETRE_RESERVED_WORDS = {"docometre", "experiments"};

	public static String[] RESERVED_WORDS = {"and", "except", "lambda", "with", "as", "finally", 
											"nonlocal", "while", "assert", "false", "None", "yield", "break", "for", 
											"not", "class", "from", "or", "continue", "global", "pass", "def", "if", 
											"raise", "del", "import", "return", "elif", "in", "True", "else"};
	
	public static String[] NUMPY_RESERVED_WORDS = {"numpy", "zeros", "ones", "arange", "nansum", "r\\_"};
	
	public static String[] SCIPY_RESERVED_WORDS = {"scipy", "signal", "butter", "filtfilt"};
	
	public static String[] MATH_RESERVED_WORDS = {"math", "floor"};
	
	private static class PythonWordDetector implements IWordDetector {
		@Override
		public boolean isWordStart(char c) {
			return Character.isLetter(c);
		}
		@Override
		public boolean isWordPart(char c) {
			return Character.isLetter(c);
		}
	}
	
	private static class MyNumberRule extends NumberRule {
		
		private char[] operators = new char[] {'=', '+' , '*', '/', '%', '-', '(', ':'};
		
		public MyNumberRule(IToken token) {
			super(token);
		}
		
		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			int c = scanner.read();
			if (Character.isDigit((char)c)) {
				scanner.unread();
				scanner.unread();
				c = scanner.read();
				if(!isOperator((char) c)) return Token.UNDEFINED;
			} else scanner.unread();
			return super.evaluate(scanner);
		}

		private boolean isOperator(char c) {
			boolean isOperator = Character.isWhitespace(c);
			for (char operaror : operators) {
				isOperator = isOperator || c == operaror;
			}
			return isOperator;
		}
	}
	
	public static RuleBasedScanner getPythonCodeScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules = new ArrayList<IRule>();
		
		rules.addAll(getPythonScanner());
		rules.addAll(getNumbersScanner());
		
		ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		return ruleBasedScanner;
	}
	
	
	private static List<IRule> getPythonScanner() {
		List<IRule> rules= new ArrayList<IRule>();
        
        WordRule wordRule = new WordRule(new PythonWordDetector(), geDefaultTextAttributeToken());
        
        for (int i = 0; i < FUNCTIONS.length; i++) {
        	wordRule.addWord(FUNCTIONS[i], getFunctionsTextAttributeToken());
		}
        
        for (int i = 0; i < RESERVED_WORDS.length; i++) {
        	wordRule.addWord(RESERVED_WORDS[i], getReservedWordsTextAttributeToken());
		}
        
        for (int i = 0; i < NUMPY_RESERVED_WORDS.length; i++) {
        	wordRule.addWord(NUMPY_RESERVED_WORDS[i], getNumpyReservedWordsTextAttributeToken());
		}
        
        for (int i = 0; i < SCIPY_RESERVED_WORDS.length; i++) {
        	wordRule.addWord(SCIPY_RESERVED_WORDS[i], getScipyReservedWordsTextAttributeToken());
		}
        
    for (int i = 0; i < MATH_RESERVED_WORDS.length; i++) {
        	wordRule.addWord(MATH_RESERVED_WORDS[i], getMathReservedWordsTextAttributeToken());
		}
        
        for (int i = 0; i < DOCOMETRE_RESERVED_WORDS.length; i++) {
        	wordRule.addWord(DOCOMETRE_RESERVED_WORDS[i], getDocometreReservedWordsTextAttributeToken());
		}
        
        rules.add(wordRule);
        return rules;
	}
	
	public static RuleBasedScanner  getCommentScanner() {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		List<IRule> rules= new ArrayList<IRule>();
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.GREEN), 
													ThemeColors.getBackgroundColor(), 
													SWT.NORMAL, 
													DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		IRule commentRule1 = new EndOfLineRule("#", token);
//		IRule commentRule2 = new EndOfLineRule("%%", token);
        rules.add(commentRule1);
//        rules.add(commentRule2);
		
        ruleBasedScanner.setRules(rules.toArray(new IRule[rules.size()]));
		return ruleBasedScanner;
	}
	
	private static List<IRule>  getNumbersScanner() {
		List<IRule> rules= new ArrayList<IRule>();
		
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLACK), null, SWT.BOLD);
		
	    IToken numberToken = new Token(attribute);
	    
	    FloatingPointNumberRule floatingPointNumberRule = new FloatingPointNumberRule(numberToken);
        rules.add(floatingPointNumberRule);

	    MyNumberRule numberRule = new MyNumberRule(numberToken);
        rules.add(numberRule);
	    
        return rules;
	}
	
	private static IToken getFunctionsTextAttributeToken() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLUE),
				ThemeColors.getBackgroundColor(), SWT.NORMAL,
				DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return token;
	}
	
	private static IToken getReservedWordsTextAttributeToken() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.ORANGE),
				ThemeColors.getBackgroundColor(), SWT.NORMAL,
				DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return token;
	}
	
	private static IToken getNumpyReservedWordsTextAttributeToken() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.MAROON),
				ThemeColors.getBackgroundColor(), SWT.NORMAL,
				DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return token;
	}
	
	private static IToken getScipyReservedWordsTextAttributeToken() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.MAROON),
				ThemeColors.getBackgroundColor(), SWT.NORMAL,
				DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return token;
	}
	
	private static IToken getDocometreReservedWordsTextAttributeToken() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.RED),
				ThemeColors.getBackgroundColor(), SWT.NORMAL,
				DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return token;
	}
	
	private static IToken getMathReservedWordsTextAttributeToken() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.MAROON),
				ThemeColors.getBackgroundColor(), SWT.NORMAL,
				DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return token;
	}
	
	private static IToken geDefaultTextAttributeToken() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLACK));
		IToken token = new Token(attribute);
		return token;
	}
	

}
