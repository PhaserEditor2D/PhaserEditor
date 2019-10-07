/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../ide/ViewPart.ts"/>
/// <reference path="../ide/DesignWindow.ts"/>
/// <reference path="../../core/io/FileStorage.ts"/>
/// <reference path="./editors/image/ImageEditor.ts"/>

namespace phasereditor2d.ui.ide {

    export const EVENT_PART_DEACTIVATED = "partDeactivated";
    export const EVENT_PART_ACTIVATED = "partActivated";

    export const EVENT_EDITOR_DEACTIVATED = "editorDeactivated";
    export const EVENT_EDITOR_ACTIVATED = "editorActivated";

    export const ICON_FILE = "file";
    export const ICON_FOLDER = "folder";
    export const ICON_FILE_FONT = "file-font";
    export const ICON_FILE_IMAGE = "file-image";
    export const ICON_FILE_VIDEO = "file-movie";
    export const ICON_FILE_SCRIPT = "file-script";
    export const ICON_FILE_SOUND = "file-sound";
    export const ICON_FILE_TEXT = "file-text";
    export const ICON_ASSET_PACK = "asset-pack";
    export const ICON_OUTLINE = "outline";
    export const ICON_INSPECTOR = "inspector";
    export const ICON_BLOCKS = "blocks";

    const ICONS = [
        ICON_FILE,
        ICON_FOLDER,
        ICON_FILE_FONT,
        ICON_FILE_IMAGE,
        ICON_FILE_VIDEO,
        ICON_FILE_SCRIPT,
        ICON_FILE_SOUND,
        ICON_FILE_TEXT,
        ICON_ASSET_PACK,
        ICON_OUTLINE,
        ICON_INSPECTOR,
        ICON_BLOCKS
    ];

    export class Workbench extends EventTarget {
        private static _workbench: Workbench;


        static getWorkbench() {
            if (!Workbench._workbench) {
                Workbench._workbench = new Workbench();
            }

            return this._workbench;
        }

        private _designWindow: ide.DesignWindow;
        private _contentType_icon_Map: Map<string, controls.IImage>;
        private _fileStorage: core.io.IFileStorage;
        private _contentTypeRegistry: core.ContentTypeRegistry;
        private _activePart: Part;
        private _activeEditor: EditorPart;
        private _editorRegistry: EditorRegistry;
        private _commandManager: commands.CommandManager;

        private constructor() {
            super();

            this._contentType_icon_Map = new Map();

            this._contentType_icon_Map.set(CONTENT_TYPE_IMAGE, this.getWorkbenchIcon(ICON_FILE_IMAGE));
            this._contentType_icon_Map.set(CONTENT_TYPE_AUDIO, this.getWorkbenchIcon(ICON_FILE_SOUND));
            this._contentType_icon_Map.set(CONTENT_TYPE_VIDEO, this.getWorkbenchIcon(ICON_FILE_VIDEO));
            this._contentType_icon_Map.set(CONTENT_TYPE_SCRIPT, this.getWorkbenchIcon(ICON_FILE_SCRIPT));
            this._contentType_icon_Map.set(CONTENT_TYPE_TEXT, this.getWorkbenchIcon(ICON_FILE_TEXT));
            this._contentType_icon_Map.set(editors.pack.CONTENT_TYPE_ASSET_PACK, this.getWorkbenchIcon(ICON_ASSET_PACK));

            this._editorRegistry = new EditorRegistry();

            this._activePart = null;
            this._activeEditor = null;

        }

        async start() {

            await this.preloadIcons();

            await this.initFileStorage();

            await this.initContentTypes();

            this.initCommands();

            this.initEditors();

            this._designWindow = new ide.DesignWindow();
            document.getElementById("body").appendChild(this._designWindow.getElement());

            this.initEvents();
        }

        private async preloadIcons() {
            return Promise.all(ICONS.map(icon => this.getWorkbenchIcon(icon).preload()));
        }

        private initCommands() {
            this._commandManager = new commands.CommandManager();
            
            IDECommands.init();
            editors.scene.SceneEditorCommands.init();
        }

        getCommandManager() {
            return this._commandManager;
        }

        private initEditors(): void {
            this._editorRegistry.registerFactory(editors.image.ImageEditor.getFactory());
            this._editorRegistry.registerFactory(editors.pack.AssetPackEditor.getFactory());
            this._editorRegistry.registerFactory(editors.scene.SceneEditor.getFactory());
        }

        getDesignWindow() {
            return this._designWindow;
        }

        getActiveWindow(): ide.Window {
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

        getActiveEditor() {
            return this._activeEditor;
        }

        private setActiveEditor(editor: EditorPart) {
            if (editor === this._activeEditor) {
                return;
            }

            if (!editor) {
                this._activeEditor = null;
                return;
            }

            this._activeEditor = editor;

            this.dispatchEvent(new CustomEvent(EVENT_EDITOR_ACTIVATED, { detail: editor }));
        }

        /**
         * Users may not call this method. This is public only for convenience.
         */
        setActivePart(part: Part): void {
            if (part !== this._activePart) {

                const old = this._activePart;

                this._activePart = part;

                if (old) {
                    this.toggleActivePartClass(old);
                    this.dispatchEvent(new CustomEvent(EVENT_PART_DEACTIVATED, { detail: old }));
                }

                if (part) {
                    this.toggleActivePartClass(part);
                }

                this.dispatchEvent(new CustomEvent(EVENT_PART_ACTIVATED, { detail: part }));
            }

            if (part instanceof EditorPart) {
                this.setActiveEditor(part);
            }
        }

        private toggleActivePartClass(part: Part) {
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

        findPart(element: HTMLElement): Part {
            if (controls.TabPane.isTabLabel(element)) {
                element = controls.TabPane.getContentFromLabel(element).getElement();
            }

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

        private async initFileStorage() {
            this._fileStorage = new core.io.ServerFileStorage();
            await this._fileStorage.reload();
        }

        private async initContentTypes() {
            const reg = new core.ContentTypeRegistry();

            reg.registerResolver(new editors.pack.AssetPackContentTypeResolver());
            reg.registerResolver(new editors.scene.SceneContentTypeResolver());
            reg.registerResolver(new DefaultExtensionTypeResolver());

            this._contentTypeRegistry = reg;
        }

        getContentTypeRegistry() {
            return this._contentTypeRegistry;
        }

        getFileStorage(): core.io.IFileStorage {
            return this._fileStorage;
        }

        getContentTypeIcon(contentType: string): controls.IImage {
            if (this._contentType_icon_Map.has(contentType)) {
                return this._contentType_icon_Map.get(contentType);
            }
            return null;
        }

        getFileImage(file: core.io.FilePath) {
            return controls.Controls.getImage(file.getUrl(), file.getId());
        }

        getWorkbenchIcon(name: string) {
            return controls.Controls.getIcon(name, "phasereditor2d/ui/ide/images");
        }

        getEditorRegistry() {
            return this._editorRegistry;
        }

        getEditors(): EditorPart[] {
            const editorArea = this.getActiveWindow().getEditorArea();
            return <EditorPart[]>editorArea.getContentList();
        }

        openEditor(input: any): void {
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