namespace phasereditor2d.ide {

    import ide = colibri.ui.ide;

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

                await this.ideOpenProject(projectName);

            } else {

                win = wb.activateWindow(ui.WelcomeWindow.ID) as ui.DesignWindow;

                if (win) {

                    win.restoreState(wb.getProjectPreferences());
                }
            }
        }

        async ideOpenProject(projectName: string) {

            this._openingProject = true;

            try {

                const wb = colibri.ui.ide.Workbench.getWorkbench();

                {
                    const win = wb.getActiveWindow();

                    if (win instanceof ui.DesignWindow) {
                        win.saveState(wb.getProjectPreferences());
                    }
                }
                
                console.log(`IDEPlugin: opening project ${projectName}`);

                await wb.openProject(projectName);

                const designWindow = wb.activateWindow(ui.DesignWindow.ID) as ui.DesignWindow;

                if (designWindow) {

                    designWindow.restoreState(wb.getProjectPreferences());
                }
            } finally {

                this._openingProject = false;
            }
        }

        isOpeningProject() {
            return this._openingProject;
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