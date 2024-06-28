package fr.univamu.ism.docometre.consoles;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;

import fr.univamu.ism.docometre.Activator;

public class DocometreConsole extends MessageConsole implements ILogListener {
	
	private static Color WARNING_COLOR = JFaceResources.getResources().createColor(new RGB(204,102,0));
	private static Color ERROR_COLOR = JFaceResources.getResources().createColor(new RGB(250, 0, 0));
	private static Color INFO_COLOR_DARK = JFaceResources.getResources().createColor(new RGB(255, 255, 255));
	private static Color INFO_COLOR = JFaceResources.getResources().createColor(new RGB(0, 0, 0));
	private static String infoColorCodeConsole;
	private static String warninColorCodeConsole;
	private static String errorColorCodeConsole;

	public DocometreConsole(String name, String consoleType, ImageDescriptor imageDescriptor, String encoding, boolean autoLifecycle) {
		super(name, consoleType, imageDescriptor, encoding, autoLifecycle);
		Activator.getDefault().getLog().addLogListener(this);
		if(Display.isSystemDarkTheme()) {
			infoColorCodeConsole = "\u001b[38;2;" + INFO_COLOR_DARK.getRed() + ";" + INFO_COLOR_DARK.getGreen() + ";" + INFO_COLOR_DARK.getBlue() + "m";
			warninColorCodeConsole = "\u001b[38;2;" + WARNING_COLOR.getRed() + ";" + WARNING_COLOR.getGreen() + ";" + WARNING_COLOR.getBlue() + "m";
			errorColorCodeConsole = "\u001b[38;2;" + ERROR_COLOR.getRed() + ";" + ERROR_COLOR.getGreen() + ";" + ERROR_COLOR.getBlue() + "m";
		} else {
			infoColorCodeConsole = "\u001b[38;2;" + INFO_COLOR.getRed() + ";" + INFO_COLOR.getGreen() + ";" + INFO_COLOR.getBlue() + "m";
			warninColorCodeConsole = "\u001b[38;2;" + WARNING_COLOR.getRed() + ";" + WARNING_COLOR.getGreen() + ";" + WARNING_COLOR.getBlue() + "m";
			errorColorCodeConsole = "\u001b[38;2;" + ERROR_COLOR.getRed() + ";" + ERROR_COLOR.getGreen() + ";" + ERROR_COLOR.getBlue() + "m";
		}
	}

	@Override
	public void logging(IStatus status, String plugin) {
		
		String message = status.getPlugin() + " : " + status.getMessage();
		
		if(status.getSeverity() == IStatus.INFO) {
			Activator.getMessageConsole().newMessageStream().println(infoColorCodeConsole + message);
		}
		if(status.getSeverity() == IStatus.WARNING) {
			Activator.getMessageConsole().newMessageStream().println(warninColorCodeConsole + message);
		}
		if(status.getSeverity() == IStatus.ERROR) {
			Activator.getMessageConsole().newMessageStream().println(errorColorCodeConsole + message);
		}
		
	}

}
