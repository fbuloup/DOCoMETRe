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
package fr.univamu.ism.docometre.dacqsystems;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import fr.univamu.ism.docometre.Activator;

/**
 * This abstract class manage a set of key/value properties
 */
public abstract class AbstractElement implements PropertyObservable, Serializable {
	
	public static final long serialVersionUID =  1L;
	
	/**
	 * This is a map of strings containing the key/value properties
	 */
	private HashMap<String,String> properties= new HashMap<String, String>(0);
	
	/**
	 * The list of properties observer. No duplicates are allowed.
	 */
	protected transient Set<PropertyObserver> propertiesObservers = new LinkedHashSet<PropertyObserver>(0);

	private transient IFile resource;
	
	private static transient boolean debuMessages = false;
	
	private void debugMessage(String message) {
		Activator.logInfoMessage(message, AbstractElement.class);
	}
	
	/**
	 * Add the observer to the list of properties observers
	 */
	public void addObserver(PropertyObserver observer) {
		if(propertiesObservers == null) propertiesObservers = new HashSet<PropertyObserver>(0);
		if(debuMessages) debugMessage("Adding observer : " + observer.getClass().getSimpleName() + " for " + getClass().getSimpleName());
		propertiesObservers.add(observer);
		if(debuMessages) debugMessage("Nb observers : " + propertiesObservers.size() + " for " + getClass().getSimpleName());
	}

	/**
	 * remove the observer to the list of properties observers
	 */
	public void removeObserver(PropertyObserver observer) {
		if(debuMessages) debugMessage("Removing observer : " + observer.getClass().getSimpleName() + " for " + getClass().getSimpleName());
//		System.out.println(propertiesObservers);
		if(propertiesObservers != null) propertiesObservers.remove(observer);
		if(debuMessages) debugMessage("Nb observers : " + ((propertiesObservers != null)?propertiesObservers.size():0) + " for " + getClass().getSimpleName());
	}
	
	/**
	 * @param key the key corresponding to the property
	 * @return the feature "value" corresponding to the property key
	 */
	public String getProperty(Property property) {
		return properties.get(property.getKey());
	}
	
	/**
	 * @param key the key corresponding to the property
	 * @param value the new value to assign to the feature "value" corresponding to the property key
	 */
	public void setProperty(Property property,String value) {
		String oldValue = getProperty(property);
		properties.put(property.getKey(), value);
		notifyObservers(property, value, oldValue);
	}
	
	@Override
	public String toString() {
		Set<String> keys = properties.keySet();
		StringBuffer stringBuffer = new StringBuffer();
		for (String key : keys) {
			stringBuffer.append(key.toString());
			stringBuffer.append(" : "); //$NON-NLS-1$
			stringBuffer.append(properties.get(key));
			stringBuffer.append(" - " + properties.get(key)); //$NON-NLS-1$
			stringBuffer.append("\n"); //$NON-NLS-1$
		}
		return stringBuffer.toString();
	}

	@Override
	public abstract Object clone() throws CloneNotSupportedException;
	
	protected void notifyObservers(Property property, Object newValue, Object oldValue) {
		if(propertiesObservers == null) propertiesObservers= new HashSet<PropertyObserver>(0);
		Object[] observers = propertiesObservers.toArray();
		if(debuMessages) debugMessage("Nb observers : " + propertiesObservers.size() + " for " + getClass().getSimpleName());
		for (int i = 0; i < observers.length; i++) {
			PropertyObserver observer = (PropertyObserver)observers[i];
			if(debuMessages) debugMessage("Notifying : " + observer.getClass().getSimpleName() + " for " + property.getClass().getSimpleName() + " - " + property.getKey());
			observer.update(property, newValue, oldValue, this);
		}
	}
	
	public abstract void initializeObservers();

	public void setResource(IFile file) {
		this.resource= file;
	}
	
	public IFile getResource() {
		return resource;
	}
	
}
