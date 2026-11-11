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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [11/11/2026]
 ******************************************************************************/
/*
 * Generic calibration and monitoring firmware for Arduino Uno R3 and R4 WiFi,
 * precompiled and uploaded by DOCoMETRe (Calibration, monitoring dialog of the
 * Arduino DACQ configurations). See CalibrationProtocol.h for the serial protocol
 * and README.txt to compile it again.
 */
#include <Wire.h>
#include "ADS1X15.h"
#include "CalibrationProtocol.h"

// Fixed baud rate : the firmware is precompiled
#define CAL_BAUD_RATE 115200

static ADS1115 ads0(0x48);
static ADS1115 ads1(0x49);
static ADS1115 ads2(0x4A);
static ADS1115 ads3(0x4B);
static ADS1115* ads[CAL_MAX_ADS] = {&ads0, &ads1, &ads2, &ads3};
static bool wireStarted = false;

const char* calBoardName() {
#if defined(ARDUINO_ARCH_RENESAS)
	return "R4";
#else
	return "R3";
#endif
}

/*
 * Analog reference given by its name in the DACQ configuration : DEFAULT, INTERNAL or
 * EXTERNAL on R3, AR_DEFAULT, AR_INTERNAL or AR_EXTERNAL on R4 WiFi
 */
bool calAnalogReference(const char* name) {
#if defined(ARDUINO_ARCH_RENESAS)
	if(strcmp(name, "AR_DEFAULT") == 0) analogReference(AR_DEFAULT);
	else if(strcmp(name, "AR_INTERNAL") == 0) analogReference(AR_INTERNAL);
	else if(strcmp(name, "AR_EXTERNAL") == 0) analogReference(AR_EXTERNAL);
	else return false;
#else
	if(strcmp(name, "DEFAULT") == 0) analogReference(DEFAULT);
	else if(strcmp(name, "INTERNAL") == 0) analogReference(INTERNAL);
	else if(strcmp(name, "EXTERNAL") == 0) analogReference(EXTERNAL);
	else return false;
#endif
	return true;
}

long calAnalogRead(uint8_t pin) {
	return analogRead(pin);
}

bool calADSBegin(uint8_t index, uint8_t address, uint8_t dataRate) {
	(void)address;
	if(!wireStarted) {
		Wire.begin();
		wireStarted = true;
	}
	if(!ads[index]->begin()) return false;
	// Single shot : inputs of a module are read one after the other
	ads[index]->setMode(1);
	ads[index]->setDataRate(dataRate);
	return true;
}

long calADSRead(uint8_t index, uint8_t channel, uint8_t gain) {
	ads[index]->setGain(gain);
	return ads[index]->readADC(channel);
}

int calDigitalRead(uint8_t pin) {
	return digitalRead(pin);
}

void calPinModeInput(uint8_t pin, bool pullUp) {
	pinMode(pin, pullUp ? INPUT_PULLUP : INPUT);
}

void calPinModeOutput(uint8_t pin) {
	pinMode(pin, OUTPUT);
}

void calDigitalWrite(uint8_t pin, uint8_t value) {
	digitalWrite(pin, value ? HIGH : LOW);
}

void calAnalogWrite(uint8_t pin, int value) {
#if defined(ARDUINO_ARCH_RENESAS)
	if(pin == DAC) {
		// DAC (pin A0) : 12 bits, the resolution is common to all the analog outputs (PWM outputs stay 8 bits)
		analogWriteResolution(12);
		analogWrite(pin, value);
		analogWriteResolution(8);
		return;
	}
#endif
	analogWrite(pin, value);
}

void calPrintLine(const char* line) {
	Serial.println(line);
}

unsigned long calMillis() {
	return millis();
}

void setup() {
#if defined(ARDUINO_ARCH_RENESAS)
	// Same resolution as the acquisition processes
	analogReadResolution(14);
#endif
	Serial.begin(CAL_BAUD_RATE);
	// Identification, also sent when the board restarts (R3 restarts when the serial port is opened)
	calExecute("I");
}

void loop() {
	while(Serial.available() > 0) calReceive((char)Serial.read());
	calUpdate();
}
