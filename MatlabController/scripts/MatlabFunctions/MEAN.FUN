MENU_TITLE = Mean
DESCRIPTION = Compute Signal mean on a provided interval
MENU_TITLE_FR = Moyenne
DESCRIPTION_FR = Calcule la moyenne sur un intervalle donné
# Now start specific matlab function 

FUNCTION_CODE = % Compute MEAN feature\n\
compute = true;\n\
NbFeatures = 0;\n\
% Check if feature already exists\n\
if(isfield(inputSignal, 'NbFeatures'))\n\
\tNbFeatures = inputSignal.NbFeatures\n\
\tfor featureNumber = 1:inputSignal.NbFeatures\n\
\t\tif(strcmp('MEAN', inputSignal.FeaturesLabels{featureNumber}))\n\
\t\t\tcompute = false;\n\
\t\t\tbreak;\n\
\t\tend\n\
\tend\n\
end\n\
if(compute)\n\
\tif(NbFeatures == 0)\n\
\t\tinputSignal.FeaturesLabels = {'MEAN'};\n\
\telse\n\
\t\tinputSignal.FeaturesLabels = [inputSignal.FeaturesLabels, {'MEAN'}];\n\
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
\tcolumns = 1:cols;\n\
\tcolumns = repelem(columns,rows,1);\n\
\tmask = (fromValues <= columns) & (columns < toValues);\n\
\tinputSignal.Feature_MEAN_Values = sum(inputSignal.Values,2)./sum(mask, 2);\n\
\tif (~exist('createdOrModifiedChannels'))\n\
\t\tcreatedOrModifiedChannels = {'inputSignal'};\n\
\telse\n\
\t\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
\tend\n\
else\n\
\tif(exist('ErrorMessages'))\n\
\t\tErrorMessages = [ErrorMessages, {[datestr(now,'HH:MM:SS.FFF'), ' - From Mean function -> A feature with this label already exists : MEAN']}];\n\
\telse\n\
\t\tErrorMessages = {[datestr(now,'HH:MM:SS.FFF'), 'From Mean function -> A feature with this label already exists : MEAN']};\n\
\tend\n\
end\n\
clear compute NbFeatures featureNumber rows cols values fromValues toValues columns mask;\n\
