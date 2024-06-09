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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import fr.univamu.ism.process.tests.SampleScript1;
import fr.univamu.ism.process.tests.SampleScript2;

/**
 * Helper class that serialize or deserialize script object
 * @author frank
 *
 */
public class ScriptBinarySerializer {
	
	/**
	 * Serialisation of a script object
	 * @param script the object to serialize
	 * @param file the destination file 
	 * @throws IOException
	 */
	public static void serialize(Script script, File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(script);
		oos.flush();
		oos.close();
	}
	
	/**
	 * Deserialization of a script object
	 * @param file the source file
	 * @return a Script instance
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Script deserialize(File file) throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		Script script = (Script) ois.readObject();
		ois.close();
		return script;
	}
	
	public static void main(String[] args) {
		//Test 1
		System.out.println("Test 1");
		try {
			System.out.println("BEFORE SERIALIZE");
			Script script = SampleScript1.getSampleScript();
			System.out.println(script.getInitializeCode(null, null));
			System.out.println(script.getLoopCode(null, null));
			System.out.println(script.getFinalizeCode(null, null));
			System.out.println("---------------");
			serialize(script, new File("./test1.bin"));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Script script = deserialize(new File("./test1.bin"));
			System.out.println("AFTER DESERIALIZE");
			System.out.println(script.getInitializeCode(null, null));
			System.out.println(script.getLoopCode(null, null));
			System.out.println(script.getFinalizeCode(null, null));
			System.out.println("---------------");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Test 2
		System.out.println("Test 2");
		try {
			System.out.println("BEFORE SERIALIZE");
			Script script = SampleScript2.getSampleScript();
			System.out.println(script.getInitializeCode(null, null));
			System.out.println(script.getLoopCode(null, null));
			System.out.println(script.getFinalizeCode(null, null));
			System.out.println("---------------");
			serialize(script, new File("./test2.bin"));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Script script = deserialize(new File("./test2.bin"));
			System.out.println("AFTER DESERIALIZE");
			System.out.println(script.getInitializeCode(null, null));
			System.out.println(script.getLoopCode(null, null));
			System.out.println(script.getFinalizeCode(null, null));
			System.out.println("---------------");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
