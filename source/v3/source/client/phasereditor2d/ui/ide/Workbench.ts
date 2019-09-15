/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/PaddingPanel.ts"/>
/// <reference path="../ide/ViewPart.ts"/>
/// <reference path="../ide/DesignWindow.ts"/>
/// <reference path="../../core/io/FileStorage.ts"/>

namespace phasereditor2d.ui.ide {

    export const PART_DEACTIVATE_EVENT = "partDeactivate";
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

            this._designWindow = new ide.DesignWindow();
            document.getElementById("body").appendChild(this._designWindow.getElement());

            this.initEvents();
        }

        getDesignWindow() {
            return this._designWindow;
        }

        getActiveWindow() : ide.Window {
            return this.getDesignWindow();
        }

        private initEvents() {
            window.addEventListener("mousedown", e => {
                const part = this.findPart(<any>e.target);
                this.setActivePart(part);
            });
        }

        getActivePart() {
            return this._activePart;
        }

        private setActivePart(part: Part): void {
            if (part === this._activePart) {
                return;
            }

            if (!part) {
                return;
            }

            const old = this._activePart;
            this._activePart = part;

            if (old) {
                this.toggleActivePart(old);
                this.dispatchEvent(new CustomEvent(PART_DEACTIVATE_EVENT, { detail: old }));
            }

            if (part) {
                this.toggleActivePart(part);
            }

            this.dispatchEvent(new CustomEvent(PART_ACTIVATE_EVENT, { detail: part }));
        }

        private toggleActivePart(part: Part) {
            const tabPane = this.findTabPane(part.getElement());

            if (part.containsClass("activePart")) {
                part.removeClass("activePart");
                tabPane.removeClass("activePart");
            } else {
                part.addClass("activePart");
                tabPane.addClass("activePart");
            }
        }

        private findTabPane(element : HTMLElement) {
            if (element) {
                const control = controls.Control.getControlOf(element);
                if (control && control instanceof controls.TabPane) {
                    return control;
                }
                return this.findTabPane(element.parentElement);
            }
            return null;
        }

        findPart(element: HTMLElement): Part {
            if (element["__part"]) {
                return element["__part"];
            }

            const control = controls.Control.getControlOf(element);

            if (control && control instanceof controls.TabPane) {
                const tabPane = <controls.TabPane>control;
                const content = tabPane.getSelectedTabContent();
                if (content) {
                    const element = content.getElement().children.item(0);
                    if (element["__part"]) {
                        return element["__part"];
                    }
                }
            }

            if (element.parentElement) {
                return this.findPart(element.parentElement);
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

        getFileImage(file: core.io.FilePath) {
            return controls.Controls.getImage(file.getUrl(), file.getId());
        }

    }


}