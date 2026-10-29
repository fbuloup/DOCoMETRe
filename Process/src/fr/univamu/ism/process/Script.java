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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

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
	
	/**
	 * 
	 */
	private transient ArrayList<IStatus> codeGenerationStatus = new ArrayList<>(0);
	
	/**
	 * 
	 */
	private transient Object resource;
	/**
	 * 
	 */
	private transient boolean indentCode = true;
	
	/*
	 * Global declarations, see GlobalDeclarations. Only standard Java types are serialized so that 
	 * processes stay readable by previous versions. These fields are null in processes saved by previous versions.
	 */
	private ArrayList<String[]> globalLibraries;
	private ArrayList<String[]> globalConstants;
	private ArrayList<String[]> globalSubroutines;
	private transient GlobalDeclarations globalDeclarations;
	
	/*
	 * Code generation state, used to detect blocks structure errors (see generateSegmentCode())
	 */
	private static class GenerationFrame {
		// IF or DO block whose branch or body is being generated
		private Block owner;
		// Block ending this branch or body
		private Block stopBlock;
		private GenerationFrame(Block owner, Block stopBlock) {
			this.owner = owner;
			this.stopBlock = stopBlock;
		}
	}
	private transient Deque<GenerationFrame> generationFrames = new ArrayDeque<>();
	private transient Set<Block> generatedBlocks = new HashSet<>();
	private transient Set<Block> duplicatedBlocks = new HashSet<>();
	// IF and DO blocks already reported : a block generated several times must be reported once
	private transient Set<Block> reportedBlocks = new HashSet<>();
	// Blocks whose branch leaves an enclosing IF block, with their warning
	private transient Map<Block, IStatus> branchLeavesBlockStatus = new LinkedHashMap<>();
	
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
	
	public Object getResource() {
		return resource;
	}
	
	public void setResource(Object resource) {
		this.resource = resource;
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

	/**
	 * Global libraries, constants and subroutines of this script
	 */
	public GlobalDeclarations getGlobalDeclarations() {
		if(globalDeclarations == null) globalDeclarations = new GlobalDeclarations(this);
		return globalDeclarations;
	}
	
	List<String[]> getGlobalLibrariesValues() {
		if(globalLibraries == null) globalLibraries = new ArrayList<>();
		return globalLibraries;
	}
	
	List<String[]> getGlobalConstantsValues() {
		if(globalConstants == null) globalConstants = new ArrayList<>();
		return globalConstants;
	}
	
	List<String[]> getGlobalSubroutinesValues() {
		if(globalSubroutines == null) globalSubroutines = new ArrayList<>();
		return globalSubroutines;
	}
	
	void setGlobalDeclarationsValues(ArrayList<String[]> libraries, ArrayList<String[]> constants, ArrayList<String[]> subroutines) {
		globalLibraries = libraries;
		globalConstants = constants;
		globalSubroutines = subroutines;
		notifyBlocksListener();
	}
	
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
		List<Block> startBlocks = getStartBlocks(initializeBlocks);
		return startBlocks.isEmpty() ? null : startBlocks.get(0);
	}
	
	/**
	 * This method returns the code for the initialize phase
	 * @return the initialize code
	 */
	public String getInitializeCode(Object context, Object step) throws Exception {
		String code = generateSegmentCode(initializeBlocks, context, step);
		String initialIndent = "";
		if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess)) {
			if(!"DECLARATION".equals(step.toString())) initialIndent = "\t\t";
		}
		return indentCode(context, code, initialIndent);
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
		List<Block> startBlocks = getStartBlocks(loopBlocks);
		return startBlocks.isEmpty() ? null : startBlocks.get(0);
	}
	
	/**
	 * This method returns the code for the loop
	 * @return the loop code
	 */
	public String getLoopCode(Object context, Object step, Object...objects) throws Exception {
		String code = generateSegmentCode(loopBlocks, context, step, objects);
		String initialIndent = "";
		if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess)) {
			if(!"DECLARATION".equals(step.toString())) initialIndent = "\t\t";
		}
		return indentCode(context, code, initialIndent);
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
		List<Block> startBlocks = getStartBlocks(finalizeBlocks);
		return startBlocks.isEmpty() ? null : startBlocks.get(0);
	}
	
	/**
	 * This method returns the code for the finalize phase
	 * @return the finalize code
	 */
	public String getFinalizeCode(Object context, Object step) throws Exception {
		String code = generateSegmentCode(finalizeBlocks, context, step);
		String initialIndent = "";
		if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess)) {
			if(!"DECLARATION".equals(step.toString())) initialIndent = "\t\t";
		}
//		initialIndent =  step.equals(ArduinounoCodeSegmentProperties.DECLARATION)? initialIndent: "";
		return indentCode(context, code, initialIndent);
	}
	
	/*
	 * Start blocks of a segment : blocks without previous block, or DO blocks whose only previous block is their end block.
	 * Comments are never connected : they are not start blocks.
	 */
	private List<Block> getStartBlocks(ScriptSegment scriptSegment) {
		List<Block> startBlocks = new ArrayList<Block>(0);
		for (Block block : scriptSegment.getBlocks()) {
			if(block instanceof Comment) continue;
			if(block.getPreviousBlocks().size() == 0) startBlocks.add(block);
			else if(block instanceof DoBlock && block.getPreviousBlocks().size() == 1 && block.getPreviousBlocks().get(0).isLastBlockOf((DoBlock) block)) startBlocks.add(block);
		}
		return startBlocks;
	}
	
	/*
	 * Warnings must only be reported for script segments steps. Steps are compared by identity : 
	 * ADwin and Arduino Uno use their own steps whose names are the same as segments names 
	 * (e.g. INITIALIZATION, LOOP or FINALIZATION), so the same warning would be reported several times.
	 */
	private static boolean isScriptSegmentStep(Object step) {
		return step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE;
	}
	
	private String getBlockName(Block block, Object context) {
		return String.valueOf(block.getName(context)).replaceAll("\n", " ");
	}
	
	/*
	 * Generate code of a segment from its start block and warn about blocks structure errors :
	 * - several start blocks : only the first one is used, the others are ignored,
	 * - a branch leaves its IF block or its DO loop, loops overlap, or a block is generated several times : generated code is wrong.
	 */
	private String generateSegmentCode(ScriptSegment scriptSegment, Object context, Object step, Object...objects) {
		boolean displayWarning = isScriptSegmentStep(step);
		// IF and DO blocks set their status during generation, others blocks status are only set below
		for (Block block : scriptSegment.getBlocks()) block.setStatus(null);
		List<Block> startBlocks = getStartBlocks(scriptSegment);
		if(startBlocks.size() > 1) {
			String ignoredBlocks = "";
			for (int i = 1; i < startBlocks.size(); i++) ignoredBlocks = ignoredBlocks + (i > 1 ? ", " : "") + "\"" + getBlockName(startBlocks.get(i), context) + "\"";
			String message = NLS.bind(ProcessMessages.SeveralStartBlocks, new Object[] {step.toString(), getBlockName(startBlocks.get(0), context), ignoredBlocks});
			IStatus status = new Status(Status.WARNING, Activator.PLUGIN_ID, message);
			if(displayWarning) addGenerationCodeStatus(status);
			for (int i = 1; i < startBlocks.size(); i++) startBlocks.get(i).setStatus(status);
		}
		generationFrames = new ArrayDeque<>();
		generatedBlocks = new HashSet<>();
		duplicatedBlocks = new HashSet<>();
		reportedBlocks = new HashSet<>();
		branchLeavesBlockStatus = new LinkedHashMap<>();
		String code = generateCode(startBlocks.isEmpty() ? null : startBlocks.get(0), null, context, step, objects);
		// Statuses are set after generation : a block generated several times resets its own status
		for (Map.Entry<Block, IStatus> entry : branchLeavesBlockStatus.entrySet()) {
			if(displayWarning) addGenerationCodeStatus(entry.getValue());
			entry.getKey().setStatus(entry.getValue());
		}
		// Duplicated blocks are a consequence of a branch leaving its IF or DO block : warn only if not already done
		if(branchLeavesBlockStatus.isEmpty()) {
			for (Block block : duplicatedBlocks) {
				String message = NLS.bind(ProcessMessages.BlockGeneratedSeveralTimes, step.toString(), getBlockName(block, context));
				IStatus status = new Status(Status.WARNING, Activator.PLUGIN_ID, message);
				if(displayWarning) addGenerationCodeStatus(status);
				block.setStatus(status);
			}
		}
		return code;
	}
	
	/*
	 * Called each time a block is about to be generated
	 */
	private void checkBlockGeneration(Block block, Object context, Object step) {
		if(!generatedBlocks.add(block)) duplicatedBlocks.add(block);
		// Block ending an enclosing IF or DO block is reached from a nested IF or DO block : a branch leaves its block, or loops overlap
		GenerationFrame innerFrame = generationFrames.peek();
		for (GenerationFrame frame : generationFrames) {
			if(frame != innerFrame && frame.stopBlock == block) {
				if(branchLeavesBlockStatus.containsKey(innerFrame.owner)) return;
				String pattern = frame.owner instanceof DoBlock ? ProcessMessages.BranchLeavesDoBLock : ProcessMessages.BranchLeavesConditionalBlock;
				String message = NLS.bind(pattern, new Object[] {step.toString(), getBlockName(block, context), getBlockName(frame.owner, context), getBlockName(innerFrame.owner, context)});
				branchLeavesBlockStatus.put(innerFrame.owner, new Status(Status.WARNING, Activator.PLUGIN_ID, message));
				return;
			}
		}
	}
	
	/**
	 * Helper method to generate code.
	 * @param block the block for which we want to generate code
	 * @param stopBlock the block where we have to stop iterative generation
	 * @return the generated code
	 */
	private String generateCode(Block block, Block stopBlock, Object context, Object step, Object...objects) throws NullPointerException {
		boolean isScriptSegmentType = step == ScriptSegmentType.INITIALIZE || step == ScriptSegmentType.LOOP || step == ScriptSegmentType.FINALIZE;
		//if(!isScriptSegmentType) return "";
		String code = "";
		if(block == stopBlock) return "";
		if(block != null) checkBlockGeneration(block, context, step);
		boolean displayWarning = isScriptSegmentStep(step);
		if(block instanceof IfBlock /*&& isScriptSegmentType*/) {
			IfBlock ifBlock = (IfBlock)block;
			code = ifBlock.getCode(context, step, objects);
			displayWarning = displayWarning && !"".equals(code);
			if(ifBlock.getNextTrueBranchBlock() == null) {
				String message = NLS.bind(ProcessMessages.IfBlockWithoutTrueBranch, step.toString(), code.replaceAll("\n$", ""));
				IStatus status = new Status(Status.WARNING, Activator.PLUGIN_ID, message);
				if(displayWarning && reportedBlocks.add(ifBlock)) addGenerationCodeStatus(status);
				ifBlock.setStatus(status);
			} else if(ifBlock.getNextFalseBranchBlock() == null) {
				String message = NLS.bind(ProcessMessages.IfBlockWithoutFalseBranch, step.toString(), code.replaceAll("\n$", ""));
				IStatus status = new Status(Status.WARNING, Activator.PLUGIN_ID, message);
				if(displayWarning && reportedBlocks.add(ifBlock)) addGenerationCodeStatus(status);
				ifBlock.setStatus(status);
			} else ifBlock.setStatus(null);
			//Find endif bloc
			Block endifBlock = ifBlock.getEndBlock();
			//generate code till this endif bloc from true branch
			generationFrames.push(new GenerationFrame(ifBlock, endifBlock));
			code = code + generateCode(ifBlock.getNextTrueBranchBlock(), endifBlock, context, step, objects);
			if(context.getClass().getSimpleName().equals(Activator.ADWinProcess) && isScriptSegmentType) code = code + "ELSE\n";
			if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess) && isScriptSegmentType) code = code + "} else {\n";
			if(context.getClass().getSimpleName().equals(Activator.Script) && isScriptSegmentType) code = code + "else\n";
			//generate code till this endif bloc from false branch
			code = code + generateCode(ifBlock.getNextFalseBranchBlock(), endifBlock, context, step, objects);
			generationFrames.pop();
			if(context.getClass().getSimpleName().equals(Activator.ADWinProcess) && isScriptSegmentType) code = code + "ENDIF\n";
			if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess) && isScriptSegmentType) code = code + "} // End if .. else\n";
			if(context.getClass().getSimpleName().equals(Activator.Script) && isScriptSegmentType) code = code + "end\n";
			//Continue to next bloc
			code = code + generateCode(endifBlock, stopBlock, context, step, objects);
		}
		else if(block instanceof DoBlock /*&& isScriptSegmentType*/) {
			DoBlock doBlock = (DoBlock)block;
			if(context.getClass().getSimpleName().equals(Activator.ADWinProcess) && isScriptSegmentType) code = "DO\n";
			if(context.getClass().getSimpleName().equals(Activator.ArduinoUnoProcess) && isScriptSegmentType) code = "do {\n";
			displayWarning = displayWarning && !"".equals(code);
			//Find end do bloc
			Block endDoBlock = doBlock.getEndBlock();
			if(endDoBlock == null ) {
				String message = NLS.bind(ProcessMessages.DoBlockWithoutEndFunction, step.toString());
				IStatus status = new Status(Status.WARNING, Activator.PLUGIN_ID,  message);
				if(displayWarning && reportedBlocks.add(doBlock)) addGenerationCodeStatus(status);
				doBlock.setStatus(status);
			} else doBlock.setStatus(null);
			if(endDoBlock != null) {
				//generate code till this end do bloc
				generationFrames.push(new GenerationFrame(doBlock, endDoBlock));
				code = code + generateCode(doBlock.getNextBlock(), endDoBlock, context, step, objects);
				//generate code for this end do bloc
				code = code + generateCode(endDoBlock, endDoBlock.getNextBlock(), context, step, objects);
				generationFrames.pop();
			}
			//generate close do bloc
			code = code + doBlock.getCode(context, step, objects);
			//Continue to next bloc
			if(endDoBlock != null) {
				if(doBlock != endDoBlock.getNextBlock())
					code = code + generateCode(endDoBlock.getNextBlock(), stopBlock, context, step, objects);
				else { 
					String message = NLS.bind(ProcessMessages.DoBlockWithoutStartFunction, step.toString());
					IStatus status = new Status(Status.WARNING, Activator.PLUGIN_ID, message);
					if(displayWarning && reportedBlocks.add(doBlock)) addGenerationCodeStatus(status);
					doBlock.setStatus(status);
				}
			}
		} else {
			if(block != null) {
				Block nextBlock = block.getNextBlock();
				code = code + block.getCode(context, step, objects) + generateCode(nextBlock, stopBlock, context, step, objects);
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
