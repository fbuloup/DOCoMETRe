MENU_TITLE = Minimum
DESCRIPTION = Mark minimum amplitude
MENU_TITLE_FR = Minimum
DESCRIPTION_FR = Marque l'amplitude minimum

# Now start specific python function 
FUNCTION_CODE = # Compute Minimum marker\n\
import numpy;\n\
from datetime import datetime;\n\
signalSizes = docometre.experiments['inputSignal.Values'].shape;\n\
nbSamples = docometre.experiments['inputSignal.NbSamples'];\n\
compute = True;\n\
NbMarkersGroups = 0;\n\
# Check if markers Group already exists\n\
if "inputSignal.NbMarkersGroups" in docometre.experiments.keys():\n\
	\tnbMarkersGroups = int(docometre.experiments["inputSignal.NbMarkersGroups"]);\n\
	\tfor markersGroupNumber in range(nbMarkersGroups):\n\
		\t\tif docometre.experiments["inputSignal.MarkersGroupsLabels"][markersGroupNumber] == "markersGroupLabel":\n\
			\t\t\tcompute = False;\n\
			\t\t\tbreak;\n\
if compute:\n\
	\t# Get signal input values\n\
	\tvalues = docometre.experiments["inputSignal.Values"];\n\
	\t# Create new output markers group\n\
	\tnbMarkersGroups = int(nbMarkersGroups) + 1;\n\
	\tdocometre.experiments["inputSignal.NbMarkersGroups"] = str(nbMarkersGroups);\n\
	\tif nbMarkersGroups == 1 :\n\
		\t\texpression = docometre.experiments["inputSignal.MarkersGroupsLabels"] = ["markersGroupLabel"];\n\
	\telse:\n\
		 \t\texpression = docometre.experiments["inputSignal.MarkersGroupsLabels"].append("markersGroupLabel");\n\
	\tdocometre.experiments["inputSignal.MarkersGroup_markersGroupLabel_Values"] = numpy.zeros((0,3));\n\
	\tminMarkersValues = docometre.experiments["inputSignal.MarkersGroup_markersGroupLabel_Values"];\n\
	\t# Convert sample frequency to float\n\
	\tsampleFrequency = float(docometre.experiments['inputSignal.SampleFrequency']);\n\
	\t# Get from and to values\n\
	\tif "From_Beginning" == "inputMarker1":\n\
		\t\tfromValues = numpy.zeros((signalSizes[0],3));\n\
		\t\tfromValues[:,0] = numpy.r_[trialsList] + 1;\n\
		\t\tfromValues[:,2] = values[:,0];\n\
	\telse:\n\
		\t\tif("inputMarker1".endswith(".FrontCut")):\n\
			\t\t\tfromValues = numpy.zeros((signalSizes[0],3));\n\
			\t\t\tfromValues[:,0] = numpy.r_[trialsList] + 1;\n\
			\t\t\tfromValues[:,1] = numpy.true_divide(docometre.experiments["inputMarker1"], sampleFrequency);\n\
			\t\t\tfromValues[:,2] = values[:,0];\n\
		\t\telse:\n\
			\t\t\tfromValues = docometre.experiments["inputMarker1_Values"];\n\
	\tif "To_End" == "inputMarker2":\n\
		\t\ttoValues = numpy.zeros((signalSizes[0],3));\n\
		\t\ttoValues[:,0] = numpy.r_[trialsList] + 1;\n\
		\t\ttoValues[:,1] = numpy.true_divide(nbSamples, sampleFrequency);\n\
		\t\ttoValues[:,2] = nbSamples;\n\
	\telse:\n\
		\t\tif("inputMarker2".endswith(".EndCut")):\n\
			\t\t\ttoValues = numpy.zeros((signalSizes[0],3));\n\
			\t\t\ttoValues[:,0] = numpy.r_[trialsList] + 1;\n\
			\t\t\ttoValues[:,1] = numpy.true_divide(docometre.experiments["inputMarker2"], sampleFrequency);\n\
			\t\t\ttoValues[:,2] = values[:,0];\n\
		\t\telse:\n\
			\t\t\ttoValues = docometre.experiments["inputMarker2_Values"];\n\
	\tfor trialNumber in numpy.r_[trialsList]:\n\
		\t\t# Get from and to index in from and to values for trialNumber\n\
		\t\tfromIndex = numpy.where(fromValues[:,0] == (trialNumber+1))[0];\n\
		\t\ttoIndex = numpy.where(toValues[:,0] == (trialNumber+1))[0];\n\
		\t\t# if both indices exist\n\
		\t\tif(fromIndex.size > 0 and toIndex.size > 0):\n\
			\t\t\t# Convert from and to in samples number\n\
			\t\t\tfromIndex = int(fromValues[fromIndex[0]][1] * sampleFrequency);\n\
			\t\t\ttoIndex = int(toValues[toIndex[0]][1] * sampleFrequency);\n\
			\t\t\t# Compute min between from and to index\n\
			\t\t\tminValue = values[trialNumber, fromIndex:toIndex].min(axis=0);\n\
			\t\t\tminIndex = values[trialNumber, fromIndex:toIndex].argmin(axis=0);\n\
			\t\t\tminMarkerTime = (fromIndex + minIndex)/sampleFrequency;\n\
			\t\t\tminMarkersValues = numpy.append(minMarkersValues, [[trialNumber+1, minMarkerTime, minValue]], axis = 0);\n\
	\tdocometre.experiments["inputSignal.MarkersGroup_markersGroupLabel_Values"] = minMarkersValues;\n\
	\tif not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = "inputSignal";\n\
	\telse:\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":inputSignal";\n\
else:\n\
	\tif "ErrorMessages" in docometre.experiments.keys():\n\
		\t\tdocometre.experiments["ErrorMessages"] = docometre.experiments["ErrorMessages"] + "|" + datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Minimum function -> A Marker with this label already exists : markersGroupLabel";\n\
	\telse:\n\
		\t\tdocometre.experiments["ErrorMessages"] = datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Minimum function -> A Marker with this label already exists : markersGroupLabel (inputSignal)";\n\