/// <reference path="./Control.ts"/>

namespace phasereditor2d.ui.controls {

    export const EVENT_SELECTION = "selected";

    export enum PreloadResult {
        NOTHING_LOADED,
        RESOURCES_LOADED
    }


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
        private _requesting: boolean;
        private _error: boolean;
        private _url: string;
        private _img: HTMLImageElement;

        constructor(img: HTMLImageElement, url: string) {
            this._img = img;
            this._url = url;
            this._ready = false;
            this._error = false;
            this._requesting = false;
        }

        preload(): Promise<PreloadResult> {
            if (this._ready || this._error || this._requesting) {
                return Controls.resolveNothingLoaded();
            }

            this._requesting = true;

            return new Promise((resolve, reject) => {
                this._img.src = this._url;

                this._img.addEventListener("load", e => {
                    this._ready = true;
                    resolve(PreloadResult.RESOURCES_LOADED);
                });

                this._img.addEventListener("error", e => {
                    console.error("ERROR: Loading image " + this._url);
                    this._error = true;
                    resolve(PreloadResult.NOTHING_LOADED);
                });
            });

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

        static resolveAll(list: Promise<PreloadResult>[]): Promise<PreloadResult> {
            return Promise.all(list).then(results => {
                for (const result of results) {
                    if (result === PreloadResult.RESOURCES_LOADED) {
                        return Promise.resolve(PreloadResult.RESOURCES_LOADED);
                    }
                }
                return Promise.resolve(PreloadResult.NOTHING_LOADED);
            });
        }

        static resolveResourceLoaded() {
            return Promise.resolve(PreloadResult.RESOURCES_LOADED);
        }

        static resolveNothingLoaded() {
            return Promise.resolve(PreloadResult.NOTHING_LOADED);
        }

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

        static getImage(url: string, id: string): IImage {
            if (Controls._images.has(id)) {
                return Controls._images.get(id);
            }

            const img = new ImageImpl(new Image(), url);

            Controls._images.set(id, img);

            return img;
        }

        static getIcon(name: string): IIcon {
            if (Controls._icons.has(name)) {
                return Controls._icons.get(name);
            }
            const img = new Image();
            img.src = `phasereditor2d.ui.controls/images/16/${name}.png`;
            const icon = new IconImpl(img);
            Controls._icons.set(name, icon);
            return icon;
        }

        static createIconElement(name: string) {
            const elem = new Image(16, 16);
            elem.src = `phasereditor2d.ui.controls/images/16/${name}.png`;
            return elem;
        }

        private static LIGHT_THEME: Theme = {
            treeItemSelectionBackground: "#4242ff",
            treeItemSelectionForeground: "#f0f0f0",
            treeItemForeground: "#000"
        };

        private static DARK_THEME: Theme = {
            treeItemSelectionBackground: "#f0a050", //"#101ea2",//"#8f8f8f",
            treeItemSelectionForeground: "#0e0e0e",
            treeItemForeground: "#f0f0f0"
        };

        public static theme: Theme = Controls.DARK_THEME;

    }
}