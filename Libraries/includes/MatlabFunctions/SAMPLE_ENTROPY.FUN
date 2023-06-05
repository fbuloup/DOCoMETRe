MENU_TITLE = Sample Entropy
DESCRIPTION = Compute Signal Sample Entropy 
MENU_TITLE_FR = Entropie d'échantillons
DESCRIPTION_FR = Calcule l'entropie d'échantillons
# Now start specific matlab function 

FUNCTION_CODE = % Compute SE feature\n\
compute = true;\n\
NbFeatures = 0;\n\
% Check if feature already exists\n\
if(isfield(inputSignal, 'NbFeatures'))\n\
	\tNbFeatures = inputSignal.NbFeatures\n\
	\tfor featureNumber = 1:inputSignal.NbFeatures\n\
		\t\tif(strcmp('SE', inputSignal.FeaturesLabels{featureNumber}))\n\
			\t\t\tcompute = false;\n\
			\t\t\tbreak;\n\
		\t\tend\n\
	\tend\n\
end\n\
if(compute)\n\
	\tif(NbFeatures == 0)\n\
		\t\tinputSignal.FeaturesLabels = "SE";\n\
	\telse\n\
		\t\tinputSignal.FeaturesLabels = [inputSignal.FeaturesLabels; "SE"];\n\
	\tend\n\
	\tinputSignal.NbFeatures = NbFeatures + 1;\n\
	\t[rows, cols] = size(inputSignal.Values);\n\
	\tfromValues = zeros(rows,cols); % Default values when from beginning\n\
	\tif("From_Beginning" ~= "fromInputSignal")\n\
		\t\tfromValues = inputSignal.FrontCut';\n\
		\t\tfromValues = repelem(fromValues,1,cols);\n\
	\tend\n\
	\ttoValues = cols*ones(rows,cols); % Default values when to end\n\
	\tif("To_End" ~= "toInputSignal")\n\
		\t\ttoValues = inputSignal.EndCut';\n\
		\t\ttoValues = repelem(toValues,1,cols);\n\
	\tend\n\
	\t%columns = 1:cols;\n\
	\t%columns = repelem(columns,rows,1);\n\
	\t%mask = (fromValues <= columns) & (columns <= toValues);\n\
	\t%inputSignal.Feature_MEAN_Values = sum(inputSignal.Values.*mask,2)./sum(mask, 2);\n\
	\tfor trialNumber = [1:rows]\n\
		\t\t% Get from and to index in from and to values for trialNumber\n\
		\t\tfromIndex = find(fromValues(:,1) == trialNumber);\n\
		\t\ttoIndex = find(toValues(:,1) == trialNumber);\n\
		\t\t% if both indices exist\n\
		\t\tif(~isempty(fromIndex) && ~isempty(toIndex))\n\
			\t\t\t% Convert from and to in samples number\n\
			\t\t\tfromIndex = floor(fromValues(fromIndex(1),2) * sampleFrequency) + 1;\n\
			\t\t\ttoIndex = floor(toValues(toIndex(1),2) * sampleFrequency);\n\
			\t\t\t% Compute max between from and to index\n\
			
			sampleEntropy = SampEn()
			
			\t\t\t[peakValues maxIndices] = findpeaks(values(trialNumber, fromIndex:toIndex), height, threshold, distance, prominence);\n\
			\t\t\tfor n = 1:length(maxIndices)\n\
				\t\t\t\tpeakValue = peakValues(n);\n\
				\t\t\t\tpeakMarkerTime = (fromIndex + maxIndices(n) - 2)/sampleFrequency;\n\
				\t\t\t\tfpMarkersValues = [fpMarkersValues; trialNumber, peakMarkerTime, peakValue];\n\
			\t\t\tend\n\
		\t\tend\n\
	\tend\n\
	\tif (~exist('createdOrModifiedChannels'))\n\
		\t\tcreatedOrModifiedChannels = {'inputSignal'};\n\
	\telse\n\
		\t\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
	\tend\n\
else\n\
	\tif(exist('ErrorMessages'))\n\
	\t\tErrorMessages = [ErrorMessages, {[datestr(now,'HH:MM:SS.FFF'), ' - From Sample Entropy function -> A feature with this label already exists : SE']}];\n\
	\telse\n\
	\t\tErrorMessages = {[datestr(now,'HH:MM:SS.FFF'), 'From Sample Entropy function -> A feature with this label already exists : SE']};\n\
	\tend\n\
end\n\
clear compute NbFeatures featureNumber rows cols values fromValues toValues columns mask;\n\
