package fr.univamu.ism.docometre.analyse.views;

import java.nio.file.Path;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;

public class FunctionsEditorLabelProvider extends LabelProvider {
	
	@SuppressWarnings("unchecked")
	@Override
	public String getText(Object element) {
		if(element instanceof Path) {
			return ((Path)element).getFileName().toString().replaceAll("\\..*$", "");
		}
		if(element instanceof Entry<?, ?>) {
			Entry<String, Path> entry = (Entry<String, Path>) element;
			return entry.getKey();
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if(element instanceof Path) {
			if(FunctionFactory.isCustomerFunction((Path)element))
			return  Activator.getImage(IImageKeys.CUSTOMER_FUNCTION_ICON);
			else return Activator.getImage(IImageKeys.FUNCTION_ICON);
		}
		return Activator.getImage(IImageKeys.FOLDER_ICON);
	}
	
}
