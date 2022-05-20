MENU_TITLE = Export Markers
DESCRIPTION = Export selected markers to text file type CSV
MENU_TITLE_FR = Export de marqueurs
DESCRIPTION_FR = Exporte les marqueurs sélectionnés dans un fichier texte type CSV

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
markersToExport = cellExpression;\n\
% This cell expression must be replaced by something like :\n\
% {'e.s.sig.MarkersGroup_MarkerLabel1_Values', 'e.s.sig.MarkersGroup_MarkerLabel2_Values', ... }\n\
nbMarkersToExport = length(markersToExport);\n\
for n = 1:nbMarkersToExport\n\
    \tfullMarkerName = markersToExport{n};\n\
    \tfullMarkerNameSplitted = split(fullMarkerName, '_');\n\
    \tmarkerLabel = fullMarkerNameSplitted{length(fullMarkerNameSplitted)};\n\
    \tdataFile = fopen('fullPathFileName', 'a', 'n', 'UTF-8');\n\
    \tfprintf(dataFile, ['MarkerLabel', 'divider', markerLabel]);\n\
    \tfprintf(dataFile, '\\n');\n\
    \tfclose(dataFile);\n\
    \tvalues = eval([fullMarkerName, '_Values']);\n\
    \tdlmwrite('fullPathFileName', values ,'delimiter', 'divider', '-append');\n\
end