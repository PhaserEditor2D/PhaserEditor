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

            // commands

            reg.addExtension(ide.commands.CommandExtension.POINT_ID,
                new ide.commands.CommandExtension("phasereditor2d.scene.commands",
                    ui.editor.commands.SceneEditorCommands.registerCommands));

            // editors

            reg.addExtension(ide.EditorExtension.POINT_ID,
                new ide.EditorExtension("phasereditor2d.scene.EditorExtension", [
                    ui.editor.SceneEditor.getFactory()
                ]));

            // new file wizards

            reg.addExtension(phasereditor2d.ide.ui.dialogs.NewFileExtension.POINT,
                new ui.dialogs.NewSceneFileDialogExtension());
        }

    }

    ide.Workbench.getWorkbench().addPlugin(ScenePlugin.getInstance());
}