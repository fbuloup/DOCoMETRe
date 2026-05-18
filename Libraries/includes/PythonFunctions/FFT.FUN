MENU_TITLE = FFT
DESCRIPTION = Compute Single Sided FFT (real-valued signal) 
MENU_TITLE_FR = FFT
DESCRIPTION_FR = Calcule la FFT monolatérale (signal à valeurs réelles)

# Now start specific matlab function 
FUNCTION_CODE = # Compute FFT\n\
import numpy;\n\
# Create fft signal\n\
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
SF = float(SF);\n\
NbSamples = SignalSizes[1];\n\
newSF = NbSamples/SF;\n\
docometre.experiments['outputSignal.SampleFrequency'] = newSF;\n\
outputEndCut = docometre.experiments['outputSignal.EndCut'];\n\
if NbSamples % 2 == 0 :\n\
	\toutputEndCut[numpy.r_[trialsList]] = NbSamples/2 - 1 + 1;\n\
else:\n\
	\toutputEndCut[numpy.r_[trialsList]] = (NbSamples - 1)/2 + 1;\n\
inputValues = docometre.experiments['inputSignal.Values'];\n\
outputValues = docometre.experiments['outputSignal.Values'];\n\
outputValues[numpy.r_[trialsList],:] = numpy.absolute(numpy.fft.fft(inputValues[numpy.r_[trialsList],:]))/NbSamples;\n\
docometre.experiments['outputSignal.isFrequencyDomain'] = 1;\n\
if not('createdOrModifiedChannels' in docometre.experiments.keys()):\n\
\tdocometre.experiments['createdOrModifiedChannels'] = 'outputSignal';\n\
else:\n\
\tdocometre.experiments['createdOrModifiedChannels'] = docometre.experiments['createdOrModifiedChannels'] + ':outputSignal';\n\