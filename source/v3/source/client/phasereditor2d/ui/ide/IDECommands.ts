namespace phasereditor2d.ui.ide {

    export const CMD_SAVE = "save";
    export const HANDLER_SAVE = "save";

    export class IDECommands {

        static init() {
            const manager = Workbench.getWorkbench().getCommandManager();

            // register commands

            manager.addCommand(new commands.Command(CMD_SAVE));

            // register handlers

            manager.addHandler(CMD_SAVE, new commands.CommandHandler({
                id: HANDLER_SAVE,
                testFunc: args => args.activeEditor && args.activeEditor.isDirty(),
                executeFunc: args => {
                    args.activeEditor.save();
                }
            }));

            // register bindings

            manager.addKeyBinding(HANDLER_SAVE, new commands.KeyMatcher({
                control: true,
                key: "s"
            }));

        }

    }

}