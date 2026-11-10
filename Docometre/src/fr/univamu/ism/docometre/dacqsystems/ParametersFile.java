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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [10/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reading and writing of the lines of a parameters file (.params), without Eclipse
 * dependencies : the file is read and written by the caller.
 * <p>
 * Format of the file :
 * <ul>
 * <li>a line starting with # is a comment, everything to the right of a # is a comment ;</li>
 * <li>the first uncommented line holds the parameters names ;</li>
 * <li>uncommented line N + 1 holds the values of trial N (N starting at 1) ;</li>
 * <li>names and values are separated by semicolon (;), comma (,) or colon (:), white spaces are ignored.</li>
 * </ul>
 */
public final class ParametersFile {
	
	/**
	 * Names and values separators
	 */
	public static final String SEPARATORS_REGEXP = ";|:|,";
	
	/**
	 * Characters removed from names and values lines (white spaces, non breaking spaces)
	 */
	private static final String WHITE_SPACES_REGEXP = "[\\s|\\u00A0]+";
	
	/**
	 * End of line comment
	 */
	private static final String COMMENT_REGEXP = "#.*$";
	
	/**
	 * Names and values of a trial, read in a parameters file
	 */
	public static final class TrialValues {
		
		private final String[] names;
		private final String[] values;
		
		private TrialValues(String[] names, String[] values) {
			this.names = names;
			this.values = values;
		}
		
		/**
		 * @param name the name of a parameter (case is ignored)
		 * @return the index of the column of this parameter, -1 if not found
		 */
		public int indexOf(String name) {
			for (int i = 0; i < names.length; i++) {
				if(names[i].equalsIgnoreCase(name)) return i;
			}
			return -1;
		}
		
		/**
		 * @return the name of the column of index index, as written in the file
		 */
		public String getName(int index) {
			return names[index];
		}
		
		/**
		 * @return the value of the column of index index, null if the line of the trial has no value for this column
		 */
		public String getValue(int index) {
			if(index < 0 || index >= values.length) return null;
			return values[index];
		}
		
	}
	
	private ParametersFile() {
	}
	
	/**
	 * Removes the comments : lines starting with # are removed, everything to the right of a #
	 * is removed from the other lines.
	 * @param lines the lines of the file, modified by this method
	 */
	public static void removeComments(List<String> lines) {
		int n = 0;
		while (n < lines.size()) {
			String line = lines.get(n);
			if(line.startsWith("#")) lines.remove(n);
			else {
				if(line.indexOf("#") > -1) {
					line = line.replaceAll(COMMENT_REGEXP, "");
					lines.set(n, line);
				}
				n++;
			}
		}
	}
	
	/**
	 * @param lines the lines of the file
	 * @return the indexes of the lines kept by {@link #removeComments(List)} : lines that do not start with a comment
	 */
	public static List<Integer> getUncommentedLinesIndexes(List<String> lines) {
		List<Integer> indexes = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			if(!lines.get(i).startsWith("#")) indexes.add(i);
		}
		return indexes;
	}
	
	/**
	 * @param lines the lines of the file (not modified)
	 * @param trialNumber the number of the trial (starting at 1)
	 * @return true if the file holds a line of values for this trial
	 */
	public static boolean hasTrialLine(List<String> lines, int trialNumber) {
		return trialNumber >= 1 && getUncommentedLinesIndexes(lines).size() - 1 >= trialNumber;
	}
	
	/**
	 * @param lines the lines of the file (not modified)
	 * @param trialNumber the number of the trial (starting at 1)
	 * @return the names and values of this trial, null if the file has no line of values for this trial
	 */
	public static TrialValues getTrialValues(List<String> lines, int trialNumber) {
		List<String> uncommentedLines = new ArrayList<>(lines);
		removeComments(uncommentedLines);
		if(trialNumber < 1 || uncommentedLines.size() - 1 < trialNumber) return null;
		String[] names = split(uncommentedLines.get(0));
		String[] values = split(uncommentedLines.get(trialNumber));
		return new TrialValues(names, values);
	}
	
	/**
	 * Replaces values of a trial : columns whose name is a key of valuesByName (case is ignored) take
	 * the associated value, the other columns keep their value. The comment at the end of the line is kept.
	 * The values of the line are then separated by semicolons.
	 * @param lines the lines of the file, modified by this method
	 * @param trialNumber the number of the trial (starting at 1)
	 * @param valuesByName the new values, by parameter name
	 * @return false if the file has no line of values for this trial (lines are not modified)
	 */
	public static boolean setTrialValues(List<String> lines, int trialNumber, Map<String, String> valuesByName) {
		// Physical indexes of uncommented lines : first one holds parameters names,
		// uncommented line N + 1 holds values of trial N.
		List<Integer> uncommentedLinesIndexes = getUncommentedLinesIndexes(lines);
		if(trialNumber < 1 || uncommentedLinesIndexes.size() - 1 < trialNumber) return false;
		int trialLineIndex = uncommentedLinesIndexes.get(trialNumber);
		String trialLine = lines.get(trialLineIndex);
		// Comment at the end of the line is kept
		String comment = trialLine.indexOf("#") > -1 ? trialLine.substring(trialLine.indexOf("#")) : "";
		String[] names = split(lines.get(uncommentedLinesIndexes.get(0)).replaceAll(COMMENT_REGEXP, ""));
		String[] values = split(trialLine.replaceAll(COMMENT_REGEXP, ""));
		StringBuilder newLine = new StringBuilder();
		for (int i = 0; i < names.length; i++) {
			// By default, keep current value
			String value = i < values.length ? values[i] : "";
			for (Map.Entry<String, String> entry : valuesByName.entrySet()) {
				if(entry.getKey().equalsIgnoreCase(names[i])) {
					value = entry.getValue();
					break;
				}
			}
			if(i > 0) newLine.append(";");
			newLine.append(value);
		}
		if(!comment.isEmpty()) newLine.append(" ").append(comment);
		lines.set(trialLineIndex, newLine.toString());
		return true;
	}
	
	private static String[] split(String line) {
		return line.replaceAll(WHITE_SPACES_REGEXP, "").split(SEPARATORS_REGEXP);
	}

}
