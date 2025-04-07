MENU_TITLE = Butterworth high pass filter
DESCRIPTION = As 'filtfilt' function is used, resulting order will be twice the provided one
MENU_TITLE_FR = Filtre passe haut de Butterworth
DESCRIPTION_FR = Comme la fonction 'filtfilt' est utilisée, l'ordre résultant sera le double de celui fourni

# Now start specific python function 
FUNCTION_CODE = # Compute high pass Butterworth filter\n\
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
nyquistFrequency = 0.5 * float(docometre.experiments['inputSignal.SampleFrequency']); # Nyquist frequency is half sample frequency\n\
normalizedCutOffFrequency = cutOffFrequency / nyquistFrequency;\n\
den, num = scipy.signal.butter(order, normalizedCutOffFrequency, 'high', analog = False);\n\
inputValues = docometre.experiments['inputSignal.Values'];\n\
outputValues = docometre.experiments['outputSignal.Values'];\n\
outputValues[numpy.r_[trialsList],:] = scipy.signal.filtfilt(den, num, inputValues[numpy.r_[trialsList],:]);\n\
if not('createdOrModifiedChannels' in docometre.experiments.keys()):\n\
\tdocometre.experiments['createdOrModifiedChannels'] = 'outputSignal';\n\
else:\n\
\tdocometre.experiments['createdOrModifiedChannels'] = docometre.experiments['createdOrModifiedChannels'] + ':outputSignal';\n\