MENU_TITLE = Linear Mapping Value
DESCRIPTION = Map value x from [a, b] to y in [c, d], linearly
MENU_TITLE_FR = Transformation linéaire  de valeur
DESCRIPTION_FR = Transforme une valeur x de [a, b] vers y dans [c, d], linéairement
# It'a user function if USER_FUNCTION key exists 
# and its value is 1 or YES or yes or YeS...
USER_FUNCTION = YES 
PARAMETERS_NUMBER = 6
PARAMETER_1 = NAME = xValue, TYPE = TEXT:^(?!\s*$).+, LABEL = "X value :",\
LABEL_FR = "Valeur de x :"
PARAMETER_2 = NAME = xMinValue, TYPE = TEXT:^[-]?[0-9]+$, LABEL = "X Minimum value (a) :",\
LABEL_FR = "Valeur minimale de x (a) :"
PARAMETER_3 = NAME = xMaxValue, TYPE = TEXT:^[-]?[0-9]+$:, LABEL = "X Maximum value (b) :",\
LABEL_FR = "Valeur maximale de x (b) :"
PARAMETER_4 = NAME = yMinValue, TYPE = TEXT:^[-]?[0-9]+$:, LABEL = "Y Minimum value (c) :",\
LABEL_FR = "Valeur minimale de y (c) :"
PARAMETER_5 = NAME = yMaxValue, TYPE = TEXT:^[-]?[0-9]+$:, LABEL = "Y Maximum value (d) :",\
LABEL_FR = "Valeur maximale de y (d) :"
PARAMETER_6 = NAME = yValue, TYPE = TEXT:^(?!\s*$).+, LABEL = "Put generated value in (y) :", LABEL_FR = "Mettre la valeur générée dans (y) :" 
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !
FUNCTION_CODE = yValue = (yMaxValue - yMinValue)/(xMaxValue - xMinValue)*(xValue - xMinValue) + yMinValue