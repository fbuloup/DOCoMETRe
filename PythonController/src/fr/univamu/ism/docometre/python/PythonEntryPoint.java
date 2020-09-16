package fr.univamu.ism.docometre.python;

public interface PythonEntryPoint {
	
	void shutDownServer(Object object);
	void loadData(Object dataType, Object loadName, Object dataFilesList, Object sessionsProperties);

}
