MENU_TITLE = Motion Distance
DESCRIPTION = Compute motion distance
MENU_TITLE_FR = Distance de mouvement
DESCRIPTION_FR = Calcule la distance du mouvement

# Now start specific matlab function 
FUNCTION_CODE = % Compute Motion Distance\n\
% Get signals input values\n\
values1 = inputSignal1.Values;\n\
values2 = inputSignal2.Values;\n\
% Create output signal. It is assumed that input signals are same size, same samples number and same sample frequency\n\
% Create filtered signal\n\
signalSizes = size(inputSignal1.Values); % signalSizes(1) : trials number - signalSizes(2) : samples number\n\
outputSignal.Values = zeros(signalSizes(1), signalSizes(2));\n\
outputSignal.NbSamples = inputSignal1.NbSamples;\n\
outputSignal.isSignal = 1;\n\
outputSignal.isCategory = 0;\n\
outputSignal.isEvent = 0;\n\
outputSignal.NbMarkersGroups = 0;\n\
outputSignal.NbFeatures = 0;\n\
outputSignal.SampleFrequency = inputSignal1.SampleFrequency;\n\
outputSignal.FrontCut = inputSignal1.FrontCut;\n\
outputSignal.EndCut = inputSignal1.EndCut;\n\
% Get from and to values\n\
if "From_Beginning" == "inputMarker1"\n\
	\tfromValues = zeros(signalSizes(1),3);\n\
	\tfromValues(:,1) = trialsList;\n\
	\tfromValues(:,3) = values1(:,1);\n\
else\n\
	\tfromValues = inputMarker1_Values;\n\
end\n\
if "To_End" == "inputMarker2"\n\
	\ttoValues(:,1) = [trialsList];\n\
	\ttoValues(:,2) = outputSignal.NbSamples/outputSignal.SampleFrequency;\n\
	\ttoValues(:,3) = outputSignal.NbSamples;\n\
else\n\
	\ttoValues = inputMarker2_Values;\n\
end\n\
% Loop over trials	\n\
for trialNumber = trialsList\n\
	\t% Get from and to index in from and to values for trialNumber\n\
	\tfromIndex = find(fromValues(:,1) == trialNumber);\n\
	\ttoIndex = find(toValues(:,1) == trialNumber);\n\
	\t% if both indices exist\n\
	\tif(~isempty(fromIndex) && ~isempty(toIndex))\n\
		\t\t% Convert from and to in samples number\n\
		\t\tfromIndex = floor(fromValues(fromIndex(1),2) * outputSignal.SampleFrequency) + 1;\n\
		\t\ttoIndex = floor(toValues(toIndex(1),2) * outputSignal.SampleFrequency) + 1;\n\
		\t\t% Init first output signal sample\n\
		\t\toutputSignal.Values(trialNumber, fromIndex) = 0;\n\
		\t\t% Compute deltas\n\
		\t\tdx2 = (diff(values1(trialNumber, fromIndex:toIndex))).^2;\n\
		\t\tdy2 = (diff(values2(trialNumber, fromIndex:toIndex))).^2;\n\
		\t\t% Square roots\n\
		\t\toutputSignal.Values(trialNumber, fromIndex+1:toIndex)= sqrt(dx2 + dy2);\n\
 	    \t\t% Sums\n\
		\t\toutputSignal.Values(trialNumber, fromIndex:toIndex) = cumsum(outputSignal.Values(trialNumber, fromIndex:toIndex));\n\
	\tend\n\
end\n\
clear fromIndex toIndex fromValues toValues dx2 dy2 trialNumber values1 values2 signalSizes\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'outputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'outputSignal'}];\n\
end\n\