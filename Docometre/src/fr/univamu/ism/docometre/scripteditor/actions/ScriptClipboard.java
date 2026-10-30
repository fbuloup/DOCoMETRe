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
package fr.univamu.ism.docometre.scripteditor.actions;

import org.eclipse.gef.dnd.SimpleObjectTransfer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

/**
 * Clipboard of script editors : copied blocks and global declarations, and their source code text.
 * Both are written to the system clipboard at once, so that the most recent copy, in DOCoMETRe or in 
 * another application, is the one that is pasted : a copy in another application replaces copied blocks 
 * and declarations, and a copy in DOCoMETRe replaces text copied in another application.
 */
public final class ScriptClipboard {
	
	// Objects are kept in memory, system clipboard only knows that they are available
	private static final SimpleObjectTransfer TRANSFER = new SimpleObjectTransfer() {
		private static final String TYPE_NAME = "fr.univamu.ism.docometre.scriptclipboard.transfer"; //$NON-NLS-1$
		private static final int TYPE_ID = registerType(TYPE_NAME);

		@Override
		protected int[] getTypeIds() {
			return new int[] { TYPE_ID };
		}

		@Override
		protected String[] getTypeNames() {
			return new String[] { TYPE_NAME };
		}
	};
	
	private ScriptClipboard() {
	}
	
	/**
	 * @param contents copied blocks and global declarations
	 * @param text source code of copied global declarations or null
	 */
	public static void setContents(Object contents, String text) {
		Display display = Display.getCurrent();
		if(display == null) return;
		Clipboard clipboard = new Clipboard(display);
		try {
			if(text == null || text.isEmpty()) clipboard.setContents(new Object[] {contents}, new Transfer[] {TRANSFER});
			else clipboard.setContents(new Object[] {contents, text}, new Transfer[] {TRANSFER, TextTransfer.getInstance()});
		} finally {
			clipboard.dispose();
		}
	}
	
	/**
	 * @return blocks and global declarations copied in DOCoMETRe, null if the most recent copy was done in another application
	 */
	public static Object getContents() {
		return getContents(TRANSFER);
	}
	
	/**
	 * @return text of system clipboard or null
	 */
	public static String getText() {
		return (String) getContents(TextTransfer.getInstance());
	}
	
	private static Object getContents(Transfer transfer) {
		Display display = Display.getCurrent();
		if(display == null) return null;
		Clipboard clipboard = new Clipboard(display);
		try {
			return clipboard.getContents(transfer);
		} finally {
			clipboard.dispose();
		}
	}

}
