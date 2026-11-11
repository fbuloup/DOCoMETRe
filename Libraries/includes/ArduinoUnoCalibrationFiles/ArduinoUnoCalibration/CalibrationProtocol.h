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
 * Generic calibration and monitoring firmware for Arduino Uno R3 and R4 WiFi :
 * serial protocol, independent of the board (see ArduinoUnoCalibration.ino for
 * the hardware functions). This file is also compiled on the computer to test the
 * protocol (see ArduinoUnoCalibrationProtocolTest in Docometre.Tests).
 *
 * The firmware is precompiled : DOCoMETRe configures it at start, from the DACQ
 * configuration, then it sends the values of the inputs periodically.
 *
 * Commands (one line each, ended by '\n') and answers :
 *   I                          identification : "CAL<version>:<board>" (e.g. "CAL1:R3")
 *   C                          clear : stop, outputs reset, inputs and outputs removed -> "K"
 *   F<reference>               analog reference (DEFAULT, INTERNAL, EXTERNAL, AR_DEFAULT, AR_EXTERNAL) -> "K"
 *   T<period>                  period of the measures in ms (5 to 1000, default 20) -> "K"
 *   A<pin>                     adds an analog input -> "K"
 *   B<address>:<data rate>     starts an ADS1115 module (address 0x48 to 0x4B) -> "K"
 *   S<address>:<channel>:<gain> adds an ADS1115 input (gain 0 for 2/3, 1, 2, 4, 8 or 16) -> "K"
 *   D<pin>:<pull up>           adds a digital input (pull up 0 or 1) -> "K"
 *   P<pin>                     adds a digital output (LOW) -> "K"
 *   Q<pin>                     adds an analog output (0) -> "K"
 *   G                          starts the measures -> "K"
 *   H                          stops the measures, outputs reset -> "K"
 *   W<pin>:<value>             writes a digital output (0 or 1), no answer
 *   O<pin>:<value>             writes an analog output, no answer
 * A wrong command is answered by "E:<command>".
 * While started, the values of the inputs, in the order they were added, are sent
 * every period : "M:<value 1>;<value 2>;..." (raw values of the converters).
 */
#ifndef CALIBRATION_PROTOCOL_H
#define CALIBRATION_PROTOCOL_H

#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#define CAL_PROTOCOL_VERSION 1
#define CAL_MAX_INPUTS 24
#define CAL_MAX_OUTPUTS 24
#define CAL_ADS_FIRST_ADDRESS 0x48
#define CAL_MAX_ADS 4
#define CAL_LINE_SIZE 40
#define CAL_FRAME_SIZE 200
#define CAL_DEFAULT_PERIOD 20

// Hardware functions, implemented by the sketch (or by the test harness)
const char* calBoardName();
bool calAnalogReference(const char* name);
long calAnalogRead(uint8_t pin);
bool calADSBegin(uint8_t index, uint8_t address, uint8_t dataRate);
long calADSRead(uint8_t index, uint8_t channel, uint8_t gain);
int calDigitalRead(uint8_t pin);
void calPinModeInput(uint8_t pin, bool pullUp);
void calPinModeOutput(uint8_t pin);
void calDigitalWrite(uint8_t pin, uint8_t value);
void calAnalogWrite(uint8_t pin, int value);
void calPrintLine(const char* line);
unsigned long calMillis();

enum CalInputType { CAL_ANALOG = 0, CAL_ADS = 1, CAL_DIGITAL = 2 };

struct CalInput {
	uint8_t type;
	uint8_t pin;
	uint8_t ads;
	uint8_t gain;
};

struct CalOutput {
	uint8_t pin;
	bool analog;
};

static CalInput calInputs[CAL_MAX_INPUTS];
static uint8_t calInputsNumber = 0;
static CalOutput calOutputs[CAL_MAX_OUTPUTS];
static uint8_t calOutputsNumber = 0;
static bool calADSStarted[CAL_MAX_ADS];
static bool calRunning = false;
static unsigned long calPeriod = CAL_DEFAULT_PERIOD;
static unsigned long calLastMeasuresTime = 0;
static char calLine[CAL_LINE_SIZE];
static uint8_t calLineLength = 0;
static bool calLineOverflow = false;

/*
 * Parses an unsigned integer (decimal, or hexadecimal with 0x prefix) at *text,
 * followed by end of text or by separator ':' : *text is moved after it
 */
static bool calParseNumber(const char** text, long minimum, long maximum, long* value) {
	char* end;
	long number = strtol(*text, &end, 0);
	if(end == *text || number < minimum || number > maximum) return false;
	if(*end != '\0' && *end != ':') return false;
	*value = number;
	*text = (*end == ':') ? end + 1 : end;
	return true;
}

static bool calIsADSGain(long gain) {
	return gain == 0 || gain == 1 || gain == 2 || gain == 4 || gain == 8 || gain == 16;
}

static int calFindOutput(uint8_t pin, bool analog) {
	for (uint8_t i = 0; i < calOutputsNumber; i++) if(calOutputs[i].pin == pin && calOutputs[i].analog == analog) return i;
	return -1;
}

/*
 * Outputs to 0 / LOW ; digital outputs are set in high impedance (input) when release is true
 */
static void calResetOutputs(bool release) {
	for (uint8_t i = 0; i < calOutputsNumber; i++) {
		if(calOutputs[i].analog) calAnalogWrite(calOutputs[i].pin, 0);
		else {
			calDigitalWrite(calOutputs[i].pin, 0);
			if(release) calPinModeInput(calOutputs[i].pin, false);
		}
	}
}

static void calClear() {
	calRunning = false;
	calResetOutputs(true);
	calInputsNumber = 0;
	calOutputsNumber = 0;
	calPeriod = CAL_DEFAULT_PERIOD;
}

static bool calAddInput(uint8_t type, uint8_t pin, uint8_t ads, uint8_t gain) {
	if(calInputsNumber >= CAL_MAX_INPUTS) return false;
	calInputs[calInputsNumber].type = type;
	calInputs[calInputsNumber].pin = pin;
	calInputs[calInputsNumber].ads = ads;
	calInputs[calInputsNumber].gain = gain;
	calInputsNumber++;
	return true;
}

static bool calAddOutput(uint8_t pin, bool analog) {
	if(calFindOutput(pin, analog) >= 0) return true;
	if(calOutputsNumber >= CAL_MAX_OUTPUTS) return false;
	calOutputs[calOutputsNumber].pin = pin;
	calOutputs[calOutputsNumber].analog = analog;
	calOutputsNumber++;
	if(analog) {
		calPinModeOutput(pin);
		calAnalogWrite(pin, 0);
	} else {
		calPinModeOutput(pin);
		calDigitalWrite(pin, 0);
	}
	return true;
}

/*
 * Executes a command, returns false if it is wrong
 */
static bool calExecute(const char* line) {
	char command = line[0];
	const char* arguments = line + 1;
	long a, b, c;
	char answer[24];
	switch (command) {
	case 'I':
		if(*arguments != '\0') return false;
		snprintf(answer, sizeof(answer), "CAL%d:%s", CAL_PROTOCOL_VERSION, calBoardName());
		calPrintLine(answer);
		return true;
	case 'C':
		if(*arguments != '\0') return false;
		calClear();
		break;
	case 'F':
		if(!calAnalogReference(arguments)) return false;
		break;
	case 'T':
		if(!calParseNumber(&arguments, 5, 1000, &a) || *arguments != '\0') return false;
		calPeriod = (unsigned long)a;
		break;
	case 'A':
		if(!calParseNumber(&arguments, 0, 255, &a) || *arguments != '\0') return false;
		if(!calAddInput(CAL_ANALOG, (uint8_t)a, 0, 0)) return false;
		break;
	case 'B':
		if(!calParseNumber(&arguments, CAL_ADS_FIRST_ADDRESS, CAL_ADS_FIRST_ADDRESS + CAL_MAX_ADS - 1, &a)) return false;
		if(!calParseNumber(&arguments, 0, 7, &b) || *arguments != '\0') return false;
		if(!calADSBegin((uint8_t)(a - CAL_ADS_FIRST_ADDRESS), (uint8_t)a, (uint8_t)b)) return false;
		calADSStarted[a - CAL_ADS_FIRST_ADDRESS] = true;
		break;
	case 'S':
		if(!calParseNumber(&arguments, CAL_ADS_FIRST_ADDRESS, CAL_ADS_FIRST_ADDRESS + CAL_MAX_ADS - 1, &a)) return false;
		if(!calParseNumber(&arguments, 0, 3, &b)) return false;
		if(!calParseNumber(&arguments, 0, 16, &c) || *arguments != '\0' || !calIsADSGain(c)) return false;
		if(!calADSStarted[a - CAL_ADS_FIRST_ADDRESS]) return false;
		if(!calAddInput(CAL_ADS, (uint8_t)b, (uint8_t)(a - CAL_ADS_FIRST_ADDRESS), (uint8_t)c)) return false;
		break;
	case 'D':
		if(!calParseNumber(&arguments, 0, 255, &a)) return false;
		if(!calParseNumber(&arguments, 0, 1, &b) || *arguments != '\0') return false;
		if(!calAddInput(CAL_DIGITAL, (uint8_t)a, 0, 0)) return false;
		calPinModeInput((uint8_t)a, b == 1);
		break;
	case 'P':
	case 'Q':
		if(!calParseNumber(&arguments, 0, 255, &a) || *arguments != '\0') return false;
		if(!calAddOutput((uint8_t)a, command == 'Q')) return false;
		break;
	case 'G':
		if(*arguments != '\0') return false;
		calRunning = true;
		calLastMeasuresTime = calMillis() - calPeriod;
		break;
	case 'H':
		if(*arguments != '\0') return false;
		calRunning = false;
		calResetOutputs(false);
		break;
	case 'W':
	case 'O': {
		if(!calParseNumber(&arguments, 0, 255, &a)) return false;
		if(!calParseNumber(&arguments, 0, command == 'W' ? 1 : 65535, &b) || *arguments != '\0') return false;
		int index = calFindOutput((uint8_t)a, command == 'O');
		if(index < 0) return false;
		if(command == 'W') calDigitalWrite((uint8_t)a, (uint8_t)b);
		else calAnalogWrite((uint8_t)a, (int)b);
		// No answer : outputs are written while measures are sent
		return true;
	}
	default:
		return false;
	}
	calPrintLine("K");
	return true;
}

static void calHandleLine(const char* line) {
	if(!calExecute(line)) {
		char answer[CAL_LINE_SIZE + 3];
		snprintf(answer, sizeof(answer), "E:%s", line);
		calPrintLine(answer);
	}
}

/*
 * To call with each char received on the serial port
 */
static void calReceive(char c) {
	if(c == '\n' || c == '\r') {
		if(calLineOverflow) calPrintLine("E:overflow");
		else if(calLineLength > 0) {
			calLine[calLineLength] = '\0';
			calHandleLine(calLine);
		}
		calLineLength = 0;
		calLineOverflow = false;
		return;
	}
	if(calLineLength < CAL_LINE_SIZE - 1) calLine[calLineLength++] = c;
	else calLineOverflow = true;
}

/*
 * Sends the values of the inputs when the period is elapsed. To call in loop()
 */
static void calUpdate() {
	if(!calRunning) return;
	unsigned long now = calMillis();
	if(now - calLastMeasuresTime < calPeriod) return;
	calLastMeasuresTime = now;
	char frame[CAL_FRAME_SIZE];
	size_t length = 0;
	frame[length++] = 'M';
	frame[length++] = ':';
	for (uint8_t i = 0; i < calInputsNumber; i++) {
		long value = 0;
		if(calInputs[i].type == CAL_ANALOG) value = calAnalogRead(calInputs[i].pin);
		else if(calInputs[i].type == CAL_ADS) value = calADSRead(calInputs[i].ads, calInputs[i].pin, calInputs[i].gain);
		else value = calDigitalRead(calInputs[i].pin);
		int written = snprintf(frame + length, CAL_FRAME_SIZE - length, i == 0 ? "%ld" : ";%ld", value);
		if(written < 0 || (size_t)written >= CAL_FRAME_SIZE - length) break;
		length += written;
	}
	frame[length] = '\0';
	calPrintLine(frame);
}

#endif
