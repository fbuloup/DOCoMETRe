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
package fr.univamu.ism.docometre.analyse.editors;

import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreApplication;
import fr.univamu.ism.docometre.analyse.matlabeditor.MatlabRulesPartitionScanner;
import fr.univamu.ism.docometre.analyse.matlabeditor.MatlabSourceViewerConfiguration;
import fr.univamu.ism.docometre.analyse.pythoneditor.PythonRulesPartitionScanner;
import fr.univamu.ism.docometre.analyse.pythoneditor.PythonSourceViewerConfiguration;
import fr.univamu.ism.docometre.dacqsystems.ui.SourceEditor;
import fr.univamu.ism.docometre.preferences.GeneralPreferenceConstants;
import fr.univamu.ism.docometre.preferences.MathEnginePreferencesConstants;

public class DataProcessScriptSourceEditor extends SourceEditor {

	public DataProcessScriptSourceEditor(DataProcessEditor dataProcessEditor) {
		super(dataProcessEditor);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		fontSize = Activator.getDefault().getPreferenceStore().getInt(GeneralPreferenceConstants.EDITORS_FONT_SIZE);
		
		SourceViewerConfiguration sourceViewerConfiguration = null;
		
		String mathEngine = Activator.getDefault().getPreferenceStore().getString(MathEnginePreferencesConstants.MATH_ENGINE);
		if(MathEnginePreferencesConstants.MATH_ENGINE_MATLAB.equals(mathEngine)) {
			
			// See : https://github.com/amarbanerjee23/matclipse/tree/master/org.eclipselabs.matclipse.meditor/src/org/eclipselabs/matclipse/meditor/editors
			
			FastPartitioner matlabFastPartitioner = new FastPartitioner(new MatlabRulesPartitionScanner(), MatlabRulesPartitionScanner.PARTITIONS);
		    document.setDocumentPartitioner(matlabFastPartitioner);
		    matlabFastPartitioner.connect(document);
			sourceViewer.setDocument(document, annotationModel, -1, -1);
			sourceViewerConfiguration = new MatlabSourceViewerConfiguration(this);
		}
		if(MathEnginePreferencesConstants.MATH_ENGINE_PYTHON.equals(mathEngine)) {
			FastPartitioner pythonFastPartitioner = new FastPartitioner(new PythonRulesPartitionScanner(), PythonRulesPartitionScanner.PARTITIONS);
		    document.setDocumentPartitioner(pythonFastPartitioner);
		    pythonFastPartitioner.connect(document);
			sourceViewer.setDocument(document, annotationModel, -1, -1);
			sourceViewerConfiguration = new PythonSourceViewerConfiguration(this);
		}

		sourceViewer.configure(sourceViewerConfiguration);
		configureListener(sourceViewerConfiguration);
	}
	
	private void configureListener(SourceViewerConfiguration sourceViewerConfiguration) {
		sourceViewer.getTextWidget().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				boolean update = false;
				if (((event.stateMask & SWT.MOD1) == SWT.MOD1) && (event.keyCode == '=' || event.keyCode == 16777259)) {
					fontSize++;
					update = true;
				}
				if (((event.stateMask & SWT.MOD1) == SWT.MOD1) && (event.keyCode == '-' || event.keyCode == 16777261)) {
					fontSize = fontSize > 2 ? fontSize - 1 : 1;
					update = true;
				}
				if(update) {
					sourceViewer.getTextWidget().setFont(DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD, fontSize));
					lineNumberRulerColumn.setFont(DocometreApplication.getFont(DocometreApplication.COURIER_NEW_BOLD, fontSize));
					((Composite) sourceViewer.getControl()).layout(true);
					sourceViewer.setHyperlinkDetectors(null, 0);
					if (sourceViewerConfiguration instanceof MatlabSourceViewerConfiguration)
						sourceViewer.configure(new MatlabSourceViewerConfiguration(DataProcessScriptSourceEditor.this));
					if (sourceViewerConfiguration instanceof PythonSourceViewerConfiguration)
						sourceViewer.configure(new PythonSourceViewerConfiguration(DataProcessScriptSourceEditor.this));
				}
			}
		});
	}

}
