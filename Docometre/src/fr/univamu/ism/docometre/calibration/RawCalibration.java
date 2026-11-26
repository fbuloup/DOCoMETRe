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

import java.math.BigDecimal;

/**
 * Calibration of an analog input from the raw values of its converter (ADC) : one affine transformation
 * from the raw value to the physical value, u = k * raw + b, the line going through (aMin, uMin) and
 * (aMax, uMax). aMin and aMax are raw values of the converter, uMin and uMax the corresponding physical
 * values : by default (input not calibrated), the voltages of the converter for aMin and aMax.
 * Pure computations, without dependency on the DACQ systems (see {@link ChannelCalibration}).
 */
public final class RawCalibration {
	
	/**
	 * Default calibration of an input : two raw values of its converter and the corresponding voltages, given
	 * by its hardware configuration (range, reference, resolution, gain...).
	 */
	public static final class Range {
		
		private final double rawMin;
		private final double rawMax;
		private final double voltMin;
		private final double voltMax;
		
		/**
		 * @throws IllegalArgumentException if rawMin equals rawMax or if a value is not finite
		 */
		public Range(double rawMin, double rawMax, double voltMin, double voltMax) {
			if(!Double.isFinite(rawMin) || !Double.isFinite(rawMax) || !Double.isFinite(voltMin) || !Double.isFinite(voltMax)) 
				throw new IllegalArgumentException("Calibration range values must be finite");
			if(rawMin == rawMax) throw new IllegalArgumentException("Calibration range : rawMin must differ from rawMax");
			this.rawMin = rawMin;
			this.rawMax = rawMax;
			this.voltMin = voltMin;
			this.voltMax = voltMax;
		}
		
		public double getRawMin() { return rawMin; }
		public double getRawMax() { return rawMax; }
		public double getVoltMin() { return voltMin; }
		public double getVoltMax() { return voltMax; }
		
		/**
		 * @return the voltage of the converter for this raw value
		 */
		public double toVolt(double raw) {
			return voltMin + (raw - rawMin) * (voltMax - voltMin) / (rawMax - rawMin);
		}
		
		/**
		 * @return the raw value of the converter for this voltage (not rounded)
		 * @throws IllegalArgumentException if voltMin equals voltMax
		 */
		public double toRaw(double volt) {
			if(voltMin == voltMax) throw new IllegalArgumentException("Calibration range : voltMin must differ from voltMax");
			return rawMin + (volt - voltMin) * (rawMax - rawMin) / (voltMax - voltMin);
		}
		
		@Override
		public String toString() {
			return "[" + rawMin + ", " + rawMax + "] <-> [" + voltMin + ", " + voltMax + "] V";
		}
		
	}
	
	private RawCalibration() {
	}
	
	/**
	 * @return {k, b} : u = k * raw + b, the line going through (aMin, uMin) and (aMax, uMax)
	 * @throws IllegalArgumentException if aMin equals aMax
	 */
	public static double[] getCoefficients(double aMin, double aMax, double uMin, double uMax) {
		if(aMin == aMax) throw new IllegalArgumentException("Calibration : aMin must differ from aMax");
		double k = (uMax - uMin) / (aMax - aMin);
		double b = uMin - k * aMin;
		return new double[] {k, b};
	}
	
	/**
	 * @return the physical value of this raw value
	 * @throws IllegalArgumentException if aMin equals aMax
	 */
	public static double compute(double raw, double aMin, double aMax, double uMin, double uMax) {
		return uMin + (uMax - uMin) * (raw - aMin) / checkedDelta(aMin, aMax);
	}
	
	private static double checkedDelta(double aMin, double aMax) {
		if(aMin == aMax) throw new IllegalArgumentException("Calibration : aMin must differ from aMax");
		return aMax - aMin;
	}
	
	/**
	 * @param rawCode code of the raw value (call of a function of the DACQ system...)
	 * @return the code of the physical value, for the code generation : "k * rawCode + b", "k * rawCode - |b|" or
	 * "k * rawCode" when b = 0, numbers without exponent
	 * @throws IllegalArgumentException if aMin equals aMax or if a value is not a finite number
	 */
	public static String getCode(String rawCode, double aMin, double aMax, double uMin, double uMax) {
		return getCode(rawCode, aMin, aMax, uMin, uMax, false);
	}
	
	/**
	 * @param cFloatLiterals true for C float literals (see {@link #formatCFloat(double)}), false for numbers without suffix
	 * @see #getCode(String, double, double, double, double)
	 */
	public static String getCode(String rawCode, double aMin, double aMax, double uMin, double uMax, boolean cFloatLiterals) {
		double[] kb = getCheckedCoefficients(aMin, aMax, uMin, uMax);
		String code = format(kb[0], cFloatLiterals) + " * " + rawCode;
		if(kb[1] > 0) code = code + " + " + format(kb[1], cFloatLiterals);
		if(kb[1] < 0) code = code + " - " + format(-kb[1], cFloatLiterals);
		return code;
	}
	
	/**
	 * @return {k, b} of the line, as C float literals (see {@link #formatCFloat(double)})
	 * @throws IllegalArgumentException if aMin equals aMax or if a value is not a finite number
	 */
	public static String[] getCFloatCoefficients(double aMin, double aMax, double uMin, double uMax) {
		double[] kb = getCheckedCoefficients(aMin, aMax, uMin, uMax);
		return new String[] {formatCFloat(kb[0]), formatCFloat(kb[1])};
	}
	
	private static double[] getCheckedCoefficients(double aMin, double aMax, double uMin, double uMax) {
		if(!Double.isFinite(aMin) || !Double.isFinite(aMax) || !Double.isFinite(uMin) || !Double.isFinite(uMax))
			throw new IllegalArgumentException("Calibration values must be finite");
		double[] kb = getCoefficients(aMin, aMax, uMin, uMax);
		if(!Double.isFinite(kb[0]) || !Double.isFinite(kb[1])) throw new IllegalArgumentException("Calibration coefficients must be finite");
		return kb;
	}
	
	private static String format(double value, boolean cFloatLiteral) {
		return cFloatLiteral ? formatCFloat(value) : format(value);
	}
	
	/**
	 * @return a C float literal : the shortest decimal value of the float nearest to value (which the C compiler converts
	 * back to the same float), with a decimal point and the f suffix (10.0f, -0.5f, 0.0048828125f), so that the
	 * computation is made in single precision (FPU of the Uno R4, double precision emulated)
	 * @throws IllegalArgumentException if value is not a finite float
	 */
	public static String formatCFloat(double value) {
		float floatValue = (float) value;
		if(!Float.isFinite(floatValue)) throw new IllegalArgumentException("Not a finite float : " + value);
		String literal = floatValue == 0 ? "0" : new BigDecimal(Float.toString(floatValue)).stripTrailingZeros().toPlainString();
		if(!literal.contains(".")) literal = literal + ".0";
		return literal + "f";
	}
	
	/**
	 * Calibration of the format used before the raw values (aMin and aMax in volts) : identity (aMin = uMin and
	 * aMax = uMax) means that the input was not calibrated, its physical values being its voltages.
	 */
	public static boolean isIdentity(double aMin, double aMax, double uMin, double uMax) {
		return aMin == uMin && aMax == uMax;
	}
	
	/**
	 * @return the value written in a property : without exponent nor trailing zeros (65535, -10, 4.096, 0)
	 */
	public static String format(double value) {
		if(!Double.isFinite(value)) return Double.toString(value);
		if(value == 0) return "0";
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}
	
	/**
	 * @return the value of a property, NaN if it is null or not a number
	 */
	public static double parse(String value) {
		if(value == null) return Double.NaN;
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

}
