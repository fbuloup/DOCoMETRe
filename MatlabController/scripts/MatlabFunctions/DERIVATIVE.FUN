MENU_TITLE = Derivative
DESCRIPTION = Compute derivative using diff 
MENU_TITLE_FR = Dérivée
DESCRIPTION_FR = Calcul de la dérivé en utilisant diff

# Now start specific matlab function 
FUNCTION_CODE = # Compute Derivative\n\
% Create derivative signal\n\
SignalSizes = size(inputSignal.Values); % SignalSizes(1) : trials number - SignalSizes(2) : samples number\n\
outputSignal.Values = zeros(SignalSizes(1), SignalSizes(2));\n\
outputSignal.NbSamples = inputSignal.NbSamples;\n\
outputSignal.isSignal = 1;\n\
outputSignal.isCategory = 0;\n\
outputSignal.isEvent = 0;\n\
outputSignal.NbMarkersGroups = 0;\n\
outputSignal.NbFeatures = 0;\n\
outputSignal.SampleFrequency = inputSignal.SampleFrequency;\n\
outputSignal.FrontCut = inputSignal.FrontCut;\n\
outputSignal.EndCut = inputSignal.EndCut;\n\
outputSignal.Values(trialsList,:)=[diff(InputSignal1.Values(trialsList,:)) 0]*inputSignal.SampleFrequency;\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'outputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; 'outputSignal'];\n\
end\n\