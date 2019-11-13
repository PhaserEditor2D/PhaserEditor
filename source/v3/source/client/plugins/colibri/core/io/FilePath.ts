namespace colibri.core.io {
    const EMPTY_FILES = [];

    export class FilePath {

        private _parent: FilePath;
        private _name: string;
        private _nameWithoutExtension: string;
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
                    this._nameWithoutExtension = this._name.substring(0, i);
                } else {
                    this._ext = "";
                    this._nameWithoutExtension = this._name;
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

        getSize(): number {
            return this.isFile() ? this._fileSize : 0;
        }

        getName() {
            return this._name;
        }

        getNameWithoutExtension() {
            return this._nameWithoutExtension;
        }

        getModTime() {
            return this._modTime;
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

            return "./project";
        }

        getSibling(name: string) {
            
            const parent = this.getParent();

            if (parent) {
                return parent.getFile(name);
            }
            return null;
        }

        getFile(name: string) {
            return this.getFiles().find(file => file.getName() === name);
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

        remove() {

            if (this._parent) {

                const list = this._parent._files;
                const i = list.indexOf(this);

                if (i >= 0) {
                    list.splice(i, 1);
                }
            }
        }

        flatTree(files: FilePath[], includeFolders: boolean): FilePath[] {

            if (this.isFolder()) {

                if (includeFolders) {
                    files.push(this);
                }

                for (const file of this.getFiles()) {
                    file.flatTree(files, includeFolders);
                }

            } else {

                files.push(this);

            }

            return files;
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