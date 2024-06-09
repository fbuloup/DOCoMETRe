MENU_TITLE = Acquire digital input value
DESCRIPTION =  Acquire digital input value asynchronously (immediate)
MENU_TITLE_FR = Acquérir une entrée digitale
DESCRIPTION_FR = Acquérir une entrée digitale de manière asynchrone (immédiat)
# Several segments are available in ArduinoUno code :
# INCLUDE, DEFINE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, LOOP, TRANSFER, GENERATION, FUNCTION, FINALIZE
# The Arduino function code you need to add in segment INITIALIZATION, LOOP or FINALIZE may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type and CPU type separated by "_" char, when
# Arduino code depends on sytem and cpu types. 
# Finally the property key FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contains the effective ArduinoUno function code which will be added to
# any of segments INITIALIZATION, LOOP or FINALIZE depending on the actual function position.
# Take care of the fact that each key must be upper case !
FUNCTION_CODE = channelName = digitalRead(channelNumber);