MENU_TITLE = Timer marker
DESCRIPTION = Mark input signal at specific time value
MENU_TITLE_FR = Marqueur temporel
DESCRIPTION_FR = Marque le signal d'entrée à un valeur temporelle spécifique

# Now start specific python function 
FUNCTION_CODE = % Compute Time marker\n\
% Create new output markers group\n\
inputSignal.NbMarkersGroups = inputSignal.NbMarkersGroups + 1;\n\
if inputSignal.NbMarkersGroups == 1\n\
	\tinputSignal.MarkersGroupsLabels = "markersGroupLabel";\n\
else\n\
	\tinputSignal.MarkersGroupsLabels = [inputSignal.MarkersGroupsLabels ; "markersGroupLabel"];\n\
end\n\
inputSignal.MarkersGroup_markersGroupLabel_Values = [];\n\
% Get sample frequency\n\
sampleFrequency = inputSignal.SampleFrequency;\n\
% Get end cut values\n\
endCutValues = inputSignal.EndCut;\n\
% Loop over trials list\n\
for trialNumber = trialsList\n\
	\tindex = round(timeMarkerValue*sampleFrequency+1);\n\
	\tendCut = endCutValues(trialNumber);\n\
	\tif endCut >= index \n\
		\t\tnewTime = (index-1)/sampleFrequency;\n\
		\t\tinputSignal.MarkersGroup_markersGroupLabel_Values = [inputSignal.MarkersGroup_markersGroupLabel_Values; trialNumber+1, newTime, inputSignal.Values(trialNumber,index)];\n\
	\telseif markLastSample == True\n\
		\t\tnewTime = (endCutValues[TrialNumber] - 1)/sampleFrequency;\n\
		\t\tnewIndex = endCutValues[TrialNumber] - 1;\n\
		\t\tinputSignal.MarkersGroup_markersGroupLabel_Values = [inputSignal.MarkersGroup_markersGroupLabel_Values; trialNumber+1, newTime, inputSignal.Values(trialNumber,index)];\n\
	end\n\
end\n\
if (~exist('createdOrModifiedChannels'))\n\
	\tcreatedOrModifiedChannels = {inputSignal};\n\
else\n\
	\tcreatedOrModifiedChannels = createdOrModifiedChannels + ":inputSignal";\n\
end\n\