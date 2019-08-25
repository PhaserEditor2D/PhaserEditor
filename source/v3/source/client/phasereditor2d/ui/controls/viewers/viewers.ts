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
            ctx.fillStyle = "#000";

            if (img) {
                img.paint(ctx, x, args.y);
                x += 20;
            }

            ctx.fillText(label, x, args.y + 15);
        }

        abstract getLabel(obj: any): string;

        abstract getImage(obj: any): controls.IIcon;

        cellHeight(args: RenderCellArgs): number {
            return 20;
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

        contains(x: number, y: number): boolean {
            return x >= this.x && x <= this.x + this.w && y >= this.y && y <= this.y + this.h;
        }
    }

    export class PaintItem extends Rect {
        constructor(
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

        constructor() {
            super("canvas");
            this._cellSize = 32;

            this.initContext();

            this._input = null;
            this._expandedObjects = new Set();
            this._selectedObjects = new Set();

            (<any>window).cc = this;
        }

        private initContext(): void {
            this._context = this.getCanvas().getContext("2d");
            this._context.imageSmoothingEnabled = false;
            this._context.font = "14px sans-serif";
            this._context.fillStyle = "red";
            this._context.fillText("hello", 10, 100);
        }

        setExpanded(obj: any, expanded: boolean) {
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

        protected paintSelectionBackground(x: number, y: number, w: number, h: number) {

        }

        protected paintTreeHandler(x: number, y: number, collapsed: boolean) {
            if (collapsed) {
                this._context.strokeStyle = "#000";
                this._context.strokeRect(x, y, 16, 16);
            } else {
                this._context.fillStyle = "#000";
                this._context.fillRect(x, y, 16, 16);
            }
        }

        repaint(): void {
            this._paintItems = [];

            const canvas = this.getCanvas();

            this._context.clearRect(0, 0, canvas.width, canvas.height);

            if (this._cellRendererProvider && this._contentProvider && this._input !== null) {
                this.paint();
            }
        }

        layout() {
            const b = this.getBounds();
            ui.controls.setElementBounds(this.getElement(), b);

            const canvas = this.getCanvas();

            canvas.width = b.width;
            canvas.height = b.height;

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