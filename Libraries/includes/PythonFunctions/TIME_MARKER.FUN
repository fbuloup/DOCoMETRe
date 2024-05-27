MENU_TITLE = Timer marker
DESCRIPTION = Mark input signal at specific time value
MENU_TITLE_FR = Marqueur temporel
DESCRIPTION_FR = Marque le signal d'entrée à un valeur temporelle spécifique

# Now start specific python function 
FUNCTION_CODE = # Compute Time marker\n\
import numpy;\n\
# Get signal input values\n\
values = docometre.experiments["inputSignal.Values"];\n\
# Create new output markers group\n\
nbMarkersGroups = docometre.experiments["inputSignal.NbMarkersGroups"];\n\
nbMarkersGroups = int(nbMarkersGroups) + 1;\n\
docometre.experiments["inputSignal.NbMarkersGroups"] = str(nbMarkersGroups);\n\
if nbMarkersGroups == 1 :\n\
	\texpression = docometre.experiments["inputSignal.MarkersGroupsLabels"] = ["markersGroupLabel"];\n\
else:\n\
	 \texpression = docometre.experiments["inputSignal.MarkersGroupsLabels"].append("markersGroupLabel");\n\
docometre.experiments["inputSignal.MarkersGroup_markersGroupLabel_Values"] = numpy.zeros((0,3));\n\
timeMarkersValues = docometre.experiments["inputSignal.MarkersGroup_markersGroupLabel_Values"];\n\
# Get sample frequency\n\
sampleFrequency = docometre.experiments['inputSignal.SampleFrequency'];\n\
sampleFrequency = float(sampleFrequency);\n\
# Get end cut values\n\
endCutValues = docometre.experiments["inputSignal.EndCut"];\n\
# Loop over trials list\n\
for trialNumber in numpy.r_[trialsList]:\n\
	\tindex = round(timeMarkerValue*sampleFrequency+1);\n\
	\tendCut = endCutValues[trialNumber];\n\
	\tif endCut >= index and markLastSample != True :\n\
		\t\tnewTime = (index-1)/sampleFrequency;\n\
		\t\ttimeMarkersValues = numpy.append(timeMarkersValues, [[trialNumber+1, newTime, values[trialNumber,index]]], axis = 0);\n\
	\telif markLastSample == True :\n\
		\t\tnewTime = (endCutValues[trialNumber] - 1)/sampleFrequency;\n\
		\t\tnewIndex = int(endCutValues[trialNumber] - 1);\n\
		\t\ttimeMarkersValues = numpy.append(timeMarkersValues, [[trialNumber+1, newTime, values[trialNumber,newIndex]]], axis = 0);\n\
docometre.experiments["inputSignal.MarkersGroup_markersGroupLabel_Values"] = timeMarkersValues;\n\
if not("createdOrModifiedChannels" in docometre.experiments.keys()):\n\
	\tdocometre.experiments["createdOrModifiedChannels"] = "inputSignal";\n\
else:\n\
	\tdocometre.experiments["createdOrModifiedChannels"] = docometre.experiments["createdOrModifiedChannels"] + ":inputSignal";\n\