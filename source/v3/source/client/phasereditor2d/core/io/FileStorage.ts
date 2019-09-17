namespace phasereditor2d.core.io {

    export interface IFileStorage {
        reload(): Promise<FilePath>;

        getRoot(): FilePath;

        getFileString(file : FilePath) : Promise<string>;
    }

    function makeApiRequest(method: string, body?: any): Promise<Response> {
        return fetch("../api", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                "method": method,
                "body": body
            })
        });
    }

    export class ServerFileStorage implements IFileStorage {
        
        private _root : FilePath;

        public getRoot(): FilePath {
            return this._root;
        }

        public async reload(): Promise<FilePath> {
            const resp = await makeApiRequest("GetProjectFiles");
            const data = await resp.json();

            //TODO: handle error
            const self = this;

            return new Promise(function (resolve, reject) {
                self._root = new FilePath(null, data);
                resolve(self._root);
            });
        }

        public async getFileString(file: FilePath): Promise<string> {
            const resp = await makeApiRequest("GetFileString", {
                path: file.getFullName()
            });
            const data = await resp.json();
            return new Promise(function (resolve, reject) {
                resolve(data["content"]);
            });
        }
    }
}