var phasereditor2d;
(function (phasereditor2d) {
    phasereditor2d.VER = "3.0.0";
    async function main() {
        console.log(`%c %c Phaser Editor 2D %c v${phasereditor2d.VER} %c %c https://phasereditor2d.com `, "background-color:red", "background-color:#3f3f3f;color:whitesmoke", "background-color:orange;color:black", "background-color:red", "background-color:silver");
        console.log("");
        console.log("Preloading UI resources");
        await phasereditor2d.ui.controls.Controls.preload();
        console.log("Starting the workbench");
        const workbench = phasereditor2d.ui.ide.Workbench.getWorkbench();
        workbench.start();
    }
    phasereditor2d.main = main;
})(phasereditor2d || (phasereditor2d = {}));
window.addEventListener("load", phasereditor2d.main);
var phasereditor2d;
(function (phasereditor2d) {
    var core;
    (function (core) {
        class ContentTypeRegistry {
            constructor() {
                this._resolvers = [];
                this._cache = new Map();
            }
            registerResolver(resolver) {
                this._resolvers.push(resolver);
            }
            getCachedContentType(file) {
                const id = file.getId();
                if (this._cache.has(id)) {
                    return this._cache.get(id);
                }
                return core.CONTENT_TYPE_ANY;
            }
            async preload(file) {
                const id = file.getId();
                if (this._cache.has(id)) {
                    return phasereditor2d.ui.controls.Controls.resolveNothingLoaded();
                }
                for (const resolver of this._resolvers) {
                    const ct = await resolver.computeContentType(file);
                    if (ct !== core.CONTENT_TYPE_ANY) {
                        this._cache.set(id, ct);
                        return phasereditor2d.ui.controls.Controls.resolveResourceLoaded();
                    }
                }
                return phasereditor2d.ui.controls.Controls.resolveNothingLoaded();
            }
        }
        core.ContentTypeRegistry = ContentTypeRegistry;
    })(core = phasereditor2d.core || (phasereditor2d.core = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var core;
    (function (core) {
        core.CONTENT_TYPE_ANY = "any";
    })(core = phasereditor2d.core || (phasereditor2d.core = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var core;
    (function (core) {
        var io;
        (function (io) {
            const EMPTY_FILES = [];
            class FilePath {
                constructor(parent, fileData) {
                    this._parent = parent;
                    this._name = fileData.name;
                    this._isFile = fileData.isFile;
                    this._fileSize = fileData.size;
                    this._modTime = fileData.modTime;
                    {
                        const i = this._name.lastIndexOf(".");
                        if (i >= 0) {
                            this._ext = this._name.substring(i + 1);
                        }
                        else {
                            this._ext = "";
                        }
                    }
                    if (fileData.children) {
                        this._files = [];
                        for (let child of fileData.children) {
                            this._files.push(new FilePath(this, child));
                        }
                        this._files.sort((a, b) => {
                            const a1 = a._isFile ? 1 : 0;
                            const b1 = b._isFile ? 1 : 0;
                            return a1 - b1;
                        });
                    }
                    else {
                        this._files = EMPTY_FILES;
                    }
                }
                getExtension() {
                    return this._ext;
                }
                getSize() {
                    return this.isFile() ? this._fileSize : 0;
                }
                getName() {
                    return this._name;
                }
                getId() {
                    if (this._id) {
                        return this._id;
                    }
                    this._id = this.getFullName() + "@" + this._modTime + "@" + this._fileSize;
                    return this._id;
                }
                getFullName() {
                    if (this._parent) {
                        return this._parent.getFullName() + "/" + this._name;
                    }
                    return this._name;
                }
                getUrl() {
                    if (this._parent) {
                        return this._parent.getUrl() + "/" + this._name;
                    }
                    return "../project";
                }
                getSibling(name) {
                    const parent = this.getParent();
                    if (parent) {
                        return parent.getChild(name);
                    }
                    return null;
                }
                getChild(name) {
                    return this.getFiles().find(file => file.getName() === name);
                }
                getParent() {
                    return this._parent;
                }
                isFile() {
                    return this._isFile;
                }
                isFolder() {
                    return !this.isFile();
                }
                getFiles() {
                    return this._files;
                }
                toString() {
                    if (this._parent) {
                        return this._parent.toString() + "/" + this._name;
                    }
                    return this._name;
                }
                toStringTree() {
                    return this.toStringTree2(0);
                }
                toStringTree2(depth) {
                    let s = " ".repeat(depth * 4);
                    s += this.getName() + (this.isFolder() ? "/" : "") + "\n";
                    if (this.isFolder()) {
                        for (let file of this._files) {
                            s += file.toStringTree2(depth + 1);
                        }
                    }
                    return s;
                }
            }
            io.FilePath = FilePath;
        })(io = core.io || (core.io = {}));
    })(core = phasereditor2d.core || (phasereditor2d.core = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var core;
    (function (core) {
        var io;
        (function (io) {
            async function makeApiRequest(method, body) {
                try {
                    const resp = await fetch("../api", {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({
                            "method": method,
                            "body": body
                        })
                    });
                    const json = await resp.json();
                    return json;
                }
                catch (e) {
                    console.error(e);
                    return new Promise((resolve, reject) => {
                        resolve({
                            error: e.message
                        });
                    });
                }
            }
            class ServerFileStorage {
                constructor() {
                    this._fileStringContentMap = new Map();
                }
                getRoot() {
                    return this._root;
                }
                async reload() {
                    this._fileStringContentMap = new Map();
                    const data = await makeApiRequest("GetProjectFiles");
                    //TODO: handle error
                    const self = this;
                    return new Promise(function (resolve, reject) {
                        self._root = new io.FilePath(null, data);
                        resolve(self._root);
                    });
                }
                hasFileStringInCache(file) {
                    return this._fileStringContentMap.has(file.getId());
                }
                getFileStringFromCache(file) {
                    const id = file.getId();
                    if (this._fileStringContentMap.has(id)) {
                        const content = this._fileStringContentMap.get(id);
                        return content;
                    }
                    return null;
                }
                async getFileString(file) {
                    const id = file.getId();
                    if (this._fileStringContentMap.has(id)) {
                        const content = this._fileStringContentMap.get(id);
                        return content;
                    }
                    const data = await makeApiRequest("GetFileString", {
                        path: file.getFullName()
                    });
                    if (data.error) {
                        alert(`Cannot get file content of '${file.getFullName()}'`);
                        return null;
                    }
                    const content = data["content"];
                    this._fileStringContentMap.set(id, content);
                    return content;
                }
                async setFileString(file, content) {
                    const data = await makeApiRequest("SetFileString", {
                        path: file.getFullName(),
                        content: content
                    });
                    if (data.error) {
                        alert(`Cannot set file content to '${file.getFullName()}'`);
                        return false;
                    }
                    this._fileStringContentMap.set(file.getId(), content);
                    return true;
                }
            }
            io.ServerFileStorage = ServerFileStorage;
        })(io = core.io || (core.io = {}));
    })(core = phasereditor2d.core || (phasereditor2d.core = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var core;
    (function (core) {
        var json;
        (function (json) {
            function write(data, name, value, defaultValue) {
                if (value !== defaultValue) {
                    data[name] = value;
                }
            }
            json.write = write;
            function read(data, name, defaultValue) {
                if (name in data) {
                    return data[name];
                }
                return defaultValue;
            }
            json.read = read;
        })(json = core.json || (core.json = {}));
    })(core = phasereditor2d.core || (phasereditor2d.core = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            controls.EVENT_CONTROL_LAYOUT = "controlLayout";
            class Control extends EventTarget {
                constructor(tagName = "div", ...classList) {
                    super();
                    this._bounds = { x: 0, y: 0, width: 0, height: 0 };
                    this._handlePosition = true;
                    this._children = [];
                    this._element = document.createElement(tagName);
                    this._element["__control"] = this;
                    this.addClass("Control", ...classList);
                    this._layout = null;
                    this._container = null;
                    this._scrollY = 0;
                    this._layoutChildren = true;
                }
                static getControlOf(element) {
                    return element["__control"];
                }
                isHandlePosition() {
                    return this._handlePosition;
                }
                setHandlePosition(_handlePosition) {
                    this._handlePosition = _handlePosition;
                }
                get style() {
                    return this.getElement().style;
                }
                isLayoutChildren() {
                    return this._layoutChildren;
                }
                setLayoutChildren(layout) {
                    this._layoutChildren = layout;
                }
                getScrollY() {
                    return this._scrollY;
                }
                setScrollY(scrollY) {
                    this._scrollY = scrollY;
                }
                getContainer() {
                    return this._container;
                }
                getLayout() {
                    return this._layout;
                }
                setLayout(layout) {
                    this._layout = layout;
                    this.layout();
                }
                addClass(...tokens) {
                    this._element.classList.add(...tokens);
                }
                removeClass(...tokens) {
                    this._element.classList.remove(...tokens);
                }
                containsClass(className) {
                    return this._element.classList.contains(className);
                }
                getElement() {
                    return this._element;
                }
                getControlPosition(windowX, windowY) {
                    const b = this.getElement().getBoundingClientRect();
                    return {
                        x: windowX - b.left,
                        y: windowY - b.top
                    };
                }
                containsLocalPoint(x, y) {
                    return x >= 0 && x <= this._bounds.width && y >= 0 && y <= this._bounds.height;
                }
                setBounds(bounds) {
                    this._bounds.x = bounds.x === undefined ? this._bounds.x : bounds.x;
                    this._bounds.y = bounds.y === undefined ? this._bounds.y : bounds.y;
                    this._bounds.width = bounds.width === undefined ? this._bounds.width : bounds.width;
                    this._bounds.height = bounds.height === undefined ? this._bounds.height : bounds.height;
                    this.layout();
                }
                setBoundsValues(x, y, w, h) {
                    this.setBounds({ x: x, y: y, width: w, height: h });
                }
                getBounds() {
                    return this._bounds;
                }
                setLocation(x, y) {
                    this._element.style.left = x + "px";
                    this._element.style.top = y + "px";
                    this._bounds.x = x;
                    this._bounds.y = y;
                }
                layout() {
                    if (this.isHandlePosition()) {
                        controls.setElementBounds(this._element, this._bounds);
                    }
                    else {
                        controls.setElementBounds(this._element, {
                            width: this._bounds.width,
                            height: this._bounds.height
                        });
                    }
                    if (this._layout) {
                        this._layout.layout(this);
                    }
                    else {
                        if (this._layoutChildren) {
                            for (let child of this._children) {
                                child.layout();
                            }
                        }
                    }
                    this.dispatchLayoutEvent();
                }
                dispatchLayoutEvent() {
                    this.dispatchEvent(new CustomEvent(controls.EVENT_CONTROL_LAYOUT));
                }
                add(control) {
                    control._container = this;
                    this._children.push(control);
                    this._element.appendChild(control.getElement());
                    control.onControlAdded();
                }
                onControlAdded() {
                }
                getChildren() {
                    return this._children;
                }
            }
            controls.Control = Control;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../phasereditor2d.ui.controls/Control.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class Window extends ui.controls.Control {
                constructor() {
                    super("div", "Window");
                    this.setLayout(new ui.controls.FillLayout(5));
                    window.addEventListener("resize", e => {
                        this.setBoundsValues(0, 0, window.innerWidth, window.innerHeight);
                    });
                    window.addEventListener(ui.controls.EVENT_THEME_CHANGED, e => this.layout());
                }
                createViewFolder(...parts) {
                    const folder = new ide.ViewFolder();
                    for (const part of parts) {
                        folder.addPart(part);
                    }
                    return folder;
                }
            }
            ide.Window = Window;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../ide/Window.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class DesignWindow extends ide.Window {
                constructor() {
                    super();
                    this._outlineView = new ide.views.outline.OutlineView();
                    this._filesView = new ide.views.files.FilesView();
                    this._inspectorView = new ide.views.inspector.InspectorView();
                    this._blocksView = new ide.views.blocks.BlocksView();
                    this._editorArea = new ide.EditorArea();
                    this._split_Files_Blocks = new ui.controls.SplitPanel(this.createViewFolder(this._filesView), this.createViewFolder(this._blocksView));
                    this._split_Editor_FilesBlocks = new ui.controls.SplitPanel(this._editorArea, this._split_Files_Blocks, false);
                    this._split_Outline_EditorFilesBlocks = new ui.controls.SplitPanel(this.createViewFolder(this._outlineView), this._split_Editor_FilesBlocks);
                    this._split_OutlineEditorFilesBlocks_Inspector = new ui.controls.SplitPanel(this._split_Outline_EditorFilesBlocks, this.createViewFolder(this._inspectorView));
                    this.add(this._split_OutlineEditorFilesBlocks_Inspector);
                    this.initialLayout();
                }
                getEditorArea() {
                    return this._editorArea;
                }
                initialLayout() {
                    const b = { x: 0, y: 0, width: window.innerWidth, height: window.innerHeight };
                    this._split_Files_Blocks.setSplitFactor(0.2);
                    this._split_Editor_FilesBlocks.setSplitFactor(0.6);
                    this._split_Outline_EditorFilesBlocks.setSplitFactor(0.15);
                    this._split_OutlineEditorFilesBlocks_Inspector.setSplitFactor(0.8);
                    this.setBounds(b);
                }
            }
            ide.DesignWindow = DesignWindow;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./Control.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            controls.EVENT_SELECTION_CHANGED = "selectionChanged";
            controls.EVENT_THEME_CHANGED = "themeChanged";
            let PreloadResult;
            (function (PreloadResult) {
                PreloadResult[PreloadResult["NOTHING_LOADED"] = 0] = "NOTHING_LOADED";
                PreloadResult[PreloadResult["RESOURCES_LOADED"] = 1] = "RESOURCES_LOADED";
            })(PreloadResult = controls.PreloadResult || (controls.PreloadResult = {}));
            controls.ICON_CONTROL_TREE_COLLAPSE = "tree-collapse";
            controls.ICON_CONTROL_TREE_EXPAND = "tree-expand";
            controls.ICON_CONTROL_CLOSE = "close";
            controls.ICON_CONTROL_DIRTY = "dirty";
            controls.ICON_SIZE = 16;
            const ICONS = [
                controls.ICON_CONTROL_TREE_COLLAPSE,
                controls.ICON_CONTROL_TREE_EXPAND,
                controls.ICON_CONTROL_CLOSE,
                controls.ICON_CONTROL_DIRTY
            ];
            class Controls {
                static setDragEventImage(e, render) {
                    let canvas = document.getElementById("__drag__canvas");
                    if (!canvas) {
                        canvas = document.createElement("canvas");
                        canvas.setAttribute("id", "__drag__canvas");
                        canvas.style.imageRendering = "crisp-edges";
                        canvas.width = 64;
                        canvas.height = 64;
                        canvas.style.width = canvas.width + "px";
                        canvas.style.height = canvas.height + "px";
                        canvas.style.position = "fixed";
                        canvas.style.left = -100 + "px";
                        document.body.appendChild(canvas);
                    }
                    const ctx = canvas.getContext("2d");
                    ctx.clearRect(0, 0, canvas.width, canvas.height);
                    render(ctx, canvas.width, canvas.height);
                    e.dataTransfer.setDragImage(canvas, 10, 10);
                }
                static getApplicationDragData() {
                    return this._applicationDragData;
                }
                static getApplicationDragDataAndClean() {
                    const data = this._applicationDragData;
                    this._applicationDragData = null;
                    return data;
                }
                static setApplicationDragData(data) {
                    this._applicationDragData = data;
                }
                static resolveAll(list) {
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
                static getImage(url, id) {
                    if (Controls._images.has(id)) {
                        return Controls._images.get(id);
                    }
                    const img = new controls.DefaultImage(new Image(), url);
                    Controls._images.set(id, img);
                    return img;
                }
                static getIcon(name, baseUrl = "phasereditor2d.ui.controls/images") {
                    const url = `${baseUrl}/${controls.ICON_SIZE}/${name}.png`;
                    return Controls.getImage(url, name);
                }
                static createIconElement(icon, overIcon) {
                    const element = document.createElement("canvas");
                    element.width = element.height = controls.ICON_SIZE;
                    element.style.width = element.style.height = controls.ICON_SIZE + "px";
                    const context = element.getContext("2d");
                    context.imageSmoothingEnabled = false;
                    if (overIcon) {
                        element.addEventListener("mouseenter", e => {
                            context.clearRect(0, 0, controls.ICON_SIZE, controls.ICON_SIZE);
                            overIcon.paint(context, 0, 0, controls.ICON_SIZE, controls.ICON_SIZE, false);
                        });
                        element.addEventListener("mouseleave", e => {
                            context.clearRect(0, 0, controls.ICON_SIZE, controls.ICON_SIZE);
                            icon.paint(context, 0, 0, controls.ICON_SIZE, controls.ICON_SIZE, false);
                        });
                    }
                    if (icon) {
                        icon.paint(context, 0, 0, controls.ICON_SIZE, controls.ICON_SIZE, false);
                    }
                    return element;
                }
                static switchTheme() {
                    const classList = document.getElementsByTagName("html")[0].classList;
                    if (classList.contains("light")) {
                        this.theme = this.DARK_THEME;
                        classList.remove("light");
                        classList.add("dark");
                    }
                    else {
                        this.theme = this.LIGHT_THEME;
                        classList.remove("dark");
                        classList.add("light");
                    }
                    window.dispatchEvent(new CustomEvent(controls.EVENT_THEME_CHANGED, { detail: this.theme }));
                }
                static drawRoundedRect(ctx, x, y, w, h, topLeft = 5, topRight = 5, bottomRight = 5, bottomLeft = 5) {
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
            Controls._images = new Map();
            Controls._applicationDragData = null;
            Controls.LIGHT_THEME = {
                //treeItemSelectionBackground: "#4242ff",
                treeItemSelectionBackground: "#525252",
                treeItemSelectionForeground: "#f0f0f0",
                treeItemForeground: "#000000"
            };
            Controls.DARK_THEME = {
                treeItemSelectionBackground: "#f0a050",
                treeItemSelectionForeground: "#0e0e0e",
                treeItemForeground: "#f0f0f0"
            };
            Controls.theme = Controls.DARK_THEME;
            controls.Controls = Controls;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            ide.EVENT_PART_TITLE_UPDATED = "partTitledUpdated";
            class Part extends ui.controls.Control {
                constructor(id) {
                    super();
                    this._id = id;
                    this._title = "";
                    this._selection = [];
                    this._partCreated = false;
                    this._undoManager = new ide.undo.UndoManager();
                    this.getElement().setAttribute("id", id);
                    this.getElement().classList.add("Part");
                    this.getElement()["__part"] = this;
                }
                getUndoManager() {
                    return this._undoManager;
                }
                getPartFolder() {
                    return this._folder;
                }
                setPartFolder(folder) {
                    this._folder = folder;
                }
                getTitle() {
                    return this._title;
                }
                setTitle(title) {
                    this._title = title;
                    this.dispatchTitleUpdatedEvent();
                }
                setIcon(icon) {
                    this._icon = icon;
                    this.dispatchTitleUpdatedEvent();
                }
                dispatchTitleUpdatedEvent() {
                    this.dispatchEvent(new CustomEvent(ide.EVENT_PART_TITLE_UPDATED, { detail: this }));
                }
                getIcon() {
                    return this._icon;
                }
                getId() {
                    return this._id;
                }
                setSelection(selection, notify = true) {
                    this._selection = selection;
                    window["SELECTION"] = selection;
                    if (notify) {
                        this.dispatchEvent(new CustomEvent(ui.controls.EVENT_SELECTION_CHANGED, {
                            detail: selection
                        }));
                    }
                }
                getSelection() {
                    return this._selection;
                }
                getPropertyProvider() {
                    return null;
                }
                layout() {
                }
                onPartClosed() {
                    return true;
                }
                onPartShown() {
                    if (!this._partCreated) {
                        this._partCreated = true;
                        this.createPart();
                    }
                }
            }
            ide.Part = Part;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class EditorPart extends ide.Part {
                constructor(id) {
                    super(id);
                    this.addClass("EditorPart");
                    this._dirty = false;
                }
                setDirty(dirty) {
                    this._dirty = dirty;
                    const folder = this.getPartFolder();
                    const label = folder.getLabelFromContent(this);
                    const iconClose = ui.controls.Controls.getIcon(ui.controls.ICON_CONTROL_CLOSE);
                    const iconDirty = dirty ? ui.controls.Controls.getIcon(ui.controls.ICON_CONTROL_DIRTY) : iconClose;
                    folder.setTabCloseIcons(label, iconDirty, iconClose);
                }
                isDirty() {
                    return this._dirty;
                }
                save() {
                }
                onPartClosed() {
                    if (this.isDirty()) {
                        return confirm("This editor is not saved, do you want to close it?");
                    }
                    return true;
                }
                getInput() {
                    return this._input;
                }
                setInput(input) {
                    this._input = input;
                }
                getEditorViewerProvider(key) {
                    return null;
                }
            }
            ide.EditorPart = EditorPart;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            controls.EVENT_TAB_CLOSED = "tabClosed";
            controls.EVENT_TAB_SELECTED = "tabSelected";
            class CloseIconManager {
                constructor() {
                    this._element = document.createElement("canvas");
                    this._element.classList.add("closeIcon");
                    this._element.width = controls.ICON_SIZE;
                    this._element.height = controls.ICON_SIZE;
                    this._element.style.width = controls.ICON_SIZE + "px";
                    this._element.style.height = controls.ICON_SIZE + "px";
                    this._context = this._element.getContext("2d");
                    this._element.addEventListener("mouseenter", e => {
                        this.paint(this._overIcon);
                    });
                    this._element.addEventListener("mouseleave", e => {
                        this.paint(this._icon);
                    });
                }
                setIcon(icon) {
                    this._icon = icon;
                }
                setOverIcon(icon) {
                    this._overIcon = icon;
                }
                getElement() {
                    return this._element;
                }
                repaint() {
                    this.paint(this._icon);
                }
                paint(icon) {
                    if (icon) {
                        this._context.clearRect(0, 0, controls.ICON_SIZE, controls.ICON_SIZE);
                        icon.paint(this._context, 0, 0, controls.ICON_SIZE, controls.ICON_SIZE, true);
                    }
                }
            }
            class TabPane extends controls.Control {
                constructor(...classList) {
                    super("div", "TabPane", ...classList);
                    this._selectionHistoryLabelElement = [];
                    this._titleBarElement = document.createElement("div");
                    this._titleBarElement.classList.add("TabPaneTitleBar");
                    this.getElement().appendChild(this._titleBarElement);
                    this._contentAreaElement = document.createElement("div");
                    this._contentAreaElement.classList.add("TabPaneContentArea");
                    this.getElement().appendChild(this._contentAreaElement);
                }
                addTab(label, icon, content, closeable = false) {
                    const labelElement = this.makeLabel(label, icon, closeable);
                    this._titleBarElement.appendChild(labelElement);
                    labelElement.addEventListener("mousedown", e => this.selectTab(labelElement));
                    const contentArea = new controls.Control("div", "ContentArea");
                    contentArea.add(content);
                    this._contentAreaElement.appendChild(contentArea.getElement());
                    labelElement["__contentArea"] = contentArea.getElement();
                    if (this._titleBarElement.childElementCount === 1) {
                        this.selectTab(labelElement);
                    }
                }
                makeLabel(label, icon, closeable) {
                    const labelElement = document.createElement("div");
                    labelElement.classList.add("TabPaneLabel");
                    const tabIconElement = controls.Controls.createIconElement(icon);
                    labelElement.appendChild(tabIconElement);
                    const textElement = document.createElement("span");
                    textElement.innerHTML = label;
                    labelElement.appendChild(textElement);
                    if (closeable) {
                        const manager = new CloseIconManager();
                        manager.setIcon(controls.Controls.getIcon(controls.ICON_CONTROL_CLOSE));
                        manager.repaint();
                        labelElement.appendChild(manager.getElement());
                        labelElement.classList.add("closeable");
                        labelElement["__CloseIconManager"] = manager;
                        manager.getElement().addEventListener("click", e => {
                            e.stopImmediatePropagation();
                            this.closeTabLabel(labelElement);
                        });
                    }
                    return labelElement;
                }
                setTabCloseIcons(labelElement, icon, overIcon) {
                    const manager = labelElement["__CloseIconManager"];
                    if (manager) {
                        manager.setIcon(icon);
                        manager.setOverIcon(overIcon);
                        manager.repaint();
                    }
                }
                closeTab(content) {
                    const label = this.getLabelFromContent(content);
                    if (label) {
                        this.closeTabLabel(label);
                    }
                }
                closeTabLabel(labelElement) {
                    {
                        const content = TabPane.getContentFromLabel(labelElement);
                        const event = new CustomEvent(controls.EVENT_TAB_CLOSED, {
                            detail: content,
                            cancelable: true
                        });
                        if (!this.dispatchEvent(event)) {
                            return;
                        }
                    }
                    this._titleBarElement.removeChild(labelElement);
                    const contentArea = labelElement["__contentArea"];
                    this._contentAreaElement.removeChild(contentArea);
                    let toSelectLabel = null;
                    const selectedLabel = this.getSelectedLabelElement();
                    if (selectedLabel === labelElement) {
                        this._selectionHistoryLabelElement.pop();
                        const nextInHistory = this._selectionHistoryLabelElement.pop();
                        ;
                        if (nextInHistory) {
                            toSelectLabel = nextInHistory;
                        }
                        else {
                            if (this._titleBarElement.childElementCount > 0) {
                                toSelectLabel = this._titleBarElement.firstChild;
                            }
                        }
                    }
                    if (toSelectLabel) {
                        this.selectTab(toSelectLabel);
                    }
                }
                setTabTitle(content, title, icon) {
                    for (let i = 0; i < this._titleBarElement.childElementCount; i++) {
                        const label = this._titleBarElement.children.item(i);
                        const content2 = TabPane.getContentFromLabel(label);
                        if (content2 === content) {
                            const iconElement = label.firstChild;
                            const textElement = iconElement.nextSibling;
                            if (icon) {
                                const context = iconElement.getContext("2d");
                                context.clearRect(0, 0, iconElement.width, iconElement.height);
                                icon.paint(context, 0, 0, iconElement.width, iconElement.height, false);
                            }
                            textElement.innerHTML = title;
                        }
                    }
                }
                static isTabLabel(element) {
                    return element.classList.contains("TabPaneLabel");
                }
                getLabelFromContent(content) {
                    for (let i = 0; i < this._titleBarElement.childElementCount; i++) {
                        const label = this._titleBarElement.children.item(i);
                        const content2 = TabPane.getContentFromLabel(label);
                        if (content2 === content) {
                            return label;
                        }
                    }
                    return null;
                }
                static getContentAreaFromLabel(labelElement) {
                    return labelElement["__contentArea"];
                }
                static getContentFromLabel(labelElement) {
                    return controls.Control.getControlOf(this.getContentAreaFromLabel(labelElement).firstChild);
                }
                selectTabWithContent(content) {
                    const label = this.getLabelFromContent(content);
                    if (label) {
                        this.selectTab(label);
                    }
                }
                selectTab(toSelectLabel) {
                    const selectedLabel = this._selectionHistoryLabelElement.pop();
                    if (selectedLabel) {
                        if (selectedLabel === toSelectLabel) {
                            this._selectionHistoryLabelElement.push(selectedLabel);
                            return;
                        }
                        selectedLabel.classList.remove("selected");
                        const selectedContentArea = TabPane.getContentAreaFromLabel(selectedLabel);
                        selectedContentArea.classList.remove("selected");
                    }
                    toSelectLabel.classList.add("selected");
                    const toSelectContentArea = TabPane.getContentAreaFromLabel(toSelectLabel);
                    toSelectContentArea.classList.add("selected");
                    this._selectionHistoryLabelElement.push(toSelectLabel);
                    this.dispatchEvent(new CustomEvent(controls.EVENT_TAB_SELECTED, {
                        detail: TabPane.getContentFromLabel(toSelectLabel)
                    }));
                    this.dispatchLayoutEvent();
                }
                getSelectedTabContent() {
                    const label = this.getSelectedLabelElement();
                    if (label) {
                        const area = TabPane.getContentAreaFromLabel(label);
                        return controls.Control.getControlOf(area.firstChild);
                    }
                    return null;
                }
                getContentList() {
                    const list = [];
                    for (let i = 0; i < this._titleBarElement.children.length; i++) {
                        const label = this._titleBarElement.children.item(i);
                        const content = TabPane.getContentFromLabel(label);
                        list.push(content);
                    }
                    return list;
                }
                getSelectedLabelElement() {
                    return this._selectionHistoryLabelElement.length > 0 ?
                        this._selectionHistoryLabelElement[this._selectionHistoryLabelElement.length - 1]
                        : null;
                }
            }
            controls.TabPane = TabPane;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../phasereditor2d.ui.controls/TabPane.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class PartFolder extends ui.controls.TabPane {
                constructor(...classList) {
                    super("PartsTabPane", ...classList);
                    this.addEventListener(ui.controls.EVENT_CONTROL_LAYOUT, (e) => {
                        const content = this.getSelectedTabContent();
                        if (content) {
                            content.layout();
                        }
                    });
                    this.addEventListener(ui.controls.EVENT_TAB_CLOSED, (e) => {
                        const part = e.detail;
                        if (part.onPartClosed()) {
                            if (this.getContentList().length === 1) {
                                ide.Workbench.getWorkbench().setActivePart(null);
                                if (this instanceof ide.EditorArea) {
                                    ide.Workbench.getWorkbench().setActiveEditor(null);
                                }
                            }
                        }
                        else {
                            e.preventDefault();
                        }
                    });
                    this.addEventListener(ui.controls.EVENT_TAB_SELECTED, (e) => {
                        const part = e.detail;
                        ide.Workbench.getWorkbench().setActivePart(part);
                        part.onPartShown();
                    });
                }
                addPart(part, closeable = false) {
                    part.addEventListener(ide.EVENT_PART_TITLE_UPDATED, (e) => {
                        this.setTabTitle(part, part.getTitle(), part.getIcon());
                    });
                    this.addTab(part.getTitle(), part.getIcon(), part, closeable);
                    part.setPartFolder(this);
                }
            }
            ide.PartFolder = PartFolder;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./Part.ts"/>
/// <reference path="./EditorPart.ts"/>
/// <reference path="./PartFolder.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/TabPane.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class EditorArea extends ide.PartFolder {
                constructor() {
                    super("EditorArea");
                }
                activateEditor(editor) {
                    super.selectTabWithContent(editor);
                }
            }
            ide.EditorArea = EditorArea;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class EditorFactory {
                constructor(id) {
                    this._id = id;
                }
                getId() {
                    return this._id;
                }
            }
            ide.EditorFactory = EditorFactory;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class EditorRegistry {
                constructor() {
                    this._map = new Map();
                }
                registerFactory(factory) {
                    this._map.set(factory.getId(), factory);
                }
                getFactoryForInput(input) {
                    for (const factory of this._map.values()) {
                        if (factory.acceptInput(input)) {
                            return factory;
                        }
                    }
                    return null;
                }
            }
            ide.EditorRegistry = EditorRegistry;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class EditorViewerProvider {
                constructor() {
                    this._viewer = null;
                    this._initialSelection = null;
                }
                setViewer(viewer) {
                    this._viewer = viewer;
                    if (this._initialSelection) {
                        this.setSelection(this._initialSelection, true, true);
                        this._initialSelection = null;
                    }
                }
                setSelection(selection, reveal, notify) {
                    if (this._viewer) {
                        this._viewer.setSelection(selection, notify);
                        this._viewer.reveal(...selection);
                    }
                    else {
                        this._initialSelection = selection;
                    }
                }
                onViewerSelectionChanged(selection) {
                }
                repaint() {
                    if (this._viewer) {
                        this._viewer.repaint();
                    }
                }
            }
            ide.EditorViewerProvider = EditorViewerProvider;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                class FilterControl extends controls.Control {
                    constructor() {
                        super("div", "FilterControl");
                        this.setLayoutChildren(false);
                        this._filterElement = document.createElement("input");
                        this.getElement().appendChild(this._filterElement);
                    }
                    getFilterElement() {
                        return this._filterElement;
                    }
                }
                class ViewerContainer extends controls.Control {
                    constructor(viewer) {
                        super("div", "ViewerContainer");
                        this._viewer = viewer;
                        this.add(viewer);
                        setTimeout(() => this.layout(), 1);
                    }
                    getViewer() {
                        return this._viewer;
                    }
                    layout() {
                        const b = this.getElement().getBoundingClientRect();
                        this._viewer.setBoundsValues(b.left, b.top, b.width, b.height);
                    }
                }
                viewers.ViewerContainer = ViewerContainer;
                class FilteredViewer extends controls.Control {
                    constructor(viewer, ...classList) {
                        super("div", "FilteredViewer", ...classList);
                        this._viewer = viewer;
                        this._filterControl = new FilterControl();
                        this._filterControl.getFilterElement().addEventListener("input", e => this.onFilterInput(e));
                        this.add(this._filterControl);
                        this._viewerContainer = new ViewerContainer(this._viewer);
                        this._scrollPane = new controls.ScrollPane(this._viewerContainer);
                        this.add(this._scrollPane);
                        this.setLayoutChildren(false);
                    }
                    onFilterInput(e) {
                        const value = this._filterControl.getFilterElement().value;
                        this._viewer.setFilterText(value);
                    }
                    getViewer() {
                        return this._viewer;
                    }
                    layout() {
                        this._viewerContainer.layout();
                        this._scrollPane.layout();
                    }
                    __layout() {
                        super.layout();
                        const b = this.getBounds();
                        this._filterControl.setBoundsValues(0, 0, b.width, controls.FILTERED_VIEWER_FILTER_HEIGHT);
                        this._scrollPane.setBounds({
                            x: 0,
                            y: controls.FILTERED_VIEWER_FILTER_HEIGHT,
                            width: b.width,
                            height: b.height - controls.FILTERED_VIEWER_FILTER_HEIGHT
                        });
                    }
                }
                viewers.FilteredViewer = FilteredViewer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class ViewPart extends ide.Part {
                constructor(id) {
                    super(id);
                    this.addClass("View");
                }
            }
            ide.ViewPart = ViewPart;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../phasereditor2d.ui.controls/viewers/FilteredViewer.ts" />
/// <reference path="./ViewPart.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class ViewerView extends ide.ViewPart {
                constructor(id) {
                    super(id);
                }
                createPart() {
                    this._viewer = this.createViewer();
                    this.addClass("ViewerView");
                    this._filteredViewer = new ui.controls.viewers.FilteredViewer(this._viewer);
                    this.add(this._filteredViewer);
                    this._viewer.addEventListener(ui.controls.EVENT_SELECTION_CHANGED, (e) => {
                        this.setSelection(e.detail);
                    });
                }
                layout() {
                    if (this._filteredViewer) {
                        this._filteredViewer.layout();
                    }
                }
            }
            ide.ViewerView = ViewerView;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./ViewerView.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var viewers = ui.controls.viewers;
            class EditorViewerView extends ide.ViewerView {
                constructor(id) {
                    super(id);
                    this._viewerMap = new Map();
                }
                createViewer() {
                    const viewer = new viewers.TreeViewer();
                    viewer.addEventListener(ui.controls.EVENT_SELECTION_CHANGED, e => {
                        if (this._currentViewerProvider) {
                            this._currentViewerProvider.onViewerSelectionChanged(this._viewer.getSelection());
                        }
                    });
                    return viewer;
                }
                createPart() {
                    super.createPart();
                    ide.Workbench.getWorkbench().addEventListener(ide.EVENT_EDITOR_ACTIVATED, e => this.onWorkbenchEditorActivated());
                }
                async onWorkbenchEditorActivated() {
                    if (this._currentEditor !== null) {
                        const state = this._viewer.getState();
                        this._viewerMap.set(this._currentEditor, state);
                    }
                    const editor = ide.Workbench.getWorkbench().getActiveEditor();
                    let provider = null;
                    if (editor) {
                        if (editor === this._currentEditor) {
                            provider = this._currentViewerProvider;
                        }
                        else {
                            provider = this.getViewerProvider(editor);
                        }
                    }
                    if (provider) {
                        provider.setViewer(this._viewer);
                        await provider.preload();
                        this._viewer.setTreeRenderer(provider.getTreeViewerRenderer(this._viewer));
                        this._viewer.setLabelProvider(provider.getLabelProvider());
                        this._viewer.setCellRendererProvider(provider.getCellRendererProvider());
                        this._viewer.setContentProvider(provider.getContentProvider());
                        this._viewer.setInput(provider.getInput());
                        const state = this._viewerMap.get(editor);
                        if (state) {
                            this._viewer.setState(state);
                        }
                    }
                    else {
                        this._viewer.setInput(null);
                        this._viewer.setContentProvider(new ui.controls.viewers.EmptyTreeContentProvider());
                    }
                    this._currentViewerProvider = provider;
                    this._currentEditor = editor;
                    this._viewer.repaint();
                }
                getPropertyProvider() {
                    if (this._currentViewerProvider) {
                        return this._currentViewerProvider.getPropertySectionProvider();
                    }
                    return null;
                }
            }
            ide.EditorViewerView = EditorViewerView;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class FileEditor extends ide.EditorPart {
                constructor(id) {
                    super(id);
                }
                setInput(file) {
                    super.setInput(file);
                    this.setTitle(file.getName());
                }
                getInput() {
                    return super.getInput();
                }
                getIcon() {
                    const file = this.getInput();
                    if (!file) {
                        return ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FILE);
                    }
                    const wb = ide.Workbench.getWorkbench();
                    const ct = wb.getContentTypeRegistry().getCachedContentType(file);
                    const icon = wb.getContentTypeIcon(ct);
                    return icon;
                }
            }
            ide.FileEditor = FileEditor;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class FileUtils {
                static getImage(file) {
                    return ide.Workbench.getWorkbench().getFileImage(file);
                }
                static getFileStringFromCache(file) {
                    return ide.Workbench.getWorkbench().getFileStorage().getFileStringFromCache(file);
                }
                static getFileString(file) {
                    return ide.Workbench.getWorkbench().getFileStorage().getFileString(file);
                }
                static setFileString(file, content) {
                    return ide.Workbench.getWorkbench().getFileStorage().setFileString(file, content);
                }
                static async preloadFileString(file) {
                    const storage = ide.Workbench.getWorkbench().getFileStorage();
                    if (storage.hasFileStringInCache(file)) {
                        return ui.controls.Controls.resolveNothingLoaded();
                    }
                    await storage.getFileString(file);
                    return ui.controls.Controls.resolveResourceLoaded();
                }
                static getFileFromPath(path) {
                    const root = ide.Workbench.getWorkbench().getFileStorage().getRoot();
                    const names = path.split("/");
                    let result = root;
                    for (const name of names) {
                        const child = result.getFiles().find(file => file.getName() === name);
                        if (child) {
                            result = child;
                        }
                        else {
                            return null;
                        }
                    }
                    return result;
                }
                static async getFilesWithContentType(contentType) {
                    const reg = ide.Workbench.getWorkbench().getContentTypeRegistry();
                    const files = this.getAllFiles();
                    for (const file of files) {
                        await reg.preload(file);
                    }
                    return files.filter(file => reg.getCachedContentType(file) === contentType);
                }
                static getAllFiles() {
                    const files = [];
                    this.getAllFiles2(ide.Workbench.getWorkbench().getFileStorage().getRoot(), files);
                    return files;
                }
                static getAllFiles2(folder, files) {
                    for (const file of folder.getFiles()) {
                        if (file.isFolder()) {
                            this.getAllFiles2(file, files);
                        }
                        else {
                            files.push(file);
                        }
                    }
                }
            }
            ide.FileUtils = FileUtils;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            ide.CMD_SAVE = "save";
            ide.CMD_DELETE = "delete";
            ide.CMD_RENAME = "rename";
            ide.CMD_UNDO = "undo";
            ide.CMD_REDO = "redo";
            class IDECommands {
                static init() {
                    const manager = ide.Workbench.getWorkbench().getCommandManager();
                    // register commands
                    manager.addCommandHelper(ide.CMD_SAVE);
                    manager.addCommandHelper(ide.CMD_DELETE);
                    manager.addCommandHelper(ide.CMD_RENAME);
                    manager.addCommandHelper(ide.CMD_UNDO);
                    manager.addCommandHelper(ide.CMD_REDO);
                    // register handlers
                    manager.addHandlerHelper(ide.CMD_SAVE, args => args.activeEditor && args.activeEditor.isDirty(), args => {
                        args.activeEditor.save();
                    });
                    manager.addHandlerHelper(ide.CMD_UNDO, args => args.activePart !== null, args => {
                        args.activePart.getUndoManager().undo();
                    });
                    manager.addHandlerHelper(ide.CMD_REDO, args => args.activePart !== null, args => {
                        args.activePart.getUndoManager().redo();
                    });
                    // register bindings
                    manager.addKeyBinding(ide.CMD_SAVE, new ide.commands.KeyMatcher({
                        control: true,
                        key: "s"
                    }));
                    manager.addKeyBinding(ide.CMD_DELETE, new ide.commands.KeyMatcher({
                        key: "delete"
                    }));
                    manager.addKeyBinding(ide.CMD_RENAME, new ide.commands.KeyMatcher({
                        key: "f2"
                    }));
                    manager.addKeyBinding(ide.CMD_UNDO, new ide.commands.KeyMatcher({
                        control: true,
                        key: "z"
                    }));
                    manager.addKeyBinding(ide.CMD_REDO, new ide.commands.KeyMatcher({
                        control: true,
                        shift: true,
                        key: "z"
                    }));
                }
            }
            ide.IDECommands = IDECommands;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            ide.CONTENT_TYPE_IMAGE = "image";
            ide.CONTENT_TYPE_AUDIO = "audio";
            ide.CONTENT_TYPE_VIDEO = "video";
            ide.CONTENT_TYPE_SCRIPT = "script";
            ide.CONTENT_TYPE_TEXT = "text";
            class ExtensionContentTypeResolver {
                constructor(defs) {
                    this._map = new Map();
                    for (const def of defs) {
                        this._map.set(def[0].toUpperCase(), def[1]);
                    }
                }
                computeContentType(file) {
                    const ext = file.getExtension().toUpperCase();
                    if (this._map.has(ext)) {
                        return Promise.resolve(this._map.get(ext));
                    }
                    return Promise.resolve(phasereditor2d.core.CONTENT_TYPE_ANY);
                }
            }
            ide.ExtensionContentTypeResolver = ExtensionContentTypeResolver;
            class DefaultExtensionTypeResolver extends ExtensionContentTypeResolver {
                constructor() {
                    super([
                        ["png", ide.CONTENT_TYPE_IMAGE],
                        ["jpg", ide.CONTENT_TYPE_IMAGE],
                        ["bmp", ide.CONTENT_TYPE_IMAGE],
                        ["gif", ide.CONTENT_TYPE_IMAGE],
                        ["webp", ide.CONTENT_TYPE_IMAGE],
                        ["mp3", ide.CONTENT_TYPE_AUDIO],
                        ["wav", ide.CONTENT_TYPE_AUDIO],
                        ["ogg", ide.CONTENT_TYPE_AUDIO],
                        ["mp4", ide.CONTENT_TYPE_VIDEO],
                        ["ogv", ide.CONTENT_TYPE_VIDEO],
                        ["mp4", ide.CONTENT_TYPE_VIDEO],
                        ["webm", ide.CONTENT_TYPE_VIDEO],
                        ["js", ide.CONTENT_TYPE_SCRIPT],
                        ["html", ide.CONTENT_TYPE_SCRIPT],
                        ["css", ide.CONTENT_TYPE_SCRIPT],
                        ["ts", ide.CONTENT_TYPE_SCRIPT],
                        ["json", ide.CONTENT_TYPE_SCRIPT],
                        ["txt", ide.CONTENT_TYPE_TEXT],
                        ["md", ide.CONTENT_TYPE_TEXT],
                    ]);
                }
            }
            ide.DefaultExtensionTypeResolver = DefaultExtensionTypeResolver;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class OutlineProvider extends EventTarget {
                constructor(editor) {
                    super();
                    this._editor = editor;
                }
            }
            ide.OutlineProvider = OutlineProvider;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var toolbar;
        (function (toolbar) {
            class Toolbar {
                constructor() {
                    this._toolbarElement = document.createElement("div");
                    this._toolbarElement.innerHTML = `

            <button>Load</button>
            <button>Play</button>

            `;
                    this._toolbarElement.classList.add("toolbar");
                    document.getElementsByTagName("body")[0].appendChild(this._toolbarElement);
                }
                getElement() {
                    return this._toolbarElement;
                }
            }
            toolbar.Toolbar = Toolbar;
        })(toolbar = ui.toolbar || (ui.toolbar = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            class ViewFolder extends ide.PartFolder {
                constructor(...classList) {
                    super("ViewFolder", ...classList);
                }
            }
            ide.ViewFolder = ViewFolder;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../FileEditor.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var image;
                (function (image) {
                    class ImageEditorFactory extends ide.EditorFactory {
                        constructor() {
                            super("phasereditor2d.ImageEditorFactory");
                        }
                        acceptInput(input) {
                            if (input instanceof phasereditor2d.core.io.FilePath) {
                                const file = input;
                                const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);
                                if (contentType === ide.CONTENT_TYPE_IMAGE) {
                                    return true;
                                }
                            }
                            return false;
                        }
                        createEditor() {
                            return new ImageEditor();
                        }
                    }
                    class ImageEditor extends ide.FileEditor {
                        constructor() {
                            super("phasereditor2d.ImageEditor");
                            this.addClass("ImageEditor");
                        }
                        static getFactory() {
                            return new ImageEditorFactory();
                        }
                        async createPart() {
                            this._imageControl = new ui.controls.ImageControl();
                            const container = document.createElement("div");
                            container.classList.add("ImageEditorContainer");
                            container.appendChild(this._imageControl.getElement());
                            this.getElement().appendChild(container);
                            this.updateImage();
                        }
                        async updateImage() {
                            const file = this.getInput();
                            if (!file) {
                                return;
                            }
                            const img = ide.Workbench.getWorkbench().getFileImage(file);
                            this._imageControl.setImage(img);
                            this._imageControl.repaint();
                            const result = await img.preload();
                            if (result === ui.controls.PreloadResult.RESOURCES_LOADED) {
                                this._imageControl.repaint();
                            }
                            this.dispatchTitleUpdatedEvent();
                        }
                        getIcon() {
                            const file = this.getInput();
                            if (!file) {
                                return super.getIcon();
                            }
                            const img = ide.Workbench.getWorkbench().getFileImage(file);
                            return img;
                        }
                        layout() {
                            if (this._imageControl) {
                                this._imageControl.resizeTo();
                            }
                        }
                        setInput(input) {
                            super.setInput(input);
                            if (this._imageControl) {
                                this.updateImage();
                            }
                        }
                    }
                    image.ImageEditor = ImageEditor;
                })(image = editors.image || (editors.image = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../ide/ViewPart.ts"/>
/// <reference path="../ide/DesignWindow.ts"/>
/// <reference path="../../core/io/FileStorage.ts"/>
/// <reference path="./editors/image/ImageEditor.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            ide.EVENT_PART_DEACTIVATED = "partDeactivated";
            ide.EVENT_PART_ACTIVATED = "partActivated";
            ide.EVENT_EDITOR_DEACTIVATED = "editorDeactivated";
            ide.EVENT_EDITOR_ACTIVATED = "editorActivated";
            ide.ICON_FILE = "file";
            ide.ICON_FOLDER = "folder";
            ide.ICON_FILE_FONT = "file-font";
            ide.ICON_FILE_IMAGE = "file-image";
            ide.ICON_FILE_VIDEO = "file-movie";
            ide.ICON_FILE_SCRIPT = "file-script";
            ide.ICON_FILE_SOUND = "file-sound";
            ide.ICON_FILE_TEXT = "file-text";
            ide.ICON_ASSET_PACK = "asset-pack";
            ide.ICON_OUTLINE = "outline";
            ide.ICON_INSPECTOR = "inspector";
            ide.ICON_BLOCKS = "blocks";
            const ICONS = [
                ide.ICON_FILE,
                ide.ICON_FOLDER,
                ide.ICON_FILE_FONT,
                ide.ICON_FILE_IMAGE,
                ide.ICON_FILE_VIDEO,
                ide.ICON_FILE_SCRIPT,
                ide.ICON_FILE_SOUND,
                ide.ICON_FILE_TEXT,
                ide.ICON_ASSET_PACK,
                ide.ICON_OUTLINE,
                ide.ICON_INSPECTOR,
                ide.ICON_BLOCKS
            ];
            class Workbench extends EventTarget {
                constructor() {
                    super();
                    this._contentType_icon_Map = new Map();
                    this._contentType_icon_Map.set(ide.CONTENT_TYPE_IMAGE, this.getWorkbenchIcon(ide.ICON_FILE_IMAGE));
                    this._contentType_icon_Map.set(ide.CONTENT_TYPE_AUDIO, this.getWorkbenchIcon(ide.ICON_FILE_SOUND));
                    this._contentType_icon_Map.set(ide.CONTENT_TYPE_VIDEO, this.getWorkbenchIcon(ide.ICON_FILE_VIDEO));
                    this._contentType_icon_Map.set(ide.CONTENT_TYPE_SCRIPT, this.getWorkbenchIcon(ide.ICON_FILE_SCRIPT));
                    this._contentType_icon_Map.set(ide.CONTENT_TYPE_TEXT, this.getWorkbenchIcon(ide.ICON_FILE_TEXT));
                    this._contentType_icon_Map.set(ide.editors.pack.CONTENT_TYPE_ASSET_PACK, this.getWorkbenchIcon(ide.ICON_ASSET_PACK));
                    this._editorRegistry = new ide.EditorRegistry();
                    this._activePart = null;
                    this._activeEditor = null;
                }
                static getWorkbench() {
                    if (!Workbench._workbench) {
                        Workbench._workbench = new Workbench();
                    }
                    return this._workbench;
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
                async preloadIcons() {
                    return Promise.all(ICONS.map(icon => this.getWorkbenchIcon(icon).preload()));
                }
                initCommands() {
                    this._commandManager = new ide.commands.CommandManager();
                    ide.IDECommands.init();
                    ide.editors.scene.SceneEditorCommands.init();
                }
                getCommandManager() {
                    return this._commandManager;
                }
                initEditors() {
                    this._editorRegistry.registerFactory(ide.editors.image.ImageEditor.getFactory());
                    this._editorRegistry.registerFactory(ide.editors.pack.AssetPackEditor.getFactory());
                    this._editorRegistry.registerFactory(ide.editors.scene.SceneEditor.getFactory());
                }
                getDesignWindow() {
                    return this._designWindow;
                }
                getActiveWindow() {
                    return this.getDesignWindow();
                }
                initEvents() {
                    window.addEventListener("mousedown", e => {
                        const part = this.findPart(e.target);
                        this.setActivePart(part);
                    });
                }
                getActivePart() {
                    return this._activePart;
                }
                getActiveEditor() {
                    return this._activeEditor;
                }
                setActiveEditor(editor) {
                    if (editor === this._activeEditor) {
                        return;
                    }
                    this._activeEditor = editor;
                    this.dispatchEvent(new CustomEvent(ide.EVENT_EDITOR_ACTIVATED, { detail: editor }));
                }
                /**
                 * Users may not call this method. This is public only for convenience.
                 */
                setActivePart(part) {
                    if (part !== this._activePart) {
                        const old = this._activePart;
                        this._activePart = part;
                        if (old) {
                            this.toggleActivePartClass(old);
                            this.dispatchEvent(new CustomEvent(ide.EVENT_PART_DEACTIVATED, { detail: old }));
                        }
                        if (part) {
                            this.toggleActivePartClass(part);
                        }
                        this.dispatchEvent(new CustomEvent(ide.EVENT_PART_ACTIVATED, { detail: part }));
                    }
                    if (part instanceof ide.EditorPart) {
                        this.setActiveEditor(part);
                    }
                }
                toggleActivePartClass(part) {
                    const tabPane = this.findTabPane(part.getElement());
                    if (!tabPane) {
                        // maybe the clicked part was closed
                        return;
                    }
                    if (part.containsClass("activePart")) {
                        part.removeClass("activePart");
                        tabPane.removeClass("activePart");
                    }
                    else {
                        part.addClass("activePart");
                        tabPane.addClass("activePart");
                    }
                }
                findTabPane(element) {
                    if (element) {
                        const control = ui.controls.Control.getControlOf(element);
                        if (control && control instanceof ui.controls.TabPane) {
                            return control;
                        }
                        return this.findTabPane(element.parentElement);
                    }
                    return null;
                }
                findPart(element) {
                    if (ui.controls.TabPane.isTabLabel(element)) {
                        element = ui.controls.TabPane.getContentFromLabel(element).getElement();
                    }
                    if (element["__part"]) {
                        return element["__part"];
                    }
                    const control = ui.controls.Control.getControlOf(element);
                    if (control && control instanceof ui.controls.TabPane) {
                        const tabPane = control;
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
                async initFileStorage() {
                    this._fileStorage = new phasereditor2d.core.io.ServerFileStorage();
                    await this._fileStorage.reload();
                }
                async initContentTypes() {
                    const reg = new phasereditor2d.core.ContentTypeRegistry();
                    reg.registerResolver(new ide.editors.pack.AssetPackContentTypeResolver());
                    reg.registerResolver(new ide.editors.scene.SceneContentTypeResolver());
                    reg.registerResolver(new ide.DefaultExtensionTypeResolver());
                    this._contentTypeRegistry = reg;
                }
                getContentTypeRegistry() {
                    return this._contentTypeRegistry;
                }
                getFileStorage() {
                    return this._fileStorage;
                }
                getContentTypeIcon(contentType) {
                    if (this._contentType_icon_Map.has(contentType)) {
                        return this._contentType_icon_Map.get(contentType);
                    }
                    return null;
                }
                getFileImage(file) {
                    return ui.controls.Controls.getImage(file.getUrl(), file.getId());
                }
                getWorkbenchIcon(name) {
                    return ui.controls.Controls.getIcon(name, "phasereditor2d/ui/ide/images");
                }
                getEditorRegistry() {
                    return this._editorRegistry;
                }
                getEditors() {
                    const editorArea = this.getActiveWindow().getEditorArea();
                    return editorArea.getContentList();
                }
                openEditor(input) {
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
                    }
                    else {
                        alert("No editor available for the given input.");
                    }
                }
            }
            ide.Workbench = Workbench;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            ide.IMG_SECTION_PADDING = 10;
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var commands;
            (function (commands) {
                class Command {
                    constructor(id) {
                        this._id = id;
                    }
                    getId() {
                        return this._id;
                    }
                }
                commands.Command = Command;
            })(commands = ide.commands || (ide.commands = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var commands;
            (function (commands) {
                class CommandArgs {
                    constructor(activePart, activeEditor) {
                        this.activePart = activePart;
                        this.activeEditor = activeEditor;
                    }
                }
                commands.CommandArgs = CommandArgs;
            })(commands = ide.commands || (ide.commands = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var commands;
            (function (commands) {
                class CommandHandler {
                    constructor(config) {
                        this._testFunc = config.testFunc;
                        this._executeFunc = config.executeFunc;
                    }
                    test(args) {
                        return this._testFunc ? this._testFunc(args) : true;
                    }
                    execute(args) {
                        if (this._executeFunc) {
                            this._executeFunc(args);
                        }
                    }
                }
                commands.CommandHandler = CommandHandler;
            })(commands = ide.commands || (ide.commands = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var commands;
            (function (commands) {
                class CommandManager {
                    constructor() {
                        this._commands = [];
                        this._commandIdMap = new Map();
                        this._commandMatcherMap = new Map();
                        this._commandHandlerMap = new Map();
                        window.addEventListener("keydown", e => { this.onKeyDown(e); });
                    }
                    onKeyDown(event) {
                        if (event.isComposing) {
                            return;
                        }
                        const args = this.makeArgs();
                        for (const command of this._commands) {
                            let eventMatches = false;
                            const matchers = this._commandMatcherMap.get(command);
                            for (const matcher of matchers) {
                                if (matcher.matchesKeys(event) && matcher.matchesTarget(event.target)) {
                                    event.preventDefault();
                                    eventMatches = true;
                                    break;
                                }
                            }
                            if (eventMatches) {
                                const handlers = this._commandHandlerMap.get(command);
                                for (const handler of handlers) {
                                    if (handler.test(args)) {
                                        handler.execute(args);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    addCommand(cmd) {
                        this._commands.push(cmd);
                        this._commandIdMap.set(cmd.getId(), cmd);
                        this._commandMatcherMap.set(cmd, []);
                        this._commandHandlerMap.set(cmd, []);
                    }
                    addCommandHelper(id) {
                        this.addCommand(new commands.Command(id));
                    }
                    makeArgs() {
                        const wb = ide.Workbench.getWorkbench();
                        return new commands.CommandArgs(wb.getActivePart(), wb.getActiveEditor());
                    }
                    getCommand(id) {
                        const command = this._commandIdMap.get(id);
                        if (!command) {
                            console.warn(`Command ${id} not found.`);
                        }
                        return command;
                    }
                    addKeyBinding(commandId, matcher) {
                        const command = this.getCommand(commandId);
                        if (command) {
                            this._commandMatcherMap.get(command).push(matcher);
                        }
                    }
                    addHandler(commandId, handler) {
                        const command = this.getCommand(commandId);
                        if (command) {
                            this._commandHandlerMap.get(command).push(handler);
                        }
                    }
                    addHandlerHelper(commandId, testFunc, executeFunc) {
                        this.addHandler(commandId, new commands.CommandHandler({
                            testFunc: testFunc,
                            executeFunc: executeFunc
                        }));
                    }
                }
                commands.CommandManager = CommandManager;
            })(commands = ide.commands || (ide.commands = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var commands;
            (function (commands) {
                class KeyMatcher {
                    constructor(config) {
                        this._control = config.control === undefined ? false : config.control;
                        this._shift = config.shift === undefined ? false : config.shift;
                        this._alt = config.alt === undefined ? false : config.alt;
                        this._meta = config.meta === undefined ? false : config.meta;
                        this._key = config.key === undefined ? "" : config.key;
                        this._filterInputElements = config.filterInputElements === undefined ? true : config.filterInputElements;
                    }
                    matchesKeys(event) {
                        return event.ctrlKey === this._control
                            && event.shiftKey === this._shift
                            && event.altKey === this._alt
                            && event.metaKey === this._meta
                            && event.key.toLowerCase() === this._key.toLowerCase();
                    }
                    matchesTarget(element) {
                        if (this._filterInputElements) {
                            return !(element instanceof HTMLInputElement);
                        }
                        return true;
                    }
                }
                commands.KeyMatcher = KeyMatcher;
            })(commands = ide.commands || (ide.commands = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack_1) {
                    class AssetFinder {
                        constructor(packs) {
                            this._packs = packs;
                        }
                        static async create() {
                            return new AssetFinder(await pack_1.AssetPackUtils.getAllPacks());
                        }
                        async update() {
                            this._packs = await pack_1.AssetPackUtils.getAllPacks();
                        }
                        getPacks() {
                            return this._packs;
                        }
                        findAssetPackItem(key) {
                            return this._packs
                                .flatMap(pack => pack.getItems())
                                .find(item => item.getKey() === key);
                        }
                        getAssetPackItemOrFrame(key, frame) {
                            let item = this.findAssetPackItem(key);
                            if (!item) {
                                return null;
                            }
                            if (item.getType() === pack_1.IMAGE_TYPE) {
                                if (frame === null || frame === undefined) {
                                    return item;
                                }
                                return null;
                            }
                            else if (pack_1.AssetPackUtils.isImageFrameContainer(item)) {
                                const frames = pack_1.AssetPackUtils.getImageFrames(item);
                                const imageFrame = frames.find(imageFrame => imageFrame.getName() === frame);
                                return imageFrame;
                            }
                            return item;
                        }
                        getAssetPackItemImage(key, frame) {
                            const asset = this.getAssetPackItemOrFrame(key, frame);
                            if (asset instanceof pack.AssetPackItem && asset.getType() === pack.IMAGE_TYPE) {
                                return pack.AssetPackUtils.getImageFromPackUrl(asset.getData().url);
                            }
                            else if (asset instanceof pack.AssetPackImageFrame) {
                                return asset;
                            }
                            return new ui.controls.ImageWrapper(null);
                        }
                    }
                    pack_1.AssetFinder = AssetFinder;
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    pack.IMAGE_TYPE = "image";
                    pack.ATLAS_TYPE = "atlas";
                    pack.ATLAS_XML_TYPE = "atlasXML";
                    pack.UNITY_ATLAS_TYPE = "unityAtlas";
                    pack.MULTI_ATLAS_TYPE = "multiatlas";
                    pack.SPRITESHEET_TYPE = "spritesheet";
                    class AssetPack {
                        constructor(file, content) {
                            this._file = file;
                            this._items = [];
                            if (content) {
                                try {
                                    const data = JSON.parse(content);
                                    for (const sectionId in data) {
                                        const sectionData = data[sectionId];
                                        const filesData = sectionData["files"];
                                        if (filesData) {
                                            for (const fileData of filesData) {
                                                const item = new pack.AssetPackItem(this, fileData);
                                                this._items.push(item);
                                            }
                                        }
                                    }
                                }
                                catch (e) {
                                    console.error(e);
                                    alert(e.message);
                                }
                            }
                        }
                        static async createFromFile(file) {
                            const content = await ui.ide.Workbench.getWorkbench().getFileStorage().getFileString(file);
                            return new AssetPack(file, content);
                        }
                        getItems() {
                            return this._items;
                        }
                        getFile() {
                            return this._file;
                        }
                    }
                    pack.AssetPack = AssetPack;
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    pack.CONTENT_TYPE_ASSET_PACK = "PhaserAssetPack";
                    class AssetPackContentTypeResolver {
                        async computeContentType(file) {
                            if (file.getExtension() === "json") {
                                const content = await ui.ide.Workbench.getWorkbench().getFileStorage().getFileString(file);
                                if (content !== null) {
                                    try {
                                        const data = JSON.parse(content);
                                        const meta = data["meta"];
                                        if (meta["contentType"] === "Phaser v3 Asset Pack") {
                                            return pack.CONTENT_TYPE_ASSET_PACK;
                                        }
                                    }
                                    catch (e) {
                                    }
                                }
                            }
                            return phasereditor2d.core.CONTENT_TYPE_ANY;
                        }
                    }
                    pack.AssetPackContentTypeResolver = AssetPackContentTypeResolver;
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var io = phasereditor2d.core.io;
                    class AssetPackEditorFactory extends ide.EditorFactory {
                        constructor() {
                            super("phasereditor2d.AssetPackEditorFactory");
                        }
                        acceptInput(input) {
                            if (input instanceof io.FilePath) {
                                const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                                return contentType === pack.CONTENT_TYPE_ASSET_PACK;
                            }
                            return false;
                        }
                        createEditor() {
                            return new AssetPackEditor();
                        }
                    }
                    pack.AssetPackEditorFactory = AssetPackEditorFactory;
                    class AssetPackEditor extends ide.FileEditor {
                        constructor() {
                            super("phasereditor2d.AssetPackEditor");
                            this.addClass("AssetPackEditor");
                        }
                        static getFactory() {
                            return new AssetPackEditorFactory();
                        }
                        createPart() {
                            this.updateContent();
                        }
                        async updateContent() {
                            const file = this.getInput();
                            if (!file) {
                                return;
                            }
                            const content = await ide.Workbench.getWorkbench().getFileStorage().getFileString(file);
                            this.getElement().innerHTML = content;
                        }
                        setInput(file) {
                            super.setInput(file);
                            this.updateContent();
                        }
                    }
                    pack.AssetPackEditor = AssetPackEditor;
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class ImageFrame {
                constructor(name, image, frameData) {
                    this._name = name;
                    this._image = image;
                    this._frameData = frameData;
                }
                getName() {
                    return this._name;
                }
                getImage() {
                    return this._image;
                }
                getFrameData() {
                    return this._frameData;
                }
                paint(context, x, y, w, h, center) {
                    const fd = this._frameData;
                    const img = this._image;
                    const renderWidth = w;
                    const renderHeight = h;
                    let imgW = fd.src.w;
                    let imgH = fd.src.h;
                    // compute the right width
                    imgW = imgW * (renderHeight / imgH);
                    imgH = renderHeight;
                    // fix width if it goes beyond the area
                    if (imgW > renderWidth) {
                        imgH = imgH * (renderWidth / imgW);
                        imgW = renderWidth;
                    }
                    const scale = imgW / fd.src.w;
                    var imgX = x + (center ? renderWidth / 2 - imgW / 2 : 0);
                    var imgY = y + renderHeight / 2 - imgH / 2;
                    // here we use the trimmed version of the image, maybe this should be parametrized
                    const imgDstW = fd.src.w * scale;
                    const imgDstH = fd.src.h * scale;
                    if (imgDstW > 0 && imgDstH > 0) {
                        img.paintFrame(context, fd.src.x, fd.src.y, fd.src.w, fd.src.h, imgX, imgY, imgDstW, imgDstH);
                    }
                }
                paintFrame(context, srcX, srcY, scrW, srcH, dstX, dstY, dstW, dstH) {
                    // not implemented fow now
                }
                preload() {
                    return this._image.preload();
                }
                getWidth() {
                    return this._frameData.srcSize.x;
                }
                getHeight() {
                    return this._frameData.srcSize.y;
                }
            }
            controls.ImageFrame = ImageFrame;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../phasereditor2d.ui.controls/ImageFrame.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    class AssetPackImageFrame extends ui.controls.ImageFrame {
                        constructor(packItem, name, frameImage, frameData) {
                            super(name, frameImage, frameData);
                            this._packItem = packItem;
                        }
                        getPackItem() {
                            return this._packItem;
                        }
                    }
                    pack.AssetPackImageFrame = AssetPackImageFrame;
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack_2) {
                    class AssetPackItem {
                        constructor(pack, data) {
                            this._pack = pack;
                            this._data = data;
                            this._editorData = {};
                        }
                        getEditorData() {
                            return this._editorData;
                        }
                        getPack() {
                            return this._pack;
                        }
                        getKey() {
                            return this._data["key"];
                        }
                        getType() {
                            return this._data["type"];
                        }
                        getData() {
                            return this._data;
                        }
                    }
                    pack_2.AssetPackItem = AssetPackItem;
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack_3) {
                    const IMAGE_FRAME_CONTAINER_TYPES = new Set([
                        pack_3.IMAGE_TYPE,
                        pack_3.MULTI_ATLAS_TYPE,
                        pack_3.ATLAS_TYPE,
                        pack_3.UNITY_ATLAS_TYPE,
                        pack_3.ATLAS_XML_TYPE,
                        pack_3.SPRITESHEET_TYPE
                    ]);
                    const ATLAS_TYPES = new Set([
                        pack_3.MULTI_ATLAS_TYPE,
                        pack_3.ATLAS_TYPE,
                        pack_3.UNITY_ATLAS_TYPE,
                        pack_3.ATLAS_XML_TYPE,
                    ]);
                    class AssetPackUtils {
                        static isAtlasPackItem(packItem) {
                            return ATLAS_TYPES.has(packItem.getType());
                        }
                        static isImageFrameContainer(packItem) {
                            return IMAGE_FRAME_CONTAINER_TYPES.has(packItem.getType());
                        }
                        static getImageFrames(packItem) {
                            const parser = this.getImageFrameParser(packItem);
                            if (parser) {
                                return parser.parse();
                            }
                            return [];
                        }
                        static getImageFrameParser(packItem) {
                            switch (packItem.getType()) {
                                case pack_3.IMAGE_TYPE:
                                    return new pack.parsers.ImageParser(packItem);
                                case pack_3.ATLAS_TYPE:
                                    return new pack.parsers.AtlasParser(packItem);
                                case pack_3.ATLAS_XML_TYPE:
                                    return new pack.parsers.AtlasXMLParser(packItem);
                                case pack_3.UNITY_ATLAS_TYPE:
                                    return new pack.parsers.UnityAtlasParser(packItem);
                                case pack_3.MULTI_ATLAS_TYPE:
                                    return new pack.parsers.MultiAtlasParser(packItem);
                                case pack_3.SPRITESHEET_TYPE:
                                    return new pack.parsers.SpriteSheetParser(packItem);
                                default:
                                    break;
                            }
                            return null;
                        }
                        static async preloadAssetPackItems(packItems) {
                            for (const item of packItems) {
                                if (this.isImageFrameContainer(item)) {
                                    const parser = this.getImageFrameParser(item);
                                    await parser.preload();
                                }
                            }
                        }
                        static async getAllPacks() {
                            const files = await ide.FileUtils.getFilesWithContentType(pack_3.CONTENT_TYPE_ASSET_PACK);
                            const packs = [];
                            for (const file of files) {
                                const pack = await pack_3.AssetPack.createFromFile(file);
                                packs.push(pack);
                            }
                            return packs;
                        }
                        static getFileFromPackUrl(url) {
                            return ide.FileUtils.getFileFromPath(url);
                        }
                        static getFileStringFromPackUrl(url) {
                            const file = ide.FileUtils.getFileFromPath(url);
                            const str = ide.Workbench.getWorkbench().getFileStorage().getFileStringFromCache(file);
                            return str;
                        }
                        static getFileJSONFromPackUrl(url) {
                            const str = this.getFileStringFromPackUrl(url);
                            return JSON.parse(str);
                        }
                        static getFileXMLFromPackUrl(url) {
                            const str = this.getFileStringFromPackUrl(url);
                            const parser = new DOMParser();
                            return parser.parseFromString(str, "text/xml");
                        }
                        static getImageFromPackUrl(url) {
                            const file = this.getFileFromPackUrl(url);
                            if (file) {
                                return ide.Workbench.getWorkbench().getFileImage(file);
                            }
                            return null;
                        }
                    }
                    pack_3.AssetPackUtils = AssetPackUtils;
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var parsers;
                    (function (parsers) {
                        class ImageFrameParser {
                            constructor(packItem) {
                                this._packItem = packItem;
                            }
                            setCachedFrames(frames) {
                                this._packItem.getEditorData()["__frames_cache"] = frames;
                            }
                            getCachedFrames() {
                                return this._packItem.getEditorData()["__frames_cache"];
                            }
                            hasCachedFrames() {
                                return "__frames_cache" in this._packItem.getEditorData();
                            }
                            getPackItem() {
                                return this._packItem;
                            }
                            async preload() {
                                if (this.hasCachedFrames()) {
                                    return ui.controls.Controls.resolveNothingLoaded();
                                }
                                return this.preloadFrames();
                            }
                            parse() {
                                if (this.hasCachedFrames()) {
                                    return this.getCachedFrames();
                                }
                                const frames = this.parseFrames();
                                this.setCachedFrames(frames);
                                return frames;
                            }
                        }
                        parsers.ImageFrameParser = ImageFrameParser;
                    })(parsers = pack.parsers || (pack.parsers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./ImageFrameParser.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var parsers;
                    (function (parsers) {
                        class BaseAtlasParser extends parsers.ImageFrameParser {
                            constructor(packItem) {
                                super(packItem);
                            }
                            addToPhaserCache(game) {
                                const item = this.getPackItem();
                                if (!game.textures.exists(item.getKey())) {
                                    const atlasURL = item.getData().atlasURL;
                                    const atlasData = pack.AssetPackUtils.getFileJSONFromPackUrl(atlasURL);
                                    const textureURL = item.getData().textureURL;
                                    const image = pack.AssetPackUtils.getImageFromPackUrl(textureURL);
                                    game.textures.addAtlas(item.getKey(), image.getImageElement(), atlasData);
                                }
                            }
                            async preloadFrames() {
                                const data = this.getPackItem().getData();
                                const dataFile = pack.AssetPackUtils.getFileFromPackUrl(data.atlasURL);
                                let result1 = await ide.FileUtils.preloadFileString(dataFile);
                                const imageFile = pack.AssetPackUtils.getFileFromPackUrl(data.textureURL);
                                const image = ide.FileUtils.getImage(imageFile);
                                let result2 = await image.preload();
                                return Math.max(result1, result2);
                            }
                            parseFrames() {
                                if (this.hasCachedFrames()) {
                                    return this.getCachedFrames();
                                }
                                const list = [];
                                const data = this.getPackItem().getData();
                                const dataFile = pack.AssetPackUtils.getFileFromPackUrl(data.atlasURL);
                                const imageFile = pack.AssetPackUtils.getFileFromPackUrl(data.textureURL);
                                const image = ide.FileUtils.getImage(imageFile);
                                if (dataFile) {
                                    const str = ide.FileUtils.getFileStringFromCache(dataFile);
                                    try {
                                        this.parseFrames2(list, image, str);
                                    }
                                    catch (e) {
                                        console.error(e);
                                    }
                                }
                                return list;
                            }
                        }
                        parsers.BaseAtlasParser = BaseAtlasParser;
                    })(parsers = pack.parsers || (pack.parsers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseAtlasParser.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var parsers;
                    (function (parsers) {
                        class AtlasParser extends parsers.BaseAtlasParser {
                            constructor(packItem) {
                                super(packItem);
                            }
                            parseFrames2(imageFrames, image, atlas) {
                                try {
                                    const data = JSON.parse(atlas);
                                    if (Array.isArray(data.frames)) {
                                        for (const frame of data.frames) {
                                            const frameData = AtlasParser.buildFrameData(this.getPackItem(), image, frame, imageFrames.length);
                                            imageFrames.push(frameData);
                                        }
                                    }
                                    else {
                                        for (const name in data.frames) {
                                            const frame = data.frames[name];
                                            frame.filename = name;
                                            const frameData = AtlasParser.buildFrameData(this.getPackItem(), image, frame, imageFrames.length);
                                            imageFrames.push(frameData);
                                        }
                                    }
                                }
                                catch (e) {
                                    console.error(e);
                                }
                            }
                            static buildFrameData(packItem, image, frame, index) {
                                const src = new ui.controls.Rect(frame.frame.x, frame.frame.y, frame.frame.w, frame.frame.h);
                                const dst = new ui.controls.Rect(frame.spriteSourceSize.x, frame.spriteSourceSize.y, frame.spriteSourceSize.w, frame.spriteSourceSize.h);
                                const srcSize = new ui.controls.Point(frame.sourceSize.w, frame.sourceSize.h);
                                const frameData = new ui.controls.FrameData(index, src, dst, srcSize);
                                return new pack.AssetPackImageFrame(packItem, frame.filename, image, frameData);
                            }
                        }
                        parsers.AtlasParser = AtlasParser;
                    })(parsers = pack.parsers || (pack.parsers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseAtlasParser.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var parsers;
                    (function (parsers) {
                        class AtlasXMLParser extends parsers.BaseAtlasParser {
                            constructor(packItem) {
                                super(packItem);
                            }
                            addToPhaserCache(game) {
                                const item = this.getPackItem();
                                if (!game.textures.exists(item.getKey())) {
                                    const atlasURL = item.getData().atlasURL;
                                    const atlasData = pack.AssetPackUtils.getFileXMLFromPackUrl(atlasURL);
                                    const textureURL = item.getData().textureURL;
                                    const image = pack.AssetPackUtils.getImageFromPackUrl(textureURL);
                                    game.textures.addAtlasXML(item.getKey(), image.getImageElement(), atlasData);
                                }
                            }
                            parseFrames2(imageFrames, image, atlas) {
                                try {
                                    const parser = new DOMParser();
                                    const data = parser.parseFromString(atlas, "text/xml");
                                    const elements = data.getElementsByTagName("SubTexture");
                                    for (let i = 0; i < elements.length; i++) {
                                        const elem = elements.item(i);
                                        const name = elem.getAttribute("name");
                                        const frameX = Number.parseInt(elem.getAttribute("x"));
                                        const frameY = Number.parseInt(elem.getAttribute("y"));
                                        const frameW = Number.parseInt(elem.getAttribute("width"));
                                        const frameH = Number.parseInt(elem.getAttribute("height"));
                                        let spriteX = frameX;
                                        let spriteY = frameY;
                                        let spriteW = frameW;
                                        let spriteH = frameH;
                                        if (elem.hasAttribute("frameX")) {
                                            spriteX = Number.parseInt(elem.getAttribute("frameX"));
                                            spriteY = Number.parseInt(elem.getAttribute("frameY"));
                                            spriteW = Number.parseInt(elem.getAttribute("frameWidth"));
                                            spriteH = Number.parseInt(elem.getAttribute("frameHeight"));
                                        }
                                        const fd = new ui.controls.FrameData(i, new ui.controls.Rect(frameX, frameY, frameW, frameH), new ui.controls.Rect(spriteX, spriteY, spriteW, spriteH), new ui.controls.Point(frameW, frameH));
                                        imageFrames.push(new pack.AssetPackImageFrame(this.getPackItem(), name, image, fd));
                                    }
                                }
                                catch (e) {
                                    console.error(e);
                                }
                            }
                        }
                        parsers.AtlasXMLParser = AtlasXMLParser;
                    })(parsers = pack.parsers || (pack.parsers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var parsers;
                    (function (parsers) {
                        class ImageParser extends parsers.ImageFrameParser {
                            constructor(packItem) {
                                super(packItem);
                            }
                            addToPhaserCache(game) {
                                const item = this.getPackItem();
                                if (!game.textures.exists(item.getKey())) {
                                    const url = item.getData().url;
                                    const image = pack.AssetPackUtils.getImageFromPackUrl(url);
                                    game.textures.addImage(item.getKey(), image.getImageElement());
                                }
                            }
                            preloadFrames() {
                                const url = this.getPackItem().getData().url;
                                const img = pack.AssetPackUtils.getImageFromPackUrl(url);
                                return img.preload();
                            }
                            parseFrames() {
                                const url = this.getPackItem().getData().url;
                                const img = pack.AssetPackUtils.getImageFromPackUrl(url);
                                const fd = new ui.controls.FrameData(0, new ui.controls.Rect(0, 0, img.getWidth(), img.getHeight()), new ui.controls.Rect(0, 0, img.getWidth(), img.getHeight()), new ui.controls.Point(img.getWidth(), img.getWidth()));
                                return [new pack.AssetPackImageFrame(this.getPackItem(), this.getPackItem().getKey(), img, fd)];
                            }
                        }
                        parsers.ImageParser = ImageParser;
                    })(parsers = pack.parsers || (pack.parsers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var parsers;
                    (function (parsers) {
                        class MultiAtlasParser extends parsers.ImageFrameParser {
                            constructor(packItem) {
                                super(packItem);
                            }
                            addToPhaserCache(game) {
                                const item = this.getPackItem();
                                if (!game.textures.exists(item.getKey())) {
                                    const packItemData = item.getData();
                                    const atlasDataFile = pack.AssetPackUtils.getFileFromPackUrl(packItemData.url);
                                    const atlasData = pack.AssetPackUtils.getFileJSONFromPackUrl(packItemData.url);
                                    const images = [];
                                    const jsonArrayData = [];
                                    for (const textureData of atlasData.textures) {
                                        const imageName = textureData.image;
                                        const imageFile = atlasDataFile.getSibling(imageName);
                                        const image = ide.FileUtils.getImage(imageFile);
                                        images.push(image.getImageElement());
                                        jsonArrayData.push(textureData);
                                    }
                                    game.textures.addAtlasJSONArray(this.getPackItem().getKey(), images, jsonArrayData);
                                }
                            }
                            async preloadFrames() {
                                const data = this.getPackItem().getData();
                                const dataFile = pack.AssetPackUtils.getFileFromPackUrl(data.url);
                                if (dataFile) {
                                    let result = await ide.FileUtils.preloadFileString(dataFile);
                                    const str = ide.FileUtils.getFileStringFromCache(dataFile);
                                    try {
                                        const data = JSON.parse(str);
                                        if (data.textures) {
                                            for (const texture of data.textures) {
                                                const imageName = texture.image;
                                                const imageFile = dataFile.getSibling(imageName);
                                                if (imageFile) {
                                                    const image = ide.Workbench.getWorkbench().getFileImage(imageFile);
                                                    const result2 = await image.preload();
                                                    result = Math.max(result, result2);
                                                }
                                            }
                                        }
                                    }
                                    catch (e) {
                                    }
                                    return result;
                                }
                                return ui.controls.Controls.resolveNothingLoaded();
                            }
                            parseFrames() {
                                const list = [];
                                const data = this.getPackItem().getData();
                                const dataFile = pack.AssetPackUtils.getFileFromPackUrl(data.url);
                                if (dataFile) {
                                    const str = ide.Workbench.getWorkbench().getFileStorage().getFileStringFromCache(dataFile);
                                    try {
                                        const data = JSON.parse(str);
                                        if (data.textures) {
                                            for (const textureData of data.textures) {
                                                const imageName = textureData.image;
                                                const imageFile = dataFile.getSibling(imageName);
                                                const image = ide.FileUtils.getImage(imageFile);
                                                for (const frame of textureData.frames) {
                                                    const frameData = parsers.AtlasParser.buildFrameData(this.getPackItem(), image, frame, list.length);
                                                    list.push(frameData);
                                                }
                                            }
                                        }
                                    }
                                    catch (e) {
                                        console.error(e);
                                    }
                                }
                                return list;
                            }
                        }
                        parsers.MultiAtlasParser = MultiAtlasParser;
                    })(parsers = pack.parsers || (pack.parsers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./BaseAtlasParser.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var parsers;
                    (function (parsers) {
                        class SpriteSheetParser extends parsers.ImageFrameParser {
                            constructor(packItem) {
                                super(packItem);
                            }
                            addToPhaserCache(game) {
                                const item = this.getPackItem();
                                if (!game.textures.exists(item.getKey())) {
                                    const data = item.getData();
                                    const image = pack.AssetPackUtils.getImageFromPackUrl(data.url);
                                    game.textures.addSpriteSheet(item.getKey(), image.getImageElement(), data.frameConfig);
                                }
                            }
                            async preloadFrames() {
                                const data = this.getPackItem().getData();
                                const imageFile = pack.AssetPackUtils.getFileFromPackUrl(data.url);
                                const image = ide.FileUtils.getImage(imageFile);
                                return await image.preload();
                            }
                            parseFrames() {
                                const frames = [];
                                const data = this.getPackItem().getData();
                                const imageFile = pack.AssetPackUtils.getFileFromPackUrl(data.url);
                                const image = ide.FileUtils.getImage(imageFile);
                                const w = data.frameConfig.frameWidth;
                                const h = data.frameConfig.frameHeight;
                                const margin = data.frameConfig.margin || 0;
                                const spacing = data.frameConfig.spacing || 0;
                                const startFrame = data.frameConfig.startFrame || 0;
                                const endFrame = data.frameConfig.endFrame || -1;
                                if (w <= 0 || h <= 0 || spacing < 0 || margin < 0) {
                                    // invalid values
                                    return frames;
                                }
                                const start = startFrame < 0 ? 0 : startFrame;
                                const end = endFrame < 0 ? Number.MAX_VALUE : endFrame;
                                let i = 0;
                                let row = 0;
                                let column = 0;
                                let x = margin;
                                let y = margin;
                                while (true) {
                                    if (i > end || y >= image.getHeight() || i > 50) {
                                        break;
                                    }
                                    if (i >= start) {
                                        if (x + w <= image.getWidth() && y + h <= image.getHeight()) {
                                            // FrameModel frame = new FrameModel(this, i, row, column, new Rectangle(x, y, w, h));
                                            // list.add(frame);
                                            const fd = new ui.controls.FrameData(i, new ui.controls.Rect(x, y, w, h), new ui.controls.Rect(0, 0, w, h), new ui.controls.Point(w, h));
                                            frames.push(new pack.AssetPackImageFrame(this.getPackItem(), i.toString(), image, fd));
                                        }
                                    }
                                    column++;
                                    x += w + spacing;
                                    if (x >= image.getWidth()) {
                                        x = margin;
                                        y += h + spacing;
                                        column = 0;
                                        row++;
                                    }
                                    i++;
                                }
                                return frames;
                            }
                        }
                        parsers.SpriteSheetParser = SpriteSheetParser;
                    })(parsers = pack.parsers || (pack.parsers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var parsers;
                    (function (parsers) {
                        class UnityAtlasParser extends parsers.BaseAtlasParser {
                            addToPhaserCache(game) {
                                const item = this.getPackItem();
                                if (!game.textures.exists(item.getKey())) {
                                    const atlasURL = item.getData().atlasURL;
                                    const atlasData = pack.AssetPackUtils.getFileStringFromPackUrl(atlasURL);
                                    const textureURL = item.getData().textureURL;
                                    const image = pack.AssetPackUtils.getImageFromPackUrl(textureURL);
                                    game.textures.addUnityAtlas(item.getKey(), image.getImageElement(), atlasData);
                                }
                            }
                            parseFrames2(imageFrames, image, atlas) {
                                // Taken from Phaser code.
                                const data = atlas.split('\n');
                                const lineRegExp = /^[ ]*(- )*(\w+)+[: ]+(.*)/;
                                let prevSprite = '';
                                let currentSprite = '';
                                let rect = { x: 0, y: 0, width: 0, height: 0 };
                                // const pivot = { x: 0, y: 0 };
                                // const border = { x: 0, y: 0, z: 0, w: 0 };
                                for (let i = 0; i < data.length; i++) {
                                    const results = data[i].match(lineRegExp);
                                    if (!results) {
                                        continue;
                                    }
                                    const isList = (results[1] === '- ');
                                    const key = results[2];
                                    const value = results[3];
                                    if (isList) {
                                        if (currentSprite !== prevSprite) {
                                            this.addFrame(image, imageFrames, currentSprite, rect);
                                            prevSprite = currentSprite;
                                        }
                                        rect = { x: 0, y: 0, width: 0, height: 0 };
                                    }
                                    if (key === 'name') {
                                        //  Start new list
                                        currentSprite = value;
                                        continue;
                                    }
                                    switch (key) {
                                        case 'x':
                                        case 'y':
                                        case 'width':
                                        case 'height':
                                            rect[key] = parseInt(value, 10);
                                            break;
                                        // case 'pivot':
                                        //     pivot = eval('const obj = ' + value);
                                        //     break;
                                        // case 'border':
                                        //     border = eval('const obj = ' + value);
                                        //     break;
                                    }
                                }
                                if (currentSprite !== prevSprite) {
                                    this.addFrame(image, imageFrames, currentSprite, rect);
                                }
                            }
                            addFrame(image, imageFrames, spriteName, rect) {
                                const src = new ui.controls.Rect(rect.x, rect.y, rect.width, rect.height);
                                src.y = image.getHeight() - src.y - src.h;
                                const dst = new ui.controls.Rect(0, 0, rect.width, rect.height);
                                const srcSize = new ui.controls.Point(rect.width, rect.height);
                                const fd = new ui.controls.FrameData(imageFrames.length, src, dst, srcSize);
                                imageFrames.push(new pack.AssetPackImageFrame(this.getPackItem(), spriteName, image, fd));
                            }
                        }
                        parsers.UnityAtlasParser = UnityAtlasParser;
                    })(parsers = pack.parsers || (pack.parsers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/*

TextureImporter:
  spritePivot: {x: .5, y: .5}
  spriteBorder: {x: 0, y: 0, z: 0, w: 0}
  spritePixelsToUnits: 100
  spriteSheet:
    sprites:
    - name: asteroids_0
      rect:
        serializedVersion: 2
        x: 5
        y: 328
        width: 65
        height: 82
      alignment: 0
      pivot: {x: 0, y: 0}
      border: {x: 0, y: 0, z: 0, w: 0}
    - name: asteroids_1
      rect:
        serializedVersion: 2
        x: 80
        y: 322
        width: 53
        height: 88
      alignment: 0
      pivot: {x: 0, y: 0}
      border: {x: 0, y: 0, z: 0, w: 0}
  spritePackingTag: Asteroids

  */ 
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var properties;
            (function (properties) {
                class PropertySection {
                    constructor(page, id, title, fillSpace = false) {
                        this._page = page;
                        this._id = id;
                        this._title = title;
                        this._fillSpace = fillSpace;
                        this._updaters = [];
                    }
                    updateWithSelection() {
                        for (const updater of this._updaters) {
                            updater();
                        }
                    }
                    addUpdater(updater) {
                        this._updaters.push(updater);
                    }
                    isFillSpace() {
                        return this._fillSpace;
                    }
                    getPage() {
                        return this._page;
                    }
                    getSelection() {
                        return this._page.getSelection();
                    }
                    getId() {
                        return this._id;
                    }
                    getTitle() {
                        return this._title;
                    }
                    create(parent) {
                        this.createForm(parent);
                    }
                    flatValues_Number(values) {
                        const set = new Set(values);
                        if (set.size == 1) {
                            const value = set.values().next().value;
                            return value.toString();
                        }
                        return "";
                    }
                    flatValues_StringJoin(values) {
                        return values.join(",");
                    }
                    createGridElement(parent, cols = 0, simpleProps = true) {
                        const div = document.createElement("div");
                        div.classList.add("formGrid");
                        if (cols > 0) {
                            div.classList.add("formGrid-cols-" + cols);
                        }
                        if (simpleProps) {
                            div.classList.add("formSimpleProps");
                        }
                        parent.appendChild(div);
                        return div;
                    }
                    createLabel(parent, text = "") {
                        const label = document.createElement("label");
                        label.classList.add("formLabel");
                        label.innerText = text;
                        parent.appendChild(label);
                        return label;
                    }
                    createText(parent, readOnly = false) {
                        const text = document.createElement("input");
                        text.type = "text";
                        text.classList.add("formText");
                        text.readOnly = readOnly;
                        parent.appendChild(text);
                        return text;
                    }
                }
                properties.PropertySection = PropertySection;
            })(properties = controls.properties || (controls.properties = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../../phasereditor2d.ui.controls/properties/PropertySection.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var properties;
                    (function (properties) {
                        class AssetPackItemSection extends ui.controls.properties.PropertySection {
                            constructor(page) {
                                super(page, "AssetPackItemPropertySection", "File Key", false);
                            }
                            createForm(parent) {
                                const comp = this.createGridElement(parent, 2);
                                {
                                    // Key
                                    this.createLabel(comp, "Key");
                                    const text = this.createText(comp, true);
                                    this.addUpdater(() => {
                                        text.value = this.flatValues_StringJoin(this.getSelection().map(item => item.getKey()));
                                    });
                                }
                            }
                            canEdit(obj) {
                                return obj instanceof pack.AssetPackItem;
                            }
                            canEditNumber(n) {
                                return n === 1;
                            }
                        }
                        properties.AssetPackItemSection = AssetPackItemSection;
                    })(properties = pack.properties || (pack.properties = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var properties;
                    (function (properties) {
                        class ImageSection extends ui.controls.properties.PropertySection {
                            constructor(page) {
                                super(page, "pack.ImageSection", "Image", true);
                            }
                            createForm(parent) {
                                parent.classList.add("ImagePreviewFormArea", "PreviewBackground");
                                const imgControl = new ui.controls.ImageControl(ide.IMG_SECTION_PADDING);
                                this.getPage().addEventListener(ui.controls.EVENT_CONTROL_LAYOUT, (e) => {
                                    imgControl.resizeTo();
                                });
                                parent.appendChild(imgControl.getElement());
                                setTimeout(() => imgControl.resizeTo(), 1);
                                this.addUpdater(() => {
                                    const obj = this.getSelection()[0];
                                    let img;
                                    if (obj instanceof pack.AssetPackItem) {
                                        img = pack.AssetPackUtils.getImageFromPackUrl(obj.getData().url);
                                    }
                                    else {
                                        img = obj;
                                    }
                                    imgControl.setImage(img);
                                    setTimeout(() => imgControl.resizeTo(), 1);
                                });
                            }
                            canEdit(obj) {
                                return obj instanceof pack.AssetPackItem && obj.getType() === "image" || obj instanceof ui.controls.ImageFrame;
                            }
                            canEditNumber(n) {
                                return n === 1;
                            }
                        }
                        properties.ImageSection = ImageSection;
                    })(properties = pack.properties || (pack.properties = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var properties;
                    (function (properties) {
                        class ManyImageSection extends ui.controls.properties.PropertySection {
                            constructor(page) {
                                super(page, "phasereditor2d.ui.ide.editors.pack.properties.ManyImageSection", "Images", true);
                            }
                            createForm(parent) {
                                parent.classList.add("ManyImagePreviewFormArea");
                                const viewer = new ui.controls.viewers.TreeViewer("PreviewBackground");
                                viewer.setContentProvider(new ui.controls.viewers.ArrayTreeContentProvider());
                                viewer.setTreeRenderer(new ui.controls.viewers.GridTreeViewerRenderer(viewer, true));
                                viewer.setLabelProvider(new pack.viewers.AssetPackLabelProvider());
                                viewer.setCellRendererProvider(new pack.viewers.AssetPackCellRendererProvider());
                                const filteredViewer = new ide.properties.FilteredViewerInPropertySection(this.getPage(), viewer);
                                parent.appendChild(filteredViewer.getElement());
                                this.addUpdater(async () => {
                                    const frames = await this.getImageFrames();
                                    // clean the viewer first
                                    viewer.setInput([]);
                                    viewer.repaint();
                                    viewer.setInput(frames);
                                    filteredViewer.resizeTo();
                                });
                            }
                            async getImageFrames() {
                                const packItems = this.getSelection().filter(e => e instanceof pack.AssetPackItem);
                                await pack.AssetPackUtils.preloadAssetPackItems(packItems);
                                const frames = this.getSelection().flatMap(obj => {
                                    if (obj instanceof pack.AssetPackItem) {
                                        return pack.AssetPackUtils.getImageFrames(obj);
                                    }
                                    return [obj];
                                });
                                return frames;
                            }
                            canEdit(obj, n) {
                                if (n === 1) {
                                    return obj instanceof pack.AssetPackItem && obj.getType() !== pack.IMAGE_TYPE && pack.AssetPackUtils.isImageFrameContainer(obj);
                                }
                                return obj instanceof ui.controls.ImageFrame || obj instanceof pack.AssetPackItem && pack.AssetPackUtils.isImageFrameContainer(obj);
                            }
                            canEditNumber(n) {
                                return n > 0;
                            }
                        }
                        properties.ManyImageSection = ManyImageSection;
                    })(properties = pack.properties || (pack.properties = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                class TreeViewerRenderer {
                    constructor(viewer, cellSize = controls.ROW_HEIGHT) {
                        this._viewer = viewer;
                        this._viewer.setCellSize(cellSize);
                    }
                    getViewer() {
                        return this._viewer;
                    }
                    paint() {
                        const viewer = this._viewer;
                        let x = 0;
                        let y = viewer.getScrollY();
                        const contentProvider = viewer.getContentProvider();
                        const roots = contentProvider.getRoots(viewer.getInput());
                        const treeIconList = [];
                        const paintItems = [];
                        this.paintItems(roots, treeIconList, paintItems, x, y);
                        let contentHeight = Number.MIN_VALUE;
                        for (const paintItem of paintItems) {
                            contentHeight = Math.max(paintItem.y + paintItem.h, contentHeight);
                        }
                        contentHeight -= viewer.getScrollY();
                        return {
                            contentHeight: contentHeight,
                            treeIconList: treeIconList,
                            paintItems: paintItems
                        };
                    }
                    paintItems(objects, treeIconList, paintItems, x, y) {
                        const viewer = this._viewer;
                        const context = viewer.getContext();
                        const b = viewer.getBounds();
                        for (let obj of objects) {
                            const children = viewer.getContentProvider().getChildren(obj);
                            const expanded = viewer.isExpanded(obj);
                            if (viewer.isFilterIncluded(obj)) {
                                const renderer = viewer.getCellRendererProvider().getCellRenderer(obj);
                                const args = new viewers.RenderCellArgs(context, x + viewers.LABEL_MARGIN, y, b.width - x - viewers.LABEL_MARGIN, 0, obj, viewer);
                                const cellHeight = renderer.cellHeight(args);
                                args.h = cellHeight;
                                viewer.paintItemBackground(obj, 0, y, b.width, cellHeight);
                                if (y > -viewer.getCellSize() && y < b.height) {
                                    // render tree icon
                                    if (children.length > 0) {
                                        const iconY = y + (cellHeight - viewers.TREE_ICON_SIZE) / 2;
                                        const icon = controls.Controls.getIcon(expanded ? controls.ICON_CONTROL_TREE_COLLAPSE : controls.ICON_CONTROL_TREE_EXPAND);
                                        icon.paint(context, x, iconY, controls.ICON_SIZE, controls.ICON_SIZE, false);
                                        treeIconList.push({
                                            rect: new controls.Rect(x, iconY, viewers.TREE_ICON_SIZE, viewers.TREE_ICON_SIZE),
                                            obj: obj
                                        });
                                    }
                                    this.renderTreeCell(args, renderer);
                                }
                                const item = new viewers.PaintItem(paintItems.length, obj);
                                item.set(args.x, args.y, args.w, args.h);
                                paintItems.push(item);
                                y += cellHeight;
                            }
                            if (expanded) {
                                const result = this.paintItems(children, treeIconList, paintItems, x + viewers.LABEL_MARGIN, y);
                                y = result.y;
                            }
                        }
                        return { x: x, y: y };
                    }
                    renderTreeCell(args, renderer) {
                        const label = args.viewer.getLabelProvider().getLabel(args.obj);
                        let x = args.x;
                        let y = args.y;
                        const ctx = args.canvasContext;
                        ctx.fillStyle = controls.Controls.theme.treeItemForeground;
                        let args2;
                        if (args.h <= controls.ROW_HEIGHT) {
                            args2 = new viewers.RenderCellArgs(args.canvasContext, args.x, args.y, controls.ICON_SIZE, args.h, args.obj, args.viewer);
                            x += 20;
                            y += 15;
                        }
                        else {
                            args2 = new viewers.RenderCellArgs(args.canvasContext, args.x, args.y, args.w, args.h - 20, args.obj, args.viewer);
                            y += args2.h + 15;
                        }
                        renderer.renderCell(args2);
                        ctx.save();
                        if (args.viewer.isSelected(args.obj)) {
                            ctx.fillStyle = controls.Controls.theme.treeItemSelectionForeground;
                        }
                        ctx.fillText(label, x, y);
                        ctx.restore();
                    }
                }
                viewers.TreeViewerRenderer = TreeViewerRenderer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./TreeViewerRenderer.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                viewers.TREE_RENDERER_GRID_PADDING = 5;
                class GridTreeViewerRenderer extends viewers.TreeViewerRenderer {
                    constructor(viewer, center = false) {
                        super(viewer);
                        viewer.setCellSize(128);
                        this._center = center;
                        this._sections = [];
                    }
                    setSections(sections) {
                        this._sections = sections;
                    }
                    getSections() {
                        return this._sections;
                    }
                    paintItems(objects, treeIconList, paintItems, x, y) {
                        const viewer = this.getViewer();
                        const cellSize = viewer.getCellSize();
                        if (cellSize <= 48) {
                            return super.paintItems(objects, treeIconList, paintItems, x, y);
                        }
                        const b = viewer.getBounds();
                        if (this._sections.length > 0) {
                            const ctx = viewer.getContext();
                            let y2 = y + 20;
                            let x2 = x + viewers.TREE_RENDERER_GRID_PADDING;
                            for (const section of this._sections) {
                                const objects2 = viewer
                                    .getContentProvider()
                                    .getChildren(section)
                                    .filter(obj => viewer.isFilterIncluded(obj));
                                if (objects2.length === 0) {
                                    continue;
                                }
                                const label = viewer
                                    .getLabelProvider()
                                    .getLabel(section)
                                    .toUpperCase();
                                ctx.save();
                                ctx.fillStyle = controls.Controls.theme.treeItemForeground + "44";
                                {
                                    ctx.fillText(label, x2, y2);
                                }
                                ctx.strokeStyle = controls.Controls.theme.treeItemForeground + "44";
                                ctx.beginPath();
                                ctx.moveTo(0, y2 + 5);
                                ctx.lineTo(b.width, y2 + 5);
                                ctx.stroke();
                                ctx.restore();
                                y2 += 10;
                                const result = this.paintItems2(objects2, treeIconList, paintItems, x2, y2, viewers.TREE_RENDERER_GRID_PADDING, 0);
                                y2 = result.y + 20;
                                if (result.x > viewers.TREE_RENDERER_GRID_PADDING) {
                                    y2 += cellSize;
                                }
                            }
                            return {
                                x: viewers.TREE_RENDERER_GRID_PADDING,
                                y: y2
                            };
                        }
                        else {
                            const offset = this._center ? Math.floor(b.width % (viewer.getCellSize() + viewers.TREE_RENDERER_GRID_PADDING) / 2) : viewers.TREE_RENDERER_GRID_PADDING;
                            return this.paintItems2(objects, treeIconList, paintItems, x + offset, y + viewers.TREE_RENDERER_GRID_PADDING, offset, 0);
                        }
                    }
                    paintItems2(objects, treeIconList, paintItems, x, y, offset, depth) {
                        const viewer = this.getViewer();
                        const cellSize = Math.max(controls.ROW_HEIGHT, viewer.getCellSize());
                        const context = viewer.getContext();
                        const b = viewer.getBounds();
                        const included = objects.filter(obj => viewer.isFilterIncluded(obj));
                        const lastObj = included.length === 0 ? null : included[included.length - 1];
                        for (let obj of objects) {
                            const children = viewer.getContentProvider().getChildren(obj);
                            const expanded = viewer.isExpanded(obj);
                            if (viewer.isFilterIncluded(obj)) {
                                const renderer = viewer.getCellRendererProvider().getCellRenderer(obj);
                                const args = new viewers.RenderCellArgs(context, x, y, cellSize, cellSize, obj, viewer, true);
                                this.renderGridCell(args, renderer, depth, obj === lastObj);
                                if (y > -cellSize && y < b.height) {
                                    // render tree icon
                                    if (children.length > 0) {
                                        const iconY = y + (cellSize - viewers.TREE_ICON_SIZE) / 2;
                                        const icon = controls.Controls.getIcon(expanded ? controls.ICON_CONTROL_TREE_COLLAPSE : controls.ICON_CONTROL_TREE_EXPAND);
                                        icon.paint(context, x + 5, iconY, controls.ICON_SIZE, controls.ICON_SIZE, false);
                                        treeIconList.push({
                                            rect: new controls.Rect(x, iconY, viewers.TREE_ICON_SIZE, viewers.TREE_ICON_SIZE),
                                            obj: obj
                                        });
                                    }
                                }
                                const item = new viewers.PaintItem(paintItems.length, obj);
                                item.set(args.x, args.y, args.w, args.h);
                                paintItems.push(item);
                                x += cellSize + viewers.TREE_RENDERER_GRID_PADDING;
                                if (x + cellSize > b.width) {
                                    y += cellSize + viewers.TREE_RENDERER_GRID_PADDING;
                                    x = 0 + offset;
                                }
                            }
                            if (expanded) {
                                const result = this.paintItems2(children, treeIconList, paintItems, x, y, offset, depth + 1);
                                y = result.y;
                                x = result.x;
                            }
                        }
                        return {
                            x: x,
                            y: y
                        };
                    }
                    renderGridCell(args, renderer, depth, isLastChild) {
                        const cellSize = args.viewer.getCellSize();
                        const b = args.viewer.getBounds();
                        const lineHeight = 20;
                        let x = args.x;
                        const ctx = args.canvasContext;
                        const label = args.viewer.getLabelProvider().getLabel(args.obj);
                        let line = "";
                        for (const c of label) {
                            const test = line + c;
                            const m = ctx.measureText(test);
                            if (m.width > args.w) {
                                if (line.length > 2) {
                                    line = line.substring(0, line.length - 2) + "..";
                                }
                                break;
                            }
                            else {
                                line += c;
                            }
                        }
                        const selected = args.viewer.isSelected(args.obj);
                        let labelHeight;
                        let visible;
                        {
                            labelHeight = lineHeight;
                            visible = args.y > -(cellSize + labelHeight) && args.y < b.height;
                            if (visible) {
                                this.renderCellBack(args, selected, isLastChild);
                                const args2 = new viewers.RenderCellArgs(args.canvasContext, args.x + 3, args.y + 3, args.w - 6, args.h - 6 - lineHeight, args.obj, args.viewer, args.center);
                                renderer.renderCell(args2);
                                this.renderCellFront(args, selected, isLastChild);
                                args.viewer.paintItemBackground(args.obj, args.x, args.y + args.h - lineHeight, args.w, labelHeight, 10);
                            }
                        }
                        if (visible) {
                            ctx.save();
                            if (selected) {
                                ctx.fillStyle = controls.Controls.theme.treeItemSelectionForeground;
                            }
                            else {
                                ctx.fillStyle = controls.Controls.theme.treeItemForeground;
                            }
                            const m = ctx.measureText(line);
                            const x2 = Math.max(x, x + args.w / 2 - m.width / 2);
                            ctx.fillText(line, x2, args.y + args.h - 5);
                            ctx.restore();
                        }
                    }
                    renderCellBack(args, selected, isLastChild) {
                        if (selected) {
                            const ctx = args.canvasContext;
                            ctx.save();
                            ctx.fillStyle = controls.Controls.theme.treeItemSelectionBackground + "88";
                            ctx.fillRect(args.x, args.y, args.w, args.h);
                            ctx.restore();
                        }
                    }
                    renderCellFront(args, selected, isLastChild) {
                        if (selected) {
                            const ctx = args.canvasContext;
                            ctx.save();
                            ctx.fillStyle = controls.Controls.theme.treeItemSelectionBackground + "44";
                            ctx.fillRect(args.x, args.y, args.w, args.h);
                            ctx.restore();
                        }
                    }
                }
                viewers.GridTreeViewerRenderer = GridTreeViewerRenderer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../../phasereditor2d.ui.controls/viewers/GridTreeViewerRenderer.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var viewers;
                    (function (viewers) {
                        class AssetPackBlocksTreeViewerRenderer extends ui.controls.viewers.GridTreeViewerRenderer {
                            constructor(viewer) {
                                super(viewer, false);
                                viewer.setCellSize(64);
                            }
                            renderCellBack(args, selected, isLastChild) {
                                super.renderCellBack(args, selected, isLastChild);
                                const isParent = this.isParent(args.obj);
                                const isChild = this.isChild(args.obj);
                                const expanded = args.viewer.isExpanded(args.obj);
                                if (isParent) {
                                    const ctx = args.canvasContext;
                                    ctx.save();
                                    ctx.fillStyle = "rgba(0, 0, 0, 0.2)";
                                    if (expanded) {
                                        ui.controls.Controls.drawRoundedRect(ctx, args.x, args.y, args.w, args.h, 5, 0, 0, 5);
                                    }
                                    else {
                                        ui.controls.Controls.drawRoundedRect(ctx, args.x, args.y, args.w, args.h, 5, 5, 5, 5);
                                    }
                                    ctx.restore();
                                }
                                else if (isChild) {
                                    const margin = ui.controls.viewers.TREE_RENDERER_GRID_PADDING;
                                    const ctx = args.canvasContext;
                                    ctx.save();
                                    ctx.fillStyle = "rgba(0, 0, 0, 0.2)";
                                    if (isLastChild) {
                                        ui.controls.Controls.drawRoundedRect(ctx, args.x - margin, args.y, args.w + margin, args.h, 0, 5, 5, 0);
                                    }
                                    else {
                                        ui.controls.Controls.drawRoundedRect(ctx, args.x - margin, args.y, args.w + margin, args.h, 0, 0, 0, 0);
                                    }
                                    ctx.restore();
                                }
                            }
                            isParent(obj) {
                                if (obj instanceof pack.AssetPackItem) {
                                    switch (obj.getType()) {
                                        case "atlas":
                                        case "multiatlas":
                                        case "atlasXML":
                                        case "unityAtlas":
                                        case "spritesheet":
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                                return false;
                            }
                            isChild(obj) {
                                return obj instanceof ui.controls.ImageFrame;
                            }
                        }
                        viewers.AssetPackBlocksTreeViewerRenderer = AssetPackBlocksTreeViewerRenderer;
                    })(viewers = pack.viewers || (pack.viewers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var viewers;
                    (function (viewers) {
                        class AssetPackCellRendererProvider {
                            getCellRenderer(element) {
                                if (element instanceof pack.AssetPackItem) {
                                    const type = element.getType();
                                    switch (type) {
                                        case "image":
                                            return new viewers.ImageAssetPackItemCellRenderer();
                                        case "multiatlas":
                                        case "atlas":
                                        case "unityAtlas":
                                        case "atlasXML":
                                        case "spritesheet":
                                            return new ui.controls.viewers.FolderCellRenderer();
                                        default:
                                            break;
                                    }
                                }
                                else if (element instanceof ui.controls.ImageFrame) {
                                    return new ui.controls.viewers.ImageCellRenderer();
                                }
                                return new ui.controls.viewers.EmptyCellRenderer();
                            }
                            preload(element) {
                                return ui.controls.Controls.resolveNothingLoaded();
                            }
                        }
                        viewers.AssetPackCellRendererProvider = AssetPackCellRendererProvider;
                    })(viewers = pack.viewers || (pack.viewers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var viewers;
                    (function (viewers) {
                        class AssetPackContentProvider {
                            getChildren(parent) {
                                if (parent instanceof pack.AssetPack) {
                                    return parent.getItems();
                                }
                                if (parent instanceof pack.AssetPackItem) {
                                    if (parent.getType() === pack.IMAGE_TYPE) {
                                        return [];
                                    }
                                    if (pack.AssetPackUtils.isImageFrameContainer(parent)) {
                                        return pack.AssetPackUtils.getImageFrames(parent);
                                    }
                                }
                                return [];
                            }
                        }
                        viewers.AssetPackContentProvider = AssetPackContentProvider;
                    })(viewers = pack.viewers || (pack.viewers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var viewers;
                    (function (viewers) {
                        class AssetPackLabelProvider {
                            getLabel(obj) {
                                if (obj instanceof pack.AssetPack) {
                                    return obj.getFile().getName();
                                }
                                if (obj instanceof pack.AssetPackItem) {
                                    return obj.getKey();
                                }
                                if (obj instanceof ui.controls.ImageFrame) {
                                    return obj.getName();
                                }
                                if (typeof (obj) === "string") {
                                    return obj;
                                }
                                return "";
                            }
                        }
                        viewers.AssetPackLabelProvider = AssetPackLabelProvider;
                    })(viewers = pack.viewers || (pack.viewers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                class EmptyTreeContentProvider {
                    getRoots(input) {
                        return viewers.EMPTY_ARRAY;
                    }
                    getChildren(parent) {
                        return viewers.EMPTY_ARRAY;
                    }
                }
                viewers.EmptyTreeContentProvider = EmptyTreeContentProvider;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                class ImageCellRenderer {
                    getImage(obj) {
                        return obj;
                    }
                    renderCell(args) {
                        const img = this.getImage(args.obj);
                        if (!img) {
                            controls.DefaultImage.paintEmpty(args.canvasContext, args.x, args.y, args.w, args.h);
                        }
                        else {
                            img.paint(args.canvasContext, args.x, args.y, args.w, args.h, args.center);
                        }
                    }
                    cellHeight(args) {
                        return args.viewer.getCellSize();
                    }
                    preload(obj) {
                        return this.getImage(obj).preload();
                    }
                }
                viewers.ImageCellRenderer = ImageCellRenderer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../../phasereditor2d.ui.controls/viewers/ImageCellRenderer.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
                    var viewers;
                    (function (viewers) {
                        class ImageAssetPackItemCellRenderer extends ui.controls.viewers.ImageCellRenderer {
                            getImage(obj) {
                                const item = obj;
                                const data = item.getData();
                                return pack.AssetPackUtils.getImageFromPackUrl(data.url);
                            }
                        }
                        viewers.ImageAssetPackItemCellRenderer = ImageAssetPackItemCellRenderer;
                    })(viewers = pack.viewers || (pack.viewers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    class ActionManager {
                        constructor(editor) {
                            this._editor = editor;
                        }
                        deleteObjects() {
                            console.log("scene editor delete objects!");
                        }
                    }
                    scene.ActionManager = ActionManager;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
Phaser.Cameras.Scene2D.Camera.prototype.getScreenPoint = function (worldX, worldY) {
    let x = worldX * this.zoom - this.scrollX * this.zoom;
    let y = worldY * this.zoom - this.scrollY * this.zoom;
    return new Phaser.Math.Vector2(x, y);
};
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene_1) {
                    class CameraManager {
                        constructor(editor) {
                            this._editor = editor;
                            this._dragStartPoint = null;
                            const canvas = this._editor.getOverlayLayer().getCanvas();
                            canvas.addEventListener("wheel", e => this.onWheel(e));
                            canvas.addEventListener("mousedown", e => this.onMouseDown(e));
                            canvas.addEventListener("mousemove", e => this.onMouseMove(e));
                            canvas.addEventListener("mouseup", e => this.onMouseUp(e));
                        }
                        getCamera() {
                            return this._editor.getGameScene().getCamera();
                        }
                        onMouseDown(e) {
                            if (e.button === 1) {
                                const camera = this.getCamera();
                                this._dragStartPoint = new Phaser.Math.Vector2(e.offsetX, e.offsetY);
                                this._dragStartCameraScroll = new Phaser.Math.Vector2(camera.scrollX, camera.scrollY);
                                e.preventDefault();
                            }
                        }
                        onMouseMove(e) {
                            if (this._dragStartPoint === null) {
                                return;
                            }
                            const dx = this._dragStartPoint.x - e.offsetX;
                            const dy = this._dragStartPoint.y - e.offsetY;
                            const camera = this.getCamera();
                            camera.scrollX = this._dragStartCameraScroll.x + dx / camera.zoom;
                            camera.scrollY = this._dragStartCameraScroll.y + dy / camera.zoom;
                            this._editor.repaint();
                            e.preventDefault();
                        }
                        onMouseUp(e) {
                            this._dragStartPoint = null;
                            this._dragStartCameraScroll = null;
                        }
                        onWheel(e) {
                            const scene = this._editor.getGameScene();
                            const camera = scene.getCamera();
                            const delta = e.deltaY;
                            const zoomDelta = (delta > 0 ? 0.9 : 1.1);
                            //const pointer = scene.input.activePointer;
                            const point1 = camera.getWorldPoint(e.offsetX, e.offsetY);
                            camera.zoom *= zoomDelta;
                            // update the camera matrix
                            camera.preRender(scene.scale.resolution);
                            const point2 = camera.getWorldPoint(e.offsetX, e.offsetY);
                            const dx = point2.x - point1.x;
                            const dy = point2.y - point1.y;
                            camera.scrollX += -dx;
                            camera.scrollY += -dy;
                            this._editor.repaint();
                        }
                    }
                    scene_1.CameraManager = CameraManager;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    class DropManager {
                        constructor(editor) {
                            this._editor = editor;
                            const canvas = this._editor.getOverlayLayer().getCanvas();
                            canvas.addEventListener("dragover", e => this.onDragOver(e));
                            canvas.addEventListener("drop", e => this.onDragDrop_async(e));
                        }
                        async onDragDrop_async(e) {
                            const dataArray = ui.controls.Controls.getApplicationDragDataAndClean();
                            if (this.acceptsDropDataArray(dataArray)) {
                                e.preventDefault();
                                const sprites = await this._editor.getSceneMaker().createWithDropEvent_async(e, dataArray);
                                this._editor.getUndoManager().add(new scene.undo.AddObjectsOperation(this._editor, sprites));
                                this._editor.refreshOutline();
                                this._editor.setDirty(true);
                                ide.Workbench.getWorkbench().setActivePart(this._editor);
                            }
                        }
                        onDragOver(e) {
                            if (this.acceptsDropDataArray(ui.controls.Controls.getApplicationDragData())) {
                                e.preventDefault();
                            }
                        }
                        acceptsDropData(data) {
                            if (data instanceof editors.pack.AssetPackItem) {
                                if (data.getType() === editors.pack.IMAGE_TYPE) {
                                    return true;
                                }
                            }
                            else if (data instanceof editors.pack.AssetPackImageFrame) {
                                return true;
                            }
                            return false;
                        }
                        acceptsDropDataArray(dataArray) {
                            if (!dataArray) {
                                return false;
                            }
                            for (const item of dataArray) {
                                if (!this.acceptsDropData(item)) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
                    scene.DropManager = DropManager;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
Phaser.GameObjects.GameObject.prototype.getEditorLabel = function () {
    return this.getData("label") || "";
};
Phaser.GameObjects.GameObject.prototype.setEditorLabel = function (label) {
    this.setData("label", label);
};
Phaser.GameObjects.Image.prototype.setEditorTexture = function (key, frame) {
    this.setData("textureKey", key);
    this.setData("textureFrameKey", frame);
};
Phaser.GameObjects.Image.prototype.getEditorTexture = function () {
    return {
        key: this.getData("textureKey"),
        frame: this.getData("textureFrameKey")
    };
};
for (const proto of [
    Phaser.GameObjects.Image.prototype,
    Phaser.GameObjects.TileSprite.prototype,
    Phaser.GameObjects.BitmapText.prototype,
    Phaser.GameObjects.Text.prototype
]) {
    proto.getScreenBounds = function (camera) {
        return phasereditor2d.ui.ide.editors.scene.getScreenBounds(this, camera);
    };
}
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    function getScreenBounds(sprite, camera) {
                        const points = [
                            new Phaser.Math.Vector2(0, 0),
                            new Phaser.Math.Vector2(0, 0),
                            new Phaser.Math.Vector2(0, 0),
                            new Phaser.Math.Vector2(0, 0)
                        ];
                        let w = sprite.width;
                        let h = sprite.height;
                        if (sprite instanceof Phaser.GameObjects.BitmapText) {
                            // the BitmapText.width is considered a displayWidth, it is already multiplied by the scale
                            w = w / sprite.scaleX;
                            h = h / sprite.scaleY;
                        }
                        let flipX = sprite.flipX ? -1 : 1;
                        let flipY = sprite.flipY ? -1 : 1;
                        if (sprite instanceof Phaser.GameObjects.TileSprite) {
                            flipX = 1;
                            flipY = 1;
                        }
                        const ox = sprite.originX;
                        const oy = sprite.originY;
                        const x = -w * ox * flipX;
                        const y = -h * oy * flipY;
                        const tx = sprite.getWorldTransformMatrix();
                        tx.transformPoint(x, y, points[0]);
                        tx.transformPoint(x + w * flipX, y, points[1]);
                        tx.transformPoint(x + w * flipX, y + h * flipY, points[2]);
                        tx.transformPoint(x, y + h * flipY, points[3]);
                        return points.map(p => camera.getScreenPoint(p.x, p.y));
                    }
                    scene.getScreenBounds = getScreenBounds;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var GAME = null;
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    class GameScene extends Phaser.Scene {
                        constructor(editor) {
                            super("ObjectScene");
                            this._editor = editor;
                        }
                        getCamera() {
                            return this.cameras.main;
                        }
                        create() {
                            const camera = this.getCamera();
                            camera.setOrigin(0, 0);
                            camera.backgroundColor = Phaser.Display.Color.ValueToColor("#6e6e6e");
                        }
                    }
                    scene.GameScene = GameScene;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    class OverlayLayer {
                        constructor(editor) {
                            this._editor = editor;
                            this._canvas = document.createElement("canvas");
                            this._canvas.style.position = "absolute";
                        }
                        getCanvas() {
                            return this._canvas;
                        }
                        resetContext() {
                            this._ctx = this._canvas.getContext("2d");
                            this._ctx.imageSmoothingEnabled = false;
                            this._ctx.font = "12px Monospace";
                        }
                        resizeTo() {
                            const parent = this._canvas.parentElement;
                            this._canvas.width = parent.clientWidth | 0;
                            this._canvas.height = parent.clientHeight | 0;
                            this._canvas.style.width = this._canvas.width + "px";
                            this._canvas.style.height = this._canvas.height + "px";
                            this.resetContext();
                        }
                        render() {
                            if (!this._ctx) {
                                this.resetContext();
                            }
                            this.renderGrid();
                            this.renderSelection();
                        }
                        renderSelection() {
                            const ctx = this._ctx;
                            ctx.save();
                            const camera = this._editor.getGameScene().getCamera();
                            for (const obj of this._editor.getSelection()) {
                                if (obj instanceof Phaser.GameObjects.GameObject) {
                                    const points = obj.getScreenBounds(camera);
                                    ctx.strokeStyle = "black";
                                    ctx.lineWidth = 4;
                                    ctx.beginPath();
                                    ctx.moveTo(points[0].x, points[0].y);
                                    ctx.lineTo(points[1].x, points[1].y);
                                    ctx.lineTo(points[2].x, points[2].y);
                                    ctx.lineTo(points[3].x, points[3].y);
                                    ctx.closePath();
                                    ctx.stroke();
                                    ctx.strokeStyle = "#00ff00";
                                    ctx.lineWidth = 2;
                                    ctx.beginPath();
                                    ctx.moveTo(points[0].x, points[0].y);
                                    ctx.lineTo(points[1].x, points[1].y);
                                    ctx.lineTo(points[2].x, points[2].y);
                                    ctx.lineTo(points[3].x, points[3].y);
                                    ctx.closePath();
                                    ctx.stroke();
                                }
                            }
                            ctx.restore();
                        }
                        renderGrid() {
                            const camera = this._editor.getGameScene().getCamera();
                            // parameters from settings
                            const snapEnabled = false;
                            const snapX = 10;
                            const snapY = 10;
                            const borderX = 0;
                            const borderY = 0;
                            const borderWidth = 800;
                            const borderHeight = 600;
                            const ctx = this._ctx;
                            const canvasWidth = this._canvas.width;
                            const canvasHeight = this._canvas.height;
                            ctx.clearRect(0, 0, canvasWidth, canvasHeight);
                            // render grid
                            ctx.strokeStyle = "#aeaeae";
                            ctx.lineWidth = 1;
                            let gapX = 4;
                            let gapY = 4;
                            if (snapEnabled) {
                                gapX = snapX;
                                gapY = snapY;
                            }
                            {
                                for (let i = 1; true; i++) {
                                    const delta = camera.getScreenPoint(gapX * i, gapY * i).subtract(camera.getScreenPoint(0, 0));
                                    if (delta.x > 64 && delta.y > 64) {
                                        gapX = gapX * i;
                                        gapY = gapY * i;
                                        break;
                                    }
                                }
                            }
                            const worldStartPoint = camera.getWorldPoint(0, 0);
                            worldStartPoint.x = Phaser.Math.Snap.Floor(worldStartPoint.x, gapX);
                            worldStartPoint.y = Phaser.Math.Snap.Floor(worldStartPoint.y, gapY);
                            const worldEndPoint = camera.getWorldPoint(canvasWidth, canvasHeight);
                            const grid = (render) => {
                                let worldY = worldStartPoint.y;
                                while (worldY < worldEndPoint.y) {
                                    let point = camera.getScreenPoint(0, worldY);
                                    render.horizontal(worldY, point.y | 0);
                                    worldY += gapY;
                                }
                                let worldX = worldStartPoint.x;
                                while (worldX < worldEndPoint.x) {
                                    let point = camera.getScreenPoint(worldX, 0);
                                    render.vertical(worldX, point.x | 0);
                                    worldX += gapX;
                                }
                            };
                            let labelWidth = 0;
                            ctx.save();
                            ctx.fillStyle = ctx.strokeStyle;
                            // labels
                            grid({
                                horizontal: (worldY, screenY) => {
                                    const w = ctx.measureText(worldY.toString()).width;
                                    labelWidth = Math.max(labelWidth, w + 2);
                                    ctx.save();
                                    ctx.fillStyle = "#000000";
                                    ctx.fillText(worldY.toString(), 0 + 1, screenY + 4 + 1);
                                    ctx.restore();
                                    ctx.fillText(worldY.toString(), 0, screenY + 4);
                                },
                                vertical: (worldX, screenX) => {
                                    if (screenX < labelWidth) {
                                        return;
                                    }
                                    const w = ctx.measureText(worldX.toString()).width;
                                    ctx.save();
                                    ctx.fillStyle = "#000000";
                                    ctx.fillText(worldX.toString(), screenX - w / 2 + 1, 15 + 1);
                                    ctx.restore();
                                    ctx.fillText(worldX.toString(), screenX - w / 2, 15);
                                }
                            });
                            // lines 
                            grid({
                                horizontal: (worldY, screenY) => {
                                    if (screenY < 20) {
                                        return;
                                    }
                                    ctx.beginPath();
                                    ctx.moveTo(labelWidth, screenY);
                                    ctx.lineTo(canvasWidth, screenY);
                                    ctx.stroke();
                                },
                                vertical: (worldX, screenX) => {
                                    if (screenX < labelWidth) {
                                        return;
                                    }
                                    ctx.beginPath();
                                    ctx.moveTo(screenX, 20);
                                    ctx.lineTo(screenX, canvasHeight);
                                    ctx.stroke();
                                }
                            });
                            ctx.restore();
                            {
                                ctx.save();
                                ctx.lineWidth = 2;
                                const a = camera.getScreenPoint(borderX, borderY);
                                const b = camera.getScreenPoint(borderX + borderWidth, borderY + borderHeight);
                                ctx.save();
                                ctx.strokeStyle = "#404040";
                                ctx.strokeRect(a.x + 2, a.y + 2, b.x - a.x, b.y - a.y);
                                ctx.restore();
                                ctx.lineWidth = 1;
                                ctx.strokeRect(a.x, a.y, b.x - a.x, b.y - a.y);
                                ctx.restore();
                            }
                        }
                    }
                    scene.OverlayLayer = OverlayLayer;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    scene.CONTENT_TYPE_SCENE = "Scene";
                    class SceneContentTypeResolver {
                        async computeContentType(file) {
                            if (file.getExtension() === "scene") {
                                return scene.CONTENT_TYPE_SCENE;
                            }
                            return phasereditor2d.core.CONTENT_TYPE_ANY;
                        }
                    }
                    scene.SceneContentTypeResolver = SceneContentTypeResolver;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var outline;
                    (function (outline) {
                        class SceneEditorOutlineLabelProvider {
                            getLabel(obj) {
                                if (obj instanceof Phaser.GameObjects.GameObject) {
                                    return obj.getEditorLabel();
                                }
                                if (obj instanceof Phaser.GameObjects.DisplayList) {
                                    return "Display List";
                                }
                                return "" + obj;
                            }
                        }
                        class SceneEditorOutlineContentProvider {
                            getRoots(input) {
                                const editor = input;
                                const displayList = editor.getGameScene().sys.displayList;
                                if (displayList) {
                                    return [displayList];
                                }
                                return [];
                            }
                            getChildren(parent) {
                                if (parent instanceof Phaser.GameObjects.DisplayList) {
                                    return parent.getChildren();
                                }
                                return [];
                            }
                        }
                        class SceneEditorOutlineRendererProvider {
                            constructor(editor) {
                                this._editor = editor;
                                this._assetRendererProvider = new editors.pack.viewers.AssetPackCellRendererProvider();
                                this._packs = null;
                            }
                            getCellRenderer(element) {
                                if (this._packs !== null) {
                                    if (element instanceof Phaser.GameObjects.Image) {
                                        return new outline.GameObjectCellRenderer(this._packs);
                                    }
                                    else if (element instanceof Phaser.GameObjects.DisplayList) {
                                        return new ui.controls.viewers.IconImageCellRenderer(ui.controls.Controls.getIcon(ide.ICON_FOLDER));
                                    }
                                }
                                return new ui.controls.viewers.EmptyCellRenderer(false);
                            }
                            async preload(element) {
                                if (this._packs === null) {
                                    return editors.pack.AssetPackUtils.getAllPacks().then(packs => {
                                        this._packs = packs;
                                        return ui.controls.PreloadResult.RESOURCES_LOADED;
                                    });
                                }
                                return ui.controls.Controls.resolveNothingLoaded();
                            }
                        }
                        class SceneEditorOutlineProvider extends ide.EditorViewerProvider {
                            constructor(editor) {
                                super();
                                this._editor = editor;
                            }
                            getContentProvider() {
                                return new SceneEditorOutlineContentProvider();
                            }
                            getLabelProvider() {
                                return new SceneEditorOutlineLabelProvider();
                            }
                            getCellRendererProvider() {
                                return new SceneEditorOutlineRendererProvider(this._editor);
                            }
                            getTreeViewerRenderer(viewer) {
                                return new ui.controls.viewers.TreeViewerRenderer(viewer, 48);
                            }
                            getPropertySectionProvider() {
                                return this._editor.getPropertyProvider();
                            }
                            getInput() {
                                return this._editor;
                            }
                            preload() {
                                return;
                            }
                            onViewerSelectionChanged(selection) {
                                this._editor.setSelection(selection, false);
                                this._editor.repaint();
                            }
                        }
                        outline.SceneEditorOutlineProvider = SceneEditorOutlineProvider;
                    })(outline = scene.outline || (scene.outline = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../EditorViewerProvider.ts" />
/// <reference path="./outline/SceneEditorOutlineProvider.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var io = phasereditor2d.core.io;
                    class SceneEditorFactory extends ide.EditorFactory {
                        constructor() {
                            super("phasereditor2d.SceneEditorFactory");
                        }
                        acceptInput(input) {
                            if (input instanceof io.FilePath) {
                                const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                                return contentType === scene.CONTENT_TYPE_SCENE;
                            }
                            return false;
                        }
                        createEditor() {
                            return new SceneEditor();
                        }
                    }
                    class SceneEditor extends ide.FileEditor {
                        constructor() {
                            super("phasereditor2d.SceneEditor");
                            this._blocksProvider = new scene.blocks.SceneEditorBlocksProvider();
                            this._outlineProvider = new scene.outline.SceneEditorOutlineProvider(this);
                            this._propertyProvider = new scene.properties.SceneEditorSectionProvider();
                        }
                        static getFactory() {
                            return new SceneEditorFactory();
                        }
                        async save() {
                            const writer = new scene.json.SceneWriter(this.getGameScene());
                            const data = writer.toJSON();
                            const content = JSON.stringify(data, null, 4);
                            const ok = await ide.FileUtils.setFileString(this.getInput(), content);
                            if (ok) {
                                this.setDirty(false);
                            }
                        }
                        createPart() {
                            this.setLayoutChildren(false);
                            this._gameCanvas = document.createElement("canvas");
                            this._gameCanvas.style.position = "absolute";
                            this.getElement().appendChild(this._gameCanvas);
                            this._overlayLayer = new scene.OverlayLayer(this);
                            this.getElement().appendChild(this._overlayLayer.getCanvas());
                            // create game scene
                            this._gameScene = new scene.GameScene(this);
                            this._game = new Phaser.Game({
                                type: Phaser.WEBGL,
                                canvas: this._gameCanvas,
                                scale: {
                                    mode: Phaser.Scale.NONE
                                },
                                render: {
                                    pixelArt: true,
                                    transparent: true
                                },
                                audio: {
                                    noAudio: true
                                },
                                scene: this._gameScene,
                            });
                            this._sceneRead = false;
                            this._gameBooted = false;
                            this._game.config.postBoot = () => {
                                this.onGameBoot();
                            };
                            // init managers and factories
                            this._sceneMaker = new scene.SceneMaker(this);
                            this._dropManager = new scene.DropManager(this);
                            this._cameraManager = new scene.CameraManager(this);
                            this._selectionManager = new scene.SelectionManager(this);
                            this._actionManager = new scene.ActionManager(this);
                        }
                        async setInput(file) {
                            super.setInput(file);
                            if (this._gameBooted) {
                                await this.readScene();
                            }
                        }
                        async readScene() {
                            this._sceneRead = true;
                            try {
                                const file = this.getInput();
                                const content = await ide.FileUtils.getFileString(file);
                                const data = JSON.parse(content);
                                const parser = new scene.json.SceneParser(this.getGameScene());
                                await parser.createSceneCache_async(data);
                                await parser.createScene(data);
                            }
                            catch (e) {
                                alert(e.message);
                                throw e;
                            }
                        }
                        getActionManager() {
                            return this._actionManager;
                        }
                        getSelectionManager() {
                            return this._selectionManager;
                        }
                        getOverlayLayer() {
                            return this._overlayLayer;
                        }
                        getGameCanvas() {
                            return this._gameCanvas;
                        }
                        getGameScene() {
                            return this._gameScene;
                        }
                        getGame() {
                            return this._game;
                        }
                        getSceneMaker() {
                            return this._sceneMaker;
                        }
                        layout() {
                            super.layout();
                            if (!this._gameBooted) {
                                return;
                            }
                            this._overlayLayer.resizeTo();
                            const parent = this.getElement();
                            const w = parent.clientWidth;
                            const h = parent.clientHeight;
                            this._game.scale.resize(w, h);
                            this._gameScene.scale.resize(w, h);
                            this._gameScene.getCamera().setSize(w, h);
                            this.repaint();
                        }
                        getPropertyProvider() {
                            return this._propertyProvider;
                        }
                        getEditorViewerProvider(key) {
                            switch (key) {
                                case ide.views.blocks.BlocksView.EDITOR_VIEWER_PROVIDER_KEY:
                                    return this._blocksProvider;
                                case ide.views.outline.OutlineView.EDITOR_VIEWER_PROVIDER_KEY:
                                    return this._outlineProvider;
                                default:
                                    break;
                            }
                            return null;
                        }
                        getOutlineProvider() {
                            return this._outlineProvider;
                        }
                        refreshOutline() {
                            this._outlineProvider.repaint();
                        }
                        async onGameBoot() {
                            this._gameBooted = true;
                            if (!this._sceneRead) {
                                await this.readScene();
                            }
                            this.layout();
                            this.refreshOutline();
                            // for some reason, we should do this after a time, or the game is not stopped well.
                            setTimeout(() => this._game.loop.stop(), 500);
                        }
                        repaint() {
                            if (!this._gameBooted) {
                                return;
                            }
                            this._game.loop.tick();
                            this._overlayLayer.render();
                        }
                    }
                    scene.SceneEditor = SceneEditor;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene_2) {
                    class SceneMaker {
                        constructor(editor) {
                            this._editor = editor;
                        }
                        createObject(objData) {
                            const reader = new scene_2.json.SceneParser(this._editor.getGameScene());
                            reader.createObject(objData);
                        }
                        async createWithDropEvent_async(e, dropDataArray) {
                            const scene = this._editor.getGameScene();
                            const nameMaker = new ide.utils.NameMaker(obj => {
                                return obj.getEditorLabel();
                            });
                            nameMaker.update(scene.sys.displayList.getChildren());
                            const worldPoint = scene.getCamera().getWorldPoint(e.offsetX, e.offsetY);
                            const x = worldPoint.x;
                            const y = worldPoint.y;
                            const parser = new scene_2.json.SceneParser(scene);
                            for (const data of dropDataArray) {
                                await parser.addToCache_async(data);
                            }
                            const sprites = [];
                            for (const data of dropDataArray) {
                                if (data instanceof editors.pack.AssetPackImageFrame) {
                                    const sprite = scene.add.image(x, y, data.getPackItem().getKey(), data.getName());
                                    sprite.setEditorLabel(nameMaker.makeName(data.getName()));
                                    sprite.setEditorTexture(data.getPackItem().getKey(), data.getName());
                                    sprites.push(sprite);
                                }
                                else if (data instanceof editors.pack.AssetPackItem) {
                                    switch (data.getType()) {
                                        case editors.pack.IMAGE_TYPE: {
                                            const sprite = scene.add.image(x, y, data.getKey());
                                            sprite.setEditorLabel(nameMaker.makeName(data.getKey()));
                                            sprite.setEditorTexture(data.getKey(), null);
                                            sprites.push(sprite);
                                            break;
                                        }
                                    }
                                }
                            }
                            for (const sprite of sprites) {
                                scene_2.json.SceneParser.setNewId(sprite);
                                scene_2.json.SceneParser.initSprite(sprite);
                            }
                            this._editor.setSelection(sprites);
                            this._editor.repaint();
                            return sprites;
                        }
                    }
                    scene_2.SceneMaker = SceneMaker;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene_3) {
                    class SelectionManager {
                        constructor(editor) {
                            this._editor = editor;
                            const canvas = this._editor.getOverlayLayer().getCanvas();
                            canvas.addEventListener("click", e => this.onMouseClick(e));
                            this._editor.addEventListener(ui.controls.EVENT_SELECTION_CHANGED, e => this.updateOutlineSelection());
                        }
                        cleanSelection() {
                            this._editor.setSelection(this._editor.getSelection().filter(obj => {
                                if (obj instanceof Phaser.GameObjects.GameObject) {
                                    return this._editor.getGameScene().sys.displayList.exists(obj);
                                }
                                return true;
                            }));
                        }
                        updateOutlineSelection() {
                            const provider = this._editor.getOutlineProvider();
                            provider.setSelection(this._editor.getSelection(), true, false);
                            provider.repaint();
                        }
                        onMouseClick(e) {
                            const result = this.hitTestOfActivePointer();
                            let next = [];
                            if (result) {
                                const current = this._editor.getSelection();
                                const selected = result.pop();
                                if (e.ctrlKey || e.metaKey) {
                                    if (new Set(current).has(selected)) {
                                        next = current.filter(obj => obj !== selected);
                                    }
                                    else {
                                        next = current;
                                        next.push(selected);
                                    }
                                }
                                else {
                                    next = [selected];
                                }
                            }
                            this._editor.setSelection(next);
                            this._editor.repaint();
                        }
                        hitTestOfActivePointer() {
                            const scene = this._editor.getGameScene();
                            const input = scene.input;
                            // const real = input["real_hitTest"];
                            // const fake = input["hitTest"];
                            // input["hitTest"] = real;
                            const result = input.hitTestPointer(scene.input.activePointer);
                            // input["hitTest"] = fake;
                            return result;
                        }
                    }
                    scene_3.SelectionManager = SelectionManager;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var properties;
            (function (properties) {
                class PropertySectionProvider {
                }
                properties.PropertySectionProvider = PropertySectionProvider;
            })(properties = controls.properties || (controls.properties = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../../phasereditor2d.ui.controls/properties/PropertySectionProvider.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var blocks;
                    (function (blocks) {
                        class SceneEditorBlockPropertyProvider extends ui.controls.properties.PropertySectionProvider {
                            addSections(page, sections) {
                                sections.push(new editors.pack.properties.AssetPackItemSection(page));
                                sections.push(new editors.pack.properties.ImageSection(page));
                                sections.push(new editors.pack.properties.ManyImageSection(page));
                            }
                        }
                        blocks.SceneEditorBlockPropertyProvider = SceneEditorBlockPropertyProvider;
                    })(blocks = scene.blocks || (scene.blocks = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../pack/viewers/AssetPackContentProvider.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var blocks;
                    (function (blocks) {
                        const SCENE_EDITOR_BLOCKS_PACK_ITEM_TYPES = new Set(["image", "atlas", "atlasXML", "multiatlas", "unityAtlas", "spritesheet"]);
                        class SceneEditorBlocksContentProvider extends editors.pack.viewers.AssetPackContentProvider {
                            constructor(packs) {
                                super();
                                this._items = packs
                                    .flatMap(pack => pack.getItems())
                                    .filter(item => SCENE_EDITOR_BLOCKS_PACK_ITEM_TYPES.has(item.getType()));
                            }
                            getItems() {
                                return this._items;
                            }
                            getRoots(input) {
                                return this._items;
                            }
                            getChildren(parent) {
                                if (typeof (parent) === "string") {
                                    switch (parent) {
                                        case editors.pack.ATLAS_TYPE:
                                        case editors.pack.ATLAS_XML_TYPE:
                                        case editors.pack.MULTI_ATLAS_TYPE:
                                        case editors.pack.UNITY_ATLAS_TYPE:
                                            return this._items.filter(item => editors.pack.AssetPackUtils.isAtlasPackItem(item));
                                    }
                                    return this._items.filter(item => item.getType() === parent);
                                }
                                return super.getChildren(parent);
                            }
                        }
                        blocks.SceneEditorBlocksContentProvider = SceneEditorBlocksContentProvider;
                    })(blocks = scene.blocks || (scene.blocks = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var blocks;
                    (function (blocks) {
                        class SceneEditorTreeRendererProvider extends editors.pack.viewers.AssetPackBlocksTreeViewerRenderer {
                            constructor(viewer) {
                                super(viewer);
                                this.setSections([
                                    "image",
                                    "atlas",
                                    "spritesheet"
                                ]);
                            }
                        }
                        class SceneEditorBlocksProvider extends ide.EditorViewerProvider {
                            async preload() {
                                if (this._contentProvider) {
                                    return;
                                }
                                const packs = await editors.pack.AssetPackUtils.getAllPacks();
                                this._contentProvider = new blocks.SceneEditorBlocksContentProvider(packs);
                                await editors.pack.AssetPackUtils.preloadAssetPackItems(this._contentProvider.getItems());
                            }
                            getContentProvider() {
                                return this._contentProvider;
                            }
                            getLabelProvider() {
                                return new editors.pack.viewers.AssetPackLabelProvider();
                            }
                            getCellRendererProvider() {
                                return new editors.pack.viewers.AssetPackCellRendererProvider();
                            }
                            getTreeViewerRenderer(viewer) {
                                return new SceneEditorTreeRendererProvider(viewer);
                            }
                            getPropertySectionProvider() {
                                return new blocks.SceneEditorBlockPropertyProvider();
                            }
                            getInput() {
                                return this;
                            }
                        }
                        blocks.SceneEditorBlocksProvider = SceneEditorBlocksProvider;
                    })(blocks = scene.blocks || (scene.blocks = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    function isSceneScope(args) {
                        return args.activePart instanceof scene.SceneEditor ||
                            args.activePart instanceof ide.views.outline.OutlineView && args.activeEditor instanceof scene.SceneEditor;
                    }
                    class SceneEditorCommands {
                        static init() {
                            const manager = ide.Workbench.getWorkbench().getCommandManager();
                            manager.addHandlerHelper(ide.CMD_DELETE, args => isSceneScope(args), args => {
                                const editor = args.activeEditor;
                                editor.getActionManager().deleteObjects();
                            });
                        }
                    }
                    scene.SceneEditorCommands = SceneEditorCommands;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var json;
                    (function (json) {
                        class ImageComponent {
                            static write(sprite, data) {
                                json.ObjectComponent.write(sprite, data);
                                json.VariableComponent.write(sprite, data);
                                json.TransformComponent.write(sprite, data);
                                json.TextureComponent.write(sprite, data);
                            }
                            static read(sprite, data) {
                                json.ObjectComponent.read(sprite, data);
                                json.VariableComponent.read(sprite, data);
                                json.TransformComponent.read(sprite, data);
                                json.TextureComponent.read(sprite, data);
                            }
                        }
                        json.ImageComponent = ImageComponent;
                    })(json = scene.json || (scene.json = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var json;
                    (function (json) {
                        var write = phasereditor2d.core.json.write;
                        var read = phasereditor2d.core.json.read;
                        class ObjectComponent {
                            static write(sprite, data) {
                                write(data, "name", sprite.name);
                                write(data, "type", sprite.type);
                            }
                            static read(sprite, data) {
                                sprite.name = read(data, "name");
                            }
                        }
                        json.ObjectComponent = ObjectComponent;
                    })(json = scene.json || (scene.json = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
Phaser.GameObjects.Image.prototype.writeJSON = function (data) {
    data.type = "Image";
    phasereditor2d.ui.ide.editors.scene.json.ImageComponent.write(this, data);
};
Phaser.GameObjects.Image.prototype.readJSON = function (data) {
    phasereditor2d.ui.ide.editors.scene.json.ImageComponent.read(this, data);
};
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene_4) {
                    var json;
                    (function (json) {
                        let SPRITE_ID = 0;
                        class SceneParser {
                            constructor(scene) {
                                this._scene = scene;
                            }
                            createScene(data) {
                                for (const objData of data.displayList) {
                                    this.createObject(objData);
                                }
                            }
                            async createSceneCache_async(data) {
                                for (const objData of data.displayList) {
                                    const type = objData.type;
                                    switch (type) {
                                        case "Image": {
                                            const key = objData[json.TextureComponent.textureKey];
                                            const finder = await editors.pack.AssetFinder.create();
                                            const item = finder.findAssetPackItem(key);
                                            if (item) {
                                                await this.addToCache_async(item);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            async addToCache_async(data) {
                                let imageFrameContainerPackItem = null;
                                if (data instanceof editors.pack.AssetPackItem) {
                                    if (data.getType() === editors.pack.IMAGE_TYPE) {
                                        imageFrameContainerPackItem = data;
                                    }
                                    else if (editors.pack.AssetPackUtils.isImageFrameContainer(data)) {
                                        imageFrameContainerPackItem = data;
                                    }
                                }
                                else if (data instanceof editors.pack.AssetPackImageFrame) {
                                    imageFrameContainerPackItem = data.getPackItem();
                                }
                                if (imageFrameContainerPackItem !== null) {
                                    const parser = editors.pack.AssetPackUtils.getImageFrameParser(imageFrameContainerPackItem);
                                    await parser.preload();
                                    parser.addToPhaserCache(this._scene.game);
                                }
                            }
                            createObject(data) {
                                const type = data.type;
                                let sprite = null;
                                switch (type) {
                                    case "Image":
                                        sprite = this._scene.add.image(0, 0, "");
                                        break;
                                }
                                if (sprite) {
                                    sprite.readJSON(data);
                                    SceneParser.setNewId(sprite);
                                    SceneParser.initSprite(sprite);
                                }
                                return sprite;
                            }
                            static initSprite(sprite) {
                                sprite.setInteractive();
                            }
                            static setNewId(sprite) {
                                sprite.name = (SPRITE_ID++).toString();
                            }
                        }
                        json.SceneParser = SceneParser;
                    })(json = scene_4.json || (scene_4.json = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene_5) {
                    var json;
                    (function (json_1) {
                        class SceneWriter {
                            constructor(scene) {
                                this._scene = scene;
                            }
                            toJSON() {
                                const sceneData = {
                                    displayList: []
                                };
                                for (const obj of this._scene.sys.displayList.getChildren()) {
                                    const objData = {};
                                    obj.writeJSON(objData);
                                    sceneData.displayList.push(objData);
                                }
                                return sceneData;
                            }
                            toString() {
                                const json = this.toJSON();
                                return JSON.stringify(json);
                            }
                        }
                        json_1.SceneWriter = SceneWriter;
                    })(json = scene_5.json || (scene_5.json = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var json;
                    (function (json) {
                        var write = phasereditor2d.core.json.write;
                        var read = phasereditor2d.core.json.read;
                        class TextureComponent {
                            static write(sprite, data) {
                                const texture = sprite.getEditorTexture();
                                write(data, this.textureKey, texture.key);
                                write(data, this.frameKey, texture.frame);
                            }
                            static read(sprite, data) {
                                const key = read(data, this.textureKey);
                                const frame = read(data, this.frameKey);
                                sprite.setEditorTexture(key, frame);
                                sprite.setTexture(key, frame);
                            }
                        }
                        TextureComponent.textureKey = "textureKey";
                        TextureComponent.frameKey = "frameKey";
                        json.TextureComponent = TextureComponent;
                    })(json = scene.json || (scene.json = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var json;
                    (function (json) {
                        var write = phasereditor2d.core.json.write;
                        var read = phasereditor2d.core.json.read;
                        class TransformComponent {
                            static write(sprite, data) {
                                write(data, "x", sprite.x, 0);
                                write(data, "y", sprite.y, 0);
                                write(data, "scaleX", sprite.scaleX, 1);
                                write(data, "scaleY", sprite.scaleY, 1);
                                write(data, "angle", sprite.angle, 0);
                            }
                            static read(sprite, data) {
                                sprite.x = read(data, "x", 0);
                                sprite.y = read(data, "y", 0);
                                sprite.scaleX = read(data, "scaleX", 1);
                                sprite.scaleY = read(data, "scaleY", 1);
                                sprite.angle = read(data, "angle", 0);
                            }
                        }
                        json.TransformComponent = TransformComponent;
                    })(json = scene.json || (scene.json = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var json;
                    (function (json) {
                        var write = phasereditor2d.core.json.write;
                        var read = phasereditor2d.core.json.read;
                        class VariableComponent {
                            static write(sprite, data) {
                                write(data, "label", sprite.getEditorLabel());
                            }
                            static read(sprite, data) {
                                sprite.setEditorLabel(read(data, "label"));
                            }
                        }
                        json.VariableComponent = VariableComponent;
                    })(json = scene.json || (scene.json = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var outline;
                    (function (outline) {
                        class GameObjectCellRenderer {
                            constructor(packs) {
                                this._packs = packs;
                            }
                            renderCell(args) {
                                const sprite = args.obj;
                                if (sprite instanceof Phaser.GameObjects.Image) {
                                    const { key, frame } = sprite.getEditorTexture();
                                    const finder = new editors.pack.AssetFinder(this._packs);
                                    const img = finder.getAssetPackItemImage(key, frame);
                                    if (img) {
                                        img.paint(args.canvasContext, args.x, args.y, args.w, args.h, false);
                                    }
                                }
                            }
                            cellHeight(args) {
                                if (args.obj instanceof Phaser.GameObjects.Image) {
                                    return args.viewer.getCellSize();
                                }
                                return ui.controls.ROW_HEIGHT;
                            }
                            preload(obj) {
                                return ui.controls.Controls.resolveNothingLoaded();
                            }
                        }
                        outline.GameObjectCellRenderer = GameObjectCellRenderer;
                    })(outline = scene.outline || (scene.outline = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var properties;
                    (function (properties) {
                        class SceneSection extends ui.controls.properties.PropertySection {
                        }
                        properties.SceneSection = SceneSection;
                    })(properties = scene.properties || (scene.properties = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./SceneSection.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var properties;
                    (function (properties) {
                        class OriginSection extends properties.SceneSection {
                            constructor(page) {
                                super(page, "SceneEditor.OriginSection", "Origin", false);
                            }
                            createForm(parent) {
                                const comp = this.createGridElement(parent, 5);
                                // Position
                                {
                                    this.createLabel(comp, "Origin");
                                    // X
                                    {
                                        this.createLabel(comp, "X");
                                        const text = this.createText(comp);
                                        this.addUpdater(() => {
                                            text.value = this.flatValues_Number(this.getSelection().map(obj => obj.originX));
                                        });
                                    }
                                    // y
                                    {
                                        this.createLabel(comp, "Y");
                                        const text = this.createText(comp);
                                        this.addUpdater(() => {
                                            text.value = this.flatValues_Number(this.getSelection().map(obj => obj.originY));
                                        });
                                    }
                                }
                            }
                            canEdit(obj, n) {
                                return obj instanceof Phaser.GameObjects.Image;
                            }
                            canEditNumber(n) {
                                return n > 0;
                            }
                        }
                        properties.OriginSection = OriginSection;
                    })(properties = scene.properties || (scene.properties = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var properties;
            (function (properties) {
                class PropertySectionPane extends controls.Control {
                    constructor(page, section) {
                        super();
                        this._page = page;
                        this._section = section;
                        this.addClass("PropertySectionPane");
                    }
                    createOrUpdateWithSelection() {
                        if (!this._formArea) {
                            this._titleArea = document.createElement("div");
                            this._titleArea.classList.add("PropertyTitleArea");
                            this._expandBtn = document.createElement("div");
                            this._expandBtn.classList.add("expandBtn", "expanded");
                            this._expandBtn.addEventListener("mouseup", () => this.toggleSection());
                            this._titleArea.appendChild(this._expandBtn);
                            const label = document.createElement("label");
                            label.innerText = this._section.getTitle();
                            label.addEventListener("mouseup", () => this.toggleSection());
                            this._titleArea.appendChild(label);
                            this._formArea = document.createElement("div");
                            this._formArea.classList.add("PropertyFormArea");
                            this._section.create(this._formArea);
                            this.getElement().appendChild(this._titleArea);
                            this.getElement().appendChild(this._formArea);
                        }
                        this._section.updateWithSelection();
                    }
                    isExpanded() {
                        return this._expandBtn.classList.contains("expanded");
                    }
                    toggleSection() {
                        if (this.isExpanded()) {
                            this._expandBtn.classList.remove("expanded");
                            this._expandBtn.classList.add("collapsed");
                            this._formArea.style.display = "none";
                        }
                        else {
                            this._expandBtn.classList.add("expanded");
                            this._expandBtn.classList.remove("collapsed");
                            this._formArea.style.display = "initial";
                        }
                        this._page.updateExpandStatus();
                        this.getContainer().dispatchLayoutEvent();
                    }
                    getSection() {
                        return this._section;
                    }
                    getFormArea() {
                        return this._formArea;
                    }
                }
                class PropertyPage extends controls.Control {
                    constructor() {
                        super("div");
                        this.addClass("PropertyPage");
                        this._sectionPanes = [];
                        this._sectionPaneMap = new Map();
                        this._selection = [];
                    }
                    build() {
                        if (this._sectionProvider) {
                            const list = [];
                            this._sectionProvider.addSections(this, list);
                            for (const section of list) {
                                if (!this._sectionPaneMap.has(section.getId())) {
                                    const pane = new PropertySectionPane(this, section);
                                    this.add(pane);
                                    this._sectionPaneMap.set(section.getId(), pane);
                                    this._sectionPanes.push(pane);
                                }
                            }
                            this.updateWithSelection();
                        }
                        else {
                            for (const pane of this._sectionPanes) {
                                pane.getElement().style.display = "none";
                            }
                        }
                    }
                    updateWithSelection() {
                        const n = this._selection.length;
                        for (const pane of this._sectionPanes) {
                            const section = pane.getSection();
                            let show = false;
                            if (section.canEditNumber(n)) {
                                show = true;
                                for (const obj of this._selection) {
                                    if (!section.canEdit(obj, n)) {
                                        show = false;
                                        break;
                                    }
                                }
                            }
                            if (show) {
                                pane.getElement().style.display = "grid";
                                pane.createOrUpdateWithSelection();
                            }
                            else {
                                pane.getElement().style.display = "none";
                            }
                        }
                        this.updateExpandStatus();
                    }
                    updateExpandStatus() {
                        let templateRows = "";
                        for (const pane of this._sectionPanes) {
                            if (pane.style.display !== "none") {
                                pane.createOrUpdateWithSelection();
                                if (pane.isExpanded()) {
                                    templateRows += " " + (pane.getSection().isFillSpace() ? "1fr" : "min-content");
                                }
                                else {
                                    templateRows += " min-content";
                                }
                            }
                        }
                        this.getElement().style.gridTemplateRows = templateRows + " ";
                    }
                    getSelection() {
                        return this._selection;
                    }
                    setSelection(sel) {
                        this._selection = sel;
                        this.updateWithSelection();
                    }
                    setSectionProvider(provider) {
                        this._sectionProvider = provider;
                        this.build();
                    }
                    getSectionProvider() {
                        return this._sectionProvider;
                    }
                }
                properties.PropertyPage = PropertyPage;
            })(properties = controls.properties || (controls.properties = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../../phasereditor2d.ui.controls/properties/PropertyPage.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var properties;
                    (function (properties) {
                        class SceneEditorSectionProvider extends ui.controls.properties.PropertySectionProvider {
                            addSections(page, sections) {
                                sections.push(new properties.VariableSection(page));
                                sections.push(new properties.TransformSection(page));
                                sections.push(new properties.OriginSection(page));
                                sections.push(new properties.TextureSection(page));
                            }
                        }
                        properties.SceneEditorSectionProvider = SceneEditorSectionProvider;
                    })(properties = scene.properties || (scene.properties = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var properties;
                    (function (properties) {
                        class TextureSection extends properties.SceneSection {
                            constructor(page) {
                                super(page, "SceneEditor.TextureSection", "Texture", true);
                            }
                            createForm(parent) {
                                parent.classList.add("ImagePreviewFormArea", "PreviewBackground");
                                const imgControl = new ui.controls.ImageControl(ide.IMG_SECTION_PADDING);
                                this.getPage().addEventListener(ui.controls.EVENT_CONTROL_LAYOUT, (e) => {
                                    imgControl.resizeTo();
                                });
                                parent.appendChild(imgControl.getElement());
                                setTimeout(() => imgControl.resizeTo(), 1);
                                this.addUpdater(async () => {
                                    const obj = this.getSelection()[0];
                                    const { key, frame } = obj.getEditorTexture();
                                    const finder = await editors.pack.AssetFinder.create();
                                    const img = finder.getAssetPackItemImage(key, frame);
                                    imgControl.setImage(img);
                                    setTimeout(() => imgControl.resizeTo(), 1);
                                });
                            }
                            canEdit(obj) {
                                return obj instanceof Phaser.GameObjects.Image;
                            }
                            canEditNumber(n) {
                                return n === 1;
                            }
                        }
                        properties.TextureSection = TextureSection;
                    })(properties = scene.properties || (scene.properties = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var properties;
                    (function (properties) {
                        class TransformSection extends properties.SceneSection {
                            constructor(page) {
                                super(page, "SceneEditor.TransformSection", "Transform", false);
                            }
                            createForm(parent) {
                                const comp = this.createGridElement(parent, 5);
                                // Position
                                {
                                    this.createLabel(comp, "Position");
                                    // X
                                    {
                                        this.createLabel(comp, "X");
                                        const text = this.createText(comp);
                                        this.addUpdater(() => {
                                            text.value = this.flatValues_Number(this.getSelection().map(obj => obj.x));
                                        });
                                    }
                                    // y
                                    {
                                        this.createLabel(comp, "Y");
                                        const text = this.createText(comp);
                                        this.addUpdater(() => {
                                            text.value = this.flatValues_Number(this.getSelection().map(obj => obj.y));
                                        });
                                    }
                                }
                                // Scale
                                {
                                    this.createLabel(comp, "Scale");
                                    // X
                                    {
                                        this.createLabel(comp, "X");
                                        const text = this.createText(comp);
                                        this.addUpdater(() => {
                                            text.value = this.flatValues_Number(this.getSelection().map(obj => obj.scaleX));
                                        });
                                    }
                                    // y
                                    {
                                        this.createLabel(comp, "Y");
                                        const text = this.createText(comp);
                                        this.addUpdater(() => {
                                            text.value = this.flatValues_Number(this.getSelection().map(obj => obj.scaleY));
                                        });
                                    }
                                }
                                // Angle
                                {
                                    this.createLabel(comp, "Angle").style.gridColumnStart = "span 2";
                                    const text = this.createText(comp);
                                    this.addUpdater(() => {
                                        text.value = this.flatValues_Number(this.getSelection().map(obj => obj.angle));
                                    });
                                    this.createLabel(comp, "").style.gridColumnStart = "span 2";
                                }
                            }
                            canEdit(obj, n) {
                                return obj instanceof Phaser.GameObjects.Image;
                            }
                            canEditNumber(n) {
                                return n > 0;
                            }
                        }
                        properties.TransformSection = TransformSection;
                    })(properties = scene.properties || (scene.properties = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./SceneSection.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var properties;
                    (function (properties) {
                        class VariableSection extends properties.SceneSection {
                            constructor(page) {
                                super(page, "SceneEditor.VariableSection", "Variable", false);
                            }
                            createForm(parent) {
                                const comp = this.createGridElement(parent, 2);
                                {
                                    // Name
                                    this.createLabel(comp, "Name");
                                    const text = this.createText(comp);
                                    this.addUpdater(() => {
                                        text.value = this.flatValues_StringJoin(this.getSelection().map(obj => obj.getEditorLabel()));
                                    });
                                }
                            }
                            canEdit(obj, n) {
                                return obj instanceof Phaser.GameObjects.GameObject;
                            }
                            canEditNumber(n) {
                                return n === 1;
                            }
                        }
                        properties.VariableSection = VariableSection;
                    })(properties = scene.properties || (scene.properties = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var undo;
            (function (undo) {
                class Operation {
                }
                undo.Operation = Operation;
            })(undo = ide.undo || (ide.undo = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../undo/Operation.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var undo;
                    (function (undo) {
                        class SceneEditorOperation extends ide.undo.Operation {
                            constructor(editor) {
                                super();
                                this._editor = editor;
                            }
                        }
                        undo.SceneEditorOperation = SceneEditorOperation;
                    })(undo = scene.undo || (scene.undo = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./SceneEditorOperation.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var scene;
                (function (scene) {
                    var undo;
                    (function (undo) {
                        class AddObjectsOperation extends undo.SceneEditorOperation {
                            constructor(editor, objects) {
                                super(editor);
                                this._dataList = objects.map(obj => {
                                    const data = {};
                                    obj.writeJSON(data);
                                    return data;
                                });
                            }
                            undo() {
                                const displayList = this._editor.getGameScene().sys.displayList;
                                for (const data of this._dataList) {
                                    const obj = displayList.getByName(data.name);
                                    if (obj) {
                                        obj.destroy();
                                    }
                                    else {
                                        console.warn(`Undo: object with id=${data.name} not found.`);
                                    }
                                }
                                this._editor.getSelectionManager().cleanSelection();
                                this.updateEditor();
                            }
                            redo() {
                                const maker = this._editor.getSceneMaker();
                                for (const data of this._dataList) {
                                    maker.createObject(data);
                                }
                                this.updateEditor();
                            }
                            updateEditor() {
                                this._editor.setDirty(true);
                                this._editor.repaint();
                                this._editor.refreshOutline();
                            }
                        }
                        undo.AddObjectsOperation = AddObjectsOperation;
                    })(undo = scene.undo || (scene.undo = {}));
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var properties;
            (function (properties) {
                class FilteredViewerInPropertySection extends ui.controls.viewers.FilteredViewer {
                    constructor(page, viewer, ...classList) {
                        super(viewer, ...classList);
                        this.setHandlePosition(false);
                        this.style.position = "relative";
                        this.style.height = "100%";
                        this.resizeTo();
                        page.addEventListener(ui.controls.EVENT_CONTROL_LAYOUT, (e) => {
                            this.resizeTo();
                        });
                    }
                    resizeTo() {
                        setTimeout(() => {
                            const parent = this.getElement().parentElement;
                            if (parent) {
                                this.setBounds({
                                    width: parent.clientWidth,
                                    height: parent.clientHeight
                                });
                            }
                            this.getViewer().repaint();
                        }, 10);
                    }
                }
                properties.FilteredViewerInPropertySection = FilteredViewerInPropertySection;
            })(properties = ide.properties || (ide.properties = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var undo;
            (function (undo) {
                class UndoManager {
                    constructor() {
                        this._undoList = [];
                        this._redoList = [];
                    }
                    add(op) {
                        this._undoList.push(op);
                        this._redoList = [];
                    }
                    undo() {
                        if (this._undoList.length > 0) {
                            const op = this._undoList.pop();
                            op.undo();
                            this._redoList.push(op);
                        }
                    }
                    redo() {
                        if (this._redoList.length > 0) {
                            const op = this._redoList.pop();
                            op.redo();
                            this._undoList.push(op);
                        }
                    }
                }
                undo.UndoManager = UndoManager;
            })(undo = ide.undo || (ide.undo = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var utils;
            (function (utils) {
                class NameMaker {
                    constructor(getName) {
                        this._getName = getName;
                        this._nameSet = new Set();
                    }
                    update(objects) {
                        for (const obj of objects) {
                            const name = this._getName(obj);
                            this._nameSet.add(name);
                        }
                    }
                    makeName(baseName) {
                        let name;
                        let i = 0;
                        do {
                            name = baseName + (i === 0 ? "" : "_" + i);
                            i++;
                        } while (this._nameSet.has(name));
                        this._nameSet.add(name);
                        return name;
                    }
                }
                utils.NameMaker = NameMaker;
            })(utils = ide.utils || (ide.utils = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>
/// <reference path="../../EditorViewerView.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var blocks;
                (function (blocks) {
                    class BlocksView extends ide.EditorViewerView {
                        constructor() {
                            super("BlocksView");
                            this.setTitle("Blocks");
                            this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_BLOCKS));
                        }
                        getViewerProvider(editor) {
                            return editor.getEditorViewerProvider(BlocksView.EDITOR_VIEWER_PROVIDER_KEY);
                        }
                    }
                    BlocksView.EDITOR_VIEWER_PROVIDER_KEY = "Blocks";
                    blocks.BlocksView = BlocksView;
                })(blocks = views.blocks || (views.blocks = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class Rect {
                constructor(x = 0, y = 0, w = 0, h = 0) {
                    this.x = x;
                    this.y = y;
                    this.w = w;
                    this.h = h;
                }
                set(x, y, w, h) {
                    this.x = x;
                    this.y = y;
                    this.w = w;
                    this.h = h;
                }
                contains(x, y) {
                    return x >= this.x && x <= this.x + this.w && y >= this.y && y <= this.y + this.h;
                }
                clone() {
                    return new Rect(this.x, this.y, this.w, this.h);
                }
            }
            controls.Rect = Rect;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                class LabelCellRenderer {
                    renderCell(args) {
                        const img = this.getImage(args.obj);
                        let x = args.x;
                        const ctx = args.canvasContext;
                        if (img) {
                            img.paint(ctx, x, args.y, controls.ICON_SIZE, args.h, false);
                        }
                    }
                    cellHeight(args) {
                        return controls.ROW_HEIGHT;
                    }
                    preload(obj) {
                        return Promise.resolve();
                    }
                }
                viewers.LabelCellRenderer = LabelCellRenderer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../Rect.ts"/>
/// <reference path="../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="./LabelCellRenderer.ts"/>
/// <reference path="./ImageCellRenderer.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                viewers.EVENT_OPEN_ITEM = "itemOpened";
                class Viewer extends controls.Control {
                    constructor(...classList) {
                        super("canvas", "Viewer");
                        this._labelProvider = null;
                        this._lastSelectedItemIndex = -1;
                        this._contentHeight = 0;
                        this.getElement().tabIndex = 1;
                        this.getElement().draggable = true;
                        this._filterText = "";
                        this._cellSize = 48;
                        this.initContext();
                        this._input = null;
                        this._expandedObjects = new Set();
                        this._selectedObjects = new Set();
                        window.cc = this;
                        this.initListeners();
                    }
                    initListeners() {
                        const canvas = this.getCanvas();
                        canvas.addEventListener("mouseup", e => this.onMouseUp(e));
                        canvas.addEventListener("wheel", e => this.onWheel(e));
                        canvas.addEventListener("keydown", e => this.onKeyDown(e));
                        canvas.addEventListener("dblclick", e => this.onDoubleClick(e));
                        canvas.addEventListener("dragstart", e => this.onDragStart(e));
                    }
                    onDragStart(e) {
                        const paintItemUnderCursor = this.getPaintItemAt(e);
                        if (paintItemUnderCursor) {
                            let dragObjects = [];
                            {
                                const sel = this.getSelection();
                                if (new Set(sel).has(paintItemUnderCursor.data)) {
                                    dragObjects = sel;
                                }
                                else {
                                    dragObjects = [paintItemUnderCursor.data];
                                }
                            }
                            controls.Controls.setDragEventImage(e, (ctx, w, h) => {
                                for (const obj of dragObjects) {
                                    const renderer = this.getCellRendererProvider().getCellRenderer(obj);
                                    renderer.renderCell(new viewers.RenderCellArgs(ctx, 0, 0, w, h, obj, this, true));
                                }
                            });
                            const labels = dragObjects.map(obj => this.getLabelProvider().getLabel(obj)).join(",");
                            e.dataTransfer.setData("plain/text", labels);
                            controls.Controls.setApplicationDragData(dragObjects);
                        }
                        else {
                            e.preventDefault();
                        }
                    }
                    getLabelProvider() {
                        return this._labelProvider;
                    }
                    setLabelProvider(labelProvider) {
                        this._labelProvider = labelProvider;
                    }
                    setFilterText(filterText) {
                        this._filterText = filterText.toLowerCase();
                        this.repaint();
                    }
                    getFilterText() {
                        return this._filterText;
                    }
                    prepareFiltering() {
                        this._filterIncludeSet = new Set();
                        this.buildFilterIncludeMap();
                    }
                    isFilterIncluded(obj) {
                        return this._filterIncludeSet.has(obj);
                    }
                    matches(obj) {
                        const labelProvider = this.getLabelProvider();
                        const filter = this.getFilterText();
                        if (labelProvider === null) {
                            return true;
                        }
                        if (filter === "") {
                            return true;
                        }
                        const label = labelProvider.getLabel(obj);
                        if (label.toLocaleLowerCase().indexOf(filter) !== -1) {
                            return true;
                        }
                        return false;
                    }
                    getPaintItemAt(e) {
                        for (let item of this._paintItems) {
                            if (item.contains(e.offsetX, e.offsetY)) {
                                return item;
                            }
                        }
                        return null;
                    }
                    getSelection() {
                        const sel = [];
                        for (const obj of this._selectedObjects) {
                            sel.push(obj);
                        }
                        return sel;
                    }
                    setSelection(selection, notify = true) {
                        this._selectedObjects = new Set(selection);
                        if (notify) {
                            this.fireSelectionChanged();
                            this.repaint();
                        }
                    }
                    fireSelectionChanged() {
                        this.dispatchEvent(new CustomEvent(controls.EVENT_SELECTION_CHANGED, {
                            detail: this.getSelection()
                        }));
                    }
                    onKeyDown(e) {
                        if (e.key === "Escape") {
                            if (this._selectedObjects.size > 0) {
                                this._selectedObjects.clear();
                                this.repaint();
                                this.fireSelectionChanged();
                            }
                        }
                    }
                    onWheel(e) {
                        if (!e.shiftKey) {
                            return;
                        }
                        if (e.deltaY < 0) {
                            this.setCellSize(this.getCellSize() + controls.ROW_HEIGHT);
                        }
                        else if (this._cellSize > controls.ICON_SIZE) {
                            this.setCellSize(this.getCellSize() - controls.ROW_HEIGHT);
                        }
                        this.repaint();
                    }
                    onDoubleClick(e) {
                        const item = this.getPaintItemAt(e);
                        this.dispatchEvent(new CustomEvent(viewers.EVENT_OPEN_ITEM, {
                            detail: item.data
                        }));
                    }
                    onMouseUp(e) {
                        if (e.button !== 0) {
                            return;
                        }
                        if (!this.canSelectAtPoint(e)) {
                            return;
                        }
                        const item = this.getPaintItemAt(e);
                        let selChanged = false;
                        if (item === null) {
                            this._selectedObjects.clear();
                            selChanged = true;
                        }
                        else {
                            const data = item.data;
                            if (e.ctrlKey || e.metaKey) {
                                if (this._selectedObjects.has(data)) {
                                    this._selectedObjects.delete(data);
                                }
                                else {
                                    this._selectedObjects.add(data);
                                }
                                selChanged = true;
                            }
                            else if (e.shiftKey) {
                                if (this._lastSelectedItemIndex >= 0 && this._lastSelectedItemIndex != item.index) {
                                    const start = Math.min(this._lastSelectedItemIndex, item.index);
                                    const end = Math.max(this._lastSelectedItemIndex, item.index);
                                    for (let i = start; i <= end; i++) {
                                        const obj = this._paintItems[i].data;
                                        this._selectedObjects.add(obj);
                                    }
                                    selChanged = true;
                                }
                            }
                            else {
                                this._selectedObjects.clear();
                                this._selectedObjects.add(data);
                                selChanged = true;
                            }
                        }
                        if (selChanged) {
                            this.repaint();
                            this.fireSelectionChanged();
                            this._lastSelectedItemIndex = item ? item.index : 0;
                        }
                    }
                    initContext() {
                        this._context = this.getCanvas().getContext("2d");
                        this._context.imageSmoothingEnabled = false;
                        this._context.font = `${controls.FONT_HEIGHT}px sans-serif`;
                    }
                    setExpanded(obj, expanded) {
                        if (expanded) {
                            this._expandedObjects.add(obj);
                        }
                        else {
                            this._expandedObjects.delete(obj);
                        }
                    }
                    isExpanded(obj) {
                        return this._expandedObjects.has(obj);
                    }
                    getExpandedObjects() {
                        return this._expandedObjects;
                    }
                    isCollapsed(obj) {
                        return !this.isExpanded(obj);
                    }
                    isSelected(obj) {
                        return this._selectedObjects.has(obj);
                    }
                    paintTreeHandler(x, y, collapsed) {
                        if (collapsed) {
                            this._context.strokeStyle = "#000";
                            this._context.strokeRect(x, y, controls.ICON_SIZE, controls.ICON_SIZE);
                        }
                        else {
                            this._context.fillStyle = "#000";
                            this._context.fillRect(x, y, controls.ICON_SIZE, controls.ICON_SIZE);
                        }
                    }
                    async repaint() {
                        this.prepareFiltering();
                        this.repaint2();
                        const result = await this.preload();
                        if (result === controls.PreloadResult.RESOURCES_LOADED) {
                            this.repaint2();
                        }
                        this.updateScrollPane();
                    }
                    updateScrollPane() {
                        const pane = this.getContainer().getContainer();
                        if (pane instanceof controls.ScrollPane) {
                            pane.updateScroll(this._contentHeight);
                        }
                    }
                    repaint2() {
                        this._paintItems = [];
                        const canvas = this.getCanvas();
                        this._context.clearRect(0, 0, canvas.width, canvas.height);
                        if (this._cellRendererProvider && this._contentProvider && this._input !== null) {
                            this.paint();
                        }
                    }
                    paintItemBackground(obj, x, y, w, h, radius = 0) {
                        let fillStyle = null;
                        if (this.isSelected(obj)) {
                            fillStyle = controls.Controls.theme.treeItemSelectionBackground;
                        }
                        if (fillStyle != null) {
                            this._context.save();
                            this._context.fillStyle = fillStyle;
                            this._context.strokeStyle = fillStyle;
                            if (radius > 0) {
                                this._context.lineJoin = "round";
                                this._context.lineWidth = radius;
                                this._context.strokeRect(x + (radius / 2), y + (radius / 2), w - radius, h - radius);
                                this._context.fillRect(x + (radius / 2), y + (radius / 2), w - radius, h - radius);
                            }
                            else {
                                this._context.fillRect(x, y, w, h);
                            }
                            this._context.restore();
                        }
                    }
                    setScrollY(scrollY) {
                        const b = this.getBounds();
                        scrollY = Math.max(-this._contentHeight + b.height, scrollY);
                        scrollY = Math.min(0, scrollY);
                        super.setScrollY(scrollY);
                        this.repaint();
                    }
                    layout() {
                        const b = this.getBounds();
                        if (this.isHandlePosition()) {
                            ui.controls.setElementBounds(this.getElement(), {
                                x: b.x,
                                y: b.y,
                                width: b.width | 0,
                                height: b.height | 0
                            });
                        }
                        else {
                            ui.controls.setElementBounds(this.getElement(), {
                                width: b.width | 0,
                                height: b.height | 0
                            });
                        }
                        const canvas = this.getCanvas();
                        canvas.width = b.width | 0;
                        canvas.height = b.height | 0;
                        this.initContext();
                        this.repaint();
                    }
                    getCanvas() {
                        return this.getElement();
                    }
                    getContext() {
                        return this._context;
                    }
                    getCellSize() {
                        return this._cellSize;
                    }
                    setCellSize(cellSize) {
                        this._cellSize = Math.max(controls.ROW_HEIGHT, cellSize);
                    }
                    getContentProvider() {
                        return this._contentProvider;
                    }
                    setContentProvider(contentProvider) {
                        this._contentProvider = contentProvider;
                    }
                    getCellRendererProvider() {
                        return this._cellRendererProvider;
                    }
                    setCellRendererProvider(cellRendererProvider) {
                        this._cellRendererProvider = cellRendererProvider;
                    }
                    getInput() {
                        return this._input;
                    }
                    setInput(input) {
                        this._input = input;
                    }
                    getState() {
                        return {
                            filterText: this._filterText,
                            expandedObjects: this._expandedObjects
                        };
                    }
                    setState(state) {
                        this._expandedObjects = state.expandedObjects;
                        this.setFilterText(state.filterText);
                    }
                }
                viewers.Viewer = Viewer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var files;
                (function (files) {
                    var viewers = phasereditor2d.ui.controls.viewers;
                    class FileCellRenderer extends viewers.LabelCellRenderer {
                        getImage(obj) {
                            const file = obj;
                            if (file.isFile()) {
                                const ct = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);
                                const icon = ide.Workbench.getWorkbench().getContentTypeIcon(ct);
                                if (icon) {
                                    return icon;
                                }
                            }
                            else {
                                return ui.controls.Controls.getIcon(ide.ICON_FOLDER);
                            }
                            return ui.controls.Controls.getIcon(ide.ICON_FILE);
                        }
                        preload(obj) {
                            const file = obj;
                            if (file.isFile()) {
                                return ide.Workbench.getWorkbench().getContentTypeRegistry().preload(file);
                            }
                            return super.preload(obj);
                        }
                    }
                    files.FileCellRenderer = FileCellRenderer;
                })(files = views.files || (views.files = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var files;
                (function (files) {
                    class FileCellRendererProvider {
                        getCellRenderer(file) {
                            if (ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file) === ui.ide.CONTENT_TYPE_IMAGE) {
                                return new files.FileImageRenderer();
                            }
                            return new files.FileCellRenderer();
                        }
                        preload(file) {
                            return ide.Workbench.getWorkbench().getContentTypeRegistry().preload(file);
                        }
                    }
                    files.FileCellRendererProvider = FileCellRendererProvider;
                })(files = views.files || (views.files = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var files;
                (function (files) {
                    var viewers = phasereditor2d.ui.controls.viewers;
                    class FileImageRenderer extends viewers.ImageCellRenderer {
                        getLabel(file) {
                            return file.getName();
                        }
                        getImage(file) {
                            return ide.Workbench.getWorkbench().getFileImage(file);
                        }
                    }
                    files.FileImageRenderer = FileImageRenderer;
                })(files = views.files || (views.files = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var files;
                (function (files) {
                    class FileLabelProvider {
                        getLabel(obj) {
                            return obj.getName();
                        }
                    }
                    files.FileLabelProvider = FileLabelProvider;
                })(files = views.files || (views.files = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySection.ts" />
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySectionProvider.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var files;
                (function (files) {
                    class FilePropertySectionProvider extends ui.controls.properties.PropertySectionProvider {
                        addSections(page, sections) {
                            sections.push(new files.FileSection(page));
                            sections.push(new files.ImageFileSection(page));
                            sections.push(new files.ManyImageFileSection(page));
                        }
                    }
                    files.FilePropertySectionProvider = FilePropertySectionProvider;
                })(files = views.files || (views.files = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var files;
                (function (files) {
                    class FileSection extends ui.controls.properties.PropertySection {
                        constructor(page) {
                            super(page, "files.FileSection", "File");
                        }
                        createForm(parent) {
                            const comp = this.createGridElement(parent, 2);
                            {
                                // Name
                                this.createLabel(comp, "Name");
                                const text = this.createText(comp, true);
                                this.addUpdater(() => {
                                    text.value = this.flatValues_StringJoin(this.getSelection().map(file => file.getName()));
                                });
                            }
                            {
                                // Full Name
                                this.createLabel(comp, "Full Name");
                                const text = this.createText(comp, true);
                                this.addUpdater(() => {
                                    text.value = this.flatValues_StringJoin(this.getSelection().map(file => file.getFullName()));
                                });
                            }
                            {
                                // Size
                                this.createLabel(comp, "Size");
                                const text = this.createText(comp, true);
                                this.addUpdater(() => {
                                    text.value = this.getSelection()
                                        .map(f => f.getSize())
                                        .reduce((a, b) => a + b)
                                        .toString();
                                });
                            }
                        }
                        canEdit(obj) {
                            return obj instanceof phasereditor2d.core.io.FilePath;
                        }
                        canEditNumber(n) {
                            return n > 0;
                        }
                    }
                    files.FileSection = FileSection;
                })(files = views.files || (views.files = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var files;
                (function (files) {
                    var io = phasereditor2d.core.io;
                    class FileTreeContentProvider {
                        getRoots(input) {
                            if (input instanceof io.FilePath) {
                                return [input];
                            }
                            if (input instanceof Array) {
                                return input;
                            }
                            return this.getChildren(input);
                        }
                        getChildren(parent) {
                            return parent.getFiles();
                        }
                    }
                    files.FileTreeContentProvider = FileTreeContentProvider;
                })(files = views.files || (views.files = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySectionProvider.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySection.ts"/>
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>
/// <reference path="../../ViewerView.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var files;
                (function (files) {
                    var viewers = phasereditor2d.ui.controls.viewers;
                    class FilesView extends ide.ViewerView {
                        constructor() {
                            super("filesView");
                            this._propertyProvider = new files.FilePropertySectionProvider();
                            this.setTitle("Files");
                            this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_FOLDER));
                        }
                        createViewer() {
                            return new viewers.TreeViewer();
                        }
                        getPropertyProvider() {
                            return this._propertyProvider;
                        }
                        createPart() {
                            super.createPart();
                            const root = ide.Workbench.getWorkbench().getFileStorage().getRoot();
                            const viewer = this._viewer;
                            viewer.setLabelProvider(new files.FileLabelProvider());
                            viewer.setContentProvider(new files.FileTreeContentProvider());
                            viewer.setCellRendererProvider(new files.FileCellRendererProvider());
                            viewer.setInput(root);
                            viewer.repaint();
                            viewer.addEventListener(ui.controls.viewers.EVENT_OPEN_ITEM, (e) => {
                                ide.Workbench.getWorkbench().openEditor(e.detail);
                            });
                        }
                        getIcon() {
                            return ui.controls.Controls.getIcon(ide.ICON_FOLDER);
                        }
                    }
                    files.FilesView = FilesView;
                })(files = views.files || (views.files = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var files;
                (function (files) {
                    class ImageFileSection extends ui.controls.properties.PropertySection {
                        constructor(page) {
                            super(page, "files.ImagePreviewSection", "Image", true);
                        }
                        createForm(parent) {
                            parent.classList.add("ImagePreviewFormArea", "PreviewBackground");
                            const imgControl = new ui.controls.ImageControl(ide.IMG_SECTION_PADDING);
                            this.getPage().addEventListener(ui.controls.EVENT_CONTROL_LAYOUT, (e) => {
                                imgControl.resizeTo();
                            });
                            parent.appendChild(imgControl.getElement());
                            setTimeout(() => imgControl.resizeTo(), 1);
                            this.addUpdater(() => {
                                const file = this.getSelection()[0];
                                const img = ide.Workbench.getWorkbench().getFileImage(file);
                                imgControl.setImage(img);
                                setTimeout(() => imgControl.resizeTo(), 1);
                            });
                        }
                        canEdit(obj) {
                            if (obj instanceof phasereditor2d.core.io.FilePath) {
                                const ct = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(obj);
                                return ct === ide.CONTENT_TYPE_IMAGE;
                            }
                            return false;
                        }
                        canEditNumber(n) {
                            return n == 1;
                        }
                    }
                    files.ImageFileSection = ImageFileSection;
                })(files = views.files || (views.files = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./Viewer.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                viewers.TREE_ICON_SIZE = controls.ICON_SIZE;
                viewers.LABEL_MARGIN = viewers.TREE_ICON_SIZE + 0;
                class TreeViewer extends viewers.Viewer {
                    constructor(...classList) {
                        super("TreeViewer", ...classList);
                        this.getCanvas().addEventListener("click", e => this.onClick(e));
                        this._treeRenderer = new viewers.TreeViewerRenderer(this);
                        this._treeIconList = [];
                        this.setContentProvider(new controls.viewers.EmptyTreeContentProvider());
                    }
                    getTreeRenderer() {
                        return this._treeRenderer;
                    }
                    setTreeRenderer(treeRenderer) {
                        this._treeRenderer = treeRenderer;
                    }
                    canSelectAtPoint(e) {
                        const icon = this.getTreeIconAtPoint(e);
                        return icon === null;
                    }
                    reveal(...objects) {
                        for (const obj of objects) {
                            const path = this.getObjectPath(obj);
                            this.revealPath(path);
                        }
                    }
                    revealPath(path) {
                        for (let i = 0; i < path.length - 1; i++) {
                            this.setExpanded(path[i], true);
                        }
                    }
                    getObjectPath(obj) {
                        const list = this.getContentProvider().getRoots(this.getInput());
                        const path = [];
                        this.getObjectPath2(obj, path, list);
                        return path;
                    }
                    getObjectPath2(obj, path, children) {
                        const contentProvider = this.getContentProvider();
                        for (const child of children) {
                            path.push(child);
                            if (obj === child) {
                                return true;
                            }
                            const found = this.getObjectPath2(obj, path, contentProvider.getChildren(child));
                            if (found) {
                                return true;
                            }
                            path.pop();
                        }
                        return false;
                    }
                    getTreeIconAtPoint(e) {
                        for (let icon of this._treeIconList) {
                            if (icon.rect.contains(e.offsetX, e.offsetY)) {
                                return icon;
                            }
                        }
                        return null;
                    }
                    onClick(e) {
                        const icon = this.getTreeIconAtPoint(e);
                        if (icon) {
                            this.setExpanded(icon.obj, !this.isExpanded(icon.obj));
                            this.repaint();
                        }
                    }
                    visitObjects(visitor) {
                        const provider = this.getContentProvider();
                        const list = provider ? provider.getRoots(this.getInput()) : [];
                        this.visitObjects2(list, visitor);
                    }
                    visitObjects2(objects, visitor) {
                        for (var obj of objects) {
                            visitor(obj);
                            if (this.isExpanded(obj) || this.getFilterText() !== "") {
                                const list = this.getContentProvider().getChildren(obj);
                                this.visitObjects2(list, visitor);
                            }
                        }
                    }
                    async preload() {
                        const list = [];
                        this.visitObjects(obj => {
                            const provider = this.getCellRendererProvider();
                            list.push(provider.preload(obj).then(r => {
                                const renderer = provider.getCellRenderer(obj);
                                return renderer.preload(obj);
                            }));
                        });
                        return controls.Controls.resolveAll(list);
                    }
                    paint() {
                        const result = this._treeRenderer.paint();
                        this._contentHeight = result.contentHeight;
                        this._paintItems = result.paintItems;
                        this._treeIconList = result.treeIconList;
                    }
                    setFilterText(filter) {
                        super.setFilterText(filter);
                        if (filter !== "") {
                            this.expandFilteredParents(this.getContentProvider().getRoots(this.getInput()));
                            this.repaint();
                        }
                    }
                    expandFilteredParents(objects) {
                        const contentProvider = this.getContentProvider();
                        for (const obj of objects) {
                            if (this.isFilterIncluded(obj)) {
                                const children = contentProvider.getChildren(obj);
                                if (children.length > 0) {
                                    this.setExpanded(obj, true);
                                    this.expandFilteredParents(children);
                                }
                            }
                        }
                    }
                    buildFilterIncludeMap() {
                        const provider = this.getContentProvider();
                        const roots = provider ? provider.getRoots(this.getInput()) : [];
                        this.buildFilterIncludeMap2(roots);
                    }
                    buildFilterIncludeMap2(objects) {
                        let result = false;
                        for (const obj of objects) {
                            let resultThis = this.matches(obj);
                            const children = this.getContentProvider().getChildren(obj);
                            const resultChildren = this.buildFilterIncludeMap2(children);
                            resultThis = resultThis || resultChildren;
                            if (resultThis) {
                                this._filterIncludeSet.add(obj);
                                result = true;
                            }
                        }
                        return result;
                    }
                    getContentProvider() {
                        return super.getContentProvider();
                    }
                    setContentProvider(contentProvider) {
                        super.setContentProvider(contentProvider);
                    }
                }
                viewers.TreeViewer = TreeViewer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/TreeViewer.ts" />
/// <reference path="../../../../../phasereditor2d.ui.controls/viewers/GridTreeViewerRenderer.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var files;
                (function (files) {
                    class GridImageFileViewer extends ui.controls.viewers.TreeViewer {
                        constructor(...classList) {
                            super("PreviewBackground", ...classList);
                            this.setContentProvider(new ui.controls.viewers.ArrayTreeContentProvider());
                            this.setLabelProvider(new files.FileLabelProvider());
                            this.setCellRendererProvider(new files.FileCellRendererProvider());
                            this.setTreeRenderer(new ui.controls.viewers.GridTreeViewerRenderer(this, true));
                            this.getCanvas().classList.add("PreviewBackground");
                        }
                    }
                    class ManyImageFileSection extends ui.controls.properties.PropertySection {
                        constructor(page) {
                            super(page, "files.ManyImageFileSection", "Images", true);
                        }
                        createForm(parent) {
                            parent.classList.add("ManyImagePreviewFormArea");
                            const viewer = new GridImageFileViewer();
                            const filteredViewer = new ide.properties.FilteredViewerInPropertySection(this.getPage(), viewer);
                            parent.appendChild(filteredViewer.getElement());
                            this.addUpdater(() => {
                                // clean the viewer first
                                viewer.setInput([]);
                                viewer.repaint();
                                viewer.setInput(this.getSelection());
                                filteredViewer.resizeTo();
                            });
                        }
                        canEdit(obj) {
                            if (obj instanceof phasereditor2d.core.io.FilePath) {
                                const ct = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(obj);
                                return ct === ide.CONTENT_TYPE_IMAGE;
                            }
                            return false;
                        }
                        canEditNumber(n) {
                            return n > 1;
                        }
                    }
                    files.ManyImageFileSection = ManyImageFileSection;
                })(files = views.files || (views.files = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../ViewPart.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertyPage.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySection.ts"/>
/// <reference path="../../../../../phasereditor2d.ui.controls/properties/PropertySectionProvider.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var inspector;
                (function (inspector) {
                    class InspectorView extends ide.ViewPart {
                        constructor() {
                            super("InspectorView");
                            this.setTitle("Inspector");
                            this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_INSPECTOR));
                        }
                        layout() {
                            this._propertyPage.dispatchLayoutEvent();
                        }
                        createPart() {
                            this._propertyPage = new ui.controls.properties.PropertyPage();
                            this.add(this._propertyPage);
                            this._selectionListener = (e) => this.onPartSelection();
                            ide.Workbench.getWorkbench().addEventListener(ide.EVENT_PART_ACTIVATED, e => this.onWorkbenchPartActivate());
                        }
                        onWorkbenchPartActivate() {
                            const part = ide.Workbench.getWorkbench().getActivePart();
                            if (part !== this && part !== this._currentPart) {
                                if (this._currentPart) {
                                    this._currentPart.removeEventListener(ui.controls.EVENT_SELECTION_CHANGED, this._selectionListener);
                                }
                                this._currentPart = part;
                                if (part) {
                                    part.addEventListener(ui.controls.EVENT_SELECTION_CHANGED, this._selectionListener);
                                    this.onPartSelection();
                                }
                                else {
                                    this._propertyPage.setSectionProvider(null);
                                }
                            }
                        }
                        onPartSelection() {
                            const sel = this._currentPart.getSelection();
                            const provider = this._currentPart.getPropertyProvider();
                            this._propertyPage.setSectionProvider(provider);
                            this._propertyPage.setSelection(sel);
                        }
                    }
                    inspector.InspectorView = InspectorView;
                })(inspector = views.inspector || (views.inspector = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../ViewPart.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var views;
            (function (views) {
                var outline;
                (function (outline) {
                    class OutlineView extends ide.EditorViewerView {
                        constructor() {
                            super("OutlineView");
                            this.setTitle("Outline");
                            this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_OUTLINE));
                        }
                        getViewerProvider(editor) {
                            return editor.getEditorViewerProvider(OutlineView.EDITOR_VIEWER_PROVIDER_KEY);
                        }
                    }
                    OutlineView.EDITOR_VIEWER_PROVIDER_KEY = "Outline";
                    outline.OutlineView = OutlineView;
                })(outline = views.outline || (views.outline = {}));
            })(views = ide.views || (ide.views = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class Action {
            }
            controls.Action = Action;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class ActionButton extends controls.Control {
                constructor(action) {
                    super("button");
                    this._action = action;
                    this.getElement().classList.add("actionButton");
                }
                getAction() {
                    return this._action;
                }
            }
            controls.ActionButton = ActionButton;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class CanvasControl extends controls.Control {
                constructor(padding = 0, ...classList) {
                    super("canvas", "CanvasControl", ...classList);
                    this._padding = padding;
                    this._canvas = this.getElement();
                    this.initContext();
                }
                getCanvas() {
                    return this._canvas;
                }
                resizeTo(parent) {
                    parent = parent || this.getElement().parentElement;
                    this.style.width = parent.clientWidth - this._padding * 2 + "px";
                    this.style.height = parent.clientHeight - this._padding * 2 + "px";
                    this.repaint();
                }
                getPadding() {
                    return this._padding;
                }
                ensureCanvasSize() {
                    if (this._canvas.width !== this._canvas.clientWidth || this._canvas.height !== this._canvas.clientHeight) {
                        this._canvas.width = this._canvas.clientWidth;
                        this._canvas.height = this._canvas.clientHeight;
                        this.initContext();
                    }
                }
                clear() {
                    this._context.clearRect(0, 0, this._canvas.width, this._canvas.height);
                }
                repaint() {
                    this.ensureCanvasSize();
                    this._context.clearRect(0, 0, this._canvas.width, this._canvas.height);
                    this.paint();
                }
                initContext() {
                    this._context = this.getCanvas().getContext("2d");
                    this._context.imageSmoothingEnabled = false;
                    this._context.font = `${controls.FONT_HEIGHT}px sans-serif`;
                }
            }
            controls.CanvasControl = CanvasControl;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class DefaultImage {
                constructor(img, url) {
                    this._imageElement = img;
                    this._url = url;
                    this._ready = false;
                    this._error = false;
                }
                getImageElement() {
                    return this._imageElement;
                }
                preload() {
                    if (this._ready || this._error) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                    if (this._requestPromise) {
                        return this._requestPromise;
                    }
                    this._requestPromise = new Promise((resolve, reject) => {
                        this._imageElement.src = this._url;
                        this._imageElement.addEventListener("load", e => {
                            this._requestPromise = null;
                            this._ready = true;
                            resolve(controls.PreloadResult.RESOURCES_LOADED);
                        });
                        this._imageElement.addEventListener("error", e => {
                            console.error("ERROR: Loading image " + this._url);
                            this._requestPromise = null;
                            this._error = true;
                            resolve(controls.PreloadResult.NOTHING_LOADED);
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
                    return this._ready ? this._imageElement.naturalWidth : 16;
                }
                getHeight() {
                    return this._ready ? this._imageElement.naturalHeight : 16;
                }
                paint(context, x, y, w, h, center) {
                    if (this._ready) {
                        DefaultImage.paintImageElement(context, this._imageElement, x, y, w, h, center);
                    }
                    else {
                        DefaultImage.paintEmpty(context, x, y, w, h);
                    }
                }
                static paintImageElement(context, image, x, y, w, h, center) {
                    const naturalWidth = image.naturalWidth;
                    const naturalHeight = image.naturalHeight;
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
                        context.drawImage(image, imgX, imgY, imgDstW, imgDstH);
                    }
                }
                static paintEmpty(context, x, y, w, h) {
                    if (w > 10 && h > 10) {
                        context.save();
                        context.strokeStyle = controls.Controls.theme.treeItemForeground;
                        const cx = x + w / 2;
                        const cy = y + h / 2;
                        context.strokeRect(cx, cy - 1, 2, 2);
                        context.strokeRect(cx - 5, cy - 1, 2, 2);
                        context.strokeRect(cx + 5, cy - 1, 2, 2);
                        context.restore();
                    }
                }
                static paintImageElementFrame(context, image, srcX, srcY, scrW, srcH, dstX, dstY, dstW, dstH) {
                    context.drawImage(image, srcX, srcY, scrW, srcH, dstX, dstY, dstW, dstH);
                }
                paintFrame(context, srcX, srcY, scrW, srcH, dstX, dstY, dstW, dstH) {
                    if (this._ready) {
                        DefaultImage.paintImageElementFrame(context, this._imageElement, srcX, srcY, scrW, srcH, dstX, dstY, dstW, dstH);
                    }
                    else {
                        DefaultImage.paintEmpty(context, dstX, dstY, dstW, dstH);
                    }
                }
            }
            controls.DefaultImage = DefaultImage;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class FillLayout {
                constructor(padding = 0) {
                    this._padding = 0;
                    this._padding = padding;
                }
                getPadding() {
                    return this._padding;
                }
                setPadding(padding) {
                    this._padding = padding;
                }
                layout(parent) {
                    const children = parent.getChildren();
                    if (children.length > 1) {
                        console.warn("[FillLayout] Invalid number for children or parent control.");
                    }
                    const b = parent.getBounds();
                    controls.setElementBounds(parent.getElement(), b);
                    if (children.length > 0) {
                        const child = children[0];
                        child.setBoundsValues(this._padding, this._padding, b.width - this._padding * 2, b.height - this._padding * 2);
                    }
                }
            }
            controls.FillLayout = FillLayout;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class FrameData {
                constructor(index, src, dst, srcSize) {
                    this.index = index;
                    this.src = src;
                    this.dst = dst;
                    this.srcSize = srcSize;
                }
                static fromRect(index, rect) {
                    return new FrameData(0, rect.clone(), new controls.Rect(0, 0, rect.w, rect.h), new controls.Point(rect.w, rect.h));
                }
            }
            controls.FrameData = FrameData;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="CanvasControl.ts" />
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class ImageControl extends controls.CanvasControl {
                constructor(padding = 0, ...classList) {
                    super(padding, "ImageControl", ...classList);
                }
                setImage(image) {
                    this._image = image;
                }
                getImage() {
                    return this._image;
                }
                async paint() {
                    if (this._image) {
                        this.paint2();
                        const result = await this._image.preload();
                        if (result === controls.PreloadResult.RESOURCES_LOADED) {
                            this.paint2();
                        }
                    }
                    else {
                        this.clear();
                    }
                }
                paint2() {
                    this.ensureCanvasSize();
                    this.clear();
                    this._image.paint(this._context, 0, 0, this._canvas.width, this._canvas.height, true);
                }
            }
            controls.ImageControl = ImageControl;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class ImageWrapper {
                constructor(imageElement) {
                    this._imageElement = imageElement;
                }
                paint(context, x, y, w, h, center) {
                    if (this._imageElement) {
                        controls.DefaultImage.paintImageElement(context, this._imageElement, x, y, w, h, center);
                    }
                    else {
                        controls.DefaultImage.paintEmpty(context, x, y, w, h);
                    }
                }
                paintFrame(context, srcX, srcY, scrW, srcH, dstX, dstY, dstW, dstH) {
                    if (this._imageElement) {
                        controls.DefaultImage.paintImageElementFrame(context, this._imageElement, srcX, srcY, scrW, srcH, dstX, dstY, dstW, dstH);
                    }
                    else {
                        controls.DefaultImage.paintEmpty(context, dstX, dstY, dstW, dstH);
                    }
                }
                preload() {
                    return controls.Controls.resolveNothingLoaded();
                }
                getWidth() {
                    if (this._imageElement) {
                        return this._imageElement.naturalWidth;
                    }
                    return 0;
                }
                getHeight() {
                    if (this._imageElement) {
                        return this._imageElement.naturalHeight;
                    }
                    return 0;
                }
            }
            controls.ImageWrapper = ImageWrapper;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class Point {
                constructor(x, y) {
                    this.x = x;
                    this.y = y;
                }
            }
            controls.Point = Point;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class ScrollPane extends controls.Control {
                constructor(clientControl) {
                    super("div", "ScrollPane");
                    this._clientContentHeight = 0;
                    this._startDragY = -1;
                    this._startScrollY = 0;
                    this._clientControl = clientControl;
                    this.add(this._clientControl);
                    this._scrollBar = document.createElement("div");
                    this._scrollBar.classList.add("ScrollBar");
                    this.getElement().appendChild(this._scrollBar);
                    this._scrollHandler = document.createElement("div");
                    this._scrollHandler.classList.add("ScrollHandler");
                    this._scrollBar.appendChild(this._scrollHandler);
                    const l2 = (e) => this.onMouseDown(e);
                    const l3 = (e) => this.onMouseUp(e);
                    const l4 = (e) => this.onMouseMove(e);
                    const l5 = (e) => {
                        if (!this.getElement().isConnected) {
                            window.removeEventListener("mousedown", l2);
                            window.removeEventListener("mouseup", l3);
                            window.removeEventListener("mousemove", l4);
                            window.removeEventListener("mousemove", l5);
                        }
                    };
                    window.addEventListener("mousedown", l2);
                    window.addEventListener("mouseup", l3);
                    window.addEventListener("mousemove", l4);
                    window.addEventListener("mousemove", l5);
                    this.getViewer().getElement().addEventListener("wheel", e => this.onClientWheel(e));
                    this._scrollBar.addEventListener("mousedown", e => this.onBarMouseDown(e));
                }
                getViewer() {
                    if (this._clientControl instanceof controls.viewers.ViewerContainer) {
                        return this._clientControl.getViewer();
                    }
                    return this._clientControl;
                }
                updateScroll(clientContentHeight) {
                    const scrollY = this.getViewer().getScrollY();
                    const b = this.getBounds();
                    let newScrollY = scrollY;
                    newScrollY = Math.max(-this._clientContentHeight + b.height, newScrollY);
                    newScrollY = Math.min(0, newScrollY);
                    if (newScrollY !== scrollY) {
                        this._clientContentHeight = clientContentHeight;
                        this.setClientScrollY(scrollY);
                    }
                    else if (clientContentHeight !== this._clientContentHeight) {
                        this._clientContentHeight = clientContentHeight;
                        this.layout();
                    }
                }
                onBarMouseDown(e) {
                    if (e.target !== this._scrollBar) {
                        return;
                    }
                    e.stopImmediatePropagation();
                    const b = this.getBounds();
                    this.setClientScrollY(-e.offsetY / b.height * (this._clientContentHeight - b.height));
                }
                onClientWheel(e) {
                    if (e.shiftKey || e.ctrlKey || e.metaKey || e.altKey) {
                        return;
                    }
                    let y = this.getViewer().getScrollY();
                    y += e.deltaY < 0 ? 30 : -30;
                    this.setClientScrollY(y);
                }
                setClientScrollY(y) {
                    const b = this.getBounds();
                    y = Math.max(-this._clientContentHeight + b.height, y);
                    y = Math.min(0, y);
                    this.getViewer().setScrollY(y);
                    this.layout();
                }
                onMouseDown(e) {
                    if (e.target === this._scrollHandler) {
                        e.stopImmediatePropagation();
                        this._startDragY = e.y;
                        this._startScrollY = this.getViewer().getScrollY();
                    }
                }
                onMouseMove(e) {
                    if (this._startDragY !== -1) {
                        let delta = e.y - this._startDragY;
                        const b = this.getBounds();
                        delta = delta / b.height * this._clientContentHeight;
                        this.setClientScrollY(this._startScrollY - delta);
                    }
                }
                onMouseUp(e) {
                    if (this._startDragY !== -1) {
                        e.stopImmediatePropagation();
                        this._startDragY = -1;
                    }
                }
                getBounds() {
                    const b = this.getElement().getBoundingClientRect();
                    return { x: 0, y: 0, width: b.width, height: b.height };
                }
                layout() {
                    const b = this.getBounds();
                    if (b.height < this._clientContentHeight) {
                        this._scrollHandler.style.display = "block";
                        const h = Math.max(10, b.height / this._clientContentHeight * b.height);
                        const y = -(b.height - h) * this.getViewer().getScrollY() / (this._clientContentHeight - b.height);
                        controls.setElementBounds(this._scrollHandler, {
                            y: y,
                            height: h
                        });
                        this.removeClass("hideScrollBar");
                    }
                    else {
                        this.addClass("hideScrollBar");
                    }
                    this._clientControl.layout();
                }
            }
            controls.ScrollPane = ScrollPane;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            class SplitPanel extends controls.Control {
                constructor(left, right, horizontal = true) {
                    super("div", "split");
                    this._startDrag = -1;
                    this._horizontal = horizontal;
                    this._splitPosition = 50;
                    this._splitFactor = 0.5;
                    this._splitWidth = 2;
                    const l1 = (e) => this.onMouseLeave(e);
                    const l2 = (e) => this.onMouseDown(e);
                    const l3 = (e) => this.onMouseUp(e);
                    const l4 = (e) => this.onMouseMove(e);
                    const l5 = (e) => {
                        if (!this.getElement().isConnected) {
                            window.removeEventListener("mouseleave", l1);
                            window.removeEventListener("mousedown", l2);
                            window.removeEventListener("mouseup", l3);
                            window.removeEventListener("mousemove", l4);
                            window.removeEventListener("mousemove", l5);
                        }
                    };
                    window.addEventListener("mouseleave", l1);
                    window.addEventListener("mousedown", l2);
                    window.addEventListener("mouseup", l3);
                    window.addEventListener("mousemove", l4);
                    window.addEventListener("mousemove", l5);
                    if (left) {
                        this.setLeftControl(left);
                    }
                    if (right) {
                        this.setRightControl(right);
                    }
                }
                onMouseDown(e) {
                    const pos = this.getControlPosition(e.x, e.y);
                    const offset = this._horizontal ? pos.x : pos.y;
                    const inside = Math.abs(offset - this._splitPosition) <= controls.SPLIT_OVER_ZONE_WIDTH && this.containsLocalPoint(pos.x, pos.y);
                    if (inside) {
                        e.stopImmediatePropagation();
                        this._startDrag = this._horizontal ? e.x : e.y;
                        this._startPos = this._splitPosition;
                    }
                }
                onMouseUp(e) {
                    if (this._startDrag !== -1) {
                        e.stopImmediatePropagation();
                    }
                    this._startDrag = -1;
                }
                onMouseMove(e) {
                    const pos = this.getControlPosition(e.x, e.y);
                    const offset = this._horizontal ? pos.x : pos.y;
                    const screen = this._horizontal ? e.x : e.y;
                    const boundsSize = this._horizontal ? this.getBounds().width : this.getBounds().height;
                    const cursorResize = this._horizontal ? "ew-resize" : "ns-resize";
                    const inside = Math.abs(offset - this._splitPosition) <= controls.SPLIT_OVER_ZONE_WIDTH && this.containsLocalPoint(pos.x, pos.y);
                    if (inside) {
                        if (e.buttons === 0 || this._startDrag !== -1) {
                            e.preventDefault();
                            this.getElement().style.cursor = cursorResize;
                        }
                    }
                    else {
                        this.getElement().style.cursor = "inherit";
                    }
                    if (this._startDrag !== -1) {
                        this.getElement().style.cursor = cursorResize;
                        const newPos = this._startPos + screen - this._startDrag;
                        if (newPos > 100 && boundsSize - newPos > 100) {
                            this._splitPosition = newPos;
                            this._splitFactor = this._splitPosition / boundsSize;
                            this.layout();
                        }
                    }
                }
                onMouseLeave(e) {
                    this.getElement().style.cursor = "inherit";
                    this._startDrag = -1;
                }
                setHorizontal(horizontal = true) {
                    this._horizontal = horizontal;
                }
                setVertical(vertical = true) {
                    this._horizontal = !vertical;
                }
                getSplitFactor() {
                    return this._splitFactor;
                }
                getSize() {
                    const b = this.getBounds();
                    return this._horizontal ? b.width : b.height;
                }
                setSplitFactor(factor) {
                    this._splitFactor = Math.min(Math.max(0, factor), 1);
                    this._splitPosition = this.getSize() * this._splitFactor;
                }
                setLeftControl(control) {
                    this._leftControl = control;
                    this.add(control);
                }
                getLeftControl() {
                    return this._leftControl;
                }
                setRightControl(control) {
                    this._rightControl = control;
                    this.add(control);
                }
                getRightControl() {
                    return this._rightControl;
                }
                layout() {
                    controls.setElementBounds(this.getElement(), this.getBounds());
                    if (!this._leftControl || !this._rightControl) {
                        return;
                    }
                    this.setSplitFactor(this._splitFactor);
                    const pos = this._splitPosition;
                    const sw = this._splitWidth;
                    let b = this.getBounds();
                    if (this._horizontal) {
                        this._leftControl.setBoundsValues(0, 0, pos - sw, b.height);
                        this._rightControl.setBoundsValues(pos + sw, 0, b.width - pos - sw, b.height);
                    }
                    else {
                        this._leftControl.setBoundsValues(0, 0, b.width, pos - sw);
                        this._rightControl.setBoundsValues(0, pos + sw, b.width, b.height - pos - sw);
                    }
                }
            }
            controls.SplitPanel = SplitPanel;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            controls.CONTROL_PADDING = 3;
            controls.ROW_HEIGHT = 20;
            controls.FONT_HEIGHT = 14;
            controls.FONT_OFFSET = 2;
            controls.ACTION_WIDTH = 20;
            controls.PANEL_BORDER_SIZE = 5;
            controls.PANEL_TITLE_HEIGHT = 22;
            controls.FILTERED_VIEWER_FILTER_HEIGHT = 30;
            controls.SPLIT_OVER_ZONE_WIDTH = 6;
            function setElementBounds(elem, bounds) {
                if (bounds.x !== undefined) {
                    elem.style.left = bounds.x + "px";
                }
                if (bounds.y !== undefined) {
                    elem.style.top = bounds.y + "px";
                }
                if (bounds.width !== undefined) {
                    elem.style.width = bounds.width + "px";
                }
                if (bounds.height !== undefined) {
                    elem.style.height = bounds.height + "px";
                }
            }
            controls.setElementBounds = setElementBounds;
            function getElementBounds(elem) {
                return {
                    x: elem.clientLeft,
                    y: elem.clientTop,
                    width: elem.clientWidth,
                    height: elem.clientHeight
                };
            }
            controls.getElementBounds = getElementBounds;
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                viewers.EMPTY_ARRAY = [];
                class ArrayTreeContentProvider {
                    getRoots(input) {
                        // ok, we assume the input is an array
                        return input;
                    }
                    getChildren(parent) {
                        return viewers.EMPTY_ARRAY;
                    }
                }
                viewers.ArrayTreeContentProvider = ArrayTreeContentProvider;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                class EmptyCellRenderer {
                    constructor(variableSize = true) {
                        this._variableSize = variableSize;
                    }
                    renderCell(args) {
                    }
                    cellHeight(args) {
                        return this._variableSize ? args.viewer.getCellSize() : controls.ROW_HEIGHT;
                    }
                    preload(obj) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                }
                viewers.EmptyCellRenderer = EmptyCellRenderer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                class FolderCellRenderer {
                    constructor(maxCount = 8) {
                        this._maxCount = maxCount;
                    }
                    renderCell(args) {
                        if (this.cellHeight(args) === controls.ROW_HEIGHT) {
                            this.renderFolder(args);
                        }
                        else {
                            this.renderGrid(args);
                        }
                    }
                    renderFolder(args) {
                        const icon = ui.ide.Workbench.getWorkbench().getWorkbenchIcon(ui.ide.ICON_FOLDER);
                        icon.paint(args.canvasContext, args.x, args.y, args.w, args.h, true);
                    }
                    renderGrid(args) {
                        const contentProvider = args.viewer.getContentProvider();
                        const children = contentProvider.getChildren(args.obj);
                        const width = args.w - 20;
                        const height = args.h - 2;
                        if (children) {
                            const realCount = children.length;
                            let frameCount = realCount;
                            if (frameCount == 0) {
                                return;
                            }
                            let step = 1;
                            if (frameCount > this._maxCount) {
                                step = frameCount / this._maxCount;
                                frameCount = this._maxCount;
                            }
                            var size = Math.floor(Math.sqrt(width * height / frameCount) * 0.8) + 1;
                            var cols = width / size;
                            var rows = frameCount / cols + (frameCount % cols == 0 ? 0 : 1);
                            var marginX = Math.max(0, (width - cols * size) / 2);
                            var marginY = Math.max(0, (height - rows * size) / 2);
                            var itemX = 0;
                            var itemY = 0;
                            const startX = 20 + args.x + marginX;
                            const startY = 2 + args.y + marginY;
                            for (var i = 0; i < frameCount; i++) {
                                if (itemY + size > height) {
                                    break;
                                }
                                const index = Math.min(realCount - 1, Math.round(i * step));
                                const obj = children[index];
                                const renderer = args.viewer.getCellRendererProvider().getCellRenderer(obj);
                                const args2 = new viewers.RenderCellArgs(args.canvasContext, startX + itemX, startY + itemY, size, size, obj, args.viewer, true);
                                renderer.renderCell(args2);
                                itemX += size;
                                if (itemX + size > width) {
                                    itemY += size;
                                    itemX = 0;
                                }
                            }
                        }
                    }
                    cellHeight(args) {
                        return args.viewer.getCellSize() < 50 ? controls.ROW_HEIGHT : args.viewer.getCellSize();
                    }
                    preload(obj) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                }
                viewers.FolderCellRenderer = FolderCellRenderer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./Viewer.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                class IconImageCellRenderer extends viewers.ImageCellRenderer {
                    constructor(icon) {
                        super();
                        this._icon = icon;
                    }
                    getImage() {
                        return this._icon;
                    }
                    cellHeight(args) {
                        return controls.ROW_HEIGHT;
                    }
                }
                viewers.IconImageCellRenderer = IconImageCellRenderer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                class PaintItem extends controls.Rect {
                    constructor(index, data) {
                        super();
                        this.index = index;
                        this.data = data;
                    }
                }
                viewers.PaintItem = PaintItem;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                class RenderCellArgs {
                    constructor(canvasContext, x, y, w, h, obj, viewer, center = false) {
                        this.canvasContext = canvasContext;
                        this.x = x;
                        this.y = y;
                        this.w = w;
                        this.h = h;
                        this.obj = obj;
                        this.viewer = viewer;
                        this.center = center;
                    }
                    clone() {
                        return new RenderCellArgs(this.canvasContext, this.x, this.y, this.w, this.h, this.obj, this.viewer, this.center);
                    }
                }
                viewers.RenderCellArgs = RenderCellArgs;
                ;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
