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
package fr.univamu.ism.docometre.dacqsystems.arduinouno.diary;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;

import fr.univamu.ism.docometre.DocometreApplication;

/**
 * Each diary line is colored according to its beginning. Errors and anomalies are red. Rules only apply 
 * at the beginning of a line (column 0) : a channel named "Attente" or a process 
 * named "dacqProcess" no more triggers the "At" or "Process" rules.
 * No font is given to the text attributes so that colored lines follow the 
 * editor font size (Ctrl+= / Ctrl+-).
 */
public class ArduinoDiaryScanner extends RuleBasedScanner {

	private static final String RECOVERING_CHANNEL = "Total number of transferred samples for";
	private static final String AT_WORKLOAD = "At :";
	private static final String RECEIVED_STOP = "Received stop";
	private static final String START_FOOTER = "Total samples for transferred channels";
	private static final String STOP_FOOTER = " end.";
	private static final String ERROR = "ERROR";
	// Anomalies (the "Stop 's' char not received !" line is not one : stop requested by the user)
	private static final String[] ANOMALIES = {"Stop 's' char not received after", "Arduino restart ('w' char) not received", 
											   "Parameters not acknowledged", "Propagated parameters not acknowledged"};
	
	public ArduinoDiaryScanner() {
		List<IRule> rules = new ArrayList<IRule>();
		Token errorToken = createToken(DocometreApplication.RED);
		rules.add(createRule(new EndOfLineRule(ERROR, errorToken)));
		for (String anomaly : ANOMALIES) rules.add(createRule(new EndOfLineRule(anomaly, errorToken)));
		rules.add(createRule(new EndOfLineRule(RECOVERING_CHANNEL, createToken(DocometreApplication.ORANGE))));
		rules.add(createRule(new EndOfLineRule(AT_WORKLOAD, createToken(DocometreApplication.GREEN))));
		rules.add(createRule(new EndOfLineRule(RECEIVED_STOP, createToken(DocometreApplication.MAROON))));
		// Final block : transferred samples per channel up to the "... end." line
		rules.add(createRule(new MultiLineRule(START_FOOTER, STOP_FOOTER, createToken(DocometreApplication.BLUE))));
		setRules(rules.toArray(new IRule[rules.size()]));
	}
	
	private static Token createToken(String color) {
		return new Token(new TextAttribute(DocometreApplication.getColor(color)));
	}
	
	private static IRule createRule(PatternRule rule) {
		rule.setColumnConstraint(0);
		return rule;
	}

}
