
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
                        webContentTypes.core.CONTENT_TYPE_IMAGE,
                        new ui.viewers.ImageFileCellRenderer())
                );

            reg
                .addExtension(
                    files.ui.viewers.ContentTypeCellRendererExtension.POINT,
                    new files.ui.viewers.SimpleContentTypeCellRendererExtension(
                        webContentTypes.core.CONTENT_TYPE_SVG,
                        new ui.viewers.ImageFileCellRenderer())
                );

            // editors

            reg.addExtension(ide.EditorExtension.POINT_ID, new ide.EditorExtension("phasereditor2d.images.EditorExtension", [
                ui.editors.ImageEditor.getFactory()
            ]));
        }

    }

    ide.Workbench.getWorkbench().addPlugin(ImagesPlugin.getInstance());
}