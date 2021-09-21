MENU_TITLE = Motion Direction
DESCRIPTION = This function computes the motion direction between two markers. This direction is computed\n\
for each point of the curve formed by the two input signals and with respect to the starting marker.
MENU_TITLE_FR = Direction du mouvement 
DESCRIPTION_FR = Cette fonction calcule la direction du mouvement entre deux marqueurs. Cette direction est calculée\n\
pour chaque point de la courbe formée par les deux signaux d'entrée et par rapport au marqueur d'origine.  

# Now start specific matlab function 
FUNCTION_CODE = % Compute Motion Direction\n\
% Compute result angles in degree or radian\n\
resultInDegree = resultInDegreeValue;\n\
% Get signals input values\n\
values1 = inputSignal1.Values;\n\
values2 = inputSignal2.Values;\n\
% Create output signal. It is assumed that input signals are same size, same samples number and same sample frequency\n\
signalSizes = size(inputSignal1.Values); % signalSizes(1) : trials number - signalSizes(2) : samples number\n\
outputSignal.Values = zeros(signalSizes(1), signalSizes(2));\n\
outputSignal.NbSamples = inputSignal1.NbSamples;\n\
outputSignal.isSignal = 1;\n\
outputSignal.isCategory = 0;\n\
outputSignal.isEvent = 0;\n\
outputSignal.NbMarkersGroups = 0;\n\
outputSignal.NbFeatures = 0;\n\
outputSignal.SampleFrequency = inputSignal1.SampleFrequency;\n\
outputSignal.FrontCut = inputSignal1.FrontCut;\n\
outputSignal.EndCut = inputSignal1.EndCut;\n\
% Get from and to values\n\
if "From_Beginning" == "inputMarker1"\n\
	\tfromValues = zeros(signalSizes(1),3);\n\
	\tfromValues(:,1) = trialsList;\n\
	\tfromValues(:,3) = values1(:,1);\n\
else\n\
	\tfromValues = inputMarker1_Values;\n\
end\n\
if "To_End" == "inputMarker2"\n\
	\ttoValues = zeros(signalSizes(1),3);\n\
	\ttoValues(:,1) = trialsList;\n\
	\ttoValues(:,2) = outputSignal.NbSamples/outputSignal.SampleFrequency;\n\
	\ttoValues(:,3) = outputSignal.NbSamples;\n\
else\n\
	\ttoValues = inputMarker2_Values;\n\
end\n\
% Loop over trials	\n\
for trialNumber = trialsList\n\
	\t% Get from and to index in from and to values for trialNumber\n\
	\tfromIndex = find(fromValues(:,1) == trialNumber);\n\
	\ttoIndex = find(toValues(:,1) == trialNumber);\n\
	\t% if both indices exist\n\
	\tif(~isempty(fromIndex) && ~isempty(toIndex))\n\
		\t\t% Convert from and to in samples number\n\
		\t\tfromIndex = floor(fromValues(fromIndex(1),2) * outputSignal.SampleFrequency) + 1;\n\
		\t\ttoIndex = floor(toValues(toIndex(1),2) * outputSignal.SampleFrequency) + 2;\n\
		\t\t% Origine coordinates\n\
		\t\tx0 = values1(trialNumber,fromIndex);\n\
		\t\ty0 = values2(trialNumber,fromIndex);\n\
		\t\t% Init first output signal sample\n\
		\t\toutputSignal.Values(trialNumber, fromIndex) = 0;\n\
		\t\tfor index = fromIndex:toIndex\n\
			\t\t\tdx = values1(trialNumber, index) - y0;\n\
			\t\t\tdy = values2(trialNumber, index) - y0;\n\
			\t\t\t% Compute angle\n\
			\t\t\ttheta =  acos(dx/sqrt(dx^2 + dy^2));\n\
			\t\t\tif (dx<0) && (dy>0)\n\
				\t\t\t\ttheta = -theta + pi/2;\n\
			\t\t\telseif (dx<0) && (dy<0)\n\
				\t\t\t\ttheta = -theta + pi/2;\n\
			\t\t\telseif (dx>0) && (dy>0)\n\
				\t\t\t\ttheta = pi/2 - theta;\n\
			\t\t\telseif (dx>0) && (dy<0) \n\
				\t\t\t\ttheta = pi/2 - theta;\n\
			\t\t\tend\n\
			\t\t\tif resultInDegree\n\
				\t\t\t\ttheta = theta*180/pi;\n\
			\t\t\tend\n\
			\t\t\toutputSignal.Values(trialNumber, index) = theta;\n\
		\t\tend\n\
	\tend\n\
end\n\
clear fromIndex toIndex fromValues toValues dx dy trialNumber values1 values2 theta index resultInDegree signalSizes x0 y0\n\
if (~exist('createdOrModifiedChannels'))\n\
\tcreatedOrModifiedChannels = {'outputSignal'};\n\
else\n\
\tcreatedOrModifiedChannels = [createdOrModifiedChannels; {'outputSignal'}];\n\
end\n\