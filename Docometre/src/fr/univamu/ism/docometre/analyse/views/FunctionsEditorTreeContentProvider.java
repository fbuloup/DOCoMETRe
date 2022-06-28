package fr.univamu.ism.docometre.analyse.views;

import java.nio.file.Path;
import java.util.Map.Entry;

import org.apache.commons.collections4.BidiMap;
import org.eclipse.jface.viewers.ITreeContentProvider;

public class FunctionsEditorTreeContentProvider implements ITreeContentProvider {
	
	private BidiMap<String, Path> values;

	@SuppressWarnings("unchecked")
	@Override
	public Object[] getElements(Object inputElement) {
		if(!(inputElement instanceof BidiMap<?, ?>)) return null;
		values = (BidiMap<String, Path>) inputElement;
		return values.entrySet().toArray();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object[] getChildren(Object parentElement) {
		if(!(parentElement instanceof Entry<?, ?>)) return null;
		Entry<String, Path> entry = (Entry<String, Path>) parentElement;
		if(!(entry.getValue() instanceof Path)) return null;
		Path path = (Path)entry.getValue();
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
		if(!(element instanceof String)) return null;
		return values.get(element);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean hasChildren(Object element) {
		if(!(element instanceof Entry<?, ?>)) return false;
		Entry<String, Path> entry = (Entry<String, Path>) element;
		Path path = (Path)entry.getValue();
		if(!path.toFile().isDirectory()) return false;
		return path.toFile().list().length > 0;
	}

}
