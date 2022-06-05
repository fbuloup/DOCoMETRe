from py4j.clientserver import ClientServer, JavaParameters, PythonParameters;
import sys;
import os;
import numpy;
import io;
import re;
import time;

class DOCoMETRe(object):

	#global experiments;
	global jvmMode;

	def __init__(self, gateway):
	 	self.gateway = gateway;
	 	self.experiments = dict();
	 	if(jvmMode): gateway.jvm.System.out.println("In __init__ gateway");

	def shutDownServer(self, object):
		if(jvmMode): self.gateway.jvm.System.out.println("In shutdown server");
		pass;

	def toString(self):
		if(jvmMode): self.gateway.jvm.System.out.println("In toString");
		return "This is DOCoMETRe Python Entry Point";

	def loadData(self, dataType, loadName, dataFilesList, sessionsProperties):
		if(dataType == "DOCOMETRE"):
			self.loadDataDocometre(loadName, dataFilesList, sessionsProperties);
		elif(dataType == "OPTITRACK_TYPE_1"):
			self.loadDataOptitrackType1(loadName, dataFilesList);
		else:
			pass;

	def loadDataDocometre(self, loadName, dataFilesList, sessionsProperties):
		if(jvmMode): self.gateway.jvm.System.out.println("In loadDataDocometre");
		if ".sau" in dataFilesList:
			if(jvmMode): self.gateway.jvm.System.out.println("For now, sau files are not handled with Python");
		elif ".samples" in dataFilesList:
			self.loadDataDocometreSAMPLES(loadName, dataFilesList, sessionsProperties);
		elif ".adw" in dataFilesList:
			if(jvmMode): self.gateway.jvm.System.out.println("For now, ADW files are not handled with Python");
		else:
			if(jvmMode): self.gateway.jvm.System.out.println("Data files format not hanled with Python");

	def loadDataOptitrackType1(self, loadName, dataFilesAbsolutePath):
		if(jvmMode): self.gateway.jvm.System.out.println("In loadDataOptitrackType1");
		
		createdCategories = dict();
		createdChannelsNames = list();
		channelNames = list();
		nbSamples = 0;
		firstTime = True;
		
		files = dataFilesAbsolutePath.split(";");
		nbFiles = len(files);
		nbTrials = nbFiles;
		
		for n in range(0, nbFiles):
			fileName = os.path.basename(files[n]);
			fileName = os.path.splitext(fileName)[0];
			
			fileNameSplitted = fileName.split("_");
			criteria = fileNameSplitted[1];    
			
			if criteria in createdCategories:
				trialsList = createdCategories[criteria];
				append = False;
				
				if isinstance(trialsList, numpy.ndarray):
					if n+1 not in trialsList:
						append = True;
				
				if append:
					trialsList = numpy.append(trialsList, n+1);
				
			else:
				trialsList = numpy.array(n+1);
			
			createdCategories[criteria] = trialsList;

			fid = open(files[n], "r");
			line = fid.readline();
			tempValue = line.split(",");
			nbSamples = max(nbSamples, int(tempValue[1]));
			
			if firstTime:
				firstTime = False;
				line = fid.readline();
				line = fid.readline();
				tempValue = line.split(",");
				sampleFrequency = float(tempValue[1]);
				line = fid.readline();
				line = fid.readline();
				line = fid.readline();
				tempValue = line.split(',');
				for p in range(0, len(tempValue)):
					tempValue2 = tempValue[p];
					if tempValue2 != "Time" and tempValue2 != "" and tempValue2 != "\n":
							channelNames.append(tempValue2 + "_X");
							channelNames.append(tempValue2 + "_Y");
							channelNames.append(tempValue2 + "_Z");
			
			fid.close();
		
		for trialNumber in range(1, nbTrials+1):
			data = numpy.loadtxt(files[trialNumber-1], delimiter=',', skiprows=8);
			data = numpy.delete(data, 0, axis = 1);
			
			fid = open(files[trialNumber-1], "r");
			line = fid.readline();
			tempValue = line.split(",");
			localNBSamples = int(tempValue[1]);
			fid.close();
			
			numChannel = 0;
			for channelName in channelNames:
				if channelName not in createdChannelsNames:
					self.experiments[loadName + "." + channelName + "." + "SampleFrequency"] = sampleFrequency;
					self.experiments[loadName + "." + channelName + "." + "isSignal"] = "1";
					self.experiments[loadName + "." + channelName + "." + "isCategory"] = "0";
					self.experiments[loadName + "." + channelName + "." + "isEvent"] = "0";
					self.experiments[loadName + "." + channelName + "." + "NbFeatures"] = "0";
					self.experiments[loadName + "." + channelName + "." + "NbMarkersGroups"] = "0";
					self.experiments[loadName + "." + channelName + "." + "Values"] = numpy.zeros((nbTrials, nbSamples));
					self.experiments[loadName + "." + channelName + "." + "NbSamples"] = numpy.zeros(nbTrials);
					self.experiments[loadName + "." + channelName + "." + "FrontCut"] = numpy.zeros(nbTrials);
					self.experiments[loadName + "." + channelName + "." + "EndCut"] = numpy.zeros(nbTrials);
					createdChannelsNames.append(channelName);
			
				sizeData = len(data[:,numChannel]);
				key = loadName + "." + channelName + ".NbSamples";
				self.experiments[key][trialNumber - 1] = localNBSamples;
				key = loadName + "." + channelName + ".EndCut";
				self.experiments[key][trialNumber - 1] = localNBSamples;
				values = self.experiments[loadName + "." + channelName + ".Values"];
				values[trialNumber - 1][0:sizeData] = data[:, numChannel];
				numChannel+=1;
			
		n = 1;
		for criteria in createdCategories:
			values = createdCategories[criteria];
			self.experiments[loadName + ".Category" + str(n) + ".Criteria"] = criteria;
			self.experiments[loadName + ".Category" + str(n) + ".TrialsList"] = values;
			self.experiments[loadName + ".Category" + str(n) + ".isSignal"] = "0";
			self.experiments[loadName + ".Category" + str(n) + ".isCategory"] = "1";
			self.experiments[loadName + ".Category" + str(n) + ".isEvent"] = "0";
			n+=1;
			
			
				
	def loadDataDocometreSAMPLES(self, loadName, dataFilesList, sessionsProperties):
		if(jvmMode): self.gateway.jvm.System.out.println("In loadDataDocometreSAMPLES");
		# if(jvmMode): self.gateway.jvm.System.out.println(sessionsProperties)

		prefix_QN = "_DATA_FILES_NAMES_PREFIX";
		baseTrialsNumber_QN = "_BASE_TRIALS_NUMBER";

		createdCategories = dict();
		createdChannels = list();

		dataFiles = dataFilesList.split(";");
		nbDataFiles = len(dataFiles);

		maximumSamples = sessionsProperties["MAXIMUM_SAMPLES"];
		totalTrialsNumber = sessionsProperties["TOTAL_TRIALS_NUMBER"];

		for n in range(0, nbDataFiles):
			segments = dataFiles[n].split(os.path.sep);

			# Get session name for criteria, trial number and prefix
			sessionName = segments[len(segments) - 3];
			trialName = segments[len(segments) - 2];
			key = os.path.dirname(os.path.abspath(dataFiles[n]))
			process = sessionsProperties[key + "_PROCESS"];
			# if(jvmMode): self.gateway.jvm.System.out.println("nbDataFiles : " + str(nbDataFiles) + " fichier : " + str(n+1) + " sessionName : " + sessionName + " process : " + process + " - " + dataFiles[n]);
			criteria = sessionName + "." + process;
			if sessionName + prefix_QN in sessionsProperties:
				if sessionsProperties[sessionName + prefix_QN] != "":
					criteria = sessionsProperties[sessionName + prefix_QN];
					criteria = criteria + "." + sessionName + "." + process;
			# if(jvmMode): self.gateway.jvm.System.out.println("nbDataFiles : " + str(nbDataFiles) + " fichier : " + str(n+1) + " critere : " + criteria);
			# if(jvmMode): self.gateway.jvm.System.out.println(str(n) + " - " + dataFiles[n]);
			trialNumber = trialName.split("\u00b0")[1];
			system = sessionsProperties[key + "_SYSTEM"];
			baseTrialsNumber = sessionsProperties[sessionName + baseTrialsNumber_QN];
			trialNumber = str(int(baseTrialsNumber) + int(trialNumber));

			# Get channel's name
			fileName = segments[len(segments) - 1];
			fileNameSegments = fileName.split('.');
			channelName = fileNameSegments[0];
			if sessionName + prefix_QN in sessionsProperties:
				if sessionsProperties[sessionName + prefix_QN] != "":
					channelName = fileNameSegments[1];

			if criteria in createdCategories:

				append = False;
				trialsList = createdCategories[criteria];

				if isinstance(trialsList, numpy.ndarray):
					if int(trialNumber) not in trialsList:
						append = True;
				#else:
				#	if int(trialNumber) != trialsList:
				#		append = True;

				if append:
					trialsList = numpy.append(trialsList, int(trialNumber));
					#if(jvmMode): self.gateway.jvm.System.out.println(ListConverter().convert(trialsList.tolist(), gateway._gateway_client));
			else:
				trialsList = numpy.array(int(trialNumber));
				#if(jvmMode): self.gateway.jvm.System.out.println(trialsList);

			createdCategories[criteria] = trialsList;

			if channelName not in createdChannels:
				createdChannels.append(channelName);
				sampleFrequency = sessionsProperties[channelName + "_SF"];
				self.experiments[loadName + "." + channelName + "." + "SampleFrequency"] = sampleFrequency;
				self.experiments[loadName + "." + channelName + "." + "isSignal"] = "1";
				self.experiments[loadName + "." + channelName + "." + "isCategory"] = "0";
				self.experiments[loadName + "." + channelName + "." + "isEvent"] = "0";
				self.experiments[loadName + "." + channelName + "." + "NbFeatures"] = "0";
				self.experiments[loadName + "." + channelName + "." + "NbMarkersGroups"] = "0";
				self.experiments[loadName + "." + channelName + "." + "Values"] = numpy.zeros((int(totalTrialsNumber), int(maximumSamples)));
				self.experiments[loadName + "." + channelName + "." + "NbSamples"] = numpy.zeros(int(totalTrialsNumber));
				self.experiments[loadName + "." + channelName + "." + "FrontCut"] = numpy.zeros(int(totalTrialsNumber));
				self.experiments[loadName + "." + channelName + "." + "EndCut"] = numpy.zeros(int(totalTrialsNumber));

			# Read data
			data = numpy.fromfile(dataFiles[n], dtype="float32");
			
			#if system == "Arduino UNO":
			#	data = data[1::2];

			sizeData = len(data);
			#if(jvmMode): self.gateway.jvm.System.out.println("sizeData : " + str(sizeData));
			key = loadName + "." + channelName + ".NbSamples";
			self.experiments[key][int(trialNumber) - 1] = sizeData;
			key = loadName + "." + channelName + ".FrontCut";
			self.experiments[key][int(trialNumber) - 1] = 0;
			key = loadName + "." + channelName + ".EndCut";
			self.experiments[key][int(trialNumber) - 1] = sizeData;
			values = self.experiments[loadName + "." + channelName + ".Values"];
			#if(jvmMode): self.gateway.jvm.System.out.println("size values : " + str(len(values[int(trialNumber) - 1])));
			values[int(trialNumber) - 1][0:sizeData] = data;

		n = 1;
		for criteria in createdCategories:
			values = createdCategories[criteria];
			self.experiments[loadName + ".Category" + str(n) + ".Criteria"] = criteria;
			self.experiments[loadName + ".Category" + str(n) + ".TrialsList"] = values;
			self.experiments[loadName + ".Category" + str(n) + ".isSignal"] = "0";
			self.experiments[loadName + ".Category" + str(n) + ".isCategory"] = "1";
			self.experiments[loadName + ".Category" + str(n) + ".isEvent"] = "0";
			n = n + 1;
			
	def evaluate(self, expression):
		# if(jvmMode): self.gateway.jvm.System.out.println("Evaluate : " + expression);
		return str(eval(expression));
	
	def unload(self, fullName):
		code = "import re\n";
		code = code + "keysToDelete = {key for key in docometre.experiments.keys() if re.match('^" + fullName + "', key) != None}\n"
		code = code + "for key in keysToDelete:\n";
		code = code + "\tdocometre.experiments.pop(key)";
		exec(code);
		
	def getChannels(self, subjectFullName):
		channels = list({k:v for k,v in self.experiments.copy().items() if re.search("^" + subjectFullName + "\.\w+\.isSignal$", k)});
		channels  = [re.sub("\.\w+$", "", channel) for channel in channels];
		channels  = [re.sub("^\w+\.\w+\.", "", channel) for channel in channels];
		return ",".join(channels);
	
	def getVector(self, expression, dataType, trialNumber, frontCut, endCut):
		values = eval(expression);
		if(trialNumber > -1):
			values = values[trialNumber][frontCut:endCut];
		
		arrayValues = numpy.array(values);
		
		arrayValues = arrayValues.astype(dataType);
		
		return arrayValues.tobytes();
	
	def runScript(self, code):
		if(jvmMode): gateway.jvm.System.out.println("In runScript : " + code);
		exec(code);
		
	def saveSubject(self, fullSubjectNameRegExp, dataFilesFullPath):
		subject = {k:v for k,v in self.experiments.items() if re.search(fullSubjectNameRegExp, k) != None}
		ndArrayFileNumber = 1;
		file = open(dataFilesFullPath + 'save.data','w');
		for key,value in subject.items():
			# Remove Experiment.Subject prefix
			newKey = re.sub("^\w+\.\w+\.", "", key); 
			if isinstance(value, str):
				file.write(newKey);
				file.write('\n');
				file.write('str("' + value + '")');
				file.write('\n');
			elif isinstance(value, int):
				file.write(newKey);
				file.write('\n');
				file.write('int(' + str(value) + ')');
				file.write('\n');
			elif isinstance(value, float):
				file.write(newKey);
				file.write('\n');
				file.write('float(' + str(value) + ')');
				file.write('\n');
			elif isinstance(value, numpy.ndarray):
				fileName =  'data_' + str(ndArrayFileNumber) + '.numpy';
				ndArrayFileNumber = ndArrayFileNumber + 1;
				file.write(newKey);
				file.write('\n');
				file.write('numpy.ndarray:' + fileName);
				file.write('\n');
				ndArrayFile = open(dataFilesFullPath + fileName,'wb');
				numpy.save(ndArrayFile, value, False, False);
				ndArrayFile.close();
			elif isinstance(value, list) and all(isinstance(n, str) for n in value):
				file.write(newKey);
				file.write('\n');
				file.write('list.str(' + ':'.join(value) + ')');
				file.write('\n');
			else:				
				if(jvmMode): 
					self.gateway.jvm.System.out.print("Type not handled : " + type(value).__name__);
					self.gateway.jvm.System.out.println(" for key : " + key);
				else: print("Type not handled : " + type(value) + " for key : " + key);
				
		file.close();
		
	def loadSubject(self, saveFilesFullPath):
		previousWD = os.getcwd();
		try:
			subject = dict();
			segments = saveFilesFullPath.split(os.path.sep);
			experimentName = segments[len(segments) - 3];
			subjectName = segments[len(segments) - 2];
			currentWD = previousWD + os.path.sep + experimentName + os.path.sep + subjectName;
			os.chdir(currentWD);
			prefixKey = experimentName + "." + subjectName + ".";
			file = open('save.data','r');
			key = file.readline().strip();
			while key:
			# Add Experiment.Subject prefix
				key = prefixKey + key;
				value = file.readline().strip();
				if(value.startswith('str(')):
					subject[key] = eval(value);
				elif(value.startswith('int(')):
					subject[key] = eval(value);
				elif(value.startswith('float(')):
					subject[key] = eval(value);
				elif(value.startswith('numpy.ndarray:')):
					fileName = value.replace('numpy.ndarray:', '');
					ndArrayFile = open(fileName,'rb');
					value = numpy.load(ndArrayFile, None);
					ndArrayFile.close();
					subject[key] = value;		
				elif(value.startswith('list.str')):
						value = re.sub("^list\.str\(", "", value);
						value = re.sub("\)$", "", value);
						subject[key] = value.split(":");
				key = file.readline().strip();
	
			self.experiments.update(subject);
			
		finally:
			os.chdir(previousWD);
			
		if(jvmMode): 
			self.gateway.jvm.System.out.println("WD : " + os.getcwd());
			
	def rename(self, keyRegExp, keyReplace):
		subDict = {k:v for k,v in docometre.experiments.items() if re.search(keyRegExp, k) != None}
		for key,value in subDict.items():
			newKey = re.sub(keyRegExp, keyReplace, key);
			docometre.experiments[newKey] = docometre.experiments.pop(key);
		subDict = {k:v for k,v in docometre.experiments.items() if re.search(keyRegExp, k) != None}
		keyReplace = "^" + re.sub("\.", "\.", keyReplace);
		subDict2 = {k:v for k,v in docometre.experiments.items() if re.search(keyReplace, k) != None}
		#if(jvmMode): 
		#	self.gateway.jvm.System.out.println("Is dict empty : " + str(any(subDict)) + " for " + keyRegExp);
		#	self.gateway.jvm.System.out.println("Is dict empty : " + str(any(subDict2)) + " for " + keyReplace);
		return (not(any(subDict)) and any(subDict2))

	def getLoadedSubjects(self):
		keys = {k for k,v in docometre.experiments.items() if re.search('^\w+\.\w+\.\w+\.isSignal', k) != None};
		loadedSubjects = set();
		for key in keys:
			newKey = re.sub("\.\w+\.isSignal$", "", key);
			loadedSubjects.add(newKey);
		if(len(loadedSubjects) > 0):
			return ":".join(loadedSubjects);
		return "";
		
	class Java:
		implements = ["fr.univamu.ism.docometre.python.PythonEntryPoint"]

if __name__ == "__main__":
	
	print("Current working folder : " + os.getcwd());
	jvmMode = True;
	
	#experiments = dict();
	
	if(len(sys.argv) > 1):
		if(sys.argv[1] == "-jvm"):	
			gateway = ClientServer(java_parameters = JavaParameters(), python_parameters = PythonParameters());	
			docometre = DOCoMETRe(gateway);
			D = docometre.experiments;
			gateway.entry_point.setPythonEntryPoint(docometre);			
		else:
			jvmMode = False;
		
	else:
		jvmMode = False;
		
	if(not jvmMode):
		print("We are not in JVM mode");
		
		docometre = DOCoMETRe(None);
		D = docometre.experiments;
        
        # Example to find global maximum

		docometre.loadData("OPTITRACK_TYPE_1", "test.S02", "/Users/frank/Desktop/Demo/test/S02/D/S02_D_1.csv;/Users/frank/Desktop/Demo/test/S02/D/S02_D_2.csv;/Users/frank/Desktop/Demo/test/S02/D/S02_D_3.csv;/Users/frank/Desktop/Demo/test/S02/D/S02_D_4.csv;/Users/frank/Desktop/Demo/test/S02/D/S02_D_5.csv;/Users/frank/Desktop/Demo/test/S02/D/S02_D_6.csv;/Users/frank/Desktop/Demo/test/S02/ND/S02_ND_1.csv;/Users/frank/Desktop/Demo/test/S02/ND/S02_ND_2.csv;/Users/frank/Desktop/Demo/test/S02/ND/S02_ND_3.csv;/Users/frank/Desktop/Demo/test/S02/ND/S02_ND_4.csv;/Users/frank/Desktop/Demo/test/S02/ND/S02_ND_5.csv;/Users/frank/Desktop/Demo/test/S02/ND/S02_ND_6.csv", None);

		import numpy;
		from datetime import datetime;

		signalSizes = docometre.experiments['test.S02.doigt_Y.Values'].shape;
		nbSamples = docometre.experiments['test.S02.doigt_Y.NbSamples'];
		compute = True;
		NbMarkersGroups = 0;
		# Check if markers Group already exists
		if "test.S02.doigt_Y.NbMarkersGroups" in docometre.experiments.keys():
			nbMarkersGroups = int(docometre.experiments["test.S02.doigt_Y.NbMarkersGroups"]);
			for markersGroupNumber in range(nbMarkersGroups):
				if docometre.experiments["test.S02.doigt_Y.MarkersGroupsLabels"][markersGroupNumber] == "Maximum":
					compute = False;
					break;
		if compute:
			# Get signal input values
			values = docometre.experiments["test.S02.doigt_Y.Values"];
			# Create new output markers group
			nbMarkersGroups = int(nbMarkersGroups) + 1;
			docometre.experiments["test.S02.doigt_Y.NbMarkersGroups"] = str(nbMarkersGroups);
			if nbMarkersGroups == 1:
				expression = docometre.experiments["test.S02.doigt_Y.MarkersGroupsLabels"] = ["Maximum"];
			else:
				expression = docometre.experiments["test.S02.doigt_Y.MarkersGroupsLabels"].append("Maximum");
			docometre.experiments["test.S02.doigt_Y.MarkersGroup_Maximum_Values"] = numpy.zeros((0, 3));
			maxMarkersValues = docometre.experiments["test.S02.doigt_Y.MarkersGroup_Maximum_Values"];
			# Convert sample frequency to float
			sampleFrequency = float(docometre.experiments['test.S02.doigt_Y.SampleFrequency']);
			# Get from and to values
			if "From_Beginning" == "From_Beginning":
				fromValues = numpy.zeros((signalSizes[0], 3));
				fromValues[:, 0] = numpy.r_[0:12] + 1;
				fromValues[:, 2] = values[:, 0];
			else:
				if ("From_Beginning".endswith(".FrontCut")):
					fromValues = numpy.zeros((signalSizes[0], 3));
					fromValues[:, 0] = numpy.r_[0:12] + 1;
					fromValues[:, 1] = numpy.true_divide(docometre.experiments["From_Beginning"], sampleFrequency);
					fromValues[:, 2] = values[:, 0];
				else:
					fromValues = docometre.experiments["From_Beginning_Values"];
			if "To_End" == "To_End":
				toValues = numpy.zeros((signalSizes[0], 3));
				toValues[:, 0] = numpy.r_[0:12] + 1;
				toValues[:, 1] = numpy.true_divide(nbSamples, sampleFrequency);
				toValues[:, 2] = nbSamples;
			else:
				if ("To_End".endswith(".EndCut")):
					toValues = numpy.zeros((signalSizes[0], 3));
					toValues[:, 0] = numpy.r_[0:12] + 1;
					toValues[:, 1] = numpy.true_divide(docometre.experiments["To_End"], sampleFrequency);
					toValues[:, 2] = values[:, 0];
				else:
					toValues = docometre.experiments["To_End_Values"];
			for trialNumber in numpy.r_[0:12]:
				# Get from and to index in from and to values for trialNumber
				fromIndex = numpy.where(fromValues[:, 0] == (trialNumber + 1))[0];
				toIndex = numpy.where(toValues[:, 0] == (trialNumber + 1))[0];
				# if both indices exist
				if (fromIndex.size > 0 and toIndex.size > 0):
					# Convert from and to in samples number
					fromIndex = int(fromValues[fromIndex[0]][1] * sampleFrequency);
					toIndex = int(toValues[toIndex[0]][1] * sampleFrequency);
					# Compute max between from and to index
					maxValue = values[trialNumber, fromIndex:toIndex].max(axis=0);
					maxIndex = values[trialNumber, fromIndex:toIndex].argmax(axis=0);
					maxMarkerTime = (fromIndex + maxIndex) / sampleFrequency;
					maxMarkersValues = numpy.append(maxMarkersValues, [[trialNumber + 1, maxMarkerTime, maxValue]],
													axis=0);
			docometre.experiments["test.S02.doigt_Y.MarkersGroup_Maximum_Values"] = maxMarkersValues;
			if not ("createdOrModifiedChannels" in docometre.experiments.keys()):
				docometre.experiments["createdOrModifiedChannels"] = "test.S02.doigt_Y";
			else:
				docometre.experiments["createdOrModifiedChannels"] = docometre.experiments[
																		 "createdOrModifiedChannels"] + ":test.S02.doigt_Y";
		else:
			if "ErrorMessages" in docometre.experiments.keys():
				docometre.experiments["ErrorMessages"] = docometre.experiments[
															 "ErrorMessages"] + "|" + datetime.utcnow().strftime(
					'%Y-%m-%d %H:%M:%S:%f')[
																					  :-3] + " - From Maximum function -> A Marker with this label already exists : Maximum";
			else:
				docometre.experiments["ErrorMessages"] = datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S:%f')[
														 :-3] + " - From Maximum function -> A Marker with this label already exists : Maximum (test.S02.doigt_Y)";

		# loadName = "ReachabilityCoriolis.PreTestFull";
		#
		# fileHandle = io.open("./tests/dataFilesList2.txt", "r", encoding="utf-8");
		# dataFilesList = fileHandle.read();
		# fileHandle.close();
		#
		# fileHandle = io.open("./tests/sessionsProperties2.txt", "r", encoding="utf-8");
		# sessionPropertiesString = fileHandle.read();
		# sessionProperties = eval(sessionPropertiesString);
		# fileHandle.close();
		#
		# docometre.loadData("DOCOMETRE", loadName, dataFilesList, sessionProperties);
		#
		# # Some infos
		# print(docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.Criteria"]);
		# print(docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.isSignal"]);
		# print(docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.isCategory"]);
		# print(docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.isEvent"]);
		# print(docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.TrialsList"]);
		# print(21 in docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.TrialsList"]);
		#
		# # Test if subject is loaded
		# filteredDictionnary = {k:v for k,v in docometre.experiments.items() if re.search("^ReachabilityCoriolis\.PreTestFull\.", k)};
		# testLoaded = len(filteredDictionnary) > 0;
		# print(testLoaded);
		#
		# # Test evaluate 1
		# # expression = "len({k:v for k,v in docometre.experiments.items() if re.search(\"^" + "ReachabilityCoriolis\.PreTestFull" + "\.\", k)})";
		# #response = docometre.evaluate(expression);
		# # print(response);
		#
		# # Test evaluate 1
		# # expression = "len({k:v for k,v in docometre.experiments.items() if re.search(\"^" + "ReachabilityCoriolis\.PreTestFull" + "\.\", k)}) > 0";
		# # response = docometre.evaluate(expression);
		# # print(response);
		#
		# # Unload subject
		# #print(docometre.experiments);
		# #docometre.unload("ReachabilityCoriolis\.PreTestFull");
		# #print(docometre.experiments);
		#
		#
		# # Get channels, signals, categories or events names
		# keys = docometre.experiments.keys();
		#
		# signals = list({k:v for k,v in docometre.experiments.items() if re.search("isSignal$", k) and v == "1"});
		# signals  = [re.sub("\.\w+$", "", signal) for signal in signals];
		# signals  = [re.sub("^\w+\.\w+\.", "", signal) for signal in signals];
		#
		# categories = list({k:v for k,v in docometre.experiments.items() if re.search("isCategory$", k) and v == "1"});
		# categories  = [re.sub("\.\w+$", "", category) for category in categories];
		# categories  = [re.sub("^\w+\.\w+\.", "", category) for category in categories];
		#
		# events = list({k:v for k,v in docometre.experiments.items() if re.search("isEvent$", k) and v == "1"});
		# events  = [re.sub("\.\w+$", "", event) for event in events];
		# events  = [re.sub("^\w+\.\w+\.", "", event) for event in events];
		#
		# channels = list({k:v for k,v in docometre.experiments.items() if re.search("isSignal$", k)});
		# channels  = [re.sub("\.\w+$", "", channel) for channel in channels];
		# channels  = [re.sub("^\w+\.\w+\.", "", channel) for channel in channels];
		#
		# # channels = signals + categories + events;
		#
		# print(signals);
		# print(categories);
		# print(events);
		# print(channels);
		#
		# channels = docometre.getChannels("ReachabilityCoriolis.PreTestFull");
		# print(channels);
		#
		# values = docometre.getVector("docometre.experiments[\"ReachabilityCoriolis.PreTestFull.Category1.TrialsList\"]", "i", -1, -1, -1);
		# print(values);
		#
		# nbTrials = len(docometre.experiments["ReachabilityCoriolis.PreTestFull.CAN_FrameID.Values"]);
		# print(nbTrials);
		#
		# values = docometre.getVector("docometre.experiments[\"ReachabilityCoriolis.PreTestFull.CAN_FrameID.Values\"]", "f", 0, 0, 100);
		# print(values);
		#
		# # Unload channel
		# docometre.unload("ReachabilityCoriolis\.PreTestFull\.CAN_FrameID")
		# channels = docometre.getChannels("ReachabilityCoriolis.PreTestFull");
		# print(channels);
		#
		# # Save Subject
		# startTime = time.time();
		# docometre.saveSubject("^ReachabilityCoriolis\.PreTestFull", "./tests/data/")
		# print("Time to save subject :" + str(time.time() - startTime));
		#
		# # Unload subject
		# docometre.unload("ReachabilityCoriolis\.PreTestFull");
		# print(docometre.experiments);
		#
		# # Load Subject
		# startTime = time.time();
		#
		# #os.chdir(os.getcwd() + '/scripts')
		#
		# docometre.loadSubject("./tests/data/");
		# # print(docometre.experiments);
		# print("Time to load subject : " + str(time.time() - startTime));
		#
		# loadedSubjects = docometre.getLoadedSubjects();
		# print(loadedSubjects);
		#
		#
		# # Rename
		# startTime = time.time();
		# docometre.rename("^tests\.data", "ExperimentName.SubjectName");
		# print("Time to rename : " + str(time.time() - startTime));
		# #print(docometre.experiments)
		# subject = {k:v for k,v in docometre.experiments.items() if re.search("^tests\.data", k) != None}
		# #print(subject)
		#
		# # Compute mean using mask
		# #docometre.experiments["ExperimentName.SubjectName.CAN_Marker1_X.NbFeatures"] = NbFeatures + 1;
		# values = docometre.experiments["ExperimentName.SubjectName.CAN_Marker1_X.Values"];
		# fromValues = docometre.experiments["ExperimentName.SubjectName.CAN_Marker1_X.FrontCut"];
		# toValues = docometre.experiments["ExperimentName.SubjectName.CAN_Marker1_X.EndCut"];
		# columns = numpy.arange(values.shape[1]).reshape(-1,1);
		#
		# fromValues = numpy.zeros(values.shape[0]);
		# #if "From_Beginning" != "fromInputSignal" : fromValues = docometre.experiments["ExperimentName.SubjectName.CAN_Marker1_X.FrontCut"];
		# toValues = values.shape[1]*numpy.ones(values.shape[0]);
		# #if "To_End" != "toInputSignal" : toValues = docometre.experiments["ExperimentName.SubjectName.CAN_Marker1_X.EndCut"];
		#
		#
		# mask = (fromValues <= columns) & (columns < toValues);
		# docometre.experiments["ExperimentName.SubjectName.Feature_MEAN_Values"] = numpy.nansum(values.T*mask, axis = 0)/mask.sum(axis = 0);
		# print(docometre.experiments["ExperimentName.SubjectName.Feature_MEAN_Values"][0])
		# print(docometre.experiments["ExperimentName.SubjectName.Feature_MEAN_Values"][1])
		# print(docometre.experiments["ExperimentName.SubjectName.Feature_MEAN_Values"][2])
		# print(docometre.experiments["ExperimentName.SubjectName.Feature_MEAN_Values"][3])


		#columns = numpy.arange(docometre.experiments["inputSignal" + ".Values"].shape[1]).reshape(-1,1);
		#frontCut = from;
		#endCut = to;
		
		