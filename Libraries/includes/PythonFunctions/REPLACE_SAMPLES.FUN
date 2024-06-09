MENU_TITLE_FR = Remplacer les valeurs d'échantillons
DESCRIPTION_FR = Remplace les valeurs d'une série d'échantillons par la valeur d'un autre échantillon
MENU_TITLE = Replace samples values
DESCRIPTION = Replace the values ​​of a series of samples with the value of another sample

# Now start specific Python function 
FUNCTION_CODE = # Compute replace samples\n\
# Create output signal\n\
docometre.experiments['outputSignal.Values'] = numpy.copy(docometre.experiments['inputSignal.Values']);\n\
docometre.experiments['outputSignal.NbSamples'] = numpy.copy(docometre.experiments['inputSignal.NbSamples']);\n\
docometre.experiments['outputSignal.isSignal'] = 1;\n\
docometre.experiments['outputSignal.isCategory'] = 0;\n\
docometre.experiments['outputSignal.isEvent'] = 0;\n\
docometre.experiments['outputSignal.NbMarkersGroups'] = 0;\n\
docometre.experiments['outputSignal.NbFeatures'] = 0;\n\
docometre.experiments['outputSignal.SampleFrequency'] = numpy.copy(docometre.experiments['inputSignal.SampleFrequency']);\n\
docometre.experiments['outputSignal.FrontCut'] = numpy.copy(docometre.experiments['inputSignal.FrontCut']);\n\
docometre.experiments['outputSignal.EndCut'] = numpy.copy(docometre.experiments['inputSignal.EndCut']);\n\
# Loop over trials	\n\
values = docometre.experiments['inputSignal.Values'];\n\
for trialNumber in numpy.r_[trialsList]:\n\
	\treplacementValue = values[trialNumber , replacementIndex-1];\n\
	\tdocometre.experiments['outputSignal.Values'][trialNumber, fromIndex-1:toIndex-1] = replacementValue;\n\
if not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
	\tdocometre.experiments["createdOrModifiedChannels"] = "outputSignal";\n\
else:\n\
	\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":outputSignal";\n\