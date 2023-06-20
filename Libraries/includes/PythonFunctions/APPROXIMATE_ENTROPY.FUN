MENU_TITLE = Sample Entropy
DESCRIPTION = Compute Signal Sample Entropy 
MENU_TITLE_FR = Entropie d'échantillons
DESCRIPTION_FR = Calcule l'entropie d'échantillons

# Now start specific python function 
FUNCTION_CODE = # Compute Sample Entropy feature\n\
import numpy;\n\
import EntropyHub;\n\
import math;\n\
from datetime import datetime;\n\
compute = True;\n\
NbFeatures = 0;\n\
# Check if feature already exists\n\
if "inputSignal.NbFeatures" in docometre.experiments.keys():\n\
	\tNbFeatures = int(docometre.experiments["inputSignal.NbFeatures"]);\n\
	\tfor featureNumber in range(NbFeatures):\n\
		\t\tif docometre.experiments["inputSignal.FeaturesLabels"][featureNumber] == "SE":\n\
			\t\t\tcompute = False;\n\
			\t\t\tbreak;\n\
if compute:\n\
	\tsignalSizes = docometre.experiments['inputSignal.Values'].shape;\n\
	\tnbTrials = signalSizes[0];\n\
	\tnbSamples = signalSizes[1];\n\
	\tsampleFrequency = float(docometre.experiments['inputSignal.SampleFrequency']);\n\
	\tif NbFeatures == 0:\n\
		\t\tdocometre.experiments["inputSignal.FeaturesLabels"] = ["SE"];\n\
	\telse:\n\
		\t\tdocometre.experiments["inputSignal.FeaturesLabels"].append("SE");\n\
	\tdocometre.experiments["inputSignal.NbFeatures"] = NbFeatures + 1;\n\
	\tvalues = docometre.experiments["inputSignal.Values"];\n\
	\tdocometre.experiments["inputSignal.Feature_SE_Values"] = numpy.zeros((0,3));\n\
	\t# Get from and to values\n\
	\tif "From_Beginning" == "inputMarker1":\n\
		\t\tfromValues = numpy.zeros((signalSizes[0],3));\n\
		\t\tfromValues[:,0] = numpy.r_[0:nbTrials] + 1;\n\
		\t\tfromValues[:,2] = values[:,0];\n\
	\telse:\n\
		\t\tif("inputMarker1".endswith(".FrontCut")):\n\
			\t\t\tfromValues = numpy.zeros((signalSizes[0],3));\n\
			\t\t\tfromValues[:,0] = numpy.r_[0:nbTrials] + 1;\n\
			\t\t\tfromValues[:,1] = numpy.true_divide(docometre.experiments["inputMarker1"], sampleFrequency);\n\
			\t\t\tfromValues[:,2] = values[:,0];\n\
		\t\telse:\n\
			\t\t\tfromValues = docometre.experiments["inputMarker1_Values"];\n\
	\tif "To_End" == "inputMarker2":\n\
		\t\ttoValues = numpy.zeros((signalSizes[0],3));\n\
		\t\ttoValues[:,0] = numpy.r_[0:nbTrials] + 1;\n\
		\t\ttoValues[:,1] = numpy.true_divide(nbSamples, sampleFrequency);\n\
		\t\ttoValues[:,2] = nbSamples;\n\
	\telse:\n\
		\t\tif("inputMarker2".endswith(".EndCut")):\n\
			\t\t\ttoValues = numpy.zeros((signalSizes[0],3));\n\
			\t\t\ttoValues[:,0] = numpy.r_[0:nbTrials] + 1;\n\
			\t\t\ttoValues[:,1] = numpy.true_divide(docometre.experiments["inputMarker2"], sampleFrequency);\n\
			\t\t\ttoValues[:,2] = values[:,0];\n\
		\t\telse:\n\
			\t\t\ttoValues = docometre.experiments["inputMarker2_Values"];\n\
	\tfor trialNumber in numpy.r_[0:nbTrials]:\n\
	\t\t# Get from and to index in from and to values for trialNumber\n\
		\t\tfromIndex = numpy.where(fromValues[:,0] == (trialNumber+1))[0];\n\
		\t\ttoIndex = numpy.where(toValues[:,0] == (trialNumber+1))[0];\n\
		\t\t# if both indices exist\n\
		\t\tif(fromIndex.size > 0 and toIndex.size > 0):\n\
			\t\t\t# Convert from and to in samples number\n\
			\t\t\tfromIndex = int(fromValues[fromIndex[0]][1] * sampleFrequency);\n\
			\t\t\ttoIndex = int(toValues[toIndex[0]][1] * sampleFrequency);\n\
			\t\t\ttrialValues = values[trialNumber, fromIndex:toIndex];\n\
			\t\t\tif(radiusDistanceThreshold == 0):\n\
			\t\t\t\tsampleEntropy, phi1, phi2 = EntropyHub.ApEn(trialValues, m = embeddingDimension, tau = timeDelay, Logx = logarithmBase);\n\
			\t\t\telse:\n\
			\t\t\t\tsampleEntropy, phi1, phi2 = EntropyHub.ApEn(trialValues, m = embeddingDimension, tau = timeDelay, r = radiusDistanceThreshold, Logx = logarithmBase);\n\
			\t\t\tif(trialNumber == 0):\n\
			\t\t\t\tdocometre.experiments["inputSignal.Feature_SE_Values"] = sampleEntropy;\n\
			\t\t\telse:\n\
			\t\t\t\tdocometre.experiments["inputSignal.Feature_SE_Values"] = numpy.vstack([docometre.experiments["inputSignal.Feature_SE_Values"], sampleEntropy]);\n\
	\tif not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = "inputSignal";\n\
	\telse:\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":inputSignal";\n\
else:\n\
	\tif "ErrorMessages" in docometre.experiments.keys():\n\
		\t\tdocometre.experiments["ErrorMessages"] = docometre.experiments["ErrorMessages"] + "|" + datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Sample Entropy function -> A feature with this label already exists : SE";\n\
	\telse:\n\
		\t\tdocometre.experiments["ErrorMessages"] = datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Sample Entropy function -> A feature with this label already exists : SE (inputSignal)";\n\
#del compute NbFeatures featureNumber;\n\