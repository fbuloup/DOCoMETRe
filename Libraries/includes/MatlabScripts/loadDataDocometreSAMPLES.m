function subject = loadDataDocometre(dataFilesList, varargin)

    % useFirstSuffix_QN = '_USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES';
    % useSecondSuffix_QN = '_USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES';
    prefix_QN = '_DATA_FILES_NAMES_PREFIX';
    baseTrialsNumber_QN = '_BASE_TRIALS_NUMBER';
    
    dataFiles = split(dataFilesList, ';');
     
    % Each data file has format : 
    % ...something/WorkspaceName/ExpName/SubjectName/Session/Trial/channel.samples
    
    createdChannels = java.util.ArrayList();
    createdCategories = containers.Map('KeyType','char','ValueType','char');
    
    nbDataFiles = length(dataFiles);
    sessionsProperties = containers.Map(varargin{1}, varargin{2});
    
    maximumSamples = sessionsProperties('MAXIMUM_SAMPLES');
    totalTrialsNumber = sessionsProperties('TOTAL_TRIALS_NUMBER');
    
    for n = 1:nbDataFiles
        
        segments = split(dataFiles{n}, filesep);        
        
        % Get session name for criteria, trial number and prefix
        sessionNameCell = segments(length(segments) - 2);
        trialNameCell = segments(length(segments) - 1);
        prefix = sessionsProperties([sessionNameCell{1}, prefix_QN]);
        process = sessionsProperties([fileparts(dataFiles{n}), '_PROCESS']);
        criteria = [sessionNameCell{1}, '.', process];
        if(~isempty(prefix))
            criteria = [prefix, '.', sessionNameCell{1}, '.', process];
        end
        trialName = trialNameCell{1};        
        trialNumberCell = split(trialName, char(176));
        trialNumber = trialNumberCell{2};
        system = sessionsProperties([fileparts(dataFiles{n}), '_SYSTEM']);
        
        baseTrialsNumber = sessionsProperties([sessionNameCell{1}, baseTrialsNumber_QN]);
        trialNumber = num2str(str2double(baseTrialsNumber) + str2double(trialNumber));
        
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

    n = 1;
    for k = keys(createdCategories)
        values = createdCategories(k{1});
        eval(['subject.Category', int2str(n), '.Criteria = ''', k{1},''';']);
        eval(['subject.Category', int2str(n), '.TrialsList = sort([', values,']);']);
        eval(['subject.Category', int2str(n), '.isSignal = 0;']);
        eval(['subject.Category', int2str(n), '.isCategory = 1;']);
        eval(['subject.Category', int2str(n), '.isEvent = 0;']);
        n = n + 1;
    end
    
end