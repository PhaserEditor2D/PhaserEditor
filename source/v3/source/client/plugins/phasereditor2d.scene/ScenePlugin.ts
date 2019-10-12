namespace phasereditor2d.scene {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export class ScenePlugin extends ide.Plugin {

        private static _instance = new ScenePlugin();

        static getInstance(): ide.Plugin {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.scene.ScenePlugin");
        }

        registerContentTypes(registry: colibri.core.ContentTypeRegistry) {
            registry.registerResolver(new core.SceneContentTypeResolver());
        }

        registerEditor(registry: ide.EditorRegistry) {
            registry.registerFactory(ui.editor.SceneEditor.getFactory());
        }

        registerCommands(manager: ide.commands.CommandManager) {
            ui.editor.commands.SceneEditorCommands.registerCommands(manager);
        }
    }

}