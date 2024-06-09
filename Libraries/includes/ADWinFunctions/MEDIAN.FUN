MENU_TITLE = Median filter
DESCRIPTION = Apply median filter to selected input
MENU_TITLE_FR = Filtre médian
DESCRIPTION_FR = Applique un filtre médian sur l'entrée sélectionnée
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !
DECLARATION = DIM medianFilterValues_HashCode[historySize] AS FLOAT\n\
DIM orderedMedianFilterValues_HashCode[historySize] AS FLOAT\n\
DIM i_HashCode AS INTEGER\n\
DIM firstTime_HashCode AS INTEGER\n\
DIM history_HashCode AS INTEGER\n\
DIM compute_HashCode AS INTEGER

INITIALIZATION = firstTime_HashCode = 1\n\
history_HashCode = 1\n\
compute_HashCode = sampleFrequencyRatio

FUNCTION_CODE = IF (compute_HashCode = sampleFrequencyRatio) THEN\n\
compute_HashCode = 0\n\
IF (firstTime_HashCode = 1) THEN\n\
FOR i_HashCode = 1 TO historySize\n\
medianFilterValues_HashCode[i_HashCode] = inputChannelName\n\
orderedMedianFilterValues_HashCode[i_HashCode] = inputChannelName\n\
NEXT i_HashCode\n\
firstTime_HashCode = 0\n\
ENDIF \n\
FOR i_HashCode = 0 TO historySize - 2\n\
medianFilterValues_HashCode[historySize - i_HashCode] = medianFilterValues_HashCode[historySize - i_HashCode - 1]\n\
orderedMedianFilterValues_HashCode[historySize - i_HashCode] = medianFilterValues_HashCode[historySize - i_HashCode]\n\
NEXT i_HashCode\n\
medianFilterValues_HashCode[1] = inputChannelName\n\
orderedMedianFilterValues_HashCode[1] = inputChannelName\n\
outputChannelName = median(orderedMedianFilterValues_HashCode, history_HashCode)\n\
IF (history_HashCode < historySize) THEN\n\
history_HashCode = history_HashCode + 1\n\
ENDIF\n\
ENDIF\n\
INC(compute_HashCode)