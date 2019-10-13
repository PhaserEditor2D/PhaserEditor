
namespace phasereditor2d.images {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export class ImagesPlugin extends ide.Plugin {

        private static _instance = new ImagesPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.images");
        }

        registerExtensions(registry: colibri.core.extensions.ExtensionRegistry) {

            registry
                .addExtension(
                    files.ui.viewers.ContentTypeCellRendererExtension.POINT,
                    new files.ui.viewers.SimpleContentTypeCellRendererExtension(
                        files.core.CONTENT_TYPE_IMAGE,
                        new ui.viewers.ImageFileCellRenderer())
                );

            registry.addExtension(
                colibri.ui.ide.CSSFileLoaderExtension.POINT_ID,
                new colibri.ui.ide.CSSFileLoaderExtension(
                    "phasereditor2d.images.CSSFileLoaderExtension",
                    [
                        "plugins/phasereditor2d.images/ui/css/ImageEditor.css",
                        "plugins/phasereditor2d.images/ui/css/ImageEditor-dark.css",
                        "plugins/phasereditor2d.images/ui/css/ImageEditor-light.css"
                    ])
            );
        }

        registerEditor(registry: ide.EditorRegistry) {
            registry.registerFactory(ui.editors.ImageEditor.getFactory());
        }

    }

}