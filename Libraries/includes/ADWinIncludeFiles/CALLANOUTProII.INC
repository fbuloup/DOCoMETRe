'<ADbasic Header, Headerversion 001.001>
'<Header End>
SUB Call_AnOut(moduleNumber, channelNumber, channelValue, ampMin, ampMax)

	DIM channelValue_long AS LONG

	channelValue_long = (channelValue - ampMin)/(ampMax - ampMin)*65535
	P2_DAC(moduleNumber, channelNumber, channelValue_long)

ENDSUB
