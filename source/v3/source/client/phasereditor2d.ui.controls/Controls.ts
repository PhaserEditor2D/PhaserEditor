/// <reference path="./Control.ts"/>

namespace phasereditor2d.ui.controls {

    class IconImpl implements IIcon {

        constructor(public img: HTMLImageElement) {

        }

        paint(context: CanvasRenderingContext2D, x: number, y: number, w?: number, h?: number) {
            // we assume the image size is under 16x16 (for now)
            w = w ? w : 16;
            h = h ? h : 16;
            const imgW = this.img.naturalWidth;
            const imgH = this.img.naturalHeight;
            const dx = (w - imgW) / 2;
            const dy = (h - imgH) / 2;
            context.drawImage(this.img, (x + dx) | 0, (y + dy) | 0);
        }

    }

    class ImageImpl implements IImage {
        private _ready: boolean;

        constructor(public readonly imageElement: HTMLImageElement) {
            this._ready = false;
        }

        preload(): Promise<any> {
            if (this._ready) {
                return Promise.resolve();
            }

            return this.imageElement.decode().then(_ => {
                this._ready = true;
            });
        }

        paint(context: CanvasRenderingContext2D, x: number, y: number, w: number, h: number): void {
            if (this._ready) {
                const center = true;

                const naturalWidth = this.imageElement.naturalWidth;
                const naturalHeight = this.imageElement.naturalHeight;

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
                    context.drawImage(this.imageElement, imgX, imgY, imgDstW, imgDstH);
                }
            } else {
                context.strokeRect(x, y, w, h);
            }
        }
    }

    export class Controls {
        private static _icons: Map<string, IIcon> = new Map();
        private static _images: Map<String, IImage> = new Map();

        static ICON_TREE_COLLAPSE = "tree-collapse";
        static ICON_TREE_EXPAND = "tree-expand";
        static ICON_FILE = "file";
        static ICON_FOLDER = "folder";
        static ICON_FILE_FONT = "file-font";
        static ICON_FILE_IMAGE = "file-image";
        static ICON_FILE_VIDEO = "file-movie";
        static ICON_FILE_SCRIPT = "file-script";
        static ICON_FILE_SOUND = "file-sound";
        static ICON_FILE_TEXT = "file-text";


        private static ICONS = [
            Controls.ICON_TREE_COLLAPSE,
            Controls.ICON_TREE_EXPAND,
            Controls.ICON_FILE,
            Controls.ICON_FOLDER,
            Controls.ICON_FILE_FONT,
            Controls.ICON_FILE_IMAGE,
            Controls.ICON_FILE_SCRIPT,
            Controls.ICON_FILE_SOUND,
            Controls.ICON_FILE_TEXT,
            Controls.ICON_FILE_VIDEO
        ];

        static preload() {
            return Promise.all(
                Controls.ICONS.map(
                    name => {
                        const icon = <IconImpl>this.getIcon(name);
                        return icon.img.decode();
                    }
                )
            );
        }

        static getImage(url: string, id: string = url): IImage {
            if (Controls._images.has(id)) {
                return Controls._images.get(id);
            }

            const img = new ImageImpl(new Image());

            img.imageElement.src = url;

            Controls._images.set(id, img);

            return img;
        }

        static getIcon(name: string): IIcon {
            if (Controls._icons.has(name)) {
                return Controls._icons.get(name);
            }
            const img = new Image();
            img.src = "phasereditor2d.ui.controls/images/16/" + name + ".png";
            const icon = new IconImpl(img);
            Controls._icons.set(name, icon);
            return icon;
        }

        private static LIGHT_THEME: Theme = {
            treeItemOverBackground: "#0000001f",
            treeItemSelectionBackground: "#5555ffdf",
            treeItemSelectionForeground: "#fafafa",
            treeItemForeground: "#000"
        };

        private static DARK_THEME: Theme = Controls.LIGHT_THEME;

        public static theme: Theme = Controls.LIGHT_THEME;

        private static getSmoothingPrefix(context: CanvasRenderingContext2D) {
            const vendors = ['i', 'webkitI', 'msI', 'mozI', 'oI'];
            for (let i = 0; i < vendors.length; i++) {
                const s = vendors[i] + 'mageSmoothingEnabled';
                if (s in context) {
                    return s;
                }
            }
            return null;
        };


        static disableCanvasSmoothing(context: CanvasRenderingContext2D) {
            const prefix = this.getSmoothingPrefix(context);
            if (prefix) {
                context[prefix] = false;
            }
            return context;
        };
    }
}