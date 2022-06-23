package fr.univamu.ism.docometre.analyse.views;

import java.nio.file.Path;

import org.apache.commons.collections4.BidiMap;
import org.eclipse.jface.viewers.ITreeContentProvider;

public class FunctionsEditorTreeContentProvider implements ITreeContentProvider {
	
	private BidiMap<String, Path> values;

	@SuppressWarnings("unchecked")
	@Override
	public Object[] getElements(Object inputElement) {
		if(!(inputElement instanceof BidiMap<?, ?>)) return null;
		values = (BidiMap<String, Path>) inputElement;
		return values.keySet().toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if(!(parentElement instanceof String)) return null;
		Path path = values.get(parentElement);
		String[] filesList = path.toFile().list();
		if(filesList == null) return null;
		Path[] filesPath = new Path[filesList.length];
		for (int i = 0; i < filesPath.length; i++) {
			filesPath[i] = path.resolve(filesList[i]);
		}
	    return filesPath;
	}

	@Override
	public Object getParent(Object element) {
		if(!(element instanceof Path)) return null;
		return values.getKey(element);
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof String;
	}

}
