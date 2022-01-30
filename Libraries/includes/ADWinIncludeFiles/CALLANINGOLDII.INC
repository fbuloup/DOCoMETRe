'<ADbasic Header, Headerversion 001.001>
'<Header End>
FUNCTION Call_AnIn(canal_AnIn,gain_AnIn,Amin_AnIn,Amax_AnIn,Umin_AnIn,Umax_AnIn) AS FLOAT
  
  DIM bits_AnIn AS LONG
  DIM bits_AnIn_Temp AS FLOAT
  DIM Call_AnIn_Temp AS FLOAT
  DIM numADC AS LONG
     
  bits_AnIn_Temp = LOG(gain_AnIn)/LOG(2)
  bits_AnIn = bits_AnIn_Temp 'Gain PGA
    
  if ((canal_AnIn = 1) or (canal_AnIn=2)) then 
    bits_AnIn = shift_left(bits_AnIn,3)
    if(canal_AnIn=1) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((canal_AnIn = 3) or (canal_AnIn=4)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 1
    if(canal_AnIn=3) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((canal_AnIn = 5) or (canal_AnIn=6)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 2
    if(canal_AnIn=5) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((canal_AnIn = 7) or (canal_AnIn=8)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 3
    if(canal_AnIn=7) then 
      numADC = 1
    else 
      numADC = 2 
    endif
  endif
  if ((canal_AnIn = 9) or (canal_AnIn=10)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 4
    if(canal_AnIn=9) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((canal_AnIn = 11) or (canal_AnIn=12)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 5
    if(canal_AnIn=11) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((canal_AnIn = 13) or (canal_AnIn=14)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 6
    if(canal_AnIn=13) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((canal_AnIn = 15) or (canal_AnIn=16)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 7
    if(canal_AnIn=15) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  
  if (numADC = 1) then
    set_mux1(bits_AnIn)    
  else
    set_mux2(bits_AnIn)    
  endif

  IO_sleep(200)
  start_conv(numADC)
  wait_eoc(numADC)  
  if (numADC = 1) then
    Call_AnIn_Temp = Read_ADC(1)
  else
    Call_AnIn_Temp = read_adc(2)
  endif
  
  Call_AnIn_Temp = (Analog_In_Max-Analog_In_Min)/65535*Call_AnIn_Temp +Analog_In_Min
  Call_AnIn = (Umax_Anin-Umin_Anin)/(Amax_AnIn-Amin_AnIn)*(Call_AnIn_Temp-Amin_AnIn)+Umin_AnIn
    
ENDFUNCTION



    

