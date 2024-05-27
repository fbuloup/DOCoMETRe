MENU_TITLE = Timer marker
DESCRIPTION = Mark input signal at specific time value
MENU_TITLE_FR = Marqueur temporel
DESCRIPTION_FR = Marque le signal d'entrée à un valeur temporelle spécifique

# Now start specific matlab function 
FUNCTION_CODE = % Compute Time marker\n\
signalSizes = size(inputSignal.Values); % signalSizes(1) : trials number - signalSizes(2) : samples number\n\
compute = true;\n\
nbMarkersGroups = 0;\n\
% Check if markers Group already exists\n\
if isfield(inputSignal, 'NbMarkersGroups')\n\
	\tnbMarkersGroups = inputSignal.NbMarkersGroups;\n\
	\tfor markersGroupNumber = 1:nbMarkersGroups\n\
		\t\tif inputSignal.MarkersGroupsLabels(markersGroupNumber) == "markersGroupLabel"\n\
			\t\t\tcompute = false;\n\
			\t\t\tbreak;\n\
		\t\tend\n\
	\tend\n\
end\n\
if compute\n\
	\t% Create new output markers group\n\
	\tinputSignal.NbMarkersGroups = inputSignal.NbMarkersGroups + 1;\n\
	\tif inputSignal.NbMarkersGroups == 1\n\
		\t\tinputSignal.MarkersGroupsLabels = "markersGroupLabel";\n\
	\telse\n\
		\t\tinputSignal.MarkersGroupsLabels = [inputSignal.MarkersGroupsLabels ; "markersGroupLabel"];\n\
	\tend\n\
	\tinputSignal.MarkersGroup_markersGroupLabel_Values = [];\n\
	\t% Get sample frequency\n\
	\tsampleFrequency = inputSignal.SampleFrequency;\n\
	\t% Get end cut values\n\
	\tendCutValues = inputSignal.EndCut;\n\
	\t% Loop over trials list\n\
	\tfor trialNumber = trialsList\n\
		\t\tindex = round(timeMarkerValue*sampleFrequency+1);\n\
		\t\tendCut = endCutValues(trialNumber);\n\
		\t\tif endCut >= index && markLastSample ~= true\n\
			\t\t\tnewTime = (index-1)/sampleFrequency;\n\
			\t\t\tinputSignal.MarkersGroup_markersGroupLabel_Values = [inputSignal.MarkersGroup_markersGroupLabel_Values; trialNumber, newTime, inputSignal.Values(trialNumber,index)];\n\
		\t\telseif markLastSample == true\n\
			\t\t\tnewTime = (endCutValues(trialNumber) - 1)/sampleFrequency;\n\
			\t\t\tnewIndex = endCutValues(trialNumber) - 1;\n\
			\t\t\tinputSignal.MarkersGroup_markersGroupLabel_Values = [inputSignal.MarkersGroup_markersGroupLabel_Values; trialNumber, newTime, inputSignal.Values(trialNumber,newIndex)];\n\
		\t\tend\n\
	\tend\n\
	\tif (~exist('createdOrModifiedChannels'))\n\
		\t\tcreatedOrModifiedChannels = {'inputSignal'};\n\
	\telse\n\
		\t\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
	\tend\n\
else\n\
	\tif(exist('ErrorMessages'))\n\
		\t\tErrorMessages = [ErrorMessages, {[datestr(now,'HH:MM:SS.FFF'), ' - From Time Marker function -> A Marker with this label already exists : markersGroupLabel (inputSignal)']}];\n\
	\telse\n\
		\t\tErrorMessages = {[datestr(now,'HH:MM:SS.FFF'), ' - From Time Marker function -> A Marker with this label already exists : markersGroupLabel (inputSignal)']};\n\
	\tend\n\
end\n\
clear compute endCut endCutValues index markersGroupNumber nbMarkersGroups newTime sampleFrequency signalSizes trialNumber\n\