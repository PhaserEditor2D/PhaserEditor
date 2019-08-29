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

        private onFilterInput(e : Event) {
            this._viewer.setFilterText(this._filterElement.value);
            this._viewer.repaint();
        }

        getViewer() {
            return this._viewer;
        }

        layout() {
            const b = this.getBounds();
            controls.setElementBounds(this.getElement(), b);

            const inputH = ROW_HEIGHT;

            controls.setElementBounds(this._filterElement, {
                x: 0,
                y: 0,
                width: b.width - 4 /* padding */
            });

            this._filterElement.style.minHeight = inputH + "px";
            this._filterElement.style.maxHeight = inputH + "px";
            this._filterElement.style.height = inputH + "px";

            this._scrollPane.setBounds({
                x : 2,
                y : inputH + 2 + 2 /*padding*/,
                width: b.width - 4,
                height: b.height - inputH - 2 - 2
            });
        }
    }
}