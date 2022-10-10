MENU_TITLE = Remove pulses
DESCRIPTION = Suppress pulses with predefined amplitude and duration
MENU_TITLE_FR = Supprimer les impulsions
DESCRIPTION_FR = Supprimer les impulsions d'amplitude et de durée déterminées

# Now start specific matlab function 
FUNCTION_CODE = % Remove pulses\n\
% Create cleared signal\n\
signalSizes = size(inputSignal.Values); % signalSizes(1) : trials number - signalSizes(2) : samples number\n\
outputSignal.Values = inputSignal.Values; %zeros(signalSizes(1), signalSizes(2));\n\
outputSignal.NbSamples = inputSignal.NbSamples;\n\
outputSignal.isSignal = 1;\n\
outputSignal.isCategory = 0;\n\
outputSignal.isEvent = 0;\n\
outputSignal.NbMarkersGroups = 0;\n\
outputSignal.NbFeatures = 0;\n\
outputSignal.SampleFrequency = inputSignal.SampleFrequency;\n\
outputSignal.FrontCut = inputSignal.FrontCut;\n\
outputSignal.EndCut = inputSignal.EndCut;\n\
lastDelta = 0;\n\
pulseWidth = inputSignal.SampleFrequency*duration;\n\
for trialNumber = trialsList\n\
		trialValues = inputSignal.Values(trialNumber,:);\n\
		previousValue = trialValues(1);\n\
		lastSampleNumber = -1;\n\
		for sampleNumber = 2:inputSignal.NbSamples(trialNumber)\n\
			currentValue = trialValues(sampleNumber);\n\
			if abs(currentValue - previousValue) > delta\n\
				if sign((currentValue - previousValue)*lastDelta) <= 0\n\
					if lastSampleNumber > -1\n\
						% Interpolate\n\
						xInputValues = [lastSampleNumber/inputSignal.SampleFrequency, sampleNumber/inputSignal.SampleFrequency];\n\
						yInputValues = [trialValues(lastSampleNumber), trialValues(sampleNumber)];\n\
						xOutputValues = (lastSampleNumber:sampleNumber)/outputSignal.SampleFrequency;\n\
						outputSignal.Values(trialNumber,lastSampleNumber:sampleNumber) = interp1(xInputValues, yInputValues, xOutputValues, 'linear');\n\
						lastSampleNumber = -1;\n\
						lastDelta = 0;\n\
					else\n\
						lastSampleNumber = sampleNumber - 1;\n\
						lastDelta = currentValue - previousValue;\n\
					end\n\
				else\n\
					lastSampleNumber = -1;\n\
					lastDelta = 0;\n\
				end\n\
			end\n\
			if lastSampleNumber > -1\n\
				if sampleNumber - lastSampleNumber > pulseWidth\n\
					lastSampleNumber = -1;\n\
					lastDelta = 0;\n\
				end\n\
			end\n\
			previousValue = currentValue;\n\
		end\n\
end\n\
clear previousValue currentValue lastSampleNumber sampleNumber\n\
clear xInputValues yInputValues xOutputValues\n\
clear trialNumber trialValues pulseWidth lastDelta\n\
if (~exist('createdOrModifiedChannels'))\n\
		createdOrModifiedChannels = {'outputSignal'};\n\
else\n\
		createdOrModifiedChannels = [createdOrModifiedChannels; {'outputSignal'}];\n\
end