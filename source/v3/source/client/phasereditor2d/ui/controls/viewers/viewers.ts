namespace phasereditor2d.ui.controls.viewers {

    export class RenderCellArgs {
        constructor(
            public canvasContext: CanvasRenderingContext2D,
            public x: number,
            public y: number,
            public obj: any,
            public view: Viewer) {
        }
    };

    export interface ICellRenderer {
        renderCell(args: RenderCellArgs): void;

        cellHeight(args: RenderCellArgs): number;

        preload(obj: any): Promise<any>;
    }

    export interface ICellRendererProvider {
        getCellRenderer(element: any): ICellRenderer;
    }

    export abstract class LabelCellRenderer implements ICellRenderer {

        renderCell(args: RenderCellArgs): void {
            const label = this.getLabel(args.obj);
            const img = this.getImage(args.obj);
            let x = args.x;

            const ctx = args.canvasContext;
            ctx.fillStyle = Controls.theme.treeItemForeground;

            if (img) {
                const h = this.cellHeight(args);
                img.paint(ctx, x, args.y, 16, h);
                x += 20;
            }

            ctx.save();
            if (args.view.isSelected(args.obj)) {
                ctx.fillStyle = Controls.theme.treeItemSelectionForeground;
            }
            ctx.fillText(label, x, args.y + 15);
            ctx.restore();
        }

        abstract getLabel(obj: any): string;

        abstract getImage(obj: any): controls.IIcon;

        cellHeight(args: RenderCellArgs): number {
            return 20;
        }

        preload(): Promise<any> {
            return Promise.resolve();
        }
    }

    export abstract class ImageCellRenderer implements ICellRenderer {
        abstract getLabel(obj: any): string;

        abstract getImage(obj: any): IImage;

        renderCell(args: RenderCellArgs): void {
            const label = this.getLabel(args.obj);
            const h = this.cellHeight(args);

            const ctx = args.canvasContext;

            const img = this.getImage(args.obj);
            img.paint(ctx, args.x, args.y, h, h);
            ctx.save();

            ctx.fillStyle = Controls.theme.treeItemForeground;

            if (args.view.isSelected(args.obj)) {
                ctx.fillStyle = Controls.theme.treeItemSelectionForeground;
            }

            ctx.fillText(label, args.x + h + 5, args.y + h / 2 + 6);

            ctx.restore();
        }

        cellHeight(args: RenderCellArgs): number {
            return args.view.getCellSize();
        }

        preload(obj: any): Promise<any> {
            return this.getImage(obj).preload();
        }
    }

    export interface IContentProvider {

    }

    export class Rect {
        constructor(
            public x: number = 0,
            public y: number = 0,
            public w: number = 0,
            public h: number = 0,
        ) {
        }

        set(x: number, y: number, w: number, h: number) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        contains(x: number, y: number): boolean {
            return x >= this.x && x <= this.x + this.w && y >= this.y && y <= this.y + this.h;
        }
    }

    export class PaintItem extends Rect {
        constructor(
            public index: number,
            public data: any
        ) {
            super();
        }
    }

    export abstract class Viewer extends Control {
        private _contentProvider: IContentProvider;
        private _cellRendererProvider: ICellRendererProvider;
        private _input: any;
        private _cellSize: number;
        private _expandedObjects: Set<any>;
        private _selectedObjects: Set<any>;
        protected _context: CanvasRenderingContext2D;
        protected _paintItems: PaintItem[];
        private _overObject: any;
        private _lastSelectedItemIndex: number = -1;

        constructor() {
            super("canvas");
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
            canvas.addEventListener("mouseup", e => this.onMouseUp(e));
            // canvas.parentElement.addEventListener("keydown", e => this.onKeyDown(e));
        }


        protected getPaintItemAt(e: MouseEvent): PaintItem {
            for (let item of this._paintItems) {
                if (item.contains(e.offsetX, e.offsetY)) {
                    return item;
                }
            }
            return null;
        }

        private fireSelectionChanged() {

        }


        //TODO: is not fired, I am looking the reason.
        private onKeyDown(e: KeyboardEvent): void {
            if (e.key === "Escape") {
                if (this._selectedObjects.size > 0) {
                    this._selectedObjects.clear();
                    this.repaint();
                    this.fireSelectionChanged();
                }
            }
        }

        private onMouseUp(e: MouseEvent): void {
            if (e.button !== 0) {
                return;
            }

            const item = this.getPaintItemAt(e);

            if (item === null) {
                return;
            }

            let selChanged = false;

            const data = item.data;

            if (e.ctrlKey || e.metaKey) {
                this._selectedObjects.add(data);
                selChanged = true;
            } else if (e.shiftKey) {
                if (this._lastSelectedItemIndex >= 0 && this._lastSelectedItemIndex != item.index) {
                    const start = Math.min(this._lastSelectedItemIndex, item.index);
                    const end = Math.max(this._lastSelectedItemIndex, item.index);
                    for (let i = start; i <= end; i++) {
                        this._selectedObjects.add(this._paintItems[i].data);
                    }
                    selChanged = true;
                }
            } else {
                this._selectedObjects.clear();
                this._selectedObjects.add(data);
                selChanged = true;
            }

            if (selChanged) {
                this.repaint();
                this.fireSelectionChanged();
                this._lastSelectedItemIndex = item.index;
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
                this.repaint();
            }
        }

        getOverObject() {
            return this._overObject;
        }

        private initContext(): void {
            this._context = this.getCanvas().getContext("2d");
            this._context.imageSmoothingEnabled = false;
            Controls.disableCanvasSmoothing(this._context);
            this._context.font = "14px sans-serif";
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

            console.log("first paint");
            this.repaint2();

            console.log("preload");
            await this.preload();

            console.log("second paint");
            this.repaint2();
        }

        private repaint2(): void {
            this._paintItems = [];

            const canvas = this.getCanvas();

            this._context.clearRect(0, 0, canvas.width, canvas.height);

            if (this._cellRendererProvider && this._contentProvider && this._input !== null) {
                this.paint();
            }
        }

        protected abstract preload(): Promise<any>;

        protected paintItemBackground(obj: any, x: number, y: number, w: number, h: number): void {
            let fillStyle = null;

            if (this.isSelected(obj)) {
                fillStyle = Controls.theme.treeItemSelectionBackground;;
            } else if (obj === this._overObject) {
                fillStyle = Controls.theme.treeItemOverBackground;
            }

            if (fillStyle != null) {
                this._context.save();
                this._context.fillStyle = fillStyle;
                this._context.fillRect(x, y, w, h);
                this._context.restore();
            }
        }

        layout(): void {
            const b = this.getBounds();
            ui.controls.setElementBounds(this.getElement(), {
                x: b.x,
                y: b.y,
                width: b.width | 0,
                height: b.height | 0
            });

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

        getCellSize() {
            return this._cellSize;
        }

        setCellSize(cellSize: number): void {
            this._cellSize = cellSize;
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