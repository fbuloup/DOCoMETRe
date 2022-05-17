MENU_TITLE = Maxima
DESCRIPTION = Mark all peaks
MENU_TITLE_FR = Maxima
DESCRIPTION_FR = Marque tous les pics

# Now start specific matlab function 
FUNCTION_CODE = % Compute find peaks\n\
signalSizes = size(inputSignal.Values); % signalSizes(1) : trials number - signalSizes(2) : samples number\n\
nbMarkersGroups = 0;\n\
compute = true;\n\
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
	\t% Get signal input values\n\
	\tvalues = inputSignal.Values;\n\
	\t% Create new output markers group\n\
	\tnbMarkersGroups = nbMarkersGroups + 1;\n\
	\tinputSignal.NbMarkersGroups = nbMarkersGroups;\n\
	\tif nbMarkersGroups == 1\n\
		\t\tinputSignal.MarkersGroupsLabels = "markersGroupLabel";\n\
	\telse\n\
		\t\tinputSignal.MarkersGroupsLabels = [inputSignal.MarkersGroupsLabels ; "markersGroupLabel"];\n\
	\tend\n\
	\tfpMarkersValues = [];\n\
	\t% Get sample frequency to float\n\
	\tsampleFrequency = inputSignal.SampleFrequency;\n\
	\t% Get from and to values\n\
	\tif "From_Beginning" == "inputMarker1"\n\
		\t\tfromValues = zeros(signalSizes(1),3);\n\
		\t\tfromValues(:,1) = trialsList;\n\
		\t\tfromValues(:,3) = values(:,1);\n\
	\telse\n\
		\t\tif(endsWith("inputMarker1",".FrontCut"))\n\
			\t\t\tfromValues = zeros(signalSizes(1),3);\n\
			\t\t\tfromValues(:,1) = trialsList;\n\
			\t\t\tfromValues(:,2) = inputMarker1 ./ sampleFrequency;\n\
			\t\t\tfromValues(:,3) = values(:,1);\n\
		\t\telse\n\
			\t\t\tfromValues = inputMarker1_Values;\n\
		\t\tend\n\
	\tend\n\
	\tif "To_End" == "inputMarker2"\n\
		\t\ttoValues = zeros(signalSizes(1),3);\n\
		\t\ttoValues(:,1) = trialsList;\n\
		\t\ttoValues(:,2) = inputSignal.NbSamples ./ sampleFrequency;\n\
		\t\ttoValues(:,3) = inputSignal.NbSamples;\n\
	\telse\n\
		\t\tif(endsWith("inputMarker2", ".EndCut"))\n\
			\t\t\ttoValues = zeros(signalSizes(1),3);\n\
			\t\t\ttoValues(:,1) = trialsList;\n\
			\t\t\ttoValues(:,2) = inputMarker2 ./ sampleFrequency;\n\
			\t\t\ttoValues(:,3) = values(:,1);\n\
		\t\telse\n\
			\t\t\ttoValues = inputMarker2_Values;\n\
		\t\tend\n\
	\t\end\n\
	\tfor trialNumber = [trialsList]\n\
		\t\t% Get from and to index in from and to values for trialNumber\n\
		\t\tfromIndex = find(fromValues(:,1) == trialNumber);\n\
		\t\ttoIndex = find(toValues(:,1) == trialNumber);\n\
		\t\t% if both indices exist\n\
		\t\tif(~isempty(fromIndex) && ~isempty(toIndex))\n\
			\t\t\t% Convert from and to in samples number\n\
			\t\t\tfromIndex = floor(fromValues(fromIndex(1),2) * sampleFrequency) + 1;\n\
			\t\t\ttoIndex = floor(toValues(toIndex(1),2) * sampleFrequency);\n\
			\t\t\t% Compute max between from and to index\n\
			\t\t\t[peakValues maxIndices] = findpeaks(values(trialNumber, fromIndex:toIndex), height, threshold, distance, prominence);\n\
			\t\t\tfor n = 1:length(maxIndices)\n\
			\t\t\t\tpeakValue = peakValues(n);\n\
			\t\t\t\tpeakMarkerTime = (fromIndex + maxIndices(n) - 2)/sampleFrequency;\n\
			\t\t\t\tfpMarkersValues = [fpMarkersValues; trialNumber, peakMarkerTime, peakValue];\n\
			\t\t\tend\n\
		\t\tend\n\
	\tend\n\
	\tinputSignal.MarkersGroup_markersGroupLabel_Values = fpMarkersValues;\n\
	\tif (~exist('createdOrModifiedChannels'))\n\
		\t\tcreatedOrModifiedChannels = {'inputSignal'};\n\
	\telse\n\
		\t\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
	\tend\n\
	\tclear markersGroupNumber compute fromIndex fromValues nbMarkersGroups sampleFrequency signalSizes toIndex toValues trialNumber values\n\
	\tclear fpMarkersValues maxIndices n peakMarkerTime peakValue peakValues\n\
else\n\
	\tif(exist('ErrorMessages'))\n\
		\t\tErrorMessages = [ErrorMessages, {[datestr(now,'HH:MM:SS.FFF'), ' - From Find Amplitude Forward function -> A Marker with this label already exists : markersGroupLabel']}];\n\
	\telse\n\
		\t\tErrorMessages = {[datestr(now,'HH:MM:SS.FFF'), ' - From Find Amplitude Forward function -> A Marker with this label already exists : markersGroupLabel (inputSignal)']};\n\
	\tend\n\
	\tclear compute markersGroupNumber nbMarkersGroups signalSizes\n\
end\n\