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
package fr.univamu.ism.docometre.analyse.datamodel;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;

public class BatchDataProcessing extends AbstractElement {
	
	public static final long serialVersionUID =  1L;
	
	private ArrayList<BatchDataProcessingItem> processes = new ArrayList<>();
	private ArrayList<BatchDataProcessingItem> subjects = new ArrayList<>();
	private BatchDataProcessingItem selectedProcess;
	private boolean runSingleProcess;
	
	public BatchDataProcessing() {
		BatchDataProcessingProperties.populateProperties(this);
	}
	
	public BatchDataProcessingItem getSelectedProcess() {
		return selectedProcess;
	}
	
	public void setSelectedProcess(BatchDataProcessingItem selectedProcess) {
		this.selectedProcess = selectedProcess;
	}
	
	public boolean isRunSingleProcess() {
		return runSingleProcess;
	}
	
	public void setRunSingleProcess(boolean runSingleProcess) {
		this.runSingleProcess = runSingleProcess;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return BatchDataProcessingProperties.cloneBatchDataProcessing(this);
	}

	@Override
	public void initializeObservers() {
		// TODO Auto-generated method stub

	}
	
	private String getPath(IResource resource) {
		return resource.getFullPath().toPortableString();
	}
	
	public boolean loadSubject() {
		return "true".equalsIgnoreCase(getProperty(BatchDataProcessingProperties.AUTO_LOAD_SUBJECT));
	}
	
	public boolean unloadSubject() {
		return "true".equalsIgnoreCase(getProperty(BatchDataProcessingProperties.AUTO_UNLOAD_SUBJECT));
	}
	
	//Processes
	public BatchDataProcessingItem addProcess(IResource process) {
		BatchDataProcessingItem batchDataProcessingItem = new BatchDataProcessingItem(getPath(process), true);
		processes.add(batchDataProcessingItem);
		return batchDataProcessingItem;
	}
	
	public BatchDataProcessingItem[] addProcesses(IResource[] processes) {
		BatchDataProcessingItem[] addedBatchDataProcessing = new BatchDataProcessingItem[processes.length];
		int i = 0;
		for (IResource process : processes) {
			BatchDataProcessingItem batchDataProcessingItem = addProcess(process);
			addedBatchDataProcessing[i] = batchDataProcessingItem;
			i++;
		}
		return addedBatchDataProcessing;
	}
	
	public void addProcesses(BatchDataProcessingItem[] items, int[] indexes) {
		int i = 0;
		for (BatchDataProcessingItem item : items) {
			processes.add(indexes[i], item);
			i++;
		}
	}
	
	public void addProcesses(BatchDataProcessingItem[] items) {
		for (BatchDataProcessingItem item : items) {
			processes.add(item);
		}
	}
	
	public void removeProcess(int index) {
		processes.remove(index);
	}
	
	public int[] removeProcesses(BatchDataProcessingItem[] batchDataProcessingItems) {
		int[] indexes = new int[batchDataProcessingItems.length]; 
		int i = 0;
		for (BatchDataProcessingItem batchDataProcessingItem : batchDataProcessingItems) {
			indexes[i] = processes.indexOf(batchDataProcessingItem);
			i++;
			processes.remove(batchDataProcessingItem);
		}
		return indexes;
	}
	
	public void moveProcessUp(int index) {
		if(index > 0) {
			BatchDataProcessingItem batchDataProcessingItem = processes.remove(index);
			index--;
			processes.add(index, batchDataProcessingItem);
		}
	}
	
	public void moveProcessUp(BatchDataProcessingItem batchDataProcessingItem) {
		int index = processes.indexOf(batchDataProcessingItem);
		moveProcessUp(index);
	}
	
	public void moveProcessDown(int index) {
		if(index < processes.size() - 1) {
			BatchDataProcessingItem batchDataProcessingItem = processes.remove(index);
			index++;
			processes.add(index, batchDataProcessingItem);
		}
	}
	
	public void moveProcessDown(BatchDataProcessingItem batchDataProcessingItem) {
		int index = processes.indexOf(batchDataProcessingItem);
		moveProcessDown(index);
	}
	
	public BatchDataProcessingItem[] getProcesses() {
		BatchDataProcessingItem[] items = processes.toArray(new BatchDataProcessingItem[processes.size()]);
		for (BatchDataProcessingItem item : items) {
			IResource process = ResourcesPlugin.getWorkspace().getRoot().findMember(item.getPath());
			if(process == null) processes.remove(item);
		}
		return processes.toArray(new BatchDataProcessingItem[processes.size()]);
	}
	
	public boolean canMoveProcessesUp(BatchDataProcessingItem[] items) {
		for (BatchDataProcessingItem item : items) {
			if(processes.indexOf(item) == 0) return false;
		}
		return true;
	}
	
	public boolean canMoveProcessesDown(BatchDataProcessingItem[] items) {
		for (BatchDataProcessingItem item : items) {
			if(processes.indexOf(item) == processes.size() - 1) return false;
		}
		return true;
	}
	
	// Subjects
	public BatchDataProcessingItem addSubject(IResource subject) {
		BatchDataProcessingItem batchDataProcessingItem = new BatchDataProcessingItem(getPath(subject), true);
		subjects.add(batchDataProcessingItem);
		return batchDataProcessingItem;
	}
	
	public BatchDataProcessingItem[] addSubjects(IResource[] subjects) {
		BatchDataProcessingItem[] addedBatchDataProcessing = new BatchDataProcessingItem[subjects.length];
		int i = 0;
		for (IResource subject : subjects) {
			BatchDataProcessingItem batchDataProcessingItem = addSubject(subject);
			addedBatchDataProcessing[i] = batchDataProcessingItem;
			i++;
		}
		return addedBatchDataProcessing;
	}
	
	public void addSubjects(BatchDataProcessingItem[] items) {
		for (BatchDataProcessingItem item : items) {
			subjects.add(item);
		}
	}
	
	public void addSubjects(BatchDataProcessingItem[] items, int[] indexes) {
		int i = 0;
		for (BatchDataProcessingItem item : items) {
			subjects.add(indexes[i], item);
			i++;
		}
	}
	
	public void removeSubject(int index) {
		subjects.remove(index);
	}
	
	public int[] removeSubjects(BatchDataProcessingItem[] batchDataProcessingItems) {
		int[] indexes = new int[batchDataProcessingItems.length]; 
		int i = 0;
		for (BatchDataProcessingItem batchDataProcessingItem : batchDataProcessingItems) {
			indexes[i] = subjects.indexOf(batchDataProcessingItem);
			i++;
			subjects.remove(batchDataProcessingItem);
		}
		return indexes;
	}
	
	public void moveSubjectUp(int index) {
		if(index > 0) {
			BatchDataProcessingItem batchDataProcessingItem = subjects.remove(index);
			index--;
			subjects.add(index, batchDataProcessingItem);
		}
	}
	
	public void moveSubjectUp(BatchDataProcessingItem batchDataProcessingItem) {
		int index = subjects.indexOf(batchDataProcessingItem);
		moveSubjectUp(index);
	}
	
	public void moveSubjectDown(int index) {
		if(index < subjects.size() - 1) {
			BatchDataProcessingItem batchDataProcessingItem = subjects.remove(index);
			index++;
			subjects.add(index, batchDataProcessingItem);
		}
	}
	
	public void moveSubjectDown(BatchDataProcessingItem batchDataProcessingItem) {
		int index = subjects.indexOf(batchDataProcessingItem);
		moveSubjectDown(index);
	}
	
	public BatchDataProcessingItem[] getSubjects() {
		BatchDataProcessingItem[] items = subjects.toArray(new BatchDataProcessingItem[subjects.size()]);
		for (BatchDataProcessingItem item : items) {
			IResource subject = ResourcesPlugin.getWorkspace().getRoot().findMember(item.getPath());
			if(subject == null) subjects.remove(item);
		}
		return subjects.toArray(new BatchDataProcessingItem[subjects.size()]);
	}

	public boolean canMoveSubjectsUp(BatchDataProcessingItem[] items) {
		for (BatchDataProcessingItem item : items) {
			if(subjects.indexOf(item) == 0) return false;
		}
		return true;
	}
	
	public boolean canMoveSubjectsDown(BatchDataProcessingItem[] items) {
		for (BatchDataProcessingItem item : items) {
			if(subjects.indexOf(item) == subjects.size() - 1) return false;
		}
		return true;
	}
	
}
