/// <reference path="../../../phasereditor2d.ui.controls/viewers/FilteredViewer.ts" />

namespace phasereditor2d.ui.ide {
    export abstract class ViewerView extends ViewPart {
        protected _filteredViewer: controls.viewers.FilteredViewer<any>;
        protected _viewer : controls.viewers.Viewer;

        constructor(id : string, viewer : controls.viewers.Viewer) {
            super(id)
            
            this._viewer = viewer;

            this.addClass("ViewerView");

            this._filteredViewer = new controls.viewers.FilteredViewer(viewer);
            this.add(this._filteredViewer);

            viewer.addEventListener(controls.SELECTION_EVENT, (e: CustomEvent) => {
                this.setSelection(e.detail);
            });
        }

        layout() {
            this._filteredViewer.layout();
        }
    }
}