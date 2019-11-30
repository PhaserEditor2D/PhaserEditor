namespace colibri.ui.ide {

    export class PreloadProjectResourcesExtension extends core.extensions.Extension {

        static POINT_ID = "colibri.ui.ide.PreloadProjectResourcesExtension";

        private _getPreloadPromise: (monitor: controls.IProgressMonitor) => Promise<any>;

        constructor(getPreloadPromise: (monitor: controls.IProgressMonitor) => Promise<any>) {
            super();

            this._getPreloadPromise = getPreloadPromise;
        }

        getPreloadPromise(monitor: controls.IProgressMonitor) {
            return this._getPreloadPromise(monitor);
        }
    }

}