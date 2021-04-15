package fr.univamu.ism.docometre.analyse.datamodel;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.dacqsystems.Property;

public final class BatchDataProcessingProperties extends Property  {
	
	public static final BatchDataProcessingProperties AUTO_LOAD_SUBJECT = new BatchDataProcessingProperties("BatchDataProcessingProperties.AUTO_LOAD_SUBJECT", "", "", "^(true|false)$");
	public static final BatchDataProcessingProperties AUTO_UNLOAD_SUBJECT = new BatchDataProcessingProperties("BatchDataProcessingProperties.AUTO_UNLOAD_SUBJECT", "", "", "^(true|false)$");

	public static void populateProperties(BatchDataProcessing batchDataProcessing) {
		batchDataProcessing.setProperty(AUTO_LOAD_SUBJECT, "true");
		batchDataProcessing.setProperty(AUTO_UNLOAD_SUBJECT, "true");
	}
	
	public static BatchDataProcessing cloneBatchDataProcessing(BatchDataProcessing batchDataProcessing) {
		try {
			BatchDataProcessing clonedBatchDataProcessing = new BatchDataProcessing();
			clonedBatchDataProcessing.setProperty(AUTO_LOAD_SUBJECT, batchDataProcessing.getProperty(AUTO_LOAD_SUBJECT));
			clonedBatchDataProcessing.setProperty(AUTO_UNLOAD_SUBJECT, batchDataProcessing.getProperty(AUTO_UNLOAD_SUBJECT));
			BatchDataProcessingItem[] items = batchDataProcessing.getProcesses();
			BatchDataProcessingItem[] clonedItems = new BatchDataProcessingItem[items.length];
			int i = 0;
			for (BatchDataProcessingItem item : items) {
				BatchDataProcessingItem batchDataProcessingItem = (BatchDataProcessingItem) item.clone();
				clonedItems[i] = batchDataProcessingItem;
				i++;
			}
			clonedBatchDataProcessing.addProcesses(clonedItems);
			items = batchDataProcessing.getSubjects();
			clonedItems = new BatchDataProcessingItem[items.length];
			i = 0;
			for (BatchDataProcessingItem item : items) {
				BatchDataProcessingItem batchDataProcessingItem = (BatchDataProcessingItem) item.clone();
				clonedItems[i] = batchDataProcessingItem;
				i++;
			}
			clonedBatchDataProcessing.addSubjects(clonedItems);
			return clonedBatchDataProcessing;
		} catch (CloneNotSupportedException e) {
			Activator.logErrorMessageWithCause(e);
			e.printStackTrace();
		}
		return null;
		
	}
	
	public BatchDataProcessingProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}

}
