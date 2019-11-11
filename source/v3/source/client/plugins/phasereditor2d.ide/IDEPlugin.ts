namespace phasereditor2d.ide {

    import ide = colibri.ui.ide;

    export const ICON_NEW_FILE = "file-new";

    export class IDEPlugin extends ide.Plugin {

        private static _instance = new IDEPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ide");
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // icons loader

            reg.addExtension(
                ide.IconLoaderExtension.POINT_ID,
                ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_NEW_FILE
                ])
            );

            // new file

            reg.addExtension(ui.dialogs.NewFileExtension.POINT,
                new ui.dialogs.NewFolderExtension());
        }

        createWindow(windows: ide.WorkbenchWindow[]): void {
            windows.push(new ui.windows.DesignWindow());
        }
    }

    ide.Workbench.getWorkbench().addPlugin(IDEPlugin.getInstance());

    export const VER = "3.0.0";

    function main() {

        console.log(`%c %c Phaser Editor 2D %c v${VER} %c %c https://phasereditor2d.com `,
            "background-color:red",
            "background-color:#3f3f3f;color:whitesmoke",
            "background-color:orange;color:black",
            "background-color:red",
            "background-color:silver",
        );

        ide.Workbench.getWorkbench().launch();
    }


    window.addEventListener("load", main);
}