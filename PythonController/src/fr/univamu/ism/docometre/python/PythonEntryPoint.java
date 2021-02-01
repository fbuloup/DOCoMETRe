package fr.univamu.ism.docometre.python;

public interface PythonEntryPoint {
	
	static String DATA_TYPE_INT = "i";
	static String DATA_TYPE_FLOAT = "f";
	static String DATA_TYPE_DOUBLE = "d";
	
	void shutDownServer(Object object);
	void loadData(Object dataType, Object loadName, Object dataFilesList, Object sessionsProperties);
	String evaluate(Object expression);
	void unload(Object expression);
	String getChannels(Object subjectFullName);
	byte[] getVector(Object expression, Object dataType, Object trialNumber, Object frontCut, Object endCut);
	void runScript(Object code);
	void saveSubject(Object subjectFullNameRegExp, Object saveFilesFullPath);
	void loadSubject(Object saveFilesFullPath);
	boolean rename(String keyRegExp, String keyReplace);
	String getLoadedSubjects();

}
