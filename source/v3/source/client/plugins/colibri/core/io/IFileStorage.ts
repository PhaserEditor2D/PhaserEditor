namespace colibri.core.io {

    export declare type ChangeListenerFunc = (change : FileStorageChange) => void;

    export interface IFileStorage {

        reload(): Promise<void>;

        getRoot(): FilePath;

        getFileString(file: FilePath): Promise<string>;

        setFileString(file: FilePath, content: string): Promise<void>;

        createFile(container : FilePath, fileName : string, content : string): Promise<FilePath>;

        createFolder(container : FilePath, folderName : string) : Promise<FilePath>;

        deleteFiles(files : FilePath[]);

        renameFile(file : FilePath, newName : string);

        addChangeListener(listener : ChangeListenerFunc);

    }

}