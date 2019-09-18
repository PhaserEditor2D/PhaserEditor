/// <reference path="./Control.ts"/>

namespace phasereditor2d.ui.controls {

    export const EVENT_SELECTION = "selected";

    export enum PreloadResult {
        NOTHING_LOADED,
        RESOURCES_LOADED
    }


    class ImageImpl implements IImage {
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
        }
    }

    export const ICON_CONTROL_TREE_COLLAPSE = "tree-collapse";
    export const ICON_CONTROL_TREE_EXPAND = "tree-expand";
    export const ICON_CONTROL_CLOSE = "close";
    export const ICON_SIZE = 16;

    const ICONS = [
        ICON_CONTROL_TREE_COLLAPSE,
        ICON_CONTROL_TREE_EXPAND,
        ICON_CONTROL_CLOSE
    ];

    export class Controls {

        private static _images: Map<String, IImage> = new Map();

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

        static async preload() {
            return Promise.all(ICONS.map(icon => this.getIcon(icon).preload()));
        }

        static getImage(url: string, id: string): IImage {
            if (Controls._images.has(id)) {
                return Controls._images.get(id);
            }

            const img = new ImageImpl(new Image(), url);

            Controls._images.set(id, img);

            return img;
        }

        static getIcon(name: string, baseUrl: string = "phasereditor2d.ui.controls/images"): IImage {
            const url = `${baseUrl}/${ICON_SIZE}/${name}.png`;
            return Controls.getImage(url, name);
        }

        static createIconElement(icon: IImage) {
            // const elem = new Image(ICON_SIZE, ICON_SIZE);
            // elem.src = `phasereditor2d.ui.controls/images/${ICON_SIZE}/${name}.png`;
            // return elem;
            const element = document.createElement("canvas");
            element.width = element.height = ICON_SIZE;
            element.style.width = element.style.height = ICON_SIZE + "px";
            const context = element.getContext("2d");
            context.imageSmoothingEnabled = false;
            icon.paint(context, 0, 0, ICON_SIZE, ICON_SIZE, false);
            return element;
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

        static theme: Theme = Controls.DARK_THEME;

    }
}