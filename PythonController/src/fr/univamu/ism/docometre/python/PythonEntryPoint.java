package fr.univamu.ism.docometre.python;

public interface PythonEntryPoint {
	
	void shutDownServer(Object object);
	void loadData(Object dataType, Object loadName, Object dataFilesList, Object sessionsProperties);
	String evaluate(Object expression);
	void unload(Object expression);
	String getChannels(Object subjectFullName);
	byte[] getVector(Object expression);

}
