MENU_TITLE = Mean
DESCRIPTION = Compute Signal mean on a provided interval
MENU_TITLE_FR = Moyenne
DESCRIPTION_FR = Calcule la moyenne sur un intervalle donné

# Now start specific python function 
FUNCTION_CODE = # Compute MEAN feature\n\
import numpy;\n\
from datetime import datetime;\n\
compute = True;\n\
NbFeatures = 0;\n\
# Check if feature already exists\n\
if "inputSignal.NbFeatures" in docometre.experiments.keys():\n\
	\tNbFeatures = int(docometre.experiments["inputSignal.NbFeatures"]);\n\
	\tfor featureNumber in range(NbFeatures):\n\
		\t\tif docometre.experiments["inputSignal.FeaturesLabels"][featureNumber] == "MEAN":\n\
			\t\t\tcompute = False;\n\
			\t\t\tbreak;\n\
if compute:\n\
	\tif NbFeatures == 0:\n\
		\t\tdocometre.experiments["inputSignal.FeaturesLabels"] = ["MEAN"];\n\
	\telse:\n\
		\t\tdocometre.experiments["inputSignal.FeaturesLabels"].append("MEAN");\n\
	\tdocometre.experiments["inputSignal.NbFeatures"] = NbFeatures + 1;\n\
	\tvalues = docometre.experiments["inputSignal.Values"];\n\
	\tfromValues = numpy.zeros(values.shape[0]); # Default values when from beginning\n\
	\tif "From_Beginning" != "fromInputSignal" : fromValues = docometre.experiments["fromInputSignal"];\n\
	\ttoValues = values.shape[1]*numpy.ones(values.shape[0]); # Default values when to end\n\
	\tif "To_End" != "toInputSignal" : toValues = docometre.experiments["toInputSignal"];\n\
	\tcolumns = numpy.arange(values.shape[1]).reshape(-1,1);\n\
	\tmask = (fromValues <= columns) & (columns < toValues);\n\
	\tdocometre.experiments["inputSignal.Feature_MEAN_Values"] = numpy.nansum(values.T*mask, axis = 0)/mask.sum(axis = 0);\n\
	\tif not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = "inputSignal";\n\
	\telse:\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":inputSignal";\n\
else:\n\
	\tif "ErrorMessages" in docometre.experiments.keys():\n\
		\t\tdocometre.experiments["ErrorMessages"] = docometre.experiments["ErrorMessages"] + "|" + datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Mean function -> A feature with this label already exists : MEAN";\n\
	\telse:\n\
		\t\tdocometre.experiments["ErrorMessages"] = datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Mean function -> A feature with this label already exists : MEAN (inputSignal)";\n\
#del compute NbFeatures featureNumber;\n\