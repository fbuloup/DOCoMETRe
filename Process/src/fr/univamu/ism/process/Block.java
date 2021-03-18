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
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.draw2d.geometry.Rectangle;

//Rule 1: if a normal block (not a conditional block) has two outgoing links, at least one of them must point to an ancestor DO block
//Rule 2: an IF block should always have two outgoing links, one on the wrong branch, the other on the true branch, even if one of these  branches is empty (does not containe any block)
//Rule 3: a block can have several incoming links
//Rule 4: an IF block cannot loop on an ancestor block

/**
 * This is the base abstract class used in Script. It can represent a block of type
 * IF or DO, or a function
 * @author frank buloup
 */
public abstract class Block implements Serializable {

	/**
	 * Serial Id version
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The name of the block. Can be used as a comment.
	 */
	protected String name;
	
	/**
	 * The x position of the block
	 */
	private int x = 0;
	
	/**
	 * The y position of the block
	 */
	private int y = 0;
	
	/**
	 * The width of the block
	 */
	private int width = 150;
	
	/**
	 * The height of the block
	 */
	private int height = 30;
	
	/**
	 * It true, this block will taken in account.
	 * It false, this block will be ignored.
	 */
	private boolean activated = true;
	
	/**
	 * The parent script that is the container of this block
	 */
	private Script script;
	
	/**
	 * This array holds next blocks of this block. Any Block has a maximum
	 * of two next blocks. If it is a DoBlock, it has only one next block.
	 * If it is an IfBlock, it has two next blocks, if it is a Function
	 * it can have two next blocks. For IfBlock, the block of index zero 
	 * is the next true branch block whereas the block of index one is 
	 * the next false branch block. When Function has two next blocks, 
	 * it means that this Function is part of a DoBlock as the last block
	 * of this loop and as a consequence one of these next blocs goes 
	 * to its parent DoBlock.
	 */
	private Block[] nextBlocks = new Block[] { null, null };
	
	/**
	 * This array list holds all the previous block of this block. Any block 
	 * can have any number of previous blocks.
	 */
	private List<Block> previousBlocks = new ArrayList<Block>(0);
	
	/**
	 * This boolean array list holds informations on the kinds of previous
	 * block of this block. When this information is true for an index i, it 
	 * tells that this block is a DoBlock and that the previous block of index 
	 * i is the last block of this do block.  
	 */
	private List<Boolean> previousBlocksIsLastBlockInDoBlock = new ArrayList<Boolean>(0);
	
	/**
	 * Listeners that will be notified when size or position change
	 */
	private transient List<SizeAndLocationListener> sizeAndLocationListeners = new ArrayList<SizeAndLocationListener>(0);
	
	private transient IStatus status;
	
	//Contructors
	public Block() {
		activated = true;
	}

	public Block(Script script, String name) {
		this();
		this.script = script;
		this.name = name;
	}
	
	//Getters and Setters
	public Script getScript() {
		return script;
	}

	public void setScript(Script script) {
		this.script = script;
	}
	
	public String getName(Object context) {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
	
	public boolean isActivated() {
		return activated;
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
	}

	protected List<Boolean> getPreviousBlocksIsLastBlockInDoBlock() {
		return previousBlocksIsLastBlockInDoBlock;
	}

//	protected void setPreviousBlocksIsLastBlockInDoBlock(List<Boolean> previousBlocksIsLastBlockInDoBlock) {
//		this.previousBlocksIsLastBlockInDoBlock = previousBlocksIsLastBlockInDoBlock;
//	}

	public Block[] getNextBlocks() {
		return nextBlocks;
	}
	
	public int getNextBlocksNumber() {
		int nbNextBlocks = 0;
		if(nextBlocks[0] != null) nbNextBlocks++;
		if(nextBlocks[1] != null) nbNextBlocks++;
		return nbNextBlocks;
	}
	
	protected void setNextBlocks(Block[] nextBlocks) {
		this.nextBlocks = nextBlocks;
	}

	public List<Block> getPreviousBlocks() {
		return previousBlocks;
	}

	public void setPreviousBlocks(List<Block> previousBlocks) {
		this.previousBlocks = previousBlocks;
	}
	
	//Logic methods
	/**
	 * This method returns the next block in the flow. It never returns a DoBlock
	 * if this block is contained in the DoBlock loop.
	 * @return the next block of this block in the flow
	 */
	public Block getNextBlock() {
		if (nextBlocks[0] != null) {
			if (nextBlocks[0] instanceof DoBlock) {
				if(!hasParent(nextBlocks[0])) return nextBlocks[0];
			} else return nextBlocks[0];
		}
		if (nextBlocks[1] != null) {
			if (nextBlocks[1] instanceof DoBlock) {
				if(!hasParent(nextBlocks[1])) return nextBlocks[1];
			} else return nextBlocks[1];
		}
		return null;
	}
	
	/**
	 * This method tells if the current block is the last block of the parameter do block
	 * @param doBlock the do block to be tested
	 * @return true if this block is last block of the parameter do block
	 */
	public boolean isLastBlockOf(DoBlock doBlock) {
		Block endBlock = doBlock.getEndBlock();
		return endBlock == this;
//		if(nextBlocks[0] != null && nextBlocks[0] instanceof DoBlock && hasParent(nextBlocks[0]) && nextBlocks[0] == doBlock) return true;
//		if(nextBlocks[1] != null && nextBlocks[1] instanceof DoBlock && hasParent(nextBlocks[1]) && nextBlocks[1] == doBlock) return true;
//		return false;
	}

	/**
	 * This method tells if this block has the parameter block as parent. The ways
	 * that are taken to find this parent block never goes through an end do block :
	 * if we want to see if a block is a parent of a do block, we do not take the
	 * ways that goes through the last block of the do block.
	 * @param parentBlock the block to be tested
	 * @return true if the parameter block is a parent of this block
	 */
	private boolean hasParent(Block parentBloc) {
		for (int i = 0; i < previousBlocks.size(); i++) {
			Block currentBloc = previousBlocks.get(i);
			boolean isLastBlocInDoBloc = previousBlocksIsLastBlockInDoBlock.get(i);
			if(!isLastBlocInDoBloc) {
				if (currentBloc == parentBloc)
					return true;
				else if (currentBloc.hasParent(parentBloc))
					return true;
			}
		}
		return false;
	}

	/**
	 * This method sets one of the two next block of this block to the block parameter.
	 * @param block the block which is the next block of this block
	 */
	public void setNextBlock(Block block) {
		setNextBlock(block, false);
//		if(block == nextBlocks[0]) return;
//		if(block == nextBlocks[1]) return;
//		if (nextBlocks[0] == null) {
//			nextBlocks[0] = block;
//			block.addPreviousBlock(this, false);
//		} else if (nextBlocks[1] == null) {
//			nextBlocks[1] = block;
//			block.addPreviousBlock(this, false);
//		}
	}
	
	public void setNextBlock(Block block, boolean isLastBlockInDoBlock) {
		if(block == nextBlocks[0]) return;
		if(block == nextBlocks[1]) return;
		if (nextBlocks[0] == null) {
			nextBlocks[0] = block;
			block.addPreviousBlock(this, isLastBlockInDoBlock);
		} else if (nextBlocks[1] == null) {
			nextBlocks[1] = block;
			block.addPreviousBlock(this, isLastBlockInDoBlock);
		}
	}

	/**
	 * This method adds a block as previous block of this block. if the block
	 * to add is the last block of a do block loop, second parameter must be
	 * set to true, which means that the current block is a do block.
	 * @param block the block to be added as previous block of this block
	 * @param isLastBlockInDoBlock whether or not the block to be added is the last block of this block. If true, this block is a do block
	 */
	public void addPreviousBlock(Block block, boolean isLastBlockInDoBlock) {
		if (!previousBlocks.contains(block)) {
			previousBlocks.add(block);
			previousBlocksIsLastBlockInDoBlock.add(isLastBlockInDoBlock);
		}
	}

	/**
	 * This method removes a block from one of the two next blocks of this block.
	 * It also removes this block from previous blocks of any of these next blocks. This is 
	 * very useful for blocks connection deletion.
	 * @param block the block to be removed
	 */
	public void removeNextBlock(Block block) {
		if (nextBlocks[0] == block) {
			nextBlocks[0].removePreviousBlock(this);
			nextBlocks[0] = null;

		}
		if (nextBlocks[1] == block) {
			nextBlocks[1].removePreviousBlock(this);
			nextBlocks[1] = null;
		}
	}

	/**
	 * This method removes a block from all the previous blocks of this block.
	 * @param block the block to be removed
	 */
	public void removePreviousBlock(Block block) {
		int index = previousBlocks.indexOf(block);
		if(index > -1) {
			previousBlocks.remove(index);
			previousBlocksIsLastBlockInDoBlock.remove(index);
		}
	}

	/**
	 * Returns the code associated with this block. This method is abstract
	 * as the generated code depends on the concrete type of the block
	 * @return the code
	 */
	public abstract String getCode(Object context, Object step);

	/**
	 * Helper method that return size and location as a draw2D rectangle
	 * @return the Draw2D rectangle that holds size and location
	 */
	public Rectangle getSizeAndLocation() {
		return new Rectangle(x, y, width, height);
	}
	
	/**
	 * Helper method that update size and position's block
	 * @param newSizeAndLocation a draw2D Rectangle that holds size and location
	 */
	public void setSizeAndLocation(Rectangle newSizeAndLocation) {
		setX(newSizeAndLocation.x);
		setY(newSizeAndLocation.y);
		setWidth(newSizeAndLocation.width);
		setHeight(newSizeAndLocation.height);
		notifySizeAndLocationListener();
	}

	/**
	 * This method will notify each size an location listener
	 */
	private void notifySizeAndLocationListener() {
		if(sizeAndLocationListeners == null) sizeAndLocationListeners = new ArrayList<SizeAndLocationListener>(0);
		for (int i = 0; i < sizeAndLocationListeners.size(); i++) sizeAndLocationListeners.get(i).updateSizeAndLocation();
	}

	/**
	 * Add a listener to the list of size and location listener
	 * @param listener the listener to be added
	 */
	public void addSizeAndLocationListener(SizeAndLocationListener listener) {
		if(sizeAndLocationListeners == null) sizeAndLocationListeners = new ArrayList<SizeAndLocationListener>(0);
		if(!sizeAndLocationListeners.contains(listener)) sizeAndLocationListeners.add(listener);
	}

	/**
	 * Remove a listener to the list of size and location listener
	 * @param listener the listener to be removed
	 */
	public void removeSizeAndLocationListener(SizeAndLocationListener listener) {
		if(sizeAndLocationListeners == null) sizeAndLocationListeners = new ArrayList<SizeAndLocationListener>(0);
		sizeAndLocationListeners.remove(listener);
	}
	 
	public abstract void clone(Block cloneBlock);
	
	public abstract Block clone();
	
	public void setStatus(IStatus status) {
		this.status = status;
	}
	
	public IStatus getStatus() {
		return status;
	}
	
}
