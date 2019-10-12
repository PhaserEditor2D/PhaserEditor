namespace phasereditor2d.scene {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export const ICON_GROUP = "group";

    export class ScenePlugin extends ide.Plugin {

        private static _instance = new ScenePlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.scene.ScenePlugin");
        }

        registerContentTypes(registry: colibri.core.ContentTypeRegistry) {
            registry.registerResolver(new core.SceneContentTypeResolver());
        }

        async preloadIcons() {
            await this.getIcon(ICON_GROUP).preload();
        }

        getIcon(name: string) {
            return controls.Controls.getIcon(name, "plugins/phasereditor2d.scene/ui/icons");
        }

        registerEditor(registry: ide.EditorRegistry) {
            registry.registerFactory(ui.editor.SceneEditor.getFactory());
        }

        registerCommands(manager: ide.commands.CommandManager) {
            ui.editor.commands.SceneEditorCommands.registerCommands(manager);
        }

    }

}