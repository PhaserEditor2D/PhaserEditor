namespace phasereditor2d.ide {

    import ide = colibri.ui.ide;

    export const ICON_PLAY = "play";

    export class IDEPlugin extends ide.Plugin {

        private static _instance = new IDEPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ide");
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // windows

            reg.addExtension(colibri.ui.ide.WindowExtension.POINT_ID,
                new colibri.ui.ide.WindowExtension(
                    "phasereditor2d.ide.ui.DesignWindow",
                    10,
                    () => new ui.DesignWindow()
                )
            );

            // icons

            reg.addExtension(colibri.ui.ide.IconLoaderExtension.POINT_ID,
                new colibri.ui.ide.IconLoaderExtension("phasereditor2d.ide.ui.IconLoader", [
                    this.getIcon(ICON_PLAY)
                ]));
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