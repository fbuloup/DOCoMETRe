MENU_TITLE = Write value to Servomotor Register
DESCRIPTION = Write value to specific Servomotor Register (e.g. Torque, Acceleration, Velocity or Position register)
MENU_TITLE_FR = Écrire dans un registre du servomoteur
DESCRIPTION_FR = Écrire une valeur dans un registre spécifique du servomoteur (e.g. registre Couple, Accélération, Vitesse ou Postion)
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
	DIM regValue_HashCode AS LONG\n\
	DIM nbResponses_HashCode AS LONG

FUNCTION_CODE_PRO_I = regValue_HashCode = registerValue\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, registerType)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(registerType))\n\
 IF((registerType = 7) OR ((registerType = 6) OR (registerType = 5))) THEN\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 002H)\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FDH)\n\
  ENDIF\n\
  IF(registerType = 3) THEN\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 004H)\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FBH)\n\
  ENDIF\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, regValue_HashCode)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(regValue_HashCode))\n\
  regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, regValue_HashCode)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(regValue_HashCode))\n\
  IF(registerType = 3) THEN\n\
  	regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, regValue_HashCode)\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(regValue_HashCode))\n\
  	regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
    dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, regValue_HashCode)\n\
    dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(regValue_HashCode))\n\
  ENDIF\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  nbResponses_HashCode = 0\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(moduleNumber, portNumber)\n\
  	if(dummyResponse_HashCode = 11H) THEN INC(nbResponses_HashCode)\n\
  UNTIL(nbResponses_HashCode = 3)
  
FUNCTION_CODE_PRO_II = regValue_HashCode = registerValue\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, registerType)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(registerType))\n\
  IF((registerType = 7) OR ((registerType = 6) OR (registerType = 5))) THEN\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 002H)\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FDH)\n\
  ENDIF\n\
  IF(registerType = 3) THEN\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 004H)\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0FBH)\n\
  ENDIF\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, regValue_HashCode)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(regValue_HashCode))\n\
  regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, regValue_HashCode)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(regValue_HashCode))\n\
  IF(registerType = 3) THEN\n\
  	regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, regValue_HashCode)\n\
  	dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(regValue_HashCode))\n\
  	regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
    dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, regValue_HashCode)\n\
    dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, NOT(regValue_HashCode))\n\
  ENDIF\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 0AAH)\n\
  nbResponses_HashCode = 0\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(moduleNumber, portNumber)\n\
  	if(dummyResponse_HashCode = 11H) THEN INC(nbResponses_HashCode)\n\
  UNTIL(nbResponses_HashCode = 3)
  
FUNCTION_CODE_GOLD_I = regValue_HashCode = registerValue\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, registerType)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(registerType))\n\
 IF((registerType = 7) OR ((registerType = 6) OR (registerType = 5))) THEN\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, 002H)\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FDH)\n\
  ENDIF\n\
  IF(registerType = 3) THEN\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, 004H)\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FBH)\n\
  ENDIF\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, regValue_HashCode)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(regValue_HashCode))\n\
  regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, regValue_HashCode)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(regValue_HashCode))\n\
  IF(registerType = 3) THEN\n\
  	regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, regValue_HashCode)\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(regValue_HashCode))\n\
  	regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
    dummyResponse_HashCode = WRITE_FIFO(portNumber, regValue_HashCode)\n\
    dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(regValue_HashCode))\n\
  ENDIF\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  nbResponses_HashCode = 0\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(moduleNumber, portNumber)\n\
  	if(dummyResponse_HashCode = 11H) THEN INC(nbResponses_HashCode)\n\
  UNTIL(nbResponses_HashCode = 3)
  
FUNCTION_CODE_GOLD_II = regValue_HashCode = registerValue\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 052H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FFH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 000H)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, registerType)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(registerType))\n\
 IF((registerType = 7) OR ((registerType = 6) OR (registerType = 5))) THEN\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, 002H)\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FDH)\n\
  ENDIF\n\
  IF(registerType = 3) THEN\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, 004H)\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, 0FBH)\n\
  ENDIF\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, regValue_HashCode)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(regValue_HashCode))\n\
  regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, regValue_HashCode)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(regValue_HashCode))\n\
  IF(registerType = 3) THEN\n\
  	regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, regValue_HashCode)\n\
  	dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(regValue_HashCode))\n\
  	regValue_HashCode = shift_right(regValue_HashCode, 8)\n\
    dummyResponse_HashCode = WRITE_FIFO(portNumber, regValue_HashCode)\n\
    dummyResponse_HashCode = WRITE_FIFO(portNumber, NOT(regValue_HashCode))\n\
  ENDIF\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  dummyResponse_HashCode = WRITE_FIFO(portNumber, 0AAH)\n\
  nbResponses_HashCode = 0\n\
  DO\n\
  	dummyResponse_HashCode = READ_FIFO(moduleNumber, portNumber)\n\
  	if(dummyResponse_HashCode = 11H) THEN INC(nbResponses_HashCode)\n\
  UNTIL(nbResponses_HashCode = 3)