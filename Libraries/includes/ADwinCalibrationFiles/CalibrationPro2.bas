'<ADbasic Header, Headerversion 001.001>
' Process_Number                 = 10
' Initial_Processdelay           = 400000
' Eventsource                    = Timer
' Control_long_Delays_for_Stop   = No
' Priority                       = High
' Version                        = 1
' ADbasic_Version                = 6.4.0
' Optimize                       = Yes
' Optimize_Level                 = 1
' Stacksize                      = 1000
' Info_Last_Save                 = PREC3650BULOUP  PREC3650BULOUP\buloup
'<Header End>
#include adwinpro1.inc
#include adwinpro2.inc
'#include adwpad.inc
'#include adwpdio.inc
'#include adwpda.inc

'Pour l'ampli multiplex� des entr�es analogiques
DIM bits_AnIn,bits_AnIn_Temp2 AS LONG
DIM bits_AnIn_Temp AS FLOAT 

INIT :
' Module number : sent by DOCoMETRe (Par_15) before each access to an input or an output
par_15=1

'Entr�es analogiques
par_1=0'Num�ro de l'entr�e analogique 1 .. 32
par_2=0'Valeur associ�e � l'entr�e analogique s�lectionn�e

'Entr�es num�riques
par_3=0'Num�ro de l'entr�e num�rique
par_4=0'Valeur associ�e � l'entr�e num�rique s�lectionn�e

'Sorties analogiques
par_5=0'Num�ro de la sortie analogique 1 .. 4
par_6=0'Valeur associ�e � la sortie analogique s�lectionn�e 0 .. 65535

'Sortie num�riques
par_7=-1'Num�ro de la sortie num�rique 0 .. 31
par_8=0'Valeur associ�e � la sortie num�rique s�lectionn�e 0 ou 1

'Prog du DIO tout en entr�e par d�faut � la mise sous tension
'par_9=0
'par_10=0
'par_11=0
'par_12=0 'SE ou DIFF
par_14=1 'gain des entr�es analogiques 1,2,4 ou 8
GLOBALDELAY = 3*10^8/1000 ' T11 timer at 300 MHz (10/3 ns) : 10^9/(1000*3.3) gave a period 1 % too long

'par_12  : 0, mode SE ou 1, mode DIFF
SE_DIFF(1,par_12)

IF (par_9=1) THEN
  P2_DIGPROG(1,par_10)
ENDIF

par_13=P2_GET_DIGOUT_LONG(1) 'Lecture etat initial des ESnum

EVENT :
'Acquisition entr�e analogique
IF (par_1>0) then      
  ' Gain code of the PGA (0, 1, 2, 3 for a gain of 1, 2, 4, 8) : + 0.1 so that a result
  ' of LOG(par_14)/LOG(2) slightly below the integer value is not truncated
  bits_AnIn_Temp = LOG(par_14)/LOG(2) + 0.1
  bits_AnIn_Temp2 = bits_AnIn_Temp
  bits_AnIn = shift_left(bits_AnIn_Temp2,5) + par_1 - 1

  ' Module par_15, Pro II functions : as the acquisition (see CALLANINProII.INC)
  P2_Set_Mux(par_15,bits_AnIn)
  P2_Wait_Mux(par_15)
  P2_Start_Conv(par_15)
  P2_Wait_EOC(par_15)
  
  par_2 = P2_Read_ADC(par_15)
  par_1 = 0
ENDIF

'Acquisition entr�e num�rique
IF (par_3>0) then
  par_4 = P2_DIGIN_LONG(par_15) and par_3
  par_3=0
ENDIF

'sortie d'un valeur analogique
IF (par_5>0) then
  P2_DAC(par_15,par_5,par_6)
  par_5=0
ENDIF

'Sortie d'un valeur num�rique
IF (par_7>-1) then
  P2_DIGOUT(par_15,par_7,par_8)
  par_7=-1
ENDIF
