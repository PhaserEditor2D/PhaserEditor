
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

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // file cell renderers

            reg
                .addExtension(
                    files.ui.viewers.ContentTypeCellRendererExtension.POINT,
                    new files.ui.viewers.SimpleContentTypeCellRendererExtension(
                        files.core.CONTENT_TYPE_IMAGE,
                        new ui.viewers.ImageFileCellRenderer())
                );

            // css loader

            reg.addExtension(
                ide.CSSFileLoaderExtension.POINT_ID,
                new ide.CSSFileLoaderExtension(
                    "phasereditor2d.images.CSSFileLoaderExtension",
                    [
                        "plugins/phasereditor2d.images/ui/css/ImageEditor.css",
                        "plugins/phasereditor2d.images/ui/css/ImageEditor-dark.css",
                        "plugins/phasereditor2d.images/ui/css/ImageEditor-light.css"
                    ])
            );


            // editors

            reg.addExtension(ide.EditorExtension.POINT_ID, new ide.EditorExtension("phasereditor2d.images.EditorExtension", [
                ui.editors.ImageEditor.getFactory()
            ]));
        }

    }

}