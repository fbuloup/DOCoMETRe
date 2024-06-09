MENU_TITLE = Minimum
DESCRIPTION = Mark minimum amplitude
MENU_TITLE_FR = Minimum
DESCRIPTION_FR = Marque l'amplitude minimale

# Now start specific matlab function 
FUNCTION_CODE = % Compute Minimum marker\n\
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
	\tinputSignal.NbMarkersGroups = nbMarkersGroups + 1;\n\
	\tif inputSignal.NbMarkersGroups == 1\n\
		\t\tinputSignal.MarkersGroupsLabels = "markersGroupLabel";\n\
	\telse\n\
		\t\tinputSignal.MarkersGroupsLabels = [inputSignal.MarkersGroupsLabels ; "markersGroupLabel"];\n\
	\tend\n\
	\tinputSignal.MarkersGroup_markersGroupLabel_Values = [];\n\
	\t% Get from and to values\n\
	\tif "From_Beginning" == "inputMarker1"\n\
		\t\tfromValues = zeros(signalSizes(1),3);\n\
		\t\tfromValues(:,1) = trialsList;\n\
		\t\tfromValues(:,3) = inputSignal.Values(:,1);\n\
	\telse\n\
		\t\tif(endsWith("inputMarker1",".FrontCut"))\n\
			\t\t\tfromValues = zeros(signalSizes(1),3);\n\
			\t\t\tfromValues(:,1) = trialsList;\n\
			\t\t\tfromValues(:,2) = inputMarker1 ./ inputSignal.SampleFrequency;\n\
			\t\t\tfromValues(:,3) = inputSignal.Values(:,1);\n\
		\t\telse\n\
			\t\t\tfromValues = inputMarker1_Values;\n\
		\t\tend\n\
	\tend\n\
	\tif "To_End" == "inputMarker2"\n\
		\t\ttoValues = zeros(signalSizes(1),3);\n\
		\t\ttoValues(:,1) = trialsList;\n\
		\t\ttoValues(:,2) = (inputSignal.NbSamples - 1)/inputSignal.SampleFrequency;\n\
		\t\ttoValues(:,3) = inputSignal.NbSamples;\n\
	\telse\n\
		\t\tif(endsWith("inputMarker2",".EndCut"))\n\
			\t\t\ttoValues = zeros(signalSizes(1),3);\n\
			\t\t\ttoValues(:,1) = trialsList;\n\
			\t\t\ttoValues(:,2) = inputMarker2 ./ inputSignal.SampleFrequency;\n\
			\t\t\ttoValues(:,3) = inputSignal.Values(:,1);\n\
		\t\telse\n\
			\t\t\ttoValues = inputMarker2_Values;\n\
		\t\tend\n\
	\tend\n\
	\tfor trialNumber = trialsList\n\
		\t\t% Get from and to index in from and to values for trialNumber\n\
		\t\tfromIndex = find(fromValues(:,1) == (trialNumber));\n\
		\t\ttoIndex = find(toValues(:,1) == (trialNumber));\n\
		\t\t% if both indices exist\n\
		\t\tif(~isempty(fromIndex) && ~isempty(toIndex))\n\
			\t\t\t% Convert from and to in samples number\n\
			\t\t\tfromIndex = floor(fromValues(fromIndex(1),2) * inputSignal.SampleFrequency + 1);\n\
			\t\t\ttoIndex = floor(toValues(toIndex(1),2) * inputSignal.SampleFrequency + 1);\n\
			\t\t\t% Compute min between from and to index\n\
			\t\t\t[minValue, minIndex] = min(inputSignal.Values(trialNumber, fromIndex:toIndex));\n\
			\t\t\tminMarkerTime = (fromIndex + minIndex - 2)/inputSignal.SampleFrequency;\n\
			\t\t\tinputSignal.MarkersGroup_markersGroupLabel_Values = [inputSignal.MarkersGroup_markersGroupLabel_Values; trialNumber, minMarkerTime, minValue];\n\
		\t\tend\n\
	\tend\n\
	\tif (~exist('createdOrModifiedChannels'))\n\
		\t\tcreatedOrModifiedChannels = {'inputSignal'};\n\
	\telse\n\
		\t\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
	\tend\n\
else\n\
	\tif(exist('ErrorMessages'))\n\
		\t\tErrorMessages = [ErrorMessages, {[datestr(now,'HH:MM:SS.FFF'), ' - From Minimum function -> A Marker with this label already exists : markersGroupLabel (inputSignal)']}];\n\
	\telse\n\
		\t\tErrorMessages = {[datestr(now,'HH:MM:SS.FFF'), ' - From Minimum function -> A Marker with this label already exists : markersGroupLabel (inputSignal)']};\n\
	\tend\n\
end\n\
clear compute signalSizes nbMarkersGroups fromIndex fromValues minIndex minMarkerTime minValue nbMarkersGroups toIndex toValues trialNumber markersGroupNumber;\n\