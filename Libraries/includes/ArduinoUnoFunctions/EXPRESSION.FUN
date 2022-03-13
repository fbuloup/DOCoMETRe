MENU_TITLE = Compute expression(s)
DESCRIPTION = Compute given expression(d)
MENU_TITLE_FR = Calculer une ou des expressions
DESCRIPTION_FR = Calcule la ou les expressions fournies
# Several segments are available in Arduino code :
# INCLUDE, DEFINE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, LOOP, TRANSFER, GENERATION, FUNCTION, FINALIZE
# The Arduino function code you need to add in any segments INITIALIZATION, LOOP or FINALIZE may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type and CPU type separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ArduinoUno function code which will be added to
# any of segments INITIALIZATION, LOOP or FINALIZE depending on the actual function position.
# Take care of the fact that each key must be upper case !
FUNCTION_CODE = expression