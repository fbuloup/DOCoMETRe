'<ADbasic Header, Headerversion 001.001>
'<Header End>
FUNCTION Call_AnIn(moduleNumber, channelNumber, gain, ampMin, ampMax, unitMin, unitMax) AS FLOAT
  
  DIM bits AS LONG
  DIM gainBits AS LONG
  DIM gainBitsTemp AS FLOAT
  DIM temporaryValue AS FLOAT
     
  ' Gain code of the PGA (0, 1, 2, 3 for a gain of 1, 2, 4, 8) : + 0.1 so that a result
  ' of LOG(gain)/LOG(2) slightly below the integer value is not truncated
  gainBitsTemp = LOG(gain)/LOG(2) + 0.1
  gainBits = gainBitsTemp
  bits = shift_left(gainBits,5) + channelNumber - 1

  P2_Set_Mux(moduleNumber,bits)    
  P2_Wait_Mux(moduleNumber)
  P2_Start_Conv(moduleNumber)
  P2_Wait_EOC(moduleNumber)
  
  temporaryValue = (Analog_In_Max - Analog_In_Min) / 65535 * P2_Read_ADC(moduleNumber) + Analog_In_Min
      
  Call_AnIn = (unitMax - unitMin) / (ampMax - ampMin)*(temporaryValue - ampMin) + unitMin
    
ENDFUNCTION
