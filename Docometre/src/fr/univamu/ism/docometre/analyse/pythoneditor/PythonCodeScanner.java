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
