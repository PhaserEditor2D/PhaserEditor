namespace phasereditor2d.ui.ide {

    export const CMD_SAVE = "save";
    export const CMD_DELETE = "delete";
    export const CMD_RENAME = "rename";
    export const CMD_UNDO = "undo";
    export const CMD_REDO = "redo";
    export const CMD_SWITCH_THEME = "switchTheme";


    export class IDECommands {

        static init() {
            const manager = Workbench.getWorkbench().getCommandManager();

            // save

            manager.addCommandHelper(CMD_SAVE);
            manager.addHandlerHelper(CMD_SAVE,

                args => args.activeEditor && args.activeEditor.isDirty(),

                args => {
                    args.activeEditor.save();
                }
            );
            manager.addKeyBinding(CMD_SAVE, new commands.KeyMatcher({
                control: true,
                key: "s"
            }));

            // delete

            manager.addCommandHelper(CMD_DELETE);
            manager.addKeyBinding(CMD_DELETE, new commands.KeyMatcher({
                key: "delete"
            }));

            // rename

            manager.addCommandHelper(CMD_RENAME);
            manager.addKeyBinding(CMD_RENAME, new commands.KeyMatcher({
                key: "f2"
            }));

            // undo

            manager.addCommandHelper(CMD_UNDO);
            manager.addHandlerHelper(CMD_UNDO,
                args => args.activePart !== null,
                args => args.activePart.getUndoManager().undo()
            );
            manager.addKeyBinding(CMD_UNDO, new commands.KeyMatcher({
                control: true,
                key: "z"
            }));


            // redo

            manager.addCommandHelper(CMD_REDO);
            manager.addHandlerHelper(CMD_REDO,
                args => args.activePart !== null,
                args => args.activePart.getUndoManager().redo()

            );
            manager.addKeyBinding(CMD_REDO, new commands.KeyMatcher({
                control: true,
                shift: true,
                key: "z"
            }));

            // switch theme

            manager.addCommandHelper(CMD_SWITCH_THEME);
            manager.addHandlerHelper(CMD_SWITCH_THEME,
                args => true,
                args => controls.Controls.switchTheme()
            );
            manager.addKeyBinding(CMD_SWITCH_THEME, new commands.KeyMatcher({
                control: true,
                key: "2"
            }));
        }

    }

}