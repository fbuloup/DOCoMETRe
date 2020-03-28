'<ADbasic Header, Headerversion 001.001>
'<Header End>
SUB Call_DigOut(channelNumber, channelValue)

	DIGOUT(channelNumber - 1, channelValue)
	
ENDSUB
