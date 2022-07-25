MENU_TITLE = Change Sample Frequency
DESCRIPTION = Change one or more signals Sample Frequency
MENU_TITLE_FR = Modifier la fréquence d'échantillonnage
DESCRIPTION_FR = Modifier la fréquence d'échantillonnage d'un ou plusieurs signaux

# Now start specific matlab function 
FUNCTION_CODE = % Change sample frequency value\n\
inputSignal.SampleFrequency = sampleFrequency;\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'inputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
end\n\