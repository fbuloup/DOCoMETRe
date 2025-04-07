MENU_TITLE = Butterworth high pass filter (filtfilt)
DESCRIPTION = As 'filtfilt' function is used, resulting order will be twice the provided one (filtfilt)
MENU_TITLE_FR = Filtre passe haut de Butterworth (filtfilt)
DESCRIPTION_FR = Comme la fonction 'filtfilt' est utilisée, l'ordre résultant sera le double de celui fourni (filtfilt)

# Now start specific matlab function 
FUNCTION_CODE = % Compute high pass Butterworth filter\n\
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
[num, den] = butter(order, normalizedCutOffFrequency, 'high');\n\
outputSignal.Values(trialsList, :) = filtfilt(num, den, inputSignal.Values(trialsList, :)')';\n\
clear num den nyquistFrequency normalizedCutOffFrequency signalSizes trialNumber;\n\
if (~exist('createdOrModifiedChannels'))\n\
createdOrModifiedChannels = {'outputSignal'};\n\
else\n\
createdOrModifiedChannels = [createdOrModifiedChannels; {'outputSignal'}];\n\
end\n\