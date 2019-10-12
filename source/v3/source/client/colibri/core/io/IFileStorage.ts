namespace colibri.core.io {

    export declare type ChangeListenerFunc = (change : FileStorageChange) => void;

    export interface IFileStorage {

        reload(): Promise<void>;

        getRoot(): FilePath;

        getFileString(file: FilePath): Promise<string>;

        setFileString(file: FilePath, content: string): Promise<void>;

        addChangeListener(listener : ChangeListenerFunc);

    }

}