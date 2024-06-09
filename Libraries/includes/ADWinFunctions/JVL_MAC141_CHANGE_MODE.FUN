MENU_TITLE = Change Servomotor Mode
DESCRIPTION = Change Servomotor mode to Passive, Velocity, Position etc.
MENU_TITLE_FR = Changer le Mode du Servomoteur
DESCRIPTION_FR = Changer le mode du Servomoteur vers Passif, Vitesse, Position etc.
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
	DIM nbResponses_HashCode AS LONG

FUNCTION_CODE_PRO_I = dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 002H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FDH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 002H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FDH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, mode)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(mode))\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  nbResponses_HashCode = 0\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(moduleNumber, portNumber)\n\
  	if(dummyResponse_HashCode = 11H) THEN INC(nbResponses_HashCode)\n\
  UNTIL(nbResponses_HashCode = 3)
  
FUNCTION_CODE_PRO_II = dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 002H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FDH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 002H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FDH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, mode)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(mode))\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  nbResponses_HashCode = 0\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(moduleNumber, portNumber)\n\
  	if(dummyResponse_HashCode = 11H) THEN INC(nbResponses_HashCode)\n\
  UNTIL(nbResponses_HashCode = 3)
  
FUNCTION_CODE_GOLD_I = dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 002H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FDH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 002H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FDH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, mode)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(mode))\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  nbResponses_HashCode = 0\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(portNumber)\n\
  	if(dummyResponse_HashCode = 11H) THEN INC(nbResponses_HashCode)\n\
  UNTIL(nbResponses_HashCode = 3)
  
FUNCTION_CODE_GOLD_II = dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 002H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FDH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 002H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FDH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, mode)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(mode))\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  nbResponses_HashCode = 0\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(portNumber)\n\
  	if(dummyResponse_HashCode = 11H) THEN INC(nbResponses_HashCode)\n\
  UNTIL(nbResponses_HashCode = 3)