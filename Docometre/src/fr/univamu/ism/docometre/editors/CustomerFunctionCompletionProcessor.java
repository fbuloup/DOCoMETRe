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
		Template mt1 = new CustomerFunctionTemplate("MENU_TITLE", DocometreMessages.menuTitleDescription, 0);
		Template mt2 = new CustomerFunctionTemplate("DESCRIPTION", DocometreMessages.descriptionDescription, 0);
		Template mt3 = new CustomerFunctionTemplate("USER_FUNCTION", DocometreMessages.userFunctionDescription, "USER_FUNCTION = YES", 0);
		Template mt4 = new CustomerFunctionTemplate("PARAMETERS_NUMBER", DocometreMessages.parameterNumberDescription, 0);
		Template mt5 = new CustomerFunctionTemplate("PARAMETER_N", DocometreMessages.parameterNDescription, "PARAMETER_placeParameterNumberHere = NAME = placeParameterNameHere, TYPE = TEXT, LABEL = \"Place parameter label here :\"", 0);
		Template mt6 = new CustomerFunctionTemplate("FUNCTION_CODE", DocometreMessages.functionCodeDescription, 0);
		Template st1 = new CustomerFunctionTemplate("NAME", DocometreMessages.nameDescription, 1);
		Template st2 = new CustomerFunctionTemplate("TYPE", DocometreMessages.typeDescription, 1);
		Template st3 = new CustomerFunctionTemplate("TEXT", DocometreMessages.textTypeDescription, 1);
		Template st4 = new CustomerFunctionTemplate("LABEL", DocometreMessages.labelDescription, 1);
		return new Template[] {mt1, mt2, mt3, mt4, mt5, mt6, st1, st2, st3, st4};
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