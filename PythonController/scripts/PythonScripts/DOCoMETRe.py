from py4j.clientserver import ClientServer, JavaParameters, PythonParameters;
import sys;
import os;
import numpy;
import io;
import re;

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
		else:
			pass;

	def loadDataDocometre(self, loadName, dataFilesList, sessionsProperties):
		if(jvmMode): self.gateway.jvm.System.out.println("In loadDataDocometre");
		if ".sau" in dataFilesList:
			pass;
		elif ".samples" in dataFilesList:
			self.loadDataDocometreSAMPLES(loadName, dataFilesList, sessionsProperties);
		else:
			pass;

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
				self.experiments[loadName + "." + channelName + "." + "NbFields"] = "0";
				self.experiments[loadName + "." + channelName + "." + "NbMarkersGroups"] = "0";
				self.experiments[loadName + "." + channelName + "." + "Values"] = numpy.zeros((int(totalTrialsNumber), int(maximumSamples)));

			# Read data
			data = numpy.fromfile(dataFiles[n], dtype="float32");
			if system == "Arduino UNO":
				data = data[2::2];

			sizeData = len(data);
			#if(jvmMode): self.gateway.jvm.System.out.println("sizeData : " + str(sizeData));
			key = loadName + "." + channelName + ".NbSamples." + trialNumber;
			self.experiments[key] = sizeData;
			key = loadName + "." + channelName + ".EndCut." + trialNumber;
			self.experiments[key] = sizeData + 1;
			key = loadName + "." + channelName + ".FrontCut." + trialNumber;
			self.experiments[key] = 0;
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
	
	def unload(self, subjectFullName):
		exec("import re;docometre.experiments = {k:v for k,v in docometre.experiments.items() if re.search('^" + subjectFullName + "', k) == None}");
		
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
		exec(code);

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
			gateway.entry_point.setPythonEntryPoint(docometre);
		else:
			jvmMode = False;
		
	else:
		jvmMode = False;
		
	if(not jvmMode):
		print("We are not in JVM mode");
		jvmMode = False;
		
		docometre = DOCoMETRe(None);
		
		loadName = "ReachabilityCoriolis.PreTestFull";

		fileHandle = io.open("./scripts/PythonScripts/dataFilesList.txt", "r", encoding="utf-8");		
		dataFilesList = fileHandle.read();
		fileHandle.close();
		
		fileHandle = io.open("./scripts/PythonScripts/sessionsProperties.txt", "r", encoding="utf-8");		
		sessionPropertiesString = fileHandle.read();
		sessionProperties = eval(sessionPropertiesString);
		fileHandle.close();

		docometre.loadData("DOCOMETRE", loadName, dataFilesList, sessionProperties);
		
		# Some infos
		print(docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.Criteria"]);
		print(docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.isSignal"]);
		print(docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.isCategory"]);
		print(docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.isEvent"]);
		print(docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.TrialsList"]);
		print(21 in docometre.experiments["ReachabilityCoriolis.PreTestFull.Category1.TrialsList"]);
		
		# Test if subject is loaded
		filteredDictionnary = {k:v for k,v in docometre.experiments.items() if re.search("^ReachabilityCoriolis\.PreTestFull\.", k)};
		testLoaded = len(filteredDictionnary) > 0;
		print(testLoaded);
		
		# Test evaluate 1
		# expression = "len({k:v for k,v in docometre.experiments.items() if re.search(\"^" + "ReachabilityCoriolis\.PreTestFull" + "\.\", k)})";
		#response = docometre.evaluate(expression);
		# print(response);
		
		# Test evaluate 1
		# expression = "len({k:v for k,v in docometre.experiments.items() if re.search(\"^" + "ReachabilityCoriolis\.PreTestFull" + "\.\", k)}) > 0";
		# response = docometre.evaluate(expression);
		# print(response);
		
		# Unload subject
		#print(docometre.experiments);
		#docometre.unload("ReachabilityCoriolis\.PreTestFull");
		#print(docometre.experiments);
		
		
		# Get channels, signals, categories or events names
		keys = docometre.experiments.keys();
		
		signals = list({k:v for k,v in docometre.experiments.items() if re.search("isSignal$", k) and v == "1"});
		signals  = [re.sub("\.\w+$", "", signal) for signal in signals];
		signals  = [re.sub("^\w+\.\w+\.", "", signal) for signal in signals];
		
		categories = list({k:v for k,v in docometre.experiments.items() if re.search("isCategory$", k) and v == "1"});
		categories  = [re.sub("\.\w+$", "", category) for category in categories];
		categories  = [re.sub("^\w+\.\w+\.", "", category) for category in categories];
		
		events = list({k:v for k,v in docometre.experiments.items() if re.search("isEvent$", k) and v == "1"});
		events  = [re.sub("\.\w+$", "", event) for event in events];
		events  = [re.sub("^\w+\.\w+\.", "", event) for event in events];
		
		channels = list({k:v for k,v in docometre.experiments.items() if re.search("isSignal$", k)});
		channels  = [re.sub("\.\w+$", "", channel) for channel in channels];
		channels  = [re.sub("^\w+\.\w+\.", "", channel) for channel in channels];
		
		# channels = signals + categories + events;
		
		print(signals);
		print(categories);
		print(events);
		print(channels);
		
		channels = docometre.getChannels("ReachabilityCoriolis.PreTestFull");
		print(channels);
		
		values = docometre.getVector("docometre.experiments[\"ReachabilityCoriolis.PreTestFull.Category1.TrialsList\"]", "i", -1, -1, -1);
		print(values);
		
		nbTrials = len(docometre.experiments["ReachabilityCoriolis.PreTestFull.CAN_FrameID.Values"]);
		print(nbTrials);
		
		values = docometre.getVector("docometre.experiments[\"ReachabilityCoriolis.PreTestFull.CAN_FrameID.Values\"]", "f", 0, 0, 100);
		print(values);
		
		# Unload channel
		docometre.unload("ReachabilityCoriolis\.PreTestFull\.CAN_FrameID")
		channels = docometre.getChannels("ReachabilityCoriolis.PreTestFull");
		print(channels);
	
