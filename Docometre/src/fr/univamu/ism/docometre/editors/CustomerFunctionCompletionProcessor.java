package fr.univamu.ism.docometre.editors;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.IImageKeys;


public class CustomerFunctionCompletionProcessor extends TemplateCompletionProcessor {

	public static String KEY = "KEY";
	
	@Override
	protected Template[] getTemplates(String contextTypeId) {
		Template menuTitle = new CustomerFunctionTemplate("MENU_TITLE", DocometreMessages.menuTitleDescription, 0);
		Template description = new CustomerFunctionTemplate("DESCRIPTION", DocometreMessages.descriptionDescription, 0);
		Template userFunction = new CustomerFunctionTemplate("USER_FUNCTION", DocometreMessages.userFunctionDescription, "USER_FUNCTION = YES", 0);
		Template parametersNumber = new CustomerFunctionTemplate("PARAMETERS_NUMBER", DocometreMessages.parameterNumberDescription, "PARAMETERS_NUMBER = 0", 0);
		Template parameter = new CustomerFunctionTemplate("PARAMETER_N", DocometreMessages.parameterNDescription, "PARAMETER_placeParameterNumberHere = NAME = placeParameterNameHere, TYPE = TEXT, LABEL = \"Place parameter label here :\"", 0);
		Template functionCode = new CustomerFunctionTemplate("FUNCTION_CODE", DocometreMessages.functionCodeDescription, 0);
		Template name = new CustomerFunctionTemplate("NAME", DocometreMessages.nameDescription, 1);
		Template type = new CustomerFunctionTemplate("TYPE", DocometreMessages.typeDescription, 1);
		Template text = new CustomerFunctionTemplate("TEXT", DocometreMessages.textTypeDescription, 1);
		Template text2 = new CustomerFunctionTemplate("TEXT with init.", DocometreMessages.text2TypeDescription, "TEXT[placeInitValueHere]", 1);
		Template text3 = new CustomerFunctionTemplate("TEXT integer numbers", DocometreMessages.text3TypeDescription, "TEXT[0]:[+-]?(\\d+)", 1);
		Template text4 = new CustomerFunctionTemplate("TEXT positive integer numbers", DocometreMessages.text4TypeDescription, "TEXT[0]:[+]?(\\d+)", 1);
		Template text5 = new CustomerFunctionTemplate("TEXT real numbers", DocometreMessages.text5TypeDescription, "TEXT[0.0]:[+-]?(\\d+\\.?\\d*|\\d*\\.\\d+)", 1);
		Template text6 = new CustomerFunctionTemplate("TEXT positive real numbers", DocometreMessages.text6TypeDescription, "TEXT[0.0]:[+]?(\\d+\\.?\\d*|\\d*\\.\\d+)", 1);
		Template folder = new CustomerFunctionTemplate("FOLDER", DocometreMessages.folderTypeDescription, "FOLDER", 1);
		Template folder2 = new CustomerFunctionTemplate("FOLDER Editable", DocometreMessages.folder2TypeDescription, "FOLDER:EDITABLE", 1);
		Template file = new CustomerFunctionTemplate("FILE", DocometreMessages.fileTypeDescription, "FILE", 1);
		Template file2 = new CustomerFunctionTemplate("FILE Editable", DocometreMessages.file2TypeDescription, "FILE:EDITABLE", 1);
		Template combo = new CustomerFunctionTemplate("COMBO", DocometreMessages.comboTypeDescription, "COMBO[value1]:(value1;value2;value3)", 1);
		Template label = new CustomerFunctionTemplate("LABEL", DocometreMessages.labelDescription, 1);
		return new Template[] {menuTitle, description, userFunction, parametersNumber, parameter, functionCode, name, type, text, text2, text3, text4, text5, text6, folder, folder2, file, file2, combo, label};
	}

	@Override
	protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
		return new TemplateContextType(KEY);
	}

	@Override
	protected Image getImage(Template template) {
		if(((CustomerFunctionTemplate)template).getLevel() == 1) return Activator.getImage(IImageKeys.INNER_PROPERTY_ICON);
		return Activator.getImage(IImageKeys.MAIN_PROPERTY_ICON);
	}
	
}