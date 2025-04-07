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
package fr.univamu.ism.docometre.matlab;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import matlabcontrol.MatlabProxyFactoryOptions.Builder;
import matlabcontrol.extensions.MatlabNumericArray;
import matlabcontrol.extensions.MatlabTypeConverter;

public final class MatlabController {
	
	private static MatlabController maltabController;
	
	public static MatlabController getInstance() {
		if(maltabController == null) maltabController = new MatlabController();
		return maltabController;
	}

	private MatlabProxy matlabProxy;
	
	private MatlabController() {
	}
	
	public void startMatlab(boolean showWindow, int timeOut, String matlabLocation, String matlabScriptsLocation,  String matlabFunctionsLocation, String licence) throws Exception {
		
		if(matlabProxy != null) return;
		
		// Get current context class loader 
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader(); 

		try {
			// Set class loader 
			Thread.currentThread().setContextClassLoader(maltabController.getClass().getClassLoader()); 
			
			// Try to start Matlab
			Builder matlabBuilder = new Builder();
			matlabBuilder.setUsePreviouslyControlledSession(true);
			matlabBuilder.setHidden(!showWindow);
			matlabBuilder.setProxyTimeout(timeOut*1000);
			if(matlabLocation != null && !matlabLocation.equals("")) matlabBuilder.setMatlabLocation(matlabLocation);
			if(licence != null && !licence.equals("")) matlabBuilder.setLicenseFile(licence);
			MatlabProxyFactoryOptions matlabOptions = matlabBuilder.build();
			MatlabProxyFactory matlabProxyFactory = new MatlabProxyFactory(matlabOptions);
			matlabProxy = matlabProxyFactory.getProxy();
			if(matlabProxy.isExistingSession()) matlabProxy.eval("clear all;");
			if(matlabScriptsLocation != null && !matlabScriptsLocation.equals("")) matlabProxy.eval("addpath('" + matlabScriptsLocation + "');");
			if(matlabFunctionsLocation != null && !matlabFunctionsLocation.equals("")) matlabProxy.eval("addpath('" + matlabFunctionsLocation + "');");
			
		} finally {
			// Return to current class loader 
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
		
	}
	
	public void stopMatlab() throws Exception {
		if(matlabProxy != null) {
			matlabProxy.exit();
			matlabProxy.disconnect();
		}
		matlabProxy = null;
	}
	
	public boolean isStarted() {
		return matlabProxy != null;
	}
	
	public Object[] returningEval(String cmd, int nbParameters) throws Exception {
		if(isStarted()) return matlabProxy.returningEval(cmd, nbParameters);
		return null;
	}
	
	public void eval(String cmd) throws Exception {
		if(isStarted()) matlabProxy.eval(cmd);
		//else throw new Exception("Matlab is not running !");
	}
	
	public void evaluate(String command) throws Exception {
			eval(command);
	}
	
	public Object getVariable(String variableName) throws Exception {
		if(isStarted()) 
				return matlabProxy.getVariable(variableName);
		return null;
	}
	
	public double[][] getVariable2DArray(String arrayName) throws Exception {
		if(isStarted())  {
				MatlabTypeConverter processor = new MatlabTypeConverter(matlabProxy);
				MatlabNumericArray array = processor.getNumericArray(arrayName);
				return array.getRealArray2D();
			} 
		return null;
	}

}
