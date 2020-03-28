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
import fr.univamu.ism.process.Operator;
import fr.univamu.ism.process.Script;
import fr.univamu.ism.process.ScriptSegmentType;

/**
 * Sample test two
 * @author frank
 *
 */
public class SampleScript2 {
	
	private static Script script;
	
	public SampleScript2() {
		script = new Script();
		
		Block block1 = new Function(script, "Block 1");
		Block block2 = new Function(script, "Block 2");
		IfBlock ifBlock1 = new IfBlock(script, "If block 1");
		ifBlock1.setLeftOperand("EAN1");
		ifBlock1.setOperator(Operator.IS_GREATER_THAN);
		ifBlock1.setRightOperand("2");
		Block block3 = new Function(script, "block 3");
		Block block4 = new Function(script, "Block 4");
		Block block5 = new Function(script, "Block 5");
		DoBlock doBlock1 = new DoBlock(script, "Do block 1");
		doBlock1.setLeftOperand("LED1");
		doBlock1.setOperator(Operator.IS_EQUAL_TO);
		doBlock1.setRightOperand("1");
		Block block6 = new Function(script, "Block 6");
		IfBlock ifBlock2 = new IfBlock(script, "If block 2");
		ifBlock2.setLeftOperand("time");
		ifBlock2.setOperator(Operator.IS_LESS_THAN);
		ifBlock2.setRightOperand("10");
		Block block9 = new Function(script, "Block 9");
		Block block10 = new Function(script, "Block 10");
		Block block11 = new Function(script, "Block 11");
		Block block7 = new Function(script, "Block 7");
		Block block8 = new Function(script, "Block 8");
		
		block1.setNextBlock(block2);
		block2.setNextBlock(ifBlock1);
		ifBlock1.setNextTrueBranchBlock(block3);
		ifBlock1.setNextFalseBranchBlock(block4);
		block4.setNextBlock(block5);
		block3.setNextBlock(block5);
		block5.setNextBlock(doBlock1);
		doBlock1.setNextBlock(block6);
		block6.setNextBlock(ifBlock2);
		ifBlock2.setNextTrueBranchBlock(block9);
		ifBlock2.setNextFalseBranchBlock(block10);
		block9.setNextBlock(block11);
		block10.setNextBlock(block11);
		block11.setNextBlock(block7);
		block7.setNextBlock(doBlock1);
		doBlock1.setEndBlock(block7);
		block7.setNextBlock(block8);
		
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
		script.addLoopBlock(ifBlock1);
		script.addLoopBlock(ifBlock2);
		script.addLoopBlock(doBlock1);
		
	}
	
	/**
	 * Return the instance of sample script
	 * @return sample script instance
	 */
	public static Script getSampleScript() {
		if(script == null) new SampleScript2();
		return script;
	}
	
	public static void main(String[] args) {
		new SampleScript2();
		try {
			System.out.println(script.getLoopCode(null, ScriptSegmentType.LOOP));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
