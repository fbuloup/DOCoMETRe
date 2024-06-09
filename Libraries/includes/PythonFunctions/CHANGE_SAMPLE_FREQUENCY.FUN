MENU_TITLE = Change Sample Frequency
DESCRIPTION = Change one or more signals Sample Frequency
MENU_TITLE_FR = Modifier la fréquence d'échantillonnage
DESCRIPTION_FR = Modifier la fréquence d'échantillonnage d'un ou plusieurs signaux

# Now start specific python function
FUNCTION_CODE = # Change sample frequency value\n\
docometre.experiments['inputSignal.SampleFrequency'] = sampleFrequency;\n\
if not('createdOrModifiedChannels' in docometre.experiments.keys()):\n\
\tdocometre.experiments['createdOrModifiedChannels'] = 'inputSignal';\n\
else:\n\
\tdocometre.experiments['createdOrModifiedChannels'] = docometre.experiments['createdOrModifiedChannels'] + ':inputSignal';\n\