function subject = loadDataOptitrackType2(dataFilesAbsolutePath)
% Read data file
% mlines =
% textread(dataFileAbsolutePath,'%s','bufsize',131072,'delimiter','\n');

[files, nbFiles] = explode(dataFilesAbsolutePath, ';');

nbTrials = nbFiles;
nbSamples = 0;
channelsNames = java.util.ArrayList();
% Get nb Samples, sample frequency and signals names on first file     
fid = fopen(files{1});
line = fgetl(fid);
tempValue = explode(line, ',');
nbSamples = str2double(tempValue{15});
sampleFrequency = str2double(tempValue{7});    
fgetl(fid);
fgetl(fid);        
fgetl(fid);
fgetl(fid);
fgetl(fid);
fgetl(fid);
line = fgetl(fid);
tempValue = explode(line, ',');
for p=1:length(tempValue)
    tempValue2 = tempValue(p);
    if(~strcmp(tempValue2, 'Time (Seconds)') && ~strcmp(tempValue2, 'Frame'))                
        if(~strcmp(tempValue2, ''))
            channelName = tempValue2{1};
            channelsNames.add(channelName); 
        end
    end
end
fclose(fid);

createdChannelsNames = java.util.ArrayList();
for trialNumber=1:nbTrials
    
    data = dlmread(files{trialNumber}, ',', 8, 2);
    
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
     
        eval(['subject.', channelName, '.NbSamples(trialNumber) = ', int2str(length(data(:,1))),';']);            
        eval(['subject.', channelName, '.EndCut(trialNumber) = ', int2str(length(data(:,1))),';']);            
        eval(['subject.', channelName, '.FrontCut(trialNumber) = 0;']);
        
        dataSize = length(data(:,numChannel));
        eval(['subject.', channelName, '.Values(', 'trialNumber,1:dataSize) = data(:,numChannel)'';']);
        
    end    
end

eval('subject.Category1.Criteria = ''Session1'';');
eval('subject.Category1.TrialsList = 1:nbTrials;');
eval('subject.Category1.isSignal = 0;');
eval('subject.Category1.isCategory = 1;');
eval('subject.Category1.isEvent = 0;');
