namespace phasereditor2d.ui.ide {

    export class DesignPlugin extends Plugin {

        private static _instance = new DesignPlugin();

        static getInstance(): Plugin {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ui.ide.DesignPlugin");
        }

        createWindow(windows: ui.ide.WorkbenchWindow[]): void {
            windows.push(new DesignWindow());
        }

    }
}