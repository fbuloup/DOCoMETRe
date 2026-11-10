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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.univamu.ism.docometre.dacqsystems.ParametersFile.TrialValues;

/**
 * Tests of parameters files reading and writing, without Eclipse : run them as plain JUnit 5 tests.
 */
public class ParametersFileTest {
	
	private static List<String> lines(String... lines) {
		return new ArrayList<>(Arrays.asList(lines));
	}
	
	private static List<String> sampleFile() {
		return lines("# Welcome in this parameters file !",
					 "#",
					 "Amplitude ; Duration ; Condition # names",
					 "# first block",
					 "10.25 ; 5 ; condition-1",
					 "-10.25 ; 4 ; condition-2 # second trial",
					 "5 ; 3 ; condition-1");
	}
	
	@Test
	public void removeCommentsRemovesCommentLinesAndEndOfLineComments() {
		List<String> lines = sampleFile();
		ParametersFile.removeComments(lines);
		assertEquals(Arrays.asList("Amplitude ; Duration ; Condition ", "10.25 ; 5 ; condition-1", "-10.25 ; 4 ; condition-2 ", "5 ; 3 ; condition-1"), lines);
	}
	
	@Test
	public void uncommentedLinesIndexesArePhysicalIndexes() {
		assertEquals(Arrays.asList(2, 4, 5, 6), ParametersFile.getUncommentedLinesIndexes(sampleFile()));
	}
	
	@Test
	public void trialValuesAreReadByTrialNumber() {
		List<String> lines = sampleFile();
		TrialValues trialValues = ParametersFile.getTrialValues(lines, 2);
		assertNotNull(trialValues);
		assertEquals("-10.25", trialValues.getValue(trialValues.indexOf("Amplitude")));
		assertEquals("4", trialValues.getValue(trialValues.indexOf("Duration")));
		assertEquals("condition-2", trialValues.getValue(trialValues.indexOf("Condition")));
		// Lines are not modified
		assertEquals(sampleFile(), lines);
	}
	
	@Test
	public void namesAreCaseInsensitive() {
		TrialValues trialValues = ParametersFile.getTrialValues(sampleFile(), 1);
		int index = trialValues.indexOf("aMpLiTuDe");
		assertEquals(0, index);
		// Name as written in the file
		assertEquals("Amplitude", trialValues.getName(index));
		assertEquals(-1, trialValues.indexOf("Unknown"));
	}
	
	@Test
	public void allSeparatorsAndWhiteSpacesAreAccepted() {
		List<String> lines = lines("a, b :c ;\td", "1 ,2: 3 ; 4");
		TrialValues trialValues = ParametersFile.getTrialValues(lines, 1);
		assertEquals("1", trialValues.getValue(trialValues.indexOf("a")));
		assertEquals("2", trialValues.getValue(trialValues.indexOf("b")));
		assertEquals("3", trialValues.getValue(trialValues.indexOf("c")));
		assertEquals("4", trialValues.getValue(trialValues.indexOf("d")));
	}
	
	@Test
	public void missingTrialLineGivesNull() {
		assertNull(ParametersFile.getTrialValues(sampleFile(), 4));
		assertNull(ParametersFile.getTrialValues(sampleFile(), 0));
		assertNull(ParametersFile.getTrialValues(lines("# only comments"), 1));
		assertTrue(ParametersFile.hasTrialLine(sampleFile(), 3));
		assertFalse(ParametersFile.hasTrialLine(sampleFile(), 4));
	}
	
	@Test
	public void missingValueGivesNull() {
		TrialValues trialValues = ParametersFile.getTrialValues(lines("a;b;c", "1;2"), 1);
		assertEquals("2", trialValues.getValue(trialValues.indexOf("b")));
		assertNull(trialValues.getValue(trialValues.indexOf("c")));
		assertNull(trialValues.getValue(-1));
	}
	
	@Test
	public void setTrialValuesReplacesOnlyGivenColumnsAndKeepsComment() {
		List<String> lines = sampleFile();
		Map<String, String> values = new LinkedHashMap<>();
		values.put("AMPLITUDE", "1.5");
		assertTrue(ParametersFile.setTrialValues(lines, 2, values));
		assertEquals("1.5;4;condition-2 # second trial", lines.get(5));
		// Other lines are not modified
		List<String> expected = sampleFile();
		expected.set(5, "1.5;4;condition-2 # second trial");
		assertEquals(expected, lines);
	}
	
	@Test
	public void setTrialValuesFillsMissingValues() {
		List<String> lines = lines("a;b;c", "1;2;3", "4");
		Map<String, String> values = new LinkedHashMap<>();
		values.put("c", "9");
		assertTrue(ParametersFile.setTrialValues(lines, 2, values));
		assertEquals("4;;9", lines.get(2));
	}
	
	@Test
	public void setTrialValuesWithoutTrialLineDoesNothing() {
		List<String> lines = sampleFile();
		Map<String, String> values = new LinkedHashMap<>();
		values.put("Amplitude", "1.5");
		assertFalse(ParametersFile.setTrialValues(lines, 4, values));
		assertEquals(sampleFile(), lines);
	}
	
	@Test
	public void writtenValuesAreReadBack() {
		List<String> lines = sampleFile();
		Map<String, String> values = new LinkedHashMap<>();
		values.put("Duration", "12");
		ParametersFile.setTrialValues(lines, 3, values);
		TrialValues trialValues = ParametersFile.getTrialValues(lines, 3);
		assertEquals("12", trialValues.getValue(trialValues.indexOf("Duration")));
		assertEquals("5", trialValues.getValue(trialValues.indexOf("Amplitude")));
	}

}
