****************************************
*	 Prozeﬂnummer = 1
*	 Delay = 1000
*	 Eventsource = 0
*	 Number of Loops = 0
*	 Priorit‰t = 0
*	 Version = 1
*	 FastStop = 0
*	 AdbasicVersion = 4000001
*	 ATSRAM = 0
*	 OPT_LEVEL = 1
*	 SAVECOMPIL = 0
****************************************
FUNCTION Call_AnIn(canal_AnIn,gain_AnIn,Amin_AnIn,Amax_AnIn,Umin_AnIn,Umax_AnIn) AS FLOAT
	
DIM bits_AnIn AS LONG
DIM bits_AnIn_Temp AS FLOAT
DIM bits_AnIn_Temp2 AS LONG
DIM Call_AnIn_Temp AS FLOAT
DIM numADC AS LONG
 		
		bits_AnIn_Temp = LOG(gain_AnIn)/LOG(2)+0.1	
		bits_AnIn_Temp2 = bits_AnIn_Temp
		bits_AnIn = shift_left(bits_AnIn_Temp2 ,2)'Gain PGA 2
		bits_AnIn = bits_AnIn  + bits_AnIn_Temp2 'Gain PGA 1
		
		if ((canal_AnIn = 1) or (canal_AnIn=2)) then 
			bits_AnIn = shift_left(bits_AnIn,3)
			bits_AnIn = shift_left(bits_AnIn,3)
			if(canal_AnIn=1) then 
				numADC = 1 
			else 
				numADC = 2 
			endif
		endif
		if ((canal_AnIn = 3) or (canal_AnIn=4)) then 
			bits_AnIn = shift_left(bits_AnIn,3) + 1
			bits_AnIn = shift_left(bits_AnIn,3) + 1 
			if(canal_AnIn=3) then 
					numADC = 1 
			else 
					numADC = 2 
			endif
		endif
		if ((canal_AnIn = 5) or (canal_AnIn=6)) then 
			bits_AnIn = shift_left(bits_AnIn,3) + 2
			bits_AnIn = shift_left(bits_AnIn,3) + 2 
			if(canal_AnIn=5) then 
				numADC = 1 
			else 
				numADC = 2 
			endif
		endif
		if ((canal_AnIn = 7) or (canal_AnIn=8)) then 
			bits_AnIn = shift_left(bits_AnIn,3) + 3
			bits_AnIn = shift_left(bits_AnIn,3) + 3 
			if(canal_AnIn=7) then 
					numADC = 1
			else 
					numADC = 2 
			endif
		endif
		if ((canal_AnIn = 9) or (canal_AnIn=10)) then 
			bits_AnIn = shift_left(bits_AnIn,3) + 4
			bits_AnIn = shift_left(bits_AnIn,3) + 4 
			if(canal_AnIn=9) then 
					numADC = 1 
			else 
					numADC = 2 
			endif
		endif
		if ((canal_AnIn = 11) or (canal_AnIn=12)) then 
			bits_AnIn = shift_left(bits_AnIn,3) + 5
			bits_AnIn = shift_left(bits_AnIn,3) + 5 
			if(canal_AnIn=11) then 
					numADC = 1 
			else 
				numADC = 2 
			endif
		endif
		if ((canal_AnIn = 13) or (canal_AnIn=14)) then 
			bits_AnIn = shift_left(bits_AnIn,3) + 6
			bits_AnIn = shift_left(bits_AnIn,3) + 6 
			if(canal_AnIn=13) then 
				numADC = 1 
			else 
				numADC = 2 
			endif
		endif
		if ((canal_AnIn = 15) or (canal_AnIn=16)) then 
			bits_AnIn = shift_left(bits_AnIn,3) + 7
			bits_AnIn = shift_left(bits_AnIn,3) + 7 
			if(canal_AnIn=15) then 
				numADC = 1 
			else 
				numADC = 2 
			endif
		endif

		set_mux(bits_AnIn)		
		sleep(60)
		start_conv(11011b)
		wait_eoc(11b)
		if (numADC = 1) then
		  Call_AnIn_Temp =readadc(1)
		else
			Call_AnIn_Temp =readadc(2)
		endif
		Call_AnIn_Temp = (Analog_In_Max-Analog_In_Min)/65535*Call_AnIn_Temp +Analog_In_Min
		Call_AnIn = (Umax_Anin-Umin_Anin)/(Amax_AnIn-Amin_AnIn)*(Call_AnIn_Temp-Amin_AnIn)+Umin_AnIn
		
ENDFUNCTION



		

