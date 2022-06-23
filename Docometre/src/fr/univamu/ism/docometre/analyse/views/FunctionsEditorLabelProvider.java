package fr.univamu.ism.docometre.analyse.views;

import java.nio.file.Path;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.IImageKeys;
import fr.univamu.ism.docometre.scripteditor.actions.FunctionFactory;

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
			if(FunctionFactory.isCustomerFunction((Path)element))
			return  Activator.getImage(IImageKeys.CUSTOMER_FUNCTION_EDITABLE_ICON);
			else return Activator.getImage(IImageKeys.CUSTOMER_FUNCTION_ICON);
		}
		return Activator.getImage(IImageKeys.FOLDER_ICON);
	}
	
}
