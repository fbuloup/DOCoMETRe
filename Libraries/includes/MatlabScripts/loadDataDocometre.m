function subject = loadDataDocometre(dataFilesList, varargin) %, options)
      
if contains(dataFilesList,'.sau,')
	% loadDataDocometreSAU(dataFilesList);
elseif contains(dataFilesList,'.samples;')
    subject = loadDataDocometreSAMPLES(dataFilesList, varargin{1}, varargin{2});
else
    
%% Here we are : this is ADW data file, only one file in data files list
tempSubject = {};
sessionsProperties = containers.Map(varargin{1}, varargin{2});
paddWithLastValue = sessionsProperties('PADD_WITH_LAST_VALUE_RATHER_THAN_ZERO');


fhandle = fopen(dataFilesList, 'rb','l');
		
% Nb channels
nbChannels = fread(fhandle,1,'int32');
channelsNames = readNames(fhandle, nbChannels);
	
% Nb sessions
nbSessions = fread(fhandle,1,'int32');
sessionsNames = readNames(fhandle, nbSessions);
		
% Nb conditions
nbConditions = fread(fhandle,1,'int32');
conditionsNames = readNames(fhandle, nbConditions);
		
% Nb seq. type
nbSeqTypes = fread(fhandle,1,'int32');
seqTypesNames = readNames(fhandle, nbSeqTypes);
		
% Nb trials
nbTrials = fread(fhandle,1,'int32');

nbTotalCategories = nbSessions*nbConditions*nbSeqTypes;
		
criteria = cell(1,nbTrials);
criteria(:) = {""};
createdCategories = containers.Map;
Categories = [];
		
for currentTrialNumber = 1:nbTrials
	currentSession = fread(fhandle,1,'int32');
	currentCondition = fread(fhandle,1,'int32');
	currentSeqType = fread(fhandle,1,'int32');
	nbChannelsInTrial = fread(fhandle,1,'int32');
	criteria = [char(sessionsNames{currentSession}) '_' char(seqTypesNames{currentSeqType})];
    Categories = [Categories; {criteria}];
	if isKey(createdCategories, criteria)
	    append = false;
	    trialsList = createdCategories(criteria);
        if isempty(find(trialsList == currentTrialNumber))
            append = true;
        end
	    if append
	        trialsList = [trialsList, currentTrialNumber];
        end
	else
	    trialsList = currentTrialNumber;
    end
	createdCategories(criteria) = trialsList;
	if nbChannelsInTrial > 0
	    for currentChannelNumber = 1:nbChannelsInTrial
	        channelNumber = fread(fhandle,1,'int32');
	        channelName = char(channelsNames{channelNumber});
            fullSignalName = ['tempSubject.' channelName];
	        sampleFrequency = fread(fhandle,1,'float32');
	        nbSamples = fread(fhandle,1,'int32');
	        if nbSamples > 0
	            newValues = fread(fhandle, nbSamples,'float32');
	            if isfield(tempSubject, channelName)
                    eval(['values = ' fullSignalName '.Values;']);
	                currentNbSamples = size(values, 2);
	                if(currentNbSamples > nbSamples)
	                	if(strcmp(paddWithLastValue, 'true'))
	                		paddValues = newValues(nbSamples)*ones(1,currentNbSamples - nbSamples);
	                	else
	                		paddValues = zeros(1,currentNbSamples - nbSamples);
	                	end
                        newValues = [newValues', paddValues];
                    elseif(currentNbSamples < nbSamples)
                    	if(strcmp(paddWithLastValue, 'true'))
                    		lastSamples = values(:, end);
                    		paddValues = repmat(lastSamples, 1, nbSamples - currentNbSamples);
                    	else
                    		paddValues = zeros(nbTrials, nbSamples - currentNbSamples);
                    	end
                        values = [values, paddValues];
                        eval([fullSignalName '.Values = values;']);
                    end
	                eval([fullSignalName '.Values(currentTrialNumber,:) = newValues;']);
		        else
	                eval([fullSignalName '.Values = zeros(nbTrials, nbSamples);']);
	                eval([fullSignalName '.NbSamples = nbSamples*ones(nbTrials, 1);']);
	                eval([fullSignalName '.FrontCut = zeros(nbTrials, 1);']);
	                eval([fullSignalName '.EndCut = nbSamples*ones(nbTrials, 1);']);
	                eval([fullSignalName '.Values(currentTrialNumber,:) = newValues;']);
                end
	            eval([fullSignalName '.EndCut(currentTrialNumber) = nbSamples;']);
	            eval([fullSignalName '.isSignal = 1;']);
	            eval([fullSignalName '.isCategory = 0;']);
	            eval([fullSignalName '.isEvent = 0;']);
	            eval([fullSignalName '.NbFeatures = 0;']);
	            eval([fullSignalName '.NbMarkersGroups = 0;']);
	            eval([fullSignalName '.SampleFrequency = sampleFrequency;']);
            end
        end
    end
end
	
n = 1;
for criteria = keys(createdCategories)
    values = createdCategories(criteria{1});
    eval(['tempSubject.Category' num2str(n) '.Criteria = criteria{1};']);
    eval(['tempSubject.Category' num2str(n) '.TrialsList = values'''';']);
    eval(['tempSubject.Category' num2str(n) '.isSignal = 0;']);
    eval(['tempSubject.Category' num2str(n) '.isCategory = 1;']);
    eval(['tempSubject.Category' num2str(n) '.isEvent = 0;']);
    n = n + 1;
end
tempSubject.Categories.Names = Categories;
tempSubject.Categories.isCategory = 0;
tempSubject.Categories.isSignal = 0;
tempSubject.Categories.isEvent = 0;

subject = tempSubject;
fclose(fhandle);

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