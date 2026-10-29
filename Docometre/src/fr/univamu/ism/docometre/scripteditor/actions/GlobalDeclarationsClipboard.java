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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.dacqsystems.adwin.ADWinProcess;
import fr.univamu.ism.docometre.scripteditor.actions.GlobalDeclarationsTextFormat.ParseResult;
import fr.univamu.ism.process.GlobalDeclarations.Constant;
import fr.univamu.ism.process.GlobalDeclarations.Content;
import fr.univamu.ism.process.GlobalDeclarations.Library;
import fr.univamu.ism.process.GlobalDeclarations.Subroutine;

/**
 * Copy and paste of global declarations (libraries, constants and subroutines), all or part of them, from a process to another one or with external source code.
 * Copied declarations are stored in the script clipboard with copied blocks, and as source code text, so that they can 
 * be pasted in an external editor. Pasted declarations come from the most recent copy : declarations copied in DOCoMETRe 
 * or source code text copied in an external editor (see ScriptClipboard).
 */
public final class GlobalDeclarationsClipboard {
	
	/**
	 * Declarations to paste and where they come from
	 */
	public static final class PastedDeclarations {
		
		private Content content;
		private boolean fromText;
		private List<String> ignoredLines;
		
		private PastedDeclarations(Content content, boolean fromText, List<String> ignoredLines) {
			this.content = content;
			this.fromText = fromText;
			this.ignoredLines = ignoredLines;
		}
		
		public Content getContent() {
			return content;
		}
		
		/**
		 * @return true if declarations come from external source code, they must be reviewed before being added
		 */
		public boolean isFromText() {
			return fromText;
		}
		
		/**
		 * @return lines of external source code that are neither constants nor subroutines
		 */
		public List<String> getIgnoredLines() {
			return ignoredLines;
		}
		
	}
	
	private GlobalDeclarationsClipboard() {
	}
	
	/**
	 * Copy libraries, constants and subroutines to the clipboard, with their source code text
	 */
	public static void copy(Content content, Object context) {
		ArrayList<Object> contents = new ArrayList<Object>();
		contents.add(content);
		ScriptClipboard.setContents(contents, toText(content, context));
	}
	
	/**
	 * @return source code text of libraries, constants and subroutines in the language of this context
	 */
	public static String toText(Content content, Object context) {
		return GlobalDeclarationsTextFormat.format(content, isADBasic(context));
	}
	
	/**
	 * @return declarations of the most recent copy, from DOCoMETRe or from external source code, or null if there is none
	 */
	public static PastedDeclarations getPastedDeclarations(Object context) {
		Object contents = ScriptClipboard.getContents();
		if(contents instanceof List) {
			// Most recent copy was done in DOCoMETRe : blocks and/or global declarations
			for (Object object : (List<?>)contents) {
				if(object instanceof Content && !((Content)object).isEmpty()) return new PastedDeclarations((Content) object, false, new ArrayList<>());
			}
			return null;
		}
		String text = ScriptClipboard.getText();
		if(!GlobalDeclarationsTextFormat.mayContainDeclarations(text)) return null;
		ParseResult parseResult = GlobalDeclarationsTextFormat.parse(text, isADBasic(context));
		if(parseResult.getContent().isEmpty()) return null;
		return new PastedDeclarations(parseResult.getContent(), true, parseResult.getIgnoredLines());
	}
	
	/**
	 * Merge pasted libraries, constants and subroutines into current ones. Identical declarations are not duplicated. 
	 * When a pasted name already exists with a different definition, user chooses to replace existing 
	 * definitions, to keep them or to cancel paste.
	 * @param caseSensitive false for ADBasic whose names are not case sensitive
	 * @return merged content or null if paste is cancelled
	 */
	public static Content merge(Shell shell, Content current, Content pasted, boolean caseSensitive) {
		List<Library> libraries = current.getLibraries();
		List<Constant> constants = current.getConstants();
		List<Subroutine> subroutines = current.getSubroutines();
		List<String> conflicts = new ArrayList<>();
		for (Library library : pasted.getLibraries()) {
			int index = indexOf(libraries, library.getFile(), caseSensitive);
			if(index > -1 && !libraries.get(index).equals(library)) conflicts.add(library.getFile());
		}
		for (Constant constant : pasted.getConstants()) {
			int index = indexOf(constants, constant.getName(), caseSensitive);
			if(index > -1 && !constants.get(index).equals(constant)) conflicts.add(constant.getName());
		}
		for (Subroutine subroutine : pasted.getSubroutines()) {
			int index = indexOf(subroutines, subroutine.getName(), caseSensitive);
			if(index > -1 && !subroutines.get(index).equals(subroutine)) conflicts.add(subroutine.getName());
		}
		boolean replace = false;
		if(conflicts.size() > 0) {
			String[] buttons = new String[] {DocometreMessages.GlobalDeclarationsReplaceButton, DocometreMessages.GlobalDeclarationsKeepButton, IDialogConstants.CANCEL_LABEL};
			String message = NLS.bind(DocometreMessages.GlobalDeclarationsPasteConflictMessage, String.join(", ", conflicts));
			MessageDialog messageDialog = new MessageDialog(shell, DocometreMessages.GlobalDeclarationsPasteConflictTitle, null, message, MessageDialog.QUESTION, buttons, 0);
			int answer = messageDialog.open();
			if(answer != 0 && answer != 1) return null;
			replace = answer == 0;
		}
		for (Library library : pasted.getLibraries()) {
			int index = indexOf(libraries, library.getFile(), caseSensitive);
			if(index == -1) libraries.add(library);
			else if(replace) libraries.set(index, library);
		}
		for (Constant constant : pasted.getConstants()) {
			int index = indexOf(constants, constant.getName(), caseSensitive);
			if(index == -1) constants.add(constant);
			else if(replace) constants.set(index, constant);
		}
		for (Subroutine subroutine : pasted.getSubroutines()) {
			int index = indexOf(subroutines, subroutine.getName(), caseSensitive);
			if(index == -1) subroutines.add(subroutine);
			else if(replace) subroutines.set(index, subroutine);
		}
		return new Content(libraries, constants, subroutines);
	}
	
	private static int indexOf(List<?> declarations, String name, boolean caseSensitive) {
		for (int i = 0; i < declarations.size(); i++) {
			Object declaration = declarations.get(i);
			String declarationName = "";
			if(declaration instanceof Library) declarationName = ((Library)declaration).getFile();
			if(declaration instanceof Constant) declarationName = ((Constant)declaration).getName();
			if(declaration instanceof Subroutine) declarationName = ((Subroutine)declaration).getName();
			if(caseSensitive ? declarationName.equals(name) : declarationName.equalsIgnoreCase(name)) return i;
		}
		return -1;
	}
	
	/**
	 * @return true if names are case sensitive in the language generated for this context
	 */
	public static boolean isCaseSensitive(Object context) {
		return !isADBasic(context);
	}
	
	private static boolean isADBasic(Object context) {
		return context instanceof ADWinProcess;
	}
	
	
}
