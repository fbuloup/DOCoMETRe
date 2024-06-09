****************************************
*	 Prozeßnummer = 1
*	 Delay = 1000
*	 Eventsource = 0
*	 Number of Loops = 0
*	 Priorität = 0
*	 Version = 1
*	 FastStop = 0
*	 AdbasicVersion = 4000001
*	 ATSRAM = 0
*	 OPT_LEVEL = 1
*	 SAVECOMPIL = 0
****************************************
SUB Call_ConvIEEE754SendICE(numPserie,num_canal_ConvIEEE754_SendICE,valeur_ConvIEEE754_SendICE)

DIM Exposant_ConvIEEE754,mantisse_ConvIEEE754,bit_ConvIEEE754,signe_ConvIEEE754,valeur_IEEE754 AS INTEGER
DIM Compteur_de_Boucle,byte_IEEE754 AS INTEGER
DIM valeur_ConvIEEE754 AS FLOAT
		
	valeur_ConvIEEE754 = valeur_ConvIEEE754_SendICE
	
	'Conversion au format IEEE754
	if (valeur_ConvIEEE754 >= 0) then
		signe_ConvIEEE754 = 0 
	else
		signe_ConvIEEE754 = 1 
	endif
	if (valeur_ConvIEEE754 <> 0) then
		Exposant_ConvIEEE754 = 127+ln(absf(valeur_ConvIEEE754))/ln(2)
	else
		Exposant_ConvIEEE754 = 0
	endif
	if (valeur_ConvIEEE754 <> 0) then 
		valeur_ConvIEEE754 = absf(valeur_ConvIEEE754 )/2^(exposant_ConvIEEE754-127)-1	
	endif
	mantisse_ConvIEEE754 = 0
	for Compteur_de_Boucle=0 to 22
		valeur_ConvIEEE754 = 2*valeur_ConvIEEE754 
		mantisse_ConvIEEE754 = shift_left(mantisse_ConvIEEE754,1)
		bit_ConvIEEE754 = valeur_ConvIEEE754 
		mantisse_ConvIEEE754 = mantisse_ConvIEEE754 OR bit_ConvIEEE754
		valeur_ConvIEEE754 = valeur_ConvIEEE754 - bit_ConvIEEE754 
	Next Compteur_de_Boucle

	'Envoie de la valeur
	valeur_IEEE754 = shift_left(signe_ConvIEEE754,31) + shift_left(exposant_ConvIEEE754,23)+mantisse_ConvIEEE754
	byte_IEEE754 = valeur_IEEE754
	if (write_fifo(numPserie,num_canal_ConvIEEE754_SendICE) = 0) then
		if (write_fifo(numPserie,byte_IEEE754) = 0) then
			byte_IEEE754 = shift_right(byte_IEEE754,8)
			if (write_fifo(numPserie,byte_IEEE754) = 0) then
				byte_IEEE754 = shift_right(byte_IEEE754,8)
				if (write_fifo(numPserie,byte_IEEE754) = 0) then
					byte_IEEE754 = shift_right(byte_IEEE754,8)
					byte_IEEE754 = write_fifo(numPserie,byte_IEEE754)
				endif	
			endif
		endif
	endif

ENDSUB