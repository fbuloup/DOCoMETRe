'<ADbasic Header, Headerversion 001.001>
'<Header End>
FUNCTION Call_AnIn(moduleNumber, channelNumber, gain, ampMin, ampMax, unitMin, unitMax) AS FLOAT
  
  DIM bits AS LONG
  DIM temporaryValue AS FLOAT
     
  bits = shift_left(LOG(gain_AnIn)/LOG(2),5) + channelNumber - 1

  P2_Set_Mux(moduleNumber,bits)    
  P2_Wait_Mux(moduleNumber)
  P2_Start_Conv(moduleNumber)
  P2_Wait_EOC(moduleNumber)
  
  temporaryValue = (Analog_In_Max - Analog_In_Min) / 65535 * P2_Read_ADC(moduleNumber) + Analog_In_Min
      
  Call_AnIn = (unitMax - unitMin) / (ampMax - ampMin)*(temporaryValue - ampMin) + unitMin
    
ENDFUNCTION
