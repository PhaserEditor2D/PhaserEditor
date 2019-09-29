var phasereditor2d;
(function (phasereditor2d) {
    async function main() {
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
            function makeApiRequest(method, body) {
                return fetch("../api", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        "method": method,
                        "body": body
                    })
                });
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
                    const resp = await makeApiRequest("GetProjectFiles");
                    const data = await resp.json();
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
                    const resp = await makeApiRequest("GetFileString", {
                        path: file.getFullName()
                    });
                    const data = await resp.json();
                    if (data.error) {
                        alert(`Cannot get file content of '${file.getFullName()}'`);
                        return null;
                    }
                    const content = data["content"];
                    this._fileStringContentMap.set(id, content);
                    return content;
                }
            }
            io.ServerFileStorage = ServerFileStorage;
        })(io = core.io || (core.io = {}));
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
                    this._outlineView = new ide.outline.OutlineView();
                    this._filesView = new ide.views.files.FilesView();
                    this._inspectorView = new ide.inspector.InspectorView();
                    this._blocksView = new ide.blocks.BlocksView();
                    this._editorArea = new ide.EditorArea();
                    this._split_Files_Blocks = new ui.controls.SplitPanel(this.createViewFolder(this._filesView), this.createViewFolder(this._blocksView));
                    this._split_Editor_FilesBlocks = new ui.controls.SplitPanel(this._editorArea, this._split_Files_Blocks, false);
                    this._split_Outline_EditorFilesBlocks = new ui.controls.SplitPanel(this.createViewFolder(this._outlineView), this._split_Editor_FilesBlocks);
                    this._split_OutlineEditorFilesBlocks_Inspector = new ui.controls.SplitPanel(this._split_Outline_EditorFilesBlocks, this.createViewFolder(this._inspectorView));
                    this.add(this._split_OutlineEditorFilesBlocks_Inspector);
                    window.addEventListener("resize", e => {
                        this.setBoundsValues(0, 0, window.innerWidth, window.innerHeight);
                    });
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
            controls.EVENT_SELECTION = "selectionChanged";
            controls.EVENT_THEME = "themeChanged";
            let PreloadResult;
            (function (PreloadResult) {
                PreloadResult[PreloadResult["NOTHING_LOADED"] = 0] = "NOTHING_LOADED";
                PreloadResult[PreloadResult["RESOURCES_LOADED"] = 1] = "RESOURCES_LOADED";
            })(PreloadResult = controls.PreloadResult || (controls.PreloadResult = {}));
            class ImageImpl {
                constructor(img, url) {
                    this._img = img;
                    this._url = url;
                    this._ready = false;
                    this._error = false;
                }
                preload() {
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
                getWidth() {
                    return this._ready ? this._img.naturalWidth : 16;
                }
                getHeight() {
                    return this._ready ? this._img.naturalHeight : 16;
                }
                paint(context, x, y, w, h, center) {
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
                    }
                    else {
                        this.paintEmpty(context, x, y, w, h);
                    }
                }
                paintEmpty(context, x, y, w, h) {
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
                paintFrame(context, srcX, srcY, scrW, srcH, dstX, dstY, dstW, dstH) {
                    if (this._ready) {
                        context.drawImage(this._img, srcX, srcY, scrW, srcH, dstX, dstY, dstW, dstH);
                    }
                    else {
                        this.paintEmpty(context, dstX, dstY, dstW, dstH);
                    }
                }
            }
            controls.ICON_CONTROL_TREE_COLLAPSE = "tree-collapse";
            controls.ICON_CONTROL_TREE_EXPAND = "tree-expand";
            controls.ICON_CONTROL_CLOSE = "close";
            controls.ICON_SIZE = 16;
            const ICONS = [
                controls.ICON_CONTROL_TREE_COLLAPSE,
                controls.ICON_CONTROL_TREE_EXPAND,
                controls.ICON_CONTROL_CLOSE
            ];
            class Controls {
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
                    const img = new ImageImpl(new Image(), url);
                    Controls._images.set(id, img);
                    return img;
                }
                static getIcon(name, baseUrl = "phasereditor2d.ui.controls/images") {
                    const url = `${baseUrl}/${controls.ICON_SIZE}/${name}.png`;
                    return Controls.getImage(url, name);
                }
                static createIconElement(icon) {
                    const element = document.createElement("canvas");
                    element.width = element.height = controls.ICON_SIZE;
                    element.style.width = element.style.height = controls.ICON_SIZE + "px";
                    const context = element.getContext("2d");
                    context.imageSmoothingEnabled = false;
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
                    window.dispatchEvent(new CustomEvent(controls.EVENT_THEME, { detail: this.theme }));
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
            Controls.LIGHT_THEME = {
                treeItemSelectionBackground: "#4242ff",
                treeItemSelectionForeground: "#f0f0f0",
                treeItemForeground: "#000"
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
                    this.getElement().setAttribute("id", id);
                    this.getElement().classList.add("Part");
                    this.getElement()["__part"] = this;
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
                setSelection(selection) {
                    this._selection = selection;
                    this.dispatchEvent(new CustomEvent(ui.controls.EVENT_SELECTION, {
                        detail: selection
                    }));
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
                }
                getInput() {
                    return this._input;
                }
                setInput(input) {
                    this._input = input;
                }
                getBlocksProvider() {
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
                    labelElement.addEventListener("click", e => this.selectTab(labelElement));
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
                        const closeIconElement = controls.Controls.createIconElement(controls.Controls.getIcon(controls.ICON_CONTROL_CLOSE));
                        closeIconElement.classList.add("closeIcon");
                        closeIconElement.addEventListener("click", e => {
                            e.stopImmediatePropagation();
                            this.closeTab(labelElement);
                        });
                        labelElement.appendChild(closeIconElement);
                        labelElement.classList.add("closeable");
                    }
                    return labelElement;
                }
                closeTab(labelElement) {
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
                    this.dispatchEvent(new CustomEvent(controls.EVENT_TAB_CLOSED, {
                        detail: controls.Control.getControlOf(contentArea.firstChild)
                    }));
                    if (toSelectLabel) {
                        this.selectTab(toSelectLabel);
                    }
                }
                setTabTitle(content, title, icon) {
                    for (let i = 0; i < this._titleBarElement.childElementCount; i++) {
                        const label = this._titleBarElement.children.item(i);
                        const content2 = this.getContentFromLabel(label);
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
                getLabelFromContent(content) {
                    for (let i = 0; i < this._titleBarElement.childElementCount; i++) {
                        const label = this._titleBarElement.children.item(i);
                        const content2 = this.getContentFromLabel(label);
                        if (content2 === content) {
                            return label;
                        }
                    }
                    return null;
                }
                getContentAreaFromLabel(labelElement) {
                    return labelElement["__contentArea"];
                }
                getContentFromLabel(labelElement) {
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
                        const selectedContentArea = this.getContentAreaFromLabel(selectedLabel);
                        selectedContentArea.classList.remove("selected");
                    }
                    toSelectLabel.classList.add("selected");
                    const toSelectContentArea = this.getContentAreaFromLabel(toSelectLabel);
                    toSelectContentArea.classList.add("selected");
                    this._selectionHistoryLabelElement.push(toSelectLabel);
                    this.dispatchEvent(new CustomEvent(controls.EVENT_TAB_SELECTED, {
                        detail: this.getContentFromLabel(toSelectLabel)
                    }));
                    this.dispatchLayoutEvent();
                }
                getSelectedTabContent() {
                    const label = this.getSelectedLabelElement();
                    if (label) {
                        const area = this.getContentAreaFromLabel(label);
                        return controls.Control.getControlOf(area.firstChild);
                    }
                    return null;
                }
                getContentList() {
                    const list = [];
                    for (let i = 0; i < this._titleBarElement.children.length; i++) {
                        const label = this._titleBarElement.children.item(i);
                        const content = this.getContentFromLabel(label);
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
                        part.onPartClosed();
                    });
                    this.addEventListener(ui.controls.EVENT_TAB_SELECTED, (e) => {
                        const part = e.detail;
                        part.onPartShown();
                    });
                }
                addPart(part, closeable = false) {
                    part.addEventListener(ide.EVENT_PART_TITLE_UPDATED, (e) => {
                        this.setTabTitle(part, part.getTitle(), part.getIcon());
                    });
                    this.addTab(part.getTitle(), part.getIcon(), part, closeable);
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
            class DemoEditor extends ide.EditorPart {
                constructor(id, title) {
                    super(id);
                    this.setTitle(title);
                }
                createPart() {
                    this.getElement().innerHTML = "Editor " + this.getId();
                }
            }
            class EditorArea extends ide.PartFolder {
                constructor() {
                    super("EditorArea");
                    //this.addPart(new DemoEditor("demoEditor1", "Level1.scene"), true);
                    //this.addPart(new DemoEditor("demoEditor2", "Level2.scene"), true);
                    //this.addPart(new DemoEditor("demoEditor3", "pack.json"), true);
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
            class EditorBlocksProvider {
            }
            ide.EditorBlocksProvider = EditorBlocksProvider;
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
/// <reference path="../../../phasereditor2d.ui.controls/viewers/FilteredViewer.ts" />
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
                    this._viewer.addEventListener(ui.controls.EVENT_SELECTION, (e) => {
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
            ide.EVENT_PART_DEACTIVATE = "partDeactivate";
            ide.EVENT_PART_ACTIVATE = "partActivate";
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
                    this.initEditors();
                    this._designWindow = new ide.DesignWindow();
                    document.getElementById("body").appendChild(this._designWindow.getElement());
                    this.initEvents();
                }
                async preloadIcons() {
                    return Promise.all(ICONS.map(icon => this.getWorkbenchIcon(icon).preload()));
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
                setActivePart(part) {
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
                        this.dispatchEvent(new CustomEvent(ide.EVENT_PART_DEACTIVATE, { detail: old }));
                    }
                    if (part) {
                        this.toggleActivePart(part);
                    }
                    this.dispatchEvent(new CustomEvent(ide.EVENT_PART_ACTIVATE, { detail: part }));
                }
                toggleActivePart(part) {
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
            var editors;
            (function (editors) {
                var pack;
                (function (pack) {
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
        var ide;
        (function (ide) {
            var editors;
            (function (editors) {
                var pack;
                (function (pack_1) {
                    class AssetPackItem {
                        constructor(pack, data) {
                            this._pack = pack;
                            this._data = data;
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
                    pack_1.AssetPackItem = AssetPackItem;
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
                    class AssetPackUtils {
                        static async preloadAssetPackItems(packItems) {
                            for (const item of packItems) {
                                const type = item.getType();
                                switch (type) {
                                    case "multiatlas": {
                                        const parser = new pack.parsers.MultiAtlasParser(item);
                                        await parser.preload();
                                        break;
                                    }
                                    case "atlas": {
                                        const parser = new pack.parsers.AtlasParser(item);
                                        await parser.preload();
                                        break;
                                    }
                                    case "unityAtlas": {
                                        const parser = new pack.parsers.UnityAtlasParser(item);
                                        await parser.preload();
                                        break;
                                    }
                                    case "atlasXML": {
                                        const parser = new pack.parsers.AtlasXMLParser(item);
                                        await parser.preload();
                                        break;
                                    }
                                    case "spritesheet": {
                                        const parser = new pack.parsers.SpriteSheetParser(item);
                                        await parser.preload();
                                        break;
                                    }
                                }
                            }
                        }
                        static async getAllPacks() {
                            const files = await ide.FileUtils.getFilesWithContentType(pack_2.CONTENT_TYPE_ASSET_PACK);
                            const packs = [];
                            for (const file of files) {
                                const pack = await pack_2.AssetPack.createFromFile(file);
                                packs.push(pack);
                            }
                            return packs;
                        }
                        static getFileFromPackUrl(url) {
                            return ide.FileUtils.getFileFromPath(url);
                        }
                        static getImageFromPackUrl(url) {
                            const file = this.getFileFromPackUrl(url);
                            if (file) {
                                return ide.Workbench.getWorkbench().getFileImage(file);
                            }
                            return null;
                        }
                    }
                    pack_2.AssetPackUtils = AssetPackUtils;
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
                    class FrameData {
                        constructor(index, src, dst, srcSize) {
                            this.index = index;
                            this.src = src;
                            this.dst = dst;
                            this.srcSize = srcSize;
                        }
                    }
                    pack.FrameData = FrameData;
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
                    }
                    pack.ImageFrame = ImageFrame;
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
                        class AbstractAtlasParser {
                            constructor(packItem) {
                                this._packItem = packItem;
                            }
                            async preload() {
                                if (this._packItem["__frames"]) {
                                    return ui.controls.Controls.resolveNothingLoaded();
                                }
                                const data = this._packItem.getData();
                                const dataFile = pack.AssetPackUtils.getFileFromPackUrl(data.atlasURL);
                                let result1 = await ide.FileUtils.preloadFileString(dataFile);
                                const imageFile = pack.AssetPackUtils.getFileFromPackUrl(data.textureURL);
                                const image = ide.FileUtils.getImage(imageFile);
                                let result2 = await image.preload();
                                return Math.max(result1, result2);
                            }
                            parse() {
                                if (this._packItem["__frames"]) {
                                    return this._packItem["__frames"];
                                }
                                const list = [];
                                const data = this._packItem.getData();
                                const dataFile = pack.AssetPackUtils.getFileFromPackUrl(data.atlasURL);
                                const imageFile = pack.AssetPackUtils.getFileFromPackUrl(data.textureURL);
                                const image = ide.FileUtils.getImage(imageFile);
                                if (dataFile) {
                                    const str = ide.FileUtils.getFileStringFromCache(dataFile);
                                    try {
                                        this.parse2(list, image, str);
                                    }
                                    catch (e) {
                                        console.error(e);
                                    }
                                }
                                this._packItem["__frames"] = list;
                                return list;
                            }
                            static buildFrameData(image, frame, index) {
                                const src = new ui.controls.Rect(frame.frame.x, frame.frame.y, frame.frame.w, frame.frame.h);
                                const dst = new ui.controls.Rect(frame.spriteSourceSize.x, frame.spriteSourceSize.y, frame.spriteSourceSize.w, frame.spriteSourceSize.h);
                                const srcSize = new ui.controls.Point(frame.sourceSize.w, frame.sourceSize.h);
                                const frameData = new pack.FrameData(index, src, dst, srcSize);
                                return new pack.ImageFrame(frame.filename, image, frameData);
                            }
                        }
                        parsers.AbstractAtlasParser = AbstractAtlasParser;
                    })(parsers = pack.parsers || (pack.parsers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AbstractAtlasParser.ts" />
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
                        class AtlasParser extends parsers.AbstractAtlasParser {
                            constructor(packItem) {
                                super(packItem);
                            }
                            parse2(imageFrames, image, atlas) {
                                try {
                                    const data = JSON.parse(atlas);
                                    if (Array.isArray(data.frames)) {
                                        for (const frame of data.frames) {
                                            const frameData = AtlasParser.buildFrameData(image, frame, imageFrames.length);
                                            imageFrames.push(frameData);
                                        }
                                    }
                                    else {
                                        for (const name in data.frames) {
                                            const frame = data.frames[name];
                                            frame.filename = name;
                                            const frameData = AtlasParser.buildFrameData(image, frame, imageFrames.length);
                                            imageFrames.push(frameData);
                                        }
                                    }
                                }
                                catch (e) {
                                    console.error(e);
                                }
                            }
                            static buildFrameData(image, frame, index) {
                                const src = new ui.controls.Rect(frame.frame.x, frame.frame.y, frame.frame.w, frame.frame.h);
                                const dst = new ui.controls.Rect(frame.spriteSourceSize.x, frame.spriteSourceSize.y, frame.spriteSourceSize.w, frame.spriteSourceSize.h);
                                const srcSize = new ui.controls.Point(frame.sourceSize.w, frame.sourceSize.h);
                                const frameData = new pack.FrameData(index, src, dst, srcSize);
                                return new pack.ImageFrame(frame.filename, image, frameData);
                            }
                        }
                        parsers.AtlasParser = AtlasParser;
                    })(parsers = pack.parsers || (pack.parsers = {}));
                })(pack = editors.pack || (editors.pack = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./AbstractAtlasParser.ts" />
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
                        class AtlasXMLParser extends parsers.AbstractAtlasParser {
                            constructor(packItem) {
                                super(packItem);
                            }
                            parse2(imageFrames, image, atlas) {
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
                                        const fd = new pack.FrameData(i, new ui.controls.Rect(frameX, frameY, frameW, frameH), new ui.controls.Rect(spriteX, spriteY, spriteW, spriteH), new ui.controls.Point(frameW, frameH));
                                        imageFrames.push(new pack.ImageFrame(name, image, fd));
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
                        class MultiAtlasParser {
                            constructor(packItem) {
                                this._packItem = packItem;
                            }
                            async preload() {
                                if (this._packItem["__frames"]) {
                                    return ui.controls.Controls.resolveNothingLoaded();
                                }
                                const data = this._packItem.getData();
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
                            parse() {
                                if (this._packItem["__frames"]) {
                                    return this._packItem["__frames"];
                                }
                                const list = [];
                                const data = this._packItem.getData();
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
                                                    const frameData = parsers.AtlasParser.buildFrameData(image, frame, list.length);
                                                    list.push(frameData);
                                                }
                                            }
                                        }
                                    }
                                    catch (e) {
                                        console.error(e);
                                    }
                                }
                                this._packItem["__frames"] = list;
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
/// <reference path="./AbstractAtlasParser.ts" />
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
                        class SpriteSheetParser {
                            constructor(packItem) {
                                this._packItem = packItem;
                            }
                            async preload() {
                                if (this._packItem["__frames"]) {
                                    return ui.controls.Controls.resolveNothingLoaded();
                                }
                                const data = this._packItem.getData();
                                const imageFile = pack.AssetPackUtils.getFileFromPackUrl(data.url);
                                const image = ide.FileUtils.getImage(imageFile);
                                return await image.preload();
                            }
                            parse() {
                                if (this._packItem["__frames"]) {
                                    return this._packItem["__frames"];
                                }
                                const frames = [];
                                const data = this._packItem.getData();
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
                                            const fd = new pack.FrameData(i, new ui.controls.Rect(x, y, w, h), new ui.controls.Rect(0, 0, w, h), new ui.controls.Point(w, h));
                                            frames.push(new pack.ImageFrame(i.toString(), image, fd));
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
                                this._packItem["__frames"] = frames;
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
                        class UnityAtlasParser extends parsers.AbstractAtlasParser {
                            parse2(imageFrames, image, atlas) {
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
                                const fd = new pack.FrameData(imageFrames.length, src, dst, srcSize);
                                imageFrames.push(new pack.ImageFrame(spriteName, image, fd));
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
            var viewers;
            (function (viewers) {
                class TreeViewerRenderer {
                    constructor(viewer) {
                        this._viewer = viewer;
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
                    }
                    paintItems(objects, treeIconList, paintItems, x, y) {
                        const viewer = this.getViewer();
                        if (viewer.getCellSize() <= 48) {
                            return super.paintItems(objects, treeIconList, paintItems, x, y);
                        }
                        const b = viewer.getBounds();
                        const offset = this._center ? Math.floor(b.width % (viewer.getCellSize() + viewers.TREE_RENDERER_GRID_PADDING) / 2) : viewers.TREE_RENDERER_GRID_PADDING;
                        return this.paintItems2(objects, treeIconList, paintItems, x + offset, y + viewers.TREE_RENDERER_GRID_PADDING, offset, 0);
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
                                // if (depth > 0) {
                                //     const space = args.h / (depth + 1);
                                //     const arrowH = 5;//space / 2;
                                //     let arrowY = args.y + space;
                                //     ctx.save();
                                //     ctx.lineWidth = 1;
                                //     ctx.strokeStyle = Controls.theme.treeItemForeground;
                                //     for (let i = 0; i < depth; i++) {
                                //         ctx.beginPath();
                                //         ctx.moveTo(args.x - 5, arrowY - arrowH);
                                //         ctx.lineTo(args.x, arrowY);
                                //         ctx.lineTo(args.x - 5, arrowY + arrowH);
                                //         ctx.stroke()
                                //         arrowY += space;
                                //     }
                                //     ctx.restore();
                                // }
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
                            ctx.fillStyle = controls.Controls.theme.treeItemSelectionBackground;
                            ctx.globalAlpha = 0.5;
                            ctx.fillRect(args.x, args.y, args.w, args.h);
                            ctx.restore();
                        }
                    }
                    renderCellFront(args, selected, isLastChild) {
                        if (selected) {
                            const ctx = args.canvasContext;
                            ctx.save();
                            ctx.globalAlpha = 0.3;
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
                                return obj instanceof pack.ImageFrame;
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
                                else if (element instanceof pack.ImageFrame) {
                                    return new viewers.ImageFrameCellRenderer();
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
                                    const type = parent.getType();
                                    switch (type) {
                                        case "multiatlas": {
                                            const parser = new pack.parsers.MultiAtlasParser(parent);
                                            const frames = parser.parse();
                                            return frames;
                                        }
                                        case "atlas": {
                                            const parser = new pack.parsers.AtlasParser(parent);
                                            const frames = parser.parse();
                                            return frames;
                                        }
                                        case "unityAtlas": {
                                            const parser = new pack.parsers.UnityAtlasParser(parent);
                                            const frames = parser.parse();
                                            return frames;
                                        }
                                        case "atlasXML": {
                                            const parser = new pack.parsers.AtlasXMLParser(parent);
                                            const frames = parser.parse();
                                            return frames;
                                        }
                                        case "spritesheet": {
                                            const parser = new pack.parsers.SpriteSheetParser(parent);
                                            const frames = parser.parse();
                                            return frames;
                                        }
                                        default:
                                            break;
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
                                if (obj instanceof pack.ImageFrame) {
                                    return obj.getName();
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
                    renderCell(args) {
                        const img = this.getImage(args.obj);
                        img.paint(args.canvasContext, args.x, args.y, args.w, args.h, args.center);
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
                var pack;
                (function (pack) {
                    var viewers;
                    (function (viewers) {
                        class ImageFrameCellRenderer {
                            renderCell(args) {
                                const item = args.obj;
                                const img = item.getImage();
                                const fd = item.getFrameData();
                                const renderWidth = args.w;
                                const renderHeight = args.h;
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
                                var imgX = args.x + (args.center ? renderWidth / 2 - imgW / 2 : 0);
                                var imgY = args.y + renderHeight / 2 - imgH / 2;
                                const imgDstW = fd.src.w * scale;
                                const imgDstH = fd.src.h * scale;
                                if (imgDstW > 0 && imgDstH > 0) {
                                    img.paintFrame(args.canvasContext, fd.src.x, fd.src.y, fd.src.w, fd.src.h, imgX, imgY, imgDstW, imgDstH);
                                }
                            }
                            cellHeight(args) {
                                return args.viewer.getCellSize();
                            }
                            preload(obj) {
                                const item = obj;
                                return item.getImage().preload();
                            }
                        }
                        viewers.ImageFrameCellRenderer = ImageFrameCellRenderer;
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
/// <reference path="../../EditorBlocksProvider.ts" />
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
                            this._blocksProvider = new scene.SceneEditorBlocksProvider();
                        }
                        static getFactory() {
                            return new SceneEditorFactory();
                        }
                        createPart() {
                            const label = document.createElement("label");
                            label.innerHTML = "Hello Scene Editor";
                            this.getElement().appendChild(label);
                        }
                        getBlocksProvider() {
                            return this._blocksProvider;
                        }
                    }
                    scene.SceneEditor = SceneEditor;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../pack/viewers/AssetPackContentProvider.ts" />
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
                    const SUPPORTED_PACK_ITEM_TYPES = new Set(["image", "atlas", "atlasXML", "multiatlas", "unityAtlas", "spritesheet"]);
                    //const SUPPORTED_PACK_ITEM_TYPES = new Set(["multiatlas"]);
                    class SceneEditorBlocksContentProvider extends editors.pack.viewers.AssetPackContentProvider {
                        constructor(packs) {
                            super();
                            this._items = packs
                                .flatMap(pack => pack.getItems())
                                .filter(item => SUPPORTED_PACK_ITEM_TYPES.has(item.getType()));
                        }
                        getItems() {
                            return this._items;
                        }
                        getRoots(input) {
                            return this._items;
                        }
                        getChildren(parent) {
                            return super.getChildren(parent);
                        }
                    }
                    scene.SceneEditorBlocksContentProvider = SceneEditorBlocksContentProvider;
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
                    class SceneEditorBlocksProvider extends ide.EditorBlocksProvider {
                        async preload() {
                            const packs = await editors.pack.AssetPackUtils.getAllPacks();
                            this._contentProvider = new scene.SceneEditorBlocksContentProvider(packs);
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
                            return new editors.pack.viewers.AssetPackBlocksTreeViewerRenderer(viewer);
                        }
                        getInput() {
                            return this;
                        }
                    }
                    scene.SceneEditorBlocksProvider = SceneEditorBlocksProvider;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../../Part.ts"/>
/// <reference path="../../ViewPart.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var ide;
        (function (ide) {
            var blocks;
            (function (blocks) {
                var viewers = ui.controls.viewers;
                class BlocksView extends ide.ViewerView {
                    constructor() {
                        super("blocksView");
                        this.setTitle("Blocks");
                        this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_BLOCKS));
                    }
                    createViewer() {
                        return new viewers.TreeViewer();
                    }
                    createPart() {
                        super.createPart();
                        this._selectionListener = (e) => this.onPartSelection();
                        ide.Workbench.getWorkbench().addEventListener(ide.EVENT_PART_ACTIVATE, e => this.onWorkbenchPartActivate());
                    }
                    onWorkbenchPartActivate() {
                        const part = ide.Workbench.getWorkbench().getActivePart();
                        if (!part || part instanceof ide.EditorPart && part !== this._activeEditor) {
                            if (this._activeEditor) {
                                this._activeEditor.removeEventListener(ui.controls.EVENT_SELECTION, this._selectionListener);
                            }
                            this._activeEditor = part;
                            this._activeEditor.addEventListener(ui.controls.EVENT_SELECTION, this._selectionListener);
                            this.onPartSelection();
                        }
                    }
                    async onPartSelection() {
                        const provider = this._activeEditor.getBlocksProvider();
                        if (!provider) {
                            this._viewer.setInput(null);
                            this._viewer.setContentProvider(new ui.controls.viewers.EmptyTreeContentProvider());
                        }
                        await provider.preload();
                        this._viewer.setTreeRenderer(provider.getTreeViewerRenderer(this._viewer));
                        this._viewer.setLabelProvider(provider.getLabelProvider());
                        this._viewer.setCellRendererProvider(provider.getCellRendererProvider());
                        this._viewer.setContentProvider(provider.getContentProvider());
                        this._viewer.setInput(provider.getInput());
                        this._viewer.repaint();
                    }
                }
                blocks.BlocksView = BlocksView;
            })(blocks = ide.blocks || (ide.blocks = {}));
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
                        canvas.addEventListener("mousedown", e => this.onMouseDown(e));
                        canvas.addEventListener("wheel", e => this.onWheel(e));
                        canvas.addEventListener("keydown", e => this.onKeyDown(e));
                        canvas.addEventListener("dblclick", e => this.onDoubleClick(e));
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
                    fireSelectionChanged() {
                        this.dispatchEvent(new CustomEvent(controls.EVENT_SELECTION, {
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
                    onMouseDown(e) {
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
                    flatValues_String(values) {
                        return values.join(",");
                    }
                    createGridElement(parent, cols, simpleProps = true) {
                        const div = document.createElement("div");
                        div.classList.add("formGrid", "formGrid-cols-" + cols);
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
                                    text.value = this.flatValues_String(this.getSelection().map(file => file.getName()));
                                });
                            }
                            {
                                // Full Name
                                this.createLabel(comp, "Full Name");
                                const text = this.createText(comp, true);
                                this.addUpdater(() => {
                                    text.value = this.flatValues_String(this.getSelection().map(file => file.getFullName()));
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
                            const filteredViewer = new ui.controls.viewers.FilteredViewer(viewer);
                            filteredViewer.setHandlePosition(false);
                            filteredViewer.style.position = "relative";
                            filteredViewer.style.height = "100%";
                            parent.appendChild(filteredViewer.getElement());
                            this.resizeTo(filteredViewer, parent);
                            this.getPage().addEventListener(ui.controls.EVENT_CONTROL_LAYOUT, (e) => {
                                this.resizeTo(filteredViewer, parent);
                            });
                            this.addUpdater(() => {
                                viewer.setInput(this.getSelection());
                                this.resizeTo(filteredViewer, parent);
                            });
                        }
                        resizeTo(filteredViewer, parent) {
                            setTimeout(() => {
                                filteredViewer.setBounds({
                                    width: parent.clientWidth,
                                    height: parent.clientHeight
                                });
                                filteredViewer.getViewer().repaint();
                            }, 10);
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
                        }
                        this.updateWithSelection();
                    }
                    updateWithSelection() {
                        const n = this._selection.length;
                        for (const pane of this._sectionPanes) {
                            const section = pane.getSection();
                            let show = false;
                            if (section.canEditNumber(n)) {
                                show = true;
                                for (const obj of this._selection) {
                                    if (!section.canEdit(obj)) {
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
                        ide.Workbench.getWorkbench().addEventListener(ide.EVENT_PART_ACTIVATE, e => this.onWorkbenchPartActivate());
                    }
                    onWorkbenchPartActivate() {
                        const part = ide.Workbench.getWorkbench().getActivePart();
                        if (!part || part !== this && part !== this._activePart) {
                            if (this._activePart) {
                                this._activePart.removeEventListener(ui.controls.EVENT_SELECTION, this._selectionListener);
                            }
                            this._activePart = part;
                            this._activePart.addEventListener(ui.controls.EVENT_SELECTION, this._selectionListener);
                            this.onPartSelection();
                        }
                    }
                    onPartSelection() {
                        const sel = this._activePart.getSelection();
                        const provider = this._activePart.getPropertyProvider();
                        this._propertyPage.setSectionProvider(provider);
                        this._propertyPage.setSelection(sel);
                    }
                }
                inspector.InspectorView = InspectorView;
            })(inspector = ide.inspector || (ide.inspector = {}));
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
            var outline;
            (function (outline) {
                class OutlineView extends ide.ViewPart {
                    constructor() {
                        super("outlineView");
                        this.setTitle("Outline");
                        this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_OUTLINE));
                    }
                    createPart() {
                    }
                }
                outline.OutlineView = OutlineView;
            })(outline = ide.outline || (ide.outline = {}));
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
                    renderCell(args) {
                    }
                    cellHeight(args) {
                        return args.viewer.getCellSize();
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
                }
                viewers.RenderCellArgs = RenderCellArgs;
                ;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
