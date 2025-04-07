****************************************
*	 Proze�nummer = 1
*	 Delay = 1000
*	 Eventsource = 0
*	 Number of Loops = 0
*	 Priorit�t = 0
*	 Version = 1
*	 FastStop = 0
*	 AdbasicVersion = 4000001
*	 ATSRAM = 0
*	 OPT_LEVEL = 1
*	 SAVECOMPIL = 0
****************************************
FUNCTION Call_DigIn(channelNumber) AS LONG

	DIM channelValue AS LONG

	channelValue = DIGIN_WORD()	
	channelValue = shift_right(channelValue, channelNumber - 1)
	Call_DigIn = channelValue and 1

ENDFUNCTION