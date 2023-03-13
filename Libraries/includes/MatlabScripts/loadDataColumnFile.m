function subject = loadDataColumnFile(dataFilesAbsolutePath)
%Read data file
%mlines =
%textread(dataFileAbsolutePath,'%s','bufsize',131072,'delimiter','\n');

[files, nbFiles] = explode(dataFilesAbsolutePath, ';');

createdCategories = containers.Map('KeyType','char','ValueType','char');
firstTime = true;

nbSamples = 0;
for n=1:nbFiles
    data = dlmread(files{n}, '\t', 2, 0);
     if(nbSamples < length(data(:,1)))
         nbSamples = length(data(:,1));
     end
end

for n=1:nbFiles    
    [~, fileName, ~] = fileparts(files{n});    
    fileNameSplitted = explode(fileName, '_');        
    criteria = [fileNameSplitted{2}, '_', fileNameSplitted{3}, '_', fileNameSplitted{4}];    
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
    tempValuesNames = strsplit(line, '\t'); 
    line = fgetl(fid);
    tempValuesFrequencies = strsplit(line, '\t');
    fclose(fid);
    data = dlmread(files{n}, '\t', 2, 0);
    for nbChannels = 1:length(tempValuesNames)
        channelName = tempValuesNames{nbChannels};
        currentValues = data(:,nbChannels);
        eval(['subject.', channelName, '.SampleFrequency = ', tempValuesFrequencies{nbChannels}, ';']);
        eval(['subject.', channelName, '.isSignal = 1;']);
        eval(['subject.', channelName, '.isCategory = 0;']);
        eval(['subject.', channelName, '.isEvent = 0;']);
        eval(['subject.', channelName, '.NbFeatures = 0;']);
        eval(['subject.', channelName, '.NbMarkersGroups = 0;']);
        eval(['subject.', channelName, '.NbSamples(n) = nbSamples;']);            
        eval(['subject.', channelName, '.EndCut(n) = length(currentValues);']);            
        eval(['subject.', channelName, '.FrontCut(n) = 0;']);
        if(firstTime)
            eval(['subject.', channelName, '.Values = zeros(nbFiles, nbSamples);']);
        end
        eval(['subject.', channelName, '.Values(n,1:length(currentValues)) = currentValues;']);
    end
    firstTime = false;
    createdCategories(criteria) = trialsList;    
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
