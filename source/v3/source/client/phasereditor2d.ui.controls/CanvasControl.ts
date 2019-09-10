
namespace phasereditor2d.ui.controls {

    export abstract class CanvasControl extends Control {

        protected _canvas: HTMLCanvasElement;
        protected _context: CanvasRenderingContext2D;
        private _padding: number;

        constructor(padding: number = 0, ...classList : string[]) {
            super("canvas", "CanvasControl", ...classList);
            this._padding = padding;
            this._canvas = <HTMLCanvasElement>this.getElement();
            this._context = this._canvas.getContext("2d");
        }

        getCanvas() {
            return this._canvas;
        }

        resizeTo(parent?: HTMLElement): void {
            parent = parent || this.getElement().parentElement;
            this.style.width = parent.clientWidth - this._padding * 2 + "px";
            this.style.height = parent.clientHeight - this._padding * 2 + "px";
            this.repaint();
        }

        getPadding() {
            return this._padding;
        }

        protected ensureCanvasSize(): void {
            if (this._canvas.width !== this._canvas.clientWidth || this._canvas.height !== this._canvas.clientHeight) {
                this._canvas.width = this._canvas.clientWidth;
                this._canvas.height = this._canvas.clientHeight;
            }
        }

        clear() : void {
            this._context.clearRect(0, 0, this._canvas.width, this._canvas.height);
        }

        repaint(): void {
            this.ensureCanvasSize();
            this._context.clearRect(0, 0, this._canvas.width, this._canvas.height);
            this.paint();
        }

        protected abstract paint(): void;
    }
}