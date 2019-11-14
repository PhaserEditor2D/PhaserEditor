namespace colibri.core.io {

    async function apiRequest(method: string, body?: any) {
        try {

            const resp = await fetch("api", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    "method": method,
                    "body": body
                })
            });

            const json = await resp.json();

            return json;

        } catch (e) {
            console.error(e);
            return new Promise((resolve, reject) => {
                resolve({
                    error: e.message
                });
            });
        }
    }

    export class FileStorage_HTTPServer implements IFileStorage {

        private _root: FilePath;
        private _changeListeners: ChangeListenerFunc[];

        constructor() {

            this._root = null;

            this._changeListeners = [];

        }

        addChangeListener(listener: ChangeListenerFunc) {
            this._changeListeners.push(listener);
        }

        getRoot(): FilePath {
            return this._root;
        }

        async reload(): Promise<void> {

            const data = await apiRequest("GetProjectFiles");

            const oldRoot = this._root;

            const newRoot = new FilePath(null, data);

            this._root = newRoot;

            if (oldRoot) {

                const change = FileStorage_HTTPServer.compare(oldRoot, newRoot);
                this.fireChange(change);
            }
        }

        private fireChange(change: FileStorageChange) {

            for (const listener of this._changeListeners) {
                try {
                    listener(change);
                } catch (e) {
                    console.error(e);
                }
            }
        }

        private static compare(oldRoot: FilePath, newRoot: FilePath): FileStorageChange {

            const oldFiles: FilePath[] = [];
            const newFiles: FilePath[] = [];

            oldRoot.flatTree(oldFiles, false);
            newRoot.flatTree(newFiles, false);

            const newNameMap = new Map<string, FilePath>();

            for (const file of newFiles) {
                newNameMap.set(file.getFullName(), file);
            }

            const newNameSet = new Set(newFiles.map(file => file.getFullName()));
            const oldNameSet = new Set(oldFiles.map(file => file.getFullName()));

            const deleted = [];
            const modified = [];
            const added = [];

            for (const oldFile of oldFiles) {

                const oldName = oldFile.getFullName();

                if (newNameSet.has(oldName)) {

                    const newFile = newNameMap.get(oldName);

                    if (newFile.getModTime() !== oldFile.getModTime()) {
                        modified.push(newFile);
                    }

                } else {
                    deleted.push(oldFile);
                }
            }

            for (const newFile of newFiles) {

                if (!oldNameSet.has(newFile.getFullName())) {
                    added.push(newFile);
                }
            }

            return new FileStorageChange(modified, added, deleted);
        }

        async createFile(folder: FilePath, fileName: string, content: string): Promise<FilePath> {

            const file = new FilePath(folder, {
                children: [],
                isFile: true,
                name: fileName,
                size: 0,
                modTime: 0
            });

            await this.setFileString_priv(file, content);

            folder["_files"].push(file);
            folder["sort"]();

            this.fireChange(new FileStorageChange([], [file], []));

            return file;
        }

        async createFolder(container: FilePath, folderName: string): Promise<FilePath> {
            const newFolder = new FilePath(container, {
                children: [],
                isFile: false,
                name: folderName,
                size: 0,
                modTime: 0
            });

            const path = container.getFullName() + "/" + folderName;

            const data = await apiRequest("CreateFolder", {
                path: path
            });

            if (data.error) {
                alert(`Cannot create folder at '${path}'`);
                throw new Error(data.error);
            }

            newFolder["_modTime"] = data["modTime"];
            container["_files"].push(newFolder);
            container["_files"].sort((a, b) => a.getName().localeCompare(b.getName()));

            this.fireChange(new FileStorageChange([], [newFolder], []));

            return newFolder;
        }

        async getFileString(file: FilePath): Promise<string> {

            const data = await apiRequest("GetFileString", {
                path: file.getFullName()
            });

            if (data.error) {
                alert(`Cannot get file content of '${file.getFullName()}'`);
                return null;
            }

            const content = data["content"];

            return content;
        }

        async setFileString(file: FilePath, content: string): Promise<void> {

            await this.setFileString_priv(file, content);

            this.fireChange(new FileStorageChange([file], [], []));
        }

        private async setFileString_priv(file: FilePath, content: string): Promise<void> {

            const data = await apiRequest("SetFileString", {
                path: file.getFullName(),
                content: content
            });

            if (data.error) {
                alert(`Cannot set file content to '${file.getFullName()}'`);
                throw new Error(data.error);
            }

            file["_modTime"] = data["modTime"];
            file["_fileSize"] = data["size"];
        }

        async deleteFiles(files: FilePath[]) {
            const data = await apiRequest("DeleteFiles", {
                paths: files.map(file => file.getFullName())
            });

            if (data.error) {
                alert(`Cannot delete the files.`);
                throw new Error(data.error);
            }

            const deletedSet = new Set<FilePath>();

            for (const file of files) {

                deletedSet.add(file);

                for (const file2 of file.flatTree([], true)) {
                    deletedSet.add(file2);
                }
            }

            const deletedList: FilePath[] = [];

            for (const file of deletedSet) {
                deletedList.push(file);
            }

            for (const file of deletedList) {
                file.remove();
            }

            this.fireChange(new FileStorageChange([], [], deletedList));
        }

        async renameFile(file: FilePath, newName: string) {

            const oldName = file.getName();

            const data = await apiRequest("RenameFile", {
                oldPath: file.getFullName(),
                newPath: file.getParent().getFullName() + "/" + newName
            });

            if (data.error) {
                alert(`Cannot rename the file.`);
                throw new Error(data.error);
            }

            file["setName"](newName);

            const change = new FileStorageChange([], [file], []);

            change.addRenameData(file, oldName);

            this.fireChange(change);
        }

        async moveFiles(movingFiles: FilePath[], moveTo: FilePath): Promise<void> {

            const data = await apiRequest("MoveFiles", {
                movingPaths: movingFiles.map(file => file.getFullName()),
                movingToPath: moveTo.getFullName()
            });

            if (data.error) {
                alert(`Cannot move the files.`);
                throw new Error(data.error);
            }

            for (const srcFile of movingFiles) {

                const i = srcFile.getParent().getFiles().indexOf(srcFile);
                srcFile.getParent().getFiles().splice(i, 1);
                
                srcFile["_parent"] = moveTo;
                
                moveTo.getFiles().push(srcFile);
            }

            moveTo["sort"]();

            const change = new FileStorageChange([], [], movingFiles);

            this.fireChange(change);
        }
    }
}