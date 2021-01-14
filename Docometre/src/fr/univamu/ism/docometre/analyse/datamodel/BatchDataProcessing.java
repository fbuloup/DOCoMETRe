package fr.univamu.ism.docometre.analyse.datamodel;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import fr.univamu.ism.docometre.dacqsystems.AbstractElement;

public class BatchDataProcessing extends AbstractElement {
	
	public static final long serialVersionUID =  1L;
	
	private ArrayList<BatchDataProcessingItem> processes = new ArrayList<>();
	private ArrayList<BatchDataProcessingItem> subjects = new ArrayList<>();
	
	public BatchDataProcessing() {
		BatchDataProcessingProperties.populateProperties(this);
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
