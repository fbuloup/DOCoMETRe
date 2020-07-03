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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {// The plug-in ID
	
	public static final String PLUGIN_ID = "Docometre.process"; //$NON-NLS-1$
	
	/*
	 * Place here all process simple class name in order to take
	 * system specificity in account without links creation to 
	 * Docometre main plugin 
	 */
	public static final String ADWinProcess = "ADWinProcess";
	public static final String ArduinoUnoProcess = "ArduinoUnoProcess";
	public static final String Script = "Script";

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}
	
	public static ArrayList<Block> clone(ArrayList<Block> blocks) {
		HashMap<Block, Block> originalAndClonedBlocks = new HashMap<>();
		
		// First pass : clone blocks
		for (Block block : blocks) {
			originalAndClonedBlocks.put(block, block.clone());
		}
		
		//Second pass : report links
		for (Block block : blocks) {
			Block[] nextBlocks = block.getNextBlocks();
			for (int i = 0; i < nextBlocks.length; i++) {
				if(nextBlocks[i] != null) {
					Block clonedNextBlock = originalAndClonedBlocks.get(nextBlocks[i]);
					if(clonedNextBlock != null) {
						Block clonedBlock = originalAndClonedBlocks.get(block);
						clonedBlock.setNextBlock(clonedNextBlock);
						if(clonedNextBlock instanceof DoBlock) {
							if(block.isLastBlockOf((DoBlock) nextBlocks[i])) {
								((DoBlock)clonedNextBlock).setEndBlock(clonedBlock);
							}
						}
					}
				}
			}
		}
		
		ArrayList<Block> clonedBlocks = new ArrayList<>();
		for(Map.Entry<Block,Block> entrySet : originalAndClonedBlocks.entrySet()) clonedBlocks.add(entrySet.getValue());
		return clonedBlocks;
	}

}
