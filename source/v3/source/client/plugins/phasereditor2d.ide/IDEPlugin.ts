namespace phasereditor2d.ide {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export const ICON_PLAY = "play";

    export class IDEPlugin extends ide.Plugin {

        private static _instance = new IDEPlugin();

        private _openingProject: boolean;

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ide");

            this._openingProject = false;
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // windows

            reg.addExtension(colibri.ui.ide.WindowExtension.POINT_ID,
                new colibri.ui.ide.WindowExtension(
                    "phasereditor2d.ide.ui.DesignWindow",
                    () => new ui.DesignWindow()
                )
            );

            reg.addExtension(colibri.ui.ide.WindowExtension.POINT_ID,
                new colibri.ui.ide.WindowExtension(
                    "phasereditor2d.ide.ui.WelcomeWindow",
                    () => new ui.WelcomeWindow()
                )
            );

            // icons

            reg.addExtension(colibri.ui.ide.IconLoaderExtension.POINT_ID,
                new colibri.ui.ide.IconLoaderExtension("phasereditor2d.ide.ui.IconLoader", [
                    this.getIcon(ICON_PLAY)
                ]));

            // keys

            reg.addExtension(colibri.ui.ide.commands.CommandExtension.POINT_ID,
                new colibri.ui.ide.commands.CommandExtension(
                    "phasereditor2d.welcome.ui.actions.WelcomeActions",
                    ui.actions.IDEActions.registerCommands
                )
            );
        }

        async openFirstWindow() {

            this.restoreTheme();

            const wb = colibri.ui.ide.Workbench.getWorkbench();

            wb.addEventListener(colibri.ui.ide.EVENT_PROJECT_OPENED, e => {

                wb.getGlobalPreferences().setValue("defaultProjectData", {
                    "projectName": wb.getFileStorage().getRoot().getName()
                });
            });

            const prefs = wb.getGlobalPreferences();

            const defaultProjectData = prefs.getValue("defaultProjectData");

            let win: ui.DesignWindow = null;

            if (defaultProjectData) {

                const projectName = defaultProjectData["projectName"];

                let projects = await wb.getFileStorage().getProjects();

                if (projects.indexOf(projectName) >= 0) {

                    await this.ideOpenProject(projectName);

                    return;
                }
            }

            win = wb.activateWindow(ui.WelcomeWindow.ID) as ui.DesignWindow;

            if (win) {

                win.restoreState(wb.getProjectPreferences());
            }
        }

        async ideOpenProject(projectName: string) {

            this._openingProject = true;

            const dlg = new ui.dialogs.OpeningProjectDialog();
            dlg.create();
            dlg.setTitle("Opening " + projectName);
            dlg.setProgress(0);

            const monitor = new controls.dialogs.ProgressDialogMonitor(dlg);

            try {

                const wb = colibri.ui.ide.Workbench.getWorkbench();

                {
                    const win = wb.getActiveWindow();

                    if (win instanceof ui.DesignWindow) {
                        win.saveState(wb.getProjectPreferences());
                    }
                }

                console.log(`IDEPlugin: opening project ${projectName}`);

                await wb.openProject(projectName, monitor);

                dlg.setProgress(1);

                const designWindow = wb.activateWindow(ui.DesignWindow.ID) as ui.DesignWindow;

                if (designWindow) {

                    designWindow.restoreState(wb.getProjectPreferences());
                }
            } finally {

                this._openingProject = false;

                dlg.close();
            }
        }

        isOpeningProject() {
            return this._openingProject;
        }

        switchTheme() {

            const theme = controls.Controls.switchTheme();

            const prefs = colibri.ui.ide.Workbench.getWorkbench().getGlobalPreferences();

            prefs.setValue("phasereditor2d.ide.theme", {
                theme: theme.name
            });
        }

        restoreTheme() {

            const prefs = colibri.ui.ide.Workbench.getWorkbench().getGlobalPreferences();

            const themeData = prefs.getValue("phasereditor2d.ide.theme");

            let theme = controls.Controls.LIGHT_THEME;

            if (themeData) {

                const themeName = themeData.theme;

                if (themeName === "dark") {
                    theme = controls.Controls.DARK_THEME;
                }
            }

            controls.Controls.useTheme(theme);
        }
    }

    ide.Workbench.getWorkbench().addPlugin(IDEPlugin.getInstance());

    export const VER = "3.0.0";

    async function main() {

        console.log(`%c %c Phaser Editor 2D %c v${VER} %c %c https://phasereditor2d.com `,
            "background-color:red",
            "background-color:#3f3f3f;color:whitesmoke",
            "background-color:orange;color:black",
            "background-color:red",
            "background-color:silver",
        );

        const wb = ide.Workbench.getWorkbench();

        await wb.launch();

        await IDEPlugin.getInstance().openFirstWindow();
    }

    window.addEventListener("load", main);
}