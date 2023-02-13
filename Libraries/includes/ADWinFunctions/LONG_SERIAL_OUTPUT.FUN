MENU_TITLE = Write long value to serial port
DESCRIPTION = Convert long value to ASCII string and write result to selected serial port
MENU_TITLE_FR = Écrire un entier long sur un port série
DESCRIPTION_FR = Convertir un entier long en chaine de caractères ASCII et l'écrire sur le port série sélectionné
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !

DECLARATION = DIM stringValue_HashCode[13] AS STRING\n\
DIM currentChar_HashCode[1] AS STRING\n\
DIM currentCharNumber_HashCode AS LONG\n\
DIM dummyResponse_HashCode AS LONG\n\
DIM addLF_HashCode AS LONG

INITIALIZATION = addLF_HashCode = add_LF

FUNCTION_CODE_PRO_I = LngToStr(longValue, stringValue_HashCode)\n\
FOR currentCharNumber_HashCode = 1 TO STRLEN(stringValue_HashCode)\n\
STRMID(stringValue_HashCode, currentCharNumber_HashCode, 1, currentChar_HashCode)\n\
dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, ASC(currentChar_HashCode))\n\
NEXT currentCharNumber_HashCode\n\
IF (addLF_HashCode = 1) THEN\n\
dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 10)\n\
ENDIF

FUNCTION_CODE_PRO_II = LngToStr(longValue, stringValue_HashCode)\n\
FOR currentCharNumber_HashCode = 1 TO STRLEN(stringValue_HashCode)\n\
STRMID(stringValue_HashCode, currentCharNumber_HashCode, 1, currentChar_HashCode)\n\
dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, ASC(currentChar_HashCode))\n\
NEXT currentCharNumber_HashCode\n\
IF (addLF_HashCode = 1) THEN\n\
dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 10)\n\
ENDIF

FUNCTION_CODE_GOLD_I = LngToStr(longValue, stringValue_HashCode)\n\
FOR currentCharNumber_HashCode = 1 TO STRLEN(stringValue_HashCode)\n\
STRMID(stringValue_HashCode, currentCharNumber_HashCode, 1, currentChar_HashCode)\n\
dummyResponse_HashCode = WRITE_FIFO(portNumber, ASC(currentChar_HashCode))\n\
NEXT currentCharNumber_HashCode\n\
IF (addLF_HashCode = 1) THEN\n\
dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 10)\n\
ENDIF

FUNCTION_CODE_GOLD_II = LngToStr(longValue, stringValue_HashCode)\n\
FOR currentCharNumber_HashCode = 1 TO STRLEN(stringValue_HashCode)\n\
STRMID(stringValue_HashCode, currentCharNumber_HashCode, 1, currentChar_HashCode)\n\
dummyResponse_HashCode = WRITE_FIFO(portNumber, ASC(currentChar_HashCode))\n\
NEXT currentCharNumber_HashCode\n\
IF (addLF_HashCode = 1) THEN\n\
dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, 10)\n\
ENDIF