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
                ide.CSSFileLoaderExtension.POINT_ID,
                new ide.CSSFileLoaderExtension(
                    "phasereditor2d.inspector.CSSFileLoaderExtension",
                    [
                        "plugins/phasereditor2d.inspector/ui/css/InspectorView.css"
                    ])
            );

            reg.addExtension(
                ide.CSSFileLoaderExtension.POINT_ID,
                new ide.CSSFileLoaderExtension(
                    "phasereditor2d.images.ui.CSSFileLoaderExtension",
                    [
                        "plugins/phasereditor2d.images/ui/css/ImageEditor.css",
                        "plugins/phasereditor2d.images/ui/css/ImageEditor-dark.css",
                        "plugins/phasereditor2d.images/ui/css/ImageEditor-light.css"
                    ])
            );

            reg.addExtension(
                ide.IconLoaderExtension.POINT_ID,
                ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_INSPECTOR
                ])
            );

        }
    }
}