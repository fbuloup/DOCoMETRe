function subject = loadDataDocometre(dataFilesList, varargin) %, options)
      
if contains(dataFilesList,'.sau,')
	loadDataDocometreSAU(dataFilesList);
elseif contains(dataFilesList,'.samples;')
    subject = loadDataDocometreSAMPLES(dataFilesList, varargin{1}, varargin{2});
else
    
%% Here we are : this is ADW data file, only one file in data files list

dataFile = dataFilesList;

fp = fopen(dataFile, 'rb','l');
 
nbChannels = fread(fp,1,'int32');          % nbA/D total possible
channelsNames = readNames(fp,nbChannels);      %findnamesN(fp,vg.nad);
  
nbSessions = fread(fp,1,'int32')  ;          % nb sessions
sessionsNames = readNames(fp,nbSessions);        

nbConditions = fread(fp,1,'int32')    ;        % nb conditions
conditionsNames = readNames(fp,nbConditions);
  
nbSeqTypes = fread(fp,1,'int32');            % nb s�quence types
seqTypesNames = readNames(fp,nbSeqTypes);
  
nbTrials = fread(fp,1,'int32');

nbTotCategories =  nbSessions*nbConditions*nbSeqTypes;

% stoptrial = -1;
% maxPercent = 2*nbTrials + nbTotCategories;
% percentComplete = 0;
% evalin('base','percentComplete = 0;');

trials = cell(nbTrials,1);
Categories = [];
if nbTrials > 0
	for currentNumTrial = 1:nbTrials
       
        currentSession(currentNumTrial) = fread(fp,1,'int32');
        currentCondition(currentNumTrial) = fread(fp,1,'int32');
        currentSeqType(currentNumTrial) = fread(fp,1,'int32');        
        nbChannelsInTrial = fread(fp,1,'int32');
        
        ind(currentNumTrial) = ((currentSession(currentNumTrial) -1)*nbConditions*nbSeqTypes) + ((currentCondition(currentNumTrial) -1)* nbSeqTypes) + currentSeqType(currentNumTrial);
        fullcondname{currentNumTrial} = [ sessionsNames{currentSession(currentNumTrial)} '_' seqTypesNames{currentSeqType(currentNumTrial)}];    
        
        Categories = [Categories; {fullcondname{currentNumTrial}}];
        
        if nbChannelsInTrial > 0            
            
            signals = cell(nbChannelsInTrial,1);                                    
                        
            for currentNumChannel = 1:nbChannelsInTrial     
             	                            
                numChannel = fread(fp,1,'int32');
                sampleFrequency = fread(fp,1,'float32') ;
                nbData = fread(fp,1,'int32');
                                                              
                if nbData >= 0
                    
                    signals{currentNumChannel,1}.isSignal = 1;   
                    signals{currentNumChannel,1}.isCategory = 0;   
                    signals{currentNumChannel,1}.isEvent = 0;       
                    signals{currentNumChannel,1}.NbMarkers = 0;
                    signals{currentNumChannel,1}.NbFields = 0;
                    signals{currentNumChannel,1}.SampleFrequency = sampleFrequency;                    
                    signalName = char(channelsNames{numChannel,1});
                    fullSignalName = [ 'tempSubject.' signalName ];
                    values = fread(fp,nbData,'float32');
                    signals{currentNumChannel,1}.Values = values';%fread(fp,nbData,'float32');                  
                    signals{currentNumChannel,1}.FrontCut = 1;                    
                    signals{currentNumChannel,1}.NbSamples = nbData;
                    signals{currentNumChannel,1}.EndCut = signals{currentNumChannel,1}.NbSamples;   
                    signals{currentNumChannel,1}.FullSignalName = fullSignalName;      
                    %signals{currentNumChannel,1}.Category = {fullcondname{currentNumTrial}};
                    
                    
               end
            end
            trials{currentNumTrial,1} = signals;
            
            
        end                     
        
%         percentComplete = percentComplete + 1;
%         percentCompleteString = int2str(100*percentComplete/maxPercent);        
%         evalin('base',['percentComplete = ',percentCompleteString,';']);
%         
%         if currentNumTrial == stoptrial 
%             break
%         end        
        
    end
    
   % categoriesTrialsNames = {};
   % categoriesTrialsNumber = [];
    
    nbTrialsWithChannels = 0;
    for currentNumTrial = 1:nbTrials
        %['currentNumTrial', ' : ', int2str(currentNumTrial)]
        signals = trials{currentNumTrial,1};        
        sizeTemp = size(signals);
        nbChannelsInTrial = sizeTemp(1);
        if(nbChannelsInTrial > 0)
            nbTrialsWithChannels = nbTrialsWithChannels + 1;
        end
        
        for currentNumChannel = 1:nbChannelsInTrial
            signal = signals{currentNumChannel,1};            
            fullSignalName = signal.FullSignalName;
            signal = rmfield(signal,'FullSignalName');   
            %DEBUG['currentSignal', ' : ', fullSignalName]            
            if nbTrialsWithChannels == 1
                eval([fullSignalName, ' = signal;']);      
                eval(['currentValuesSizes = size(', fullSignalName,'.Values);']);
                %DEBUG['create Signal 1 ', fullSignalName]
            else
                %Check if signal exists. Create it if not.
                segmentsNames = explode(fullSignalName,'.');
                %cmd = strcat(segmentsNames(1), '.', segmentsNames(2));
                %cmd = strcat(cmd,',','''',segmentsNames(3),'''');
                cmd = strcat('signalExist = isfield(tempSubject,''',segmentsNames(2),''');');
                eval(cat(2,cmd{:}));
                if(signalExist == 0)
                    eval([fullSignalName, ' = signal;']);
                    nbSamplesString = int2str(signal.NbSamples);                                        
                    eval([fullSignalName,'.Values = []']); 
                    eval([fullSignalName,'.FrontCut = []']);            
                    eval([fullSignalName,'.EndCut = []']);                
                    eval([fullSignalName,'.NbSamples = []']); 
                    currentValuesSize = signal.NbSamples;   
                    eval(['currentValuesSizes = size(', fullSignalName,'.Values);']);
                    %DEBUG['create Signal 2 ', fullSignalName, ' with ', currentNumTrialString, ' empty trials']
                else
                    eval(['currentValuesSizes = size(', fullSignalName,'.Values);']);
                    currentValuesSize = currentValuesSizes(2);
                    nbSamplesString = int2str(currentValuesSize);
                    eval(['currentValuesSizes = size(', fullSignalName,'.Values);']);
                end                
                %If there are missed trials : trials without data for this signal, add...
                trialsSize = currentValuesSizes(1);
                %currentValuesSizeString = int2str(currentValuesSize);
                if(trialsSize < currentNumTrial - 1)                    
                    nbTrialsToAdd = currentNumTrial - 1 - trialsSize;
                    nbTrialsToAddString = int2str(nbTrialsToAdd);
                    expression = [fullSignalName,'.Values = [', fullSignalName ,'.Values; zeros(',nbTrialsToAddString,',',nbSamplesString,')];'];
                    eval(expression); 
                    eval([fullSignalName,'.FrontCut = [', fullSignalName ,'.FrontCut; ones(',nbTrialsToAddString,',1)];']);            
                    eval([fullSignalName,'.EndCut = [', fullSignalName ,'.EndCut; ',nbSamplesString,'*ones(',nbTrialsToAddString,',1)];']);                
                    eval([fullSignalName,'.NbSamples = [', fullSignalName ,'.NbSamples; ', nbSamplesString, '*ones(',nbTrialsToAddString,',1)];']);
                    %DEBUG['add ', nbTrialsToAddString, ' empty trials to ', fullSignalName]
                end
                
                %currentCategories = eval([fullSignalName, '.Category;']);
                %currentCategories = [currentCategories; signal.Category];
                %eval([fullSignalName,'.Category = currentCategories;']);       
                
                %If size of new trial is bigger than all others, increase matrix Values
                %padding with zeros (or last values within comments)
                if(currentValuesSize < signal.NbSamples)
                    currentNumTrialString = int2str(currentNumTrial - 1);
                    increaseByString = int2str(signal.NbSamples - currentValuesSize);
                    expression = [fullSignalName,'.Values = [', fullSignalName ,'.Values, zeros(',currentNumTrialString,',',increaseByString,')];'];
                    eval(expression);                                                                                                                                       
                end
                
                %If size of new trial is smaller than all others, increase vector values 
                %pading withi zeros (or last value whithin comments)
                if(currentValuesSize > signal.NbSamples)
	                signal.Values = [signal.Values zeros(1,currentValuesSize - signal.NbSamples)];
                end
                
                eval([fullSignalName,'.Values = [', fullSignalName ,'.Values;  signal.Values];']);               
                eval([fullSignalName,'.FrontCut = [', fullSignalName ,'.FrontCut;  signal.FrontCut];']);            
                eval([fullSignalName,'.EndCut = [', fullSignalName ,'.EndCut;  signal.EndCut];']);                
                eval([fullSignalName,'.NbSamples = [', fullSignalName ,'.NbSamples;  signal.NbSamples];']);
                
            end
            
%             if currentNumTrial == 1            
%                 assignin('base','tempSignal',signal); % create temporary variable 
%                 %Affect this temp variable to fullSignalName with evalin
%                 evalin('base',[fullSignalName,' = tempSignal;']);                         
%                 evalin('base','clear tempSignal;')            
%             else
%                 %sizeTemp = size(signal.Values);                     
%                 evalin('base',['currentValuesSizes = size(', fullSignalName,'.Values);']);
%                 currentValuesSizes = evalin('base','currentValuesSizes');
%                 currentValuesSize = currentValuesSizes(2);
%                 evalin('base','clear currentValuesSizes;')
%                 
%                 currentCategories = evalin('base',[fullSignalName, '.Category']);
%                 signal.Category = [currentCategories; signal.Category];
%                 assignin('base','tempSignalCategory',signal.Category); % create temporary variable 
%                 %Affect this temp variable to fullSignalName with evalin
%                 evalin('base',[fullSignalName,'.Category = tempSignalCategory;']);                         
%                 evalin('base','clear tempSignalCategory;')    
%                 
%                 %expression = [fullSignalName, '.category = [', fullSignalName, '.category; ', category, '];']
%                 %evalin('base',expression); 
%                 
%                 if(currentValuesSize < signal.NbSamples)
%                     currentNumTrialString = int2str(currentNumTrial - 1);
%                     increaseByString = int2str(signal.NbSamples - currentValuesSize);
%                     expression = [fullSignalName,'.Values = [', fullSignalName ,'.Values, zeros(',currentNumTrialString,',',increaseByString,')];'];
%                     evalin('base',expression);                                                
%                     
%                     nbSamplesString = int2str(signal.NbSamples);
%                     expression = [fullSignalName,'.NbSamples = ', nbSamplesString,'*ones(1,',currentNumTrialString,');'];
%                    	evalin('base',expression);                                                                                                                                   
%                 end
%                 
%                 if(currentValuesSize > signal.NbSamples)
%                     signal.Values = [signal.Values zeros(1,currentValuesSize - signal.NbSamples)];
%                     signal.NbSamples = currentValuesSize;
%                 end
%                 
%                 assignin('base','tempSignalValues',signal.Values); % create temporary variable 
%                 %Affect this temp variable to fullSignalName with evalin
%                 evalin('base',[fullSignalName,'.Values = [', fullSignalName ,'.Values;  tempSignalValues];']);
%                 
%                 assignin('base','tempSignalValues',signal.FrontCut); % create temporary variable 
%                 %Affect this temp variable to fullSignalName with evalin
%                 evalin('base',[fullSignalName,'.FrontCut = [', fullSignalName ,'.FrontCut  tempSignalValues];']);
%                 
%                 assignin('base','tempSignalValues',signal.EndCut); % create temporary variable 
%                 %Affect this temp variable to fullSignalName with evalin
%                 evalin('base',[fullSignalName,'.EndCut = [', fullSignalName ,'.EndCut  tempSignalValues];']);
%                 
%                 assignin('base','tempSignalValues',signal.NbSamples); % create temporary variable 
%                 %Affect this temp variable to fullSignalName with evalin
%                 evalin('base',[fullSignalName,'.NbSamples = [', fullSignalName ,'.NbSamples  tempSignalValues];']);               
%                 
%                 evalin('base','clear tempSignalValues;')
%             end
            
        end
        
%         percentComplete = percentComplete + 1;
%         percentCompleteString = int2str(100*percentComplete/maxPercent);        
%         evalin('base',['percentComplete = ',percentCompleteString,';']);
%         
%         if currentNumTrial == stoptrial 
%             break
%         end
        
    end
end

%eval(['tempSubject = [', experimentName ,'.' ,  subjectName, '];']); 

%fieldsNames = eval('fieldnames(tempSubject)');
%firstSignalName = fieldsNames(1);
%eval(['firstSignal = tempSubject.', firstSignalName{1}, ';']); 
%categories = firstSignal.Category;
categoriesDone = {};
numCategory = 1;
for i=1:length(Categories)
    categoryName = Categories{i};
    if(isempty(indexesOf(categoriesDone,categoryName)))
        indexes = indexesOf(Categories,categoryName);
        eval(['tempSubject.Category', int2str(numCategory), '.isCategory = 1;']);
        eval(['tempSubject.Category', int2str(numCategory), '.isSignal = 0;']);
        eval(['tempSubject.Category', int2str(numCategory), '.isEvent = 0;']);
        eval(['tempSubject.Category', int2str(numCategory), '.Criteria = categoryName;']); 
        eval(['tempSubject.Category', int2str(numCategory), '.TrialsList = indexes;']);     
        if(exist('categoriesDone'))
            categoriesDone(1,numCategory)={categoryName};           
        else
            categoriesDone = {categoryName}; 
        end
        numCategory = numCategory + 1;
    end    
end
tempSubject.Categories.Names = Categories;
tempSubject.Categories.isCategory = 0;
tempSubject.Categories.isSignal = 0;
tempSubject.Categories.isEvent = 0;
    
subject = tempSubject;

%assignin('base','tempSubject',tempSubject);
%evalin('base', [experimentName,'.', subjectName,'=tempSubject;']);
%evalin('base','clear tempSubject;');
fclose(fp);
end

%**************************************************************************
function names = readNames(id, nbNames)

    names = cell(nbNames,1);
    for i = 1:nbNames
        charact = fread(id,1,'char');   
        name = '';
        while charact ~= '|'              
            name = [name charact];
            charact = fread(id,1,'char');
        end        
        names{i,1} = name;
    end

%**************************************************************************
function indexes = indexesOf(names, name)
    indexes = [];
    for i=1:length(names)
        if(strcmp(names(i),name)) 
            indexes=[indexes, i];
        end
    end
