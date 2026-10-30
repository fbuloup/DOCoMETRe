package fr.univamu.ism.docometre.consoles;

import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.MessageConsole;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.ThemeColors;

public class DocometreConsole extends MessageConsole implements ILogListener {
	
	private static RGB WARNING_RGB = new RGB(204,102,0);
	private static RGB ERROR_RGB = new RGB(250, 0, 0);
	
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
	}
	
	private static String getColorCodeConsole(RGB rgb) {
		return "\u001b[38;2;" + rgb.red + ";" + rgb.green + ";" + rgb.blue + "m";
	}
	
	// Reset to console default color, so that following messages are not colored
	private static final String RESET_CODE_CONSOLE = "\u001b[0m";

	@Override
	public void logging(IStatus status, String plugin) {

		String message = status.getPlugin() + " : " + status.getMessage();
		
		// Info messages use console default color, which follows the theme
		if(status.getSeverity() == IStatus.INFO) {
			docometreConsole.newMessageStream().println(message);
		}
		// Theme colors are read here, once theme is applied
		if(status.getSeverity() == IStatus.WARNING) {
			docometreConsole.newMessageStream().println(getColorCodeConsole(ThemeColors.getRGB(ThemeColors.CONSOLE_WARNING_COLOR, WARNING_RGB)) + message + RESET_CODE_CONSOLE);
		}
		if(status.getSeverity() == IStatus.ERROR) {
			docometreConsole.newMessageStream().println(getColorCodeConsole(ThemeColors.getRGB(ThemeColors.CONSOLE_ERROR_COLOR, ERROR_RGB)) + message + RESET_CODE_CONSOLE);
		}
		
	}

}
