namespace phasereditor2d.ide.ui.actions {

    export const CMD_OPEN_PROJECTS_DIALOG = "phasereditor2d.ide.ui.actions.OpenProjectsDialog";
    export const CMD_SWITCH_THEME = "phasereditor2d.ide.ui.actions.SwitchTheme";
    export const CMD_EDITOR_TABS_SIZE_UP = "phasereditor2d.ide.ui.actions.EditorTabsSizeUp";
    export const CMD_EDITOR_TABS_SIZE_DOWN = "phasereditor2d.ide.ui.actions.EditorTabsSizeDown";

    import controls = colibri.ui.controls;
    import commands = colibri.ui.ide.commands;


    function isNotWelcomeWindowScope(args: commands.CommandArgs): boolean {
        return !(args.activeWindow instanceof ui.WelcomeWindow);
    }

    export class IDEActions {

        static registerCommands(manager: commands.CommandManager): void {

            // open project

            manager.addCommandHelper(CMD_OPEN_PROJECTS_DIALOG);

            manager.addHandlerHelper(CMD_OPEN_PROJECTS_DIALOG,
                isNotWelcomeWindowScope,
                args => new OpenProjectsDialogAction().run());

            manager.addKeyBinding(CMD_OPEN_PROJECTS_DIALOG, new commands.KeyMatcher({
                control: true,
                alt: true,
                key: "P",
                filterInputElements: false
            }));

            // theme dialog

            manager.addCommandHelper(CMD_SWITCH_THEME);

            manager.addHandlerHelper(CMD_SWITCH_THEME,
                args => true,
                args => new actions.OpenThemeDialogAction().run()
            );

            manager.addKeyBinding(CMD_SWITCH_THEME, new commands.KeyMatcher({
                control: true,
                key: "2",
                filterInputElements: false
            }));

            // editor tabs size

            manager.addCommandHelper(CMD_EDITOR_TABS_SIZE_DOWN);
            manager.addCommandHelper(CMD_EDITOR_TABS_SIZE_UP);

            manager.addHandlerHelper(CMD_EDITOR_TABS_SIZE_DOWN,
                isNotWelcomeWindowScope,
                args =>
                    colibri.ui.ide.Workbench.getWorkbench().getActiveWindow().getEditorArea().incrementTabSize(-5)
            );

            manager.addHandlerHelper(CMD_EDITOR_TABS_SIZE_UP,
                isNotWelcomeWindowScope,
                args =>
                    colibri.ui.ide.Workbench.getWorkbench().getActiveWindow().getEditorArea().incrementTabSize(5)
            );

            manager.addKeyBinding(CMD_EDITOR_TABS_SIZE_DOWN, new commands.KeyMatcher({
                control: true,
                key: "3"
            }));

            manager.addKeyBinding(CMD_EDITOR_TABS_SIZE_UP, new commands.KeyMatcher({
                control: true,
                key: "4"
            }));
        }
    }
}