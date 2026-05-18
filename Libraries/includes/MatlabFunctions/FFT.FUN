MENU_TITLE = FFT
DESCRIPTION = Compute Single Sided FFT (real-valued signal) 
MENU_TITLE_FR = FFT
DESCRIPTION_FR = Calcule la FFT monolatérale (signal à valeurs réelles)

# Now start specific matlab function 
FUNCTION_CODE = % Compute FFT\n\
% Create fft signal\n\
signalSizes = size(inputSignal.Values); % signalSizes(1) : trials number - signalSizes(2) : samples number\n\
outputSignal.Values = zeros(signalSizes(1), signalSizes(2));\n\
outputSignal.NbSamples = inputSignal.NbSamples;\n\
outputSignal.isSignal = 1;\n\
outputSignal.isCategory = 0;\n\
outputSignal.isEvent = 0;\n\
outputSignal.NbMarkersGroups = 0;\n\
outputSignal.NbFeatures = 0;\n\
outputSignal.SampleFrequency = signalSizes(2)/inputSignal.SampleFrequency;\n\
outputSignal.FrontCut = inputSignal.FrontCut;\n\
outputSignal.EndCut = inputSignal.EndCut;\n\
if (mod(inputSignal.NbSamples(1),2) == 0)\n\
	outputSignal.EndCut(1:signalSizes(1)) = signalSizes(2)/2;\n\
else\n\
	outputSignal.EndCut(1:signalSizes(1)) = (signalSizes(2) + 1)/2;\n\
end\n\
tempValues = abs(fft(inputSignal.Values(trialsList,:)'))';\n\
outputSignal.Values(trialsList,:) = tempValues./signalSizes(2);\n\
outputSignal.isFrequencyDomain = true;\n\
clear signalSizes tempValues;\n\
if (~exist('createdOrModifiedChannels'))\n\
	createdOrModifiedChannels = {'outputSignal'};\n\
else\n\
	createdOrModifiedChannels = [createdOrModifiedChannels; {'outputSignal'}];\n\
end\n\