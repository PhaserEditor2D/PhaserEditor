namespace colibri.core.io {

    export declare type RenameData = {

        oldName: string,

        newFile: FilePath
    }

    export class FileStorageChange {

        private _modified: FilePath[];
        private _modifiedFileNameSet: Set<string>;
        private _added: FilePath[];
        private _deleted: FilePath[];
        private _deletedFileNameSet: Set<string>;
        private _renameData: RenameData[];

        constructor(modified: FilePath[], added: FilePath[], deleted: FilePath[]) {

            this._modified = modified;

            this._modifiedFileNameSet = new Set(modified.map(file => file.getFullName()));

            this._added = added;

            this._deleted = deleted;

            this._deletedFileNameSet = new Set(deleted.map(file => file.getFullName()));

            this._renameData = [];
        }

        addRenameData(newFile: FilePath, oldName: string) {
            this._renameData.push({
                newFile: newFile,
                oldName: oldName
            });
        }

        getRenamedFile(oldFile : FilePath) : FilePath {

            for(const data of this._renameData) {
                
                const parent = data.newFile.getParent();

                if (oldFile.getParent() === parent && oldFile.getName() === data.oldName) {
                    return data.newFile;
                }
            }

            return null;
        }

        isModified(file: FilePath): boolean {
            return this._modifiedFileNameSet.has(file.getFullName());
        }

        isDeleted(file: FilePath): boolean {
            return this._deletedFileNameSet.has(file.getFullName());
        }

        getAddedFiles(): FilePath[] {
            return this._added;
        }

        getModifiedFiles(): FilePath[] {
            return this._modified;
        }

        getDeletedFiles(): FilePath[] {
            return this._deleted;
        }

    }

}