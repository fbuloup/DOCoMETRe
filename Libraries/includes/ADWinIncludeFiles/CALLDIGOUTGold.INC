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
SUB Call_DigOut(channelNumber, channelValue)

	if (channelValue = 1) then SET_DIGOUT(channelNumber -17)
	if (channelValue = 0) then CLEAR_DIGOUT(channelNumber -17)
	
ENDSUB