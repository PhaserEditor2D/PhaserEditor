namespace phasereditor2d.ui.ide.design {

    export class DesignPlugin extends Plugin {

        private static _instance = new DesignPlugin();

        static getInstance(): Plugin {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.ui.ide.design.DesignPlugin");
        }

        createWindow(windows: ide.WorkbenchWindow[]): void {
            windows.push(new DesignWindow());
        }

    }
}