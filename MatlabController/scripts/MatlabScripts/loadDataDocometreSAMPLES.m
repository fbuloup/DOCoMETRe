function subject = loadDataDocometre(dataFilesList)
    
    dataFiles = split(dataFilesList, ';');
     
    % Each data file has format : 
    % ...something/WorkspaceName/ExpName/SubjectName/Session/Trial/channel.samples
    
    nbDataFiles = length(dataFiles);
    
    for n = 1:nbDataFiles
        
        segments = split(dataFiles{n}, '/');        
        
        % Get session name for criteria and trial number
        sessionNameCell = segments(length(segments) - 2);
        trialNameCell = segments(length(segments) - 1);
        criteria = sessionNameCell{1};
        trialName = trialNameCell{1};        
        trialNumberCell = split(trialName, '°');
        trialNumber = str2num(trialNumberCell{2});
        
        % Get sample frequency
        sf= 1000;
        
        % Read data
        
        
    end

end