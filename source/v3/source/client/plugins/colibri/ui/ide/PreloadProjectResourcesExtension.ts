namespace colibri.ui.ide {

    export class PreloadProjectResourcesExtension extends core.extensions.Extension {

        static POINT_ID = "colibri.ui.ide.PreloadProjectResourcesExtension";

        private _getPreloadPromise: (monitor: controls.IProgressMonitor) => Promise<any>;

        constructor(id: string, getPreloadPromise: (monitor: controls.IProgressMonitor) => Promise<any>) {
            super(id);

            this._getPreloadPromise = getPreloadPromise;
        }

        getPreloadPromise(monitor: controls.IProgressMonitor) {
            return this._getPreloadPromise(monitor);
        }
    }

}