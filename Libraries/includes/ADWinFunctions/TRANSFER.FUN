MENU_TITLE = Transfer
DESCRIPTION = Transfer Signal from system to PC
MENU_TITLE_FR = Transfert
DESCRIPTION_FR = Transfert du signal du système vers le PC
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !

FUNCTION_CODE = IF (TRANSFERT_channelName = frequencyRatio) THEN\n\
	TRANSFERT_channelName = 0\n\
	IF(FIFO_EMPTY(transferNumber) = 0) THEN\n\
		PAR_transferNumber = PAR_transferNumber + 1\n\
		FIFO_CLEAR(transferNumber)\n\
	ELSE\n\
		channelName_TAB = channelName\n\
	ENDIF\n\
ENDIF\n\
INC(TRANSFERT_channelName)