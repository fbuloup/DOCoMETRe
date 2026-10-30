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
package fr.univamu.ism.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Global declarations of a script : libraries, constants and subroutines available in all segments.
 * They are displayed in the INITIALIZE segment editor as an element that cannot be deleted nor moved.
 * 
 * This class is a view on the script fields : constants and subroutines are serialized 
 * in the script as standard Java types, so that processes stay readable by previous 
 * versions of the software.
 */
public final class GlobalDeclarations {
	
	/**
	 * A library : #include <file> or #include "file" in C, #INCLUDE file in ADBasic
	 */
	public static final class Library {
		
		private String file;
		private String comment;
		
		public Library(String file, String comment) {
			this.file = file == null ? "" : file;
			this.comment = comment == null ? "" : comment;
		}
		
		public String getFile() {
			return file;
		}
		
		public String getComment() {
			return comment;
		}
		
		public Library copy() {
			return new Library(file, comment);
		}
		
		private String[] toArray() {
			return new String[] {file, comment};
		}
		
		private static Library fromArray(String[] values) {
			return new Library(values.length > 0 ? values[0] : "", values.length > 1 ? values[1] : "");
		}
		
		@Override
		public boolean equals(Object object) {
			if(!(object instanceof Library)) return false;
			Library library = (Library) object;
			return file.equals(library.file) && comment.equals(library.comment);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(file, comment);
		}
		
	}
	
	/**
	 * A global constant : #define in C, #DEFINE in ADBasic
	 */
	public static final class Constant {
		
		private String name;
		private String value;
		private String comment;
		
		public Constant(String name, String value, String comment) {
			this.name = name == null ? "" : name;
			this.value = value == null ? "" : value;
			this.comment = comment == null ? "" : comment;
		}
		
		public String getName() {
			return name;
		}
		
		public String getValue() {
			return value;
		}
		
		public String getComment() {
			return comment;
		}
		
		public Constant copy() {
			return new Constant(name, value, comment);
		}
		
		private String[] toArray() {
			return new String[] {name, value, comment};
		}
		
		private static Constant fromArray(String[] values) {
			return new Constant(values.length > 0 ? values[0] : "", values.length > 1 ? values[1] : "", values.length > 2 ? values[2] : "");
		}
		
		@Override
		public boolean equals(Object object) {
			if(!(object instanceof Constant)) return false;
			Constant constant = (Constant) object;
			return name.equals(constant.name) && value.equals(constant.value) && comment.equals(constant.comment);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(name, value, comment);
		}
		
	}
	
	/**
	 * A global subroutine without return value : void name(parameters) {...} in C, SUB name(parameters) ... ENDSUB in ADBasic.
	 * Parameters and body are written in the target language.
	 */
	public static final class Subroutine {
		
		private String name;
		private String parameters;
		private String body;
		
		public Subroutine(String name, String parameters, String body) {
			this.name = name == null ? "" : name;
			this.parameters = parameters == null ? "" : parameters;
			this.body = body == null ? "" : body;
		}
		
		public String getName() {
			return name;
		}
		
		public String getParameters() {
			return parameters;
		}
		
		public String getBody() {
			return body;
		}
		
		public Subroutine copy() {
			return new Subroutine(name, parameters, body);
		}
		
		private String[] toArray() {
			return new String[] {name, parameters, body};
		}
		
		private static Subroutine fromArray(String[] values) {
			return new Subroutine(values.length > 0 ? values[0] : "", values.length > 1 ? values[1] : "", values.length > 2 ? values[2] : "");
		}
		
		@Override
		public boolean equals(Object object) {
			if(!(object instanceof Subroutine)) return false;
			Subroutine subroutine = (Subroutine) object;
			return name.equals(subroutine.name) && parameters.equals(subroutine.parameters) && body.equals(subroutine.body);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(name, parameters, body);
		}
		
	}
	
	/**
	 * Libraries, constants and subroutines, e.g. all or part of global declarations copied to the clipboard 
	 */
	public static final class Content {
		
		private List<Library> libraries = new ArrayList<>();
		private List<Constant> constants = new ArrayList<>();
		private List<Subroutine> subroutines = new ArrayList<>();
		
		public Content(List<Constant> constants, List<Subroutine> subroutines) {
			this(null, constants, subroutines);
		}
		
		public Content(List<Library> libraries, List<Constant> constants, List<Subroutine> subroutines) {
			if(libraries != null) for (Library library : libraries) this.libraries.add(library.copy());
			if(constants != null) for (Constant constant : constants) this.constants.add(constant.copy());
			if(subroutines != null) for (Subroutine subroutine : subroutines) this.subroutines.add(subroutine.copy());
		}
		
		public List<Library> getLibraries() {
			List<Library> copy = new ArrayList<>();
			for (Library library : libraries) copy.add(library.copy());
			return copy;
		}
		
		public List<Constant> getConstants() {
			List<Constant> copy = new ArrayList<>();
			for (Constant constant : constants) copy.add(constant.copy());
			return copy;
		}
		
		public List<Subroutine> getSubroutines() {
			List<Subroutine> copy = new ArrayList<>();
			for (Subroutine subroutine : subroutines) copy.add(subroutine.copy());
			return copy;
		}
		
		public boolean isEmpty() {
			return libraries.isEmpty() && constants.isEmpty() && subroutines.isEmpty();
		}
		
		@Override
		public boolean equals(Object object) {
			if(!(object instanceof Content)) return false;
			Content content = (Content) object;
			return libraries.equals(content.libraries) && constants.equals(content.constants) && subroutines.equals(content.subroutines);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(libraries, constants, subroutines);
		}
		
	}
	
	// Fixed location of global declarations in INITIALIZE segment editor
	public static final int X = 10;
	public static final int Y = 10;
	public static final int HEIGHT = 30;
	
	private Script script;
	
	GlobalDeclarations(Script script) {
		this.script = script;
	}
	
	public Script getScript() {
		return script;
	}
	
	public List<Library> getLibraries() {
		List<Library> libraries = new ArrayList<>();
		for (String[] values : script.getGlobalLibrariesValues()) libraries.add(Library.fromArray(values));
		return libraries;
	}
	
	public List<Constant> getConstants() {
		List<Constant> constants = new ArrayList<>();
		for (String[] values : script.getGlobalConstantsValues()) constants.add(Constant.fromArray(values));
		return constants;
	}
	
	public List<Subroutine> getSubroutines() {
		List<Subroutine> subroutines = new ArrayList<>();
		for (String[] values : script.getGlobalSubroutinesValues()) subroutines.add(Subroutine.fromArray(values));
		return subroutines;
	}
	
	public Content getContent() {
		return new Content(getLibraries(), getConstants(), getSubroutines());
	}
	
	public boolean isEmpty() {
		return script.getGlobalLibrariesValues().isEmpty() && script.getGlobalConstantsValues().isEmpty() && script.getGlobalSubroutinesValues().isEmpty();
	}
	
	/**
	 * Replace all libraries, constants and subroutines
	 */
	public void setContent(Content content) {
		ArrayList<String[]> librariesValues = new ArrayList<>();
		for (Library library : content.getLibraries()) librariesValues.add(library.toArray());
		ArrayList<String[]> constantsValues = new ArrayList<>();
		for (Constant constant : content.getConstants()) constantsValues.add(constant.toArray());
		ArrayList<String[]> subroutinesValues = new ArrayList<>();
		for (Subroutine subroutine : content.getSubroutines()) subroutinesValues.add(subroutine.toArray());
		script.setGlobalDeclarationsValues(librariesValues, constantsValues, subroutinesValues);
	}
	
}
