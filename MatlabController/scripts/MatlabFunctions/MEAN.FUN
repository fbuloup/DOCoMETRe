MENU_TITLE = Mean
DESCRIPTION = Compute Signal mean
MENU_TITLE_FR = Moyenne
DESCRIPTION_FR = Calcule la moyenne
# Now start specific matlab function 

#%Enter your comments here
#function Butterworth_lowpass (TrialsList , signalsNamesList, markersNamesList, fieldsNamesList, signalsNamesSuffix, newMarkersNamesList, newFieldsNamesList, signalsModifiedNumber, Cutoff, Order)
#%beginAnalyseHeader
#%SignalsUsedNumber = 1
#%MarkersUsedNumber = 0
#%FieldsUsedNumber = 0
#%SignalsCreatedNumber = 1
#%MarkersCreatedNumber = 0
#%fieldsCreatedNumber = 0
#%signalsModifiedNumber = 0
#%param1MatlabType = float
#%param1Component = 1
#%param1Label = Cutoff
#%param1ToolTip = Cutoff frequency
#%param1RegExp = (^[+]?\d*\.?\d*[1-9]+\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\d*\.\d*([eE][-+]?[0-9]+)?$)
#%param1AvailableListValue = 
#%param1DefaultValue = 
#%param1SignalsAvailable = false
#%param1MarkersAvailable = false
#%param1FieldsAvailable = false
#%param2MatlabType = integer
#%param2Component = 1
#%param2Label = Order
#%param2ToolTip = Filter order
#%param2RegExp = ^[+]?0*[1-9]+[0-9]*$
#%param2AvailableListValue = 
#%param2DefaultValue = 
#%param2SignalsAvailable = false
#%param2MarkersAvailable = false
#%param2FieldsAvailable = false
#%endAnalyseHeader


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
\tinputSignal.Feature_MEAN_Values = mean(inputSignal.Values,2);\n\
\tif (~exist('createdOrModifiedChannels'))\n\
\t\tcreatedOrModifiedChannels = {'inputSignal'};\n\
\telse\n\
\t\tcreatedOrModifiedChannels = [createdOrModifiedChannels; 'inputSignal'];\n\
\tend\n\
else\n\
\tif(exist('ErrorMessages'))\n\
\t\tErrorMessages = [ErrorMessages, {[datestr(now,'HH:MM:SS.FFF'), ' - From Mean function -> A feature with this label already exists : MEAN']}];\n\
\telse\n\
\t\tErrorMessages = {[datestr(now,'HH:MM:SS.FFF'), 'From Mean function -> A feature with this label already exists : MEAN']};\n\
\tend\n\
end\n\
clear compute NbFeatures featureNumber;\n\
