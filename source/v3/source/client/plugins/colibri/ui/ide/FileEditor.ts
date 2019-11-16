namespace colibri.ui.ide {

    import io = core.io;

    export abstract class FileEditor extends EditorPart {

        private _onFileStorageListener: io.ChangeListenerFunc;

        constructor(id: string) {
            super(id);

            this._onFileStorageListener = change => {
                this.onFileStorageChanged(change);
            };

            Workbench.getWorkbench().getFileStorage().addChangeListener(this._onFileStorageListener);
        }

        private onFileStorageChanged(change: io.FileStorageChange) {

            const editorFile = this.getInput();

            const editorFileFullName = editorFile.getFullName();

            if (change.isDeleted(editorFileFullName)) {

                // close the editor

            } else if (change.isModified(editorFileFullName)) {

                // reload the editor, if the change is not made by the editor itself

            } else if (change.wasRenamed(editorFileFullName)) {

                this.setTitle(editorFile.getName());
            }
        }

        onPartClosed() {

            const closeIt = super.onPartClosed();

            if (closeIt) {
                Workbench.getWorkbench().getFileStorage().removeChangeListener(this._onFileStorageListener);
            }

            return closeIt;
        }

        setInput(file: io.FilePath) {

            super.setInput(file);

            this.setTitle(file.getName());
        }

        getInput(): core.io.FilePath {
            return super.getInput();
        }

        getIcon() {
            const file = this.getInput();

            if (!file) {
                return Workbench.getWorkbench().getWorkbenchIcon(ICON_FILE);
            }

            const wb = Workbench.getWorkbench();
            const ct = wb.getContentTypeRegistry().getCachedContentType(file);
            const icon = wb.getContentTypeIcon(ct);

            return icon;
        }
    }
}