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

import java.util.function.Function;

import fr.univamu.ism.docometre.dacqsystems.Channel;
import fr.univamu.ism.docometre.dacqsystems.Property;

/**
 * Calibration properties of the analog inputs of a DACQ system (see {@link RawCalibration}) :
 * <ul>
 * <li>aMin, aMax : raw values of the converter ;</li>
 * <li>uMin, uMax : corresponding physical values, the voltages of the converter by default ;</li>
 * <li>calibrated : "true" when aMin, aMax, uMin or uMax have been set by the user, "false" when they are the
 * default values of the hardware, which then follow its changes (range, reference, resolution, gain...),
 * not defined (null) for the inputs saved before the calibration from the raw values (see
 * {@link #migrate(Channel, boolean)}).</li>
 * </ul>
 */
public final class ChannelCalibration {

	public static final String TRUE = "true";
	public static final String FALSE = "false";

	private final Property aMin;
	private final Property aMax;
	private final Property uMin;
	private final Property uMax;
	private final Property calibrated;
	private final Function<Channel, RawCalibration.Range> defaultRangeProvider;

	// True while values are written by the program (default values, conversion, copy) : not a calibration by the user
	private final ThreadLocal<Boolean> writingByProgram = ThreadLocal.withInitial(() -> false);

	/**
	 * @param defaultRangeProvider default calibration of an input given by its hardware configuration, null when
	 * it can not be determined (input without module or without DACQ configuration...)
	 */
	public ChannelCalibration(Property aMin, Property aMax, Property uMin, Property uMax, Property calibrated, Function<Channel, RawCalibration.Range> defaultRangeProvider) {
		this.aMin = aMin;
		this.aMax = aMax;
		this.uMin = uMin;
		this.uMax = uMax;
		this.calibrated = calibrated;
		this.defaultRangeProvider = defaultRangeProvider;
	}

	/**
	 * @return the default calibration of this input, null if it can not be determined
	 */
	public RawCalibration.Range getDefaultRange(Channel channel) {
		try {
			return defaultRangeProvider.apply(channel);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/**
	 * @return the property which tells if the input is calibrated ("true"), not calibrated ("false") or has a calibration
	 * saved before the raw values (null)
	 */
	public Property getCalibratedProperty() {
		return calibrated;
	}

	/**
	 * @return true if aMin, aMax, uMin or uMax of this input have been set by the user
	 */
	public boolean isCalibrated(Channel channel) {
		return TRUE.equals(channel.getProperty(calibrated));
	}

	/**
	 * @return true if the calibration of this input has the format used before the raw values (aMin and aMax
	 * in volts), not converted
	 */
	public boolean isLegacy(Channel channel) {
		return channel.getProperty(calibrated) == null;
	}

	/**
	 * @return true if property is aMin, aMax, uMin or uMax
	 */
	public boolean isCalibrationProperty(Property property) {
		return aMin.equals(property) || aMax.equals(property) || uMin.equals(property) || uMax.equals(property);
	}

	/**
	 * @return {aMin, aMax, uMin, uMax} of this input (NaN for a value which is not a number)
	 */
	public double[] getCalibration(Channel channel) {
		return new double[] {RawCalibration.parse(channel.getProperty(aMin)), RawCalibration.parse(channel.getProperty(aMax)),
				RawCalibration.parse(channel.getProperty(uMin)), RawCalibration.parse(channel.getProperty(uMax))};
	}

	/**
	 * Default calibration of the hardware written in this input, which is then not calibrated
	 * @return false if the default calibration can not be determined (nothing written)
	 */
	public boolean applyDefault(Channel channel) {
		return applyDefault(channel, null);
	}

	/**
	 * Default calibration of the hardware written in this input, which is then not calibrated
	 * @param fallback calibration written when the default calibration can not be determined (channel created
	 * without module, for example), nothing written if it is null
	 * @return false if nothing has been written
	 */
	public boolean applyDefault(Channel channel, RawCalibration.Range fallback) {
		RawCalibration.Range range = getDefaultRange(channel);
		if(range == null) range = fallback;
		if(range == null) return false;
		RawCalibration.Range defaultRange = range;
		writeByProgram(() -> {
			channel.setProperty(aMin, RawCalibration.format(defaultRange.getRawMin()));
			channel.setProperty(aMax, RawCalibration.format(defaultRange.getRawMax()));
			channel.setProperty(uMin, RawCalibration.format(defaultRange.getVoltMin()));
			channel.setProperty(uMax, RawCalibration.format(defaultRange.getVoltMax()));
			channel.setProperty(calibrated, FALSE);
		});
		return true;
	}

	/**
	 * Default calibration of this input updated after a change of the hardware configuration, if the input is
	 * not calibrated (nothing done otherwise)
	 */
	public void updateDefault(Channel channel) {
		if(FALSE.equals(channel.getProperty(calibrated))) applyDefault(channel);
	}

	/**
	 * To be called after each change of a property of an input : aMin, aMax, uMin or uMax set by the user (and not
	 * by the program, see {@link #applyDefault(Channel)}) : the input is calibrated, its values no more follow the
	 * changes of the hardware configuration.
	 */
	public void propertyChanged(Channel channel, Property property) {
		if(writingByProgram.get() || !isCalibrationProperty(property)) return;
		if(!TRUE.equals(channel.getProperty(calibrated))) writeByProgram(() -> channel.setProperty(calibrated, TRUE));
	}

	/**
	 * Copies the calibration of an input in another one (clone)
	 */
	public void copy(Channel from, Channel to) {
		writeByProgram(() -> {
			copy(from, to, aMin);
			copy(from, to, aMax);
			copy(from, to, uMin);
			copy(from, to, uMax);
			copy(from, to, calibrated);
		});
	}

	private static void copy(Channel from, Channel to, Property property) {
		String value = from.getProperty(property);
		if(value != null) to.setProperty(property, new String(value));
	}

	/**
	 * @return the values of the calibration properties of this input (aMin, aMax, uMin, uMax, calibrated), as saved,
	 * to be restored by {@link #restore(Channel, String[])}
	 */
	public String[] getValues(Channel channel) {
		return new String[] {channel.getProperty(aMin), channel.getProperty(aMax), channel.getProperty(uMin), channel.getProperty(uMax), channel.getProperty(calibrated)};
	}
	
	/**
	 * Restores values given by {@link #getValues(Channel)} (undo of a reset of the calibration, see ResetCalibrationOperation)
	 */
	public void restore(Channel channel, String[] values) {
		writeByProgram(() -> {
			channel.setProperty(aMin, values[0]);
			channel.setProperty(aMax, values[1]);
			channel.setProperty(uMin, values[2]);
			channel.setProperty(uMax, values[3]);
			channel.setProperty(calibrated, values[4]);
		});
	}
	
	/**
	 * Conversion of the calibration of an input saved before the raw values (property calibrated not defined) :
	 * <ul>
	 * <li>identity (aMin = uMin and aMax = uMax : physical values = voltages) : not calibrated, default calibration
	 * of the hardware ;</li>
	 * <li>otherwise : calibrated, aMin and aMax converted from volts to raw values when amplitudesInVolts, uMin and
	 * uMax unchanged : same line, the physical values are unchanged.</li>
	 * </ul>
	 * Nothing is done if the input has already been converted or if its default calibration can not be determined.
	 * @param amplitudesInVolts true if aMin and aMax have been saved in volts, false if they are already raw values
	 * @return true if the input has been converted
	 */
	public boolean migrate(Channel channel, boolean amplitudesInVolts) {
		if(!isLegacy(channel)) return false;
		RawCalibration.Range range = getDefaultRange(channel);
		if(range == null) return false;
		double[] values = getCalibration(channel);
		for (double value : values) if(Double.isNaN(value)) return applyDefault(channel);
		if(RawCalibration.isIdentity(values[0], values[1], values[2], values[3])) return applyDefault(channel);
		try {
			String rawMin = amplitudesInVolts ? RawCalibration.format(range.toRaw(values[0])) : channel.getProperty(aMin);
			String rawMax = amplitudesInVolts ? RawCalibration.format(range.toRaw(values[1])) : channel.getProperty(aMax);
			writeByProgram(() -> {
				channel.setProperty(aMin, rawMin);
				channel.setProperty(aMax, rawMax);
				channel.setProperty(calibrated, TRUE);
			});
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private void writeByProgram(Runnable runnable) {
		boolean previous = writingByProgram.get();
		writingByProgram.set(true);
		try {
			runnable.run();
		} finally {
			writingByProgram.set(previous);
		}
	}

}
