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
package fr.univamu.ism.process.tests;

import fr.univamu.ism.process.Block;
import fr.univamu.ism.process.DoBlock;
import fr.univamu.ism.process.Function;
import fr.univamu.ism.process.IfBlock;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegmentType;

/**
 * Sample test one
 * @author frank
 *
 */
public class SampleScript1 {
	
	private static Script script;
	
	public SampleScript1() {
		script = new Script();
		
		Block block1 = new Function(script, "Function 1");
		Block block2 = new Function(script, "Function 2");
		Block block3 = new Function(script, "Function 3");
		Block block4 = new Function(script, "Function 4");
		Block block5 = new Function(script, "Function 5");
		Block block6 = new Function(script, "Function 6");
		Block block7 = new Function(script, "Function 7");
		Block block8 = new Function(script, "Function 8");
		Block block9 = new Function(script, "Function 9");
		Block block10 = new Function(script, "Function 10");
		Block block11 = new Function(script, "Function 11");
		Block block12 = new Function(script, "Function 12");
		Block block13 = new Function(script, "Function 13");
		IfBlock ifBlock1 = new IfBlock(script, "If block 1");
		IfBlock ifBlock2 = new IfBlock(script, "If block 2");
		IfBlock ifBlock3 = new IfBlock(script, "If block 3");
		IfBlock ifBlock4 = new IfBlock(script, "If block 4");
		DoBlock doBlock1 = new DoBlock(script, "Do block 1");
		DoBlock doBlock2 = new DoBlock(script, "Do block 2");
		DoBlock doBlock3 = new DoBlock(script, "Do block 3");
		DoBlock doBlock4 = new DoBlock(script, "Do block 4");
		
		block1.setNextBlock(block2);
		block2.setNextBlock(ifBlock1);
		ifBlock1.setNextFalseBranchBlock(block3);
		ifBlock1.setNextTrueBranchBlock(ifBlock2);
		block3.setNextBlock(ifBlock3);
		ifBlock3.setNextFalseBranchBlock(doBlock2);
		ifBlock3.setNextTrueBranchBlock(doBlock3);
		doBlock2.setNextBlock(doBlock4);
		doBlock4.setNextBlock(block8);
		block8.setNextBlock(doBlock4);
		doBlock4.setEndBlock(block8);
		block8.setNextBlock(block9);
		block9.setNextBlock(doBlock2);
		doBlock2.setEndBlock(block9);
		block9.setNextBlock(block10);
		doBlock3.setNextBlock(block4);
		block4.setNextBlock(doBlock3);
		doBlock3.setEndBlock(block4);
		block4.setNextBlock(block10);
		ifBlock2.setNextFalseBranchBlock(doBlock1);
		ifBlock2.setNextTrueBranchBlock(block11);
		doBlock1.setNextBlock(ifBlock4);
		ifBlock4.setNextFalseBranchBlock(block5);
		ifBlock4.setNextTrueBranchBlock(block6);
		block5.setNextBlock(block7);
		block6.setNextBlock(block7);
		block7.setNextBlock(doBlock1);
		doBlock1.setEndBlock(block7);
		block7.setNextBlock(block11);
		block10.setNextBlock(block12);
		block11.setNextBlock(block12);
		block12.setNextBlock(block13);
		
		script.addLoopBlock(block1);
		script.addLoopBlock(block2);
		script.addLoopBlock(block3);
		script.addLoopBlock(block4);
		script.addLoopBlock(block5);
		script.addLoopBlock(block6);
		script.addLoopBlock(block7);
		script.addLoopBlock(block8);
		script.addLoopBlock(block9);
		script.addLoopBlock(block10);
		script.addLoopBlock(block11);
		script.addLoopBlock(block12);
		script.addLoopBlock(block13);
		script.addLoopBlock(ifBlock1);
		script.addLoopBlock(ifBlock2);
		script.addLoopBlock(ifBlock3);
		script.addLoopBlock(ifBlock4);
		script.addLoopBlock(doBlock1);
		script.addLoopBlock(doBlock2);
		script.addLoopBlock(doBlock3);
		script.addLoopBlock(doBlock4);
		
	}
	
	/**
	 * Return the instance of sample script
	 * @return sample script instance
	 */
	public static Script getSampleScript() {
		if(script == null) new SampleScript1();
		return script;
	}
	
	public static void main(String[] args) {
		new SampleScript1();
		try {
			System.out.println(script.getLoopCode(null, ScriptSegmentType.LOOP));
		} catch (Exception e) {
			e.printStackTrace();
		};
	}

}
