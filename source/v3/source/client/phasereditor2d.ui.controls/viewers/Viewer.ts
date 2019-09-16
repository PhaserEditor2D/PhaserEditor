/// <reference path="../Rect.ts"/>
/// <reference path="../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="./LabelCellRenderer.ts"/>
/// <reference path="./ImageCellRenderer.ts"/>

namespace phasereditor2d.ui.controls.viewers {

    export abstract class Viewer extends Control {

        private _contentProvider: IContentProvider;
        private _cellRendererProvider: ICellRendererProvider;
        private _labelProvider: ILabelProvider = null;
        private _input: any;
        private _cellSize: number;
        protected _expandedObjects: Set<any>;
        private _selectedObjects: Set<any>;
        protected _context: CanvasRenderingContext2D;
        protected _paintItems: PaintItem[];
        private _overObject: any;
        private _lastSelectedItemIndex: number = -1;
        protected _contentHeight: number = 0;
        private _filterText: string;
        protected _filterIncludeSet: Set<any>;


        constructor(...classList: string[]) {
            super("canvas", "Viewer");

            this.getElement().tabIndex = 1;

            this._filterText = "";
            this._cellSize = 48;

            this.initContext();

            this._input = null;
            this._expandedObjects = new Set();
            this._selectedObjects = new Set();

            (<any>window).cc = this;

            this.initListeners();
        }

        private initListeners() {
            const canvas = this.getCanvas();
            canvas.addEventListener("mousemove", e => this.onMouseMove(e));
            canvas.addEventListener("mousedown", e => this.onMouseDown(e));
            canvas.addEventListener("wheel", e => this.onWheel(e))
            canvas.addEventListener("keydown", e => this.onKeyDown(e));
        }

        getLabelProvider() {
            return this._labelProvider;
        }

        setLabelProvider(labelProvider: ILabelProvider) {
            this._labelProvider = labelProvider;
        }

        setFilterText(filterText: string) {
            this._filterText = filterText.toLowerCase();
            this.repaint();
        }

        getFilterText() {
            return this._filterText;
        }

        private prepareFiltering() {
            this._filterIncludeSet = new Set();
            this.buildFilterIncludeMap();
        }

        isFilterIncluded(obj: any) {
            return this._filterIncludeSet.has(obj);
        }

        protected abstract buildFilterIncludeMap();

        protected matches(obj: any): boolean {
            const labelProvider = this.getLabelProvider();
            const filter = this.getFilterText();

            if (labelProvider === null) {
                return true;
            }

            if (filter === "") {
                return true;
            }

            const label = labelProvider.getLabel(obj);
            if (label.toLocaleLowerCase().indexOf(filter) !== -1) {
                return true;
            }

            return false;
        }

        protected getPaintItemAt(e: MouseEvent): PaintItem {
            for (let item of this._paintItems) {
                if (item.contains(e.offsetX, e.offsetY)) {
                    return item;
                }
            }
            return null;
        }

        getSelection() {
            const sel = [];
            for (const obj of this._selectedObjects) {
                sel.push(obj);
            }
            return sel;
        }

        private fireSelectionChanged() {
            this.dispatchEvent(new CustomEvent(EVENT_SELECTION, {
                detail: this.getSelection()
            }));
        }

        private onKeyDown(e: KeyboardEvent): void {
            if (e.key === "Escape") {
                if (this._selectedObjects.size > 0) {
                    this._selectedObjects.clear();
                    this.repaint();
                    this.fireSelectionChanged();
                }
            }
        }

        private onWheel(e: WheelEvent): void {
            if (!e.shiftKey) {
                return;
            }

            if (e.deltaY < 0) {
                this.setCellSize(this.getCellSize() + ROW_HEIGHT);
            } else if (this._cellSize > 16) {
                this.setCellSize(this.getCellSize() - ROW_HEIGHT);
            }

            this.repaint();
        }

        private onMouseDown(e: MouseEvent): void {
            if (e.button !== 0) {
                return;
            }

            const item = this.getPaintItemAt(e);

            let selChanged = false;

            if (item === null) {
                this._selectedObjects.clear();
                selChanged = true;
            } else {

                const data = item.data;

                if (e.ctrlKey || e.metaKey) {
                    if (this._selectedObjects.has(data)) {
                        this._selectedObjects.delete(data);
                    } else {
                        this._selectedObjects.add(data);
                    }
                    selChanged = true;
                } else if (e.shiftKey) {
                    if (this._lastSelectedItemIndex >= 0 && this._lastSelectedItemIndex != item.index) {
                        const start = Math.min(this._lastSelectedItemIndex, item.index);
                        const end = Math.max(this._lastSelectedItemIndex, item.index);
                        for (let i = start; i <= end; i++) {
                            const obj = this._paintItems[i].data;
                            this._selectedObjects.add(obj);
                        }
                        selChanged = true;
                    }
                } else {
                    this._selectedObjects.clear();
                    this._selectedObjects.add(data);
                    selChanged = true;
                }
            }

            if (selChanged) {
                this.repaint();
                this.fireSelectionChanged();
                this._lastSelectedItemIndex = item ? item.index : 0;
            }
        }

        private onMouseMove(e: MouseEvent): void {
            if (e.buttons !== 0) {
                return;
            }

            const item = this.getPaintItemAt(e);

            const over = item === null ? null : item.data;

            if (over !== this._overObject) {
                this._overObject = over;
            }
        }

        getOverObject() {
            return this._overObject;
        }

        private initContext(): void {
            this._context = this.getCanvas().getContext("2d");
            this._context.imageSmoothingEnabled = false;
            this._context.font = `${controls.FONT_HEIGHT}px sans-serif`;
        }

        setExpanded(obj: any, expanded: boolean): void {
            if (expanded) {
                this._expandedObjects.add(obj);
            } else {
                this._expandedObjects.delete(obj);
            }
        }

        isExpanded(obj: any) {
            return this._expandedObjects.has(obj);
        }

        isCollapsed(obj: any) {
            return !this.isExpanded(obj);
        }

        isSelected(obj: any) {
            return this._selectedObjects.has(obj);
        }

        protected paintTreeHandler(x: number, y: number, collapsed: boolean): void {
            if (collapsed) {
                this._context.strokeStyle = "#000";
                this._context.strokeRect(x, y, 16, 16);
            } else {
                this._context.fillStyle = "#000";
                this._context.fillRect(x, y, 16, 16);
            }
        }

        async repaint() {
            this.prepareFiltering();

            this.repaint2();

            const result = await this.preload();

            if (result === PreloadResult.RESOURCES_LOADED) {
                this.repaint2();
            }

            this.updateScrollPane();
        }

        private updateScrollPane() {
            const pane = this.getContainer().getContainer();
            if (pane instanceof ScrollPane) {
                pane.updateScroll(this._contentHeight);
            }
        }

        private repaint2(): void {
            this._paintItems = [];

            const canvas = this.getCanvas();

            this._context.clearRect(0, 0, canvas.width, canvas.height);

            if (this._cellRendererProvider && this._contentProvider && this._input !== null) {
                this.paint();
            }
        }

        protected abstract preload(): Promise<PreloadResult>;

        paintItemBackground(obj: any, x: number, y: number, w: number, h: number, radius: number = 0): void {
            let fillStyle = null;

            if (this.isSelected(obj)) {
                fillStyle = Controls.theme.treeItemSelectionBackground;
            }

            if (fillStyle != null) {
                this._context.save();

                this._context.fillStyle = fillStyle;
                this._context.strokeStyle = fillStyle;

                if (radius > 0) {
                    this._context.lineJoin = "round";
                    this._context.lineWidth = radius;
                    this._context.strokeRect(x + (radius / 2), y + (radius / 2), w - radius, h - radius);
                    this._context.fillRect(x + (radius / 2), y + (radius / 2), w - radius, h - radius);
                } else {
                    this._context.fillRect(x, y, w, h);
                }

                this._context.restore();
            }
        }

        setScrollY(scrollY: number) {
            const b = this.getBounds();

            scrollY = Math.max(-this._contentHeight + b.height, scrollY);
            scrollY = Math.min(0, scrollY);

            super.setScrollY(scrollY);

            this.repaint();
        }

        layout(): void {
            const b = this.getBounds();

            if (this.isHandlePosition()) {
                ui.controls.setElementBounds(this.getElement(), {
                    x: b.x,
                    y: b.y,
                    width: b.width | 0,
                    height: b.height | 0
                });
            } else {
                ui.controls.setElementBounds(this.getElement(), {
                    width: b.width | 0,
                    height: b.height | 0
                });
            }

            const canvas = this.getCanvas();

            canvas.width = b.width | 0;
            canvas.height = b.height | 0;

            this.initContext();

            this.repaint();
        }

        protected abstract paint(): void;

        getCanvas(): HTMLCanvasElement {
            return <HTMLCanvasElement>this.getElement();
        }

        getContext() {
            return this._context;
        }

        getCellSize() {
            return this._cellSize;
        }

        setCellSize(cellSize: number): void {
            this._cellSize = Math.max(ROW_HEIGHT, cellSize);
        }

        getContentProvider() {
            return this._contentProvider;
        }

        setContentProvider(contentProvider: IContentProvider): void {
            this._contentProvider = contentProvider;
        }

        getCellRendererProvider() {
            return this._cellRendererProvider;
        }

        setCellRendererProvider(cellRendererProvider: ICellRendererProvider): void {
            this._cellRendererProvider = cellRendererProvider;
        }

        getInput() {
            return this._input;
        }

        setInput(input: any): void {
            this._input = input;
        }
    }
}