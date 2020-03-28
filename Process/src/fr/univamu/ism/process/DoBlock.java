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

import java.util.List;

/**
 * This class is the DO control structure.  
 * @author frank buloup
 *
 */
public class DoBlock extends ConditionalBlock {

	/**
	 * Serial Id version
	 */
	private static final long serialVersionUID = 1L;

	//constructors
	public DoBlock() {
		super();
	}

	public DoBlock(Script script, String name) {
		super(script, name);
	}

	/**
	 * Helper method to find the end block of a do block
	 * @return the last block of the do block
	 */
	public Block getEndBlock() {
		List<Boolean> isEndBlock = getPreviousBlocksIsLastBlockInDoBlock();
		for (int i = 0; i < isEndBlock.size(); i++) {
			if(isEndBlock.get(i)) return getPreviousBlocks().get(i);
		}
		return null;
	}
	
	/**
	 * Helper method to set the end block of this do block
	 */
	public void setEndBlock(Block block) {
		List<Block> previousBlocks = getPreviousBlocks();
		for (int i = 0; i < previousBlocks.size(); i++) {
			if(previousBlocks.get(i) == block) {
				unsetEndBlock();
				getPreviousBlocksIsLastBlockInDoBlock().set(i, true);
				return;
			}
		}
		System.out.println(getPreviousBlocksIsLastBlockInDoBlock());
	}
	
	/**
	 * Helper method to unset the end block of this do block
	 */
	public void unsetEndBlock() {
		for (int i = 0; i < getPreviousBlocks().size(); i++) getPreviousBlocksIsLastBlockInDoBlock().set(i, false);
		System.out.println(getPreviousBlocksIsLastBlockInDoBlock());
	}
	
	/**
	 * Returns the code associated with this block. This method is the
	 * implementation of the abstract super type for the Do block type
	 * @return the code
	 */
	@Override
	public String getCode(Object context, Object step) {
		if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
			if(context.getClass().getSimpleName().equals(Activator.ADWinProcess))
				return "UNTIL (" + getLeftOperand() + getOperator().getValue() + getRightOperand() + ")\n";
			if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess)) {
				String operator =  getOperator() == Operator.IS_EQUAL_TO ? " == " : getOperator().getValue();
				return "} while (" + getLeftOperand() + operator + getRightOperand() + ");\n";
			}
				
		}
		return "";
	}
	
	@Override
	public DoBlock clone() {
		DoBlock doBlock = new DoBlock();
		doBlock = (DoBlock) super.clone(doBlock);
		return doBlock;
	}
	
	@Override
	public String getName(Object context) {
		if(context.getClass().getSimpleName().equals(Activator.ADWinProcess))
			return "Do ... Until " + getLeftOperand() + getOperator().getValue() + getRightOperand();
		if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess))
			return "Do ... While " + getLeftOperand() + getOperator().getValue() + getRightOperand();
		return "";
	}

	@Override
	public void clone(Block cloneBlock) {
		// TODO Not used
	}
	
}
