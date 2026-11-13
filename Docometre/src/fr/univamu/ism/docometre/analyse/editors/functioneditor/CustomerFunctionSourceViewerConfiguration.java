package fr.univamu.ism.docometre.analyse.editors.functioneditor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;

public class CustomerFunctionSourceViewerConfiguration extends SourceViewerConfiguration {
	
	private PresentationReconciler presentationReconciler;
	CustomerFunctionEditor customerFunctionEditor;
	
	public CustomerFunctionSourceViewerConfiguration(CustomerFunctionEditor customerFunctionEditor) {
		this.customerFunctionEditor = customerFunctionEditor;
	}

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
	
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		if(presentationReconciler != null) return presentationReconciler;
		
		presentationReconciler = (PresentationReconciler) super.getPresentationReconciler(sourceViewer);
		
		DefaultDamagerRepairer defaultDamagerRepairer = new DefaultDamagerRepairer(CustomerFunctionCodeScanner.getCommentScanner(customerFunctionEditor));		
		presentationReconciler.setDamager(defaultDamagerRepairer, CustomerFunctionRulesPartitionScanner.COMMENT);
		presentationReconciler.setRepairer(defaultDamagerRepairer, CustomerFunctionRulesPartitionScanner.COMMENT);
		
		defaultDamagerRepairer = new DefaultDamagerRepairer(CustomerFunctionCodeScanner.getCodeScanner(customerFunctionEditor));		
		presentationReconciler.setDamager(defaultDamagerRepairer, IDocument.DEFAULT_CONTENT_TYPE);
		presentationReconciler.setRepairer(defaultDamagerRepairer, IDocument.DEFAULT_CONTENT_TYPE);
		
		return presentationReconciler;
	}
	
	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return CustomerFunctionRulesPartitionScanner.PARTITIONS;
	}	

}