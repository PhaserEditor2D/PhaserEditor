namespace phasereditor2d.ui.ide {

    import viewers = controls.viewers;

    export abstract class EditorViewerProvider {

        private _viewer: controls.viewers.TreeViewer;
        private _initialSelection : any[];
        private _initialListeners: any[];

        constructor() {
            this._viewer = null;
            this._initialSelection = null;
            this._initialListeners = [];
        }

        setViewer(viewer: controls.viewers.TreeViewer) {
            this._viewer = viewer;

            if (this._initialSelection) {
                this._viewer.setSelection(this._initialSelection, false);
            }
        }

        setSelection(selection: any[], notify: boolean) {
            if (this._viewer) {
                this._viewer.setSelection(selection, notify);
            } else {
                this._initialSelection = selection;
            }
        }

        onViewerSelectionChanged(selection : any[]) {

        }

        repaint() {
            if (this._viewer) {
                this._viewer.repaint();
            }
        }

        abstract getContentProvider(): viewers.ITreeContentProvider;

        abstract getLabelProvider(): viewers.ILabelProvider;

        abstract getCellRendererProvider(): viewers.ICellRendererProvider;

        abstract getTreeViewerRenderer(viewer: controls.viewers.TreeViewer): viewers.TreeViewerRenderer;

        abstract getPropertySectionProvider(): controls.properties.PropertySectionProvider;

        abstract getInput(): any;

        abstract preload(): Promise<void>;
    }
}