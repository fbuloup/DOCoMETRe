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
#Include ADwinGoldII.Inc

'Pour l'ampli multiplex� des entr�es analogiques
DIM bits_AnIn AS LONG
DIM bits_AnIn_Temp AS FLOAT 
DIM numADC AS LONG
DIM bitPattern AS LONG

INIT :

GLOBALDELAY = 10^9/(1000*3.3)

'Entr�es analogiques
par_1=0'Num�ro de l'entr�e analogique 1 .. 32
par_2=0'Valeur associ�e � l'entr�e analogique s�lectionn�e
par_14=1 'gain des entr�es analogiques 1,2,4 ou 8
'par_12 = 0 'SE ou DIFF : pas besoin car les entrées sont différentielles par défaut dans les Gold (donc 16 entrées différentielles)

'Entr�es num�riques
par_3=0'Num�ro de l'entr�e num�rique
par_4=0'Valeur associ�e � l'entr�e num�rique s�lectionn�e

'Sorties analogiques
par_5=0'Num�ro de la sortie analogique 1 .. 4
par_6=0'Valeur associ�e � la sortie analogique s�lectionn�e 0 .. 65535

'Sortie num�riques
par_7=-1'Num�ro de la sortie num�rique 0 .. 31
par_8=0'Valeur associ�e � la sortie num�rique s�lectionn�e 0 ou 1

'Programmation du DIO tout en entrée par défaut à la mise sous tension : inutile sur les golds
'Sur les golds : 16 sorties digitales de 1 à 16 et 16 entrées digitales de 17 à 32
CONF_DIO(1100b)
'par_9=0
'par_10=0
'par_11=0
'par_12=0 'SE ou DIFF : pas besoin car les entr�es sont diff�rentielles par d�faut
'dans les Gold (donc 16 entr�es diff�rentielle)
par_13=DIGIN_WORD1() + shift_left(GET_DIGOUT_WORD2(),16)'Lecture etat initial des ESnum


EVENT :
'Acquisition entr�e analogique
IF (par_1>0) then        
  bits_AnIn_Temp = LOG(par_14)/LOG(2)
  bits_AnIn = bits_AnIn_Temp 'Gain PGA
  
  if ((par_1 = 1) or (par_1=2)) then 
    bits_AnIn = shift_left(bits_AnIn,3)
    if(par_1=1) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((par_1 = 3) or (par_1=4)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 1
    if(par_1=3) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((par_1 = 5) or (par_1=6)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 2
    if(par_1=5) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((par_1 = 7) or (par_1=8)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 3
    if(par_1=7) then 
      numADC = 1
    else 
      numADC = 2 
    endif
  endif
  if ((par_1 = 9) or (par_1=10)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 4
    if(par_1=9) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((par_1 = 11) or (par_1=12)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 5
    if(par_1=11) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((par_1 = 13) or (par_1=14)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 6
    if(par_1=13) then 
      numADC = 1 
    else 
      numADC = 2 
    endif
  endif
  if ((par_1 = 15) or (par_1=16)) then 
    bits_AnIn = shift_left(bits_AnIn,3) + 7
    if(par_1=15) then 
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
  par_2 =read_adc(numADC)
  
  par_1 = 0
ENDIF

'Acquisition entr�e num�rique
IF (par_3>0) then
  par_4 = DIGIN_WORD1() and par_3
  par_3=0
ENDIF

'sortie d'un valeur analogique
IF (par_5>0) then
  DAC(par_5,par_6)
  par_5=0
ENDIF

'Sortie d'un valeur num�rique
IF (par_7>-1) THEN
  IF (par_8 = 0) THEN
    bitPattern = GET_DIGOUT_WORD2()
    bitPattern = bitPattern and not(shift_left(1,par_7 - 16))
  ENDIF
  IF (par_8 = 1) THEN
    bitPattern = GET_DIGOUT_WORD2()
    bitPattern = bitPattern or shift_left(1,par_7 - 16)
  ENDIF
  DIGOUT_WORD2(bitPattern)
  par_7=-1
ENDIF
