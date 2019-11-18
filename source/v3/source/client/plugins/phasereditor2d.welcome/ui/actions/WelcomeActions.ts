namespace phasereditor2d.welcome.ui.actions {

    export const CMD_OPEN_PROJECTS_DIALOG = "phasereditor2d.welcome.ui.actions.OpenProjectsDialog"

    export class WelcomeActions {

        static registerCommands(manager: colibri.ui.ide.commands.CommandManager) : void {

            manager.addCommandHelper(CMD_OPEN_PROJECTS_DIALOG);

            manager.addHandlerHelper(CMD_OPEN_PROJECTS_DIALOG,
                args => {
                    console.log(args.activeWindow);
                    return args.activeWindow instanceof ide.ui.DesignWindow;
                }, args => WelcomeActions.openProjectsDialog());

            manager.addKeyBinding(CMD_OPEN_PROJECTS_DIALOG, new colibri.ui.ide.commands.KeyMatcher({
                control: true,
                alt: true,
                key: "P",
                filterInputElements: false
            }));
        }

        private static openProjectsDialog() {

            const dlg = new dialogs.ProjectsDialog();

            dlg.create();
        }
    }
}