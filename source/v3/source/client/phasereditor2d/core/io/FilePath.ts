namespace phasereditor2d.core.io {
    const EMPTY_FILES = [];

    export class FilePath {
        private _parent: FilePath;
        private _name: string;
        private _isFile: boolean;
        private _files: FilePath[];
        private _ext: string;
        private _id: string;
        private _modTime: number;
        private _fileSize: number;

        constructor(parent: FilePath, fileData: FileData) {
            this._parent = parent;
            this._name = fileData.name;
            this._isFile = fileData.isFile;
            this._fileSize = fileData.size;
            this._modTime = fileData.modTime;

            {
                const i = this._name.lastIndexOf(".");
                if (i >= 0) {
                    this._ext = this._name.substring(i + 1);
                } else {
                    this._ext = "";
                }
            }

            if (fileData.children) {
                this._files = [];
                for (let child of fileData.children) {
                    this._files.push(new FilePath(this, child));
                }
                this._files.sort((a, b) => {
                    const a1 = a._isFile ? 1 : 0;
                    const b1 = b._isFile ? 1 : 0;
                    return a1 - b1;
                });
            } else {
                this._files = EMPTY_FILES;
            }
        }

        getExtension() {
            return this._ext;
        }

        getName() {
            return this._name;
        }

        getId() {
            if (this._id) {
                return this._id;
            }

            this._id = this.getFullName() + "@" + this._modTime + "@" + this._fileSize;
        }

        getFullName() {
            if (this._parent) {
                return this._parent.getFullName() + "/" + this._name;
            }
            return this._name;
        }

        getUrl() {
            if (this._parent) {
                return this._parent.getUrl() + "/" + this._name;
            }

            return "../project";
        }

        getParent() {
            return this._parent;
        }

        isFile() {
            return this._isFile;
        }

        isFolder() {
            return !this.isFile();
        }

        getFiles() {
            return this._files;
        }

        toString() {
            if (this._parent) {
                return this._parent.toString() + "/" + this._name;
            }

            return this._name;
        }

        toStringTree() {
            return this.toStringTree2(0);
        }

        private toStringTree2(depth: number) {
            let s = " ".repeat(depth * 4);
            s += this.getName() + (this.isFolder() ? "/" : "") + "\n";
            if (this.isFolder()) {
                for (let file of this._files) {
                    s += file.toStringTree2(depth + 1);
                }
            }
            return s;
        }
    }
}