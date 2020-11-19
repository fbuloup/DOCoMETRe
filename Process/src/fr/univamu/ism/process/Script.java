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

import java.io.Serializable;
import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import fr.univamu.ism.process.tests.SampleScript1;

/**
 * This is the base script class.
 * @author frank
 */
public class Script implements Serializable {
	
	/**
	 * Serial Id version
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The name of the script 
	 */
	private String name;
	
	/**
	 * List of blocks contained in the initialize phase
	 */
	private ScriptSegment initializeBlocks = new ScriptSegment(ScriptSegmentType.INITIALIZE, this);
	
	/**
	 * List of blocks contained in the loop phase
	 */
	private ScriptSegment loopBlocks = new ScriptSegment(ScriptSegmentType.LOOP, this);
	
	/**
	 * List of blocks contained in the finalize phase
	 */
	private ScriptSegment finalizeBlocks = new ScriptSegment(ScriptSegmentType.FINALIZE, this);

	/**
	 * 
	 */
	private transient ArrayList<BlocksListener> blocksListener = new ArrayList<BlocksListener>(0);
	
	private transient ArrayList<IStatus> codeGenerationStatus = new ArrayList<>(0);
	
	/**
	 * 
	 */
	private transient boolean indentCode = true;
	
	public static Script getSampleScript() {
		return SampleScript1.getSampleScript();
	}
	
	//constructors
	public Script() {
		indentCode = true;
	}
	
	public Script(String name) {
		this.name = name;
		indentCode = true;
	}
	
	//Getters and setters
	public String getName() {
		return name;
	}
	
	//
	public void setIndentCode(boolean indentCode) {
		this.indentCode = indentCode;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ScriptSegment getInitializeBlocksContainer() {
		return initializeBlocks;
	}

//	public void setInitializeBlocks(List<Block> initializetBlocks) {
//		this.initializeBlocks.setBlocks(initializetBlocks);
//	}

	public ScriptSegment getLoopBlocksContainer() {
		return loopBlocks;
	}

//	public void setLoopBlocks(ArrayList<Block> loopBlocks) {
//		this.loopBlocks.setBlocks(loopBlocks);
//	}

	public ScriptSegment getFinalizeBlocksContainer() {
		return finalizeBlocks;
	}

//	public void setFinalizeBlocks(ArrayList<Block> finalizeBlocks) {
//		this.finalizeBlocks.setBlocks(finalizeBlocks);
//	}

	//Logic methods
	/**
	 * Helper method which delete a connection between two blocks
	 * @param sourceBlock source block of the connection
	 * @param targetBlock target block of the connection
	 */
	public void removeBlocksConnection(Block sourceBlock, Block targetBlock) {
		sourceBlock.removeNextBlock(targetBlock);
//		targetBlock.removePreviousBlock(sourceBlock);
		notifyBlocksListener();
	}
	
	/**
	 * Helper method which add a connection between two blocks
	 * @param sourceBlock source block of the connection
	 * @param targetBlock target block of the connection
	 */
	public void addBlocksConnection(Block sourceBlock, Block targetBlock) {
		/*if(targetBlock instanceof DoBlock) sourceBlock.setNextDoBlock((DoBlock) targetBlock);
		else*/ sourceBlock.setNextBlock(targetBlock);
//		targetBlock.addPreviousBlock(sourceBlock, isLastBlockInDoBlock);
		notifyBlocksListener();
	}
	
	//Initialize phase
	/**
	 * This method adds a block to the initialize phase
	 * @param block the block to be added
	 */
	public void addInitializeBlock(Block block) {
		if(!initializeBlocks.getBlocks().contains(block)) {
			initializeBlocks.getBlocks().add(block);
			notifyBlocksListener();
		}
	}
	
	/**
	 * This method removes a block from the initialize phase
	 * @param block the block to be removed
	 */
	public void removeInitializeBlock(Block block) {
		if(initializeBlocks.getBlocks().remove(block)) {
			for (int i = 0; i < initializeBlocks.getBlocks().size(); i++) {
				Block currentBlock = initializeBlocks.getBlocks().get(i);
				currentBlock.removeNextBlock(block);
				currentBlock.removePreviousBlock(block);
			}
			notifyBlocksListener();
		}
	}
	
	/**
	 * This method returns the first block in the initialize phase
	 * @return the first loop block
	 */
	public Block getFirstInitializeBlock() {
		for (int i = 0; i < initializeBlocks.getBlocks().size(); i++) {
			Block currentBlock = initializeBlocks.getBlocks().get(i);
			if(currentBlock.getPreviousBlocks().size() == 0) return currentBlock;
			if(currentBlock instanceof DoBlock) {
				DoBlock doBlock = (DoBlock)currentBlock;
				if(doBlock.getPreviousBlocks().size() == 1) {
					Block previousBlock = doBlock.getPreviousBlocks().get(0);
					if(doBlock.getEndBlock() == previousBlock) return currentBlock;
				}
			}
		}
		return null;
	}
	
	/**
	 * This method returns the code for the initialize phase
	 * @return the initialize code
	 */
	public String getInitializeCode(Object context, Object step) throws Exception {
		Block firstBlock = getFirstInitializeBlock();
		String code = generateCode(firstBlock, null, context, step);
		return indentCode(context, code, (context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess))?"\t\t":"");
	}
	
	//Loop phase
	/**
	 * This method adds a block to the loop
	 * @param bloc the block to be added
	 */
	public void addLoopBlock(Block block) {
		if(!loopBlocks.getBlocks().contains(block)) {
			loopBlocks.getBlocks().add(block);
			notifyBlocksListener();
		}
	}
	
	/**
	 * This method removes a block from the loop
	 * @param bloc the block to be removed
	 */
	public void removeLoopBlock(Block block) {
		if(loopBlocks.getBlocks().remove(block)) {
			for (int i = 0; i < loopBlocks.getBlocks().size(); i++) {
				Block currentBlock = loopBlocks.getBlocks().get(i);
				currentBlock.removeNextBlock(block);
				currentBlock.removePreviousBlock(block);
			}
			notifyBlocksListener();
		}
	}
	
	/**
	 * This method returns the first block in the loop
	 * @return the first loop block
	 */
	public Block getFirstLoopBlock() {
		for (int i = 0; i < loopBlocks.getBlocks().size(); i++) {
			Block currentBlock = loopBlocks.getBlocks().get(i);
			if(currentBlock.getPreviousBlocks().size() == 0) return currentBlock;
			if(currentBlock instanceof DoBlock && currentBlock.getPreviousBlocks().size() == 1) {
				DoBlock doBlock = (DoBlock)currentBlock;
				Block previousBlock = currentBlock.getPreviousBlocks().get(0);
				if(previousBlock.isLastBlockOf(doBlock)) return doBlock;
			}
		}
		return null;
	}
	
	/**
	 * This method returns the code for the loop
	 * @return the loop code
	 */
	public String getLoopCode(Object context, Object step) throws Exception {
		Block firstBlock = getFirstLoopBlock();
		String code = generateCode(firstBlock, null, context, step);
		return indentCode(context, code, (context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess))?"\t\t\t\t\t\t":"");
	}
	
	//Finalize phase
	/**
	 * This method adds a block to the finalize phase
	 * @param block the block to be added
	 */
	public void addFinalizeBlock(Block block) {
		if(!finalizeBlocks.getBlocks().contains(block)) {
			finalizeBlocks.getBlocks().add(block);
			notifyBlocksListener();
		}
	}
	
	/**
	 * This method removes a block from the finalize phase
	 * @param block the block to be removed
	 */
	public void removeFinalizeBlock(Block block) {
		if(finalizeBlocks.getBlocks().remove(block)) {
			for (int i = 0; i < finalizeBlocks.getBlocks().size(); i++) {
				Block currentBlock = finalizeBlocks.getBlocks().get(i);
				currentBlock.removeNextBlock(block);
				currentBlock.removePreviousBlock(block);
			}
			notifyBlocksListener();
		}
	}
	
	/**
	 * This method returns the first block in the finalize phase
	 * @return the first loop block
	 */
	public Block getFirstFinalizeBlock() {
//		for (int i = 0; i < finalizeBlocks.getBlocks().size(); i++) {
//			Block currentBlock = finalizeBlocks.getBlocks().get(i);
//			if(currentBlock.getPreviousBlocks().size() == 0) return currentBlock;
//		}
//		return null;
		
		
		for (int i = 0; i < finalizeBlocks.getBlocks().size(); i++) {
			Block currentBlock = finalizeBlocks.getBlocks().get(i);
			if(currentBlock.getPreviousBlocks().size() == 0) return currentBlock;
			if(currentBlock instanceof DoBlock && currentBlock.getPreviousBlocks().size() == 1) {
				DoBlock doBlock = (DoBlock)currentBlock;
				Block previousBlock = currentBlock.getPreviousBlocks().get(0);
				if(previousBlock.isLastBlockOf(doBlock)) return doBlock;
			}
		}
		return null;
	}
	
	/**
	 * This method returns the code for the finalize phase
	 * @return the finalize code
	 */
	public String getFinalizeCode(Object context, Object step) throws Exception {
		Block firstBlock = getFirstFinalizeBlock();
		String code = generateCode(firstBlock, null, context, step);
		return indentCode(context, code, (context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess))?"\t\t":"");
	}
	
	/**
	 * Helper method to generate code.
	 * @param block the block for which we want to generate code
	 * @param stopBlock the block where we have to stop iterative generation
	 * @return the generated code
	 */
	private String generateCode(Block block, Block stopBlock, Object context, Object step) throws NullPointerException {
		boolean isScriptSegmentType = step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE;
		String code = "";
		if(block == stopBlock) return "";
		if(block instanceof IfBlock && isScriptSegmentType) {
			IfBlock ifBlock = (IfBlock)block;
			code = ifBlock.getCode(context, step);
			if(ifBlock.getNextTrueBranchBlock() == null) {
				IStatus status = new Status(Status.WARNING, Activator.PLUGIN_ID, "WARNING - In " + step.toString() + " segment => IF Block without true branch : \"" + code.replaceAll("\n$", "") + "\"");
				addGenerationCodeStatus(status);
				ifBlock.setStatus(status);
			} else if(ifBlock.getNextFalseBranchBlock() == null) {
				IStatus status = new Status(Status.WARNING, Activator.PLUGIN_ID, "WARNING - In " + step.toString() + " segment => IF Block without false branch : \"" + code.replaceAll("\n$", "") + "\"");
				addGenerationCodeStatus(status);
				ifBlock.setStatus(status);
			} else ifBlock.setStatus(null);
			//Find endif bloc
			Block endifBlock = ifBlock.getEndBlock();
			//generate code till this endif bloc from true branch
			code = code + generateCode(ifBlock.getNextTrueBranchBlock(), endifBlock, context, step);
			if(context.getClass().getSimpleName().equals(Activator.ADWinProcess)) code = code + "ELSE\n";
			if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess)) code = code + "} else {\n";
			if(context.getClass().getSimpleName().equals(Activator.Script)) code = code + "else\n";
			//generate code till this endif bloc from false branch
			code = code + generateCode(ifBlock.getNextFalseBranchBlock(), endifBlock, context, step);
			if(context.getClass().getSimpleName().equals(Activator.ADWinProcess)) code = code + "ENDIF\n";
			if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess)) code = code + "} // End if .. else\n";
			if(context.getClass().getSimpleName().equals(Activator.Script)) code = code + "end\n";
			//Continue to next bloc
			code = code + generateCode(endifBlock, stopBlock, context, step);
		}
		else if(block instanceof DoBlock && isScriptSegmentType) {
			DoBlock doBlock = (DoBlock)block;
			if(context.getClass().getSimpleName().equals(Activator.ADWinProcess)) code = "DO\n";
			if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess)) code = "do {\n";
			//Find end do bloc
			Block endDoBlock = doBlock.getEndBlock();
			if(endDoBlock == null ) {
				IStatus status = new Status(Status.WARNING, Activator.PLUGIN_ID, "WARNING - In " + step.toString() + " segment => DO Block without end block. Double click on proper incoming connection to specify the end block of this do block");
				addGenerationCodeStatus(status);
				doBlock.setStatus(status);
			} else doBlock.setStatus(null);
			if(endDoBlock != null) {
				//generate code till this end do bloc
				code = code + generateCode(doBlock.getNextBlock(), endDoBlock, context, step);
				//generate code for this end do bloc
				code = code + generateCode(endDoBlock, endDoBlock.getNextBlock(), context, step);
			}
			//generate close do bloc
			code = code + doBlock.getCode(context, step);
			//Continue to next bloc
			if(endDoBlock != null) code = code + generateCode(endDoBlock.getNextBlock(), stopBlock, context, step);
		} else {
			if(block != null) {
				Block nextBlock = block.getNextBlock();
				code = code + block.getCode(context, step) + generateCode(nextBlock, stopBlock, context, step);
			}
		}
		return code;
	}
	
	/**
	 * Helper method to indent code
	 * @param context 
	 * @param code the unindented code
	 * @return the indented code
	 */
	private String indentCode(Object context, String code, String initialIndent) {
		if(code.equals("")) return "";
		if(!indentCode) return code;
		String[] lines = code.split("\n");
		String indent = initialIndent;
		code = "";
		if(context.getClass().getSimpleName().equals(Activator.ADWinProcess)) {
			for (int i = 0; i < lines.length; i++) {
				if(lines[i].startsWith("IF") || lines[i].startsWith("DO") || lines[i].startsWith("FOR")) {
					code = code + indent + lines[i] + "\n";
					indent = indent + "\t";
				} else if(lines[i].startsWith("ELSE")) {
					code = code + indent.replaceFirst("\t", "") + lines[i] + "\n";
				} else if(lines[i].startsWith("ENDIF") || lines[i].startsWith("UNTIL") || lines[i].startsWith("NEXT")) {
					indent = indent.replaceFirst("\t", "");
					code = code + indent + lines[i] + "\n";
				} else code = code + indent + lines[i] + "\n";
			}
		}
		if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess)) {
			for (int i = 0; i < lines.length; i++) {
				if(lines[i].startsWith("if (") || lines[i].startsWith("do {") || lines[i].startsWith("for (")) {
					code = code + indent + lines[i] + "\n";
					indent = indent + "\t";
				} else if(lines[i].startsWith("} else {")) {
					code = code + indent.replaceFirst("\t", "") + lines[i] + "\n";
				} else if(lines[i].startsWith("} // End if .. else") || lines[i].startsWith("} while (") || lines[i].startsWith("} // End for")) {
					indent = indent.replaceFirst("\t", "");
					code = code + indent + lines[i] + "\n";
				} else code = code + indent + lines[i] + "\n";
			}
		}
		if(context.getClass().getSimpleName().equals(Activator.Script)) {
			for (int i = 0; i < lines.length; i++) {
				if(lines[i].startsWith("if ") || lines[i].startsWith("while ") || lines[i].startsWith("for ")) {
					code = code + indent + lines[i] + "\n";
					indent = indent + "\t";
				} else if(lines[i].startsWith("else")) {
					code = code + indent.replaceFirst("\t", "") + lines[i] + "\n";
				} else if(lines[i].startsWith("end")) {
					indent = indent.replaceFirst("\t", "");
					code = code + indent + lines[i] + "\n";
				} else code = code + indent + lines[i] + "\n";
			}
		}
		return code;
	}
	
	public void selectBlocks(Block[] blocks) {
		if(blocksListener == null) blocksListener = new ArrayList<BlocksListener>(0);
		for (int i = 0; i < blocksListener.size(); i++) blocksListener.get(i).selectBlocks(blocks);
	}
	
	/**
	 * This method will notify each size an location listener
	 */
	private void notifyBlocksListener() {
		if(blocksListener == null) blocksListener = new ArrayList<BlocksListener>(0);
		for (int i = 0; i < blocksListener.size(); i++) {
			blocksListener.get(i).updateBlocks();
		}
	}

	
	/**
	 * Add a listener to the list of blocs listener
	 * @param listener the listener to be added
	 */
	public void addBlocksListener(BlocksListener listener) {
		if(blocksListener == null) blocksListener = new ArrayList<BlocksListener>(0);
		blocksListener.add(listener);
	}
	
	/**
	 * Remove a listener to the list of blocs listener
	 * @param listener the listener to be removed
	 */
	public void removeBlocksListener(BlocksListener listener) {
		if(blocksListener == null) blocksListener = new ArrayList<BlocksListener>(0);
		blocksListener.remove(listener);
	}
	
	private void addGenerationCodeStatus(IStatus status) {
		if(codeGenerationStatus == null) codeGenerationStatus = new ArrayList<>(0);
		codeGenerationStatus.add(status);
	}

	public IStatus[] getCodeGenerationStatus() {
		if(codeGenerationStatus == null) codeGenerationStatus = new ArrayList<>(0);
		return codeGenerationStatus.toArray(new IStatus[codeGenerationStatus.size()]);
	}

	public void clearCodeGenerationStatus() {
		if(codeGenerationStatus == null) return;
		codeGenerationStatus.clear();
	}

	
	
}
