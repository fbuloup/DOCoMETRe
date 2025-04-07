package fr.univamu.ism.docometre.consoles;

import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.MessageConsole;

import fr.univamu.ism.docometre.Activator;

public class DocometreConsole extends MessageConsole implements ILogListener {
	
	private static Color WARNING_COLOR = JFaceResources.getResources().createColor(new RGB(204,102,0));
	private static Color ERROR_COLOR = JFaceResources.getResources().createColor(new RGB(250, 0, 0));
	private static Color ERROR_COLOR_DARK = JFaceResources.getResources().createColor(new RGB(225, 69, 69));
	private static Color INFO_COLOR_DARK = JFaceResources.getResources().createColor(new RGB(255, 255, 255));
	private static Color INFO_COLOR = JFaceResources.getResources().createColor(new RGB(0, 0, 0));
	private static String infoColorCodeConsole;
	private static String warninColorCodeConsole;
	private static String errorColorCodeConsole;
	
	private static DocometreConsole docometreConsole;
	
	public static DocometreConsole createInstance() {
		if(docometreConsole == null) {
			docometreConsole = new DocometreConsole("Messages", IConsoleConstants.MESSAGE_CONSOLE_TYPE, null, StandardCharsets.UTF_16.name(), true);
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { docometreConsole });
		}
		return docometreConsole;
	}
	
	public static DocometreConsole getInstance() {
		return createInstance();
	}

	private DocometreConsole(String name, String consoleType, ImageDescriptor imageDescriptor, String encoding, boolean autoLifecycle) {
		super(name, consoleType, imageDescriptor, encoding, autoLifecycle);
		Activator.getDefault().getLog().addLogListener(this);
//		IViewPart consoleView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IConsoleConstants.ID_CONSOLE_VIEW);
//		if(consoleView != null)
//			consoleView.getSite().getPage().addPartListener(this);
		if(Display.isSystemDarkTheme()) {
			infoColorCodeConsole = "\u001b[38;2;" + INFO_COLOR_DARK.getRed() + ";" + INFO_COLOR_DARK.getGreen() + ";" + INFO_COLOR_DARK.getBlue() + "m";
			warninColorCodeConsole = "\u001b[38;2;" + WARNING_COLOR.getRed() + ";" + WARNING_COLOR.getGreen() + ";" + WARNING_COLOR.getBlue() + "m";
			errorColorCodeConsole = "\u001b[38;2;" + ERROR_COLOR_DARK.getRed() + ";" + ERROR_COLOR.getGreen() + ";" + ERROR_COLOR.getBlue() + "m";
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
			docometreConsole.newMessageStream().println(infoColorCodeConsole + message);
		}
		if(status.getSeverity() == IStatus.WARNING) {
			docometreConsole.newMessageStream().println(warninColorCodeConsole + message);
		}
		if(status.getSeverity() == IStatus.ERROR) {
			docometreConsole.newMessageStream().println(errorColorCodeConsole + message);
		}
		
	}

}
