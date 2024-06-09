function loadDataDocometreSAU(experimentName,subjectName, filesList) %, options)

global dataFilesNamesList;

[dataFilesNamesList, sampleFrequenciesFileName] = retrieveFiles(filesList);
[signalsNames, signalsSampleFrequencies] = getSignalsNamesAndSampleFrequencies(sampleFrequenciesFileName);
dataFolder = getRootDataFolder(dataFilesNamesList{1});
sortRenameDataFiles(dataFolder{1}, signalsNames);

categoriesNames=[];
categories=[];
p=1;
%For each signal
for l=1:numel(signalsNames)
    signalsNames{l}
    %Get all its files
    allowedDataFilesNames = getAllowedDataFilesNames(dataFolder, signalsNames{l});    
    %For each of these files
    for n=1:numel(allowedDataFilesNames)
        %Open it to read data and create signal analyse model
        currentDataFiles = fopen(char(allowedDataFilesNames{n}));
        values = fread(currentDataFiles,'float32')';
        nbSamples = length(values);
        fclose(currentDataFiles);                
        if(n==1)
            eval([subjectName, '.', signalsNames{l}, '.Values = values;']);
            eval([subjectName, '.', signalsNames{l}, '.isSignal = 1;']);
            eval([subjectName, '.', signalsNames{l}, '.isCategory = 0;']);
            eval([subjectName, '.', signalsNames{l}, '.isEvent = 0;']);    
            eval([subjectName, '.', signalsNames{l}, '.FrontCut = [1];']);
            eval([subjectName, '.', signalsNames{l}, '.SampleFrequency =  str2num(signalsSampleFrequencies{l});']); 
            eval([subjectName, '.', signalsNames{l}, '.NbMarkersGroups =  0;']); 
            eval([subjectName, '.', signalsNames{l}, '.NbFeatures = 0;']); 
            eval([subjectName, '.', signalsNames{l}, '.EndCut = [nbSamples];']); 
            eval([subjectName, '.', signalsNames{l}, '.NbSamples = [nbSamples];']); 

        else
            eval(['nbValuesSamples = length(', subjectName, '.', signalsNames{l}, '.Values);']);
            if(nbValuesSamples > nbSamples)
                values=[values, zeros(1,nbValuesSamples-nbSamples)];   
                nbSamples=nbValuesSamples;
            end
            if(nbValuesSamples < nbSamples)
                nbZerosToAdd = nbSamples - nbValuesSamples;
                eval(['ValuesSize = size(', subjectName, '.', signalsNames{l}, '.Values);nbTrials=ValuesSize(1);']);
                expression = [subjectName, '.', signalsNames{l}, '.Values = ['];
                expression = [expression, subjectName, '.', signalsNames{l}, '.Values'];
                expression = [expression, ', zeros(nbTrials,', num2str(nbZerosToAdd), ')];'];
                eval(expression);
                eval([subjectName,'.', signalsNames{l}, '.EndCut = nbSamples*ones(n-1,1);']);
                eval([subjectName,'.', signalsNames{l}, '.NbSamples = nbSamples*ones(n-1,1);']);
            end               
            eval([subjectName, '.', signalsNames{l}, '.Values = [', subjectName, '.', signalsNames{l}, '.Values ; values];']);   
            eval([subjectName, '.', signalsNames{l}, '.NbSamples = [', subjectName, '.', signalsNames{l}, '.NbSamples ; nbSamples];']);
            eval([subjectName, '.', signalsNames{l}, '.FrontCut = [', subjectName, '.', signalsNames{l}, '.FrontCut ; 1];']);
            eval([subjectName, '.', signalsNames{l}, '.EndCut = [', subjectName, '.', signalsNames{l}, '.EndCut ; nbSamples];']);
        end
        %Build the whole category if it does not exist
        sauElements = explode(char(allowedDataFilesNames{n}),'_');
        if numel(sauElements) == 4
            currentCategories = [sauElements{1,2},'_',sauElements{1,3}];
            trialNumber = sauElements{1,4};
        else
            currentCategories = sauElements{1,2};
            trialNumber = sauElements{1,3};
        end
        trialNumberExists = 0;
        trialNumber = regexprep(trialNumber, 'E','');
        trialNumber = regexprep(trialNumber, '.sau','');         
        if isempty(categoriesNames)==0
            categoryExists = 0;
            for o=1:numel(categoriesNames)
                if strcmp(currentCategories,categoriesNames{o}) == 1
                    categoryExists = 1;
                end
            end
            if categoryExists == 0
                categoriesNames{p} = currentCategories;
                eval([subjectName, '.Category',num2str(p),'.Criteria = currentCategories;']);
                eval([subjectName, '.Category',num2str(p),'.isCategory = 1;']);
                eval([subjectName, '.Category',num2str(p),'.isEvent = 0;']);
                eval([subjectName, '.Category',num2str(p),'.isSignal = 0;']);
                eval([subjectName, '.Category',num2str(p),'.TrialsList = [];']);
                p=p+1;
                
            end
        else
            categoriesNames{1} = currentCategories;
            eval([subjectName, '.Category1.Criteria = currentCategories;']);
            eval([subjectName, '.Category1.isCategory = 1;']);
            eval([subjectName, '.Category1.isEvent = 0;']);
            eval([subjectName, '.Category1.isSignal = 0;']);
            eval([subjectName, '.Category1.TrialsList = [];']);
            p=p+1;
        end
        eval(['trialsEmpty = isempty(', subjectName, '.Category', num2str(p-1), '.TrialsList);']);
        if trialsEmpty == 1
            for h=1:numel(dataFilesNamesList)
                sauElementsForTrial = explode(char(dataFilesNamesList{h}),'_');
                if numel(sauElementsForTrial) == 4
                    currentCategoriesForTrial = [sauElementsForTrial{1,2},'_',sauElementsForTrial{1,3}];
                    trialNumber = sauElementsForTrial{1,4};
                else
                    currentCategoriesForTrial = sauElementsForTrial{1,2};
                    trialNumber = sauElementsForTrial{1,3};
                end
                trialNumber = regexprep(trialNumber, 'E','');
                trialNumber = regexprep(trialNumber, '.sau','');
                trialNumber=str2num(trialNumber);
                a = regexp(currentCategoriesForTrial,currentCategories);
                if (isempty(a) == 0)
                    eval(['trialAlreadyExists = find(',subjectName, '.Category',num2str(p-1),'.TrialsList == trialNumber);']);
                    if (isempty(trialAlreadyExists)==1)
                        expression = [subjectName, '.Category',num2str(p-1),'.TrialsList = [',subjectName,'.Category',num2str(p-1),'.TrialsList,trialNumber];'];
                        eval(expression);
                    end
                end
            end 
        end
    end
end
eval(['subjectNameTemp = ', subjectName, ';']);
assignin('base','tempSubject',subjectNameTemp);
evalin('base', [experimentName,'.', subjectName,'=tempSubject;']);
evalin('base','clear tempSubject;');
end

function [allowedDataFilesNames] = getAllowedDataFilesNames(dataFolder, signalName)

    global dataFilesNamesList;
    allowedDataFilesNames = [];
    pat = ['^', char(dataFolder), signalName, '_'];
    pat = regexprep(pat,'\\','\\\\');
    
    for i=1:numel(dataFilesNamesList)     
        result = regexp(char(dataFilesNamesList{i}), pat, 'once');
        if (isempty(result) == 0)
            allowedDataFilesNames=[allowedDataFilesNames; {dataFilesNamesList{i}}];
        end
    end
    

end

function [dataFilesNamesList, sampleFrequenciesFileName] = retrieveFiles(filesList)
    [filesList, nbFiles] = explode(filesList , ',');
    dataFilesNamesList = cell(nbFiles-1,1);
    j=1;
    for i=1:nbFiles
        a = regexp(filesList(i), '\.properties$');
        if isempty(a{1}) == 0
            sampleFrequenciesFileName = filesList(i);
        else
            dataFilesNamesList{j} = filesList(i);
            j=j+1;
        end
    end
end

function [signalsNames, signalsSampleFrequencies] = getSignalsNamesAndSampleFrequencies(sampleFrequenciesFileName)
    fid = fopen(sampleFrequenciesFileName{1});
    mline = fgetl(fid);
    firstTime = 1; 
    while ischar(mline)
       if isempty(regexp(mline, '^#', 'once'))
           if(firstTime == 0)
               mlines = [mlines; cellstr(mline)];
           else
               firstTime = 0; 
               mlines = cellstr(mline);
           end   
       end
       mline = fgetl(fid);
    end
    fclose(fid);
    nbChannels = numel(mlines);
    signalsNames = cell(nbChannels,1);
    signalsSampleFrequencies = cell(nbChannels,1);
    for j=1:nbChannels
        z = explode(mlines{j},'=');
        signalsNames{j} =  z{1};
        signalsSampleFrequencies{j} = z{2};
    end
end

function dataFolder = getRootDataFolder(dataFileName)
    dataFolder = regexprep(dataFileName,'\w+\.sau$','');
end

function renameInDataFilesNamesList(oldName, newName)

    global dataFilesNamesList;

    for i = 1:numel(dataFilesNamesList)
        if(strcmp(dataFilesNamesList{i},oldName) == 1)   
            dataFilesNamesList{i} = newName;
            break;
        end        
    end
end

function sortRenameDataFiles(dataFolder, signalsNames)
    if (isempty(dataFolder) == 1)
        dataFolder = '.';
    end
    files = dir(dataFolder);
    for i = 1:numel(signalsNames)        
        n = 1;
        %Get every signal's files names
        for j = 1:numel(files)
            result = regexp(files(j,1).name, ['^',signalsNames{i},'_'], 'once');
            if (isempty(result) == 0)
                allowedDataFiles{n} = files(j,1);
                n = n + 1;
            end
        end    
        %Sort these files by datenum      
        for j = 1:numel(allowedDataFiles) - 1
            maxIndex = j;
            for k = 1:numel(allowedDataFiles)
                if allowedDataFiles{k}.datenum < allowedDataFiles{maxIndex}.datenum
                    maxIndex = k;
                    permut = allowedDataFiles{k};
                    allowedDataFiles{k} = allowedDataFiles{maxIndex};
                    allowedDataFiles{maxIndex} = permut;
                end
            end     
        end
        %Rename if necessary
        for j=1:numel(allowedDataFiles)
            if(isempty(regexp(allowedDataFiles{j}.name,'_E\d+\.sau$'))==1)                         
                newFileName = allowedDataFiles{j}.name;
                newFileName = regexprep(newFileName,'\.sau','');
                oldName = [dataFolder, allowedDataFiles{j}.name];
                newName = [dataFolder, newFileName, '_E', num2str(j),'.sau'];
                movefile(oldName, newName);
                renameInDataFilesNamesList(oldName ,newName);
            end        
        end    
    end
end
