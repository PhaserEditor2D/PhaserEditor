namespace colibri.core.io {

    export declare type RenameData = {

        oldName: string,

        newFile: FilePath
    }

    export class FileStorageChange {

        private _renameRecords: Set<string>;
        private _renameFromToMap: Map<string, string>;
        private _deletedRecords: Set<string>;
        private _addedRecords: Set<string>;
        private _modifiedRecords: Set<string>;

        constructor() {

            this._renameRecords = new Set();
            this._deletedRecords = new Set();
            this._addedRecords = new Set();
            this._modifiedRecords = new Set();
        }

        recordRename(fromPath: string, toPath: string) {
            this._renameRecords.add(fromPath);
            this._renameFromToMap[fromPath] = toPath;
        }

        getRenameTo(fromPath: string) {
            return this._renameFromToMap[fromPath];
        }

        recordDelete(path : string) {
            this._deletedRecords.add(path);
        }

        isDeleted(path : string) {
            return this._deletedRecords.has(path);
        }

        recordAdd(path : string) {
            this._addedRecords.add(path);
        }

        isAdded(path : string) {
            return this._addedRecords.has(path);
        }

        getAddRecords() {
            return this._addedRecords;
        }

        recordModify(path : string) {
            this._modifiedRecords.add(path);
        }

        isModified(path : string) {
            return this._modifiedRecords.has(path);
        }
    }
}