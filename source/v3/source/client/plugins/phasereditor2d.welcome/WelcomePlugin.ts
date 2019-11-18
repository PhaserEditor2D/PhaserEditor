namespace phasereditor2d.welcome {

    export class WelcomePlugin extends colibri.ui.ide.Plugin {

        private static _instance: WelcomePlugin;

        static getInstance() {

            if (!this._instance) {
                this._instance = new WelcomePlugin();
            }

            return this._instance;
        }

        constructor() {
            super("phasereditor2d.welcome");
        }

        createWindow(windows: colibri.ui.ide.WorkbenchWindow[]) {
            windows.push(new ui.WelcomeWindow());
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // windows

            reg.addExtension(colibri.ui.ide.WindowExtension.POINT_ID,
                new colibri.ui.ide.WindowExtension(
                    "phasereditor2d.ide.ui.WelcomeWindow",
                    5,
                    () => new ui.WelcomeWindow()
                )
            );

            // keys

            reg.addExtension(colibri.ui.ide.commands.CommandExtension.POINT_ID,
                new colibri.ui.ide.commands.CommandExtension(
                    "phasereditor2d.welcome.ui.actions.WelcomeActions",
                    ui.actions.WelcomeActions.registerCommands
                )
            );
        }
    }

    colibri.ui.ide.Workbench.getWorkbench().addPlugin(WelcomePlugin.getInstance());
}