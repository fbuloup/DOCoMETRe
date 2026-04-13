MENU_TITLE = 1D Median filter (medfilt1)
DESCRIPTION = 1D Median filter (use medfilt1 function)
MENU_TITLE_FR = Filtre médian 1D (medfilt1)
DESCRIPTION_FR = Filtre médian 1D (utilise la fonction medfilt1)

# Now start specific python function 
FUNCTION_CODE = # Compute median filter\n\
import scipy;\n\
from scipy import signal;\n\
import numpy;\n\
# Create filtered signal\n\
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
docometre.experiments['outputSignal.SampleFrequency'] = numpy.copy(docometre.experiments['inputSignal.SampleFrequency']);\n\
# Compute filtering\n\
inputValues = docometre.experiments['inputSignal.Values'];\n\
outputValues = docometre.experiments['outputSignal.Values'];\n\
outputValues[numpy.r_[trialsList],:] = scipy.signal.medfilt(inputValues[numpy.r_[trialsList],:], order);\n\
if not('createdOrModifiedChannels' in docometre.experiments.keys()):\n\
\tdocometre.experiments['createdOrModifiedChannels'] = 'outputSignal';\n\
else:\n\
\tdocometre.experiments['createdOrModifiedChannels'] = docometre.experiments['createdOrModifiedChannels'] + ':outputSignal';\n\