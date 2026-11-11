Generic calibration and monitoring firmware for Arduino Uno R3 and R4 WiFi
==========================================================================

DOCoMETRe uploads one of these precompiled files on the board when the
"Calibration, monitoring" dialog of an Arduino DACQ configuration is started,
then configures it from the DACQ configuration (analog reference, inputs,
outputs, ADS1115 modules) through the serial port (115200 bauds).

  ArduinoUnoCalibration_R3.hex       Arduino Uno R3     (arduino:avr:uno)
  ArduinoUnoCalibration_R4WiFi.bin   Arduino Uno R4 WiFi (arduino:renesas_uno:unor4wifi)
  ArduinoUnoCalibration/             sources (sketch and serial protocol)

The serial protocol is described in ArduinoUnoCalibration/CalibrationProtocol.h.
It is tested on the computer by ArduinoUnoCalibrationProtocolTest (Docometre.Tests).

Compile the sources again (after a modification of the sketch or of the protocol)
--------------------------------------------------------------------------------

Built with arduino-cli 1.6.0, cores arduino:avr 1.8.8 and arduino:renesas_uno 1.6.0,
library ADS1X15 0.6.2 (Rob Tillaart).

  arduino-cli core install arduino:avr arduino:renesas_uno
  arduino-cli lib install ADS1X15
  arduino-cli compile --fqbn arduino:avr:uno --output-dir buildR3 ArduinoUnoCalibration
  arduino-cli compile --fqbn arduino:renesas_uno:unor4wifi --output-dir buildR4 ArduinoUnoCalibration

then copy buildR3/ArduinoUnoCalibration.ino.hex as ArduinoUnoCalibration_R3.hex
and buildR4/ArduinoUnoCalibration.ino.bin as ArduinoUnoCalibration_R4WiFi.bin.
When the protocol changes, increase CAL_PROTOCOL_VERSION in CalibrationProtocol.h
and the version expected by DOCoMETRe (ArduinoUnoCalibrationProtocol.VERSION).
