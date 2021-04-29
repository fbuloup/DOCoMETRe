MENU_TITLE = Modify "Front Cut" from marker
DESCRIPTION = This function modify front cut value of selected signal from marker.
MENU_TITLE_FR = Modifier l'"Avant coupe" à partir d'un marqueur
DESCRIPTION_FR = Cette fonction modifie l'"Avant coupe" du signal sélectionné à partir d'un marqueur.

# Now start specific python function 
FUNCTION_CODE = # Compute Front Cut from marker\n\
import numpy;\n\
# Convert sample frequency to float\n\
sampleFrequency = float(docometre.experiments['inputSignal.SampleFrequency']);\n\
# Get from marker values\n\
fromValues = docometre.experiments["inputMarker_Values"];\n\
for trialNumber in numpy.r_[trialsList]:\n\
	\t# Get from and to index in from and to values for trialNumber\n\
	\tfromIndex = numpy.where(fromValues[:,0] == (trialNumber+1))[0];\n\
	\t# if fromIndex exists\n\
	\tif fromIndex.size > 0:\n\
		\t\t# Convert from in samples number\n\
		\t\tfromIndex = int(fromValues[fromIndex[0]][1] * sampleFrequency);\n\
		\t\t# Change signal front cut\n\
		\t\tdocometre.experiments['inputSignal.FrontCut'][trialNumber] = fromIndex;
if not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
	\tdocometre.experiments["createdOrModifiedChannels"] = "inputSignal";\n\
else:\n\
	\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":inputSignal";\n\
\n\