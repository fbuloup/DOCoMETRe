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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.PlatformUI;

import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinMessages;

public class ADWinDiaryScanner extends RuleBasedScanner {
	
	private static Color backgroundColor;
	private static Color foregroundColor;
	
	private static final String HEADER_START = ADWinMessages.ADWinDiary_Header_Start_Scanner;
	private static final String HEADER_FOOTER_END = ADWinMessages.ADWinDiary_Header_Footer_End_Scanner;
	private static final String TIME_BETWEEN_TWO_ADWIN_DIALOG = ADWinMessages.ADWinDiary_TimeBetween_Scanner; 
	private static final String RECOVERING_CHANNEL = ADWinMessages.ADWinDiary_Recovering_Scanner;
	private static final String RECOVERY_TIME = ADWinMessages.ADWinDiary_RecoveryTime_Scanner;
	private static final String GENERATING_CHANNEL = ADWinMessages.ADWinDiary_Generating_Scanner;
	private static final String GENERATION_TIME = ADWinMessages.ADWinDiary_GenerationTime_Scanner;
	private static final String DISPLAY_TIME = ADWinMessages.ADWinDiary_DisplayTime_Scanner;
	private static final String DATA_LOSS = ADWinMessages.ADWinDiary_DataLoss_Scanner;
	private static final String FOOTER_START_1 = ADWinMessages.ADWinDiary_Ending_Scanner;
	private static final String FOOTER_START_2 = ADWinMessages.ADWinDiary_Approximative_Scanner;
	private static final String NO_MORE_TO_GENERATE = ADWinMessages.ADWinDiary_NoMoreToGenerate_Scanner;
	
	public ADWinDiaryScanner() {
		List<IRule> rules= new ArrayList<IRule>();
		rules.add(getRecoveryTimeScanner());
		rules.add(getGenerationTimeScanner());
		rules.add(getDisplayTimeScanner());
		rules.add(getGeneratingChannelScanner());
		rules.add(getRecoveringChannelScanner());
		rules.add(getTimeBetween2ADWinDialogScanner());
		rules.add(getDataLossScanner());
		rules.add(getHeaderScanner());
		rules.add(getFooterScanner_1());
		rules.add(getFooterScanner_2());
		rules.add(getNoMoreDataToGenerate());
		setRules(rules.toArray(new IRule[rules.size()]));
		backgroundColor = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		foregroundColor = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
	}
	
	public static IRule getTimeBetween2ADWinDialogScanner() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.BLUE), 
													backgroundColor, 
				   									SWT.NORMAL, 
				   									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
        return new EndOfLineRule(TIME_BETWEEN_TWO_ADWIN_DIALOG, token);
	}
	
	public static IRule getRecoveringChannelScanner() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.ORANGE), 
				backgroundColor, 
				   									SWT.NORMAL, 
				   									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
        return new EndOfLineRule(RECOVERING_CHANNEL, token);
	}
	
	public static IRule getRecoveryTimeScanner() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.ORANGE), 
				   									backgroundColor, 
				   									SWT.NORMAL, 
				   									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
        return new EndOfLineRule(RECOVERY_TIME, token);
	}
	
	public static IRule getGeneratingChannelScanner() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.GREEN), 
				   									backgroundColor, 
				   									SWT.NORMAL, 
				   									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
        return new EndOfLineRule(GENERATING_CHANNEL, token);
	}
	
	public static IRule getGenerationTimeScanner() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.GREEN), 
				   									backgroundColor, 
				   									SWT.NORMAL, 
				   									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
        return new EndOfLineRule(GENERATION_TIME, token);
	}
	
	public static IRule getDisplayTimeScanner() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.MAROON), 
				   									backgroundColor, 
				   									SWT.NORMAL, 
				   									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
        return new EndOfLineRule(DISPLAY_TIME, token);
	}
	
	public static IRule getDataLossScanner() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.RED), 
				   									backgroundColor, 
				   									SWT.NORMAL, 
				   									DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
        return new EndOfLineRule(DATA_LOSS, token);
	}
	
	public static IRule getHeaderScanner() {
		TextAttribute attribute = new TextAttribute(foregroundColor, 
					backgroundColor, 
					SWT.NORMAL, 
					DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return new MultiLineRule(HEADER_START, HEADER_FOOTER_END, token);
	}
	
	public static IRule getFooterScanner_1() {
		TextAttribute attribute = new TextAttribute(foregroundColor, 
					backgroundColor, 
					SWT.NORMAL, 
					DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return new MultiLineRule(FOOTER_START_1, HEADER_FOOTER_END, token);
	}
	
	public static IRule getFooterScanner_2() {
		TextAttribute attribute = new TextAttribute(foregroundColor, 
					backgroundColor, 
					SWT.NORMAL, 
					DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return new MultiLineRule(FOOTER_START_2, HEADER_FOOTER_END, token);
	}
	
	public static IRule getNoMoreDataToGenerate() {
		TextAttribute attribute = new TextAttribute(DocometreApplication.getColor(DocometreApplication.GREEN), 
					backgroundColor, 
					SWT.NORMAL, 
					DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD));
		IToken token = new Token(attribute);
		return new EndOfLineRule(NO_MORE_TO_GENERATE, token);
	}
	
	

}
