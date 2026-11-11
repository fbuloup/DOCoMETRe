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
package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;
import fr.univamu.ism.docometre.dacqsystems.ChannelProperties;
import fr.univamu.ism.docometre.dacqsystems.DACQConfiguration;
import fr.univamu.ism.docometre.dacqsystems.ParameterVariable;

public class ArduinoUnoVariable extends ArduinoUnoChannel implements ParameterVariable {

	public static final long serialVersionUID = AbstractElement.serialVersionUID;
	
	private DACQConfiguration dacqConfiguration;
	
	public ArduinoUnoVariable(DACQConfiguration dacqConfiguration) {
		super(null);
		this.dacqConfiguration = dacqConfiguration;
		ArduinoUnoVariableProperties.populateProperties(this);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return ArduinoUnoVariableProperties.cloneVariable(this);
	}

	public DACQConfiguration getDACQConfiguration() {
		return dacqConfiguration;
	}
	
	@Override
	public String getParameterName() {
		return getProperty(ChannelProperties.NAME);
	}
	
	// PARAMETER and PROPAGATE are missing in variables created before parameters management : null = false
	@Override
	public boolean isParameter() {
		return "true".equalsIgnoreCase(getProperty(ArduinoUnoVariableProperties.PARAMETER));
	}
	
	@Override
	public boolean isParameterPropagated() {
		return isParameter() && "true".equalsIgnoreCase(getProperty(ArduinoUnoVariableProperties.PROPAGATE));
	}
	
	@Override
	public boolean isStringParameter() {
		return isParameter() && isString();
	}
	
	@Override
	public String getParameterValue() {
		return getProperty(ArduinoUnoVariableProperties.PARAMETER_VALUE);
	}
	
	@Override
	public void setParameterValue(String value) {
		setProperty(ArduinoUnoVariableProperties.PARAMETER_VALUE, value);
	}
	
	@Override
	public boolean isValidParameterValue(String value) {
		return ArduinoUnoParameterValues.isValid(getProperty(ArduinoUnoVariableProperties.TYPE), getSize(), isInt32(), value);
	}
	
	public boolean isString() {
		return ArduinoUnoVariableProperties.STRING.equals(getProperty(ArduinoUnoVariableProperties.TYPE));
	}
	
	/**
	 * @return the size of the variable (maximum length for a string)
	 */
	public int getSize() {
		try {
			return Integer.parseInt(getProperty(ArduinoUnoVariableProperties.SIZE));
		} catch (NumberFormatException e) {
			return 1;
		}
	}
	
	/*
	 * int is on 32 bits for R4 WiFi (ARM), on 16 bits for R3 (AVR) and by default
	 */
	private boolean isInt32() {
		if(dacqConfiguration == null) return false;
		return ArduinoUnoDACQConfigurationProperties.REVISION_R4_WIFI.equals(dacqConfiguration.getProperty(ArduinoUnoDACQConfigurationProperties.REVISION));
	}

}
