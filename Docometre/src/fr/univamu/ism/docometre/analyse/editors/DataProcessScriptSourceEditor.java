package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.analyse.matlabeditor.MatlabRulesPartitionScanner;
import fr.univamu.ism.docometre.analyse.matlabeditor.MatlabSourceViewerConfiguration;
import fr.univamu.ism.docometre.analyse.pythoneditor.PythonRulesPartitionScanner;
import fr.univamu.ism.docometre.analyse.pythoneditor.PythonSourceViewerConfiguration;
import fr.univamu.ism.docometre.dacqsystems.ui.SourceEditor;
import fr.univamu.ism.docometre.preferences.MathEnginePreferencesConstants;

public class DataProcessScriptSourceEditor extends SourceEditor {

	public DataProcessScriptSourceEditor(CommandStack commandStack, DataProcessEditor dataProcessEditor) {
		super(commandStack, dataProcessEditor);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		String mathEngine = Activator.getDefault().getPreferenceStore().getString(MathEnginePreferencesConstants.MATH_ENGINE);
		if(MathEnginePreferencesConstants.MATH_ENGINE_MATLAB.equals(mathEngine)) {
			
			// See : https://github.com/amarbanerjee23/matclipse/tree/master/org.eclipselabs.matclipse.meditor/src/org/eclipselabs/matclipse/meditor/editors
			
			FastPartitioner matlabFastPartitioner = new FastPartitioner(new MatlabRulesPartitionScanner(), MatlabRulesPartitionScanner.PARTITIONS);
		    document.setDocumentPartitioner(matlabFastPartitioner);
		    matlabFastPartitioner.connect(document);
			sourceViewer.setDocument(document, annotationModel, -1, -1);
			sourceViewer.configure(new MatlabSourceViewerConfiguration());
		}
		if(MathEnginePreferencesConstants.MATH_ENGINE_PYTHON.equals(mathEngine)) {
			
			FastPartitioner pythonFastPartitioner = new FastPartitioner(new PythonRulesPartitionScanner(), PythonRulesPartitionScanner.PARTITIONS);
		    document.setDocumentPartitioner(pythonFastPartitioner);
		    pythonFastPartitioner.connect(document);
			sourceViewer.setDocument(document, annotationModel, -1, -1);
			sourceViewer.configure(new PythonSourceViewerConfiguration());
		}
		
		
	}
	
	@Override
	protected void update(String code) {
//		code = code.replaceAll("^(\n)*", "");
//		code = "\n" + code;
//		System.out.println("update document...");
		sourceViewer.getDocument().set(code);
//		for (int i = 0; i < sourceViewer.getDocument().getLength(); i++) {
//			ITypedRegion typedRegion = sourceViewer.getDocument().getDocumentPartitioner().getPartition(i);			
//			System.out.println(i + " = " + typedRegion.getType());
//		}
	}

}
