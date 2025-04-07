MENU_TITLE = Markers Group Transfert
DESCRIPTION = This fonction creates markers group on input signal.\n\ 
Markers time values come from input markers group.
MENU_TITLE_FR = Transférer un groupe de marqueurs
DESCRIPTION_FR = Cette fonction crée un groupe de marqueur sur le signal d'entrée.\n\
Les valeurs temporelles proviennent du groupe de marqueurs d'entrée.

# Now start specific matlab function 
FUNCTION_CODE = % Compute Markers Group Transfert\n\
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
	\tnbMarkersGroups = nbMarkersGroups + 1;\n\
	\tinputSignal.NbMarkersGroups = nbMarkersGroups;\n\
	\tif nbMarkersGroups == 1\n\
		\t\tinputSignal.MarkersGroupsLabels = "markersGroupLabel";\n\
	\telse\n\
		\t\tinputSignal.MarkersGroupsLabels = [inputSignal.MarkersGroupsLabels ; "markersGroupLabel"];\n\
	\t\end\n\
	\tinputSignal.MarkersGroup_markersGroupLabel_Values = [];\n\
	\t% Convert sample frequency to float\n\
	\tsampleFrequency = inputSignal.SampleFrequency;\n\
	\tfromValues = inputMarker_Values;\n\
	\tfor trialNumber = trialsList\n\
		\t\t% Get from index in from values for trialNumber\n\
		\t\tfromIndex = find(fromValues(:,1) == trialNumber);\n\
		\t\t% if index exists\n\
		\t\tif(~isempty(fromIndex))\n\
			\t\t\tfor markerNumber = 1:length(fromIndex)\n\
				\t\t\t\t% Get time maker\n\
				\t\t\t\ttrsfrTimeValue = fromValues(fromIndex(markerNumber),2);\n\
				\t\t\t\t% Convert from in samples number\n\
				\t\t\t\tmarkerTimeIndex = floor(trsfrTimeValue * sampleFrequency);\n\
				\t\t\t\t% Get amplitude marker value\n\
				\t\t\t\ttrsfrValue = inputSignal.Values(trialNumber, markerTimeIndex);\n\
				\t\t\t\tinputSignal.MarkersGroup_markersGroupLabel_Values = [inputSignal.MarkersGroup_markersGroupLabel_Values; trialNumber, trsfrTimeValue, trsfrValue];\n\
			\t\t\tend\n\
		\t\tend\n\
	\tend\n\
	\tclear compute fromIndex fromValues markersGroupNumber nbMarkersGroups sampleFrequency trialNumber signalSizes trsfrTimeValue trsfrValue;\n\
	\tclear markerTimeIndex markerNumber \n\
	\tif (~exist('createdOrModifiedChannels'))\n\
		\t\tcreatedOrModifiedChannels = {'inputSignal'};\n\
	\telse\n\
		\t\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
	\tend\n\
else\n\
	\tif(exist('ErrorMessages'))\n\
		\t\tErrorMessages = [ErrorMessages, {[datestr(now,'HH:MM:SS.FFF'), ' - From Transfert Markers group function -> A Marker with this label already exists : markersGroupLabel (inputSignal)']}];\n\
	\telse\n\
		\t\tErrorMessages = {[datestr(now,'HH:MM:SS.FFF'), ' - From Transfert Markers group function -> A Marker with this label already exists : markersGroupLabel (inputSignal)']};\n\
	\tend\n\
end\n\