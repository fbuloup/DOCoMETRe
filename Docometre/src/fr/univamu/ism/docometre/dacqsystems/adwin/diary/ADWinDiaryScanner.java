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
package fr.univamu.ism.docometre.dacqsystems.adwin.diary;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;

import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;

/**
 * Each diary line is colored according to its beginning. Rules only apply 
 * at the beginning of a line (column 0) : a channel, process or folder name 
 * containing "ADWin" or "Ending" no more starts a colored block in the middle of a line.
 * No font is given to the text attributes so that colored lines follow the 
 * editor font size (Ctrl+= / Ctrl+-).
 */
public class ADWinDiaryScanner extends RuleBasedScanner {

	private static final String TIME_BETWEEN_TWO_ADWIN_DIALOG = ADWinMessages.ADWinDiary_TimeBetween_Scanner; 
	private static final String RECOVERING_CHANNEL = ADWinMessages.ADWinDiary_Recovering_Scanner;
	private static final String RECOVERY_TIME = ADWinMessages.ADWinDiary_RecoveryTime_Scanner;
	private static final String GENERATING_CHANNEL = ADWinMessages.ADWinDiary_Generating_Scanner;
	private static final String GENERATION_TIME = ADWinMessages.ADWinDiary_GenerationTime_Scanner;
	private static final String DISPLAY_TIME = ADWinMessages.ADWinDiary_DisplayTime_Scanner;
	private static final String DATA_LOSS = ADWinMessages.ADWinDiary_DataLoss_Scanner;
	private static final String NO_MORE_TO_GENERATE = ADWinMessages.ADWinDiary_NoMoreToGenerate_Scanner;
	
	public ADWinDiaryScanner() {
		setRules(new IRule[] {
			createLineRule(RECOVERY_TIME, DocometreApplication.ORANGE),
			createLineRule(GENERATION_TIME, DocometreApplication.GREEN),
			createLineRule(DISPLAY_TIME, DocometreApplication.MAROON),
			createLineRule(GENERATING_CHANNEL, DocometreApplication.GREEN),
			createLineRule(RECOVERING_CHANNEL, DocometreApplication.ORANGE),
			createLineRule(TIME_BETWEEN_TWO_ADWIN_DIALOG, DocometreApplication.BLUE),
			createLineRule(DATA_LOSS, DocometreApplication.RED),
			createLineRule(NO_MORE_TO_GENERATE, DocometreApplication.GREEN)
		});
	}
	
	private static IRule createLineRule(String lineStart, String color) {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(color));
		EndOfLineRule rule = new EndOfLineRule(lineStart, new Token(attribute));
		rule.setColumnConstraint(0);
		return rule;
	}

}
