MENU_TITLE = Filtered derivator 
DESCRIPTION = First order filtering and derivative
MENU_TITLE_FR = Dérivateur filtré
DESCRIPTION_FR = Filtrage d'ordre un et dérivée
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
DIM previousInputValue_HashCode AS FLOAT\n\
DIM b_HashCode AS FLOAT\n\
DIM compute_HashCode AS INTEGER\n\
DIM firstTime_HashCode AS INTEGER

INITIALIZATION = b_HashCode = exp(-aValue*fcValue/feValue)\n\
compute_HashCode = sampleFrequencyRatio\n\
firstTime_HashCode = 1\n\
previousOutputValue_HashCode = 0

FUNCTION_CODE = IF (compute_HashCode = sampleFrequencyRatio) THEN\n\
IF (firstTime_HashCode = 1) THEN\n\
previousInputValue_HashCode = inputChannelName\n\
firstTime_HashCode = 0\n\
ENDIF\n\
compute_HashCode = 0\n\
outputChannelName =  b_HashCode * previousOutputValue_HashCode + aValue * (inputChannelName - previousInputValue_HashCode)\n\
previousOutputValue_HashCode = outputChannelName\n\
previousInputValue_HashCode = inputChannelName\n\
ENDIF\n\
INC(compute_HashCode)