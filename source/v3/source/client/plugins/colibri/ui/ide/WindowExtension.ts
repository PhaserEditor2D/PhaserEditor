namespace colibri.ui.ide {

    export declare type CreateWindowFunc = ()=> WorkbenchWindow; 

    export class WindowExtension extends core.extensions.Extension {

        static POINT_ID = "colibri.ui.ide.WindowExtension";

        private _createWindowFunc : CreateWindowFunc;

        constructor(id : string, createWindowFunc : CreateWindowFunc) {
            super(id, 10);

            this._createWindowFunc = createWindowFunc;
        }

        createWindow() {
            return this._createWindowFunc();
        }
    }
}