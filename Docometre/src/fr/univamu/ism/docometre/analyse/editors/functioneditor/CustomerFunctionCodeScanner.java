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
package fr.univamu.ism.docometre.analyse.editors.functioneditor;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
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

public class CustomerFunctionCodeScanner extends RuleBasedScanner {
	
	public static String[] RESERVED_WORDS = {"COMBO", "FILE", "EDITABLE", "FOLDER", "FUNCTION_CODE", "USER_FUNCTION", 
											 "MENU_TITLE_FR", "MENU_TITLE_PL", "MENU_TITLE", "DESCRIPTION_FR", "DESCRIPTION_PL", "DESCRIPTION", "LABEL_FR", "LABEL_PL", "LABEL", 
											 "TEXT", "TYPE", "NAME", "PARAMETERS_NUMBER"};
	private static String PARAMETER_PREFIX = "PARAMETER_";
	private static int NB_PARAMETERS = 99;
	
	/*
	 * Colors a property key : an identifier at the beginning of a line followed by '=' or ':'
	 * (e.g. FUNCTION_CODE_GOLD_II, DECLARATION, PARAMETER_1...).
	 */
	private static class PropertyKeyRule implements IRule {
		
		private IToken token;
		
		public PropertyKeyRule(IToken token) {
			this.token = token;
		}

		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			if(scanner.getColumn() != 0) return Token.UNDEFINED;
			int nbRead = 0;
			int c = scanner.read();
			while(IdentifierDetector.isIdentifierPart(c)) {
				nbRead++;
				c = scanner.read();
			}
			int keyLength = nbRead;
			nbRead++;
			while(c == ' ' || c == '\t') {
				c = scanner.read();
				nbRead++;
			}
			boolean isKey = keyLength > 0 && (c == '=' || c == ':');
			// Only the key is part of the token
			for (int i = 0; i < (isKey ? nbRead - keyLength : nbRead); i++) scanner.unread();
			return isKey ? token : Token.UNDEFINED;
		}
		
	}
	
	private static IToken createToken(String color, String font, CustomerFunctionEditor customerFunctionEditor) {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(color), 
													ThemeColors.getBackgroundColor(), 
													SWT.NORMAL, 
													DocometreApplication.getFont(font, customerFunctionEditor.fontSize));
		return new Token(attribute);
	}
	
	/*
	 * Scanner for the default partition : property keys and complete reserved words only 
	 * (NAME is no more found in FILENAME, TYPE in PROTOTYPE...), then numbers.
	 */
	public static RuleBasedScanner getCodeScanner(CustomerFunctionEditor customerFunctionEditor) {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		IToken reservedWordsToken = createToken(DocometreApplication.MAROON, DocometreApplication.COURIER_NEW_BOLD, customerFunctionEditor);
		WordRule wordRule = new WordRule(new IdentifierDetector(), new Token(null));
		for (String reservedWord : RESERVED_WORDS) wordRule.addWord(reservedWord, reservedWordsToken);
		for (int i = 1; i <= NB_PARAMETERS; i++) wordRule.addWord(PARAMETER_PREFIX + i, reservedWordsToken);
		NumberRule numberRule = new NumberRule(createToken(DocometreApplication.BLUE, DocometreApplication.COURIER_NEW, customerFunctionEditor));
		ruleBasedScanner.setRules(new IRule[] {new PropertyKeyRule(reservedWordsToken), wordRule, numberRule});
		return ruleBasedScanner;
	}
	
	public static RuleBasedScanner getCommentScanner(CustomerFunctionEditor customerFunctionEditor) {
		RuleBasedScanner ruleBasedScanner = new RuleBasedScanner();
		IToken token = createToken(DocometreApplication.GREEN, DocometreApplication.COURIER_NEW_BOLD, customerFunctionEditor);
		ruleBasedScanner.setRules(new IRule[] {new EndOfLineRule("#", token)});
		return ruleBasedScanner;
	}

}
