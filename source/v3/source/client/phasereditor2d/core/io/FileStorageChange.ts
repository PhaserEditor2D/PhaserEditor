namespace phasereditor2d.core.io {

    export class FileStorageChange {

        private _modified: FilePath[];
        private _modifiedFileNameSet: Set<string>;
        private _added: FilePath[];
        private _deleted: FilePath[];
        private _deletedFileNameSet: Set<string>;

        constructor(modified: FilePath[], added: FilePath[], deleted: FilePath[]) {

            this._modified = modified;

            this._modifiedFileNameSet = new Set(modified.map(file => file.getFullName()));

            this._added = added;

            this._deleted = deleted;

            this._deletedFileNameSet = new Set(deleted.map(file => file.getFullName()));
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