'<ADbasic Header, Headerversion 001.001>
'<Header End>
FUNCTION Call_DigIn(channelNumber) AS LONG

  DIM channelValue AS LONG

  channelValue = DIGIN_LONG()	
  channelValue = shift_right(channelValue, channelNumber - 1)
  Call_DigIn = channelValue and 1

ENDFUNCTION
