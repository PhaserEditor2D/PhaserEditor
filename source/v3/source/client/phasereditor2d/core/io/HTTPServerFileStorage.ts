namespace phasereditor2d.core.io {

    async function apiRequest(method: string, body?: any) {
        try {

            const resp = await fetch("../api", {
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

    export class HTTPServerFileStorage implements IFileStorage {

        private _root: FilePath;
        private _fileStringContentMap: Map<string, string>;
        private _changeListeners: ChangeListenerFunc[];

        constructor() {

            this._root = null;

            this._fileStringContentMap = new Map();

            this._changeListeners = [];

            this._fileStringContentMap = new Map();
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

                const change = HTTPServerFileStorage.compare(oldRoot, newRoot);
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

                    if (newFile.getId() !== oldFile.getId()) {
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

        hasFileStringInCache(file: FilePath) {
            return this._fileStringContentMap.has(file.getId());
        }

        getFileStringFromCache(file: FilePath) {
            const id = file.getId();

            if (this._fileStringContentMap.has(id)) {
                const content = this._fileStringContentMap.get(id);
                return content;
            }

            return null;
        }

        async getFileString(file: FilePath): Promise<string> {
            const id = file.getId();

            if (this._fileStringContentMap.has(id)) {
                const content = this._fileStringContentMap.get(id);
                return content;
            }

            const data = await apiRequest("GetFileString", {
                path: file.getFullName()
            });

            if (data.error) {
                alert(`Cannot get file content of '${file.getFullName()}'`);
                return null;
            }

            const content = data["content"];
            this._fileStringContentMap.set(id, content);

            return content;
        }

        async setFileString(file: FilePath, content: string): Promise<void> {

            const data = await apiRequest("SetFileString", {
                path: file.getFullName(),
                content: content
            });

            if (data.error) {
                alert(`Cannot set file content to '${file.getFullName()}'`);
                throw new Error(data.error);
            }

            // TODO: get the new timestamp of the file and update it.

            this._fileStringContentMap.set(file.getId(), content);

            this.fireChange(new FileStorageChange([file], [], []));
        }
    }

}