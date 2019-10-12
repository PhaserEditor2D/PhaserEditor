namespace phasereditor2d.ui.ide {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;

    export class DesignPlugin extends ide.Plugin {

        private static _instance = new DesignPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ui.ide.DesignPlugin");
        }

        createWindow(windows: ide.WorkbenchWindow[]): void {
            windows.push(new DesignWindow());
        }

    }
}