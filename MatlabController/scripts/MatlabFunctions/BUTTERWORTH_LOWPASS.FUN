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
% Create filtered signal\n\
SignalSizes = size(inputSignal.Values); % SignalSizes(1) : trials number - SignalSizes(2) : samples number\n\
outputSignal.Values = zeros(SignalSizes(1), SignalSizes(2));\n\
outputSignal.NbSamples = inputSignal.NbSamples;\n\
outputSignal.isSignal = 1;\n\
outputSignal.isCategory = 0;\n\
outputSignal.isEvent = 0;\n\
outputSignal.NbMarkers = 0;\n\
outputSignal.NbMarkersGroups = 0;\n\
outputSignal.NbFields = 0;\n\
outputSignal.SampleFrequency = inputSignal.SampleFrequency;\n\
outputSignal.FrontCut = ones(SignalSizes(1), 1);\n\
outputSignal.EndCut = SignalSizes(2)*ones(SignalSizes(1), 1);\n\
% Compute filtering\n\
nyquistFrequency = 0.5 * inputSignal.SampleFrequency; % Nyquist frequency is half sample frequency\n\
normalizedCutOffFrequency = cutOffFrequency / nyquistFrequency;\n\
[num, den] = butter(order, normalizedCutOffFrequency);\n\
for trialNumber = trialsList\n\
	outputSignal.Values(trialNumber, :) = filtfilt(num, den, inputSignal.Values(trialNumber, :));\n\
end\n\
clear num den nyquistFrequency normalizedCutOffFrequency SignalSizes trialNumber;\n\
if (~exist('createdOrModifiedChannels'))\n\
createdOrModifiedChannels = {'outputSignal'};\n\
else\n\
createdOrModifiedChannels = [createdOrModifiedChannels; 'outputSignal'];\n\
end






