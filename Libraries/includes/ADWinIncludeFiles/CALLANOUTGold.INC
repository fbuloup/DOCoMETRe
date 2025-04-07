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
SUB Call_AnOut(channelNumber, channelValue, ampMin, ampMax)

	DIM channelValue_long AS LONG

	channelValue_long = (channelValue - ampMin)/(ampMax - ampMin)*65535
	dac(channelNumber, channelValue_long)

ENDSUB