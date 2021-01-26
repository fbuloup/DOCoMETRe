MENU_TITLE = Mean
DESCRIPTION = Compute Signal mean
MENU_TITLE_FR = Moyenne
DESCRIPTION_FR = Calcule la moyenne

# Now start specific python function 
FUNCTION_CODE = # Compute MEAN feature\n\
import numpy;\n\
from datetime import datetime;\n\
compute = True;\n\
NbFeatures = 0;\n\
# Check if feature already exists\n\
if "inputSignal" + ".NbFeatures" in docometre.experiments.keys():\n\
	\tNbFeatures = int(docometre.experiments["inputSignal" + ".NbFeatures"]);\n\
	\tdocometre.gateway.jvm.System.out.println("NbFeatures : " + str(NbFeatures));\n\
	\tfor featureNumber in range(NbFeatures):\n\
		\t\tif docometre.experiments["inputSignal" + ".FeaturesLabels"][featureNumber] == "MEAN":\n\
			\t\t\tcompute = False;\n\
			\t\t\tbreak;\n\
docometre.gateway.jvm.System.out.println("compute : " + str(compute));\n\
if compute:\n\
	\tif NbFeatures == 0:\n\
		\t\tdocometre.experiments["inputSignal" + ".FeaturesLabels"] = ["MEAN"];\n\
	\telse:\n\
		\t\tdocometre.experiments["inputSignal" + ".FeaturesLabels"].append("MEAN");\n\
	\tdocometre.experiments["inputSignal" + ".NbFeatures"] = NbFeatures + 1;\n\
	\tdocometre.experiments["inputSignal" + ".Feature_MEAN_Values"] = numpy.mean(docometre.experiments["inputSignal" + ".Values"], axis=1);\n\
	\tif not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = "inputSignal";\n\
	\telse:\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":inputSignal";\n\
else:\n\
	\tif "ErrorMessages" in docometre.experiments.keys():\n\
		\t\tdocometre.experiments["ErrorMessages"] = docometre.experiments["ErrorMessages"] + "|" + datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Mean function -> A feature with this label already exists : MEAN";\n\
	\telse:\n\
		\t\tdocometre.experiments["ErrorMessages"] = datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Mean function -> A feature with this label already exists : MEAN";\n\
#del compute NbFeatures featureNumber;