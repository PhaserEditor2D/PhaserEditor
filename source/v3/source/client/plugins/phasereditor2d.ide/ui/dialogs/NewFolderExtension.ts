namespace phasereditor2d.ide.ui.dialogs {

    export class NewFolderExtension extends NewFileExtension {

        constructor() {
            super({
                id: "phasereditor2d.ide.ui.dialogs.NewFolderExtension",
                icon: colibri.ui.ide.Workbench.getWorkbench().getWorkbenchIcon(colibri.ui.ide.ICON_FOLDER),
                initialFileName: "folder",
                wizardName: "Folder"
            });
        }

        createDialog(): BaseNewFileDialog {
            
            const dlg = new NewFolderDialog();
            
            dlg.create();

            return dlg;
        }
    }
}