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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [13/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre;

import java.util.Set;

import org.eclipse.osgi.util.NLS;

/*
 * Rules for resources names (experiments, subjects, sessions, processes, DACQ configurations...).
 * Names are used in data files names, whose segments are separated by '.', and experiments and 
 * subjects names are MATLAB identifiers (experiment.subject.channel) and keys prefixes in Python.
 * So a name :
 *  - starts with a letter and only contains letters, digits and '_' (no '.', no space, no '-'),
 *  - has at most 63 characters (MATLAB namelengthmax : longer field names are truncated),
 *  - is not a MATLAB or Python keyword (not necessary for Python, where names are only keys, but more consistent),
 *  - is not a trial folder name ("Trial_N"), except for trials : a folder named as a trial is a trial.
 */
public final class ResourceNames {
	
	public static final String REGULAR_EXPRESSION = "^[a-zA-Z][a-zA-Z0-9_]*$"; //$NON-NLS-1$
	
	public static final int MAXIMUM_LENGTH = 63;
	
	// MATLAB keywords (iskeyword)
	private static final Set<String> MATLAB_KEYWORDS = Set.of("break", "case", "catch", "classdef", "continue", "else", "elseif", "end", "for",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
			"function", "global", "if", "otherwise", "parfor", "persistent", "return", "spmd", "switch", "try", "while"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$
	
	// Python keywords (keyword.kwlist)
	private static final Set<String> PYTHON_KEYWORDS = Set.of("False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
			"continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$
			"nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
	
	private ResourceNames() {
	}
	
	/*
	 * Return null if name, which matches REGULAR_EXPRESSION, respects the other rules, an error message otherwise.
	 * isTrial is true if name is the name of a trial folder.
	 */
	public static String getErrorMessage(String name, boolean isTrial) {
		if(name == null) return null;
		if(name.length() > MAXIMUM_LENGTH) return NLS.bind(DocometreMessages.ResourceNameTooLong, name, MAXIMUM_LENGTH);
		if(MATLAB_KEYWORDS.contains(name) || PYTHON_KEYWORDS.contains(name)) return NLS.bind(DocometreMessages.ResourceNameReservedWord, name);
		if(!isTrial && TrialNames.isName(name)) return NLS.bind(DocometreMessages.ResourceNameTrialName, name);
		return null;
	}

}
