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

/**
 * This class is the If control structure.  
 * @author frank
 *
 */
public class IfBlock extends ConditionalBlock {
	
	/**
	 * Serial Id version
	 */
	private static final long serialVersionUID = 1L;
	
	//Constructors
	public IfBlock() {
		super();
	}
	
	public IfBlock(Script script, String name) {
		super(script, name);
	}
	
	/**
	 * This method sets the next block of this block in the false branch.
	 * @param block the next false branch block
	 */
	public void setNextFalseBranchBlock(Block block) {
		getNextBlocks()[1] = block;
		getNextBlocks()[1].addPreviousBlock(this, false);
	}
	
	/**
	 * This method sets the next block of this block in the true branch.
	 * @param block the next true branch block
	 */
	public void setNextTrueBranchBlock(Block block) {
		getNextBlocks()[0] = block;
		getNextBlocks()[0].addPreviousBlock(this, false);
	}

	/**
	 * This method returns the next false branch .
	 * @return the next false branch 
	 */
	public Block getNextFalseBranchBlock() {
		return getNextBlocks()[1];
	}
	
	/**
	 * This method returns the next true branch block.
	 * @return the next true branch block
	 */
	public Block getNextTrueBranchBlock() {
		return getNextBlocks()[0];
	}
	
	/**
	 * Helper method to find the end block of an if block
	 * @return the end block of the if block
	 */
	public Block getEndBlock() {
		return getEndBlock(getNextTrueBranchBlock(), getNextFalseBranchBlock());
	}
	
	/**
	 * Helper method to find the end block of an if block
	 * @param trueBranchBlock a block in the true branch
	 * @param falseBranchBlock a block in the false branch
	 * @return the end block of the if block
	 */
	private Block getEndBlock(Block trueBranchBlock, Block falseBranchBlock) {
		Block nextBlock = falseBranchBlock;
		while( (nextBlock != trueBranchBlock) && (nextBlock != null) ) {
			nextBlock = nextBlock.getNextBlock();
		}
		if(trueBranchBlock == nextBlock) return trueBranchBlock;
		else {
			nextBlock = trueBranchBlock.getNextBlock();
			if(nextBlock != null) return getEndBlock(nextBlock, falseBranchBlock);
		}
		return null;
	}
	
	/**
	 * Returns the code associated with this block. This method is the
	 * implementation of the abstract super type for the If block type
	 * @return the code
	 */
	@Override
	public String getCode(Object context, Object step, Object...objects) {
		if(step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE) {
			String contextSimpleName = context.getClass().getSimpleName();
			if(contextSimpleName.equals(Activator.ArduinoUnoProcess)) {
				String operator =  Operator.getOperatorForC(getOperator());
				return "if (" + getLeftOperand() + operator + getRightOperand() + ") {\n";
			}
			
			if(contextSimpleName.equals(Activator.ADWinProcess))
			return "IF (" + getLeftOperand() + Operator.getOperatorForADBasic(getOperator()) + getRightOperand() + ") THEN\n";
			
			if(contextSimpleName.equals(Activator.Script))
				return "if " + getLeftOperand() + getOperator().getValue() + getRightOperand() + "\n";
		}
		
		return "";
	}

	@Override
	public IfBlock clone() {
		IfBlock ifBlock = new IfBlock();
		ifBlock = (IfBlock) super.clone(ifBlock);
		return ifBlock;
	}
	
	@Override
	public String getName(Object context) {
		return "If " + getLeftOperand() + getOperator().getValue() + getRightOperand();
	}

	public void switchBranches() {
		// Save actual true branch block
		Block saveTrueBranchBlock = getNextBlocks()[0];
		// Replace true branch block by false branch block
		getNextBlocks()[0] = getNextBlocks()[1];
		// Replace false branch block by true branch block
		getNextBlocks()[1] = saveTrueBranchBlock;
	}

	@Override
	public void clone(Block cloneBlock) {
		// TODO Not used
	}
	
}
