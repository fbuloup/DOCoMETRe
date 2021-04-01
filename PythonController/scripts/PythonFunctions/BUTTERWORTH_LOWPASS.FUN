MENU_TITLE = Butterworth low pass filter
DESCRIPTION = As 'filtfilt' function is used, resulting order will be twice the provided one
MENU_TITLE_FR = Filtre passe bas de Butterworth
DESCRIPTION_FR = Comme la fonction 'filtfilt' est utilisée, l'ordre résultant sera le double de celui fourni

# Now start specific python function 
FUNCTION_CODE = # Compute low pass Butterworth filter\n\
import scipy;\n\
from scipy import signal;\n\
import numpy;\n\
# Create filtered signal\n\
SignalSizes = docometre.experiments['inputSignal.Values'].shape; # SignalSizes[0] : trials number - SignalSizes[1] : samples number\n\
docometre.experiments['outputSignal.Values'] = numpy.zeros((SignalSizes[0], SignalSizes[1]));\n\
docometre.experiments['outputSignal.NbSamples'] = docometre.experiments['inputSignal.NbSamples'];\n\
docometre.experiments['outputSignal.FrontCut'] = docometre.experiments['inputSignal.FrontCut'];\n\
docometre.experiments['outputSignal.EndCut'] = docometre.experiments['inputSignal.EndCut'];\n\
docometre.experiments['outputSignal.isSignal'] = 1;\n\
docometre.experiments['outputSignal.isCategory'] = 0;\n\
docometre.experiments['outputSignal.isEvent'] = 0;\n\
docometre.experiments['outputSignal.NbFeatures'] = 0;\n\
docometre.experiments['outputSignal.NbMarkersGroups'] = 0;\n\
docometre.experiments['outputSignal.SampleFrequency'] = docometre.experiments['inputSignal.SampleFrequency'];\n\
# Compute filtering\n\
nyquistFrequency = 0.5 * float(docometre.experiments['inputSignal.SampleFrequency']); # Nyquist frequency is half sample frequency\n\
docometre.gateway.jvm.System.out.println("nyquistFrequency : " + str(nyquistFrequency));\n\
normalizedCutOffFrequency = cutOffFrequency / nyquistFrequency;\n\
docometre.gateway.jvm.System.out.println("normalizedCutOffFrequency : " + str(normalizedCutOffFrequency));\n\
num, den = scipy.signal.butter(order, normalizedCutOffFrequency, 'low', analog = False);\n\
inputValues = docometre.experiments['inputSignal.Values'];\n\
outputValues = docometre.experiments['outputSignal.Values'];\n\
docometre.gateway.jvm.System.out.println("trialsList : " + str(range(trialsList)));\n\
for trialNumber in range(trialsList):\n\
\toutputValues[trialNumber][0:SignalSizes[1]] = scipy.signal.filtfilt(num, den, inputValues[trialNumber][0:SignalSizes[1]]);\n\
del num; del den; del nyquistFrequency; del normalizedCutOffFrequency; del SignalSizes; del trialNumber;\n\
#del inputValues; del outputValues;\n\
if not('createdOrModifiedChannels' in docometre.experiments.keys()):\n\
\tdocometre.experiments['createdOrModifiedChannels'] = 'outputSignal';\n\
else:\n\
\tdocometre.experiments['createdOrModifiedChannels'] = docometre.experiments['createdOrModifiedChannels'] + ':outputSignal';\n\