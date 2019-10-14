namespace phasereditor2d.scene {

    import ide = colibri.ui.ide;

    export const ICON_GROUP = "group";

    export class ScenePlugin extends ide.Plugin {

        private static _instance = new ScenePlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.scene");
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // content type resolvers

            reg.addExtension(
                colibri.core.ContentTypeExtension.POINT_ID,
                new colibri.core.ContentTypeExtension("phasereditor2d.scene.core.SceneContentTypeResolver",
                    [new core.SceneContentTypeResolver()],
                    5
                ));

            // content type renderer

            reg.addExtension(
                files.ui.viewers.ContentTypeCellRendererExtension.POINT,
                new files.ui.viewers.SimpleContentTypeCellRendererExtension(
                    core.CONTENT_TYPE_SCENE,
                    new ui.viewers.SceneFileCellRenderer()
                )
            );

            // icons loader

            reg.addExtension(
                ide.IconLoaderExtension.POINT_ID,
                ide.IconLoaderExtension.withPluginFiles(this, [
                    ICON_GROUP
                ])
            );
        }

        registerEditor(registry: ide.EditorRegistry) {
            registry.registerFactory(ui.editor.SceneEditor.getFactory());
        }

        registerCommands(manager: ide.commands.CommandManager) {
            ui.editor.commands.SceneEditorCommands.registerCommands(manager);
        }

    }

}