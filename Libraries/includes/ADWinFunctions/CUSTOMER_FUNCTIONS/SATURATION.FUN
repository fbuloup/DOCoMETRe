# This is a custom/user function example
# You can use it as inspiration to create
# your own functions
﻿MENU_TITLE = Saturation
DESCRIPTION = Saturation
MENU_TITLE_FR = Saturation
DESCRIPTION_FR = Saturation
# It'a user function if USER_FUNCTION key exists 
# and its value is 1 or YES or yes or YeS...
USER_FUNCTION = YES 
PARAMETERS_NUMBER = 4
PARAMETER_1 = NAME = input, TYPE = TEXT:^(?!\s*$).+, LABEL = "Input :",\
LABEL_FR = "Entrée :"
PARAMETER_2 = NAME = output, TYPE = TEXT:^(?!\s*$).+, LABEL = "Output :",\
LABEL_FR = "Output :"
PARAMETER_3 = NAME = maxSaturationValue, TYPE = TEXT:^(?!\s*$).+, LABEL = "Max. saturation value :",\
LABEL_FR = "Valeur max. de la saturation :"
PARAMETER_4 = NAME = minSaturationValue, TYPE = TEXT:^(?!\s*$).+, LABEL = "Min. saturation value :",\
LABEL_FR = "Valeur min. de la saturation :"
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !
FUNCTION_CODE = IF (input > maxSaturationValue) THEN\n\
  output = maxSaturationValue\n\
ENDIF\n\
IF (input < minSaturationValue) THEN\n\
  output = minSaturationValue\n\
ENDIF