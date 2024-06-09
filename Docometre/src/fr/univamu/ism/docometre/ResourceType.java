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
package fr.univamu.ism.docometre;

import java.nio.file.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;

public enum ResourceType {
	
	ANY("ANY"),
	EXPERIMENT("EXPERIMENT"), 
	DACQ_CONFIGURATION("DACQ_CONFIGURATION"), 
	PROCESS("PROCESS"), 
	SUBJECT("SUBJECT"), 
	SESSION("SESSION"), 
	TRIAL("TRIAL"), 
	FOLDER("FOLDER"), 
	LOG("LOG"),
	SAMPLES("SAMPLES"),
	PARAMETERS("PARAMETERS"),
	PROCESS_TEST("PROCESS_TEST"),
	ADW_DATA_FILE("ADW_DATA_FILE"),
	CHANNEL("CHANNEL"),
	DATA_PROCESSING("DATA_PROCESSING"),
	BATCH_DATA_PROCESSING("BATCH_DATA_PROCESSING"),
	XYCHART("XYCHART"),
	XYZCHART("XYZCHART"),
	CUSTOMER_FUNCTION("CUSTOMER_FUNCTION"),
	OPTITRACK_TYPE_1("OPTITRACK_TYPE_1"),
	COLUMN_DATA_FILE("COLUMN_DATA_FILE");

	private String name = "";

	ResourceType(String name) {
	    this.name = name;
	}

	public String toString() {
		return name;
	}
	
	public static ResourceType getResourceType(IResource resource) {
		String typeValue = ResourceProperties.getTypePersistentProperty(resource); 
		if(typeValue == null) return ANY;
		if(typeValue.equals(EXPERIMENT.toString())) return EXPERIMENT;
		if(typeValue.equals(DACQ_CONFIGURATION.toString())) return DACQ_CONFIGURATION;
		if(typeValue.equals(PROCESS.toString())) return PROCESS;
		if(typeValue.equals(SUBJECT.toString())) return SUBJECT;
		if(typeValue.equals(SESSION.toString())) return SESSION;
		if(typeValue.equals(TRIAL.toString())) return TRIAL;
		if(typeValue.equals(FOLDER.toString())) return FOLDER;
		if(typeValue.equals(LOG.toString())) return LOG;
		if(typeValue.equals(SAMPLES.toString())) return SAMPLES;
		if(typeValue.equals(PARAMETERS.toString())) return PARAMETERS;
		if(typeValue.equals(PROCESS_TEST.toString())) return PROCESS_TEST;
		if(typeValue.equals(ADW_DATA_FILE.toString())) return ADW_DATA_FILE;
		if(typeValue.equals(CHANNEL.toString())) return CHANNEL;
		if(typeValue.equals(DATA_PROCESSING.toString())) return DATA_PROCESSING;
		if(typeValue.equals(BATCH_DATA_PROCESSING.toString())) return BATCH_DATA_PROCESSING;
		if(typeValue.equals(XYCHART.toString())) return XYCHART;
		if(typeValue.equals(XYZCHART.toString())) return XYZCHART;
		if(typeValue.equals(CUSTOMER_FUNCTION.toString())) return CUSTOMER_FUNCTION;
		if(typeValue.equals(OPTITRACK_TYPE_1.toString())) return OPTITRACK_TYPE_1;
		if(typeValue.equals(COLUMN_DATA_FILE.toString())) return COLUMN_DATA_FILE;
		return ANY;
	}
	
	public static boolean isExperiment(IResource resource) {
		return check(resource, EXPERIMENT);
	}
	
	public static boolean isDACQConfiguration(IResource resource) {
		return check(resource, DACQ_CONFIGURATION);
	}
	
	public static boolean isSubject(IResource resource) {
		return check(resource, SUBJECT);
	}
	
	public static boolean isSession(IResource resource) {
		return check(resource, SESSION);
	}
	
	public static boolean isTrial(IResource resource) {
		return check(resource, TRIAL);
	}
	
	public static boolean isFolder(IResource resource) {
		return check(resource, FOLDER);
	}
	
	public static boolean isProcess(IResource resource) {
		return check(resource, PROCESS);
	}
	
	public static boolean isLog(IResource resource) {
		return check(resource, LOG);
	}
	
	public static boolean isSamples(IResource resource) {
		return check(resource, SAMPLES);
	}
	
	public static boolean isParameters(IResource resource) {
		return check(resource, PARAMETERS);
	}
	
	public static boolean isProcessTest(IResource resource) {
		return check(resource, PROCESS_TEST);
	}
	
	public static boolean isADWDataFile(IResource resource) {
		return check(resource, ADW_DATA_FILE);
	}
	
	public static boolean isChannel(IResource resource) {
		return check(resource, CHANNEL);
	}
	
	public static boolean isDataProcessing(IResource resource) {
		return check(resource, DATA_PROCESSING);
	}
	
	public static boolean isBatchDataProcessing(IResource resource) {
		return check(resource, BATCH_DATA_PROCESSING);
	}
	
	public static boolean isXYChart(IResource resource) {
		return check(resource, XYCHART);
	}
	
	public static boolean isXYZChart(IResource resource) {
		return check(resource, XYZCHART);
	}
	
	public static boolean isCustomerFunction(Object resource) {
		if(resource instanceof IResource) return isCustomerFunction((IResource)resource);
		else if(resource instanceof Path)return isCustomerFunction((Path)resource);
		return false;
	}
	
	public static boolean isFunction(Object resource) {
		if(resource instanceof IResource) return isFunction((IResource)resource);
		else if(resource instanceof Path)return isFunction((Path)resource);
		return false;
	}
	
	private static boolean isCustomerFunction(IResource resource) {
		return check(resource, CUSTOMER_FUNCTION);
	}
	
	private static boolean isFunction(IResource resource) {
		boolean value = isCustomerFunction(resource);
		return  value || resource.getName().endsWith(Activator.customerFunctionFileExtension);
	}
	
	private static boolean isCustomerFunction(Path resource) {
		return FunctionFactory.isCustomerFunction(resource);
	}
	
	private static boolean isFunction(Path resource) {
		return  FunctionFactory.isFunction(resource);
	}
	
	public static boolean isOptitrack_Type_1(IResource resource) {
		return check(resource, OPTITRACK_TYPE_1);
	}
	
	public static boolean isColumnDataFile(IResource resource) {
		return check(resource, COLUMN_DATA_FILE);
	}
	
	public static boolean isAnyTest(IResource resource) {
	return check(resource, ANY);
	}
	
	public static boolean isContainer(IResource resource) {
		return resource instanceof IContainer;//isFolder(resource) || isExperiment(resource);
	}
	
	public static boolean areResourcesSameType(IResource r1, IResource r2) {
		ResourceType rType = getResourceType(r1);
		return check(r2, rType);
	}
	
	private static boolean check(IResource resource, ResourceType resourceType) {
		if(!resource.exists()) return false;
		return getResourceType(resource).equals(resourceType);
	}

	public static boolean isNumpyFile(IResource resource) {
		if(!resource.exists()) return false;
		if(resource.getFileExtension() == null) return false;
		return resource.getFileExtension().equalsIgnoreCase("numpy");
	}

	public static boolean isSaveFile(IResource resource) {
		if(!resource.exists()) return false;
		return resource.getName().equalsIgnoreCase("save.data") || resource.getName().equalsIgnoreCase("save.mat");
	}

	public static boolean isDataFile(IResource resource) {
		boolean isDataFile = ResourceType.isADWDataFile(resource) || ResourceType.isSamples(resource); 
		isDataFile = isDataFile || ResourceType.isSaveFile(resource) || ResourceType.isNumpyFile(resource);
		isDataFile = isDataFile || ResourceType.isOptitrack_Type_1(resource);
		isDataFile = isDataFile || ResourceType.isColumnDataFile(resource);
		return isDataFile;
	}

	
	
	
}
