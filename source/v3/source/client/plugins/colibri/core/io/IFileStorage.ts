namespace colibri.core.io {

    export declare type ChangeListenerFunc = (change : FileStorageChange) => void;

    export interface IFileStorage {

        reload(): Promise<void>;

        getProjects() : Promise<string[]>;

        openProject(projectName : string) : Promise<FilePath>;

        getRoot(): FilePath;

        getFileString(file: FilePath): Promise<string>;

        setFileString(file: FilePath, content: string): Promise<void>;

        createFile(container : FilePath, fileName : string, content : string): Promise<FilePath>;

        createFolder(container : FilePath, folderName : string) : Promise<FilePath>;

        deleteFiles(files : FilePath[]) : Promise<void>;

        renameFile(file : FilePath, newName : string) : Promise<void>;

        moveFiles(movingFiles: FilePath[], moveTo: FilePath) : Promise<void>;

        uploadFile(uploadFolder : FilePath, file : File) : Promise<FilePath>;

        addChangeListener(listener : ChangeListenerFunc) : void;

        removeChangeListener(listener : ChangeListenerFunc): void;
    }

}