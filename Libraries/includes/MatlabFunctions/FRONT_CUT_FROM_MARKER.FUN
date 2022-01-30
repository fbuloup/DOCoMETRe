MENU_TITLE = Modify "Front Cut" from marker
DESCRIPTION = This function modify front cut value of selected signal from marker.
MENU_TITLE_FR = Modifier "l'Avant coupe" à partir d'un marqueur
DESCRIPTION_FR = Cette fonction modifie "l'Avant coupe" du signal sélectionné à partir d'un marqueur.

# Now start specific matlab function 
FUNCTION_CODE = # Compute Front Cut from marker\n\
for trialNumber = trialsList\n\
	\ttrialIndices = find(inputMarker.Values(:,1) == TrialNumber);\n\
	\tif(~isempty(trialIndices))\n\
		\t\tvalue =round(inputMarker.Values(trialIndices(1), 2)*inputSignal.SampleFrequency+1);\n\
		\t\tinputSignal.FrontCut(TrialNumber) = value;\n\
	\tend\n\
end\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'inputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
end\n\