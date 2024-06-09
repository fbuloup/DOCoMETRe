MENU_TITLE = Markers Group Transfert
DESCRIPTION = This fonction creates markers group on input signal.\n\ 
Markers time values come from input markers group.
MENU_TITLE_FR = Transférer un groupe de marqueurs
DESCRIPTION_FR = Cette fonction crée un groupe de marqueur sur le signal d'entrée.\n\
Les valeurs temporelles proviennent du groupe de marqueurs d'entrée.

# Now start specific python function 
FUNCTION_CODE = # Compute Markers Group Transfert\n\
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
	\ttrsfrMarkersValues = docometre.experiments["inputSignal.MarkersGroup_markersGroupLabel_Values"];\n\
	\t# Convert sample frequency to float\n\
	\tsampleFrequency = float(docometre.experiments['inputSignal.SampleFrequency']);\n\
	\tfromValues = docometre.experiments["inputMarker_Values"];\n\
	\tfor trialNumber in numpy.r_[trialsList]:\n\
		\t\t# Get from index in from values for trialNumber\n\
		\t\tfromIndex = numpy.where(fromValues[:,0] == (trialNumber+1))[0];\n\
		\t\t# if index exists\n\
		\t\tif(fromIndex.size > 0):\n\
			\t\t\tfor markerNumber in numpy.r_[0:fromIndex.size]:\n\
				\t\t\t\t# Get time maker\n\
				\t\t\t\ttrsfrTimeValue = fromValues[fromIndex[markerNumber]][1];\n\
				\t\t\t\t# Convert from in samples number\n\
				\t\t\t\tmarkerTimeIndex = int(trsfrTimeValue * sampleFrequency);\n\
				\t\t\t\t# Get amplitude marker value\n\
				\t\t\t\ttrsfrValue = values[trialNumber, markerTimeIndex];\n\
				\t\t\t\ttrsfrMarkersValues = numpy.append(trsfrMarkersValues, [[trialNumber+1, trsfrTimeValue, trsfrValue]], axis = 0);\n\
	\tdocometre.experiments["inputSignal.MarkersGroup_markersGroupLabel_Values"] = trsfrMarkersValues;\n\
	\tif not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = "inputSignal";\n\
	\telse:\n\
		\t\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":inputSignal";\n\
else:\n\
	\tif "ErrorMessages" in docometre.experiments.keys():\n\
		\t\tdocometre.experiments["ErrorMessages"] = docometre.experiments["ErrorMessages"] + "|" + datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Transfert Markers group function -> A Marker with this label already exists : markersGroupLabel";\n\
	\telse:\n\
		\t\tdocometre.experiments["ErrorMessages"] = datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[:-3] + " - From Transfert Markers group function -> A Marker with this label already exists : markersGroupLabel (inputSignal)";\n\