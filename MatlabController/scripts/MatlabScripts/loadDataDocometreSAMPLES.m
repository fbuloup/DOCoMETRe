function subject = loadDataDocometre(dataFilesList, varargin)


    useFirstSuffix_QN = '_USE_SESSION_NAME_AS_FIRST_SUFFIX_IN_DATA_FILES_NAMES';
    useSecondSuffix_QN = '_USE_TRIAL_NUMBER_AS_SECOND_SUFFIX_IN_DATA_FILES_NAMES';
    prefix_QN = '_DATA_FILES_NAMES_PREFIX';
    baseTrialsNumber_QN = '_BASE_TRIALS_NUMBER';
    
    dataFiles = split(dataFilesList, ';');
     
    % Each data file has format : 
    % ...something/WorkspaceName/ExpName/SubjectName/Session/Trial/channel.samples
    
    createdChannels = java.util.ArrayList();
    
    nbDataFiles = length(dataFiles);
    sessionsProperties = containers.Map(varargin{1}, varargin{2});
    
    for n = 1:nbDataFiles
        
        segments = split(dataFiles{n}, filesep);        
        
        % Get session name for criteria, trial number and prefix
        sessionNameCell = segments(length(segments) - 2);
        trialNameCell = segments(length(segments) - 1);
        criteria = sessionNameCell{1};
        trialName = trialNameCell{1};        
        trialNumberCell = split(trialName, '°');
        trialNumber = trialNumberCell{2};
        prefix = sessionsProperties([sessionNameCell{1}, prefix_QN]);
        baseTrialsNumber = sessionsProperties([sessionNameCell{1}, baseTrialsNumber_QN]);
        trialNumber = num2str(str2double(baseTrialsNumber) + str2double(trialNumber));
        
        % Get channel's name
        fileName = segments(length(segments));
        fileNameSegments = split(fileName, '.');
        channelName = fileNameSegments{1};
        if(~isempty(prefix)) channelName = fileNameSegments{2};end
            
        if(~createdChannels.contains(channelName))
            % Create new channel
            createdChannels.add(channelName);        
            sampleFrequency = sessionsProperties([channelName, '_SF']);            
            eval(['subject.', channelName, '.SampleFrequency = ', sampleFrequency, ';']);
            eval(['subject.', channelName, '.isSignal = 1;']);
            eval(['subject.', channelName, '.isCategory = 0;']);
            eval(['subject.', channelName, '.isEvent = 0;']);
            eval(['subject.', channelName, '.nbFields = 0;']);
            eval(['subject.', channelName, '.nbMarkers = 0;']);
        end
        
        % Read data
        fileHandle = fopen(dataFiles{n}, 'r');
        data = fread(fileHandle, 'float32')';
        fclose(fileHandle);
        
        
        channelNameValues = ['subject.', channelName, '.Values'];
        sizeChannelNameValues = 0;
        sizeData = size(data);
        sizeData = sizeData(2);
        
        eval(['subject.', channelName, '.NbSamples(',trialNumber ,') = ', num2str(sizeData),';']);
        eval(['subject.', channelName, '.EndCut(',trialNumber ,') = ', num2str(sizeData + 1),';']);
        eval(['subject.', channelName, '.FrontCut(',trialNumber ,') = 0;']);
        try 
            sizeChannelNameValues = eval(['size(', channelNameValues, ')']);
            sizeChannelNameValues = sizeChannelNameValues(2);
        catch
        end
        
        if(sizeChannelNameValues > 0 && sizeData ~= sizeChannelNameValues)
            if(sizeData < sizeChannelNameValues)
                data = [data, zeros(1,sizeChannelNameValues - sizeData)];                
            else            
                cmd = [channelNameValues,' = padarray(', channelNameValues, ',[0, ', num2str(sizeData - sizeChannelNameValues),'],''post'');'];
                eval(cmd);
                
            end
        end
        
        %fprintf('criteria : %s, channel : %s, trial : %s.\n',criteria,channelName,trialNumber);
        %fprintf('\t cmd : %s.\n',['subject.', channelName, '.Values(', trialNumber, ', :) = data;']);
        %fprintf('\t size channel : %d, size data : %d.\n',sizeChannelNameValues,sizeData);
        
        eval([channelNameValues, '(', trialNumber, ', :) = data;']);
    end

end