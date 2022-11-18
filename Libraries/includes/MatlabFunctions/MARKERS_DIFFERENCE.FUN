MENU_TITLE = Markers difference
DESCRIPTION = Compute amplitude or time difference between two markers (M2-M1)
MENU_TITLE_FR = Différence de marqueurs
DESCRIPTION_FR = Calcule la différence temporelle ou d'amplitude entre deux marqueurs (M2-M1)

# Now start specific matlab function 
FUNCTION_CODE = % Compute Markers difference\n\
signalSizes = size(inputSignal.Values); % signalSizes(1) : trials number - signalSizes(2) : samples number\n\
% Values will be stored in a feature attached to the first marker\n\
compute = true;\n\
nbFeatures = 0;\n\
% Check if feature already exists\n\
if isfield(inputSignal, 'NbFeatures')\n\
	\tnbFeatures = inputSignal.NbFeatures;\n\
	\tfor featureNumber = 1:nbFeatures\n\
		\t\tif inputSignal.FeaturesLabels{featureNumber} == "featureLabel"\n\
			\t\t\tcompute = false;\n\
			\t\t\tbreak;\n\
		\t\tend\n\
	\tend\n\
end\n\
if compute\n\
	\t% Create new feature\n\
	\tnbFeatures = nbFeatures + 1;\n\
	\tinputSignal.NbFeatures = nbFeatures;\n\
	\tif nbFeatures == 1\n\
		\t\tinputSignal.FeaturesLabels = "featureLabel";\n\
	\telse\n\
		\t\tinputSignal.FeaturesLabels = [inputSignal.FeaturesLabels; "featureLabel"];\n\
	\tend\n\
	\tinputSignal.Feature_featureLabel_Values = zeros(signalSizes(1), 1);\n\
	\tfor trialNumber = trialsList\n\
		\t\t%Get only indices from current trial\n\
		\t\tindices1 = find(inputMarker1_Values(:,1) == trialNumber);\n\
		\t\tindices2 = find(inputMarker2_Values(:,1) == trialNumber);\n\
		\t\tif(~isempty(indices1) && ~isempty(indices2))\n\
			\t\t\t% Get time and amplitude values from current trial\n\
			\t\t\ttimeValues1 = inputMarker1_Values(indices1,2);\n\
			\t\t\ttimeValues2 = inputMarker2_Values(indices2,2);\n\
			\t\t\tampValues1 = inputMarker1_Values(indices1,3);\n\
			\t\t\tampValues2 = inputMarker2_Values(indices2,3);\n\
			\t\t\t% Compute differences\n\
			\t\t\tif(strcmp("Amplitude", "difference"))\n\
				\t\t\t\tinputSignal.Feature_featureLabel_Values(trialNumber) = ampValues2 - ampValues1;\n\
			\t\t\telse\n\
				\t\t\t\tinputSignal.Feature_featureLabel_Values(trialNumber) = timeValues2 - timeValues1;\n\
			\t\t\tend\n\
		\t\tend\n\
	\tend\n\
end\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'inputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
end\n\
clear signalSizes ampValues1 ampValues2 timeValues1 timeValues2 trialNumber;\n\
clear nbFeatures indices1 indices2 featureNumber compute;

