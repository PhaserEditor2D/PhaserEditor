namespace phasereditor2d.ide.ui.dialogs {

    export class NewFolderDialog extends BaseNewFileDialog {

        protected createFile(container: colibri.core.io.FilePath, name: string): Promise<colibri.core.io.FilePath> {
            return colibri.ui.ide.FileUtils.createFolder_async(container, name);
        }
    }
}