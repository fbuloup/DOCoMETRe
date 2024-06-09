'<ADbasic Header, Headerversion 001.001>
' Optimize                       = Yes
' Optimize_Level                 = 1
' Stacksize                      = 1000
' Lib_File                       = Yes
'<Header End>
' Round to nearest integer as float
LIB_FUNCTION ceil(BYVAL value AS FLOAT) AS FLOAT
  DIM floatValue AS FLOAT
  DIM integerValue AS INTEGER  
  floatValue = value + 0.5
  integerValue = floatValue  
  ceil = integerValue
LIB_ENDFUNCTION

' Lower nearest integer as float
LIB_FUNCTION floor(BYVAL value AS FLOAT) AS FLOAT
  DIM integerValue AS INTEGER  
  integerValue = value  
  floor = integerValue
LIB_ENDFUNCTION

' Lower nearest integer as integer
LIB_FUNCTION trunc(BYVAL value AS FLOAT) AS INTEGER
  trunc = floor(value)
LIB_ENDFUNCTION

' Return float fractional part 
LIB_FUNCTION frac(BYVAL value AS FLOAT) AS FLOAT
  frac = value - floor(value)
LIB_ENDFUNCTION

' Math Power function
LIB_FUNCTION power(BYVAL xValue AS FLOAT, BYVAL yValue AS FLOAT) AS FLOAT
  power = xValue^(yValue)
LIB_ENDFUNCTION

' Math signus function
LIB_FUNCTION sign(BYVAL xValue AS FLOAT) AS FLOAT
  IF(xValue > 0) THEN 
    sign = 1 
  ENDIF
  IF(xValue = 0) THEN 
    sign = 0 
  ENDIF
  IF(xValue < 0) THEN 
    sign = -1 
  ENDIF
LIB_ENDFUNCTION

' Math max indice value function : find indice of max value in a float array 
LIB_FUNCTION indiceOfMaxValueInArray(BYREF values[] AS FLOAT, BYVAL nbElements AS INTEGER) AS INTEGER
  DIM i AS INTEGER
  DIM maxIndice AS INTEGER
  i = 1
  maxIndice = 1
  IF(i < nbElements) THEN
    DO
      IF(values[i] > values[maxIndice]) THEN
        maxIndice = i
      ENDIF
      i = i + 1
    UNTIL (i > nbElements)
  ENDIF  
  IndiceOfMaxValueInArray = maxIndice
LIB_ENDFUNCTION

' Swap two values in a float array
LIB_SUB swap(BYREF values[] AS FLOAT, BYVAL i1 AS INTEGER, BYVAL i2 AS INTEGER)
  DIM tmpValue AS FLOAT
  tmpValue = values[i1]
  values[i1] = values[i2]
  values[i2] = tmpValue
LIB_ENDSUB

' Sort array
LIB_SUB sortArray(BYREF values[] AS FLOAT, BYVAL nbElements AS INTEGER)
  DIM maxIndice AS INTEGER
  IF (nbElements > 1) THEN
    DO 
      maxIndice = indiceOfMaxValueInArray(values, nbElements)
      swap(values, nbElements, maxIndice)
      nbElements = nbElements - 1
    UNTIL (nbElements = 1)
  ENDIF
LIB_ENDSUB

' Compute median of a float array
LIB_FUNCTION median(BYREF values[] AS FLOAT, BYVAL nbElements AS INTEGER) AS FLOAT
  DIM nbElementsFloat AS FLOAT
  DIM fractionalPart AS FLOAT
  nbElementsFloat = nbElements
  fractionalPart = frac((nbElementsFloat + 1)/2)
  sortArray(values, nbElements)  
  IF(fractionalPart = 0) THEN
    median = values[(nbElements + 1)/2]
  ELSE
    median = (values[nbElements/2] + values[nbElements/2 + 1])/2
  ENDIF
LIB_ENDFUNCTION

' Compute median of a binary array
LIB_FUNCTION binaryMedian(BYREF values[] AS INTEGER, BYVAL nbElements AS INTEGER) AS INTEGER
  DIM i AS INTEGER 
  binaryMedian = 1
  i = 1
  DO
    binaryMedian = binaryMedian AND values[i]
  UNTIL (i > nbElements)
LIB_ENDFUNCTION
