from py4j.clientserver import ClientServer, JavaParameters, PythonParameters;
import os;
import numpy;

class DOCoMETRe(object):

	def __init__(self, gateway):
	 	self.gateway = gateway;
	 	gateway.jvm.System.out.println("In __init__ gateway");

	def shutDownServer(self, object):
		self.gateway.jvm.System.out.println("In shutdown server");

	def toString(self):
		self.gateway.jvm.System.out.println("In toString");
		return "This is DOCoMETRe Python Entry Point";

	def loadData(self, dataType, loadName, dataFilesList, sessionsProperties):
		if(dataType == "DOCOMETRE"):
			self.loadDataDocometre(loadName, dataFilesList, sessionsProperties);
		else:
			pass;

	def loadDataDocometre(self, loadName, dataFilesList, sessionsProperties):
		self.gateway.jvm.System.out.println("In loadDataDocometre");
		if ".sau" in dataFilesList:
			pass;
		elif ".samples" in dataFilesList:
			self.loadDataDocometreSAMPLES(loadName, dataFilesList, sessionsProperties);
		else:
			pass;

	def loadDataDocometreSAMPLES(self, loadName, dataFilesList, sessionsProperties):
		self.gateway.jvm.System.out.println("In loadDataDocometreSAMPLES");
		self.gateway.jvm.System.out.println(sessionsProperties)

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
			#self.gateway.jvm.System.out.println("nbDataFiles : " + str(nbDataFiles) + " fichier : " + str(n+1) + " sessionName : " + sessionName + " process : " + process + " - " + dataFiles[n]);
			criteria = sessionName + "." + process;
			if sessionName + prefix_QN in sessionsProperties:
				criteria = sessionsProperties[sessionName + prefix_QN];
				criteria = criteria + "." + sessionName + "." + process;
			#self.gateway.jvm.System.out.println("nbDataFiles : " + str(nbDataFiles) + " fichier : " + str(n+1) + " critere : " + criteria);
			self.gateway.jvm.System.out.println(str(n) + " - " + dataFiles[n]);
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

				self.gateway.jvm.System.out.println(trialsList);

				if isinstance(trialsList, list):
					if int(trialNumber) not in trialsList:
						append = True;
				else:
					if int(trialNumber) != trialsList:
						append = True;

				if append:
					trialsList = numpy.append(trialsList, int(trialNumber));
			else:

				trialsList = int(trialNumber);

			createdCategories[criteria] = trialsList;

			if channelName not in createdChannels:
				createdChannels.append(channelName);
				sampleFrequency = sessionsProperties[channelName + "_SF"];
				experiments[loadName + "." + channelName + "." + "SampleFrequency"] = sampleFrequency;
				experiments[loadName + "." + channelName + "." + "isSignal"] = "1";
				experiments[loadName + "." + channelName + "." + "isCategory"] = "0";
				experiments[loadName + "." + channelName + "." + "isEvent"] = "0";
				experiments[loadName + "." + channelName + "." + "nbFields"] = "0";
				experiments[loadName + "." + channelName + "." + "nbMarkers"] = "0";
				experiments[loadName + "." + channelName + "." + "Values"] = numpy.zeros((int(totalTrialsNumber), int(maximumSamples)));

			# Read data
			data = numpy.fromfile(dataFiles[n], dtype="float32");
			if system == "Arduino UNO":
				data = data[2::2];

			sizeData = len(data);
			#self.gateway.jvm.System.out.println("sizeData : " + str(sizeData));
			key = loadName + "." + channelName + ".NbSamples." + trialNumber;
			experiments[key] = sizeData;
			key = loadName + "." + channelName + ".EndCut." + trialNumber;
			experiments[key] = sizeData + 1;
			key = loadName + "." + channelName + ".FrontCut." + trialNumber;
			experiments[key] = 0;
			values = experiments[loadName + "." + channelName + ".Values"];
			#self.gateway.jvm.System.out.println("size values : " + str(len(values[int(trialNumber) - 1])));
			values[int(trialNumber) - 1][0:sizeData] = data;

		n = 1;
		for criteria in createdCategories:
			values = createdCategories[criteria];
			values.sort();
			experiments[loadName + ".Category" + str(n) + ".Criteria"] = criteria;
			experiments[loadName + ".Category" + str(n) + ".TrialsList"] = values;
			experiments[loadName + "." + channelName + "." + "isSignal"] = "0";
			experiments[loadName + "." + channelName + "." + "isCategory"] = "1";
			experiments[loadName + "." + channelName + "." + "isEvent"] = "0";
			n = n + 1;

	class Java:
		implements = ["fr.univamu.ism.docometre.python.PythonEntryPoint"]

if __name__ == "__main__":
	experiments = dict();
	gateway = ClientServer(java_parameters = JavaParameters(), python_parameters = PythonParameters());
	#gateway = 10
	docometre = DOCoMETRe(gateway);
	#docometre.loadData("DOCOMETRE", None, None, None);
	gateway.entry_point.setPythonEntryPoint(docometre);
