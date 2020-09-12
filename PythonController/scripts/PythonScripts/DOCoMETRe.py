from py4j.clientserver import ClientServer, JavaParameters, PythonParameters

class DOCoMETRe(object):

	def __init__(self, gateway):
		self.gateway = gateway;
		gateway.jvm.System.out.println("In __init__ gateway");
		
	def shutDownServer(self, object):
		self.gateway.jvm.System.out.println("In shutdown server");
		# Last chance to do anything before JVM shuts down server
		# ...
		
	def toString(self):
		self.gateway.jvm.System.out.println("In toString");
		return "This is DOCoMETRe Python Entry Point";
				
	class Java:
		implements = ["fr.univamu.ism.docometre.python.PythonEntryPoint"]
        
if __name__ == "__main__":
	gateway = ClientServer(java_parameters = JavaParameters(), python_parameters = PythonParameters());
	docometre = DOCoMETRe(gateway);
	gateway.entry_point.setPythonEntryPoint(docometre);