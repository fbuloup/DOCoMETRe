MENU_TITLE = Markers difference
DESCRIPTION = Compute amplitude or time difference between two markers (M2-M1)
MENU_TITLE_FR = Différence de marqueurs
DESCRIPTION_FR = Calcule la différence temporelle ou d'amplitude entre deux marqueurs (M2-M1)

# Now start specific python function 
FUNCTION_CODE = # Compute Markers Difference\n\
import numpy;\n\
from datetime import datetime;\n\
signalSizes = docometre.experiments['inputSignal.Values'].shape; # signalSizes[0] : trials number - signalSizes[1] : samples number\n\
compute = True;\n\
NbFeatures = 0;\n\
# Check if feature already exists\n\
if "inputSignal.NbFeatures" in docometre.experiments.keys():\n\
	\tNbFeatures = int(docometre.experiments["inputSignal.NbFeatures"]);\n\
	\tfor featureNumber in range(NbFeatures):\n\
		\t\tif docometre.experiments["inputSignal.FeaturesLabels"][featureNumber] == "featureLabel":\n\
			\t\t\tcompute = False;\n\
			\t\t\tbreak;\n\
if compute:\n\
	\tif NbFeatures == 0:\n\
		\t\tdocometre.experiments["inputSignal.FeaturesLabels"] = ["featureLabel"];\n\
	\telse:\n\
		\t\tdocometre.experiments["inputSignal.FeaturesLabels"].append("featureLabel");\n\
	\tdocometre.experiments["inputSignal.NbFeatures"] = NbFeatures + 1;\n\
	\tdocometre.experiments["inputSignal.Feature_featureLabel_Values"] = numpy.zeros(signalSizes[0]);\n\
	\tfor trialNumber in numpy.r_[trialsList]:\n\
		\t\t# Get only indices from current trial\n\
		\t\tindices1 = numpy.where(docometre.experiments["inputMarker1_Values"][:,0] == (trialNumber+1))[0];\n\
		\t\tindices2 = numpy.where(docometre.experiments["inputMarker2_Values"][:,0] == (trialNumber+1))[0];\n\
		\t\t# if both indices exist\n\
		\t\tif(indices1.size > 0 and indices2.size > 0):\n\
			\t\t\t# Get time and amplitude values from current trial\n\
			\t\t\ttimeValues1 = docometre.experiments["inputMarker1_Values"][indices1,1];\n\
			\t\t\ttimeValues2 = docometre.experiments["inputMarker2_Values"][indices2,1];\n\
			\t\t\tampValues1 = docometre.experiments["inputMarker1_Values"][indices1,2];\n\
			\t\t\tampValues2 = docometre.experiments["inputMarker2_Values"][indices2,2];\n\
			\t\t\t# Compute differences\n\
			\t\t\tif "Amplitude" == "difference":\n\
				\t\t\t\tdocometre.experiments["inputSignal.Feature_featureLabel_Values"][trialNumber] = ampValues2 - ampValues1;\n\
			\t\t\telse:\n\
				\t\t\t\tdocometre.experiments["inputSignal.Feature_featureLabel_Values"][trialNumber] = timeValues2 - timeValues1;\n\
	\tif not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = "inputSignal";\n\
	\telse:\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":inputSignal";\n\
else:\n\
	\tif "ErrorMessages" in docometre.experiments.keys():\n\
		\t\tdocometre.experiments["ErrorMessages"] = docometre.experiments["ErrorMessages"] + "|" + datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Markers Difference function -> A feature with this label already exists : featureLabel";\n\
	\telse:\n\
		\t\tdocometre.experiments["ErrorMessages"] = datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Markers Difference function -> A feature with this label already exists : featureLabel (inputSignal)";\n\