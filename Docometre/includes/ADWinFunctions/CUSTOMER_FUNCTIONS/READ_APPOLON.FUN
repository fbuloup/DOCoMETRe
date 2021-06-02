MENU_TITLE = Read Appolon Parameters
DESCRIPTION = Read Appolon Parameters (angular velocity, rise time and stage time)
MENU_TITLE_FR = Lecture des parametres Appolon
DESCRIPTION_FR = Lecture des parametres Appolon (vitesse angulaire, temps de montée et temps d'établissement)
# It'a user function if USER_FUNCTION key exists 

# and its value is 1 or YES or yes or YeS...

USER_FUNCTION = YES 

PARAMETERS_NUMBER = 7
PARAMETER_1 = NAME = angularVelocity, TYPE = TEXT:^(?!\s*$).+, LABEL = "Initial Angular Velocity :",\
LABEL_FR = "Initial Angular Velocity :"

PARAMETER_2 = NAME = riseTime, TYPE = TEXT:^(?!\s*$).+, LABEL = "Rise time :",\
LABEL_FR = "Rise time :"

PARAMETER_3 = NAME = stageTime, TYPE = TEXT:^(?!\s*$).+, LABEL = "Stage time :",\
LABEL_FR = "Stage time :"

PARAMETER_4 = NAME = portNumber, TYPE = TEXT:^[1-2]$, LABEL = "CAN port number  :",\
LABEL_FR = "Numéro du port CAN :"

PARAMETER_5 = NAME = messageObjectNumber, TYPE = TEXT:^([1-9]|1[0-5])$, LABEL = "Message object number:",\
LABEL_FR = "Numéro du message objet :"

PARAMETER_6 = NAME = messageID, TYPE = TEXT:^[-]?[0-9]+$, LABEL = "Message identifier :",\
LABEL_FR = "Identifiant du message :"


PARAMETER_7 = NAME = state, TYPE = TEXT:^(?!\s*$).+, LABEL = "State :",\
LABEL_FR = "Etat :"


# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !
DECLARATION = DIM CAN_VALUE_HashCode AS INTEGER

FUNCTION_CODE = IF(READ_MSG(portNumber, messageObjectNumber) = messageID) THEN\n\
' CAN_MSG[9] : nb bytes received\n\
' CAN_MSG[1] : Appolon code\n\
' CAN_MSG[2] : Value type\n\
' CAN_MSG[3 to 6] : Value code\n\
IF((CAN_MSG[1] = 4) AND (CAN_MSG[2] <> 4)) THEN  ' If appolon code is START with parameter\n\
CAN_VALUE_HashCode = SHIFT_LEFT(CAN_MSG[6], 24) + SHIFT_LEFT(CAN_MSG[5], 16) + SHIFT_LEFT(CAN_MSG[4], 8) + CAN_MSG[3] ' Create INTEGER representation\n\
' Convert to FLOAT\n\
IF(CAN_MSG[2] = 0) THEN\n\
angularVelocity = Cast_LongToFloat(CAN_VALUE_HashCode)' If its angular velocity\n\
ENDIF\n\
IF(CAN_MSG[2] = 1) THEN\n\
riseTime  = Cast_LongToFloat(CAN_VALUE_HashCode)' If its rise time\n\
ENDIF\n\
IF(CAN_MSG[2] = 2) THEN\n\
stageTime = Cast_LongToFloat(CAN_VALUE_HashCode)' If its stage time\n\
ENDIF\n\
ENDIF\n\
IF((CAN_MSG[1] = 4) AND (CAN_MSG[2] = 4)) THEN  ' If appolon code is START/START\n\
state = 1\n\
ENDIF\n\
ENDIF
