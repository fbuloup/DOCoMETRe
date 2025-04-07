MENU_TITLE = Duplicate
DESCRIPTION = Create a copy of the input signal
MENU_TITLE_FR = Duplication
DESCRIPTION_FR = Créé une copie du signal d'entrée

# Now start specific python function 
FUNCTION_CODE = # Compute Duplicate\n\
import numpy;\n\
# Create Duplicate signal\n\
SignalSizes = docometre.experiments['inputSignal.Values'].shape; # SignalSizes[0] : trials number - SignalSizes[1] : samples number\n\
docometre.experiments['outputSignal.Values'] = numpy.zeros((SignalSizes[0], SignalSizes[1]));\n\
docometre.experiments['outputSignal.NbSamples'] = numpy.copy(docometre.experiments['inputSignal.NbSamples']);\n\
docometre.experiments['outputSignal.FrontCut'] = numpy.copy(docometre.experiments['inputSignal.FrontCut']);\n\
docometre.experiments['outputSignal.EndCut'] = numpy.copy(docometre.experiments['inputSignal.EndCut']);\n\
docometre.experiments['outputSignal.isSignal'] = 1;\n\
docometre.experiments['outputSignal.isCategory'] = 0;\n\
docometre.experiments['outputSignal.isEvent'] = 0;\n\
docometre.experiments['outputSignal.NbFeatures'] = 0;\n\
docometre.experiments['outputSignal.NbMarkersGroups'] = 0;\n\
SF = docometre.experiments['inputSignal.SampleFrequency'];\n\
docometre.experiments['outputSignal.SampleFrequency'] = SF;\n\
# Compute duplicate\n\
inputValues = docometre.experiments['inputSignal.Values'];\n\
outputValues = docometre.experiments['outputSignal.Values'];\n\
outputValues[numpy.r_[trialsList],:] = numpy.copy(inputValues[numpy.r_[trialsList],:]);\n\
if not('createdOrModifiedChannels' in docometre.experiments.keys()):\n\
\tdocometre.experiments['createdOrModifiedChannels'] = 'outputSignal';\n\
else:\n\
\tdocometre.experiments['createdOrModifiedChannels'] = docometre.experiments['createdOrModifiedChannels'] + ':outputSignal';\n\