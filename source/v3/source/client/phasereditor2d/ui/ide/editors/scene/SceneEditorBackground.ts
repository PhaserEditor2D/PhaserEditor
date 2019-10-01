namespace phasereditor2d.ui.ide.editors.scene {

    export class SceneEditorBackground {

        private _editor: SceneEditor;
        private _canvas: HTMLCanvasElement;
        private _ctx: CanvasRenderingContext2D;

        constructor(editor: SceneEditor) {
            this._editor = editor;
            this._canvas = document.createElement("canvas");
            this._canvas.style.position = "absolute";
        }

        getCanvas(): HTMLCanvasElement {
            return this._canvas;
        }

        private resetContext() {
            this._ctx = this._canvas.getContext("2d");
            this._ctx.imageSmoothingEnabled = false;
        }

        resizeTo() {
            const parent = this._canvas.parentElement;
            this._canvas.width = parent.clientWidth;
            this._canvas.height = parent.clientHeight;
            this._canvas.style.width = this._canvas.width + "px";
            this._canvas.style.height = this._canvas.height + "px";
            this.resetContext();
        }

        render() {
            if (!this._ctx) {
                this.resetContext();
            }

            const ctx = this._ctx;
            const canvasWidth = this._canvas.width;
            const canvasHeight = this._canvas.height;

            ctx.clearRect(0, 0, canvasWidth, canvasHeight);

            // render solid background

            ctx.fillStyle = "#6e6e6e";
            ctx.fillRect(0, 0, canvasWidth, canvasHeight);

            // render grid

            ctx.strokeStyle = "#aeaeae";
            ctx.lineWidth = 1;

            let x = 0;

            while (x < canvasWidth) {
                ctx.beginPath();
                ctx.moveTo(x, 0);
                ctx.lineTo(x, canvasHeight);
                ctx.closePath();
                ctx.stroke();

                let y = 0;

                while (y < canvasHeight) {

                    ctx.beginPath();
                    ctx.moveTo(0, y);
                    ctx.lineTo(canvasWidth, y);
                    ctx.closePath();
                    ctx.stroke();

                    y += 80;
                }
                x += 80;
            }

        }
    }

}