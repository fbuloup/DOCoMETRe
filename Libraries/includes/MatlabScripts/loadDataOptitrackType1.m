function subject = loadDataOptitrackType1(dataFilesAbsolutePath)
%Read data file
%mlines =
%textread(dataFileAbsolutePath,'%s','bufsize',131072,'delimiter','\n');

[files, nbFiles] = explode(dataFilesAbsolutePath, ';');

nbCategories = 0;
categories = {};
nbTrials = nbFiles;
nbSamples = 0;
channelsNames = java.util.ArrayList();
createdCategories = containers.Map('KeyType','char','ValueType','char');
firstTime = true;
% Get all categories, sample frequency and signals names
for n=1:nbFiles    
    [~, fileName, ~] = fileparts(files{n});    
    fileNameSplitted = explode(fileName, '_');        
    criteria = fileNameSplitted{2};    
    if(isKey(createdCategories, criteria))
        trialsList = createdCategories(criteria);            
        testFindTrialInTrialsList = eval(['find([', trialsList, '] ==', num2str(n), ', 1)']);
        if(isempty(testFindTrialInTrialsList))                              
            trialsList = [trialsList, ', ', num2str(n)];
        end
    else
        trialsList = num2str(n);
    end
    
    fid = fopen(files{n});
    line = fgetl(fid);
    tempValue = explode(line, ',');
    nbSamples = max(nbSamples, str2double(tempValue{2}));
    
    if(firstTime)
        
        fgetl(fid);
        line = fgetl(fid);
        tempValue = explode(line, ',');
        sampleFrequency = str2double(tempValue{2});
        
        fgetl(fid);
        fgetl(fid);
        line = fgetl(fid);
        tempValue = explode(line, ',');
        for p=1:length(tempValue)
            tempValue2 = tempValue(p);
            if(~strcmp(tempValue2, 'Time'))                
                if(~strcmp(tempValue2, ''))
                    channelName = [tempValue2{1}, '_X'];
                    channelsNames.add(channelName);
                    channelName = [tempValue2{1}, '_Y'];
                    channelsNames.add(channelName);
                    channelName = [tempValue2{1}, '_Z'];
                    channelsNames.add(channelName); 
                end
            end
        end
        
        firstTime = false;
    end
    
    
    fclose(fid);
    createdCategories(criteria) = trialsList;    
end


createdChannelsNames = java.util.ArrayList();
for trialNumber=1:nbTrials
    
    data = dlmread(files{trialNumber}, ',', 8, 1);
    
    for numChannel = 1:channelsNames.size()
        channelName = channelsNames.get(numChannel-1);
        
        if(~createdChannelsNames.contains(channelName))

            eval(['subject.', channelName, '.SampleFrequency = sampleFrequency;']);
            eval(['subject.', channelName, '.isSignal = 1;']);
            eval(['subject.', channelName, '.isCategory = 0;']);
            eval(['subject.', channelName, '.isEvent = 0;']);
            eval(['subject.', channelName, '.NbFeatures = 0;']);
            eval(['subject.', channelName, '.NbMarkersGroups = 0;']);
            eval(['subject.', channelName, '.Values = zeros(nbTrials, nbSamples);']);

            createdChannelsNames.add(channelName);
        end
        
        fid = fopen(files{trialNumber});
        line = fgetl(fid);
        tempValue = explode(line, ',');
        localNBSamples = tempValue{2};
        fclose(fid);
        eval(['subject.', channelName, '.NbSamples(trialNumber) = ', localNBSamples,';']);            
        eval(['subject.', channelName, '.EndCut(trialNumber) = ', localNBSamples,';']);            
        eval(['subject.', channelName, '.FrontCut(trialNumber) = 0;']);
        
        dataSize = length(data(:,numChannel));
        eval(['subject.', channelName, '.Values(', 'trialNumber,1:dataSize) = data(:,numChannel)'';']);
        
    end    

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
