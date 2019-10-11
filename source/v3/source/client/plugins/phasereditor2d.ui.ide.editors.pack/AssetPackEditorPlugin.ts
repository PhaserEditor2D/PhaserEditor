namespace phasereditor2d.ui.ide.editors.pack {

    export class AssetPackEditorPlugin extends Plugin {

        private static _instance = new AssetPackEditorPlugin();

        static getInstance(): Plugin {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ui.ide.editors.pack.AssetPackEditorPlugin");
        }

        registerContentTypes(registry: core.ContentTypeRegistry): void {
            registry.registerResolver(new editors.pack.AssetPackContentTypeResolver());
        }

        async preloadProjectResources() {
            await editors.pack.PackFinder.preload();
        }

        registerEditor(registry : EditorRegistry) {
            registry.registerFactory(editors.pack.AssetPackEditor.getFactory());
        }

    }

}