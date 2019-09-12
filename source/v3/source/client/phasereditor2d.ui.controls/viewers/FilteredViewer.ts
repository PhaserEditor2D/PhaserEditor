namespace phasereditor2d.ui.controls.viewers {


    class FilterControl extends Control {
        private _filterElement: HTMLInputElement;

        constructor() {
            super("div", "FilterControl");
            this.setLayoutChildren(false);

            this._filterElement = document.createElement("input");
            this.getElement().appendChild(this._filterElement);
        }

        getFilterElement() {
            return this._filterElement;
        }

    }

    export class FilteredViewer<T extends Viewer> extends Control {

        private _viewer: T;
        private _filterControl: FilterControl;
        private _scrollPane: ScrollPane;

        constructor(viewer: T, ...classList: string[]) {
            super("div", "FilteredViewer", ...classList);
            this._viewer = viewer;

            this._filterControl = new FilterControl();
            this._filterControl.getFilterElement().addEventListener("input", e => this.onFilterInput(e));
            this.add(this._filterControl);

            this._scrollPane = new ScrollPane(this._viewer);
            this.add(this._scrollPane);

            this.setLayoutChildren(false);
        }

        private onFilterInput(e: Event) {
            try {
                this._viewer.setFilterText(this._filterControl.getFilterElement().value);
            } catch (e) {
                console.log(e);
            }
        }

        getViewer() {
            return this._viewer;
        }

        layout() {

            super.layout();

            const b = this.getBounds();

            this._filterControl.setBoundsValues(0, 0, b.width, FILTERED_VIEWER_FILTER_HEIGHT);


            this._scrollPane.setBounds({
                x: 0,
                y: FILTERED_VIEWER_FILTER_HEIGHT,
                width: b.width,
                height: b.height - FILTERED_VIEWER_FILTER_HEIGHT
            });
        }
    }
}