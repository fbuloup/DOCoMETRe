MENU_TITLE = Modify "Front Cut"
DESCRIPTION = This function modify front cut value of selected signal.\n\
Front cut is a duration, in seconds, at the beginning of signal that have not to be displayed in charts.
MENU_TITLE_FR = Modifier "l'avant coupe"
MENU_TITLE_PL = Zmiana "przycięcia początku"
DESCRIPTION_FR = Cette fonction modifie "l'avant coupe" du signal sélectionné.\n\
L'avant coupe est une durée, en secondes, au début du signal qui ne doit pas être montrée dans un graphe.
DESCRIPTION_PL = Ta funkcja zmienia wartość przycięcia początku wybranego sygnału.\n\
Przycięcie początku to czas, w sekundach, na początku sygnału, który nie jest wyświetlany na wykresach.

# Now start specific matlab function
FUNCTION_CODE = % Change front cut\n\
value =floor(frontCut*inputSignal.SampleFrequency);\n\
inputSignal.FrontCut([trialsList]) = value;\n\
clear value;\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'inputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'inputSignal'}];\n\
end\n\