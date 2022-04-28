function loadDataOptitrackType1(dataFilesAbsolutePath)
%Read data file
%mlines =
%textread(dataFileAbsolutePath,'%s','bufsize',131072,'delimiter','\n');

[files, nbFiles] = explode(dataFilesAbsolutePath, ';');

nbCategories = 0;
categories = {};
for n=1:nbFiles
    
    [filePath, fileName, extension] = fileparts(files{n});
    
    fileNameSplitted = explode(fileName, '_');
    % Get session name for criteria and trial number
    criteria = fileNameSplitted{2};    
    % baseTrialsNumber = sessionsProperties([sessionNameCell{1}, baseTrialsNumber_QN]);
    trialNumber = str2double(fileNameSplitted{3});
    
    fid = fopen(files{n});
    fgetl(fid);
    fgetl(fid);
    line = fgetl(fid);
    tempValue = explode(line, ',');
    sampleFrequency = str2double(tempValue{2});
    fclose(fid);
    
    
    
    %%%%%%%%%%%%%%%%%%%%%%
           
        
    

    
    data = readtable(files{n});
    
    % Get channel's name
    fileName = segments(length(segments));
    fileNameSegments = split(fileName, '.');
    channelName = fileNameSegments{1};
    if(~isempty(prefix)) 
        channelName = fileNameSegments{2};     
    end

    if(isKey(createdCategories, criteria))
        trialsList = createdCategories(criteria);            
        testFindTrialInTrialsList = eval(['find([', trialsList, '] ==', trialNumber, ', 1)']);
        if(isempty(testFindTrialInTrialsList))                              
            trialsList = [trialsList, ', ', trialNumber];
        end

    else
        trialsList = trialNumber;
    end
    createdCategories(criteria) = trialsList;

    if(~createdChannels.contains(channelName))
        % Create new channel
        createdChannels.add(channelName);        
        sampleFrequency = sessionsProperties([channelName, '_SF']);            
        eval(['subject.', channelName, '.SampleFrequency = ', sampleFrequency, ';']);
        eval(['subject.', channelName, '.isSignal = 1;']);
        eval(['subject.', channelName, '.isCategory = 0;']);
        eval(['subject.', channelName, '.isEvent = 0;']);
        eval(['subject.', channelName, '.NbFeatures = 0;']);
        eval(['subject.', channelName, '.NbMarkersGroups = 0;']);
        eval(['subject.', channelName, '.Values = zeros(', totalTrialsNumber, ', ', maximumSamples,');']);

    end

    % Read data
    %%%%%%%%%%
    
    
    fileHandle = fopen(dataFiles{n}, 'r');
    data = fread(fileHandle, 'float32')';       
    fclose(fileHandle);

    channelNameValues = ['subject.', channelName, '.Values'];
    sizeData = size(data);
    sizeData = sizeData(2);

    eval(['subject.', channelName, '.NbSamples(',trialNumber ,') = ', num2str(sizeData),';']);
    eval(['subject.', channelName, '.EndCut(',trialNumber ,') = ', num2str(sizeData),';']);
    eval(['subject.', channelName, '.FrontCut(',trialNumber ,') = 0;']);
    eval([channelNameValues, '(', trialNumber, ', 1:', num2str(sizeData),') = data;']);
    
    

end
% fid = fopen(mainDataFileAbsolutePath);
% mline = fgetl(fid);
% firstTime = 1;
% while ischar(mline)
%    if(firstTime == 0)
%        mlines = [mlines; cellstr(mline)];
%    else
%        firstTime = 0; 
%        mlines = cellstr(mline);
%    end   
%    mline = fgetl(fid);
% end
% fclose(fid);
% 
% fileSize = size(mlines);
% nbLines = fileSize(1);
% 
% currentDirectory = [pwd, filesep];
% 
% %Parse lines ignoring empty lines
% nbCategories = 0;
% nbEvents = 0;
% canReadValues = 0;
% signalValues = 0;
% fromTextFile = 0;
% markerValues = 0;
% fieldValues = 0;
% for n=1:nbLines
% 	%Clean comments
% 	mlines{n} = regexprep(mlines{n},'\s*#(.)*$','');
%     if(isempty(mlines{n}) == 0)
%         %Search for tokens at beginning of current line
%         %SignalName: token
%         if(regexp(mlines{n},'^SignalName:') == 1)
%             %We have a new signal, let's prepare the structure
%             signalName=mlines{n}(strfind(mlines{n},':') + 1:length(mlines{n}));
%             eval(['subject.', signalName, '.isSignal=1;']);
%             eval(['subject.', signalName, '.isEvent=0;']);
%             eval(['subject.', signalName, '.isCategory=0;']);
%             eval(['subject.', signalName, '.NbMarkersGroups=0;']);
%             eval(['subject.', signalName, '.Values=[];']);
%             eval(['subject.', signalName, '.FrontCut=[];']);
%             eval(['subject.', signalName, '.EndCut=[];']);
%             eval(['subject.', signalName, '.NbSamples=[];']);
%             nbMarkers = 0;
%             eval(['subject.', signalName, '.NbFeatures=0;']);
%             nbFields = 0;
%             canReadValues = 0;
% 			fromTextFile = 0;
%             signalValues = 1;
%             eventsValues = 0;
%             markerValues = 0;
%             fieldValues = 0;
%         %SampleFrequency:
%         elseif(regexp(mlines{n},'^SampleFrequency:') == 1)
%             sampleFrequency = mlines{n}(strfind(mlines{n},':') + 1:length(mlines{n}));
%             eval(['subject.', signalName, '.SampleFrequency=', sampleFrequency, ';']);
%         %EventsLabel:
%         elseif(regexp(mlines{n},'^EventsLabel:') == 1)
%             criteria = mlines{n}(strfind(mlines{n},':') + 1:length(mlines{n}));
%             nbEvents = nbEvents + 1;
%             eval(['subject.Event', num2str(nbEvents), '.isSignal=0;']);
%             eval(['subject.Event', num2str(nbEvents), '.isCategory=0;']);
%             eval(['subject.Event', num2str(nbEvents), '.isEvent=1;']);
%             eval(['subject.Event', num2str(nbEvents), '.Criteria=''', criteria, ''';']);
%             eval(['subject.Event', num2str(nbEvents), '.Values =[];']);
%         	canReadValues = 0;
% 			fromTextFile = 0;
%             signalValues = 0;
%             eventsValues = 1;
%             markerValues = 0;
%             fieldValues = 0;
%         %MarkersGroupLabel:
%         elseif(regexp(mlines{n},'^MarkersGroupLabel:') == 1)
%             markersGroupLabel = mlines{n}(strfind(mlines{n},':') + 1:length(mlines{n}));
%             nbMarkers = nbMarkers + 1;
%             eval(['subject.', signalName, '.NbMarkersGroups=nbMarkers;']);
%             eval(['subject.', signalName, '.Marker', num2str(nbMarkers), '_Label=''', markersGroupLabel, ''';']);
%             eval(['subject.', signalName, '.Marker', num2str(nbMarkers), '_Values=[];']);
%             canReadValues = 0;
% 			fromTextFile = 0;
%             signalValues = 0;
%             eventsValues = 0;
%             markerValues = 1;
%             fieldValues = 0;
%         %FieldLabel:
%         elseif(regexp(mlines{n},'^FieldLabel:') == 1)
%             fieldLabel = mlines{n}(strfind(mlines{n},':') + 1:length(mlines{n}));
%             nbFields = nbFields + 1;
%             eval(['subject.', signalName, '.NbFields=nbFields;']);
%             eval(['subject.', signalName, '.Field', num2str(nbFields), '_Label=''', fieldLabel, ''';']);
%             eval(['subject.', signalName, '.Field', num2str(nbFields), '_Values=[];']);
%             canReadValues = 0;
% 			fromTextFile = 0;
%             signalValues = 0;
%             eventsValues = 0;
%             markerValues = 0;
%             fieldValues = 1;
%         %CategoryCriteria:
%         elseif(regexp(mlines{n},'^CategoryCriteria:') == 1)
%             criteria = mlines{n}(strfind(mlines{n},':') + 1:length(mlines{n}));
%             nbCategories = nbCategories + 1;
%             eval(['subject.Category', num2str(nbCategories), '.isSignal=0;']);
%             eval(['subject.Category', num2str(nbCategories), '.isCategory=1;']);
%             eval(['subject.Category', num2str(nbCategories), '.isEvent=0;']);
%             eval(['subject.Category', num2str(nbCategories), '.Criteria=''', criteria, ''';']);
%             canReadValues = 0;
% 			fromTextFile = 0;
%             signalValues = 0;
%             eventsValues = 0;
%             markerValues = 0;
%             fieldValues = 0;
%         %TrialsList:
%         elseif(regexp(mlines{n},'^TrialsList:') == 1)
%             trialsList = mlines{n}(strfind(mlines{n},':') + 1:length(mlines{n}));
%             eval(['subject.Category', num2str(nbCategories), '.TrialsList=[', trialsList, '];']);
%             canReadValues = 0;
% 			fromTextFile = 0;
%             signalValues = 0;
%             eventsValues = 0;
%             markerValues = 0;
%             fieldValues = 0;
%         % Read Values        
%         elseif(canReadValues == 1)
%             if(signalValues == 1)
%            		eval(['subject.', signalName, '.Values=', '[subject.', signalName, '.Values; ', mlines{n}, '];']);
%                 eval(['nbSamplesSize=size(', 'subject.', signalName, '.Values);']);
%                 nbSamples = nbSamplesSize(2);
%                 eval(['subject.', signalName, '.FrontCut=', '[subject.', signalName, '.FrontCut; ', '1', '];']);
%                 eval(['subject.', signalName, '.EndCut=', '[subject.', signalName, '.EndCut; ', num2str(nbSamples), '];']);
%                 eval(['subject.', signalName, '.NbSamples=', '[subject.', signalName, '.NbSamples; ', num2str(nbSamples), '];']);
%             elseif(eventsValues == 1)
%            		eval(['subject.Event', num2str(nbEvents), '.Values=', '[subject.Event', num2str(nbEvents), '.Values; ', mlines{n}, '];']);
%             elseif(markerValues == 1)
%            		eval(['subject.', signalName, '.Marker', num2str(nbMarkers), '_Values=', '[subject.', signalName, '.Marker', num2str(nbMarkers), '_Values; ', mlines{n}, '];']);
%             elseif(fieldValues == 1)
%            		eval(['subject.', signalName, '.Field', num2str(nbFields), '_Values=', '[subject.', signalName, '.Field', num2str(nbFields), '_Values; ', mlines{n}, '];']);
%             end
%         %Values:
%         elseif(regexp(mlines{n},'^Values:') == 1)
%             canReadValues = 1;
%             fileName = mlines{n}(strfind(mlines{n},':') + 1:length(mlines{n}));
% 			if(isempty(fileName) == 0)
% 				fileName = [currentDirectory, experimentName, filesep, subjectName, filesep, fileName];
% 				canReadValues = 0;
% 				if(signalValues == 1)
% 					load(fileName);
% 	           		eval(['subject.', signalName, '.Values= ', signalName, ';']);
% 	                eval(['valuesSize=size(', 'subject.', signalName, '.Values);']);
%                     nbTrials = valuesSize(1);
% 	                nbSamples = valuesSize(2);
% 	                eval(['subject.', signalName, '.FrontCut= ones(1,', num2str(nbTrials), ')'';']);
% 	                eval(['subject.', signalName, '.EndCut=', num2str(nbSamples), '*ones(1,', num2str(nbTrials), ')'';']);
% 	                eval(['subject.', signalName, '.NbSamples= repmat(', num2str(nbSamples), ',20,1);']);                                                            
% 	            elseif(eventsValues == 1)
% 	           		load(fileName);
% 	           		eval(['subject.Event', num2str(nbEvents), '.Values= ', signalName, ';']);
% 	            elseif(markerValues == 1)
%             		load(fileName);
%             		eval(['subject.', signalName, '.Marker', num2str(nbMarkers), '_Values=', signalName, ';']);
% 	            elseif(fieldValues == 1)
%             		values = load(fileName);
%             		eval(['subject.', signalName, '.Field', num2str(nbFields), '_Values=', signalName, ';']);
% 	            end
% 			end
%         end
%     end
% end
% %Copy subject into base workspace
% assignin('base', 'subject', subject);
% evalin('base',[ experimentName, '.', subjectName, ' = subject;' ]);
% evalin('base','clear subject');