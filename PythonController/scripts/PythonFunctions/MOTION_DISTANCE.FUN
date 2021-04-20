MENU_TITLE = Motion Distance
DESCRIPTION = Compute motion distance
MENU_TITLE_FR = Distance de mouvement
DESCRIPTION_FR = Calcule la distance du mouvement

# Now start specific python function 
FUNCTION_CODE = # Compute Motion Distance\n\
import math;\n\
import numpy;\n\
# Get signals input values\n\
values1 = docometre.experiments['inputSignal1.Values'];\n\
values2 = docometre.experiments['inputSignal2.Values'];\n\
# Create output signal. It is assumed that input signals are same size, same samples number and same sample frequency\n\
SignalSizes = values1.shape;\n\
nbSamples = docometre.experiments['inputSignal1.NbSamples'];\n\
sampleFrequency = docometre.experiments['inputSignal1.SampleFrequency'];\n\
docometre.experiments['outputSignal.Values'] = numpy.zeros((SignalSizes[0], SignalSizes[1])); # SignalSizes[0] : trials number - SignalSizes[1] : samples number\n\
docometre.experiments['outputSignal.NbSamples'] = docometre.experiments['inputSignal1.NbSamples'];\n\
docometre.experiments['outputSignal.FrontCut'] = numpy.zeros(SignalSizes[0]);\n\
docometre.experiments['outputSignal.EndCut'] = nbSamples + 1;\n\
docometre.experiments['outputSignal.isSignal'] = 1;\n\
docometre.experiments['outputSignal.isCategory'] = 0;\n\
docometre.experiments['outputSignal.isEvent'] = 0;\n\
docometre.experiments['outputSignal.NbFeatures'] = 0;\n\
docometre.experiments['outputSignal.NbMarkersGroups'] = 0;\n\
docometre.experiments['outputSignal.SampleFrequency'] = sampleFrequency;\n\
# Convert sample frequency to float\n\
sampleFrequency = float(sampleFrequency);\n\
# Get from and to values\n\
if "From_Beginning" == "inputMarker1":\n\
	\tfromValues = numpy.zeros((SignalSizes[0],3));\n\
	\tfromValues[:,0] = numpy.r_[trialsList] + 1;\n\
	\tfromValues[:,2] = values1[:,0];\n\
else:\n\
	\tfromValues = docometre.experiments["inputMarker1_Values"];\n\
if "To_End" == "inputMarker2":\n\
	\ttoValues = numpy.zeros((SignalSizes[0],3));\n\
	\ttoValues[:,0] = numpy.r_[trialsList] + 1;\n\
	\ttoValues[:,1] = numpy.true_divide(nbSamples, sampleFrequency);\n\
	\ttoValues[:,2] = nbSamples;\n\
else:\n\
	\ttoValues = docometre.experiments["inputMarker2_Values"];\n\
# Loop over trials	\n\
for trialNumber in numpy.r_[trialsList]:\n\
	\t# Get from and to index in from and to values for trialNumber\n\
	\tfromIndex = numpy.where(fromValues[:,0] == (trialNumber+1))[0];\n\
	\ttoIndex = numpy.where(toValues[:,0] == (trialNumber+1))[0];\n\
	\t# if both indices exist\n\
	\tif(fromIndex.size > 0 and toIndex.size > 0):\n\
		\t\t# Convert from and to in samples number\n\
		\t\tfromIndex = int(fromValues[fromIndex[0]][1] * sampleFrequency);\n\
		\t\ttoIndex = int(toValues[toIndex[0]][1] * sampleFrequency);\n\
		\t\t# Init first output signal sample\n\
		\t\tdocometre.experiments["outputSignal.Values"][trialNumber, fromIndex] = 0;\n\
		\t\t# Compute deltas\n\
		\t\tdx2 = (numpy.diff(values1[trialNumber, fromIndex:toIndex]))**2;\n\
		\t\tdy2 = (numpy.diff(values2[trialNumber, fromIndex:toIndex]))**2;\n\
		\t\t# Square roots\n\
		\t\tdocometre.experiments["outputSignal.Values"][trialNumber, fromIndex+1:toIndex]= numpy.sqrt(dx2 + dy2);\n\
 	    \t\t# Sums            \n\
		\t\tdocometre.experiments["outputSignal.Values"][trialNumber, fromIndex:toIndex] = numpy.cumsum(docometre.experiments["outputSignal.Values"][trialNumber, fromIndex:toIndex]);\n\
if not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
	\tdocometre.experiments["createdOrModifiedChannels"] = "outputSignal";\n\
else:\n\
	\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":outputSignal";\n\
