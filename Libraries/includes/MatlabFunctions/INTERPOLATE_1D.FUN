MENU_TITLE = 1-D Interpolation 
DESCRIPTION = This function requires two points and interpolate input signal between these points using selected method
MENU_TITLE_FR = Interpolation 1-D
DESCRIPTION_FR = Cette fonction requiert deux points et interpole le signal d'entrée entre ces deux points selon la méthode choisie

# Now start specific matlab function 
FUNCTION_CODE = % Compute 1-D interpolation\n\
% Create interpolated signal\n\
signalSizes = size(inputSignal.Values); % signalSizes(1) : trials number - signalSizes(2) : samples number\n\
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
% Compute\n\
indexMin = tMin*outputSignal.SampleFrequency;\n\
indexMax = tMax*outputSignal.SampleFrequency;\n\
xInputValues = [tMin, tMax];\n\
xOutputValues = (indexMin:indexMax)/outputSignal.SampleFrequency;\n\
for trialNumber = trialsList\n\
	\tyInputValues = [inputSignal.Values(trialNumber, indexMin+1),  inputSignal.Values(trialNumber, indexMax+1)];\n\
	\toutputSignal.Values(trialNumber,indexMin+1:indexMax+1) = interp1(xInputValues, yInputValues, xOutputValues, 'method');\n\
end\n\
clear signalSizes indexMin indexMax xInputValues yInputValues xOutputValues;\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'outputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'outputSignal'}];\n\
end\n\