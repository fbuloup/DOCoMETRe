function subject = loadData(experimentType, dataFilesList, varargin)
% This function is the entry point used to create Analyse data model from an external model
% in Matlab workspace. It redirects to the necessary function depending on the
% experimentType value.
% params :
% experimentType (string) :  contains project type (analyse, Text file, ice, doco...)
% dataFilesList (string) : data files list. Theses files names are "comma separated".
% varargin : depends on experiment type

switch experimentType
    case 'DOCOMETRE'
    	% Experiment informations keys : varargin{1}
    	% Experiment informations values :varargin{2}
        subject = loadDataDocometre(dataFilesList, varargin{1}, varargin{2});
    
    % case 'LAVAL_ANALYSE'
    %    loadDataLaval_Analyse(experimentName, localSubjectName, dataFilesList);
        
    % case 'ICE'
    %    loadDataICE(experimentName, localSubjectName, dataFilesList);
    
    % case 'ANALYSE'
    %	load(dataFilesList);
    % 	assignin('base', 'subjectName', subjectName);
    %	subjectFullName = [char(experimentName) '.' char(localSubjectName)];
    %	evalin('base',[subjectFullName ' = subjectName;']);
    %	evalin('base','clear subjectName;');
    	
    case 'OPTITRACK_TYPE_1'
    	subject = loadDataOptitrackType1(dataFilesList);
        
    case 'COLUMN_DATA_FILE'
        subject = loadDataColumnFile(dataFilesList);
    
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