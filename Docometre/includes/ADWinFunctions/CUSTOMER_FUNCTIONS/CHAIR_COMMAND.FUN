MENU_TITLE = Chair Command
DESCRIPTION = Trapezoidal velocity profile chair command
MENU_TITLE_FR = Commande de la chaise
DESCRIPTION_FR = Commande de la chaise en profil trapézoïdal de vitesse
# It'a user function if USER_FUNCTION key exists 

# and its value is 1 or YES or yes or YeS...

USER_FUNCTION = YES 

PARAMETERS_NUMBER = 10
PARAMETER_1 = NAME = initialAngularVelocity, TYPE = TEXT:^(?!\s*$).+, LABEL = "Initial Angular Velocity :",\
LABEL_FR = "Initial Angular Velocity :"
PARAMETER_2 = NAME = riseTime, TYPE = TEXT:^(?!\s*$).+, LABEL = "Rise time :",\
LABEL_FR = "Rise time :"
PARAMETER_3 = NAME = stageTime, TYPE = TEXT:^(?!\s*$).+, LABEL = "Stage time :",\
LABEL_FR = "Stage time :"

PARAMETER_4 = NAME = initialTime, TYPE = TEXT:^(?!\s*$).+, LABEL = "Initial time :",\
LABEL_FR = "Initial time :"
PARAMETER_5 = NAME = initialAngle, TYPE = TEXT:^(?!\s*$).+, LABEL = "Initial angle :",\
LABEL_FR = "Initial angle :"
PARAMETER_6 = NAME = state, TYPE = TEXT:^(?!\s*$).+, LABEL = "State :", LABEL_FR = "State :" 
PARAMETER_7 = NAME = angle, TYPE = TEXT:^(?!\s*$).+, LABEL = "Angle :", LABEL_FR = "Angle :" 
PARAMETER_8 = NAME = gain, TYPE = TEXT:^(?!\s*$).+, LABEL = "Gain :", LABEL_FR = "Gain :" 
PARAMETER_9 = NAME = chairSpeedCmd, TYPE = TEXT:^(?!\s*$).+, LABEL = "Chair speed command :", LABEL_FR = "Chair speed command :" 
PARAMETER_10 = NAME = returnGain, TYPE = TEXT:^(?!\s*$).+, LABEL = "Return Gain :", LABEL_FR = "Gain de retour :" 
# Several segments are available in ADBascic code :
# HEADER, INCLUDE, DECLARATION, INITIALIZATION, ACQUISITION, RECOVERY, TRANSFER, EVENT, GENERATION and FINISH
# The ADBasic function code you need to add in any segments INITIALIZATION, EVENT or FINISH may require contributions to any other segment.
# If you need to contribute to any of these segments, you must create a property key which prefix is the name of that segment
# and an optional suffix which is the concatenation of system type (PRO or GOLD) and CPU type (I or II) separated by "_" char, when
# ADBasic code depends on sytem and cpu types. 
# Finally the property keys FUNCTION_CODE (and optionally FUNCTION_CODE_SYSTEMTYPE_CPUTYPE) contain the effective ADBasic function code which will be added to
# any of segments INITIALIZATION, EVENT or FINISH depending on the actual function position.
# Take care of the fact that each key must be upper case !
DECLARATION = DIM cmdAngle_HashCode AS FLOAT\n\
DIM returnToZeroFlag_HashCode AS INTEGER\n\
DIM returnToZeroTime_HashCode AS FLOAT

INITIALIZATION = initialAngularVelocity = 0\n\
riseTime = 0\n\
stageTime = 0\n\
returnToZeroFlag_HashCode = 0

FUNCTION_CODE = IF (state = 1) THEN\n\
  REM Phase accélération (rampe vitesse > 0)\n\
  cmdAngle_HashCode = initialAngularVelocity/(2*riseTime)*(time - initialTime)*(time - initialTime) + initialAngle\n\
ENDIF\n\
IF (state = 2) THEN\n\
  REM Phase vitesse cste\n\
  cmdAngle_HashCode = initialAngularVelocity*(time - initialTime) - initialAngularVelocity/2*riseTime + initialAngle\n\
ENDIF\n\
IF (state = 3) THEN\n\
  REM Phase décélération (rampe vitesse < 0)\n\
  cmdAngle_HashCode = -initialAngularVelocity/(2*riseTime)*(time - initialTime)*(time - initialTime)\n\
  cmdAngle_HashCode = cmdAngle_HashCode + initialAngularVelocity/riseTime*(riseTime + stageTime)*(time - initialTime)\n\
  cmdAngle_HashCode = cmdAngle_HashCode - initialAngularVelocity/(2*riseTime)*(stageTime*stageTime + riseTime*riseTime) + initialAngle\n\
ENDIF\n\
IF (state = 4) THEN\n\
  REM Retour position initiale\n\
  cmdAngle_HashCode = -0.005*sign(angle)\n\
  IF (returnToZeroFlag_HashCode = 1) THEN\n\
    IF (ABSF(angle) < 0.01) THEN\n\
      returnToZeroTime_HashCode = time\n\
      returnToZeroFlag_HashCode = 2\n\
    ENDIF\n\
  ENDIF\n\
  IF (returnToZeroFlag_HashCode = 2) THEN\n\
    IF (ABSF(angle) < 0.025) THEN\n\
      IF ((time - returnToZeroTime_HashCode) > 0.5) THEN\n\
        returnToZeroFlag_HashCode = 0\n\
        state = 5\n\
        chairSpeedCmd = 0\n\
      ENDIF\n\
    ELSE\n\
      returnToZeroFlag_HashCode = 1\n\
    ENDIF\n\
  ENDIF\n\
ENDIF\n\
IF ((state = 1) OR ((state = 2) OR ((state = 3) OR (state = 4)))) THEN\n\
  chairSpeedCmd = gain*(cmdAngle_HashCode - angle)\n\
  IF (state = 4) THEN\n\
    chairSpeedCmd = returnGain*(cmdAngle_HashCode - angle)\n\
  ENDIF\n\
  IF ((time > riseTime + initialTime) AND (state = 1)) THEN\n\
    state = 2\n\
  ENDIF\n\
  IF ((time > stageTime + initialTime) AND (state = 2)) THEN\n\
    state = 3\n\
  ENDIF\n\
  IF ((time > stageTime + riseTime + initialTime) AND ((state = 3) AND (returnToZeroFlag_HashCode = 0))) THEN\n\
    initialTime = time\n\
    initialAngle = angle\n\
    initialAngularVelocity = -initialAngularVelocity\n\
    stageTime = ABSF(initialAngle/initialAngularVelocity)\n\
    riseTime = stageTime/3\n\
    state = 1\n\
    returnToZeroFlag_HashCode = 1\n\
  ENDIF\n\
  IF ((time > stageTime + riseTime + initialTime) AND ((state = 3) AND (returnToZeroFlag_HashCode = 1))) THEN\n\
    state = 4\n\
  ENDIF\n\
ENDIF
