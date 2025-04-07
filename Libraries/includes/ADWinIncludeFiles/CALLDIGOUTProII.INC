'<ADbasic Header, Headerversion 001.001>
'<Header End>
SUB Call_DigOut(moduleNumber, channelNumber, channelValue)

	P2_DIGOUT(moduleNumber, channelNumber - 1, channelValue)
  
ENDSUB
