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
package fr.univamu.ism.docometre.dacqsystems.adwin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.junit.jupiter.api.Test;

import fr.univamu.ism.docometre.calibration.RawCalibration;
import fr.univamu.ism.docometre.calibration.ResetCalibrationOperation;
import fr.univamu.ism.docometre.dacqsystems.Channel;

/**
 * Calibration of the ADwin analog inputs from the raw values of the converter (see ChannelCalibration)
 */
public class ADWinRawCalibrationTest {

	private static final double EPSILON = 1e-9;

	private static void assertCalibration(Channel channel, String aMin, String aMax, String uMin, String uMax, String calibrated) {
		assertEquals(aMin, channel.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MIN));
		assertEquals(aMax, channel.getProperty(ADWinAnInChannelProperties.AMPLITUDE_MAX));
		assertEquals(uMin, channel.getProperty(ADWinAnInChannelProperties.UNIT_MIN));
		assertEquals(uMax, channel.getProperty(ADWinAnInChannelProperties.UNIT_MAX));
		assertEquals(calibrated, channel.getProperty(ADWinAnInChannelProperties.CALIBRATED));
	}

	@Test
	public void defaultCalibrationFollowsTheRangeOfTheModule() {
		ADWinAnInModule module = new ADWinAnInModule(null);
		Channel channel = module.createChannel();
		// Raw values of the 16 bits converter <-> range of the module (+/-10 V by default)
		assertCalibration(channel, "0", "65535", "-10", "10", "false");
		module.setProperty(ADWinAnInModuleProperties.AMPLITUDE_MIN, "0");
		assertCalibration(channel, "0", "65535", "0", "10", "false");
		module.setProperty(ADWinAnInModuleProperties.AMPLITUDE_MAX, "5");
		assertCalibration(channel, "0", "65535", "0", "5", "false");
		// The gain does not change the default calibration
		channel.setProperty(ADWinAnInChannelProperties.GAIN, "8");
		assertCalibration(channel, "0", "65535", "0", "5", "false");
	}

	@Test
	public void calibratedInputDoesNotFollowTheModule() {
		ADWinAnInModule module = new ADWinAnInModule(null);
		Channel channel = module.createChannel();
		channel.setProperty(ADWinAnInChannelProperties.UNIT_MAX, "100");
		assertTrue(ADWinAnInChannelProperties.CALIBRATION.isCalibrated(channel));
		module.setProperty(ADWinAnInModuleProperties.AMPLITUDE_MIN, "0");
		assertCalibration(channel, "0", "65535", "-10", "100", "true");
	}

	@Test
	public void resetOfTheCalibrationCanBeUndone() throws Exception {
		ADWinAnInModule module = new ADWinAnInModule(null);
		Channel channel = module.createChannel();
		channel.setProperty(ADWinAnInChannelProperties.AMPLITUDE_MIN, "100");
		channel.setProperty(ADWinAnInChannelProperties.UNIT_MAX, "50");
		assertCalibration(channel, "100", "65535", "-10", "50", "true");
		ResetCalibrationOperation operation = new ResetCalibrationOperation(null, "reset", ADWinAnInChannelProperties.CALIBRATION, channel, IOperationHistory.GLOBAL_UNDO_CONTEXT);
		assertTrue(operation.execute(null, null).isOK());
		assertCalibration(channel, "0", "65535", "-10", "10", "false");
		// Not calibrated again : follows the range of the module
		module.setProperty(ADWinAnInModuleProperties.AMPLITUDE_MIN, "0");
		assertCalibration(channel, "0", "65535", "0", "10", "false");
		assertTrue(operation.undo(null, null).isOK());
		assertCalibration(channel, "100", "65535", "-10", "50", "true");
		assertTrue(operation.redo(null, null).isOK());
		assertCalibration(channel, "0", "65535", "0", "10", "false");
	}

	@Test
	public void gainCodeOfThePGA() {
		ADWinAnInModule module = new ADWinAnInModule(null);
		Channel channel = module.createChannel();
		String[] gains = {"1", "2", "4", "8"};
		for (int code = 0; code < gains.length; code++) {
			channel.setProperty(ADWinAnInChannelProperties.GAIN, gains[code]);
			assertEquals(code, ADWinAnInChannelProperties.getGainCode(channel));
		}
		channel.setProperty(ADWinAnInChannelProperties.GAIN, "3");
		assertThrows(IllegalArgumentException.class, () -> ADWinAnInChannelProperties.getGainCode(channel));
	}
	
	@Test
	public void formerIdentityCalibrationIsConvertedToTheDefaultOne() {
		ADWinAnInModule module = new ADWinAnInModule(null);
		Channel channel = module.createChannel();
		setLegacyCalibration(channel, "-10", "10", "-10", "10");
		assertTrue(ADWinAnInChannelProperties.CALIBRATION.isLegacy(channel));
		assertTrue(ADWinAnInChannelProperties.CALIBRATION.migrate(channel, true));
		assertCalibration(channel, "0", "65535", "-10", "10", "false");
		// Once converted, nothing more is done
		assertFalse(ADWinAnInChannelProperties.CALIBRATION.migrate(channel, true));
	}

	@Test
	public void formerCalibrationIsConvertedWithTheSameLine() {
		ADWinAnInModule module = new ADWinAnInModule(null);
		Channel channel = module.createChannel();
		// Former format : AMPLITUDE_MIN/MAX in volts
		setLegacyCalibration(channel, "-5", "5", "0", "100");
		assertTrue(ADWinAnInChannelProperties.CALIBRATION.migrate(channel, true));
		assertCalibration(channel, "16383.75", "49151.25", "0", "100", "true");
		// Same physical values as before : voltage of the raw value, then former line
		double[] calibration = ADWinAnInChannelProperties.CALIBRATION.getCalibration(channel);
		RawCalibration.Range range = ADWinAnInChannelProperties.getDefaultRange(channel);
		for (double raw : new double[] {0, 1234, 32767.5, 65535}) {
			double formerValue = (100 - 0) / (5.0 - (-5.0)) * (range.toVolt(raw) - (-5)) + 0;
			assertEquals(formerValue, RawCalibration.compute(raw, calibration[0], calibration[1], calibration[2], calibration[3]), EPSILON);
		}
	}

	private static void setLegacyCalibration(Channel channel, String aMin, String aMax, String uMin, String uMax) {
		channel.setProperty(ADWinAnInChannelProperties.AMPLITUDE_MIN, aMin);
		channel.setProperty(ADWinAnInChannelProperties.AMPLITUDE_MAX, aMax);
		channel.setProperty(ADWinAnInChannelProperties.UNIT_MIN, uMin);
		channel.setProperty(ADWinAnInChannelProperties.UNIT_MAX, uMax);
		// Not defined in the configurations saved before the calibration from the raw values
		channel.setProperty(ADWinAnInChannelProperties.CALIBRATED, null);
	}

}
