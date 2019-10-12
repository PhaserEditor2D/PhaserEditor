namespace phasereditor2d.ui.ide.editors.scene {

    const CMD_JOIN_IN_CONTAINER = "joinObjectsInContainer";

    import controls = colibri.ui.controls;
    import core = colibri.core;

    function isSceneScope(args: colibri.ui.ide.commands.CommandArgs) {
        return args.activePart instanceof SceneEditor ||
            args.activePart instanceof ide.views.outline.OutlineView && args.activeEditor instanceof SceneEditor
    }

    export class SceneEditorCommands {

        static registerCommands(manager : colibri.ui.ide.commands.CommandManager) {

            // delete 

            manager.addHandlerHelper(colibri.ui.ide.CMD_DELETE,

                args => isSceneScope(args),

                args => {
                    const editor = <SceneEditor>args.activeEditor;
                    editor.getActionManager().deleteObjects();
                });

            // join in container

            manager.addCommandHelper(CMD_JOIN_IN_CONTAINER);

            manager.addHandlerHelper(CMD_JOIN_IN_CONTAINER,

                args => isSceneScope(args),

                args => {
                    const editor = <SceneEditor>args.activeEditor;
                    editor.getActionManager().joinObjectsInContainer();
                });

            manager.addKeyBinding(CMD_JOIN_IN_CONTAINER, new colibri.ui.ide.commands.KeyMatcher({
                key: "j"
            }));
        }

    }

}