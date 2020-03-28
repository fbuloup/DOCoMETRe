'<ADbasic Header, Headerversion 001.001>
' Process_Number                 = 10
' Initial_Processdelay           = 400000
' Eventsource                    = Timer
' Control_long_Delays_for_Stop   = No
' Priority                       = High
' Version                        = 1
' ADbasic_Version                = 6.0.0
' Optimize                       = Yes
' Optimize_Level                 = 1
' Stacksize                      = 1000
' Info_Last_Save                 = FRANK-PC  frank-PC\frank
'<Header End>
'Pour l'ampli multiplexé des entrées analogiques
DIM bits_AnIn,bits_AnIn_Temp2 AS LONG
DIM bits_AnIn_Temp AS FLOAT 
DIM numADC AS LONG

INIT :

GLOBALDELAY = 10^9/(1000*25)

'Entrées analogiques
par_1 = 0 'Numéro de l'entrée analogique : 1 .. 32
par_2 = 0 'Valeur associée à l'entrée analogique sélectionnée
par_14 = 1 'gain des entrées analogiques : 1,2,4 ou 8
'par_12 = 0 'SE ou DIFF : pas besoin car les entrées sont différentielles par défaut dans les Gold (donc 16 entrées différentielles)

'Entrées numériques
par_3 = 0 'Numéro de l'entrée numérique
par_4 = 0 'Valeur associée à l'entrée numérique sélectionnée

'Sorties analogiques
par_5 = 0 'Numéro de la sortie analogique : 1 .. 4
par_6 = 0 'Valeur associée à la sortie analogique sélectionnée : 0 .. 65535

'Sortie numériques
par_7 = -1 ' Numéro de la sortie numérique : 0 .. 31
par_8 = 0 'Valeur associée à la sortie numérique sélectionnée : 0 ou 1

'Programmation du DIO tout en entrée par défaut à la mise sous tension : inutile sur les golds
'Sur les golds : 16 sorties digitales de 1 à 16 et 16 entrées digitales de 17 à 32
CONF_DIO(1100b)
'par_9 = 0
'par_10 = 0
'par_11 = 0
par_13 = DIGIN_WORD() + shift_left(PEEK(204001B0h),16) 'Lecture état initial des ESnum

EVENT :
'Acquisition entrée analogique
IF (par_1 > 0) THEN        
  bits_AnIn_Temp = LOG(par_14)/LOG(2)+0.1  
  bits_AnIn_Temp2 = bits_AnIn_Temp
  bits_AnIn = shift_left(bits_AnIn_Temp2 ,2) 'Gain PGA 2
  bits_AnIn = bits_AnIn  + bits_AnIn_Temp2   'Gain PGA 1
    
  IF ((par_1 = 1) or (par_1 = 2)) THEN 
    bits_AnIn = shift_left(bits_AnIn,3)
    bits_AnIn = shift_left(bits_AnIn,3)
    IF(par_1 = 1) THEN 
      numADC = 1 
    ELSE 
      numADC = 2 
    ENDIF
  ENDIF
  IF ((par_1 = 3) or (par_1 = 4)) THEN 
    bits_AnIn = shift_left(bits_AnIn,3) + 1
    bits_AnIn = shift_left(bits_AnIn,3) + 1 
    IF(par_1 = 3) THEN 
      numADC = 1 
    ELSE 
      numADC = 2 
    ENDIF
  ENDIF
  IF ((par_1 = 5) or (par_1 = 6)) THEN 
    bits_AnIn = shift_left(bits_AnIn,3) + 2
    bits_AnIn = shift_left(bits_AnIn,3) + 2 
    IF(par_1 = 5) THEN 
      numADC = 1 
    ELSE 
      numADC = 2 
    ENDIF
  ENDIF
  IF ((par_1 = 7) or (par_1 = 8)) THEN 
    bits_AnIn = shift_left(bits_AnIn,3) + 3
    bits_AnIn = shift_left(bits_AnIn,3) + 3 
    IF(par_1 = 7) THEN 
      numADC = 1
    ELSE 
      numADC = 2 
    ENDIF
  ENDIF
  IF ((par_1 = 9) or (par_1 = 10)) THEN 
    bits_AnIn = shift_left(bits_AnIn,3) + 4
    bits_AnIn = shift_left(bits_AnIn,3) + 4 
    IF(par_1 = 9) THEN 
      numADC = 1 
    ELSE 
      numADC = 2 
    ENDIF
  ENDIF
  IF ((par_1 = 11) or (par_1 = 12)) THEN 
    bits_AnIn = shift_left(bits_AnIn,3) + 5
    bits_AnIn = shift_left(bits_AnIn,3) + 5 
    IF(par_1 = 11) THEN 
      numADC = 1 
    ELSE 
      numADC = 2 
    ENDIF
  ENDIF
  IF ((par_1 = 13) or (par_1 = 14)) THEN 
    bits_AnIn = shift_left(bits_AnIn,3) + 6
    bits_AnIn = shift_left(bits_AnIn,3) + 6 
    IF(par_1 = 13) THEN 
      numADC = 1 
    ELSE 
      numADC = 2 
    ENDIF
  ENDIF
  IF ((par_1 = 15) or (par_1 = 16)) THEN 
    bits_AnIn = shift_left(bits_AnIn,3) + 7
    bits_AnIn = shift_left(bits_AnIn,3) + 7 
    IF(par_1 = 15) THEN 
      numADC = 1 
    ELSE 
      numADC = 2 
    ENDIF
  ENDIF

  set_mux(bits_AnIn)    
  sleep(60)
  start_conv(11011b)
  wait_eoc(11b)
  IF (numADC = 1) THEN
    par_2 = readadc(1)
  ELSE
    par_2 = readadc(2)
  ENDIF
  par_1 = 0
ENDIF

'Acquisition entrée numérique
IF (par_3 > 0) THEN
  par_4 = DIGIN_WORD() and par_3
  par_3 = 0
ENDIF

'sortie d'une valeur analogique
IF (par_5 > 0) THEN
  DAC(par_5,par_6)
  par_5=0
ENDIF

'Sortie d'un valeur numérique
IF (par_7 > -1) THEN
  IF (par_8 = 1) THEN SET_DIGOUT(par_7 - 16)
  IF (par_8 = 0) THEN CLEAR_DIGOUT(par_7 - 16)
  par_7 = -1
ENDIF
