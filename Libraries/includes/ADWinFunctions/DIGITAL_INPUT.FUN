MENU_TITLE = Acquire digital input value
DESCRIPTION =  Acquire digital input value asynchronously (immediate)
MENU_TITLE_FR = Acquérir une entrée digitale
DESCRIPTION_FR = Acquérir une entrée digitale de manière asynchrone (immédiat)
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !
FUNCTION_CODE_PRO_I = channelName = Call_DigIn(moduleNumber, channelNumber) 'Channel name is channel value
FUNCTION_CODE_PRO_II = channelName = Call_DigIn(moduleNumber, channelNumber) 'Channel name is channel value
FUNCTION_CODE_GOLD_I = channelName = Call_DigIn(channelNumber) 'Channel name is channel value
FUNCTION_CODE_GOLD_II = channelName = Call_DigIn(channelNumber) 'Channel name is channel value