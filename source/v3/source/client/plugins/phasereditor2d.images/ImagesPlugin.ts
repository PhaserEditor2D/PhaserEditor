namespace phasereditor2d.images {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export class ImagesPlugin extends ide.Plugin {

        private static _instance = new ImagesPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.images.ImagesPlugin");
        }

        registerEditor(registry: ide.EditorRegistry) {
            registry.registerFactory(ui.editors.ImageEditor.getFactory());
        }

    }
}