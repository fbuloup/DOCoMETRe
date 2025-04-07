'<ADbasic Header, Headerversion 001.001>
'<Header End>
FUNCTION Call_AnIn(moduleNumber, channelNumber, gain, ampMin, ampMax, unitMin, unitMax) AS FLOAT
	
  DIM bits AS LONG
  DIM temporaryValue AS FLOAT
 		
  bits = shift_left(LOG(gain)/LOG(2),5) + channelNumber - 1

  set_mux(moduleNumber, bits)
  sleep(60)
  start_conv(moduleNumber)
  wait_eoc(moduleNumber)
		
  temporaryValue = (Analog_In_Max - Analog_In_Min) / 65535 * readadc(moduleNumber) + Analog_In_Min

  Call_AnIn = (unitMax - unitMin) / (ampMax - ampMin)*(temporaryValue - ampMin) + unitMin
		
ENDFUNCTION
