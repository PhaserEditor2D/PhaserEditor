namespace phasereditor2d.ui.controls {

    export class DefaultImage implements IImage {

        private _ready: boolean;
        private _error: boolean;
        private _url: string;
        private _img: HTMLImageElement;
        private _requestPromise: Promise<PreloadResult>;

        constructor(img: HTMLImageElement, url: string) {
            this._img = img;
            this._url = url;
            this._ready = false;
            this._error = false;
        }

        preload(): Promise<PreloadResult> {
            if (this._ready || this._error) {
                return Controls.resolveNothingLoaded();
            }

            if (this._requestPromise) {
                return this._requestPromise;
            }

            this._requestPromise = new Promise((resolve, reject) => {
                this._img.src = this._url;

                this._img.addEventListener("load", e => {
                    this._requestPromise = null;
                    this._ready = true;
                    resolve(PreloadResult.RESOURCES_LOADED);
                });

                this._img.addEventListener("error", e => {
                    console.error("ERROR: Loading image " + this._url);
                    this._requestPromise = null;
                    this._error = true;
                    resolve(PreloadResult.NOTHING_LOADED);
                });
            });

            return this._requestPromise;

            /*
            return this._img.decode().then(_ => {
                this._ready = true;
                return Controls.resolveResourceLoaded();
            }).catch(e => {
                this._ready = true;
                console.error("ERROR: Cannot decode " + this._url);
                console.error(e);
                return Controls.resolveNothingLoaded();
            });
            */
        }

        getWidth() {
            return this._ready ? this._img.naturalWidth : 16;
        }

        getHeight() {
            return this._ready ? this._img.naturalHeight : 16;
        }

        paint(context: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, center: boolean): void {
            if (this._ready) {
                const naturalWidth = this._img.naturalWidth;
                const naturalHeight = this._img.naturalHeight;

                let renderHeight = h;
                let renderWidth = w;

                let imgW = naturalWidth;
                let imgH = naturalHeight;

                // compute the right width
                imgW = imgW * (renderHeight / imgH);
                imgH = renderHeight;

                // fix width if it goes beyond the area
                if (imgW > renderWidth) {
                    imgH = imgH * (renderWidth / imgW);
                    imgW = renderWidth;
                }

                let scale = imgW / naturalWidth;

                let imgX = x + (center ? renderWidth / 2 - imgW / 2 : 0);
                let imgY = y + renderHeight / 2 - imgH / 2;

                let imgDstW = naturalWidth * scale;
                let imgDstH = naturalHeight * scale;

                if (imgDstW > 0 && imgDstH > 0) {
                    context.drawImage(this._img, imgX, imgY, imgDstW, imgDstH);
                }
            } else {
                this.paintEmpty(context, x, y, w, h)
            }
        }

        private paintEmpty(context: CanvasRenderingContext2D, x: number, y: number, w: number, h: number) {
            if (w > 10 && h > 10) {
                context.save();
                context.strokeStyle = Controls.theme.treeItemForeground;
                const cx = x + w / 2;
                const cy = y + h / 2;
                context.strokeRect(cx, cy - 1, 2, 2);
                context.strokeRect(cx - 5, cy - 1, 2, 2);
                context.strokeRect(cx + 5, cy - 1, 2, 2);
                context.restore();
            }
        }

        paintFrame(context: CanvasRenderingContext2D, srcX: number, srcY: number, scrW: number, srcH: number, dstX: number, dstY: number, dstW: number, dstH: number): void {
            if (this._ready) {
                context.drawImage(this._img, srcX, srcY, scrW, srcH, dstX, dstY, dstW, dstH);
            } else {
                this.paintEmpty(context, dstX, dstY, dstW, dstH);
            }
        }
    }

}