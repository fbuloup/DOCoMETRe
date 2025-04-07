MENU_TITLE = 1-D Interpolation 
DESCRIPTION = This function requires two points and interpolate input signal between these points using selected method
MENU_TITLE_FR = Interpolation 1-D
DESCRIPTION_FR = Cette fonction requiert deux points et interpole le signal d'entrée entre ces deux points selon la méthode choisie

# Now start specific matlab function 
FUNCTION_CODE = # Compute 1-D interpolation\n\
import scipy;\n\
import scipy.interpolate;\n\
# Create interpolated signal\n\
SignalSizes = docometre.experiments['inputSignal.Values'].shape; # SignalSizes[0] : trials number - SignalSizes[1] : samples number\n\
docometre.experiments['outputSignal.Values'] = numpy.copy(docometre.experiments['inputSignal.Values']);\n\
docometre.experiments['outputSignal.NbSamples'] = numpy.copy(docometre.experiments['inputSignal.NbSamples']);\n\
docometre.experiments['outputSignal.FrontCut'] = numpy.copy(docometre.experiments['inputSignal.FrontCut']);\n\
docometre.experiments['outputSignal.EndCut'] = numpy.copy(docometre.experiments['inputSignal.EndCut']);\n\
docometre.experiments['outputSignal.isSignal'] = 1;\n\
docometre.experiments['outputSignal.isCategory'] = 0;\n\
docometre.experiments['outputSignal.isEvent'] = 0;\n\
docometre.experiments['outputSignal.NbFeatures'] = 0;\n\
docometre.experiments['outputSignal.NbMarkersGroups'] = 0;\n\
docometre.experiments['outputSignal.SampleFrequency'] = numpy.copy(docometre.experiments['inputSignal.SampleFrequency']);\n\
# Compute\n\
indexMin = int(tMin*docometre.experiments['outputSignal.SampleFrequency']);\n\
indexMax = int(tMax*docometre.experiments['outputSignal.SampleFrequency']);\n\
xInputValues = [tMin, tMax];\n\
xOutputValues =range(indexMin,indexMax+1)/docometre.experiments['outputSignal.SampleFrequency'];\n\
for trialNumber in numpy.r_[trialsList]:\n\
	\tyInputValues = [docometre.experiments['outputSignal.Values'][trialNumber, indexMin], docometre.experiments['outputSignal.Values'][trialNumber, indexMax]];\n\
	\tnewValuesFunction = scipy.interpolate.interp1d(xInputValues, yInputValues, kind='linear');\n\
	\tdocometre.experiments['outputSignal.Values'][trialNumber, range(indexMin, indexMax + 1)] = newValuesFunction(xOutputValues);\n\
if not('createdOrModifiedChannels' in docometre.experiments.keys()):\n\
\tdocometre.experiments['createdOrModifiedChannels'] = 'outputSignal';\n\
else:\n\
\tdocometre.experiments['createdOrModifiedChannels'] = docometre.experiments['createdOrModifiedChannels'] + ':outputSignal';\n\