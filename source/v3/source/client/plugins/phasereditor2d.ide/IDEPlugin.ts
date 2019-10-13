namespace phasereditor2d.ide {

    import ide = colibri.ui.ide;

    export class IDEPlugin extends ide.Plugin {

        private static _instance = new IDEPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ide");
        }

        createWindow(windows: ide.WorkbenchWindow[]): void {
            windows.push(new ui.windows.DesignWindow());
        }

    }
}