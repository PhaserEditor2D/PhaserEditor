/// <reference path="../../../phasereditor2d.ui.controls/viewers/FilteredViewer.ts" />

namespace phasereditor2d.ui.ide {
    export abstract class ViewerView extends ViewPart {
        protected _filteredViewer: controls.viewers.FilteredViewer<any>;
        protected _viewer : controls.viewers.Viewer;

        constructor(id : string) {
            super(id)
        }

        protected abstract  createViewer() : controls.viewers.Viewer;

        protected createPart() : void {
            super.createPart();

            this._viewer = this.createViewer();

            this.addClass("ViewerView");

            this._filteredViewer = new controls.viewers.FilteredViewer(this._viewer);
            this.add(this._filteredViewer);

            this._viewer.addEventListener(controls.EVENT_SELECTION, (e: CustomEvent) => {
                this.setSelection(e.detail);
            });
        }

        layout() {
            if (this._filteredViewer) {
                this._filteredViewer.layout();
            }
        }
    }
}