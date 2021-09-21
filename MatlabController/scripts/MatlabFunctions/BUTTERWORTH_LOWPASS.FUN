MENU_TITLE = Butterworth low pass filter
DESCRIPTION = Butterworth low pass filter
MENU_TITLE_FR = Filtre passe bas de Butterworth
DESCRIPTION_FR = Filtre passe bas de Butterworth

# Now start specific matlab function 
FUNCTION_CODE = % Compute low pass Butterworth filter\n\
% Create filtered signal\n\
signalSizes = size(inputSignal.Values); % signalSizes(1) : trials number - signalSizes(2) : samples number\n\
outputSignal.Values = zeros(signalSizes(1), signalSizes(2));\n\
outputSignal.NbSamples = inputSignal.NbSamples;\n\
outputSignal.isSignal = 1;\n\
outputSignal.isCategory = 0;\n\
outputSignal.isEvent = 0;\n\
outputSignal.NbMarkersGroups = 0;\n\
outputSignal.NbFeatures = 0;\n\
outputSignal.SampleFrequency = inputSignal.SampleFrequency;\n\
outputSignal.FrontCut = inputSignal.FrontCut;\n\
outputSignal.EndCut = inputSignal.EndCut;\n\
% Compute filtering\n\
nyquistFrequency = 0.5 * inputSignal.SampleFrequency; % Nyquist frequency is half sample frequency\n\
normalizedCutOffFrequency = cutOffFrequency / nyquistFrequency;\n\
[num, den] = butter(order, normalizedCutOffFrequency);\n\
outputSignal.Values(trialsList, :) = filtfilt(num, den, inputSignal.Values(trialsList, :)')';\n\
clear num den nyquistFrequency normalizedCutOffFrequency signalSizes trialNumber;\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'outputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'outputSignal'}];\n\
end\n\