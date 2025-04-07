MENU_TITLE = Export Features
DESCRIPTION = Export selected features to text file type CSV
MENU_TITLE_FR = Export de caractéristiques
DESCRIPTION_FR = Exporte les caractéristiques sélectionnées dans un fichier texte type CSV

# Now start specific python function
FUNCTION_CODE = # Add categories infos to files (delete files if exist)\n\
import os;\n\
import numpy;\n\
with open(r"fullPathFileName", 'w', encoding='utf-8') as dataFile:\n\
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
featuresToExport = cellExpression;\n\
# This cell expression must be replaced by something like :\n\
# ['e.s.sig.Feature_FeatureLabel1_Values', 'e.s.sig.Feature_FeatureLabel2_Values', ... ]\n\
nbFeaturesToExport = len(featuresToExport);\n\
for numFeature in range(0, nbFeaturesToExport):\n\
	\tfullFeatureName = featuresToExport[numFeature];\n\
    \tfullFeatureNameSplitted = fullFeatureName.split('_');\n\
    \tfeatureLabel = fullFeatureNameSplitted[len(fullFeatureNameSplitted) - 1];\n\
    \twith open(r"fullPathFileName", 'a', encoding='utf-8') as dataFile:\n\
		\t\tdataFile.write('FeatureLabel' + 'divider' + featureLabel);\n\
		\t\tdataFile.write('\\n');\n\
		\t\tnumpy.savetxt(dataFile, docometre.experiments[fullFeatureName + '_Values'], delimiter='divider');