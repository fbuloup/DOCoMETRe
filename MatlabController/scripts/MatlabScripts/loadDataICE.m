function loadDataICE(experimentName,subjectName, dataFile) %, options)

[filesList, nbFiles] = explode(dataFile , ',');
for numFile=1:nbFiles
    dataFilesList(numFile) = filesList(numFile);
end
if(filesep == '\')
    dataFolder = regexprep(dataFilesList{1},'\\\w*\.txt$','');
else
    dataFolder = regexprep(dataFilesList{1},[filesep, '\w*\.txt$'],'');
end
if (isempty(dataFolder) == 1)
    dataFolder = '.';
end
D=dir(dataFolder);
dataFilesList = [];
n = 1;
for numFile=1:numel(D)
    if isempty(regexp(D(numFile,1).name,'\w+\.txt$'))==0;
        dataFilesList{n} = D(numFile,1);
        n= n+1;
    end
end

f = fopen([dataFolder,filesep,dataFilesList{1}.name]);
signalsLine = fgetl(f);
asignals = explode(signalsLine,char(9));
j=1;
for i=2:numel(asignals)
    signals(j)=asignals(i);
    j=j+1;
end
fclose(f);

if isempty(regexp(dataFilesList{1}.name,'^(\d+){3}_'))==0
    for numFile2=1:numel(dataFilesList)-1
        indicemax=numFile2;
        e = explode(dataFilesList{indicemax}.name, '_');
        ex = e{1};
        for numFile=1:numel(dataFilesList)
            z = explode(dataFilesList{numFile}.name, '_');
            zx = z{1};
            if str2num(zx) < str2num(ex)
                    indicemax = numFile;
                    permut = dataFilesList(numFile);
                    dataFilesList(numFile)=dataFilesList(indicemax);
                    dataFilesList(indicemax)=permut;
            end
            e = explode(dataFilesList{indicemax}.name, '_');
            ex = e{1};
        end
    end
else
    for numFile2=1:numel(dataFilesList)-1
        indicemax = numFile2;
        for numFile=1:numel(dataFilesList)
            if dataFilesList{numFile}.datenum < dataFilesList{indicemax}.datenum
                indicemax = numFile;
                permut = dataFilesList(numFile);
                dataFilesList(numFile)=dataFilesList(indicemax);
                dataFilesList(indicemax)=permut;
            end
        end     
    end
    for numFile=1:numel(dataFilesList)
        oldName = [dataFolder, filesep, dataFilesList{numFile}.name];
        newFileName = dataFilesList{numFile}.name;
        if numFile <10
            movefile([dataFolder, filesep, dataFilesList{numFile}.name],[dataFolder, filesep,'00',num2str(numFile),'_',newFileName]);
            dataFilesList{numFile}.name = ['00',num2str(numFile),'_',newFileName];
        else if numFile <100
                movefile([dataFolder, filesep, dataFilesList{numFile}.name],[dataFolder, filesep,'0',num2str(numFile),'_',newFileName]);
                dataFilesList{numFile}.name = ['0',num2str(numFile),'_',newFileName];
            else
                movefile([dataFolder, filesep, dataFilesList{numFile}.name],[dataFolder, filesep,num2str(numFile),'_',newFileName]);
                dataFilesList{numFile}.name = [num2str(numFile),'_',newFileName];
            end
        end
        oldName = regexptranslate('escape', oldName);
        dataFilesList{numFile}.name=regexprep(dataFilesList{numFile}.name,'\\','\\\\');
        dataFile = regexprep(dataFile,oldName,dataFilesList{numFile}.name);    
    end 
end
%--------------------------------------------------------------------------
%fin du tri et du renommage
%--------------------------------------------------------------------------
frequenciesMeans=[];
for numFile=1:numel(dataFilesList)
    fileValues = dlmread([dataFolder,filesep,dataFilesList{numFile}.name], '\t',1,0);
    sizeOfValues = size(fileValues);
    forMean = [];
    for numLine = 2:sizeOfValues(1)
        forMean = [forMean; fileValues(numLine,1)-fileValues(numLine-1,1)];
    end
    frequenciesMeans = [frequenciesMeans; mean(forMean)];
end
frequencyMean = mean(frequenciesMeans);
frequencyMean = 1/frequencyMean * 1000;%Time is ms in ICE data files
%--------------------------------------------------------------------------
%fin de moyenne des moyennes
%--------------------------------------------------------------------------
numCategory=1;
numAlreadySavedSignals = 1;
for numFile=1:numel(dataFilesList)
%----------------------------------------------------------------------
    tempfileValues = dlmread([dataFolder,filesep,dataFilesList{numFile}.name], '\t',1,0);
    sizeOfValues = size(tempfileValues);
    formerTime=tempfileValues(:,1);
    formerTime = formerTime-formerTime(1);
    newTime = 1000*((0:sizeOfValues(1)-1)/frequencyMean)';
%----------------------------------------------------------------------
    fileValues = dlmread([dataFolder,filesep,dataFilesList{numFile}.name], '\t',1,1);
    sizeOfValues = size(fileValues);
    fileValues = fileValues(:,1:sizeOfValues(2));
    resampledFileValues =  interp1(formerTime,fileValues,newTime,'linear');
    fileElem = regexprep(dataFilesList{numFile}.name, [dataFolder, filesep],'');
    fileElem = explode(fileElem, '_');
    numTrial = fileElem{1};
    if str2num(numTrial)<10
        numTrial = regexprep(numTrial,'^00','');
    else if str2num(numTrial)<100
           numTrial = regexprep(numTrial,'^0',''); 
        end
    end
    category = fileElem{2};
    variables = who;
    subjectExists = 0;
    categoryExists = 0;
    for numVariables = 1 : numel(variables)
        if strcmp(variables{numVariables},subjectName)~=0
            subjectExists = 1;
        end
    end
    if subjectExists~=0
        for numSavedCategories=1:numel(savedCategories)
            if strcmp(savedCategories(numSavedCategories),category)~=0
                categoryExists = 1;
            end
        end
    end
    if categoryExists == 0 || subjectExists == 0
        eval([subjectName, '.Category',num2str(numCategory),'.Criteria = category;']);
        eval([subjectName, '.Category',num2str(numCategory),'.isCategory = 1;']);
        eval([subjectName, '.Category',num2str(numCategory),'.isEvent = 0;']);
        eval([subjectName, '.Category',num2str(numCategory),'.isSignal = 0;']);
        eval([subjectName, '.Category',num2str(numCategory),'.TrialsList = [];']);
        savedCategories{numCategory} = category;
        numCategory=numCategory+1;
    end
    for numSavedCategories=1:numel(savedCategories)
        if strcmp(savedCategories(numSavedCategories),category)~=0
            eval([subjectName, '.Category',num2str(numSavedCategories),'.TrialsList = [',subjectName, '.Category',num2str(numSavedCategories),'.TrialsList,str2num(numTrial)];']);
        end
    end
    for numSignals = 1:numel(signals)
        signalValues = resampledFileValues(:,numSignals)';
        nbSamples2 = size(signalValues);
        nbSamples = nbSamples2(2);
        signalExists = 0;
        if numFile ~=1
            for numSavedSignals=1:numel(savedSignals)
                if strcmp(savedSignals(numSavedSignals),signals{numSignals})~=0
                    signalExists = 1;
                end
            end
        end
        if signalExists ~=1
            signals{numSignals} = regexprep(signals{numSignals},'\.','');
            eval([subjectName, '.', signals{numSignals}, '.isSignal = 1;']);
            eval([subjectName, '.', signals{numSignals}, '.isCategory = 0;']);
            eval([subjectName, '.', signals{numSignals}, '.isEvent = 0;']);    
            eval([subjectName, '.', signals{numSignals}, '.FrontCut = [1];']);
            eval([subjectName, '.', signals{numSignals}, '.SampleFrequency =  frequencyMean;']); 
            eval([subjectName, '.', signals{numSignals}, '.NbMarkers =  0;']); 
            eval([subjectName, '.', signals{numSignals}, '.NbFields = 0;']); 
            eval([subjectName, '.', signals{numSignals}, '.EndCut = nbSamples;']); 
            eval([subjectName, '.', signals{numSignals}, '.NbSamples = nbSamples;']);
            eval([subjectName, '.', signals{numSignals}, '.Values = signalValues;']);
            savedSignals{numAlreadySavedSignals} = signals{numSignals};
            numAlreadySavedSignals = numAlreadySavedSignals+1;
        else
            eval(['nbValuesSamples = length(', subjectName, '.', signals{numSignals}, '.Values);']);
            if(nbValuesSamples > nbSamples)
                signalValues=[signalValues, zeros(1,nbValuesSamples-nbSamples)];   
                nbSamples=nbValuesSamples;
            end
            if(nbValuesSamples < nbSamples)
                nbZeros = nbSamples - nbValuesSamples;
                eval(['ValuesSize = size(', subjectName, '.', signals{numSignals}, '.Values);nbTrials=ValuesSize(1);']);
                expression = [subjectName, '.', signals{numSignals}, '.Values = ['];
                expression = [expression, subjectName, '.', signals{numSignals}, '.Values'];
                expression = [expression, ', zeros(nbTrials,', num2str(nbZeros), ')];'];
                eval(expression);
                eval([subjectName,'.', signals{numSignals}, '.EndCut = nbSamples*ones(n-1,1);']);
                eval([subjectName,'.', signals{numSignals}, '.NbSamples = nbSamples*ones(n-1,1);']);
            end
            eval([subjectName, '.', signals{numSignals}, '.Values =[',subjectName, '.', signals{numSignals}, '.Values; signalValues] ;']); 
            eval([subjectName, '.', signals{numSignals}, '.NbSamples = [', subjectName, '.', signals{numSignals}, '.NbSamples ; nbSamples];']);
            eval([subjectName, '.', signals{numSignals}, '.FrontCut = [', subjectName, '.', signals{numSignals}, '.FrontCut ; 1];']);
            eval([subjectName, '.', signals{numSignals}, '.EndCut = [', subjectName, '.', signals{numSignals}, '.EndCut ; nbSamples];']); 
        end
    end
end
eval(['subjectNameTemp = ', subjectName, ';']);
assignin('base','tempSubject',subjectNameTemp);
evalin('base', [experimentName,'.', subjectName,'=tempSubject;']);
evalin('base','clear tempSubject;');
end











