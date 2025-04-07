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
package fr.univamu.ism.process;

import java.util.HashMap;
import java.util.Set;

/**
 * 
 * @author frank buloup
 *
 */
public class Function extends Block {
	
	private static final long serialVersionUID = 1L;

	public static final String FUNCTION_CLASS_NAME = "FUNCTION_CLASS_NAME";
	public static final String FUNCTION_NAME = "FUNCTION_NAME";
//	public static final String FUNCTION_TOOLTIP = "FUNCTION_TOOLTIP";
	public static final String FUNCTION_CODE = "FUNCTION_CODE";
	
	public static final String commentKey = "commentKey";
	
	private String className; 
	
	/* Used to store changes in order to run undo/redo - See : ModifyFunctionalBlockCommand */
	private transient HashMap<String, String> transientProperties = new HashMap<>();
	
	/* Used to store function properties */
	private HashMap<String, String> properties = new HashMap<>(0);
	
	public Function() {
		super();
	}
	
	public Function(Script script, String name) {
		super(script, name);
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	public Object getGUI(Object titleAreaDialog, Object parent, Object context) {
		return null;
	}
	
	public String getTitle(Object context) {
		return "";
	}
	
	public String getDescription(Object context) {
		return "";
	}
	
	public String getFunctionFileName() {
		return "";
	}
	
	@Override
	public String getName(Object context) {
		return getProperty(commentKey, super.getName(context));
	}
	
	@Override
	public void setName(String name) {
		setProperty(commentKey, name);
	}

	/**
	 * Returns the code associated with this bloc. This method is the
	 * implementation of the abstract super type for the function type
	 * @return the code
	 */
	@Override
	public String getCode(Object context, Object step, Object...objects) {
		if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
			if(context.getClass().getSimpleName().equals(Activator.ADWinProcess))
				return "\nREM Function " + getName(context) + "\n\n";
			if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess))
				return "\n// Function " + getName(context) + "\n\n";
		}
		
		return "";
	}
	
	@Override
	public Block clone() {
		Function function = new Function();
		clone(function);
//		try {
//			Class<?> clazz = Class.forName(className);
//			Constructor<?> constructor = clazz.getConstructor();
//			Function function = (Function) constructor.newInstance();
//			function.setSizeAndLocation(getSizeAndLocation());
//			function.setName(getName());
//			function.setClassName(getClassName());
//			Set<String> keys = properties.keySet();
//			for (String key : keys) {
//				String value = properties.get(key);
//				function.setProperty(key, value);
//			}
//			return function;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		return function;
	}
	
	@Override
	public void clone(Block cloneBlock) {
		Function function = (Function) cloneBlock;
		function.setSizeAndLocation(getSizeAndLocation());
		function.setName(name);
		function.setClassName(getClassName());
		Set<String> keys = properties.keySet();
		for (String key : keys) {
			String value = properties.get(key);
			function.setProperty(key, value);
		}
	}
	
	public void setProperty(String key, String value) {
		properties.put(key, value);
	}
	
	public HashMap<String, String> getProperties() {
		return properties;
	} 
	
	protected String getProperty(String key, String defaultValue) {
		String value = properties.get(key);
		if((value == null || "".equals(value)) && !"".equals(defaultValue)) {
			getTransientProperties().put(key, defaultValue);
		}
		return value == null ? defaultValue : value;
		
	}
	
	public HashMap<String, String> getTransientProperties() {
		if(transientProperties == null) transientProperties = new HashMap<>();
		return transientProperties;
	}

	

}
