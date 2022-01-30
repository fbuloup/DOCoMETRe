function  loadDataLaval_Analyse(projectName,subjectName, dataFileList)
% This function is the entry point used to create the Analyse data model
% in Matlab workspace from the old analyse data model. For Old Analyse ,
% MAT files, only allow one file per subject, as all trials contained in one file.
% params :
% projectName (string) : the project name !
% subjectName (string) : the subject name !
% dataFileList (string) : the data file list ! the file names are "comma separated"

A = load(dataFileList);


%For every channel
for i = 1: A.vg.nad    
    signal = [];                                    %remove traces of previous use of local variable                     
    signalName = cleanup(A.hdchnl(i,1).adname);     %remove any characters matlab does not tolerate in variable names    
    fullSignalName = [projectName,'.',subjectName,'.',signalName];
    signal.isSignal = 1;
    signal.isCategory = 0;;
    signal.isEvent = 0;
    signal.SampleFrequency = A.hdchnl(i,1).rate;    %assume freq for one channel is same for all trials
    signal.NbFields = 0;

    %prepare new fields (empty for now)
    mxNbMk = max([A.hdchnl(i,:).npoints]);
    signal.NbMarkers = mxNbMk;
    
    prefix = 'Pt';
    if mxNbMk
        for nbM = 1:mxNbMk
            eval(['signal.Marker' num2str(nbM) '_Values =  [];']);
            eval(['signal.Marker' num2str(nbM) '_Label =  ''' prefix num2str(nbM) ''';']);
        end
    end
    
    %For every trial...
    for j = 1:A.vg.ess
         signal.NbSamples(j) = A.hdchnl(i,j).nsmpls;
         signal.Values(j,1:A.hdchnl(i,j).nsmpls ) = A.dtchnl(1:A.hdchnl(i,j).nsmpls,i,j);
         signal.FrontCut(j) = (A.hdchnl(i,j).frontcut)*signal.SampleFrequency + 1; %reconvert into index from sec
         
        if isfield(A.hdchnl,'endcut')
            signal.EndCut(j) = (A.hdchnl(i,j).endcut)*signal.SampleFrequency;
        else
            signal.EndCut(j) = signal.NbSamples(j);
        end
        
        if A.hdchnl(i,j).npoints  
            
            for k = 1:A.hdchnl(i,j).npoints
                
                temptime = (A.ptchnl(k,A.hdchnl(i,j).point,1) - 1)/signal.SampleFrequency;
                
                tempval = A.dtchnl(A.ptchnl(k,A.hdchnl(i,j).point,1),i,j);
                
                %allMkinfo = [cast(j,'double') cast(temptime,'double') cast(tempval,'double')];  %add extra line for each trial
                allMkinfo = [double(j) double(temptime) double(tempval)];  %add extra line for each trial
                %allMkinfo = [j temptime tempval];  %add extra line for each trial
                eval(['signal.Marker' num2str(k) '_Values =  [signal.Marker' num2str(k) '_Values ; allMkinfo ];']);
            end
            
        end
    end 
    %Create a temp variable with signal values in base workspace with assignin
    %"assignin('base',fullSignalName,signal);" doesn't work : struture variable names are invalid
    assignin('base','tempSignal',signal); % create temporary variable 
    %Affect this temp variable to fullSignalName with evalin
    evalin('base',[fullSignalName,' = tempSignal;']);       
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  configuer categories  
%For every criteria/for every category
for i = 1: size(A.catego,2)
    for j = 1:A.catego(1,i,1).ncat
        categoryName = ['Category',num2str(j)]; %cleanup(A.catego(2,i,j).nom);  %
        fullCategoryName = [projectName,'.',subjectName,'.',categoryName];
        category.isCategory = 1;
        category.isSignal = 0;
        category.isEvent = 0;
        category.TrialsList = find(A.catego(2,i,j).ess);                   
        %category.SessionNum = i; %CONFIRM THIS !!
        category.Criteria = [cleanup(A.catego(1,i,1).nom ),'-',cleanup(A.catego(2,i,j).nom)];
        
        %Create a temp variable with signal values in base workspace with assignin
        %"assignin('base',fullSignalName,category);" doesn't work : structure variable names are invalid
        assignin('base','tempCategory',category); % create temporary variable 
        %Affect this temp variable to fullSignalName with evalin
        evalin('base',[fullCategoryName,' = tempCategory;']);  
    end 
end

% clear temporary variables
evalin('base','clear tempSignal;clear tempCategory');

function outstr = cleanup(instr)
%remove non-alphabetic/numeric characters from name string 

% lastch = findstr(instr, ' ')
i = 1;
count = 1;
			
while i <= length(instr) %lastchar  
    if (double(instr(i))>=65 & double(instr(i))<=90) |(double(instr(i))>=97 &double(instr(i)) <=122)...
            |(double(instr(i))>=48 & double(instr(i)) <=57 & i >1)
        outstr(count) = instr(i);
        count = count + 1;
    end
    i = i + 1;
end