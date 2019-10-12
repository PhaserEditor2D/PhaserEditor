namespace phasereditor2d.ui.ide.editors.scene {
    
    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;

    export class SceneEditorPlugin extends ide.Plugin {

        private static _instance = new SceneEditorPlugin();

        static getInstance(): ide.Plugin {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ui.ide.editors.scene.SceneEditorPlugin");
        }

        registerContentTypes(registry: core.ContentTypeRegistry) {
            registry.registerResolver(new editors.scene.SceneContentTypeResolver());
        }

        registerEditor(registry: ide.EditorRegistry) {
            registry.registerFactory(editors.scene.SceneEditor.getFactory());
        }

        registerCommands(manager: ide.commands.CommandManager) {
            SceneEditorCommands.registerCommands(manager);
        }
    }

}