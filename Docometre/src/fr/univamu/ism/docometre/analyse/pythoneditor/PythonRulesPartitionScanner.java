package fr.univamu.ism.docometre.analyse.pythoneditor;

import java.util.ArrayList;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class PythonRulesPartitionScanner extends RuleBasedPartitionScanner {
	
	public static final String COMMENT = "COMMENT"; 
	public static final String DEFAULT = IDocument.DEFAULT_CONTENT_TYPE; 
	
	public static final String[] PARTITIONS = new String[]{DEFAULT, COMMENT};
	
	public PythonRulesPartitionScanner() {
		
	    IToken commentToken = new Token(COMMENT);
	    
	    ArrayList<IRule> rules = new ArrayList<IRule>();
	    
		rules.add(new EndOfLineRule("#", commentToken));
//		rules.add(new EndOfLineRule("%%", commentToken));

	    setPredicateRules(rules.toArray(new IPredicateRule[rules.size()]));
	}

}
