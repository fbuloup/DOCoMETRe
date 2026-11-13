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

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * Single line string literal rule. Strings are detected so that comment markers 
 * ("//", "#", "%", "'") and keywords inside them are ignored.
 * An unterminated string ends at the end of the line.
 */
public class StringRule implements IPredicateRule {
	
	private static final String TRANSPOSE_PREDECESSORS = ")]}.'\"";
	
	private char quote;
	private IToken token;
	private char escapeCharacter;
	private boolean doubledQuoteEscape;
	private boolean transposeOperator;
	
	/**
	 * @param quote the string delimiter
	 * @param token the token returned on success
	 * @param escapeCharacter escape character, e.g. '\\' (0 if none)
	 * @param doubledQuoteEscape true if a doubled delimiter stands for the delimiter itself (MATLAB : 'don''t')
	 * @param transposeOperator true if the delimiter is also the transpose operator when it follows 
	 * an identifier, a closing bracket, a dot or another quote (MATLAB : x', (a+b)', a.')
	 */
	public StringRule(char quote, IToken token, char escapeCharacter, boolean doubledQuoteEscape, boolean transposeOperator) {
		this.quote = quote;
		this.token = token;
		this.escapeCharacter = escapeCharacter;
		this.doubledQuoteEscape = doubledQuoteEscape;
		this.transposeOperator = transposeOperator;
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		return evaluate(scanner, false);
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner, boolean resume) {
		// When resuming inside a partition, let the partition scanner restart from the partition start
		if(resume) return Token.UNDEFINED;
		int c = scanner.read();
		if(c != quote) {
			scanner.unread();
			return Token.UNDEFINED;
		}
		if(transposeOperator) {
			int previous = ScannerUtils.previousCharacter(scanner);
			if(IdentifierDetector.isIdentifierPart(previous) || (previous != ICharacterScanner.EOF && TRANSPOSE_PREDECESSORS.indexOf(previous) != -1)) {
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
		while(true) {
			c = scanner.read();
			if(c == ICharacterScanner.EOF || c == '\n' || c == '\r') {
				scanner.unread();
				return token;
			}
			if(escapeCharacter != 0 && c == escapeCharacter) {
				c = scanner.read();
				if(c == ICharacterScanner.EOF || c == '\n' || c == '\r') {
					scanner.unread();
					return token;
				}
				continue;
			}
			if(c == quote) {
				if(doubledQuoteEscape) {
					c = scanner.read();
					if(c == quote) continue;
					scanner.unread();
				}
				return token;
			}
		}
	}

	@Override
	public IToken getSuccessToken() {
		return token;
	}

}
