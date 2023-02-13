MENU_TITLE = Write ASCII characters to serial port
DESCRIPTION = Write ASCII characters to selected serial port\n\
e.g. : channelName or string "stringValue" (do not omit the quotation marks)
MENU_TITLE_FR = Écrire des caractères ASCII sur un port série
DESCRIPTION_FR = Écrire des caractères ASCII sur le port série sélectionné\n\
e.g. : channelName ou chaîne "stringValue" (n'omettez pas les guillemets)
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !

DECLARATION = DIM stringValue_HashCode[stringSize] AS STRING\n\
DIM currentChar_HashCode[1] AS STRING\n\
DIM currentCharNumber_HashCode AS LONG\n\
DIM dummyResponse_HashCode AS LONG\n\
DIM addLFValueLabel AS LONG

INITIALIZATION = addLFValueLabel = add_LF

FUNCTION_CODE_PRO_I = stringValue_HashCode = asciiStringValue\n\
FOR currentCharNumber_HashCode = 1 TO STRLEN(stringValue_HashCode)\n\
STRMID(stringValue_HashCode, currentCharNumber_HashCode, 1, currentChar_HashCode)\n\
dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, ASC(currentChar_HashCode))\n\
NEXT currentCharNumber_HashCode\n\
IF (addLFValueLabel = 1) THEN\n\
dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 10)\n\
ENDIF

FUNCTION_CODE_PRO_II = stringValue_HashCode = asciiStringValue\n\
FOR currentCharNumber_HashCode = 1 TO STRLEN(stringValue_HashCode)\n\
STRMID(stringValue_HashCode, currentCharNumber_HashCode, 1, currentChar_HashCode)\n\
dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, ASC(currentChar_HashCode))\n\
NEXT currentCharNumber_HashCode\n\
IF (addLFValueLabel = 1) THEN\n\
dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 10)\n\
ENDIF

FUNCTION_CODE_GOLD_I = stringValue_HashCode = asciiStringValue\n\
FOR currentCharNumber_HashCode = 1 TO STRLEN(stringValue_HashCode)\n\
STRMID(stringValue_HashCode, currentCharNumber_HashCode, 1, currentChar_HashCode)\n\
dummyResponse_HashCode = WRITE_FIFO(portNumber, ASC(currentChar_HashCode))\n\
NEXT currentCharNumber_HashCode\n\
IF (addLFValueLabel = 1) THEN\n\
dummyResponse_HashCode = WRITE_FIFO(portNumber, 10)\n\
ENDIF

FUNCTION_CODE_GOLD_II = stringValue_HashCode = asciiStringValue\n\
FOR currentCharNumber_HashCode = 1 TO STRLEN(stringValue_HashCode)\n\
STRMID(stringValue_HashCode, currentCharNumber_HashCode, 1, currentChar_HashCode)\n\
dummyResponse_HashCode = WRITE_FIFO(portNumber, ASC(currentChar_HashCode))\n\
NEXT currentCharNumber_HashCode\n\
IF (addLFValueLabel = 1) THEN\n\
dummyResponse_HashCode = WRITE_FIFO(portNumber, 10)\n\
ENDIF