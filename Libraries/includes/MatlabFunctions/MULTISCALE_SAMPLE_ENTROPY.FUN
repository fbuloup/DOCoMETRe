MENU_TITLE = Multiscale Sample Entropy
DESCRIPTION = Compute Multiscale  Sample Entropy 
MENU_TITLE_FR = Entropie d'échantillons multi-échelle
DESCRIPTION_FR = Calcule l'entropie d'échantillons multi-échelle
# Now start specific matlab function 

FUNCTION_CODE = % Compute MSE feature\n\
signalSizes = size(inputSignal.Values); % signalSizes(1) : trials number - signalSizes(2) : samples number\n\
compute = true;\n\
NbFeatures = 0;\n\
% Check if feature already exists\n\
if(isfield(inputSignal, 'NbFeatures'))\n\
	\tNbFeatures = inputSignal.NbFeatures\n\
	\tfor featureNumber = 1:inputSignal.NbFeatures\n\
		\t\tif(strcmp('MSE', inputSignal.FeaturesLabels{featureNumber}))\n\
			\t\t\tcompute = false;\n\
			\t\t\tbreak;\n\
		\t\tend\n\
	\tend\n\
end\n\
if(compute)\n\
	\tif(NbFeatures == 0)\n\
		\t\tinputSignal.FeaturesLabels = "MSE";\n\
	\telse\n\
		\t\tinputSignal.FeaturesLabels = [inputSignal.FeaturesLabels; "MSE"];\n\
	\tend\n\
	\tinputSignal.NbFeatures = NbFeatures + 1;\n\
	\tvalues = inputSignal.Values;\n\
	\t[rows, cols] = size(values);\n\
	\ttrialsList = [1:rows];\n\
	\tsampleFrequency = inputSignal.SampleFrequency;\n\
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
		\t\ttoValues(:,3) = values(:,1);\n\
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
			\t\t\tif(radiusDistanceThreshold == 0)\n\
			\t\t\t\tMobj = MSobject('entropyType', 'm', embeddingDimension, 'tau', timeDelay, 'Logx', logarithmBase);\n\
			\t\t\t\tmultiscaleSampleEntropy = MSEn(inputSignal.Values(trialNumber, fromIndex:toIndex), Mobj, 'Scales', scales, 'Methodx', 'grainingMethod', 'RadNew', radiusScalingMethod);\n\
			\t\t\telse\n\
			\t\t\t\tMobj = MSobject('entropyType', 'm', embeddingDimension, 'tau', timeDelay, 'r', radiusDistanceThreshold, 'Logx', logarithmBase);\n\
			\t\t\t\tmultiscaleSampleEntropy = MSEn(inputSignal.Values(trialNumber, fromIndex:toIndex), Mobj, 'Scales', scales, 'Methodx', 'grainingMethod', 'RadNew', radiusScalingMethod);\n\
			\t\t\tend\n\
			\t\t\tif(trialNumber == 1)\n\
			\t\t\t\tinputSignal.Feature_MSE_Values = multiscaleSampleEntropy;\n\
			\t\t\telse\n\
			\t\t\t\tinputSignal.Feature_MSE_Values(trialNumber, :) = multiscaleSampleEntropy;\n\
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
