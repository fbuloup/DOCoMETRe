MENU_TITLE = Stimulus
DESCRIPTION = Signal generation function
MENU_TITLE_FR = Stimulus
DESCRIPTION_FR = Fonction de génération d'un signal
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !

FUNCTION_CODE = REM STIMULUS - This file : "absolutePathToFile"\n\
REM will be used as stimulus for channel "output"\n\n\
IF (TRANSFERT_output = frequencyRatio) THEN\n\
	TRANSFERT_output = 0\n\
	IF(FIFO_FULL(transferNumber) = 0) THEN\n\
		PAR_transferNumber = PAR_transferNumber + 1\n\
		FIFO_CLEAR(transferNumber)\n\
	ELSE\n\
		output = output_TAB\n\
	ENDIF\n\
ENDIF\n\
INC(TRANSFERT_output)