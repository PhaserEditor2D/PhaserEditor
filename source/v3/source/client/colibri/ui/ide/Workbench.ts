/// <reference path="../controls/Controls.ts"/>

namespace colibri.ui.ide {

    export const EVENT_PART_DEACTIVATED = "partDeactivated";
    export const EVENT_PART_ACTIVATED = "partActivated";

    export const EVENT_EDITOR_DEACTIVATED = "editorDeactivated";
    export const EVENT_EDITOR_ACTIVATED = "editorActivated";

    export const ICON_FILE = "file";
    export const ICON_FOLDER = "folder";

    export class Workbench extends EventTarget {

        private static _workbench: Workbench;

        static getWorkbench() {

            if (!Workbench._workbench) {

                Workbench._workbench = new Workbench();

            }

            return this._workbench;
        }

        private _fileStringCache: core.io.FileStringCache;
        private _fileImageCache: ImageFileCache;
        private _activeWindow: ide.WorkbenchWindow;
        private _contentType_icon_Map: Map<string, controls.IImage>;
        private _fileStorage: core.io.IFileStorage;
        private _contentTypeRegistry: core.ContentTypeRegistry;
        private _activePart: Part;
        private _activeEditor: EditorPart;
        private _activeElement: HTMLElement;
        private _editorRegistry: EditorRegistry;
        private _extensionRegistry: core.extensions.ExtensionRegistry;
        private _commandManager: commands.CommandManager;

        private constructor() {

            super();

            this._editorRegistry = new EditorRegistry();

            this._activePart = null;
            this._activeEditor = null;
            this._activeElement = null;

            this._fileImageCache = new ImageFileCache();

            this._extensionRegistry = new core.extensions.ExtensionRegistry();
        }

        async launch(plugins: Plugin[]) {

            console.log("Workbench: starting.");

            for (const plugin of plugins) {
                plugin.registerExtensions(this._extensionRegistry);
            }

            for (const plugin of plugins) {
                console.log(`\tPlugin: starting %c${plugin.getId()}`, "color:blue");
                await plugin.starting();
            }

            await ui.controls.Controls.preload();

            console.log("Workbench: fetching CSS files.");

            await this.preloadCSSFiles(plugins);

            console.log("Workbench: fetching UI icons.");

            await this.preloadIcons(plugins);

            console.log("Workbench: fetching project metadata.");

            await this.preloadFileStorage();

            console.log("Workbench: registering content types.");

            this.registerContentTypes(plugins);

            this.registerContentTypeIcons(plugins);

            console.log("Workbench: fetching required project resources.");

            await this.preloadProjectResources(plugins);

            console.log("Workbench: initializing UI.");

            this.initCommands(plugins);

            this.registerEditors(plugins);

            this.registerWindow(plugins);

            this.initEvents();

            console.log("%cWorkbench: started.", "color:green");

        }

        private async preloadCSSFiles(plugins: Plugin[]) {

            const urls: string[] = [

                "colibri/ui/controls/css/controls.css",
                "colibri/css/workbench.css",
                "colibri/css/dark.css",
                "colibri/css/light.css"

            ];

            const extensions = this.getExtensionRegistry().getExtensions<CSSFileLoaderExtension>(CSSFileLoaderExtension.POINT_ID);

            for (const extension of extensions) {
                urls.push(...extension.getCSSUrls());
            }

            for (const url of urls) {

                try {

                    const resp = await fetch(url);
                    const text = await resp.text();

                    const element = document.createElement("style");
                    element.innerHTML = text;

                    document.head.appendChild(element);

                } catch (e) {
                    console.error(`Workbench: Error fetching CSS url ${url}`);
                    console.error(e.message);
                }
            }
        }

        private registerWindow(plugins: Plugin[]) {

            const windows: ide.WorkbenchWindow[] = [];

            for (const plugin of plugins) {
                plugin.createWindow(windows);
            }

            if (windows.length === 0) {

                alert("No workbench window provided.");

            } else {

                this._activeWindow = windows[0];
                document.body.appendChild(this._activeWindow.getElement());
            }
        }

        private async preloadProjectResources(plugins: Plugin[]) {

            for (const plugin of plugins) {
                await plugin.preloadProjectResources();
            }
        }

        private async preloadIcons(plugins: Plugin[]) {

            await this.getWorkbenchIcon(ICON_FILE).preload();
            await this.getWorkbenchIcon(ICON_FOLDER).preload();

            const extensions = this._extensionRegistry
                .getExtensions<IconLoaderExtension>(IconLoaderExtension.POINT_ID);

            for (const extension of extensions) {

                const icons = extension.getIcons();

                for (const icon of icons) {
                    await icon.preload();
                }
            }

        }

        private registerContentTypeIcons(plugins: Plugin[]) {

            this._contentType_icon_Map = new Map();

            for (const plugin of plugins) {
                plugin.registerContentTypeIcons(this._contentType_icon_Map);
            }

        }

        private initCommands(plugins: Plugin[]) {
            this._commandManager = new commands.CommandManager();

            IDECommands.init();

            for (const plugin of plugins) {
                plugin.registerCommands(this._commandManager);
            }
        }


        private initEvents() {
            window.addEventListener("mousedown", e => {
                this._activeElement = <HTMLElement>e.target;
                const part = this.findPart(<any>e.target);
                this.setActivePart(part);
            });
        }

        private registerEditors(plugins: Plugin[]): void {

            for (const plugin of plugins) {
                plugin.registerEditor(this._editorRegistry);
            }

        }

        getFileStringCache() {
            return this._fileStringCache;
        }

        getCommandManager() {
            return this._commandManager;
        }

        getActiveWindow() {
            return this._activeWindow;
        }

        getActiveElement() {
            return this._activeElement;
        }

        getActivePart() {
            return this._activePart;
        }

        getActiveEditor() {
            return this._activeEditor;
        }

        setActiveEditor(editor: EditorPart) {

            if (editor === this._activeEditor) {
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
                this.setActiveEditor(<EditorPart>part);
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

        private async preloadFileStorage() {

            this._fileStorage = new core.io.FileStorage_HTTPServer();

            this._fileStringCache = new core.io.FileStringCache(this._fileStorage);

            await this._fileStorage.reload();
        }

        private registerContentTypes(plugins: Plugin[]) {

            const reg = new core.ContentTypeRegistry();

            for (const plugin of plugins) {
                plugin.registerContentTypes(reg);
            }

            this._contentTypeRegistry = reg;
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

        getContentTypeRegistry() {
            return this._contentTypeRegistry;
        }

        getExtensionRegistry() {
            return this._extensionRegistry;
        }

        getProjectRoot(): core.io.FilePath {
            return this._fileStorage.getRoot();
        }

        getContentTypeIcon(contentType: string): controls.IImage {

            if (this._contentType_icon_Map.has(contentType)) {

                return this._contentType_icon_Map.get(contentType);

            }

            return null;
        }

        getFileImage(file: core.io.FilePath) {
            return this._fileImageCache.getContent(file);
        }

        getWorkbenchIcon(name: string) {
            return controls.Controls.getIcon(name, "colibri/ui/icons");
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