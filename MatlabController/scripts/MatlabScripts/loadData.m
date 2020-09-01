function subject = loadData(experimentType, dataFilesList)
% This function is the entry point used to create Analyse data model from an external model
% in Matlab or Octave workspace. It redirects to the necessary function depending on the
% experimentType value.
% params :
% experimentType (string) :  contains project type (analyse, Text file, ice, doco...)
% experimentName (string) : project name 
% localSubjectName (string) : subject name 
% dataFilesList (string) : data files list. Theses files names are "comma separated".

switch experimentType
    case 'DOCOMETRE'
        subject = loadDataDocometre(dataFilesList);
    
    case 'LAVAL_ANALYSE'
        loadDataLaval_Analyse(experimentName, localSubjectName, dataFilesList);
        
    case 'ICE'
        loadDataICE(experimentName, localSubjectName, dataFilesList);
    
    case 'ANALYSE'
    	load(dataFilesList);
    	assignin('base', 'subjectName', subjectName);
    	subjectFullName = [char(experimentName) '.' char(localSubjectName)];
    	evalin('base',[subjectFullName ' = subjectName;']);
    	evalin('base','clear subjectName;');
    	
    case 'TEXT_FILE_1'
    	loadDataTextFile1(experimentName,localSubjectName, dataFilesList);
    
    otherwise
	    returnCode = 1;
	    if( exist('loadDataExtension') == 2 ) 
	    	returnCode = loadDataExtension(experimentType, experimentName,...
	    									 localSubjectName, dataFilesList)
	    end
	    if(returnCode ~= 0)
	    	errorMessage = [mfilename '.m -> '];
	    	errorMessage = [errorMessage 'Unknown Data File Type'];
	        assignin('base', 'errorMessage', errorMessage);
	    end
end