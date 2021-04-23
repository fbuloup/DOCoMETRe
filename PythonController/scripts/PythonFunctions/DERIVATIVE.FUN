MENU_TITLE = Derivative
DESCRIPTION = Compute derivative using numpy.diff 
MENU_TITLE_FR = Dérivée
DESCRIPTION_FR = Calcul de la dérivé en utilisant numpy.diff

# Now start specific python function 
FUNCTION_CODE = # Compute Derivative\n\
import numpy;\n\
# Create derivative signal\n\
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
SF = docometre.experiments['inputSignal.SampleFrequency'];\n\
docometre.experiments['outputSignal.SampleFrequency'] = SF;\n\
# Compute derivative\n\
SF = float(SF);\n\
inputValues = docometre.experiments['inputSignal.Values'];\n\
outputValues = docometre.experiments['outputSignal.Values'];\n\
outputValues[numpy.r_[trialsList],:] = numpy.diff(inputValues[numpy.r_[trialsList],:], prepend=0)*SF;\n\
if not('createdOrModifiedChannels' in docometre.experiments.keys()):\n\
\tdocometre.experiments['createdOrModifiedChannels'] = 'outputSignal';\n\
else:\n\
\tdocometre.experiments['createdOrModifiedChannels'] = docometre.experiments['createdOrModifiedChannels'] + ':outputSignal';\n\