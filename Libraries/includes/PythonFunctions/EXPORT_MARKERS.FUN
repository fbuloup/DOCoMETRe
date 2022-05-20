MENU_TITLE = Export Markers
DESCRIPTION = Export selected markers to text file type CSV
MENU_TITLE_FR = Export de marqueurs
DESCRIPTION_FR = Exporte les marqueurs sélectionnés dans un fichier texte type CSV

# Now start specific python function
FUNCTION_CODE = # Add categories infos to files (delete files if exist)\n\
import os;\n\
import numpy;\n\
with open('fullPathFileName', 'w', encoding='utf-8') as dataFile:\n\
	\tnumCategory = 1;\n\
	\twhile 'fullSubjectName' + '.Category' + str(numCategory) + '.Criteria' in docometre.experiments.keys():\n\
		\t\tcriteria = docometre.experiments['fullSubjectName' + '.Category' + str(numCategory) + '.Criteria'];\n\
		\t\ttrialsList = docometre.experiments['fullSubjectName' + '.Category' + str(numCategory) + '.TrialsList'];\n\
		\t\ttrialsListString = numpy.array2string(trialsList, separator='divider');\n\
		\t\ttrialsListString = trialsListString[1:];\n\
		\t\ttrialsListString = trialsListString[:-1];\n\
		\t\tdataFile.write(criteria + 'divider' + trialsListString);\n\
		\t\tdataFile.write('\\n');\n\
		\t\tnumCategory = numCategory + 1;\n\
# Append data\n\
markersToExport = cellExpression;\n\
# This cell expression must be replaced by something like :\n\
# ['e.s.sig.MarkersGroup_MarkerLabel1_Values', 'e.s.sig.MarkersGroup_MarkerLabel2_Values', ... ]\n\
nbMarkersToExport = len(markersToExport);\n\
for numMarker in range(0, nbMarkersToExport):\n\
	\tfullMarkerName = markersToExport[numMarker];\n\
    \tfullMarkerNameSplitted = fullMarkerName.split('_');\n\
    \tmarkerLabel = fullMarkerNameSplitted[len(fullMarkerNameSplitted) - 1];\n\
    \twith open('fullPathFileName', 'a', encoding='utf-8') as dataFile:\n\
		\t\tdataFile.write('MarkerLabel' + 'divider' + markerLabel);\n\
		\t\tdataFile.write('\\n');\n\
		\t\tnumpy.savetxt(dataFile, docometre.experiments[fullMarkerName + '_Values'], delimiter='divider');