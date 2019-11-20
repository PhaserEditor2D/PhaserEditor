namespace phasereditor2d.ide.ui.actions {

    export const CMD_OPEN_PROJECTS_DIALOG = "phasereditor2d.ide.ui.actions.OpenProjectsDialog";
    export const CMD_SWITCH_THEME = "phasereditor2d.ide.ui.actions.SwitchTheme";

    import controls = colibri.ui.controls;

    export class IDEActions {

        static registerCommands(manager: colibri.ui.ide.commands.CommandManager): void {

            // open project

            manager.addCommandHelper(CMD_OPEN_PROJECTS_DIALOG);

            manager.addHandlerHelper(CMD_OPEN_PROJECTS_DIALOG,
                args => {

                    return args.activeWindow.getId() !== ui.WelcomeWindow.ID;

                }, args => new OpenProjectsDialogAction().run());

            manager.addKeyBinding(CMD_OPEN_PROJECTS_DIALOG, new colibri.ui.ide.commands.KeyMatcher({
                control: true,
                alt: true,
                key: "P",
                filterInputElements: false
            }));

            // switch theme

            manager.addCommandHelper(CMD_SWITCH_THEME);

            manager.addHandlerHelper(CMD_SWITCH_THEME,
                args => true,
                args => IDEPlugin.getInstance().switchTheme()
            );

            manager.addKeyBinding(CMD_SWITCH_THEME, new colibri.ui.ide.commands.KeyMatcher({
                control: true,
                key: "2",
                filterInputElements: false
            }));
        }
    }
}