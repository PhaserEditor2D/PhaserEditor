namespace colibri.ui.ide {

    export class PreloadProjectResourcesExtension extends core.extensions.Extension {

        static POINT_ID = "colibri.ui.ide.PreloadProjectResourcesExtension";

        private _getPreloadPromise: () => Promise<any>;

        constructor(id: string, getPreloadPromise: () => Promise<any>) {
            super(id);

            this._getPreloadPromise = getPreloadPromise;
        }

        getPreloadPromise() {
            return this._getPreloadPromise();
        }
    }

}