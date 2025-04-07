MENU_TITLE = Read value from Servomotor Register
DESCRIPTION = Read value from specific Servomotor Register (e.g. Torque, Acceleration, Velocity or Position register)
MENU_TITLE_FR = Lire un registre du servomoteur
DESCRIPTION_FR = Lire la valeur d'un registre spécifique du servomoteur (e.g. registre Couple, Accélération, Vitesse ou Postion)
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !

DECLARATION = DIM dummyResponse_HashCode AS LONG\n\
	DIM frame_HashCode[19] AS LONG\n\
	DIM frameIndex_HashCode AS LONG

FUNCTION_CODE_PRO_I = dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, registerType)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(registerType))\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  frameIndex_HashCode = 1\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(moduleNumber, portNumber)\n\
  	IF (dummyResponse_HashCode <> -1) THEN\n\
  		frame_HashCode[frameIndex_HashCode] = dummyResponse_HashCode\n\
  		INC(frameIndex_HashCode)\n\
  		IF (frameIndex_HashCode = 20) THEN\n\
  			channelName = frame_HashCode[10]\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[12], 8)\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[14], 16)\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[16], 24)\n\
  		ENDIF\n\
  	ENDIF\n\
  UNTIL(frameIndex_HashCode = 20)
  
FUNCTION_CODE_PRO_II = dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, registerType)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(registerType))\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  frameIndex_HashCode = 1\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(moduleNumber, portNumber)\n\
  	IF (dummyResponse_HashCode <> -1) THEN\n\
  		frame_HashCode[frameIndex_HashCode] = dummyResponse_HashCode\n\
  		INC(frameIndex_HashCode)\n\
  		IF (frameIndex_HashCode = 20) THEN\n\
  			channelName = frame_HashCode[10]\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[12], 8)\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[14], 16)\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[16], 24)\n\
  		ENDIF\n\
  	ENDIF\n\
  UNTIL(frameIndex_HashCode = 20)

FUNCTION_CODE_GOLD_I = dummyResponse_HashCode = WRITE_FIFO(portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(registerType)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(registerType))\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  frameIndex_HashCode = 1\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(portNumber)\n\
  	IF (dummyResponse_HashCode <> -1) THEN\n\
  		frame_HashCode[frameIndex_HashCode] = dummyResponse_HashCode\n\
  		INC(frameIndex_HashCode)\n\
  		IF (frameIndex_HashCode = 20) THEN\n\
  			channelName = frame_HashCode[10]\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[12], 8)\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[14], 16)\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[16], 24)\n\
  		ENDIF\n\
  	ENDIF\n\
  UNTIL(frameIndex_HashCode = 20)
  
FUNCTION_CODE_GOLD_II = dummyResponse_HashCode = WRITE_FIFO(portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 050H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(registerType)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(registerType))\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  frameIndex_HashCode = 1\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(portNumber)\n\
  	IF (dummyResponse_HashCode <> -1) THEN\n\
  		frame_HashCode[frameIndex_HashCode] = dummyResponse_HashCode\n\
  		INC(frameIndex_HashCode)\n\
  		IF (frameIndex_HashCode = 20) THEN\n\
  			channelName = frame_HashCode[10]\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[12], 8)\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[14], 16)\n\
  			channelName = channelName + SHIFT_LEFT(frame_HashCode[16], 24)\n\
  		ENDIF\n\
  	ENDIF\n\
  UNTIL(frameIndex_HashCode = 20)