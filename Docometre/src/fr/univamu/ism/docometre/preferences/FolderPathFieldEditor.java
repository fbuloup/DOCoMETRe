package fr.univamu.ism.docometre.preferences;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

public class FolderPathFieldEditor extends FileFieldEditor {
		
		public FolderPathFieldEditor(String name, String labelText, Composite parent) {
			super(name, labelText, parent);
		}
		
		private File getFile(File startingDirectory) {
	        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.SHEET);
	        if (startingDirectory != null) {
				dialog.setFilterPath(startingDirectory.getPath());
			}
	        String file = dialog.open();
	        if (file != null) {
	            file = file.trim();
	            if (file.length() > 0) {
					return new File(file);
				}
	        }
	        return null;
	    }
		
		@Override
		protected String changePressed() {
			File f = new File(getTextControl().getText());
	        if (!f.exists()) {
				f = null;
			}
	        File d = getFile(f);
	        if (d == null) {
				return null;
			}
	        return d.getAbsolutePath();
		}
		
		@Override
		protected boolean checkState() {
			if(!Platform.getOS().equals(Platform.OS_WIN32)) return super.checkState();
			else return true;
		}
		
	}