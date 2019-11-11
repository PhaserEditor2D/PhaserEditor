namespace phasereditor2d.ide.ui.dialogs {

    export class NewFolderDialog extends BaseNewFileDialog {

        protected createFile(folder: colibri.core.io.FilePath, name: string): Promise<colibri.core.io.FilePath> {
            
            //TODO: create folder

            return null;
        }
    }
}