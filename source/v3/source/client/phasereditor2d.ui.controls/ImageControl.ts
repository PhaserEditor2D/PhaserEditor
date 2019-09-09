
namespace phasereditor2d.ui.controls {

    export class ImageControl extends Control {

        private _image: IImage;
        private _canvas: HTMLCanvasElement;
        private _context: CanvasRenderingContext2D;

        constructor() {
            super("canvas", "ImageControl");
            this._canvas = <HTMLCanvasElement>this.getElement();
            this._context = this._canvas.getContext("2d");
        }

        setImage(image: IImage): void {
            this._image = image;
        }

        getImage() {
            return this._image;
        }

        getCanvas() {
            return this._canvas;
        }

        resizeTo(parent?: HTMLElement) {
            parent = parent || this.getElement().parentElement;
            this.style.width = parent.clientWidth + "px";
            this.style.height = parent.clientHeight + "px";
            this.repaint();
        }

        async repaint() {
            if (this._image) {

                this.repaint2();

                const result = await this._image.preload();

                if (result === PreloadResult.RESOURCES_LOADED) {
                    this.repaint2();
                }

            } else {
                this.clear();
            }
        }

        private ensureCanvasSize() {
            if (this._canvas.width !== this._canvas.clientWidth || this._canvas.height !== this._canvas.clientHeight) {
                this._canvas.width = this._canvas.clientWidth;
                this._canvas.height = this._canvas.clientHeight;
            }
        }

        private clear() {
            this.ensureCanvasSize();
            this._context.clearRect(0, 0, this._canvas.width, this._canvas.height);
        }

        private repaint2() {
            this.ensureCanvasSize();
            this.clear();
            this._image.paint(this._context, 0, 0, this._canvas.width, this._canvas.height, true);
        }

    }
}