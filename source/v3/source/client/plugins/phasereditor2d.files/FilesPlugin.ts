namespace phasereditor2d.files {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export const ICON_NEW_FILE = "file-new";

    export class FilesPlugin extends ide.Plugin {

        private static _instance = new FilesPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.files");
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // icons loader 
            
            reg.addExtension(
                colibri.ui.ide.IconLoaderExtension.POINT_ID,
                colibri.ui.ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_NEW_FILE
                ])
            );

            // new files

            reg.addExtension(ui.dialogs.NewFileExtension.POINT,
                new ui.dialogs.NewFolderExtension());

            // commands

            reg.addExtension(ide.commands.CommandExtension.POINT_ID,
                new ide.commands.CommandExtension("phasereditor2d.files.commands", ui.actions.FilesViewCommands.registerCommands));
        }
    }

    ide.Workbench.getWorkbench().addPlugin(FilesPlugin.getInstance());
}