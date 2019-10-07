namespace phasereditor2d.ui.ide.editors.scene {


    function isSceneScope(args: ide.commands.CommandArgs) {
        return args.activePart instanceof SceneEditor ||
            args.activePart instanceof ide.views.outline.OutlineView && args.activeEditor instanceof SceneEditor
    }

    export class SceneEditorCommands {

        static init() {
 
            const manager = Workbench.getWorkbench().getCommandManager();

            manager.addHandlerHelper(ide.CMD_DELETE,

                args => isSceneScope(args),

                args => {
                    const editor = <SceneEditor>args.activeEditor;
                    editor.getActionManager().deleteObjects();
                });

        }

    }

}