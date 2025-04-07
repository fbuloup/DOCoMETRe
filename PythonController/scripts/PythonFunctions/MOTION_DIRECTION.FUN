MENU_TITLE = Motion Direction
DESCRIPTION = This function computes the motion direction between two markers. This direction is computed\n\
for each point of the curve formed by the two input signals and with respect to the starting marker. 
DESCRIPTION_FR = Cette fonction calcule la direction du mouvement entre deux marqueurs. Cette direction est calculée\n\
pour chaque point de la courbe formée par les deux signaux d'entrée et par rapport au marqueur d'origine.  
# Now start specific python function 
FUNCTION_CODE = # Compute Motion Direction\n\
import math;\n\
import numpy;\n\
# Compute result angles in degree or radian\n\
resultInDegree = resultInDegreeValue\n\
# Get signals input values\n\
values1 = docometre.experiments['inputSignal1.Values'];\n\
values2 = docometre.experiments['inputSignal2.Values'];\n\
# Create output signal. It is assumed that input signals are same size, same samples number and same sample frequency\n\
SignalSizes = values1.shape;\n\
nbSamples = docometre.experiments['inputSignal1.NbSamples'];\n\
sampleFrequency = docometre.experiments['inputSignal1.SampleFrequency'];\n\
docometre.experiments['outputSignal.Values'] = numpy.zeros((SignalSizes[0], SignalSizes[1])); # SignalSizes[0] : trials number - SignalSizes[1] : samples number\n\
docometre.experiments['outputSignal.NbSamples'] = numpy.copy(docometre.experiments['inputSignal1.NbSamples']);\n\
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
		\t\ttoIndex = int(toValues[toIndex[0]][1] * sampleFrequency) + 1;\n\
		\t\t# Origine coordinates\n\
		\t\tx0 = values1[trialNumber,fromIndex];\n\
		\t\ty0 = values2[trialNumber,fromIndex];\n\
		\t\t# Init first output signal sample\n\
		\t\tdocometre.experiments["outputSignal.Values"][trialNumber, fromIndex] = 0;\n\
		\t\tfor index in range(fromIndex,toIndex):\n\
			\t\t\tdx = values1[trialNumber, index] - y0;\n\
			\t\t\tdy = values2[trialNumber, index] - y0;\n\
			\t\t\t# Compute angle\n\
			\t\t\ttheta =  numpy.arccos(dx/numpy.sqrt(dx**2 + dy**2));\n\
			\t\t\tif (dx<0) and (dy>0) : \n\
				\t\t\t\ttheta = -theta + math.pi/2;\n\
			\t\t\telif (dx<0) and (dy<0) : \n\
				\t\t\t\ttheta = -theta + math.pi/2;\n\
			\t\t\telif (dx>0) and (dy>0) : \n\
				\t\t\t\ttheta = math.pi/2 - theta;\n\
			\t\t\telif (dx>0) and (dy<0) : \n\
				\t\t\t\ttheta = math.pi/2 - theta;\n\
			\t\t\tif resultInDegree : \n\
				\t\t\t\ttheta = theta*180/math.pi;\n\
			\t\t\tdocometre.experiments['outputSignal.Values'][trialNumber, index] = theta;\n\
if not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
	\tdocometre.experiments["createdOrModifiedChannels"] = "outputSignal";\n\
else:\n\
	\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":outputSignal";\n\
