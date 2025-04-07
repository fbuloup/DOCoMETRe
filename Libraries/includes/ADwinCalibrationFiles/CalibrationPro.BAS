'<ADbasic Header, Headerversion 001.001>
' Process_Number                 = 1
' Initial_Processdelay           = 400000
' Eventsource                    = Timer
' Control_long_Delays_for_Stop   = No
' Priority                       = High
' Version                        = 1
' ADbasic_Version                = 6.2.0
' Optimize                       = Yes
' Optimize_Level                 = 1
' Stacksize                      = 1000
' Info_Last_Save                 = DESKTOP-G130F9I  DESKTOP-G130F9I\Utilisateur
'<Header End>
#include adwinpro.inc
#include adwpad.inc
#include adwpdio.inc
#include adwpda.inc

'Pour l'ampli multiplex� des entr�es analogiques
DIM bits_AnIn,bits_AnIn_Temp2 AS LONG
DIM bits_AnIn_Temp AS FLOAT 

INIT :

' Numéro de module
par_15 = 1

'Entr�es analogiques
par_1 = 0'Num�ro de l'entr�e analogique 1 .. 32
par_2 = 0'Valeur associ�e � l'entr�e analogique s�lectionn�e
par_14 = 1 'gain des entr�es analogiques 1,2,4 ou 8
par_12 = 0 '0, mode SE ou 1, mode DIFF
par_16 = 0 ' Programmation SE/DIFF entr�es analogiques

'Sorties analogiques
par_5=0'Num�ro de la sortie analogique 1 .. 4
par_6=0'Valeur associ�e � la sortie analogique s�lectionn�e 0 .. 65535
par_17=0'Num�ro de la sortie analogique 1 .. 4. Pour lecture valeur courante de la sortie.
par_18=0'Valeur associ�e. Pour lecture valeur courante de la sortie

'Prog du DIO tout en entr�e par d�faut � la mise sous tension
par_9 = 0 ' 1 : mode programmation
par_10 = 0 ' 16 premiers bits
par_11 = 0 ' 16 derniers bits
'par_13 : utilis� pour la valeur initiale des sorties

'Entr�es num�riques
par_3 = 0'Num�ro de l'entr�e num�rique : masque binaire. Par ex. : 0000 0010 : entrée numérique numéro deux
par_4 = 0'Valeur associ�e � l'entr�e num�rique s�lectionn�e

'Sortie num�riques
par_7 = -1'Num�ro de la sortie num�rique 0 .. 31
par_8 = 0'Valeur associ�e � la sortie num�rique s�lectionn�e 0 ou 1

GLOBALDELAY = 10^9/(1000*25)


EVENT :

' Programmation SE/DIFF module entr�e analogique
IF (par_16 = 1) THEN
  SE_DIFF(par_15,par_12)
  par_16 = 0
ENDIF

' Programmation module entr�e sortie numerique
IF (par_9 = 1) THEN
  DIGPROG1(par_15,par_10)
  DIGPROG2(par_15,par_11)
  par_13 = GET_DIGOUT_LONG(par_15) 'Lecture etat initial des ESnum
  par_9 = 0
ENDIF

'Acquisition entr�e analogique
IF (par_1 > 0) then      
  bits_AnIn_Temp = LOG(par_14)/LOG(2)+0.1    
  bits_AnIn_Temp2 = bits_AnIn_Temp
  bits_AnIn = shift_left(bits_AnIn_Temp2,5)
  bits_AnIn = bits_AnIn + par_1 - 1
  set_mux(par_15,bits_AnIn)
  sleep(60)
  start_conv(par_15)
  wait_eoc(par_15)
  par_2 =readadc(par_15)
  par_1 = 0
ENDIF

' Acquisition entr�e num�rique
IF (par_3 > 0) then
  par_4 = DIGIN_LONG(par_15) and par_3 ' par_3 : masque binaire
  par_3 = 0
ENDIF

' Sortie d'un valeur analogique
IF (par_5 > 0) then
  DAC(par_15,par_5,par_6)
  par_5=0
ENDIF

' Sortie d'un valeur num�rique
IF (par_7 > -1) then
  DIGOUT(par_15,par_7,par_8)
  par_7 = -1
ENDIF

IF (par_17 > 0) THEN
  par_17 = ((par_17-1) OR 080h) OR (SHIFT_LEFT(par_15,8))
  POKE(ADRD,par_17)
  NOP_T9
  par_18 = peek(DL)
  par_17 = 0
ENDIF
