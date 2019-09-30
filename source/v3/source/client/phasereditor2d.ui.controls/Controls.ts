/// <reference path="./Control.ts"/>

namespace phasereditor2d.ui.controls {

    export const EVENT_SELECTION = "selectionChanged";
    export const EVENT_THEME = "themeChanged";

    export enum PreloadResult {
        NOTHING_LOADED,
        RESOURCES_LOADED
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

            const img = new DefaultImage(new Image(), url);

            Controls._images.set(id, img);

            return img;
        }

        static getIcon(name: string, baseUrl: string = "phasereditor2d.ui.controls/images"): IImage {
            const url = `${baseUrl}/${ICON_SIZE}/${name}.png`;
            return Controls.getImage(url, name);
        }

        static createIconElement(icon?: IImage) {
            const element = document.createElement("canvas");
            element.width = element.height = ICON_SIZE;
            element.style.width = element.style.height = ICON_SIZE + "px";
            const context = element.getContext("2d");
            context.imageSmoothingEnabled = false;
            if (icon) {
                icon.paint(context, 0, 0, ICON_SIZE, ICON_SIZE, false);
            }
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

        static switchTheme() {
            const classList = document.getElementsByTagName("html")[0].classList;
            if (classList.contains("light")) {
                this.theme = this.DARK_THEME;
                classList.remove("light");
                classList.add("dark");
            } else {
                this.theme = this.LIGHT_THEME;
                classList.remove("dark");
                classList.add("light");
            }
            window.dispatchEvent(new CustomEvent(EVENT_THEME, { detail: this.theme }));
        }

        static drawRoundedRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, topLeft = 5, topRight = 5, bottomRight = 5, bottomLeft = 5) {
            ctx.save();
            ctx.beginPath();
            ctx.moveTo(x + topLeft, y);
            ctx.lineTo(x + w - topRight, y);
            ctx.quadraticCurveTo(x + w, y, x + w, y + topRight);
            ctx.lineTo(x + w, y + h - bottomRight);
            ctx.quadraticCurveTo(x + w, y + h, x + w - bottomRight, y + h);
            ctx.lineTo(x + bottomLeft, y + h);
            ctx.quadraticCurveTo(x, y + h, x, y + h - bottomLeft);
            ctx.lineTo(x, y + topLeft);
            ctx.quadraticCurveTo(x, y, x + topLeft, y);
            ctx.closePath();
            ctx.fill();
            ctx.restore();
        }

    }
}