namespace phasereditor2d.outline {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export const ICON_OUTLINE = "outline";

    export class OutlinePlugin extends ide.Plugin {

        private static _instance = new OutlinePlugin();

        static getInstance() {
            return this._instance;
        }

        constructor() {
            super("phasereditor2d.outline");
        }


        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            reg.addExtension(
                ide.IconLoaderExtension.POINT_ID,
                ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_OUTLINE
                ])
            );

        }

    }

    ide.Workbench.getWorkbench().addPlugin(OutlinePlugin.getInstance());
}