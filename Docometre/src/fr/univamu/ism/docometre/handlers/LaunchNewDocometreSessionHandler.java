package fr.univamu.ism.docometre.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;

public class LaunchNewDocometreSessionHandler extends AbstractHandler {
	
	@SuppressWarnings("unused")
	private static String COMMAND_ID = "LaunchNewDocometreSessionCommand";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		String launcher = System.getProperty("user.dir"); //$NON-NLS-1$
		IPath path = Path.fromOSString(launcher);
		path = path.removeLastSegments(2);
		
		File application = path.toFile();
		if (!(application.exists() && application.isDirectory() && application.getName().endsWith(".app"))) { //$NON-NLS-1$
			String message = NLS.bind(DocometreMessages.CantLaunchNewDocometreInstance, application.getAbsolutePath());
			Activator.logErrorMessage(message);
			return null;
		}  
		
		List<String> args = new ArrayList<>();
		args.add("open"); //$NON-NLS-1$
		args.add("-n"); //$NON-NLS-1$
		args.add("-a"); //$NON-NLS-1$
		args.add(application.getAbsolutePath());
		
		StringBuilder cmd = new StringBuilder();
		for (String string : args) {
			cmd.append(string);
			cmd.append(' ');
		}

		String message = NLS.bind(DocometreMessages.LaunchNewDocometreInstance, cmd);
		Activator.logInfoMessage(message, getClass());

		try {
			// Execute the command line
			Process p = Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
			if (p.waitFor() != 0) {
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				cmd.setLength(0);
				String in = null;
				while ((in = br.readLine()) != null) {
					cmd.append("\n");
					cmd.append(in);
				}
				br.close();
				if (cmd.length() > 0) {
					Activator.logErrorMessage(cmd.toString());
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			Activator.logErrorMessageWithCause(e);
		}
		
		return null;
	}



}
