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

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class FloatingPointNumberRule implements IRule {

    private IToken fToken;
    
	/**
	 * Constructeur.
	 * La rËgle renverra token si lors de l'analyse
	 * elle trouve une chaine correspondant ‡ un nombre en virgule flottante. 
	 * @param token Token ‡ renvoyer en cas de succËs.
	 */
    public FloatingPointNumberRule(IToken token) {
        fToken = token;
    }

	/**
	 * DÈtermine si l'on est en prÈsence d'une chaine
	 * correspondant ‡ un nombre en virgule flottante ou non.
	 * AppelÈ par l'ASMCodeScanner (via le RuleBasedScanner).
	 * @param scanner Scanner permettant de lire le texte.
	 * @return Le token passÈ au constructeur si la chaine est une chaine binaire.
	 * <code>Token.UNDEFINED</code> sinon ce qui permet de continuer l'analyse.
	 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
	 */
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        int size = 1;
        if(Character.isDigit(c)) {
            do {
                c = scanner.read();
                ++size;
            } while(Character.isDigit(c));
            if(c == '.') {
                c = scanner.read();
                ++size;
                if(Character.isDigit(c)) {
                    do {
                        c = scanner.read();
                        ++size;
                    } while(Character.isDigit(c));
                    if(c == 'E' || c == 'e') {
                        c = scanner.read();
                        ++size;
                        if(Character.isDigit(c)) {
                            do {
                                c = scanner.read();
                            } while(Character.isDigit(c));
                            scanner.unread();
                            return fToken;		// xxx.yyyEzzz
                        } else if(c == '+' || c == '-'){
                            c = scanner.read();
                            ++size;
                            if(Character.isDigit(c)) {
                                do {
                                    c = scanner.read();
                                } while(Character.isDigit(c));
                                scanner.unread();
                                return fToken;		// xxx.yyyE(+/-)zzz
                            }
                        }
                    } else {
                        scanner.unread();	// xxx.yyy
                        return fToken;
                    }
                }
            }
        }
        for( ; size > 0 ; --size) {
            scanner.unread();
        }
        return Token.UNDEFINED;
    }

}
