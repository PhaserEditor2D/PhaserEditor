namespace phasereditor2d.ui.ide {

    export const CMD_SAVE = "save";
    export const CMD_EDIT_DELETE = "delete";
    export const CMD_EDIT_RENAME = "rename";

    export class IDECommands {

        static init() {
            const manager = Workbench.getWorkbench().getCommandManager();

            // register commands

            manager.addCommandHelper(CMD_SAVE);
            manager.addCommandHelper(CMD_EDIT_DELETE);
            manager.addCommandHelper(CMD_EDIT_RENAME);

            // register handlers

            manager.addHandlerHelper(CMD_SAVE,

                args => args.activeEditor && args.activeEditor.isDirty(),

                args => {
                    args.activeEditor.save();
                }
            );

            // register bindings

            manager.addKeyBinding(CMD_SAVE, new commands.KeyMatcher({
                control: true,
                key: "s"
            }));

            manager.addKeyBinding(CMD_EDIT_DELETE, new commands.KeyMatcher({
                key: "delete"
            }));

            manager.addKeyBinding(CMD_EDIT_DELETE, new commands.KeyMatcher({
                key: "f2"
            }));
        }

    }

}