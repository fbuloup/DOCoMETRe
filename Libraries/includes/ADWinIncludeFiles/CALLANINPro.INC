'<ADbasic Header, Headerversion 001.001>
'<Header End>
' Raw value of the converter of an analog input (0 .. 65535 for the minimum .. maximum voltage of the range of the
' module). gainCode : code of the gain of the PGA, 0, 1, 2, 3 for a gain of 1, 2, 4, 8 (computed by DOCoMETRe).
' The physical value is computed by the generated code : k * Call_AnIn_Raw(...) + b (calibration from the raw values).
FUNCTION Call_AnIn_Raw(moduleNumber, channelNumber, gainCode) AS FLOAT
	
  DIM bits AS LONG
  DIM gainBits AS LONG
 		
  gainBits = gainCode
  bits = shift_left(gainBits,5) + channelNumber - 1

  set_mux(moduleNumber, bits)
  sleep(60)
  start_conv(moduleNumber)
  wait_eoc(moduleNumber)
		
  Call_AnIn_Raw = readadc(moduleNumber)
		
ENDFUNCTION

' Former function : voltage of the input, then calibration from volts (ampMin, ampMax). Kept for the code written
' before the calibration from the raw values, not generated anymore by DOCoMETRe.
FUNCTION Call_AnIn(moduleNumber, channelNumber, gain, ampMin, ampMax, unitMin, unitMax) AS FLOAT
	
  DIM gainCode AS LONG
  DIM gainCodeTemp AS FLOAT
  DIM temporaryValue AS FLOAT
 		
  ' + 0.1 so that a result of LOG(gain)/LOG(2) slightly below the integer value is not truncated
  gainCodeTemp = LOG(gain)/LOG(2) + 0.1
  gainCode = gainCodeTemp
		
  temporaryValue = (Analog_In_Max - Analog_In_Min) / 65535 * Call_AnIn_Raw(moduleNumber, channelNumber, gainCode) + Analog_In_Min

  Call_AnIn = (unitMax - unitMin) / (ampMax - ampMin)*(temporaryValue - ampMin) + unitMin
		
ENDFUNCTION
