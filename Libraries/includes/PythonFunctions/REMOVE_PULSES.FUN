MENU_TITLE = Remove pulses
DESCRIPTION = Suppress pulses with predefined amplitude and duration
MENU_TITLE_FR = Supprimer les impulsions
DESCRIPTION_FR = Supprimer les impulsions d'amplitude et de durée déterminées

# Now start specific Python function 
FUNCTION_CODE = # Remove pulses\n\
# Create cleared signal\n\
import numpy, scipy;\n\
import scipy.interpolate;\n\
docometre.experiments['outputSignal.Values'] = numpy.copy(docometre.experiments['inputSignal.Values']);\n\
docometre.experiments['outputSignal.NbSamples'] = numpy.copy(docometre.experiments['inputSignal.NbSamples']);\n\
docometre.experiments['outputSignal.FrontCut'] = numpy.copy(docometre.experiments['inputSignal.FrontCut']);\n\
docometre.experiments['outputSignal.EndCut'] = numpy.copy(docometre.experiments['inputSignal.EndCut']);\n\
docometre.experiments['outputSignal.isSignal'] = 1;\n\
docometre.experiments['outputSignal.isCategory'] = 0;\n\
docometre.experiments['outputSignal.isEvent'] = 0;\n\
docometre.experiments['outputSignal.NbFeatures'] = 0;\n\
docometre.experiments['outputSignal.NbMarkersGroups'] = 0;\n\
docometre.experiments['outputSignal.SampleFrequency'] = numpy.copy(docometre.experiments['inputSignal.SampleFrequency']);\n\
lastDelta = 0;\n\
pulseWidth = docometre.experiments['inputSignal.SampleFrequency']*duration;\n\
for trialNumber in numpy.r_[trialsList]:\n\
	\ttrialValues = docometre.experiments['inputSignal.Values'][trialNumber,:];\n\
	\tpreviousValue = trialValues[0];\n\
	\tlastSampleNumber = -1;\n\
	\tfor sampleNumber in range(1, numpy.int_(docometre.experiments['inputSignal.NbSamples'][trialNumber])):\n\
		\t\tcurrentValue = trialValues[sampleNumber];\n\
		\t\tif abs(currentValue - previousValue) > delta:\n\
				\t\t\tif numpy.sign((currentValue - previousValue)*lastDelta) <= 0:\n\
					\t\t\t\tif lastSampleNumber > -1:\n\
						\t\t\t\t\t# Interpolate\n\
						\t\t\t\t\txInputValues = [lastSampleNumber/docometre.experiments['inputSignal.SampleFrequency'], sampleNumber/docometre.experiments['inputSignal.SampleFrequency']];\n\
						\t\t\t\t\tyInputValues = [trialValues[lastSampleNumber], trialValues[sampleNumber]];\n\
						\t\t\t\t\tnewValuesFunction = scipy.interpolate.interp1d(xInputValues, yInputValues, kind='linear');\n\
						\t\t\t\t\txOutputValues = range(lastSampleNumber, sampleNumber+1)/docometre.experiments['outputSignal.SampleFrequency'];\n\
						\t\t\t\t\tdocometre.experiments['outputSignal.Values'][trialNumber, range(lastSampleNumber, sampleNumber + 1)] = newValuesFunction(xOutputValues);\n\
						\t\t\t\t\tlastSampleNumber = -1;\n\
						\t\t\t\t\tlastDelta = 0;\n\
					\t\t\t\telse:\n\
						\t\t\t\t\tlastSampleNumber = sampleNumber - 1;\n\
						\t\t\t\t\tlastDelta = currentValue - previousValue;\n\
				\t\t\telse:\n\
					\t\t\t\tlastSampleNumber = -1;\n\
					\t\t\t\tlastDelta = 0;\n\
			\t\tif lastSampleNumber > -1:\n\
				\t\t\tif sampleNumber - lastSampleNumber > pulseWidth:\n\
					\t\t\t\tlastSampleNumber = -1;\n\
					\t\t\t\tlastDelta = 0;\n\
			\t\tpreviousValue = currentValue;\n\
if not('createdOrModifiedChannels' in docometre.experiments.keys()):\n\
	\tdocometre.experiments['createdOrModifiedChannels'] = 'outputSignal';\n\
else:\n\
	\tdocometre.experiments['createdOrModifiedChannels'] = docometre.experiments['createdOrModifiedChannels'] + ':outputSignal';\n\