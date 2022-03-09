/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
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
