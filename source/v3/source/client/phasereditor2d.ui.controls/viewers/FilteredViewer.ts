namespace phasereditor2d.ui.controls.viewers {

    export class FilteredViewer<T extends Viewer> extends Control {

        private _viewer: T;
        private _filterElement: HTMLInputElement;
        private _scrollPane: ScrollPane;

        constructor(viewer: T) {
            super();
            this._viewer = viewer;
            this.addClass("filteredViewer");

            this._filterElement = document.createElement("input");
            this.getElement().appendChild(this._filterElement);

            this._scrollPane = new ScrollPane(this._viewer);

            this.add(this._scrollPane);

            this._filterElement.addEventListener("input", e => this.onFilterInput(e));
        }

        private onFilterInput(e: Event) {
            this._viewer.setFilterText(this._filterElement.value);
        }

        getViewer() {
            return this._viewer;
        }

        layout() {
            const b = this.getBounds();
            controls.setElementBounds(this.getElement(), b);

            const inputH = ROW_HEIGHT;

            controls.setElementBounds(this._filterElement, {
                x: CONTROL_PADDING,
                y: CONTROL_PADDING,
                width: b.width - CONTROL_PADDING * 2 - /* padding=4 */ 4
            });

            this._filterElement.style.minHeight = inputH + "px";
            this._filterElement.style.maxHeight = inputH + "px";
            this._filterElement.style.height = inputH + "px";


            const paneY = inputH + /*padding=4*/ 4 + CONTROL_PADDING * 2;
            this._scrollPane.setBounds({
                x: 0,
                y: paneY,
                width: b.width + 2,
                height: b.height - paneY
            });
        }
    }
}