from py4j.clientserver import ClientServer, JavaParameters, PythonParameters;
import sys;
import os;
import numpy;
import io;
import re;
import time;
import struct;
import argparse;
import queue;
import threading;
import matplotlib;

class DOCoMETRe(object):

	global jvmMode;
	global callback_queue;

	def __init__(self, gateway):
	 	self.gateway = gateway;
	 	self.experiments = dict();
	 	if(jvmMode): print("In __init__ gateway");

	def shutDownServer(self, object):
		if(jvmMode): print("In shutdown server");
		# Ask the main thread to leave its loop and to shut down the gateway
		if(jvmMode): callback_queue.put(None);

	def toString(self):
		if(jvmMode): print("In toString");
		return "This is DOCoMETRe Python Entry Point";

	def loadData(self, dataType, loadName, dataFilesList, sessionsProperties):
		sessionsProperties = self.decodeSessionsProperties(sessionsProperties);
		if(dataType == "DOCOMETRE"):
			self.loadDataDocometre(loadName, dataFilesList, sessionsProperties);
		elif(dataType == "OPTITRACK_TYPE_1"):
			self.loadDataOptitrackType1(loadName, dataFilesList);
		elif(dataType == "OPTITRACK_TYPE_2"):
			self.loadDataOptitrackType2(loadName, dataFilesList);
		else:
			if(jvmMode): print("Data files format not handeled with Python");

	def decodeSessionsProperties(self, sessionsProperties):
		# Java sends sessions properties as a single string "key<US>value<RS>key<US>value..."
		# (see PythonEngine.encodeSessionsProperties()) : reading a Java map would cost a Java round trip for each access.
		# A dictionary (standalone mode) is returned as is.
		if not isinstance(sessionsProperties, str): return sessionsProperties;
		properties = dict();
		if sessionsProperties == "": return properties;
		for record in sessionsProperties.split("\u001e"):
			key, separator, value = record.partition("\u001f");
			properties[key] = value;
		return properties;

	def loadDataDocometre(self, loadName, dataFilesList, sessionsProperties):
		if(jvmMode): print("In loadDataDocometre");
		if ".sau" in dataFilesList:
			if(jvmMode): print("For now, sau files are not handeled with Python");
		elif ".samples" in dataFilesList:
			self.loadDataDocometreSAMPLES(loadName, dataFilesList, sessionsProperties);
		elif ".adw" in dataFilesList:
			self.loadDataDocometreADW(loadName, dataFilesList);
		else:
			if(jvmMode): print("Data files format not handeled with Python");
	
	def loadDataDocometreADW(self, loadName, dataFilesList):
		if(jvmMode):
			print("In loadDataDocometreADW");
			print(dataFilesList);
		
		createdCategories = dict();
		# For each channel : trial index -> samples
		channelsValues = dict();
		channelsSampleFrequency = dict();
		
		with open(dataFilesList, "rb") as file:
			
			# Nb channels
			nbChannels = struct.unpack('i', file.read(4))[0]
			channelsNames = self.read_names(file, nbChannels)
		
			# Nb sessions
			nbSessions = struct.unpack('i', file.read(4))[0]
			sessionsNames = self.read_names(file, nbSessions)
			
			# Nb conditions
			nbConditions = struct.unpack('i', file.read(4))[0]
			conditionsNames = self.read_names(file, nbConditions)
			
			# Nb seq. type
			nbSeqTypes = struct.unpack('i', file.read(4))[0]
			seqTypesNames = self.read_names(file, nbSeqTypes)
			
			# Nb trials
			nbTrials = struct.unpack('i', file.read(4))[0]
			
			for currentTrialNumber in range(nbTrials):
				currentSession = struct.unpack('i', file.read(4))[0]
				currentCondition = struct.unpack('i', file.read(4))[0]
				currentSeqType = struct.unpack('i', file.read(4))[0]
				nbChannelsInTrial = struct.unpack('i', file.read(4))[0]
				criteria = sessionsNames[currentSession-1] + '_' + seqTypesNames[currentSeqType-1]
				trialsList = createdCategories.setdefault(criteria, list())
				if currentTrialNumber + 1 not in trialsList:
					trialsList.append(currentTrialNumber + 1)
				for currentChannelNumber in range(nbChannelsInTrial):
					channelNumber = struct.unpack('i', file.read(4))[0] - 1
					channelName = channelsNames[channelNumber]
					sampleFrequency = struct.unpack('f', file.read(4))[0]
					nbSamples = struct.unpack('i', file.read(4))[0]
					if nbSamples > 0:
						channelsValues.setdefault(channelName, dict())[currentTrialNumber] = numpy.fromfile(file, dtype="float32", count=nbSamples)
						channelsSampleFrequency[channelName] = sampleFrequency
		
		# Values arrays are created once the maximum number of samples of each channel is known :
		# trials may not have the same number of samples
		for channelName, trialsValues in channelsValues.items():
			prefix = loadName + "." + channelName + "."
			maximumSamples = max(len(trialValues) for trialValues in trialsValues.values())
			values = numpy.zeros((nbTrials, maximumSamples))
			nbSamples = numpy.zeros(nbTrials)
			for trialIndex, trialValues in trialsValues.items():
				values[trialIndex, 0:len(trialValues)] = trialValues
				nbSamples[trialIndex] = len(trialValues)
			self.experiments[prefix + "Values"] = values
			self.experiments[prefix + "NbSamples"] = nbSamples
			self.experiments[prefix + "FrontCut"] = numpy.zeros(nbTrials)
			self.experiments[prefix + "EndCut"] = nbSamples.copy()
			self.experiments[prefix + "isSignal"] = '1'
			self.experiments[prefix + "isCategory"] = '0'
			self.experiments[prefix + "isEvent"] = '0'
			self.experiments[prefix + "NbFeatures"] = '0'
			self.experiments[prefix + "NbMarkersGroups"] = '0'
			self.experiments[prefix + "SampleFrequency"] = channelsSampleFrequency[channelName]
			
		n = 1
		for criteria in createdCategories:
			self.experiments[loadName + ".Category" + str(n) + ".Criteria"] = criteria
			self.experiments[loadName + ".Category" + str(n) + ".TrialsList"] = numpy.array(createdCategories[criteria])
			self.experiments[loadName + ".Category" + str(n) + ".isSignal"] = "0"
			self.experiments[loadName + ".Category" + str(n) + ".isCategory"] = "1"
			self.experiments[loadName + ".Category" + str(n) + ".isEvent"] = "0"
			n = n + 1
	
	def read_names(self, file_handle, nb_names):
	    names = ["" for _ in range(nb_names)]
	    for i in range(nb_names):
	        char = ""
	        channel_name = ""
	        while char != "|":
	            channel_name = channel_name + char
	            char = struct.unpack('s', file_handle.read(1))[0].decode("utf-8")
	        names[i] = channel_name
	    return names

	def loadDataOptitrackType1(self, loadName, dataFilesAbsolutePath):
		if(jvmMode): print("In loadDataOptitrackType1");
		
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
			
	def loadDataOptitrackType2(self, loadName, dataFilesAbsolutePath):
		if(jvmMode): print("In loadDataOptitrackType2");
		
		createdCategories = dict();
		createdChannelsNames = list();
		channelNames = list();
		nbSamples = 0;
		firstTime = True;
		
		files = dataFilesAbsolutePath.split(";");
		nbFiles = len(files);
		nbTrials = nbFiles;
		
		# Get nb Samples, sample frequency and signals names on first file     
		fid = open(files[0], "r");
		line = fid.readline();
		tempValue = line.split(",");
		nbSamples = int(tempValue[15]);
		sampleFrequency = float(tempValue[7]);
		fid.readline();
		fid.readline();
		line = fid.readline();
		line = re.sub("^,Name,", "", line);
		channelsPrefix = line.split(",");
		for p in range(0, len(channelsPrefix)):
			channelsPrefix[p] = re.sub(":.*", "", channelsPrefix[p]).strip();
			if(jvmMode): 
				print("Channel prefix " + str(p) + " : " + channelsPrefix[p].strip());
		fid.readline();
		fid.readline();
		fid.readline();
		line = fid.readline();
		tempValue = line.split(",");
		prefixIndex = 0;
		for p in range(0, len(tempValue)):
			tempValue2 = tempValue[p]
			if tempValue2 != "Time (Seconds)" and tempValue2 != "Frame" and tempValue2 != "" :
				channelNames.append(channelsPrefix[prefixIndex] + "_" + tempValue2.strip());
				if(jvmMode): 
					print("Add channel name : " + channelsPrefix[prefixIndex] + "_" + tempValue2.strip());
				prefixIndex+=1;
		fid.close();
		
		for trialNumber in range(1, nbTrials+1):
			data = numpy.loadtxt(files[trialNumber-1], delimiter=',', skiprows=8);
			data = numpy.delete(data, [0, 1], axis = 1);
			
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
				self.experiments[key][trialNumber - 1] = sizeData;
				key = loadName + "." + channelName + ".EndCut";
				self.experiments[key][trialNumber - 1] = sizeData;
				values = self.experiments[loadName + "." + channelName + ".Values"];
				values[trialNumber - 1][0:sizeData] = data[:, numChannel];
				numChannel+=1;
				if(jvmMode): 
					print("Create channel : " + channelName);
						
			
		self.experiments[loadName + ".Category1.Criteria"] = "Session1";
		self.experiments[loadName + ".Category1.TrialsList"] = numpy.arange(nbTrials) + 1;
		self.experiments[loadName + ".Category1.isSignal"] = "0";
		self.experiments[loadName + ".Category1.isCategory"] = "1";
		self.experiments[loadName + ".Category1.isEvent"] = "0";		
				
	def loadDataDocometreSAMPLES(self, loadName, dataFilesList, sessionsProperties):
		if(jvmMode): print("In loadDataDocometreSAMPLES");
		# if(jvmMode): print(sessionsProperties);

		prefix_QN = "_DATA_FILES_NAMES_PREFIX";
		baseTrialsNumber_QN = "_BASE_TRIALS_NUMBER";

		createdCategories = dict();
		createdChannels = list();

		dataFiles = dataFilesList.split(";");
		nbDataFiles = len(dataFiles);

		maximumSamples = sessionsProperties["MAXIMUM_SAMPLES"];
		totalTrialsNumber = sessionsProperties["TOTAL_TRIALS_NUMBER"];
		paddWithLastValue = sessionsProperties["PADD_WITH_LAST_VALUE_RATHER_THAN_ZERO"];
		if(jvmMode):
			if paddWithLastValue == "true":
				print("Padd empty samples with last value");

		for n in range(0, nbDataFiles):
			segments = dataFiles[n].split(os.path.sep);

			# Get session name for criteria, trial number and prefix
			sessionName = segments[len(segments) - 3];
			trialName = segments[len(segments) - 2];
			key = os.path.dirname(os.path.abspath(dataFiles[n]))
			process = sessionsProperties[key + "_PROCESS"];
			# if(jvmMode): print("nbDataFiles : " + str(nbDataFiles) + " fichier : " + str(n+1) + " sessionName : " + sessionName + " process : " + process + " - " + dataFiles[n]);
			criteria = sessionName + "." + process;
			if sessionName + prefix_QN in sessionsProperties:
				if sessionsProperties[sessionName + prefix_QN] != "":
					criteria = sessionsProperties[sessionName + prefix_QN];
					criteria = criteria + "." + sessionName + "." + process;
			# if(jvmMode): print("nbDataFiles : " + str(nbDataFiles) + " fichier : " + str(n+1) + " critere : " + criteria);
			# if(jvmMode): print(str(n) + " - " + dataFiles[n]);
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
				#if(jvmMode): print(trialsList);

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
			#if(jvmMode): print("sizeData : " + str(sizeData));
			key = loadName + "." + channelName + ".NbSamples";
			self.experiments[key][int(trialNumber) - 1] = sizeData;
			key = loadName + "." + channelName + ".FrontCut";
			self.experiments[key][int(trialNumber) - 1] = 0;
			key = loadName + "." + channelName + ".EndCut";
			self.experiments[key][int(trialNumber) - 1] = sizeData;
			values = self.experiments[loadName + "." + channelName + ".Values"];
			#if(jvmMode): print("size values : " + str(len(values[int(trialNumber) - 1])));
			values[int(trialNumber) - 1][0:sizeData] = data;
			if paddWithLastValue == "true":
				values[int(trialNumber) - 1][sizeData-1:] = data[sizeData-1];
			
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
		# if(jvmMode): print("Evaluate : " + expression);
		return str(eval(expression));
	
	def unload(self, fullName):
		# fullName is a regular expression (dots escaped by Java) : it is used directly, not inserted in generated code
		keysToDelete = [key for key in self.experiments.keys() if re.match("^" + fullName, key) != None];
		for key in keysToDelete:
			self.experiments.pop(key);
		
	def getChannels(self, subjectFullName):
		channels = list({k:v for k,v in self.experiments.copy().items() if re.search("^" + subjectFullName + r"\.\w+\.isSignal$", k)});
		channels  = [re.sub(r"\.\w+$", "", channel) for channel in channels];
		channels  = [re.sub(r"^\w+\.\w+\.", "", channel) for channel in channels];
		return ",".join(channels);
	
	def getVector(self, expression, dataType, trialNumber, frontCut, endCut):
		values = eval(expression);
		if(trialNumber > -1):
			values = values[trialNumber][frontCut:endCut];
		
		arrayValues = numpy.array(values);
		
		arrayValues = arrayValues.astype(dataType);
		
		return arrayValues.tobytes();
	
	def runScript(self, code, runInMainThread):
		if(jvmMode): 
			print("Run script in main thread : " + str(runInMainThread));
			print("Script code : " + code);
		if runInMainThread:
			if(jvmMode): print("Add callback to queue");
			# Wait for the end of the script so that Java reads up to date values, and send back its error if any
			task = MainThreadTask(code);
			callback_queue.put(task);
			task.done.wait();
			if task.error is not None: raise task.error;
		else:
			exec(code);
		
	def saveSubject(self, fullSubjectNameRegExp, dataFilesFullPath):
		subject = {k:v for k,v in self.experiments.items() if re.search(fullSubjectNameRegExp, k) != None}
		ndArrayFileNumber = 1;
		file = open(dataFilesFullPath + 'save.data','w');
		for key,value in subject.items():
			# Remove Experiment.Subject prefix
			newKey = re.sub(r"^\w+\.\w+\.", "", key); 
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
				print("Type not handled : " + type(value).__name__ + " for key : " + key);
				
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
						value = re.sub(r"^list\.str\(", "", value);
						value = re.sub(r"\)$", "", value);
						subject[key] = value.split(":");
				key = file.readline().strip();
	
			self.experiments.update(subject);
			
		finally:
			os.chdir(previousWD);
			
		if(jvmMode): 
			print("WD : " + os.getcwd());
			
	def rename(self, keyRegExp, keyReplace):
		subDict = {k:v for k,v in docometre.experiments.items() if re.search(keyRegExp, k) != None}
		for key,value in subDict.items():
			newKey = re.sub(keyRegExp, keyReplace, key);
			docometre.experiments[newKey] = docometre.experiments.pop(key);
		subDict = {k:v for k,v in docometre.experiments.items() if re.search(keyRegExp, k) != None}
		keyReplace = "^" + re.sub(r"\.", r"\.", keyReplace);
		subDict2 = {k:v for k,v in docometre.experiments.items() if re.search(keyReplace, k) != None}
		#if(jvmMode): 
		#	print("Is dict empty : " + str(any(subDict)) + " for " + keyRegExp);
		#	print("Is dict empty : " + str(any(subDict2)) + " for " + keyReplace);
		return (not(any(subDict)) and any(subDict2))

	def getLoadedSubjects(self):
		keys = {k for k,v in docometre.experiments.items() if re.search(r'^\w+\.\w+\.\w+\.isSignal', k) != None};
		loadedSubjects = set();
		for key in keys:
			newKey = re.sub(r"\.\w+\.isSignal$", "", key);
			loadedSubjects.add(newKey);
		if(len(loadedSubjects) > 0):
			return ":".join(loadedSubjects);
		return "";
		
	class Java:
		implements = ["fr.univamu.ism.docometre.python.PythonEntryPoint"]
		
def runScriptMainThread(code):
    if(jvmMode): print("In runScriptMainThread : " + code);
    exec(code)

class MainThreadTask(object):
	# A script to be run by the main thread, which signals its end to the waiting gateway thread

	def __init__(self, code):
		self.code = code;
		self.error = None;
		self.done = threading.Event();

	def __call__(self):
		try:
			runScriptMainThread(self.code);
		except BaseException as error:
			# Never leave the main loop because of a script error : it is sent back to Java
			self.error = error;
		finally:
			self.done.set();

if __name__ == "__main__":
	
	print("Current working folder : " + os.getcwd());
	
	#experiments = dict();
	parser = argparse.ArgumentParser()
	parser.add_argument("-m", "--jvm", default=False, action="store_true", help = "Specifies the module must be launched with a Java/Python gateway");
	parser.add_argument("-j", "--javaPort", type=int, required = False, default = 25333, help = "Java/Python gateway's Java port number (an integer ideally in [1024, 49151], default is 25333).");
	parser.add_argument("-p", "--pythonPort", type=int, required = False, default = 25334, help = "Java/Python gateway's Python port number (an integer ideally in [1024, 49151], default is 25334).");
	args = parser.parse_args(sys.argv[1:]);
	print("Args -> JVM mode :", args.jvm, "- Java port :", args.javaPort, "- Python port :", args.pythonPort);
	
	jvmMode = args.jvm
	
	if(jvmMode):
		callback_queue = queue.Queue()
		gateway = ClientServer(java_parameters = JavaParameters(port=args.javaPort), python_parameters = PythonParameters(port=args.pythonPort));	
		docometre = DOCoMETRe(gateway);
		D = docometre.experiments;
		gateway.entry_point.setPythonEntryPoint(docometre);	
		while True:
			callback = callback_queue.get(block=True) # Block until a callback is available
			if callback is None: break; # Sent by shutDownServer
			callback()		
			print("Callback called");
		# Let shutDownServer answer Java before closing the gateway
		time.sleep(0.1);
		try:
			gateway.shutdown();
		except Exception as error:
			print("Error while shutting down gateway : " + str(error));
		
	if(not jvmMode):
		docometre = DOCoMETRe(None);
		D = docometre.experiments;
		from platform import python_version; 
		D['pythonVersion'] = python_version();
