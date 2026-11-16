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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [16/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems.ui;

import java.util.function.Consumer;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.Process;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.processeditor.ADBasicCodeScanner;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.processeditor.ADBasicRulesPartitionScanner;
import fr.univamu.ism.docometre.dacqsystems.adwin.ui.processeditor.ADBasicSourceViewerConfiguration;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ArduinoUnoProcess;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.processeditor.ArduinoUnoCodeScanner;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.processeditor.ArduinoUnoRulesPartitionScanner;
import fr.univamu.ism.docometre.dacqsystems.arduinouno.ui.processeditor.ArduinoUnoSourceViewerConfiguration;
import fr.univamu.ism.docometre.editors.WordsCompletionProcessor;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

/**
 * Source viewers of process code (Arduino or ADBasic) used outside the process source editor, 
 * e.g. in functions and dialogs : syntax coloring as in the process source editor and 
 * content assist (Ctrl+Space) of the names of the DACQ configuration and of the language keywords
 */
public final class ProcessSourceViewerFactory {
	
	private ProcessSourceViewerFactory() {
	}
	
	/**
	 * @return editors font size preference
	 */
	public static int getFontSize() {
		return Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.EDITORS_FONT_SIZE);
	}
	
	/**
	 * @return font of the source viewers, same as the colored tokens
	 */
	public static Font getFont() {
		return DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD, getFontSize());
	}
	
	/**
	 * Creates a source viewer for the language of the process
	 * @param parent parent composite
	 * @param context the process (ArduinoUnoProcess or ADWinProcess), otherwise source viewer is not configured
	 * @param extraWordsAdder adds groups of words proposed after the names of the DACQ configuration and before 
	 * the language keywords (may be null)
	 * @return the source viewer, with an empty document
	 */
	public static SourceViewer createSourceViewer(Composite parent, Object context, Consumer<WordsCompletionProcessor> extraWordsAdder) {
		SourceViewer sourceViewer = new SourceViewer(parent, null, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		Document document = new Document();
		int fontSize = getFontSize();
		DACQConfiguration dacqConfiguration = context instanceof Process ? ((Process)context).getDACQConfiguration() : null;
		String[] names = dacqConfiguration != null ? dacqConfiguration.getProposal() : null;
		IDocumentPartitioner documentPartitioner = null;
		SourceViewerConfiguration sourceViewerConfiguration = null;
		if(context instanceof ArduinoUnoProcess) {
			documentPartitioner = new FastPartitioner(new ArduinoUnoRulesPartitionScanner(), ArduinoUnoRulesPartitionScanner.PARTITIONS);
			IContentAssistProcessor contentAssistProcessor = createContentAssistProcessor(names, ArduinoUnoCodeScanner.getCompletionKeywords(), extraWordsAdder);
			sourceViewerConfiguration = new ArduinoUnoSourceViewerConfiguration(fontSize) {
				@Override
				public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
					return createContentAssistant(contentAssistProcessor);
				}
			};
		}
		if(context instanceof ADWinProcess) {
			documentPartitioner = new FastPartitioner(new ADBasicRulesPartitionScanner(), ADBasicRulesPartitionScanner.PARTITIONS);
			IContentAssistProcessor contentAssistProcessor = createContentAssistProcessor(names, ADBasicCodeScanner.getCompletionKeywords(), extraWordsAdder);
			sourceViewerConfiguration = new ADBasicSourceViewerConfiguration(fontSize) {
				@Override
				public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
					return createContentAssistant(contentAssistProcessor);
				}
			};
		}
		if(documentPartitioner != null) {
			document.setDocumentPartitioner(documentPartitioner);
			documentPartitioner.connect(document);
		}
		sourceViewer.setDocument(document);
		// Same font as the colored tokens
		sourceViewer.getTextWidget().setFont(getFont());
		if(sourceViewerConfiguration != null) {
			sourceViewer.configure(sourceViewerConfiguration);
			// Content assist on Ctrl+Space
			sourceViewer.appendVerifyKeyListener(new VerifyKeyListener() {
				@Override
				public void verifyKey(VerifyEvent event) {
					if(event.stateMask == SWT.CTRL && event.keyCode == ' ') {
						if(sourceViewer.canDoOperation(ISourceViewer.CONTENTASSIST_PROPOSALS)) sourceViewer.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
						event.doit = false;
					}
				}
			});
		}
		return sourceViewer;
	}
	
	private static IContentAssistProcessor createContentAssistProcessor(String[] names, String[] keywords, Consumer<WordsCompletionProcessor> extraWordsAdder) {
		WordsCompletionProcessor contentAssistProcessor = new WordsCompletionProcessor();
		contentAssistProcessor.addWords(names, Activator.getImage(IImageKeys.SIGNAL_ICON));
		if(extraWordsAdder != null) extraWordsAdder.accept(contentAssistProcessor);
		contentAssistProcessor.addWords(keywords, null);
		return contentAssistProcessor;
	}
	
	private static IContentAssistant createContentAssistant(IContentAssistProcessor contentAssistProcessor) {
		ContentAssistant contentAssistant = new ContentAssistant();
		// Proposals in code only : not in comments nor strings
		contentAssistant.setContentAssistProcessor(contentAssistProcessor, IDocument.DEFAULT_CONTENT_TYPE);
		contentAssistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		return contentAssistant;
	}

}
