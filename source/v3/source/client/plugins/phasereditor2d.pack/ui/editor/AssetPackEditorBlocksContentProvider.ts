namespace phasereditor2d.pack.ui.editor {

    import controls = colibri.ui.controls;
    import io = colibri.core.io;

    export class AssetPackEditorBlocksContentProvider extends files.ui.viewers.FileTreeContentProvider {

        private _editor: AssetPackEditor;
        private _ignoreFileSet: Set<io.FilePath>;

        constructor(editor: AssetPackEditor) {
            super();

            this._editor = editor;
            this._ignoreFileSet = new Set();
        }

        getIgnoreFileSet() {
            return this._ignoreFileSet;
        }

        async updateIgnoreFileSet_async() {

            let packs = (await core.AssetPackUtils.getAllPacks())
                .filter(pack => pack.getFile() !== this._editor.getInput());

            this._ignoreFileSet = new Set();

            for (const pack of packs) {
                pack.computeUsedFiles(this._ignoreFileSet);
            }

            this._editor.getPack().computeUsedFiles(this._ignoreFileSet);
        }

        getRoots(input: any): any[] {

            return super.getRoots(input)

                .filter(obj => this.isFileOrNotEmptyFolder(obj))
        }

        getChildren(parent: any): any[] {
            
            return super.getChildren(parent)

                .filter(obj => this.isFileOrNotEmptyFolder(obj));
        }

        private isFileOrNotEmptyFolder(parent: io.FilePath) {

            if (parent.isFile() && !this._ignoreFileSet.has(parent)) {
                return true;
            }

            for (const file of parent.getFiles()) {
                if (this.isFileOrNotEmptyFolder(file)) {
                    return true;
                }
            }

            return false;
        }
    }

}