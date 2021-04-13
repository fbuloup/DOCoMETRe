MENU_TITLE = Modify "End Cut"
DESCRIPTION = This function modify end cut value of selected signals.\n\
End cut is a duration, in seconds, at the end of signal that have not to be displayed in charts.
MENU_TITLE_FR = Modification de "l'après coupe"
DESCRIPTION_FR = Cette fonction modifie "l'après coupe" du signal sélectionné.\n\
L'après coupe est une durée, en secondes, en fin de signal qui ne doit pas être montrée dans un graphe.

# Now start specific python
FUNCTION_CODE = # Change front cut\n\
# import scipy;\n\
# import numpy;\n\
# Create filtered signal\n\
import math;\n\
SF = float(docometre.experiments['inputSignal.SampleFrequency']);\n\
docometre.experiments['inputSignal.EndCut'][range(trialsList)] = math.floor(endCut*SF);\n\
docometre.gateway.jvm.System.out.println("EC : " + str(math.floor(endCut*SF)));\n\
if not('createdOrModifiedChannels' in docometre.experiments.keys()):\n\
\tdocometre.experiments['createdOrModifiedChannels'] = 'inputSignal';\n\
else:\n\
\tdocometre.experiments['createdOrModifiedChannels'] = docometre.experiments['createdOrModifiedChannels'] + ':inputSignal';