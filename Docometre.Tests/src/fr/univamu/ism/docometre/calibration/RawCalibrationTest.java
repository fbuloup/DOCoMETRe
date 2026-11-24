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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [24/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.calibration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RawCalibrationTest {
	
	private static final double EPSILON = 1e-9;
	
	@Test
	public void coefficientsOfTheLine() {
		double[] kb = RawCalibration.getCoefficients(0, 65535, -10, 10);
		assertArrayEquals(new double[] {20.0 / 65535, -10}, kb, EPSILON);
		// u = k * raw + b gives the same values as compute()
		for (double raw : new double[] {0, 1234, 32767.5, 65535}) 
			assertEquals(RawCalibration.compute(raw, 0, 65535, -10, 10), kb[0] * raw + kb[1], EPSILON);
		assertThrows(IllegalArgumentException.class, () -> RawCalibration.getCoefficients(5, 5, 0, 1));
	}
	
	@Test
	public void codeOfThePhysicalValue() {
		// ADwin, range of +/-10 V : k = 20 / 65535, b = -10
		assertEquals(RawCalibration.format(20.0 / 65535) + " * Call_AnIn_Raw(1, 3, 0) - 10", RawCalibration.getCode("Call_AnIn_Raw(1, 3, 0)", 0, 65535, -10, 10));
		// b > 0
		assertEquals("0.5 * raw + 2", RawCalibration.getCode("raw", 0, 10, 2, 7));
		// b = 0 : no constant term
		assertEquals("0.25 * raw", RawCalibration.getCode("raw", 0, 4, 0, 1));
		// k < 0
		assertEquals("-2 * raw + 10", RawCalibration.getCode("raw", 0, 5, 10, 0));
		// No exponent in the generated code
		String code = RawCalibration.getCode("raw", 0, 1e9, 0, 1);
		assertFalse(code.contains("E"));
		assertThrows(IllegalArgumentException.class, () -> RawCalibration.getCode("raw", 3, 3, 0, 1));
		assertThrows(IllegalArgumentException.class, () -> RawCalibration.getCode("raw", 0, 1, Double.NaN, 1));
	}
	
	@Test
	public void rangeConversions() {
		RawCalibration.Range adwin = new RawCalibration.Range(0, 65535, -10, 10);
		assertEquals(-10, adwin.toVolt(0), EPSILON);
		assertEquals(10, adwin.toVolt(65535), EPSILON);
		assertEquals(16383.75, adwin.toRaw(-5), EPSILON);
		assertEquals(49151.25, adwin.toRaw(5), EPSILON);
		RawCalibration.Range r3 = new RawCalibration.Range(0, 1024, 0, 5);
		assertEquals(2.5, r3.toVolt(512), EPSILON);
		RawCalibration.Range ads1115 = new RawCalibration.Range(-32768, 32768, -6.144, 6.144);
		assertEquals(3.072, ads1115.toVolt(16384), EPSILON);
		assertThrows(IllegalArgumentException.class, () -> new RawCalibration.Range(1, 1, 0, 5));
	}
	
	@Test
	public void identityIsTheFormerDefaultCalibration() {
		assertTrue(RawCalibration.isIdentity(-10, 10, -10, 10));
		assertTrue(RawCalibration.isIdentity(0, 5, 0, 5));
		assertFalse(RawCalibration.isIdentity(-5, 5, 0, 100));
	}
	
	@Test
	public void valuesOfTheProperties() {
		assertEquals("0", RawCalibration.format(0));
		assertEquals("0", RawCalibration.format(-0.0));
		assertEquals("65535", RawCalibration.format(65535));
		assertEquals("-10", RawCalibration.format(-10.0));
		assertEquals("4.096", RawCalibration.format(4.096));
		assertEquals("16383.75", RawCalibration.format(16383.75));
		assertEquals(4.096, RawCalibration.parse(" 4.096 "), EPSILON);
		assertTrue(Double.isNaN(RawCalibration.parse(null)));
		assertTrue(Double.isNaN(RawCalibration.parse("abc")));
	}

}
