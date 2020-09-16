from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import os

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

	class Java:
		implements = ["fr.univamu.ism.docometre.python.PythonEntryPoint"]

if __name__ == "__main__":
	gateway = ClientServer(java_parameters = JavaParameters(), python_parameters = PythonParameters());
	#gateway = 10
	docometre = DOCoMETRe(gateway);
	#docometre.loadData("DOCOMETRE", None, None, None);
	gateway.entry_point.setPythonEntryPoint(docometre);
