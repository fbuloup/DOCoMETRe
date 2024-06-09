MENU_TITLE_FR = Remplacer les valeurs d'échantillons
DESCRIPTION_FR = Remplace les valeurs d'une série d'échantillons par la valeur d'un autre échantillon
MENU_TITLE = Replace samples values
DESCRIPTION = Replace the values ​​of a series of samples with the value of another sample

# Now start specific matlab function 
FUNCTION_CODE = % Compute replace samples\n\
% Create output signal\n\
outputSignal.Values = inputSignal.Values;\n\
outputSignal.NbSamples = inputSignal.NbSamples;\n\
outputSignal.isSignal = 1;\n\
outputSignal.isCategory = 0;\n\
outputSignal.isEvent = 0;\n\
outputSignal.NbMarkersGroups = 0;\n\
outputSignal.NbFeatures = 0;\n\
outputSignal.SampleFrequency = inputSignal.SampleFrequency;\n\
outputSignal.FrontCut = inputSignal.FrontCut;\n\
outputSignal.EndCut = inputSignal.EndCut;\n\
% Loop over trials	\n\
for trialNumber = trialsList\n\
	\treplacementValue = inputSignal.Values(trialNumber , replacementIndex);\n\
	\toutputSignal.Values(trialNumber, fromIndex:toIndex) = replacementValue;\n\
end\n\
clear replacementValue trialNumber\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'outputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'outputSignal'}];\n\
end\n\