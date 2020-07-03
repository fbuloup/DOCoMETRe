MENU_TITLE = Butterworth low pass filter
DESCRIPTION = Butterworth low pass filter
MENU_TITLE_FR = Filtre passe bas de Butterworth
DESCRIPTION_FR = Filtre passe bas de Butterworth
# Now start specific matlab function 

#%Enter your comments here
#function Butterworth_lowpass (TrialsList , signalsNamesList, markersNamesList, fieldsNamesList, signalsNamesSuffix, newMarkersNamesList, newFieldsNamesList, signalsModifiedNumber, Cutoff, Order)
#%beginAnalyseHeader
#%SignalsUsedNumber = 1
#%MarkersUsedNumber = 0
#%FieldsUsedNumber = 0
#%SignalsCreatedNumber = 1
#%MarkersCreatedNumber = 0
#%fieldsCreatedNumber = 0
#%signalsModifiedNumber = 0
#%param1MatlabType = float
#%param1Component = 1
#%param1Label = Cutoff
#%param1ToolTip = Cutoff frequency
#%param1RegExp = (^[+]?\d*\.?\d*[1-9]+\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\d*\.\d*([eE][-+]?[0-9]+)?$)
#%param1AvailableListValue = 
#%param1DefaultValue = 
#%param1SignalsAvailable = false
#%param1MarkersAvailable = false
#%param1FieldsAvailable = false
#%param2MatlabType = integer
#%param2Component = 1
#%param2Label = Order
#%param2ToolTip = Filter order
#%param2RegExp = ^[+]?0*[1-9]+[0-9]*$
#%param2AvailableListValue = 
#%param2DefaultValue = 
#%param2SignalsAvailable = false
#%param2MarkersAvailable = false
#%param2FieldsAvailable = false
#%endAnalyseHeader


FUNCTION_CODE = % Compute low pass Butterworth filter\n\
nyquistFrequency = 0.5*InputSignal1.SampleFrequency;\n\
normalizedCutOffFrequency = cutOffFrequency/nyquistFrequency;\n\
[num, den] = butter(order, normalizedCutOffFrequency);\n\
for trialNumber = trialsList\n\
	OutputSignal1.Values(TrialNumber, :) = filtfilt(num, den, InputSignal1.Values(trialNumber, :));\n\
end