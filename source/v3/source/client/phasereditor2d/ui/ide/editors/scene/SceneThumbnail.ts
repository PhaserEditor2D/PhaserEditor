namespace phasereditor2d.ui.ide.editors.scene {

    class ThumbnailScene extends GameScene {

        private _data: json.SceneData;
        private _callback: (HTMLImageElement) => void;

        constructor(data: json.SceneData, callback: (HTMLImageElement) => void) {
            super(false);

            this._data = data;
            this._callback = callback;
        }

        create() {
            const parser = new json.SceneParser(this);
            parser.createSceneCache_async(this._data)
                .then(() => {

                    parser.createScene(this._data);

                    this.sys.renderer.snapshot(img => {

                        this._callback(<HTMLImageElement>img);

                    });
                });
        }
    }

    export class SceneThumbnail implements controls.IImage {

        private _file: core.io.FilePath;
        private _image: controls.ImageWrapper;

        constructor(file: core.io.FilePath) {
            this._file = file;
            this._image = null;
        }

        paint(context: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, center: boolean): void {

            if (this._image) {
                this._image.paint(context, x, y, w, h, center);
            }
        }

        paintFrame(context: CanvasRenderingContext2D, srcX: number, srcY: number, srcW: number, srcH: number, dstX: number, dstY: number, dstW: number, dstH: number): void {

            if (this._image) {
                this._image.paintFrame(context, srcX, srcY, srcW, srcH, dstX, dstY, dstW, dstH);
            }
        }

        getWidth(): number {
            return this._image ? this._image.getWidth() : 16;
        }

        getHeight(): number {
            return this._image ? this._image.getHeight() : 16;
        }

        async preload(): Promise<controls.PreloadResult> {

            if (this._image == null) {

                const content = await FileUtils.getFileString(this._file);

                const imageElement = await new Promise<HTMLImageElement>((resolve, reject) => {

                    const data: json.SceneData = JSON.parse(content);

                    const width = 800;
                    const height = 600;

                    const scene = new ThumbnailScene(data, image => {
                        resolve(image);
                    });

                    const canvas = document.createElement("canvas");
                    canvas.style.width = (canvas.width = width) + "px";
                    canvas.style.height = (canvas.height = height) + "px";

                    const game = new Phaser.Game({
                        type: Phaser.WEBGL,
                        canvas: canvas,
                        width: width,
                        height: height,
                        scale: {
                            mode: Phaser.Scale.NONE
                        },
                        render: {
                            pixelArt: true,
                            transparent: true
                        },
                        audio: {
                            noAudio: true
                        },
                        scene: scene,
                    });

                });

                this._image = new controls.ImageWrapper(imageElement);

                return controls.Controls.resolveResourceLoaded();
            }

            return controls.Controls.resolveNothingLoaded();
        }

    }

}