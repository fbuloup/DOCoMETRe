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
import matplotlib;

class DOCoMETRe(object):

	global jvmMode;
	global callback_queue;

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
			if(jvmMode): self.gateway.jvm.System.out.println("Data files format not hanled with Python");

	def loadDataDocometre(self, loadName, dataFilesList, sessionsProperties):
		if(jvmMode): self.gateway.jvm.System.out.println("In loadDataDocometre");
		if ".sau" in dataFilesList:
			if(jvmMode): self.gateway.jvm.System.out.println("For now, sau files are not handled with Python");
		elif ".samples" in dataFilesList:
			self.loadDataDocometreSAMPLES(loadName, dataFilesList, sessionsProperties);
		elif ".adw" in dataFilesList:
			self.loadDataDocometreADW(loadName, dataFilesList);
		else:
			if(jvmMode): self.gateway.jvm.System.out.println("Data files format not hanled with Python");
	
	def loadDataDocometreADW(self, loadName, dataFilesList):
		if(jvmMode):
			self.gateway.jvm.System.out.println("In loadDataDocometreADW");
			self.gateway.jvm.System.out.println(dataFilesList);
		
		file = open(dataFilesList, "rb")
		
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
		
		nbTotalCategories = nbSessions*nbConditions*nbSeqTypes
		
		criteria = ["" for _ in range(nbTrials)]
		createdCategories = dict();
		
		for currentTrialNumber in range(nbTrials):
			currentSession = struct.unpack('i', file.read(4))[0]
			currentCondition = struct.unpack('i', file.read(4))[0]
			currentSeqType = struct.unpack('i', file.read(4))[0]
			nbChannelsInTrial = struct.unpack('i', file.read(4))[0]
			criteria = sessionsNames[currentSession-1] + '_' + seqTypesNames[currentSeqType-1]
			if criteria in createdCategories:
			    append = False
			    trialsList = createdCategories[criteria]
			    if isinstance(trialsList, numpy.ndarray):
			        if int(currentTrialNumber + 1) not in trialsList:
			            append = True
			    if append:
			        trialsList = numpy.append(trialsList, currentTrialNumber+1)
			else:
			    trialsList = numpy.array(currentTrialNumber+1)
			createdCategories[criteria] = trialsList
			if nbChannelsInTrial > 0:
			    for currentChannelNumber in range(nbChannelsInTrial):
			        channelNumber = struct.unpack('i', file.read(4))[0] - 1
			        channelName = channelsNames[channelNumber]
			        sampleFrequency = struct.unpack('f', file.read(4))[0]
			        nbSamples = struct.unpack('i', file.read(4))[0]
			        if nbSamples > 0:
			            newValues = numpy.fromfile(file, dtype="float32", count=nbSamples)
			            try:
			                values = self.experiments[loadName + "." + channelName + "." + "Values"]
			                currentNbSamples = values.shape[1]
			                if(currentNbSamples > nbSamples):
			                	newValues.resize((1, currentNbSamples), refcheck=False)
			                elif(currentNbSamples < nbSamples):
			                	values.resize((nbTrials, nbSamples), refcheck=False)
			                self.experiments[loadName + "." + channelName + "." + "Values"][currentTrialNumber][:] = newValues
			            except KeyError:
			                self.experiments[loadName + "." + channelName + "." + "Values"] = numpy.zeros((int(nbTrials), int(nbSamples)))
			                self.experiments[loadName + "." + channelName + "." + "NbSamples"] = nbSamples*numpy.ones(int(nbTrials))
			                self.experiments[loadName + "." + channelName + "." + "FrontCut"] = numpy.zeros(int(nbTrials))
			                self.experiments[loadName + "." + channelName + "." + "EndCut"] = nbSamples*numpy.ones(int(nbTrials))
			                self.experiments[loadName + "." + channelName + "." + "Values"][currentTrialNumber][:] = newValues
			            self.experiments[loadName + "." + channelName + "." + "isSignal"] = '1'
			            self.experiments[loadName + "." + channelName + "." + "isCategory"] = '0'
			            self.experiments[loadName + "." + channelName + "." + "isEvent"] = '0'
			            self.experiments[loadName + "." + channelName + "." + "NbFeatures"] = '0'
			            self.experiments[loadName + "." + channelName + "." + "NbMarkersGroups"] = '0'
			            self.experiments[loadName + "." + channelName + "." + "SampleFrequency"] = sampleFrequency
			
		n = 1
		for criteria in createdCategories:
		    values = createdCategories[criteria];
		    self.experiments[loadName + ".Category" + str(n) + ".Criteria"] = criteria
		    self.experiments[loadName + ".Category" + str(n) + ".TrialsList"] = values
		    self.experiments[loadName + ".Category" + str(n) + ".isSignal"] = "0"
		    self.experiments[loadName + ".Category" + str(n) + ".isCategory"] = "1"
		    self.experiments[loadName + ".Category" + str(n) + ".isEvent"] = "0"
		    n = n + 1
		
		# self.experiments[loadName + ".Criteria.Names"] = criteria
		
		file.close()
	
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
	
	def runScript(self, code, runInMainThread):
		if(jvmMode): 
			gateway.jvm.System.out.println("Run script in main thread : " + str(runInMainThread));
			gateway.jvm.System.out.println("Script code : " + code);
		if runInMainThread:
			if(jvmMode): gateway.jvm.System.out.println("Add callback to queue");
			callback_queue.put(lambda: runScriptMainThread(code))
		else:
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
		
def runScriptMainThread(code):
    if(jvmMode): gateway.jvm.System.out.println("In runScriptMainThread : " + code);
    exec(code)

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
			callback()		
			gateway.jvm.System.out.println("Callback called");
		
	if(not jvmMode):
		docometre = DOCoMETRe(None);
		D = docometre.experiments;
		from platform import python_version; 
		D['pythonVersion'] = python_version();
