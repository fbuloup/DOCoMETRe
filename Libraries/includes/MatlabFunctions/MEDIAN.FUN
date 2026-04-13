MENU_TITLE = 1D Median filter (medfilt1)
DESCRIPTION = 1D Median filter (use medfilt1 function)
MENU_TITLE_FR = Filtre médian 1D (medfilt1)
DESCRIPTION_FR = Filtre médian 1D (utilise la fonction medfilt1) 

# Now start specific matlab function 
FUNCTION_CODE = % Compute median filter\n\
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
outputSignal.Values(trialsList, :) = medfilt1(inputSignal.Values(trialsList, :)', order)';\n\
clear signalSizes trialNumber;\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'outputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'outputSignal'}];\n\
end\n\