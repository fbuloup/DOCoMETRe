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

/**
 * The behavior of a module depends on its hardware and software. This interface aims to propose
 * a general behavior : a module must contract this interface. Therefore it must be prepared for
 * signal generation, processing and acquisition for a given process. And in case of a programmable 
 * DACQ system, it must returns its related code. In this last case a module is responsible to give segments 
 * of code in appropriated language. For instance it can give code for segments "initialization", 
 * "loop" and "finalization". It is the responsibility of concrete classes to document these code 
 * segments.
 */
public interface ModuleBehaviour {

	/**
	 *  This  returns the code segment
	 * @param segment corresponds to the different parts which forms the whole code 
	 * @return the ADBasic code which corresponds to the segment <i>segment</i>
	 */
	public String getCodeSegment(Object segment) throws Exception;
	
	/**
	 * This method must be call in order to the module to be prepare for signal acquisition.
	 * For instance, it can retrieve data linked to a module channel.
	 */
	public void recovery();
	
	/**
	 * This method must be called in order to the module to be prepare for signal generation.
	 * For instance, it can populate data linked to a module channel.
	 */
	public void generation();
	
	/**
	 * This method must be called to prepare the module before a process is started 
	 * @param process the process for which the module must be prepared
	 */
	public void open(Process process, String prefix, String suffix);
	
	/**
	 * This method must be called to clean the module when a process is finished 
	 */
	public void close();

	/**
	 * This method must be called to reset the module
	 */
	public void reset();
	
	/**
	 * Return the current process
	 * @return the process for which the module is currently running
	 */
	public Process getCurrentProcess();

}
