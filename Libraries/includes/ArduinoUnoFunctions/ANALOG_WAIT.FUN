MENU_TITLE = Analog Wait
DESCRIPTION = Wait for an analog input to go through a given value
MENU_TITLE_FR = Attente analogique
MENU_TITLE_PL = Oczekiwanie analogowe
DESCRIPTION_FR = Attendre qu'une entrée analogique passe une valeur donnée
DESCRIPTION_PL = Czekaj, aż wejście analogowe przekroczy zadaną wartość
# Several segments are available in ArduinoUno code :
# INCLUDE, DEFINE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, LOOP, TRANSFER, GENERATION, FUNCTION, FINALIZE
# The Arduino function code you need to add in segment INITIALIZATION, LOOP or FINALIZE may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type and CPU type separated by "_" char, when
# Arduino code depends on sytem and cpu types. 
# Finally the property key FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contains the effective ArduinoUno function code which will be added to
# any of segments INITIALIZATION, LOOP or FINALIZE depending on the actual function position.
# Take care of the fact that each key must be upper case !

DECLARATION = unsigned long aWaitTime_HashCode; 

FUNCTION_CODE = aWaitTime_HashCode = 0;\n\
do {\n\
	if(millis() - aWaitTime_HashCode > 1000) {\n\
		sprintf(serialMessage, "%d:%lu:%d", 0, 0, 100);\n\
		globalSerialMessage = serialMessage;\n\
		globalSerialMessage += "\\n";\n\
		globalSerialMessage.send();\n\
		aWaitTime_HashCode = millis();\n\
	}\n\
} while(analogRead(channelNumber) comparator channelValue);

# Portenta Machine Control : FUNCTION_CODE_PMC (and optionally DECLARATION_PMC...), when defined, replaces FUNCTION_CODE :
# its inputs are read with the Arduino_PortentaMachineControl library.
FUNCTION_CODE_PMC = aWaitTime_HashCode = 0;\n\
do {\n\
	if(millis() - aWaitTime_HashCode > 1000) {\n\
		sprintf(serialMessage, "%d:%lu:%d", 0, 0, 100);\n\
		globalSerialMessage = serialMessage;\n\
		globalSerialMessage += "\\n";\n\
		globalSerialMessage.send();\n\
		aWaitTime_HashCode = millis();\n\
	}\n\
} while(MachineControl_AnalogIn.read(channelNumber) comparator channelValue);
