MENU_TITLE = Find amplitude backward
DESCRIPTION = Find amplitude backward
MENU_TITLE_FR = Trouver l'amplitude en arrière
DESCRIPTION_FR = Trouver l'amplitude en arrière

# Now start specific matlab function 
FUNCTION_CODE = % Compute Find amplitude backward\n\
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
	\tinputSignal.MarkersGroup_markersGroupLabel_Values = [];\n\
	\tfabMarkersValues = inputSignal.MarkersGroup_markersGroupLabel_Values;\n\
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
	\tend\n\
	\tfor trialNumber = trialsList\n\
		\t\t% Get from and to index in from and to values for trialNumber\n\
		\t\tfromIndex = find(fromValues(:,1) == trialNumber);\n\
		\t\ttoIndex = find(toValues(:,1) == trialNumber);\n\
		\t\t% if both indices exist\n\
		\t\tif(~isempty(fromIndex) && ~isempty(toIndex))\n\
			\t\t\tfor markerNumber = 1:length(toIndex)\n\
				\t\t\t\t% Convert from and to in samples number\n\
				\t\t\t\tfromIndexMarker = floor(fromValues(fromIndex(1),2) * sampleFrequency) + 1;\n\
				\t\t\t\ttoIndexMarker = floor(toValues(toIndex(markerNumber),2) * sampleFrequency) + 1;\n\
				\t\t\t\tcurrentIndex = toIndexMarker - 1;\n\
				\t\t\t\tpreviousValue =  values(trialNumber, toIndexMarker);\n\
				\t\t\t\tfabFound = 0;\n\
				\t\t\t\twhile currentIndex >= fromIndexMarker\n\
					\t\t\t\t\tcurrentValue = values(trialNumber, currentIndex);\n\
					\t\t\t\t\tif currentValue >= amplitude && previousValue < amplitude\n\
						\t\t\t\t\t\tfabFound = 1;\n\
					\t\t\t\t\tend\n\
					\t\t\t\t\tif currentValue <= amplitude && previousValue > amplitude\n\
						\t\t\t\t\t\tfabFound = 1;\n\
					\t\t\t\t\tend\n\
					\t\t\t\t\tif fabFound == 1\n\
						\t\t\t\t\t\tdCurrent = abs(currentValue - amplitude);\n\
						\t\t\t\t\t\tdPrevious = abs(previousValue - amplitude);\n\
						\t\t\t\t\t\tif dCurrent > dPrevious\n\
							\t\t\t\t\t\t\tfabMarkersValues = [fabMarkersValues; trialNumber, currentIndex/sampleFrequency, previousValue];\n\
							\t\t\t\t\t\t\tbreak;\n\
						\t\t\t\t\t\telse\n\
							\t\t\t\t\t\t\tfabMarkersValues = [fabMarkersValues; trialNumber, (currentIndex-1)/sampleFrequency, currentValue];\n\
							\t\t\t\t\t\t\tbreak;\n\
						\t\t\t\t\t\tend\n\
					\t\t\t\t\tend\n\
					\t\t\t\t\tcurrentIndex = currentIndex - 1;\n\
					\t\t\t\t\tpreviousValue = currentValue;\n\
				\t\t\t\tend\n\
			\t\t\tend\n\
		\t\tend\n\
	\tend\n\
	\tinputSignal.MarkersGroup_markersGroupLabel_Values = fabMarkersValues;\n\
	\tif (~exist('createdOrModifiedChannels'))\n\
		\t\tcreatedOrModifiedChannels = {'inputSignal'};\n\
	\telse\n\
		\t\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
	\tend\n\
	\tclear markersGroupNumber compute currentIndex currentValue dCurrent dPrevious fabFound fabMarkersValues fromIndex\n\
	\tclear fromValues nbMarkersGroups previousValue sampleFrequency signalSizes toIndex toValues trialNumber values\n\
	\tclear markerNumber fromIndexMarker toIndexMarker\n\
else\n\
	\tif(exist('ErrorMessages'))\n\
		\t\tErrorMessages = [ErrorMessages, {[datestr(now,'HH:MM:SS.FFF'), ' - From Find Amplitude Backward function -> A Marker with this label already exists : markersGroupLabel (inputSignal)']}];\n\
	\telse\n\
		\t\tErrorMessages = {[datestr(now,'HH:MM:SS.FFF'), ' - From Find Amplitude Backward function -> A Marker with this label already exists : markersGroupLabel (inputSignal)']};\n\
	\tend\n\
end\n\