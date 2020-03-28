MENU_TITLE = Terminate process
DESCRIPTION = Terminate current process
MENU_TITLE_FR = Arrêter le processus
DESCRIPTION_FR = Arrête le processus courant
# Several segments are available in Arduino code :
# INCLUDE, DEFINE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, LOOP, TRANSFER, GENERATION, FUNCTION
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type and CPU type separated by "_" char, when
# Arduino code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective Arduino function code which will be added to
# any of segments INITIALIZATION, EVENT depending on the actual function position.
# Take care of the fact that each key must be upper case !
FUNCTION_CODE = if(leftOperandKey operatorKey rightOperandKey) {\n\
\t\tterminateProcess = true;\n\
}