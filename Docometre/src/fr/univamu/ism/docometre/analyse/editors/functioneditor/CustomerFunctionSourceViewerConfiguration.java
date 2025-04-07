package fr.univamu.ism.docometre.analyse.editors.functioneditor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class CustomerFunctionSourceViewerConfiguration extends SourceViewerConfiguration {

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant contentAssistant = new ContentAssistant();
//			contentAssistant.enableAutoActivation(true);
//			contentAssistant.enableAutoInsert(true);
//			contentAssistant.enableCompletionProposalTriggerChars(false);
		contentAssistant.addContentAssistProcessor(new CustomerFunctionCompletionProcessor(),
				IDocument.DEFAULT_CONTENT_TYPE);
		contentAssistant.install(sourceViewer);
		return contentAssistant;
	}

	@Override
	public IUndoManager getUndoManager(ISourceViewer sourceViewer) {
		TextViewerUndoManager textViewerUndoManager = new TextViewerUndoManager(
				Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.PREF_UNDO_LIMIT));
		return textViewerUndoManager;
	}

}