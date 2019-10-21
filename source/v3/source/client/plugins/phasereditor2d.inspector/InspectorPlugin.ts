namespace phasereditor2d.inspector {

    import ide = colibri.ui.ide;

    export const ICON_INSPECTOR = "inspector";

    export class InspectorPlugin extends ide.Plugin {

        private static _instance = new InspectorPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.inspector");
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            reg.addExtension(
                ide.IconLoaderExtension.POINT_ID,
                ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_INSPECTOR
                ])
            );

        }
    }

    ide.Workbench.getWorkbench().addPlugin(InspectorPlugin.getInstance());
}