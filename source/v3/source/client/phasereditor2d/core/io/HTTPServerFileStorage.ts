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

            const data = await apiRequest("SetFileString", {
                path: file.getFullName(),
                content: content
            });

            if (data.error) {
                alert(`Cannot set file content to '${file.getFullName()}'`);
                throw new Error(data.error);
            }

            file["_modTime"] = data["modTime"];
            
            this.fireChange(new FileStorageChange([file], [], []));
        }
    }

}