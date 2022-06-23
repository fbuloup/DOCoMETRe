package fr.univamu.ism.docometre.analyse.views;

import java.nio.file.Path;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;

public class FunctionsEditorLabelProvider extends LabelProvider {
	
	@Override
	public String getText(Object element) {
		if(element instanceof Path) {
			return ((Path)element).getFileName().toString().replaceAll("\\..*$", "");
		}
		return super.getText(element);
	}


	@Override
	public Image getImage(Object element) {
		if(element instanceof Path) {
			return Activator.getImage(IImageKeys.CUSTOMER_FUNCTION_ICON);
		}
		return Activator.getImage(IImageKeys.FOLDER_ICON);
	}
	
}
