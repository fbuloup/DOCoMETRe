MENU_TITLE = Modify "End Cut"
DESCRIPTION = This function modify end cut value of selected signal.\n\
End cut is a duration, in seconds, at the end of signal that have not to be displayed in charts.
MENU_TITLE_FR = Modifier "l'après coupe"
MENU_TITLE_PL = Zmiana "przycięcia końca"
DESCRIPTION_FR = Cette fonction modifie "l'après coupe" du signal sélectionné.\n\
L'après coupe est une durée, en secondes, en fin de signal qui ne doit pas être montrée dans un graphe.
DESCRIPTION_PL = Ta funkcja zmienia wartość przycięcia końca wybranego sygnału.\n\
Przycięcie końca to czas, w sekundach, na końcu sygnału, który nie jest wyświetlany na wykresach.

# Now start specific matlab function
FUNCTION_CODE = % Change end cut\n\
value =floor(endCut*inputSignal.SampleFrequency);\n\
inputSignal.EndCut(trialsList) = value;\n\
clear value;\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'inputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
end\n\