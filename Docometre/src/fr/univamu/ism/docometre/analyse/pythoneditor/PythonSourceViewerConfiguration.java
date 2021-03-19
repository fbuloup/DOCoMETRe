package fr.univamu.ism.docometre.analyse.pythoneditor;

import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

public class PythonSourceViewerConfiguration extends SourceViewerConfiguration {
	
	private PresentationReconciler presentationReconciler;

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		if(presentationReconciler != null) return presentationReconciler;
		
		presentationReconciler = (PresentationReconciler) super.getPresentationReconciler(sourceViewer);
		
		DefaultDamagerRepairer defaultDamagerRepairer = new DefaultDamagerRepairer(PythonCodeScanner.getCommentScanner());		
		presentationReconciler.setDamager(defaultDamagerRepairer, PythonRulesPartitionScanner.COMMENT);
		presentationReconciler.setRepairer(defaultDamagerRepairer, PythonRulesPartitionScanner.COMMENT);
		
		defaultDamagerRepairer = new DefaultDamagerRepairer(PythonCodeScanner.getPythonCodeScanner());		
		presentationReconciler.setDamager(defaultDamagerRepairer, PythonRulesPartitionScanner.DEFAULT);
		presentationReconciler.setRepairer(defaultDamagerRepairer, PythonRulesPartitionScanner.DEFAULT);

		return presentationReconciler;
	}
	
	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return PythonRulesPartitionScanner.PARTITIONS;
	}	

}
