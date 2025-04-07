MENU_TITLE = Export Features
DESCRIPTION = Export selected features to text file type CSV
MENU_TITLE_FR = Export de caractéristiques
DESCRIPTION_FR = Exporte les caractéristiques sélectionnées dans un fichier texte type CSV

# Now start specific matlab function 
FUNCTION_CODE = % Add categories infos to files (delete files if exist)\n\
dataFile = fopen('fullPathFileName', 'w', 'n', 'UTF-8');\n\
numCategory = 1;\n\
while isfield(fullSubjectName,['Category', int2str(numCategory)])\n\
    \t% Get Criteria\n\
    \tcriteria = eval(['fullSubjectName', '.Category', int2str(numCategory) ,'.Criteria']);\n\
    \t% Get Trials list\n\
    \ttrialsListArray = string(eval(['fullSubjectName', '.Category', int2str(numCategory) ,'.TrialsList']));\n\
    \ttrialsListString = convertStringsToChars(strjoin(trialsListArray, 'divider'));\n\
    \t% Write to file\n\
    \tfprintf(dataFile, [criteria, 'divider', trialsListString]);\n\
    \tfprintf(dataFile, '\\n');\n\
    \tnumCategory = numCategory + 1;\n\
end\n\
fclose(dataFile);\n\
% Append data\n\
featuresToExport = cellExpression;\n\
% This cell expression must be replaced by something like :\n\
% {'e.s.sig.Feature_FeatureLabel1_Values', 'e.s.sig.Feature_FeatureLabel2_Values', ... }\n\
nbFeaturesToExport = length(featuresToExport);\n\
for n = 1:nbFeaturesToExport\n\
    \tfullFeatureName = featuresToExport{n};\n\
    \tfullFeatureNameSplitted = split(fullFeatureName, '_');\n\
    \tfeatureLabel = fullFeatureNameSplitted{length(fullFeatureNameSplitted)};\n\
    \tdataFile = fopen('fullPathFileName', 'a', 'n', 'UTF-8');\n\
    \tfprintf(dataFile, ['FeatureLabel', 'divider', featureLabel]);\n\
    \tfprintf(dataFile, '\\n');\n\
    \tfclose(dataFile);\n\
    \tvalues = eval([fullFeatureName, '_Values']);\n\
    \tdlmwrite('fullPathFileName', values ,'delimiter', 'divider', '-append');\n\
end\n\
clear criteria dataFile fullFeatureName fullFeatureNameSplitted featureLabel;\n\
clear featuresToExport n nbFeaturesToExport numCategory trialsListArray;\n\
clear trialsListString values;