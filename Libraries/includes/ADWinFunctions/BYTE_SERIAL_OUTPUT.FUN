MENU_TITLE = Write bytes to serial port
DESCRIPTION = Write bytes (eight bits velues) to selected serial port\n\
e.g. : channelName or 63 or feH or 11B or 255,58H,101B
MENU_TITLE_FR = Écrire des octets sur un port série
DESCRIPTION_FR = Écrire des octets (8 bits) sur le port série sélectionné\n\
e.g. : channelName ou 63 ou feH ou 11B ou 255,58H,101B
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !

DECLARATION = DIM dummyResponse_HashCode AS LONG

FUNCTION_CODE_PRO_I = dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, byteValue)

FUNCTION_CODE_PRO_II = dummyResponse_HashCode = WRITE_FIFO(moduleNumber, portNumber, byteValue)

FUNCTION_CODE_GOLD_I = dummyResponse_HashCode = WRITE_FIFO(portNumber, byteValue)

FUNCTION_CODE_GOLD_II = dummyResponse_HashCode = WRITE_FIFO(portNumber, byteValue)