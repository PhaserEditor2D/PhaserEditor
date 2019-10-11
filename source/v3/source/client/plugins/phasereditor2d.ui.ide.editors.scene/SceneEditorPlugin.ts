namespace phasereditor2d.ui.ide.editors.scene {

    export class SceneEditorPlugin extends Plugin {

        private static _instance = new SceneEditorPlugin();

        static getInstance(): Plugin {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ui.ide.editors.scene.SceneEditorPlugin");
        }

        registerContentTypes(registry: core.ContentTypeRegistry) {
            registry.registerResolver(new editors.scene.SceneContentTypeResolver());
        }

        registerEditor(registry: EditorRegistry) {
            registry.registerFactory(editors.scene.SceneEditor.getFactory());
        }

        registerCommands(manager: commands.CommandManager) {
            SceneEditorCommands.register(manager);
        }
    }

}