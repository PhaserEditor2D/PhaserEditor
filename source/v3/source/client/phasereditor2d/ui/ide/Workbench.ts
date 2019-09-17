/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../ide/ViewPart.ts"/>
/// <reference path="../ide/DesignWindow.ts"/>
/// <reference path="../../core/io/FileStorage.ts"/>
/// <reference path="./editors/image/ImageEditor.ts"/>

namespace phasereditor2d.ui.ide {

    export const EVENT_PART_DEACTIVATE = "partDeactivate";
    export const EVENT_PART_ACTIVATE = "partActivate";

    export class Workbench extends EventTarget {
        private static _workbench: Workbench;

        public static getWorkbench() {
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
        private _editorRegistry: EditorRegistry;

        private constructor() {
            super();

            this._contentType_icon_Map = new Map();

            this._contentType_icon_Map.set(CONTENT_TYPE_IMAGE, controls.Controls.getIcon(controls.Controls.ICON_FILE_IMAGE));
            this._contentType_icon_Map.set(CONTENT_TYPE_AUDIO, controls.Controls.getIcon(controls.Controls.ICON_FILE_SOUND));
            this._contentType_icon_Map.set(CONTENT_TYPE_VIDEO, controls.Controls.getIcon(controls.Controls.ICON_FILE_VIDEO));
            this._contentType_icon_Map.set(CONTENT_TYPE_SCRIPT, controls.Controls.getIcon(controls.Controls.ICON_FILE_SCRIPT));
            this._contentType_icon_Map.set(CONTENT_TYPE_TEXT, controls.Controls.getIcon(controls.Controls.ICON_FILE_TEXT));

            this._editorRegistry = new EditorRegistry();

        }

        public async start() {
            await this.initFileStorage();

            this.initContentTypes();

            this.initEditors();

            this._designWindow = new ide.DesignWindow();
            document.getElementById("body").appendChild(this._designWindow.getElement());

            this.initEvents();
        }

        private initEditors(): void {
            this._editorRegistry.registerFactory(editors.image.ImageEditor.getFactory());
            this._editorRegistry.registerFactory(editors.pack.AssetPackEditor.getFactory());
        }

        public getDesignWindow() {
            return this._designWindow;
        }

        public getActiveWindow(): ide.Window {
            return this.getDesignWindow();
        }

        private initEvents() {
            window.addEventListener("mousedown", e => {
                const part = this.findPart(<any>e.target);
                this.setActivePart(part);
            });
        }

        public getActivePart() {
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
                this.dispatchEvent(new CustomEvent(EVENT_PART_DEACTIVATE, { detail: old }));
            }

            if (part) {
                this.toggleActivePart(part);
            }

            this.dispatchEvent(new CustomEvent(EVENT_PART_ACTIVATE, { detail: part }));
        }

        private toggleActivePart(part: Part) {
            const tabPane = this.findTabPane(part.getElement());

            if (!tabPane) {
                // maybe the clicked part was closed
                return;
            }

            if (part.containsClass("activePart")) {
                part.removeClass("activePart");
                tabPane.removeClass("activePart");
            } else {
                part.addClass("activePart");
                tabPane.addClass("activePart");
            }
        }

        private findTabPane(element: HTMLElement) {
            if (element) {
                const control = controls.Control.getControlOf(element);
                if (control && control instanceof controls.TabPane) {
                    return control;
                }
                return this.findTabPane(element.parentElement);
            }
            return null;
        }

        public findPart(element: HTMLElement): Part {
            if (element["__part"]) {
                return element["__part"];
            }

            const control = controls.Control.getControlOf(element);

            if (control && control instanceof controls.TabPane) {
                const tabPane = <controls.TabPane>control;
                const content = tabPane.getSelectedTabContent();
                if (content) {
                    const element = content.getElement();
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

            reg.registerResolver(new core.pack.AssetPackContentTypeResolver());
            reg.registerResolver(new DefaultExtensionTypeResolver());

            this._contentTypeRegistry = reg;
        }

        public getContentTypeRegistry() {
            return this._contentTypeRegistry;
        }

        public getFileStorage(): core.io.IFileStorage {
            return this._fileStorage;
        }

        public getContentTypeIcon(contentType: string): controls.IIcon {
            if (this._contentType_icon_Map.has(contentType)) {
                return this._contentType_icon_Map.get(contentType);
            }
            return null;
        }

        public getFileImage(file: core.io.FilePath) {
            return controls.Controls.getImage(file.getUrl(), file.getId());
        }

        public getEditorRegistry() {
            return this._editorRegistry;
        }

        public getEditors(): EditorPart[] {
            const editorArea = this.getActiveWindow().getEditorArea();
            return <EditorPart[]>editorArea.getContentList();
        }

        public openEditor(input: any): void {
            const editorArea = this.getActiveWindow().getEditorArea();

            {
                const editors = this.getEditors();
                for (let editor of editors) {
                    if (editor.getInput() === input) {
                        editorArea.activateEditor(editor);
                        this.setActivePart(editor);
                        return;
                    }
                }
            }

            const factory = this._editorRegistry.getFactoryForInput(input);
            if (factory) {
                const editor = factory.createEditor();
                editorArea.addPart(editor, true);
                editor.setInput(input);
                editorArea.activateEditor(editor);
                this.setActivePart(editor);
            } else {
                alert("No editor available for the given input.");
            }
        }

    }

}