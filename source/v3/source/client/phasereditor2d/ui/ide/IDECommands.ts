namespace phasereditor2d.ui.ide {

    export const CMD_SAVE = "save";
    export const CMD_DELETE = "delete";
    export const CMD_RENAME = "rename";
    export const CMD_UNDO = "undo";
    export const CMD_REDO = "redo";


    export class IDECommands {

        static init() {
            const manager = Workbench.getWorkbench().getCommandManager();

            // register commands

            manager.addCommandHelper(CMD_SAVE);
            manager.addCommandHelper(CMD_DELETE);
            manager.addCommandHelper(CMD_RENAME);
            manager.addCommandHelper(CMD_UNDO);
            manager.addCommandHelper(CMD_REDO);

            // register handlers

            manager.addHandlerHelper(CMD_SAVE,

                args => args.activeEditor && args.activeEditor.isDirty(),

                args => {
                    args.activeEditor.save();
                }
            );

            manager.addHandlerHelper(CMD_UNDO,

                args => args.activePart !== null,

                args => {
                    args.activePart.getUndoManager().undo();
                }
            );

            manager.addHandlerHelper(CMD_REDO,

                args => args.activePart !== null,

                args => {
                    args.activePart.getUndoManager().redo();
                }
            );

            // register bindings

            manager.addKeyBinding(CMD_SAVE, new commands.KeyMatcher({
                control: true,
                key: "s"
            }));

            manager.addKeyBinding(CMD_DELETE, new commands.KeyMatcher({
                key: "delete"
            }));

            manager.addKeyBinding(CMD_RENAME, new commands.KeyMatcher({
                key: "f2"
            }));

            manager.addKeyBinding(CMD_UNDO, new commands.KeyMatcher({
                control: true,
                key: "z"
            }));

            manager.addKeyBinding(CMD_REDO, new commands.KeyMatcher({
                control: true,
                shift: true,
                key: "z"
            }));
        }

    }

}