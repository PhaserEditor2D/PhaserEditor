/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/PaddingPanel.ts"/>
/// <reference path="../ide/ViewPart.ts"/>
/// <reference path="../ide/DesignWindow.ts"/>
/// <reference path="../../core/io/FileStorage.ts"/>

namespace phasereditor2d.ui.ide {

    export const PART_ACTIVATE_EVENT = "partActivate";

    export class Workbench extends EventTarget {
        private static _workbench: Workbench;


        static getWorkbench() {
            if (!Workbench._workbench) {
                Workbench._workbench = new Workbench();
            }

            return this._workbench;
        }

        private _designWindow: ide.DesignWindow;
        private _contentType_icon_Map: Map<string, controls.IIcon>;
        private _fileStorage: core.io.IFileStorage;
        private _contentTypeRegistry: core.ContentTypeRegistry;
        private _activePart: Part;

        private constructor() {
            super();
            this._contentType_icon_Map = new Map();

            this._contentType_icon_Map.set(CONTENT_TYPE_IMAGE, controls.Controls.getIcon(controls.Controls.ICON_FILE_IMAGE));
            this._contentType_icon_Map.set(CONTENT_TYPE_AUDIO, controls.Controls.getIcon(controls.Controls.ICON_FILE_SOUND));
            this._contentType_icon_Map.set(CONTENT_TYPE_VIDEO, controls.Controls.getIcon(controls.Controls.ICON_FILE_VIDEO));
            this._contentType_icon_Map.set(CONTENT_TYPE_SCRIPT, controls.Controls.getIcon(controls.Controls.ICON_FILE_SCRIPT));
            this._contentType_icon_Map.set(CONTENT_TYPE_TEXT, controls.Controls.getIcon(controls.Controls.ICON_FILE_TEXT));

        }

        async start() {
            await this.initFileStorage();

            this.initContentTypes();

            this.initEvents();

            this._designWindow = new ide.DesignWindow();
            document.getElementById("body").appendChild(this._designWindow.getElement());
        }

        private initEvents() {
            window.addEventListener("click", e => {
                const part = this.findPart(<any>e.target);

            });
        }

        getActivePart() {
            return this._activePart;
        }

        private setActivePart(part: Part): void {
            this._activePart;
            this.dispatchEvent(new CustomEvent(PART_ACTIVATE_EVENT, { detail: part }));
        }

        findPart(element: HTMLElement): Part {
            return this.findPart2(element);
        }

        private findPart2(element: HTMLElement): Part {
            if ((<any>element).__part) {
                return (<any>element).__part;
            }

            if (element.parentElement) {
                return this.findPart2(element.parentElement);
            }

            return null;
        }

        private initFileStorage() {
            this._fileStorage = new core.io.ServerFileStorage();
            return this._fileStorage.reload();
        }

        private initContentTypes() {
            const reg = new core.ContentTypeRegistry();

            reg.registerResolver(new DefaultExtensionTypeResolver());

            this._contentTypeRegistry = reg;
        }

        getContentTypeRegistry() {
            return this._contentTypeRegistry;
        }

        getFileStorage(): core.io.IFileStorage {
            return this._fileStorage;
        }

        getContentTypeIcon(contentType: string): controls.IIcon {
            if (this._contentType_icon_Map.has(contentType)) {
                return this._contentType_icon_Map.get(contentType);
            }
            return null;
        }

    }


}