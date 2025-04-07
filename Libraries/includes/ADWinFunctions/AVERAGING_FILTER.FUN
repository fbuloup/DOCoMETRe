MENU_TITLE = Averaging filter
DESCRIPTION = First order recursive averaging filter
MENU_TITLE_FR = Filtre moyenneur
DESCRIPTION_FR = Filtre moyenneur récursif d'ordre un
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !

DECLARATION = DIM previousOutputValue_HashCode AS FLOAT\n\
DIM compute_HashCode AS INTEGER\n\
DIM firstTime_HashCode AS INTEGER

INITIALIZATION = compute_HashCode = sampleFrequencyRatio\n\
firstTime_HashCode = 1

FUNCTION_CODE = IF (compute_HashCode = sampleFrequencyRatio) THEN\n\
IF (firstTime_HashCode = 1) THEN\n\
previousOutputValue_HashCode = inputChannelName\n\
firstTime_HashCode = 0\n\
ENDIF\n\
compute_HashCode = 0\n\
outputChannelName = aValue * previousOutputValue_HashCode + bValue * inputChannelName\n\
previousOutputValue_HashCode = outputChannelName\n\
ENDIF\n\
INC(compute_HashCode)