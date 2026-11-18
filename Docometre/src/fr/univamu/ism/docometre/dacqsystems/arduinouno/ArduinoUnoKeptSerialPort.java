package fr.univamu.ism.docometre.dacqsystems.arduinouno;

import fr.univamu.ism.docometre.Activator;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * Serial port of an Arduino Uno R3 kept open from a trial to the next.
 *
 * The Uno R3 restarts when its serial port is opened (DTR) and spends about 1.5 s in its bootloader before
 * its process waits for the 'r' char. At the end of a trial, the process is restarted by software and waits
 * for the next 'r' char : if the serial port is not closed, the next trial starts without this restart.
 *
 * The port is kept at the end of a trial ended normally (see ArduinoUnoProcess.RealTimeLoopJob), taken back
 * by the next trial if it uses the same device and baud rate, and closed otherwise : at the end of the run
 * (see ExperimentScheduler), before an upload, or when another port or baud rate is used.
 */
final class ArduinoUnoKeptSerialPort {

	private static SerialPort serialPort;
	private static String devicePath;
	private static int baudRate;

	private ArduinoUnoKeptSerialPort() {
	}

	/**
	 * The kept serial port if it is open for this device and baud rate (it is no more kept : the caller owns
	 * it), null otherwise (a kept port for another device or baud rate is closed).
	 */
	static synchronized SerialPort take(String devicePath, int baudRate) {
		SerialPort port = serialPort;
		if(port != null && port.isOpened() && devicePath != null && devicePath.equals(ArduinoUnoKeptSerialPort.devicePath)
				&& baudRate == ArduinoUnoKeptSerialPort.baudRate) {
			serialPort = null;
			ArduinoUnoKeptSerialPort.devicePath = null;
			return port;
		}
		close();
		return null;
	}

	/** Keeps this open serial port for the next trial (a port kept before is closed). */
	static synchronized void keep(SerialPort port, String devicePath, int baudRate) {
		if(port == serialPort) return;
		close();
		serialPort = port;
		ArduinoUnoKeptSerialPort.devicePath = devicePath;
		ArduinoUnoKeptSerialPort.baudRate = baudRate;
	}

	/** Closes the kept serial port, if any. Returns true if a port was kept. */
	static synchronized boolean close() {
		if(serialPort == null) return false;
		try {
			if(serialPort.isOpened()) serialPort.closePort();
		} catch (SerialPortException e) {
			Activator.logErrorMessageWithCause(e);
		} finally {
			serialPort = null;
			devicePath = null;
		}
		return true;
	}

}
