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
    var core;
    (function (core) {
        var pack;
        (function (pack) {
            pack.CONTENT_TYPE_ASSET_PACK = "PhaserAssetPack";
            class AssetPackContentTypeResolver {
                async computeContentType(file) {
                    if (file.getExtension() === "json") {
                        const content = await phasereditor2d.ui.ide.Workbench.getWorkbench().getFileStorage().getFileString(file);
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
                    return core.CONTENT_TYPE_ANY;
                }
            }
            pack.AssetPackContentTypeResolver = AssetPackContentTypeResolver;
        })(pack = core.pack || (core.pack = {}));
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
            controls.EVENT_SELECTION = "selected";
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
                createPart() {
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
                    super.createPart();
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
                            super.createPart();
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
                    this._contentType_icon_Map.set(phasereditor2d.core.pack.CONTENT_TYPE_ASSET_PACK, this.getWorkbenchIcon(ide.ICON_ASSET_PACK));
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
                    this.initContentTypes();
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
                initFileStorage() {
                    this._fileStorage = new phasereditor2d.core.io.ServerFileStorage();
                    return this._fileStorage.reload();
                }
                initContentTypes() {
                    const reg = new phasereditor2d.core.ContentTypeRegistry();
                    reg.registerResolver(new phasereditor2d.core.pack.AssetPackContentTypeResolver());
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
                    var io = phasereditor2d.core.io;
                    class AssetPackEditorFactory extends ide.EditorFactory {
                        constructor() {
                            super("phasereditor2d.AssetPackEditorFactory");
                        }
                        acceptInput(input) {
                            if (input instanceof io.FilePath) {
                                const contentType = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                                return contentType === phasereditor2d.core.pack.CONTENT_TYPE_ASSET_PACK;
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
                            super.createPart();
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
                var scene;
                (function (scene) {
                    class PositionAction {
                        constructor(msg) {
                            let displayList = scene.Editor.getInstance().getObjectScene().sys.displayList;
                            let list = msg.list;
                            this._objects = list.map(id => displayList.getByName(id));
                        }
                        run() {
                            this.runPositionAction();
                            let list = this._objects.map((obj) => {
                                return {
                                    id: obj.name,
                                    x: obj.x,
                                    y: obj.y
                                };
                            });
                            scene.Editor.getInstance().sendMessage({
                                method: "SetObjectPosition",
                                list: list
                            });
                        }
                    }
                    scene.PositionAction = PositionAction;
                    class AlignAction extends PositionAction {
                        constructor(msg) {
                            super(msg);
                            this._align = msg.actionData.align;
                        }
                        runPositionAction() {
                            let editor = scene.Editor.getInstance();
                            let minX = Number.MAX_VALUE;
                            let minY = Number.MAX_VALUE;
                            let maxX = Number.MIN_VALUE;
                            let maxY = Number.MIN_VALUE;
                            let width = 0;
                            let height = 0;
                            let tx = new Phaser.GameObjects.Components.TransformMatrix();
                            let point = new Phaser.Math.Vector2();
                            if (this._objects.length === 1) {
                                minX = scene.ScenePropertiesComponent.get_borderX(editor.sceneProperties);
                                maxX = minX + scene.ScenePropertiesComponent.get_borderWidth(editor.sceneProperties);
                                minY = scene.ScenePropertiesComponent.get_borderY(editor.sceneProperties);
                                maxY = minY + scene.ScenePropertiesComponent.get_borderHeight(editor.sceneProperties);
                            }
                            else {
                                let points = [];
                                let objects = [];
                                for (let obj of this._objects) {
                                    let obj2 = obj;
                                    let w = obj2.width;
                                    let h = obj2.height;
                                    let ox = obj2.originX;
                                    let oy = obj2.originY;
                                    let x = -w * ox;
                                    let y = -h * oy;
                                    obj2.getWorldTransformMatrix(tx);
                                    tx.transformPoint(x, y, point);
                                    points.push(point.clone());
                                    tx.transformPoint(x + w, y, point);
                                    points.push(point.clone());
                                    tx.transformPoint(x + w, y + h, point);
                                    points.push(point.clone());
                                    tx.transformPoint(x, y + h, point);
                                    points.push(point.clone());
                                }
                                for (let point of points) {
                                    minX = Math.min(minX, point.x);
                                    minY = Math.min(minY, point.y);
                                    maxX = Math.max(maxX, point.x);
                                    maxY = Math.max(maxY, point.y);
                                }
                            }
                            for (let obj of this._objects) {
                                let objWidth = obj.displayWidth;
                                let objHeight = obj.displayHeight;
                                let objOriginX = obj.displayOriginX * obj.scaleX;
                                let objOriginY = obj.displayOriginY * obj.scaleY;
                                switch (this._align) {
                                    case "LEFT":
                                        this.setX(obj, minX + objOriginX);
                                        break;
                                    case "RIGHT":
                                        this.setX(obj, maxX - objWidth + objOriginX);
                                        break;
                                    case "HORIZONTAL_CENTER":
                                        this.setX(obj, (minX + maxX) / 2 - objWidth / 2 + objOriginX);
                                        break;
                                    case "TOP":
                                        this.setY(obj, minY + objOriginY);
                                        break;
                                    case "BOTTOM":
                                        this.setY(obj, maxY + height - objHeight + objOriginY);
                                        break;
                                    case "VERTICAL_CENTER":
                                        this.setY(obj, (minY + maxY) / 2 - objHeight / 2 + objOriginY);
                                        break;
                                }
                            }
                        }
                        setX(obj, x) {
                            if (obj.parentContainer) {
                                let tx = obj.parentContainer.getWorldTransformMatrix();
                                let point = tx.applyInverse(x, 0);
                                obj.x = point.x;
                            }
                            else {
                                obj.x = x;
                            }
                        }
                        setY(obj, y) {
                            if (obj.parentContainer) {
                                let tx = obj.parentContainer.getWorldTransformMatrix();
                                let point = tx.applyInverse(0, y);
                                obj.y = point.y;
                            }
                            else {
                                obj.y = y;
                            }
                        }
                    }
                    scene.AlignAction = AlignAction;
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
                    function get_value(data, name, defaultValue) {
                        var value = data[name];
                        if (value === undefined) {
                            return defaultValue;
                        }
                        return value;
                    }
                    function get_property(name, defaultValue) {
                        return function (data) {
                            return get_value(data, name, defaultValue);
                        };
                    }
                    function set_property(name) {
                        return function (data, value) {
                            data[name] = value;
                        };
                    }
                    scene.GameObjectEditorComponent = {
                        get_gameObjectEditorTransparency: get_property("gameObjectEditorTransparency", 1),
                        updateObject: function (obj, data) {
                            obj.alpha *= this.get_gameObjectEditorTransparency(data);
                        }
                    };
                    scene.TransformComponent = {
                        get_x: get_property("x", 0),
                        set_x: set_property("x"),
                        get_y: get_property("y", 0),
                        set_y: set_property("y"),
                        get_scaleX: get_property("scaleX", 1),
                        set_scaleX: set_property("scaleX"),
                        get_scaleY: get_property("scaleY", 1),
                        set_scaleY: set_property("scaleY"),
                        get_angle: get_property("angle", 0),
                        set_angle: set_property("angle"),
                        updateObject: function (obj, data) {
                            obj.x = this.get_x(data);
                            obj.y = this.get_y(data);
                            obj.scaleX = this.get_scaleX(data);
                            obj.scaleY = this.get_scaleY(data);
                            obj.angle = this.get_angle(data);
                        },
                        updateData: function (obj, data) {
                            this.set_x(data, obj.x);
                            this.set_y(data, obj.y);
                            this.set_scaleX(data, obj.scaleX);
                            this.set_scaleY(data, obj.scaleY);
                            this.set_angle(data, obj.angle);
                        }
                    };
                    scene.OriginComponent = {
                        updateObject: function (obj, data) {
                            obj.setOrigin(get_value(data, "originX", 0.5), get_value(data, "originY", 0.5));
                        },
                        updateData: function (obj, data) {
                            data.originX = obj.originX;
                            data.originY = obj.originY;
                        }
                    };
                    scene.TextureComponent = {
                        get_textureKey: get_property("textureKey"),
                        get_textureFrame: get_property("textureFrame"),
                        updateObject: function (obj, data) {
                            var key = this.get_textureKey(data);
                            if (!key) {
                                obj.setTexture("<empty>");
                            }
                        }
                    };
                    scene.TileSpriteComponent = {
                        get_tilePositionX: get_property("tilePositionX", 0),
                        set_tilePositionX: set_property("tilePositionX"),
                        get_tilePositionY: get_property("tilePositionY", 0),
                        set_tilePositionY: set_property("tilePositionY"),
                        get_tileScaleX: get_property("tileScaleX", 1),
                        set_tileScaleX: set_property("tileScaleX"),
                        get_tileScaleY: get_property("tileScaleY", 1),
                        set_tileScaleY: set_property("tileScaleY"),
                        get_width: get_property("width", -1),
                        set_width: set_property("width"),
                        get_height: get_property("height", -1),
                        set_height: set_property("height"),
                        updateObject: function (obj, data) {
                            obj.setTilePosition(this.get_tilePositionX(data), this.get_tilePositionY(data));
                            obj.setTileScale(this.get_tileScaleX(data), this.get_tileScaleY(data));
                            obj.width = this.get_width(data);
                            obj.height = this.get_height(data);
                        },
                        updateData: function (obj, data) {
                            this.set_tilePositionX(data, obj.tilePositionX);
                            this.set_tilePositionY(data, obj.tilePositionY);
                            this.set_tileScaleX(data, obj.tileScaleX);
                            this.set_tileScaleY(data, obj.tileScaleY);
                            this.set_width(data, obj.width);
                            this.set_height(data, obj.height);
                        }
                    };
                    scene.FlipComponent = {
                        get_flipX: get_property("flipX", false),
                        get_flipY: get_property("flipY", false),
                        updateObject: function (obj, data) {
                            obj.flipX = this.get_flipX(data);
                            obj.flipY = this.get_flipY(data);
                        }
                    };
                    scene.BitmapTextComponent = {
                        get_fontSize: get_property("fontSize", 0),
                        get_align: get_property("align", 0),
                        get_letterSpacing: get_property("letterSpacing", 0),
                        get_fontAssetKey: get_property("fontAssetKey"),
                        // the BitmapText object has a default origin of 0, 0
                        get_originX: get_property("originX", 0),
                        get_originY: get_property("originY", 0),
                        updateObject: function (obj, data) {
                            obj.text = scene.TextualComponent.get_text(data);
                            obj.fontSize = this.get_fontSize(data);
                            obj.align = this.get_align(data);
                            obj.letterSpacing = this.get_letterSpacing(data);
                            obj.setOrigin(this.get_originX(data), this.get_originY(data));
                        }
                    };
                    scene.DynamicBitmapTextComponent = {
                        get_cropWidth: get_property("cropWidth", 0),
                        get_cropHeight: get_property("cropHeight", 0),
                        get_scrollX: get_property("scrollX", 0),
                        get_scrollY: get_property("scrollY", 0),
                        updateObject: function (obj, data) {
                            obj.cropWidth = this.get_cropWidth(data);
                            obj.cropHeight = this.get_cropHeight(data);
                            obj.scrollX = this.get_scrollX(data);
                            obj.scrollY = this.get_scrollY(data);
                        }
                    };
                    scene.TextualComponent = {
                        get_text: get_property("text", ""),
                        updateObject: function (obj, data) {
                            obj.text = data.text;
                        }
                    };
                    scene.TextComponent = {
                        updateObject: function (obj, data) {
                            obj.style.align = data.align || "left";
                            obj.style.fontFamily = data.fontFamily || "Courier";
                            obj.style.fontSize = data.fontSize || "16px";
                            obj.style.fontStyle = data.fontStyle || "normal";
                            obj.style.backgroundColor = data.backgroundColor || null;
                            obj.style.color = data.color || "#fff";
                            obj.style.stroke = data.stroke || "#fff";
                            obj.style.strokeThickness = data.strokeThickness || 0;
                            obj.style.maxLines = data.maxLines || 0;
                            obj.style.fixedWidth = data.fixedWidth || 0;
                            obj.style.fixedHeight = data.fixedHeight || 0;
                            obj.style.baselineX = data.baselineX || 1.2;
                            obj.style.baselineY = data.baselineY || 1.4;
                            obj.style.shadowOffsetX = data["shadow.offsetX"] || 0;
                            obj.style.shadowOffsetY = data["shadow.offsetY"] || 0;
                            obj.style.shadowColor = data["shadow.color"] || "#000";
                            obj.style.shadowBlur = data["shadow.blur"] || 0;
                            obj.style.shadowStroke = data["shadow.stroke"] || false;
                            obj.style.shadowFill = data["shadow.fill"] || false;
                            obj.style.setWordWrapWidth(data["wordWrap.width"] || 0, data["wordWrap.useAdvancedWrap"] || false);
                            obj.style.update(true);
                            obj.setLineSpacing(data.lineSpacing || 0);
                            obj.setPadding(data.paddingLeft, data.paddingTop, data.paddingRight, data.paddingBottom);
                            // Text object has default origin at 0,0
                            obj.setOrigin(data.originX || 0, data.originY || 0);
                        }
                    };
                    scene.VisibleComponent = {
                        get_visible: get_property("visible", true),
                        updateObject: function (obj, data) {
                            obj.alpha = this.get_visible(data) ? 1 : 0.5;
                        }
                    };
                    scene.ScenePropertiesComponent = {
                        get_snapEnabled: get_property("snapEnabled", false),
                        get_snapWidth: get_property("snapWidth", 16),
                        get_snapHeight: get_property("snapHeight", 16),
                        get_backgroundColor: get_property("backgroundColor", "192,192,182"),
                        get_foregroundColor: get_property("foregroundColor", "255,255,255"),
                        get_borderX: get_property("borderX", 0),
                        get_borderY: get_property("borderY", 0),
                        get_borderWidth: get_property("borderWidth", 800),
                        get_borderHeight: get_property("borderHeight", 600),
                    };
                    scene.TintComponent = {
                        get_isTinted: get_property("isTinted", false),
                        get_tintFill: get_property("tintFill", false),
                        get_tintTopLeft: get_property("tintTopLeft", 0xffffff),
                        get_tintTopRight: get_property("tintTopRight", 0xffffff),
                        get_tintBottomLeft: get_property("tintBottomLeft", 0xffffff),
                        get_tintBottomRight: get_property("tintBottomRight", 0xffffff),
                        updateObject: function (obj, data) {
                            if (this.get_isTinted(data)) {
                                if (this.get_tintFill(data)) {
                                    obj.setTintFill(this.get_tintTopLeft(data), this.get_tintTopRight(data), this.get_tintBottomLeft(data), this.get_tintBottomRight(data));
                                }
                                else {
                                    obj.setTint(this.get_tintTopLeft(data), this.get_tintTopRight(data), this.get_tintBottomLeft(data), this.get_tintBottomRight(data));
                                }
                            }
                            else {
                                obj.clearTint();
                            }
                        }
                    };
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
                (function (scene_1) {
                    class Create {
                        constructor(interactive = true) {
                            this._interactive = interactive;
                        }
                        createWorld(scene, displayList) {
                            var list = displayList.children;
                            for (var i = 0; i < list.length; i++) {
                                var data = list[i];
                                this.createObject(scene, data);
                            }
                        }
                        createObject(scene, data) {
                            var type = data["-type"];
                            var obj;
                            let add = scene.add;
                            switch (type) {
                                case "Image":
                                case "Sprite":
                                    var x = scene_1.TransformComponent.get_x(data);
                                    var y = scene_1.TransformComponent.get_y(data);
                                    var key = scene_1.TextureComponent.get_textureKey(data);
                                    var frame = scene_1.TextureComponent.get_textureFrame(data);
                                    obj = add.image(x, y, key, frame);
                                    break;
                                case "TileSprite":
                                    var x = scene_1.TransformComponent.get_x(data);
                                    var y = scene_1.TransformComponent.get_y(data);
                                    var width = scene_1.TileSpriteComponent.get_width(data);
                                    var height = scene_1.TileSpriteComponent.get_height(data);
                                    var key = scene_1.TextureComponent.get_textureKey(data);
                                    var frame = scene_1.TextureComponent.get_textureFrame(data);
                                    obj = add.tileSprite(x, y, width, height, key, frame);
                                    break;
                                case "BitmapText":
                                    var x = scene_1.TransformComponent.get_x(data);
                                    var y = scene_1.TransformComponent.get_y(data);
                                    var key = scene_1.BitmapTextComponent.get_fontAssetKey(data);
                                    obj = add.bitmapText(x, y, key);
                                    break;
                                case "DynamicBitmapText":
                                    var x = scene_1.TransformComponent.get_x(data);
                                    var y = scene_1.TransformComponent.get_y(data);
                                    var key = scene_1.BitmapTextComponent.get_fontAssetKey(data);
                                    obj = add.dynamicBitmapText(x, y, key);
                                    break;
                                case "Text":
                                    var x = scene_1.TransformComponent.get_x(data);
                                    var y = scene_1.TransformComponent.get_y(data);
                                    var text = scene_1.TextualComponent.get_text(data);
                                    obj = add.text(x, y, text);
                                    break;
                            }
                            if (this._interactive) {
                                switch (type) {
                                    case "TileSprite":
                                        if (scene_1.Editor.getInstance().isWebGL()) {
                                            //obj.setInteractive(TileSpriteCallback);
                                            obj.setInteractive(getAlpha_RenderTexture);
                                        }
                                        else {
                                            obj.setInteractive(getAlpha_CanvasTexture);
                                        }
                                        break;
                                    case "BitmapText":
                                    case "DynamicBitmapText":
                                        obj.setInteractive(inBounds_BitmapText);
                                        break;
                                    case "Text":
                                        obj.setInteractive();
                                        break;
                                    default:
                                        obj.setInteractive(getAlpha_SharedTexture);
                                        break;
                                }
                            }
                            this.updateObject(obj, data);
                        }
                        updateObject(obj, data) {
                            var type = data["-type"];
                            obj.name = data["-id"];
                            scene_1.VisibleComponent.updateObject(obj, data);
                            switch (type) {
                                case "Image":
                                case "Sprite":
                                case "TileSprite":
                                    scene_1.TextureComponent.updateObject(obj, data);
                                    break;
                            }
                            switch (type) {
                                case "Image":
                                case "Sprite":
                                case "TileSprite":
                                case "BitmapText":
                                case "DynamicBitmapText":
                                case "Text":
                                    scene_1.GameObjectEditorComponent.updateObject(obj, data);
                                    scene_1.TransformComponent.updateObject(obj, data);
                                    scene_1.OriginComponent.updateObject(obj, data);
                                    scene_1.FlipComponent.updateObject(obj, data);
                                    scene_1.TintComponent.updateObject(obj, data);
                                    break;
                            }
                            switch (type) {
                                case "TileSprite":
                                    scene_1.TileSpriteComponent.updateObject(obj, data);
                                    break;
                                case "BitmapText":
                                    scene_1.BitmapTextComponent.updateObject(obj, data);
                                    break;
                                case "DynamicBitmapText":
                                    scene_1.BitmapTextComponent.updateObject(obj, data);
                                    scene_1.DynamicBitmapTextComponent.updateObject(obj, data);
                                    break;
                                case "Text":
                                    scene_1.TextualComponent.updateObject(obj, data);
                                    scene_1.TextComponent.updateObject(obj, data);
                                    break;
                            }
                        }
                    }
                    scene_1.Create = Create;
                    function inBounds_BitmapText(hitArea, x, y, gameObject) {
                        // the bitmaptext width is considered a displayWidth, it is already multiplied by the scale
                        let w = gameObject.width / gameObject.scaleX;
                        let h = gameObject.height / gameObject.scaleY;
                        return x >= 0 && y >= 0 && x <= w && y <= h;
                    }
                    function inBounds_TileSprite(hitArea, x, y, obj) {
                        return x >= 0 && y >= 0 && x <= obj.width && y <= obj.height;
                    }
                    // this is not working at this moment!
                    function getAlpha_RenderTexture(hitArea, x, y, sprite) {
                        var hitBounds = x >= 0 && y >= 0 && x <= sprite.width && y <= sprite.height;
                        if (!hitBounds) {
                            return false;
                        }
                        const scene = scene_1.Editor.getInstance().getObjectScene();
                        const renderTexture = new Phaser.GameObjects.RenderTexture(scene, 0, 0, 1, 1);
                        const scaleX = sprite.scaleX;
                        const scaleY = sprite.scaleY;
                        const originX = sprite.originX;
                        const originY = sprite.originY;
                        const angle = sprite.angle;
                        sprite.scaleX = 1;
                        sprite.scaleY = 1;
                        sprite.originX = 0;
                        sprite.originY = 0;
                        sprite.angle = 0;
                        renderTexture.draw([sprite], -x, -y);
                        sprite.scaleX = scaleX;
                        sprite.scaleY = scaleY;
                        sprite.originX = originX;
                        sprite.originY = originY;
                        sprite.angle = angle;
                        const colorArray = [];
                        renderTexture.snapshotPixel(0, 0, (function (colorArray) {
                            return function (c) {
                                consoleLog(c);
                                colorArray[0] = c;
                            };
                        })(colorArray));
                        renderTexture.destroy();
                        const color = colorArray[0];
                        const alpha = color.alpha;
                        return alpha > 0;
                    }
                    function getAlpha_CanvasTexture(hitArea, x, y, sprite) {
                        if (sprite.flipX) {
                            x = 2 * sprite.displayOriginX - x;
                        }
                        if (sprite.flipY) {
                            y = 2 * sprite.displayOriginY - y;
                        }
                        var alpha = getCanvasTexturePixelAlpha(x, y, sprite.texture);
                        return alpha > 0;
                    }
                    function getCanvasTexturePixelAlpha(x, y, canvasTexture) {
                        if (canvasTexture) {
                            //if (x >= 0 && x < canvasTexture.width && y >= 0 && y < canvasTexture.height) 
                            let imgData = canvasTexture.getContext().getImageData(x, y, 1, 1);
                            let rgb = imgData.data;
                            let alpha = rgb[3];
                            return alpha;
                        }
                        return 0;
                    }
                    function getAlpha_SharedTexture(hitArea, x, y, sprite) {
                        if (sprite.flipX) {
                            x = 2 * sprite.displayOriginX - x;
                        }
                        if (sprite.flipY) {
                            y = 2 * sprite.displayOriginY - y;
                        }
                        const textureManager = scene_1.Editor.getInstance().getGame().textures;
                        var alpha = textureManager.getPixelAlpha(x, y, sprite.texture.key, sprite.frame.name);
                        return alpha;
                    }
                    ;
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
                    class Editor {
                        constructor() {
                            this._closed = false;
                            this._isReloading = false;
                            this.selection = [];
                            Editor._instance = this;
                            this.openSocket();
                        }
                        hitTestPointer(scene, pointer) {
                            const input = scene.game.input;
                            const real = input.real_hitTest;
                            const fake = input.hitTest;
                            input.hitTest = real;
                            const result = scene.input.hitTestPointer(pointer);
                            input.hitTest = fake;
                            return result;
                        }
                        static getInstance() {
                            return Editor._instance;
                        }
                        repaint() {
                            consoleLog("repaint");
                            this._game.loop.tick();
                        }
                        stop() {
                            consoleLog("loop.stop");
                            this._game.loop.stop();
                        }
                        getCreate() {
                            return this._create;
                        }
                        getGame() {
                            return this._game;
                        }
                        getObjectScene() {
                            return this._objectScene;
                        }
                        getToolScene() {
                            return this.getObjectScene().getToolScene();
                        }
                        sceneCreated() {
                            const self = this;
                            this._game.canvas.addEventListener("mousedown", function (e) {
                                if (self._closed) {
                                    return;
                                }
                                if (self.getToolScene().containsPointer()) {
                                    self.getToolScene().onToolsMouseDown();
                                }
                                else {
                                    self.getObjectScene().getDragCameraManager().onMouseDown(e);
                                    self.getObjectScene().getPickManager().onMouseDown(e);
                                    const dragging = self.getObjectScene().getDragObjectsManager().onMouseDown(e);
                                    if (!dragging) {
                                        self.getToolScene().onSelectionDragMouseDown(e);
                                    }
                                }
                            });
                            this._game.canvas.addEventListener("mousemove", function (e) {
                                if (self._closed) {
                                    return;
                                }
                                if (self.getToolScene().isEditing()) {
                                    self.getToolScene().onToolsMouseMove();
                                }
                                else {
                                    self.getObjectScene().getDragObjectsManager().onMouseMove(e);
                                    self.getObjectScene().getDragCameraManager().onMouseMove(e);
                                    const repaint = self.getToolScene().onSelectionDragMouseMove(e);
                                    if (repaint) {
                                        self.repaint();
                                    }
                                }
                            });
                            this._game.canvas.addEventListener("mouseup", function (e) {
                                if (self._closed) {
                                    return;
                                }
                                if (self.getToolScene().isEditing()) {
                                    self.getToolScene().onToolsMouseUp();
                                }
                                else {
                                    //self.getObjectScene().getDragObjectsManager().onMouseUp();
                                    self.getObjectScene().getDragCameraManager().onMouseUp();
                                    const found = self.getObjectScene().getPickManager().onMouseUp(e);
                                    self.getObjectScene().getDragObjectsManager().onMouseUp();
                                    if (found) {
                                        self.getToolScene().selectionDragClear();
                                    }
                                    else {
                                        self.getToolScene().onSelectionDragMouseUp(e);
                                    }
                                }
                            });
                            this._game.canvas.addEventListener("mouseleave", function () {
                                if (self._closed) {
                                    return;
                                }
                                self.getObjectScene().getDragObjectsManager().onMouseUp();
                                self.getObjectScene().getDragCameraManager().onMouseUp();
                            });
                            this.sendMessage({
                                method: "GetInitialState"
                            });
                        }
                        sendKeyDown(e) {
                            const data = {
                                keyCode: e.keyCode,
                                ctrlKey: e.ctrlKey || e.metaKey,
                                shiftKey: e.shiftKey,
                            };
                            this.sendMessage({
                                method: "KeyDown",
                                data: data
                            });
                        }
                        onResize() {
                            for (let scene of this._game.scene.scenes) {
                                const scene2 = scene;
                                scene2.cameras.main.setSize(window.innerWidth, window.innerHeight);
                                scene2.scale.resize(window.innerWidth, window.innerHeight);
                            }
                            this.repaint();
                        }
                        openSocket() {
                            consoleLog("Open socket");
                            this._socket = new WebSocket(this.getWebSocketUrl());
                            const self = this;
                            // we should create the socket when the editor scene is ready, it means, the first time the preload method is called.
                            this._socket.onopen = function () {
                                self.sendMessage({
                                    method: "GetCreateGame"
                                });
                            };
                            this._socket.onmessage = function (event) {
                                var msg = JSON.parse(event.data);
                                self.onServerMessage(msg);
                            };
                            this._socket.onclose = function (event) {
                                self.onClosedSocket();
                            };
                            window.addEventListener("beforeunload", (event) => {
                                if (self._socket) {
                                    consoleLog("Closing socket...");
                                    self.closeSocket();
                                }
                                //event.preventDefault();
                                //event.returnValue = "";
                            });
                        }
                        closeSocket() {
                            this._socket.onclose = function () { };
                            this._socket.close();
                        }
                        onClosedSocket() {
                            consoleLog("Socket closed");
                            if (this._isReloading) {
                                consoleLog("Closed because a reload.");
                                return;
                            }
                            this._closed = true;
                            let body = document.getElementById("body");
                            var elem = document.createElement("div");
                            elem.innerHTML = "<p><br><br><br>Lost the connection with Phaser Editor</p><button onclick='document.location.reload()'>Reload</button>";
                            elem.setAttribute("class", "lostConnection");
                            body.appendChild(elem);
                        }
                        onSelectObjects(msg) {
                            this.selection = msg.objectIds;
                            this.getToolScene().updateSelectionObjects();
                            let list = [];
                            let point = new Phaser.Math.Vector2(0, 0);
                            let tx = new Phaser.GameObjects.Components.TransformMatrix();
                            for (let obj of this.getToolScene().getSelectedObjects()) {
                                let objTx = obj;
                                objTx.getWorldTransformMatrix(tx);
                                tx.transformPoint(0, 0, point);
                                let info = {
                                    id: obj.name
                                };
                                if (obj instanceof Phaser.GameObjects.BitmapText) {
                                    info.displayWidth = obj.width;
                                    info.displayHeight = obj.height;
                                }
                                else {
                                    info.displayWidth = obj.displayWidth;
                                    info.displayHeight = obj.displayHeight;
                                }
                                list.push(info);
                            }
                            this.sendMessage({
                                method: "SetObjectDisplayProperties",
                                list: list
                            });
                        }
                        ;
                        onUpdateObjects(msg) {
                            var list = msg.objects;
                            for (var i = 0; i < list.length; i++) {
                                var objData = list[i];
                                var id = objData["-id"];
                                var obj = this._objectScene.sys.displayList.getByName(id);
                                this._create.updateObject(obj, objData);
                            }
                        }
                        onReloadPage() {
                            this._isReloading = true;
                            this._socket.close();
                            window.location.reload();
                        }
                        onUpdateSceneProperties(msg) {
                            this.sceneProperties = msg.sceneProperties;
                            this.getObjectScene().updateBackground();
                            this.getToolScene().updateFromSceneProperties();
                            this.updateBodyColor();
                        }
                        updateBodyColor() {
                            const body = document.getElementsByTagName("body")[0];
                            body.style.backgroundColor = "rgb(" + scene_2.ScenePropertiesComponent.get_backgroundColor(this.sceneProperties) + ")";
                        }
                        onCreateGame(msg) {
                            const self = this;
                            // update the model
                            this._webgl = msg.webgl;
                            this._chromiumWebview = msg.chromiumWebview;
                            this.sceneProperties = msg.sceneProperties;
                            // create the game
                            this._create = new scene_2.Create();
                            this._game = new Phaser.Game({
                                title: "Phaser Editor 2D - Web Scene Editor",
                                width: window.innerWidth,
                                height: window.innerWidth,
                                // WEBGL is problematic on Linux
                                type: this._webgl ? Phaser.WEBGL : Phaser.CANVAS,
                                render: {
                                    pixelArt: true
                                },
                                audio: {
                                    noAudio: true
                                },
                                url: "https://phasereditor2d.com",
                                scale: {
                                    mode: Phaser.Scale.RESIZE
                                }
                            });
                            this._game.config.postBoot = function (game) {
                                consoleLog("Game booted");
                                setTimeout(() => self.stop(), 500);
                            };
                            // default hitTest is a NOOP, so it does not run heavy tests in all mouse moves.
                            const input = this._game.input;
                            input.real_hitTest = input.hitTest;
                            input.hitTest = function () {
                                return [];
                            };
                            // --
                            this._objectScene = new scene_2.ObjectScene();
                            this._game.scene.add("ObjectScene", this._objectScene);
                            this._game.scene.add("ToolScene", scene_2.ToolScene);
                            this._game.scene.start("ObjectScene", {
                                displayList: msg.displayList,
                                projectUrl: msg.projectUrl,
                                pack: msg.pack
                            });
                            this._resizeToken = 0;
                            window.addEventListener('resize', function (event) {
                                if (self._closed) {
                                    return;
                                }
                                self._resizeToken += 1;
                                setTimeout((function (token) {
                                    return function () {
                                        if (token === self._resizeToken) {
                                            self.onResize();
                                        }
                                    };
                                })(self._resizeToken), 200);
                            }, false);
                            window.addEventListener("wheel", function (e) {
                                if (self._closed) {
                                    return;
                                }
                                self.getObjectScene().onMouseWheel(e);
                                self.repaint();
                            });
                            this.updateBodyColor();
                        }
                        snapValueX(x) {
                            const props = this.sceneProperties;
                            if (scene_2.ScenePropertiesComponent.get_snapEnabled(props)) {
                                const snap = scene_2.ScenePropertiesComponent.get_snapWidth(props);
                                return Math.round(x / snap) * snap;
                            }
                            return x;
                        }
                        snapValueY(y) {
                            const props = this.sceneProperties;
                            if (scene_2.ScenePropertiesComponent.get_snapEnabled(props)) {
                                const snap = scene_2.ScenePropertiesComponent.get_snapHeight(props);
                                return Math.round(y / snap) * snap;
                            }
                            return y;
                        }
                        onDropObjects(msg) {
                            consoleLog("onDropObjects()");
                            const list = msg.list;
                            for (let model of list) {
                                this._create.createObject(this.getObjectScene(), model);
                            }
                            this.repaint();
                        }
                        onDeleteObjects(msg) {
                            let scene = this.getObjectScene();
                            let list = msg.list;
                            for (let id of list) {
                                var obj = scene.sys.displayList.getByName(id);
                                if (obj) {
                                    obj.destroy();
                                }
                            }
                        }
                        onResetScene(msg) {
                            let scene = this.getObjectScene();
                            scene.removeAllObjects();
                            this._create.createWorld(scene, msg.displayList);
                        }
                        onRunPositionAction(msg) {
                            let actionName = msg.action;
                            let action;
                            switch (actionName) {
                                case "Align":
                                    action = new scene_2.AlignAction(msg);
                                    break;
                            }
                            if (action) {
                                action.run();
                            }
                        }
                        onServerMessage(batch) {
                            consoleLog("onServerMessage:");
                            consoleLog(batch);
                            consoleLog("----");
                            var list = batch.list;
                            this.processMessageList(0, list);
                        }
                        ;
                        onLoadAssets(index, list) {
                            let loadMsg = list[index];
                            const self = this;
                            if (loadMsg.pack) {
                                let scene = this.getObjectScene();
                                Editor.getInstance().stop();
                                scene.load.once(Phaser.Loader.Events.COMPLETE, (function (index2, list2) {
                                    return function () {
                                        consoleLog("Loader complete.");
                                        self.processMessageList(index2, list2);
                                    };
                                })(index + 1, list), this);
                                consoleLog("Load: ");
                                consoleLog(loadMsg.pack);
                                scene.load.crossOrigin = "anonymous";
                                scene.load.addPack(loadMsg.pack);
                                scene.load.start();
                                setTimeout(() => this.repaint(), 100);
                            }
                            else {
                                this.processMessageList(index + 1, list);
                            }
                        }
                        onSetObjectOriginKeepPosition(msg) {
                            let list = msg.list;
                            let value = msg.value;
                            let is_x_axis = msg.axis === "x";
                            let displayList = this.getObjectScene().sys.displayList;
                            let point = new Phaser.Math.Vector2();
                            let tx = new Phaser.GameObjects.Components.TransformMatrix();
                            let data = [];
                            for (let id of list) {
                                let obj = displayList.getByName(id);
                                let x = -obj.width * obj.originX;
                                let y = -obj.height * obj.originY;
                                obj.getWorldTransformMatrix(tx);
                                tx.transformPoint(x, y, point);
                                data.push({
                                    obj: obj,
                                    x: point.x,
                                    y: point.y
                                });
                            }
                            for (let item of data) {
                                let obj = item.obj;
                                if (is_x_axis) {
                                    obj.setOrigin(value, obj.originY);
                                }
                                else {
                                    obj.setOrigin(obj.originX, value);
                                }
                            }
                            this.repaint();
                            let list2 = [];
                            for (let item of data) {
                                let obj = item.obj;
                                // restore the position!
                                let x = -obj.width * obj.originX;
                                let y = -obj.height * obj.originY;
                                obj.getWorldTransformMatrix(tx);
                                tx.transformPoint(x, y, point);
                                obj.x += item.x - point.x;
                                obj.y += item.y - point.y;
                                // build message data
                                list2.push({
                                    id: obj.name,
                                    originX: obj.originX,
                                    originY: obj.originY,
                                    x: obj.x,
                                    y: obj.y
                                });
                            }
                            Editor.getInstance().sendMessage({
                                method: "SetObjectOrigin",
                                list: list2
                            });
                        }
                        onSetCameraState(msg) {
                            let cam = this.getObjectScene().cameras.main;
                            if (msg.cameraState.scrollX !== undefined) {
                                cam.scrollX = msg.cameraState.scrollX;
                                cam.scrollY = msg.cameraState.scrollY;
                                cam.zoom = msg.cameraState.zoom;
                            }
                        }
                        onSetInteractiveTool(msg) {
                            const tools = [];
                            for (let name of msg.list) {
                                const tools2 = scene_2.ToolFactory.createByName(name);
                                for (let tool of tools2) {
                                    tools.push(tool);
                                }
                            }
                            this._transformLocalCoords = msg.transformLocalCoords;
                            this.getToolScene().setTools(tools);
                        }
                        isTransformLocalCoords() {
                            return this._transformLocalCoords;
                        }
                        isWebGL() {
                            return this._webgl;
                        }
                        isChromiumWebview() {
                            return this._chromiumWebview;
                        }
                        onSetTransformCoords(msg) {
                            this._transformLocalCoords = msg.transformLocalCoords;
                        }
                        onGetPastePosition(msg) {
                            let x = 0;
                            let y = 0;
                            if (msg.placeAtCursorPosition) {
                                const pointer = this.getObjectScene().input.activePointer;
                                const point = this.getObjectScene().getScenePoint(pointer.x, pointer.y);
                                x = point.x;
                                y = point.y;
                            }
                            else {
                                let cam = this.getObjectScene().cameras.main;
                                x = cam.midPoint.x;
                                y = cam.midPoint.y;
                            }
                            this.sendMessage({
                                method: "PasteEvent",
                                parent: msg.parent,
                                x: x,
                                y: y
                            });
                        }
                        onRevealObject(msg) {
                            const sprite = this.getObjectScene().sys.displayList.getByName(msg.id);
                            if (sprite) {
                                const tx = sprite.getWorldTransformMatrix();
                                let p = new Phaser.Math.Vector2();
                                tx.transformPoint(0, 0, p);
                                const cam = this.getObjectScene().cameras.main;
                                cam.setScroll(p.x - cam.width / 2, p.y - cam.height / 2);
                            }
                        }
                        processMessageList(startIndex, list) {
                            for (var i = startIndex; i < list.length; i++) {
                                var msg = list[i];
                                var method = msg.method;
                                switch (method) {
                                    case "ReloadPage":
                                        this.onReloadPage();
                                        break;
                                    case "CreateGame":
                                        this.onCreateGame(msg);
                                        break;
                                    case "UpdateObjects":
                                        this.onUpdateObjects(msg);
                                        break;
                                    case "SelectObjects":
                                        this.onSelectObjects(msg);
                                        break;
                                    case "UpdateSceneProperties":
                                        this.onUpdateSceneProperties(msg);
                                        break;
                                    case "DropObjects":
                                        this.onDropObjects(msg);
                                        break;
                                    case "DeleteObjects":
                                        this.onDeleteObjects(msg);
                                        break;
                                    case "ResetScene":
                                        this.onResetScene(msg);
                                        break;
                                    case "RunPositionAction":
                                        this.onRunPositionAction(msg);
                                        break;
                                    case "LoadAssets":
                                        this.onLoadAssets(i, list);
                                        // break the loop, the remaining messages are processed after the load
                                        return;
                                    case "SetObjectOriginKeepPosition":
                                        this.onSetObjectOriginKeepPosition(msg);
                                        break;
                                    case "SetCameraState":
                                        this.onSetCameraState(msg);
                                        break;
                                    case "SetInteractiveTool":
                                        this.onSetInteractiveTool(msg);
                                        break;
                                    case "SetTransformCoords":
                                        this.onSetTransformCoords(msg);
                                        break;
                                    case "GetPastePosition":
                                        this.onGetPastePosition(msg);
                                        break;
                                    case "RevealObject":
                                        this.onRevealObject(msg);
                                        break;
                                }
                            }
                            this.repaint();
                        }
                        sendMessage(msg) {
                            consoleLog("Sending message:");
                            consoleLog(msg);
                            consoleLog("----");
                            this._socket.send(JSON.stringify(msg));
                        }
                        getWebSocketUrl() {
                            var loc = document.location;
                            var channel = this.getChannelId();
                            return "ws://" + loc.host + "/ws/api?channel=" + channel;
                        }
                        getChannelId() {
                            var s = document.location.search;
                            var i = s.indexOf("=");
                            var c = s.substring(i + 1);
                            return c;
                        }
                        getWorldBounds(sprite, points) {
                            let w = sprite.width;
                            let h = sprite.height;
                            if (sprite instanceof Phaser.GameObjects.BitmapText) {
                                // the bitmaptext width is considered a displayWidth, it is already multiplied by the scale
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
                            let worldTx = sprite.getWorldTransformMatrix();
                            worldTx.transformPoint(x, y, points[0]);
                            worldTx.transformPoint(x + w * flipX, y, points[1]);
                            worldTx.transformPoint(x + w * flipX, y + h * flipY, points[2]);
                            worldTx.transformPoint(x, y + h * flipY, points[3]);
                            let cam = this.getObjectScene().cameras.main;
                            for (let p of points) {
                                p.set((p.x - cam.scrollX) * cam.zoom, (p.y - cam.scrollY) * cam.zoom);
                            }
                        }
                    }
                    scene_2.Editor = Editor;
                    class PaintDelayUtil {
                        constructor() {
                            this._delayPaint = Editor.getInstance().isChromiumWebview();
                        }
                        startPaintLoop() {
                            if (this._delayPaint) {
                                this._now = Date.now();
                            }
                        }
                        shouldPaintThisTime() {
                            if (this._delayPaint) {
                                const now = Date.now();
                                if (now - this._now > 40) {
                                    this._now = now;
                                    return true;
                                }
                                return false;
                            }
                            return true;
                        }
                    }
                    scene_2.PaintDelayUtil = PaintDelayUtil;
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
                    class BuildMessage {
                        static SetTileSpriteProperties(objects) {
                            const list = [];
                            for (let obj of objects) {
                                const sprite = obj;
                                const data = { id: sprite.name };
                                scene.TileSpriteComponent.updateData(sprite, data);
                                list.push(data);
                            }
                            return {
                                method: "SetTileSpriteProperties",
                                list: list
                            };
                        }
                        static SetOriginProperties(objects) {
                            const list = [];
                            for (let obj of objects) {
                                const data = { id: obj.name };
                                scene.OriginComponent.updateData(obj, data);
                                scene.TransformComponent.set_x(data, obj.x);
                                scene.TransformComponent.set_y(data, obj.y);
                                list.push(data);
                            }
                            return {
                                method: "SetOriginProperties",
                                list: list
                            };
                        }
                        static SetTransformProperties(objects) {
                            const list = [];
                            for (let obj of objects) {
                                const data = { id: obj.name };
                                scene.TransformComponent.updateData(obj, data);
                                list.push(data);
                            }
                            return {
                                method: "SetTransformProperties",
                                list: list
                            };
                        }
                    }
                    scene.BuildMessage = BuildMessage;
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
                    class ObjectScene extends Phaser.Scene {
                        constructor() {
                            super("ObjectScene");
                        }
                        init(initData) {
                            this._initData = initData;
                        }
                        preload() {
                            consoleLog("preload()");
                            this.load.setBaseURL(this._initData.projectUrl);
                            this.load.pack("pack", this._initData.pack);
                        }
                        create() {
                            const editor = scene_3.Editor.getInstance();
                            this._dragCameraManager = new DragCameraManager(this);
                            this._dragObjectsManager = new DragObjectsManager();
                            this._pickManager = new PickObjectManager();
                            new DropManager();
                            this.initCamera();
                            this.initSelectionScene();
                            editor.getCreate().createWorld(this, this._initData.displayList);
                            editor.sceneCreated();
                            this.sendRecordCameraStateMessage();
                            editor.stop();
                        }
                        updateBackground() {
                            const rgb = "rgb(" + scene_3.ScenePropertiesComponent.get_backgroundColor(scene_3.Editor.getInstance().sceneProperties) + ")";
                            this.cameras.main.setBackgroundColor(rgb);
                        }
                        getPickManager() {
                            return this._pickManager;
                        }
                        getDragCameraManager() {
                            return this._dragCameraManager;
                        }
                        getDragObjectsManager() {
                            return this._dragObjectsManager;
                        }
                        removeAllObjects() {
                            let list = this.sys.displayList.list;
                            for (let obj of list) {
                                obj.destroy();
                            }
                            this.sys.displayList.removeAll(false);
                        }
                        getScenePoint(pointerX, pointerY) {
                            const cam = this.cameras.main;
                            const sceneX = pointerX / cam.zoom + cam.scrollX;
                            const sceneY = pointerY / cam.zoom + cam.scrollY;
                            return new Phaser.Math.Vector2(sceneX, sceneY);
                        }
                        initSelectionScene() {
                            this.scene.launch("ToolScene");
                            this._toolScene = this.scene.get("ToolScene");
                        }
                        initCamera() {
                            var cam = this.cameras.main;
                            cam.setOrigin(0, 0);
                            cam.setRoundPixels(true);
                            this.updateBackground();
                            this.scale.resize(window.innerWidth, window.innerHeight);
                        }
                        ;
                        getToolScene() {
                            return this._toolScene;
                        }
                        onMouseWheel(e) {
                            var cam = this.cameras.main;
                            var delta = e.deltaY;
                            var zoom = (delta > 0 ? 0.9 : 1.1);
                            const pointer = this.input.activePointer;
                            const point1 = cam.getWorldPoint(pointer.x, pointer.y);
                            cam.zoom *= zoom;
                            // update the camera matrix
                            cam.preRender(this.scale.resolution);
                            const point2 = cam.getWorldPoint(pointer.x, pointer.y);
                            const dx = point2.x - point1.x;
                            const dy = point2.y - point1.y;
                            cam.scrollX += -dx;
                            cam.scrollY += -dy;
                            this.sendRecordCameraStateMessage();
                        }
                        sendRecordCameraStateMessage() {
                            let cam = this.cameras.main;
                            scene_3.Editor.getInstance().sendMessage({
                                method: "RecordCameraState",
                                cameraState: {
                                    scrollX: cam.scrollX,
                                    scrollY: cam.scrollY,
                                    width: cam.width,
                                    height: cam.height,
                                    zoom: cam.zoom
                                }
                            });
                        }
                        performResize() {
                            this.cameras.main.setSize(window.innerWidth, window.innerHeight);
                        }
                    }
                    scene_3.ObjectScene = ObjectScene;
                    class PickObjectManager {
                        constructor() {
                            this._temp = [
                                new Phaser.Math.Vector2(0, 0),
                                new Phaser.Math.Vector2(0, 0),
                                new Phaser.Math.Vector2(0, 0),
                                new Phaser.Math.Vector2(0, 0)
                            ];
                        }
                        onMouseDown(e) {
                            this._down = e;
                        }
                        onMouseUp(e) {
                            if (!this._down || this._down.x !== e.x || this._down.y !== e.y || !scene_3.isLeftButton(this._down)) {
                                return null;
                            }
                            const editor = scene_3.Editor.getInstance();
                            const scene = editor.getObjectScene();
                            const pointer = scene.input.activePointer;
                            const result = editor.hitTestPointer(scene, pointer);
                            consoleLog(result);
                            let gameObj = result.pop();
                            editor.sendMessage({
                                method: "ClickObject",
                                ctrl: e.ctrlKey,
                                shift: e.shiftKey,
                                id: gameObj ? gameObj.name : undefined
                            });
                            return gameObj;
                        }
                        selectArea(start, end) {
                            console.log("---");
                            const editor = scene_3.Editor.getInstance();
                            const scene = editor.getObjectScene();
                            const list = scene.children.getAll();
                            let x = start.x;
                            let y = start.y;
                            let width = end.x - start.x;
                            let height = end.y - start.y;
                            if (width < 0) {
                                x = end.x;
                                width = -width;
                            }
                            if (height < 0) {
                                y = end.y;
                                height = -height;
                            }
                            const area = new Phaser.Geom.Rectangle(x, y, width, height);
                            const selection = [];
                            for (let obj of list) {
                                if (obj.name) {
                                    const sprite = obj;
                                    const points = this._temp;
                                    editor.getWorldBounds(sprite, points);
                                    if (area.contains(points[0].x, points[0].y)
                                        && area.contains(points[1].x, points[1].y)
                                        && area.contains(points[2].x, points[2].y)
                                        && area.contains(points[3].x, points[3].y)) {
                                        selection.push(sprite.name);
                                    }
                                }
                            }
                            editor.sendMessage({
                                method: "SetSelection",
                                list: selection
                            });
                        }
                    }
                    class DragObjectsManager {
                        constructor() {
                            this._startPoint = null;
                            this._dragging = false;
                            this._now = 0;
                            this._paintDelayUtil = new scene_3.PaintDelayUtil();
                        }
                        getScene() {
                            return scene_3.Editor.getInstance().getObjectScene();
                        }
                        getSelectedObjects() {
                            return scene_3.Editor.getInstance().getToolScene().getSelectedObjects();
                        }
                        getPointer() {
                            return this.getScene().input.activePointer;
                        }
                        onMouseDown(e) {
                            if (!scene_3.isLeftButton(e)) {
                                return false;
                            }
                            const set1 = new Phaser.Structs.Set(scene_3.Editor.getInstance().hitTestPointer(this.getScene(), this.getPointer()));
                            const set2 = new Phaser.Structs.Set(this.getSelectedObjects());
                            const hit = set1.intersect(set2).size > 0;
                            if (!hit) {
                                return false;
                            }
                            this._paintDelayUtil.startPaintLoop();
                            this._startPoint = this.getScene().getScenePoint(this.getPointer().x, this.getPointer().y);
                            const tx = new Phaser.GameObjects.Components.TransformMatrix();
                            const p = new Phaser.Math.Vector2();
                            for (let obj of this.getSelectedObjects()) {
                                const sprite = obj;
                                sprite.getWorldTransformMatrix(tx);
                                tx.transformPoint(0, 0, p);
                                sprite.setData("DragObjectsManager", {
                                    initX: p.x,
                                    initY: p.y
                                });
                            }
                            return true;
                        }
                        onMouseMove(e) {
                            if (!scene_3.isLeftButton(e) || this._startPoint === null) {
                                return;
                            }
                            this._dragging = true;
                            const pos = this.getScene().getScenePoint(this.getPointer().x, this.getPointer().y);
                            const dx = pos.x - this._startPoint.x;
                            const dy = pos.y - this._startPoint.y;
                            for (let obj of this.getSelectedObjects()) {
                                const sprite = obj;
                                const data = sprite.getData("DragObjectsManager");
                                if (!data) {
                                    continue;
                                }
                                const x = scene_3.Editor.getInstance().snapValueX(data.initX + dx);
                                const y = scene_3.Editor.getInstance().snapValueX(data.initY + dy);
                                if (sprite.parentContainer) {
                                    const tx = sprite.parentContainer.getWorldTransformMatrix();
                                    const p = new Phaser.Math.Vector2();
                                    tx.applyInverse(x, y, p);
                                    sprite.setPosition(p.x, p.y);
                                }
                                else {
                                    sprite.setPosition(x, y);
                                }
                            }
                            if (this._paintDelayUtil.shouldPaintThisTime()) {
                                scene_3.Editor.getInstance().repaint();
                            }
                        }
                        onMouseUp() {
                            if (this._startPoint !== null && this._dragging) {
                                this._dragging = false;
                                this._startPoint = null;
                                scene_3.Editor.getInstance().sendMessage(scene_3.BuildMessage.SetTransformProperties(this.getSelectedObjects()));
                            }
                            for (let obj of this.getSelectedObjects()) {
                                const sprite = obj;
                                if (sprite.data) {
                                    sprite.data.remove("DragObjectsManager");
                                }
                            }
                            scene_3.Editor.getInstance().repaint();
                        }
                    }
                    class DragCameraManager {
                        constructor(scene) {
                            this._scene = scene;
                            this._dragStartPoint = null;
                        }
                        onMouseDown(e) {
                            // if middle button peressed
                            if (scene_3.isMiddleButton(e)) {
                                this._dragStartPoint = new Phaser.Math.Vector2(e.clientX, e.clientY);
                                const cam = this._scene.cameras.main;
                                this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);
                                e.preventDefault();
                            }
                        }
                        onMouseMove(e) {
                            if (this._dragStartPoint === null) {
                                return;
                            }
                            const dx = this._dragStartPoint.x - e.clientX;
                            const dy = this._dragStartPoint.y - e.clientY;
                            const cam = this._scene.cameras.main;
                            cam.scrollX = this._dragStartCameraScroll.x + dx / cam.zoom;
                            cam.scrollY = this._dragStartCameraScroll.y + dy / cam.zoom;
                            scene_3.Editor.getInstance().repaint();
                            e.preventDefault();
                        }
                        onMouseUp() {
                            if (this._dragStartPoint !== null) {
                                this._scene.sendRecordCameraStateMessage();
                            }
                            this._dragStartPoint = null;
                            this._dragStartCameraScroll = null;
                        }
                    }
                    class DropManager {
                        constructor() {
                            window.addEventListener("drop", function (e) {
                                let editor = scene_3.Editor.getInstance();
                                let point = editor.getObjectScene().cameras.main.getWorldPoint(e.clientX, e.clientY);
                                editor.sendMessage({
                                    method: "DropEvent",
                                    x: point.x,
                                    y: point.y
                                });
                            });
                            window.addEventListener("dragover", function (e) {
                                e.preventDefault();
                            });
                        }
                    }
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
                    var PAINT_COUNT = 0;
                    class ToolScene extends Phaser.Scene {
                        constructor() {
                            super("ToolScene");
                            this._axisToken = null;
                            this._axisLabels = [];
                            this._selectionBoxPoints = [
                                new Phaser.Math.Vector2(0, 0),
                                new Phaser.Math.Vector2(0, 0),
                                new Phaser.Math.Vector2(0, 0),
                                new Phaser.Math.Vector2(0, 0)
                            ];
                            this._selectionDragStart = null;
                            this._selectionDragEnd = null;
                            this._selectedObjects = [];
                            this._selectionGraphics = null;
                            this._tools = [];
                            this._paintDelayUtils = new scene.PaintDelayUtil();
                        }
                        create() {
                            this.initCamera();
                            this._axisToken = "";
                            this._gridGraphics = this.add.graphics();
                            this._gridGraphics.depth = -1;
                            this._selectionGraphics = this.add.graphics({
                                fillStyle: {
                                    color: 0x00ff00
                                },
                                lineStyle: {
                                    color: 0x00ff00,
                                    width: 2
                                }
                            });
                            this._selectionGraphics.depth = -1;
                            this._paintCallsLabel = this.add.text(10, 10, "", { "color": "blue", "backgroundColor": "red" });
                            this._paintCallsLabel.depth = 1000;
                        }
                        initCamera() {
                            this.cameras.main.setRoundPixels(true);
                            this.cameras.main.setOrigin(0, 0);
                        }
                        updateFromSceneProperties() {
                            this._axisToken = "";
                            this.renderAxis();
                        }
                        renderAxis() {
                            const editor = scene.Editor.getInstance();
                            const cam = editor.getObjectScene().cameras.main;
                            const w = window.innerWidth;
                            const h = window.innerHeight;
                            let dx = 16;
                            let dy = 16;
                            if (scene.ScenePropertiesComponent.get_snapEnabled(editor.sceneProperties)) {
                                dx = scene.ScenePropertiesComponent.get_snapWidth(editor.sceneProperties);
                                dy = scene.ScenePropertiesComponent.get_snapHeight(editor.sceneProperties);
                            }
                            let i = 1;
                            while (dx * i * cam.zoom < 32) {
                                i++;
                            }
                            dx = dx * i;
                            i = 1;
                            while (dy * i * cam.zoom < 32) {
                                i++;
                            }
                            dy = dy * i;
                            const sx = ((cam.scrollX / dx) | 0) * dx;
                            const sy = ((cam.scrollY / dy) | 0) * dy;
                            const bx = scene.ScenePropertiesComponent.get_borderX(editor.sceneProperties);
                            const by = scene.ScenePropertiesComponent.get_borderY(editor.sceneProperties);
                            const bw = scene.ScenePropertiesComponent.get_borderWidth(editor.sceneProperties);
                            const bh = scene.ScenePropertiesComponent.get_borderHeight(editor.sceneProperties);
                            const token = w + "-" + h + "-" + dx + "-" + dy + "-" + cam.zoom + "-" + cam.scrollX + "-" + cam.scrollY
                                + "-" + bx + "-" + by + "-" + bw + "-" + bh;
                            if (this._axisToken !== null && this._axisToken === token) {
                                return;
                            }
                            this._axisToken = token;
                            this._gridGraphics.clear();
                            const fg = Phaser.Display.Color.RGBStringToColor("rgb(" + scene.ScenePropertiesComponent.get_foregroundColor(editor.sceneProperties) + ")");
                            this._gridGraphics.lineStyle(1, fg.color, 0.5);
                            for (const label of this._axisLabels) {
                                label.destroy();
                            }
                            // labels
                            let label = null;
                            let labelHeight = 0;
                            this._axisLabels = [];
                            for (let x = sx;; x += dx) {
                                const x2 = (x - cam.scrollX) * cam.zoom;
                                if (x2 > w) {
                                    break;
                                }
                                if (label != null) {
                                    if (label.x + label.width * 2 > x2) {
                                        continue;
                                    }
                                }
                                label = this.add.text(x2, 0, x.toString());
                                label.style.setShadow(1, 1);
                                this._axisLabels.push(label);
                                labelHeight = label.height;
                                label.setOrigin(0.5, 0);
                            }
                            let labelWidth = 0;
                            for (let y = sy;; y += dy) {
                                const y2 = (y - cam.scrollY) * cam.zoom;
                                if (y2 > h) {
                                    break;
                                }
                                if (y2 < labelHeight) {
                                    continue;
                                }
                                const label = this.add.text(0, y2, (y).toString());
                                label.style.setShadow(1, 1);
                                label.setOrigin(0, 0.5);
                                this._axisLabels.push(label);
                                labelWidth = Math.max(label.width, labelWidth);
                            }
                            // lines
                            for (let x = sx;; x += dx) {
                                const x2 = (x - cam.scrollX) * cam.zoom;
                                if (x2 > w) {
                                    break;
                                }
                                if (x2 < labelWidth) {
                                    continue;
                                }
                                this._gridGraphics.lineBetween(x2, labelHeight, x2, h);
                            }
                            for (let y = sy;; y += dy) {
                                const y2 = (y - cam.scrollY) * cam.zoom;
                                if (y2 > h) {
                                    break;
                                }
                                if (y2 < labelHeight) {
                                    continue;
                                }
                                this._gridGraphics.lineBetween(labelWidth, y2, w, y2);
                            }
                            // border
                            this._gridGraphics.lineStyle(4, 0x000000, 1);
                            this._gridGraphics.strokeRect((bx - cam.scrollX) * cam.zoom, (by - cam.scrollY) * cam.zoom, bw * cam.zoom, bh * cam.zoom);
                            this._gridGraphics.lineStyle(2, 0xffffff, 1);
                            this._gridGraphics.strokeRect(((bx - cam.scrollX) * cam.zoom), (by - cam.scrollY) * cam.zoom, bw * cam.zoom, bh * cam.zoom);
                        }
                        getSelectedObjects() {
                            return this._selectedObjects;
                        }
                        updateSelectionObjects() {
                            const editor = scene.Editor.getInstance();
                            this._selectedObjects = [];
                            let objectScene = scene.Editor.getInstance().getObjectScene();
                            for (let id of editor.selection) {
                                const obj = objectScene.sys.displayList.getByName(id);
                                if (obj) {
                                    this._selectedObjects.push(obj);
                                }
                            }
                        }
                        update() {
                            this.renderAxis();
                            this.renderSelection();
                            this.updateTools();
                            this._paintCallsLabel.visible = scene.Editor.getInstance().sceneProperties.debugPaintCalls;
                            if (this._paintCallsLabel.visible) {
                                this._paintCallsLabel.text = PAINT_COUNT.toString();
                                PAINT_COUNT += 1;
                            }
                        }
                        setTools(tools) {
                            for (let tool of this._tools) {
                                tool.clear();
                            }
                            for (let tool of tools) {
                                tool.activated();
                            }
                            this._tools = tools;
                        }
                        updateTools() {
                            for (let tool of this._tools) {
                                tool.update();
                            }
                        }
                        renderSelection() {
                            this._selectionGraphics.clear();
                            const g2 = this._selectionGraphics;
                            for (let obj of this._selectedObjects) {
                                this.paintSelectionBox(g2, obj);
                            }
                            if (this._selectionDragStart && !this._selectionDragStart.equals(this._selectionDragEnd)) {
                                const x = this._selectionDragStart.x;
                                const y = this._selectionDragStart.y;
                                const width = this._selectionDragEnd.x - x;
                                const height = this._selectionDragEnd.y - y;
                                const g2 = this._selectionGraphics;
                                g2.lineStyle(4, 0x000000);
                                g2.strokeRect(x, y, width, height);
                                g2.lineStyle(2, 0x00ff00);
                                g2.strokeRect(x, y, width, height);
                            }
                        }
                        paintSelectionBox(graphics, sprite) {
                            scene.Editor.getInstance().getWorldBounds(sprite, this._selectionBoxPoints);
                            graphics.lineStyle(4, 0x000000);
                            graphics.strokePoints(this._selectionBoxPoints, true);
                            graphics.lineStyle(2, 0x00ff00);
                            graphics.strokePoints(this._selectionBoxPoints, true);
                        }
                        containsPointer() {
                            for (let tool of this._tools) {
                                const b = tool.containsPointer();
                                if (b) {
                                    return true;
                                }
                            }
                            return false;
                        }
                        isEditing() {
                            for (let tool of this._tools) {
                                if (tool.isEditing()) {
                                    return true;
                                }
                            }
                            return false;
                        }
                        testRepaint() {
                            for (let tool of this._tools) {
                                if (tool.requestRepaint) {
                                    tool.requestRepaint = false;
                                    scene.Editor.getInstance().repaint();
                                    return;
                                }
                            }
                        }
                        onToolsMouseDown() {
                            this._paintDelayUtils.startPaintLoop();
                            for (let tool of this._tools) {
                                tool.onMouseDown();
                            }
                            this.testRepaint();
                        }
                        onToolsMouseMove() {
                            for (let tool of this._tools) {
                                tool.onMouseMove();
                            }
                            if (this._paintDelayUtils.shouldPaintThisTime()) {
                                this.testRepaint();
                            }
                        }
                        onToolsMouseUp() {
                            for (let tool of this._tools) {
                                tool.onMouseUp();
                            }
                            this.testRepaint();
                        }
                        onSelectionDragMouseDown(e) {
                            if (!scene.isLeftButton(e)) {
                                return;
                            }
                            this._paintDelayUtils.startPaintLoop();
                            const pointer = this.input.activePointer;
                            this._selectionDragStart = new Phaser.Math.Vector2(pointer.x, pointer.y);
                            this._selectionDragEnd = this._selectionDragStart.clone();
                        }
                        onSelectionDragMouseMove(e) {
                            if (this._selectionDragStart) {
                                const pointer = this.input.activePointer;
                                this._selectionDragEnd.set(pointer.x, pointer.y);
                                return this._paintDelayUtils.shouldPaintThisTime();
                            }
                            return false;
                        }
                        selectionDragClear() {
                            this._selectionDragStart = null;
                            this._selectionDragEnd = null;
                        }
                        onSelectionDragMouseUp(e) {
                            if (this._selectionDragStart) {
                                scene.Editor.getInstance().getObjectScene().getPickManager().selectArea(this._selectionDragStart, this._selectionDragEnd);
                                this.selectionDragClear();
                            }
                        }
                    }
                    scene.ToolScene = ToolScene;
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
                    function main() {
                        new scene.Editor();
                    }
                    window.addEventListener("keydown", function (e) {
                        scene.Editor.getInstance().sendKeyDown(e);
                        e.preventDefault();
                        e.stopImmediatePropagation();
                    });
                    window.addEventListener("keyup", function (e) {
                        // I don't know why the ESC key is not captured in the keydown.
                        if (e.keyCode === 27) {
                            scene.Editor.getInstance().sendKeyDown(e);
                        }
                        e.preventDefault();
                        e.stopImmediatePropagation();
                    });
                    window.addEventListener("load", main);
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
                    let ws;
                    let game;
                    let MODEL_LIST = [];
                    let CURRENT_MODEL;
                    function mainScreenshot() {
                        connect();
                    }
                    scene.mainScreenshot = mainScreenshot;
                    function connect() {
                        ws = new WebSocket(getWebSocketUrl());
                        ws.addEventListener("message", onMessage);
                        ws.addEventListener("close", onClose);
                    }
                    function onClose() {
                        connect();
                    }
                    function createGame() {
                        game = new Phaser.Game({
                            width: CURRENT_MODEL,
                            height: 380,
                            render: {
                                pixelArt: true
                            },
                            backgroundColor: "#f0f0f0",
                            audio: {
                                noAudio: true
                            },
                            scale: {
                                mode: Phaser.Scale.NONE
                            }
                        });
                        game.scene.add("Preload", Preload);
                        game.scene.add("Level", Level);
                    }
                    function getWebSocketUrl() {
                        var loc = document.location;
                        return "ws://" + loc.host + "/ws/api?channel=sceneScreenshot";
                    }
                    function onMessage(event) {
                        const data = JSON.parse(event.data);
                        consoleLog("message: " + data.method);
                        consoleLog(data);
                        if (data.method === "CreateScreenshot") {
                            MODEL_LIST.push(data);
                            if (MODEL_LIST.length === 1) {
                                nextModel();
                            }
                        }
                    }
                    function nextModel() {
                        //window.location.reload();
                        if (game) {
                            game.destroy(false);
                        }
                        if (MODEL_LIST.length > 0) {
                            CURRENT_MODEL = MODEL_LIST.pop();
                            consoleLog("Start processing new model at project " + CURRENT_MODEL.projectUrl);
                            createGame();
                            game.scene.start("Preload");
                        }
                    }
                    class Preload extends Phaser.Scene {
                        constructor() {
                            super("Preload");
                        }
                        preload() {
                            this.load.setBaseURL(CURRENT_MODEL.projectUrl);
                            this.load.pack("pack", CURRENT_MODEL.pack);
                        }
                        create() {
                            this.scene.start("Level");
                        }
                    }
                    class Level extends Phaser.Scene {
                        constructor() {
                            super("Level");
                        }
                        create() {
                            const sceneInfo = CURRENT_MODEL.scenes.pop();
                            var x = scene.ScenePropertiesComponent.get_borderX(sceneInfo.model);
                            var y = scene.ScenePropertiesComponent.get_borderY(sceneInfo.model);
                            var width = scene.ScenePropertiesComponent.get_borderWidth(sceneInfo.model);
                            var height = scene.ScenePropertiesComponent.get_borderHeight(sceneInfo.model);
                            this.cameras.main.setSize(width, height);
                            this.cameras.main.setScroll(x, y);
                            this.scale.resize(width, height);
                            var create = new scene.Create(false);
                            create.createWorld(this, sceneInfo.model.displayList);
                            this.game.renderer.snapshot(function (image) {
                                var imageData = image.src;
                                const _GetDataURL = window.GetDataURL;
                                if (_GetDataURL) {
                                    _GetDataURL(imageData);
                                }
                                else {
                                    var file = sceneInfo.file;
                                    consoleLog("Sending screenshot data of " + file);
                                    var loc = document.location;
                                    var url = "http://" + loc.host + "/sceneScreenshotService/sceneInfo?" + file;
                                    var req = new XMLHttpRequest();
                                    req.open("POST", url);
                                    req.setRequestHeader("Content-Type", "application/upload");
                                    req.send(JSON.stringify({
                                        file: file,
                                        imageData: imageData
                                    }));
                                }
                                if (CURRENT_MODEL.scenes.length === 0) {
                                    nextModel();
                                }
                                else {
                                    game.scene.start("Level");
                                }
                            });
                        }
                    }
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
window.addEventListener("load", phasereditor2d.ui.ide.editors.scene.mainScreenshot);
window.addEventListener("onerror", function (e) {
    alert("WebView ERROR: " + e);
});
// needed to fix errors in MacOS SWT Browser.
window.AudioContext = function () { };
// disable log on production
var CONSOLE_LOG = false;
function consoleLog(msg) {
    if (CONSOLE_LOG) {
        console.log(msg);
    }
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
                    function isLeftButton(e) {
                        if (e.buttons === undefined) {
                            // macos swt browser
                            return e.button === 0;
                        }
                        return e.buttons === 1;
                    }
                    scene.isLeftButton = isLeftButton;
                    function isMiddleButton(e) {
                        if (e.buttons === undefined) {
                            // macos swt browser
                            return e.button === 1;
                        }
                        return e.buttons === 4;
                    }
                    scene.isMiddleButton = isMiddleButton;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
// missing types in Phaser definitions
class ActiveXObject {
}
var Phaser;
(function (Phaser) {
    var Types;
    (function (Types) {
        var Tweens;
        (function (Tweens) {
            class StaggerBuilderConfig {
            }
            Tweens.StaggerBuilderConfig = StaggerBuilderConfig;
        })(Tweens = Types.Tweens || (Types.Tweens = {}));
    })(Types = Phaser.Types || (Phaser.Types = {}));
})(Phaser || (Phaser = {}));
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
                    scene.ARROW_LENGTH = 80;
                    class InteractiveTool {
                        constructor() {
                            this.toolScene = scene.Editor.getInstance().getToolScene();
                            this.objScene = scene.Editor.getInstance().getObjectScene();
                            this.requestRepaint = false;
                        }
                        getObjects() {
                            const sel = this.toolScene.getSelectedObjects();
                            return sel.filter(obj => this.canEdit(obj));
                        }
                        containsPointer() {
                            return false;
                        }
                        isEditing() {
                            return false;
                        }
                        clear() {
                        }
                        activated() {
                        }
                        update() {
                            const list = this.getObjects();
                            if (list.length === 0) {
                                this.clear();
                            }
                            else {
                                this.render(list);
                            }
                        }
                        render(objects) {
                        }
                        onMouseDown() {
                        }
                        onMouseUp() {
                        }
                        onMouseMove() {
                        }
                        getToolPointer() {
                            return this.toolScene.input.activePointer;
                        }
                        getScenePoint(toolX, toolY) {
                            return this.objScene.getScenePoint(toolX, toolY);
                        }
                        objectGlobalAngle(obj) {
                            let a = obj.angle;
                            const parent = obj.parentContainer;
                            if (parent) {
                                a += this.objectGlobalAngle(parent);
                            }
                            return a;
                        }
                        objectGlobalScale(obj) {
                            let scaleX = obj.scaleX;
                            let scaleY = obj.scaleY;
                            const parent = obj.parentContainer;
                            if (parent) {
                                const parentScale = this.objectGlobalScale(parent);
                                scaleX *= parentScale.x;
                                scaleY *= parentScale.y;
                            }
                            return new Phaser.Math.Vector2(scaleX, scaleY);
                        }
                        angle2(a, b) {
                            return this.angle(a.x, a.y, b.x, b.y);
                        }
                        angle(x1, y1, x2, y2) {
                            const delta = (x1 * x2 + y1 * y2) / Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));
                            if (delta > 1.0) {
                                return 0;
                            }
                            if (delta < -1.0) {
                                return 180;
                            }
                            return Phaser.Math.RadToDeg(Math.acos(delta));
                        }
                        snapValueX(x) {
                            return scene.Editor.getInstance().snapValueX(x);
                        }
                        snapValueY(y) {
                            return scene.Editor.getInstance().snapValueY(y);
                        }
                        createArrowShape() {
                            const s = this.toolScene.add.triangle(0, 0, 0, 0, 12, 0, 6, 12);
                            s.setStrokeStyle(1, 0, 0.8);
                            return s;
                        }
                        createRectangleShape() {
                            const s = this.toolScene.add.rectangle(0, 0, 12, 12);
                            s.setStrokeStyle(1, 0, 0.8);
                            return s;
                        }
                        createCircleShape() {
                            const s = this.toolScene.add.circle(0, 0, 6);
                            s.setStrokeStyle(1, 0, 0.8);
                            return s;
                        }
                        createEllipseShape() {
                            const s = this.toolScene.add.ellipse(0, 0, 10, 10);
                            s.setStrokeStyle(1, 0, 0.8);
                            return s;
                        }
                        createLineShape() {
                            const s = this.toolScene.add.line();
                            return s;
                        }
                        localToParent(sprite, point) {
                            const result = new Phaser.Math.Vector2();
                            const tx = new Phaser.GameObjects.Components.TransformMatrix();
                            sprite.getWorldTransformMatrix(tx);
                            tx.transformPoint(point.x, point.y, result);
                            if (sprite.parentContainer) {
                                sprite.parentContainer.getWorldTransformMatrix(tx);
                                tx.applyInverse(result.x, result.y, result);
                            }
                            return result;
                        }
                    }
                    scene.InteractiveTool = InteractiveTool;
                    class SimpleLineTool extends InteractiveTool {
                        constructor(tool1, tool2, color) {
                            super();
                            this._color = color;
                            this._tool1 = tool1;
                            this._tool2 = tool2;
                            this._line = this.createLineShape();
                            this._line.setStrokeStyle(4, 0);
                            this._line.setOrigin(0, 0);
                            this._line.depth = -1;
                            this._line2 = this.createLineShape();
                            this._line2.setStrokeStyle(2, color);
                            this._line2.setOrigin(0, 0);
                            this._line2.depth = -1;
                        }
                        canEdit(obj) {
                            return this._tool1.canEdit(obj) && this._tool2.canEdit(obj);
                        }
                        render(objects) {
                            this._line.setTo(this._tool1.getX(), this._tool1.getY(), this._tool2.getX(), this._tool2.getY());
                            this._line2.setTo(this._tool1.getX(), this._tool1.getY(), this._tool2.getX(), this._tool2.getY());
                            this._line.visible = true;
                            this._line2.visible = true;
                        }
                        clear() {
                            this._line.visible = false;
                            this._line2.visible = false;
                        }
                        onMouseDown() {
                            if (this._tool2.isEditing()) {
                                this._line2.strokeColor = 0xffffff;
                            }
                        }
                        onMouseUp() {
                            this._line2.strokeColor = this._color;
                        }
                    }
                    scene.SimpleLineTool = SimpleLineTool;
                    class ToolFactory {
                        static createByName(name) {
                            switch (name) {
                                case "TileSize": {
                                    return [
                                        new scene.TileSizeTool(true, false),
                                        new scene.TileSizeTool(false, true),
                                        new scene.TileSizeTool(true, true)
                                    ];
                                }
                                case "TilePosition": {
                                    const toolX = new scene.TilePositionTool(true, false);
                                    const toolY = new scene.TilePositionTool(false, true);
                                    const toolXY = new scene.TilePositionTool(true, true);
                                    return [
                                        toolX,
                                        toolY,
                                        toolXY,
                                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                                    ];
                                }
                                case "TileScale": {
                                    const toolX = new scene.TileScaleTool(true, false);
                                    const toolY = new scene.TileScaleTool(false, true);
                                    const toolXY = new scene.TileScaleTool(true, true);
                                    return [
                                        toolX,
                                        toolY,
                                        toolXY,
                                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                                    ];
                                }
                                case "Origin": {
                                    const toolX = new scene.OriginTool(true, false);
                                    const toolY = new scene.OriginTool(false, true);
                                    const toolXY = new scene.OriginTool(true, true);
                                    return [
                                        toolX,
                                        toolY,
                                        toolXY,
                                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                                    ];
                                }
                                case "Angle": {
                                    const tool = new scene.AngleTool();
                                    return [
                                        tool,
                                        new scene.AngleLineTool(tool, true),
                                        new scene.AngleLineTool(tool, false)
                                    ];
                                }
                                case "Scale": {
                                    return [
                                        new scene.ScaleTool(true, false),
                                        new scene.ScaleTool(false, true),
                                        new scene.ScaleTool(true, true)
                                    ];
                                }
                                case "Position": {
                                    const toolX = new scene.PositionTool(true, false);
                                    const toolY = new scene.PositionTool(false, true);
                                    const toolXY = new scene.PositionTool(true, true);
                                    return [
                                        toolX,
                                        toolY,
                                        toolXY,
                                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                                    ];
                                }
                                case "Hand": {
                                    return [new scene.HandTool()];
                                }
                            }
                            return [];
                        }
                    }
                    scene.ToolFactory = ToolFactory;
                })(scene = editors.scene || (editors.scene = {}));
            })(editors = ide.editors || (ide.editors = {}));
        })(ide = ui.ide || (ui.ide = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./Tools.ts" />
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
                    class AngleLineTool extends scene.InteractiveTool {
                        constructor(angleTool, start) {
                            super();
                            this._angleTool = angleTool;
                            this._start = start;
                            this._color = 0xaaaaff;
                            this._shapeBorder = this.toolScene.add.rectangle(0, 0, scene.AngleTool.RADIUS, 4);
                            this._shapeBorder.setFillStyle(0);
                            this._shapeBorder.setOrigin(0, 0.5);
                            this._shapeBorder.depth = -1;
                            this._shape = this.createLineShape();
                            this._shape.setStrokeStyle(2, this._color);
                            this._shape.setOrigin(0, 0);
                            this._shape.setTo(0, 0, scene.AngleTool.RADIUS, 0);
                            this._shape.depth = -1;
                        }
                        clear() {
                            this._shape.visible = false;
                            this._shapeBorder.visible = false;
                        }
                        containsPointer() {
                            return false;
                        }
                        canEdit(obj) {
                            return this._angleTool.canEdit(obj);
                        }
                        isEditing() {
                            return this._angleTool.isEditing();
                        }
                        render(objects) {
                            const cam = scene.Editor.getInstance().getObjectScene().cameras.main;
                            const pos = new Phaser.Math.Vector2(0, 0);
                            let globalStartAngle = 0;
                            let globalEndAngle = 0;
                            const localCoords = scene.Editor.getInstance().isTransformLocalCoords();
                            for (let sprite of objects) {
                                const worldXY = new Phaser.Math.Vector2();
                                const worldTx = sprite.getWorldTransformMatrix();
                                worldTx.transformPoint(0, 0, worldXY);
                                pos.add(worldXY);
                                const endAngle = this.objectGlobalAngle(sprite);
                                const startAngle = localCoords ? endAngle - sprite.angle : 0;
                                globalStartAngle += startAngle;
                                globalEndAngle += endAngle;
                            }
                            const len = this.getObjects().length;
                            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
                            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
                            globalStartAngle /= len;
                            globalEndAngle /= len;
                            this._shape.setPosition(cameraX, cameraY);
                            this._shape.angle = this._start ? globalStartAngle : globalEndAngle;
                            this._shape.visible = true;
                            this._shapeBorder.setPosition(cameraX, cameraY);
                            this._shapeBorder.angle = this._shape.angle;
                            this._shapeBorder.visible = this._shape.visible;
                        }
                        onMouseDown() {
                            this._shape.strokeColor = 0xffffff;
                        }
                        onMouseUp() {
                            this._shape.strokeColor = this._color;
                        }
                    }
                    scene.AngleLineTool = AngleLineTool;
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
                    class AngleTool extends scene.InteractiveTool {
                        constructor() {
                            super();
                            this._dragging = false;
                            this._color = 0xaaaaff;
                            this._handlerShapeBorder = this.createEllipseShape();
                            this._handlerShapeBorder.setFillStyle(0, 0);
                            this._handlerShapeBorder.setStrokeStyle(4, 0);
                            this._handlerShapeBorder.setSize(AngleTool.RADIUS * 2, AngleTool.RADIUS * 2);
                            this._handlerShape = this.createEllipseShape();
                            this._handlerShape.setFillStyle(0, 0);
                            this._handlerShape.setStrokeStyle(2, this._color);
                            this._handlerShape.setSize(AngleTool.RADIUS * 2, AngleTool.RADIUS * 2);
                            this._centerShape = this.createCircleShape();
                            this._centerShape.setFillStyle(this._color);
                        }
                        canEdit(obj) {
                            return obj.angle !== undefined;
                        }
                        render(objects) {
                            const cam = scene.Editor.getInstance().getObjectScene().cameras.main;
                            const pos = new Phaser.Math.Vector2(0, 0);
                            for (let sprite of objects) {
                                const worldXY = new Phaser.Math.Vector2();
                                const worldTx = sprite.getWorldTransformMatrix();
                                worldTx.transformPoint(0, 0, worldXY);
                                pos.add(worldXY);
                            }
                            const len = this.getObjects().length;
                            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
                            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
                            this._handlerShape.setPosition(cameraX, cameraY);
                            this._handlerShape.visible = true;
                            this._handlerShapeBorder.setPosition(cameraX, cameraY);
                            this._handlerShapeBorder.visible = true;
                            this._centerShape.setPosition(cameraX, cameraY);
                            this._centerShape.visible = true;
                        }
                        containsPointer() {
                            const pointer = this.getToolPointer();
                            const d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);
                            return Math.abs(d - AngleTool.RADIUS) <= 10;
                        }
                        clear() {
                            this._handlerShape.visible = false;
                            this._handlerShapeBorder.visible = false;
                            this._centerShape.visible = false;
                        }
                        isEditing() {
                            return this._dragging;
                        }
                        onMouseDown() {
                            if (this.containsPointer()) {
                                this._dragging = true;
                                this.requestRepaint = true;
                                this._handlerShape.strokeColor = 0xffffff;
                                this._centerShape.setFillStyle(0xffffff);
                                this._cursorStartX = this.getToolPointer().x;
                                this._cursorStartY = this.getToolPointer().y;
                                for (let obj of this.getObjects()) {
                                    const sprite = obj;
                                    obj.setData("AngleTool", {
                                        initAngle: sprite.angle
                                    });
                                }
                            }
                        }
                        onMouseMove() {
                            if (!this._dragging) {
                                return;
                            }
                            const pointer = this.getToolPointer();
                            const cursorX = pointer.x;
                            const cursorY = pointer.y;
                            const dx = this._cursorStartX - cursorX;
                            const dy = this._cursorStartY - cursorY;
                            if (Math.abs(dx) < 1 || Math.abs(dy) < 1) {
                                return;
                            }
                            this.requestRepaint = true;
                            for (let obj of this.getObjects()) {
                                const sprite = obj;
                                const data = obj.data.get("AngleTool");
                                const deltaRadians = angleBetweenTwoPointsWithFixedPoint(cursorX, cursorY, this._cursorStartX, this._cursorStartY, this._centerShape.x, this._centerShape.y);
                                const deltaAngle = Phaser.Math.RadToDeg(deltaRadians);
                                sprite.angle = data.initAngle + deltaAngle;
                            }
                        }
                        onMouseUp() {
                            if (this._dragging) {
                                const msg = scene.BuildMessage.SetTransformProperties(this.getObjects());
                                scene.Editor.getInstance().sendMessage(msg);
                            }
                            this._dragging = false;
                            this._handlerShape.strokeColor = this._color;
                            this._centerShape.setFillStyle(this._color);
                            this.requestRepaint = true;
                        }
                    }
                    AngleTool.RADIUS = 100;
                    scene.AngleTool = AngleTool;
                    function angleBetweenTwoPointsWithFixedPoint(point1X, point1Y, point2X, point2Y, fixedX, fixedY) {
                        const angle1 = Math.atan2(point1Y - fixedY, point1X - fixedX);
                        const angle2 = Math.atan2(point2Y - fixedY, point2X - fixedX);
                        return angle1 - angle2;
                    }
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
                    class HandTool extends scene.InteractiveTool {
                        constructor() {
                            super();
                        }
                        canEdit(obj) {
                            return true;
                        }
                        containsPointer() {
                            return true;
                        }
                        update() {
                        }
                        isEditing() {
                            return this._dragging;
                        }
                        onMouseDown() {
                            this._dragging = true;
                            const pointer = this.getToolPointer();
                            this._dragStartPoint = new Phaser.Math.Vector2(pointer.x, pointer.y);
                            const cam = this.objScene.cameras.main;
                            this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);
                        }
                        onMouseMove() {
                            if (this._dragging) {
                                //this.objScene.input.setDefaultCursor("grabbing");
                                this.objScene.input.setDefaultCursor("move");
                                const pointer = this.getToolPointer();
                                const dx = this._dragStartPoint.x - pointer.x;
                                const dy = this._dragStartPoint.y - pointer.y;
                                const cam = this.objScene.cameras.main;
                                cam.scrollX = this._dragStartCameraScroll.x + dx / cam.zoom;
                                cam.scrollY = this._dragStartCameraScroll.y + dy / cam.zoom;
                                scene.Editor.getInstance().repaint();
                            }
                        }
                        onMouseUp() {
                            if (this._dragging) {
                                this._dragging = false;
                                //this.objScene.input.setDefaultCursor("grab");
                                this.objScene.input.setDefaultCursor("move");
                                this.objScene.sendRecordCameraStateMessage();
                            }
                        }
                        activated() {
                            //this.objScene.input.setDefaultCursor("grab");
                            this.objScene.input.setDefaultCursor("move");
                        }
                        clear() {
                            this.objScene.input.setDefaultCursor("default");
                        }
                    }
                    scene.HandTool = HandTool;
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
                    class OriginTool extends scene.InteractiveTool {
                        constructor(changeX, changeY) {
                            super();
                            this._dragging = false;
                            this._changeX = changeX;
                            this._changeY = changeY;
                            if (changeX && changeY) {
                                this._color = 0xffff00;
                            }
                            else if (changeX) {
                                this._color = 0xff0000;
                            }
                            else {
                                this._color = 0x00ff00;
                            }
                            this._handlerShape = changeX && changeY ? this.createCircleShape() : this.createArrowShape();
                            this._handlerShape.setFillStyle(this._color);
                        }
                        canEdit(obj) {
                            return obj.hasOwnProperty("originX");
                        }
                        getX() {
                            return this._handlerShape.x;
                        }
                        getY() {
                            return this._handlerShape.y;
                        }
                        clear() {
                            this._handlerShape.visible = false;
                        }
                        render(list) {
                            const cam = scene.Editor.getInstance().getObjectScene().cameras.main;
                            const pos = new Phaser.Math.Vector2(0, 0);
                            let angle = 0;
                            for (let obj of list) {
                                const sprite = obj;
                                const worldXY = new Phaser.Math.Vector2();
                                const worldTx = sprite.getWorldTransformMatrix();
                                let localX = 0;
                                let localY = 0;
                                const scale = this.objectGlobalScale(sprite);
                                if (!this._changeX || !this._changeY) {
                                    if (this._changeX) {
                                        localX += scene.ARROW_LENGTH / scale.x / cam.zoom * (sprite.flipX ? -1 : 1);
                                        if (sprite.flipX) {
                                            angle += 180;
                                        }
                                    }
                                    else {
                                        localY += scene.ARROW_LENGTH / scale.y / cam.zoom * (sprite.flipY ? -1 : 1);
                                        if (sprite.flipY) {
                                            angle += 180;
                                        }
                                    }
                                }
                                angle += this.objectGlobalAngle(sprite);
                                worldTx.transformPoint(localX, localY, worldXY);
                                pos.add(worldXY);
                            }
                            const len = this.getObjects().length;
                            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
                            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
                            this._handlerShape.setPosition(cameraX, cameraY);
                            this._handlerShape.angle = angle / len;
                            if (this._changeX) {
                                this._handlerShape.angle -= 90;
                            }
                            this._handlerShape.visible = true;
                        }
                        containsPointer() {
                            const pointer = this.getToolPointer();
                            const d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);
                            return d <= this._handlerShape.width;
                        }
                        isEditing() {
                            return this._dragging;
                        }
                        onMouseDown() {
                            if (this.containsPointer()) {
                                this._dragging = true;
                                this.requestRepaint = true;
                                this._handlerShape.setFillStyle(0xffffff);
                                const shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);
                                for (let obj of this.getObjects()) {
                                    const sprite = obj;
                                    const initLocalPos = new Phaser.Math.Vector2();
                                    const worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                                    sprite.getWorldTransformMatrix(worldTx);
                                    worldTx.applyInverse(shapePos.x, shapePos.y, initLocalPos);
                                    obj.setData("OriginTool", {
                                        initOriginX: sprite.originX,
                                        initOriginY: sprite.originY,
                                        initX: sprite.x,
                                        initY: sprite.y,
                                        initWorldTx: worldTx,
                                        initLocalPos: initLocalPos
                                    });
                                }
                            }
                        }
                        onMouseMove() {
                            if (!this._dragging) {
                                return;
                            }
                            this.requestRepaint = true;
                            const pointer = this.getToolPointer();
                            const pointerPos = this.getScenePoint(pointer.x, pointer.y);
                            for (let obj of this.getObjects()) {
                                const sprite = obj;
                                const data = obj.data.get("OriginTool");
                                const initLocalPos = data.initLocalPos;
                                const flipX = sprite.flipX ? -1 : 1;
                                const flipY = sprite.flipY ? -1 : 1;
                                const localPos = new Phaser.Math.Vector2();
                                const worldTx = data.initWorldTx;
                                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);
                                const dx = (localPos.x - initLocalPos.x) * flipX;
                                const dy = (localPos.y - initLocalPos.y) * flipY;
                                const width = sprite.width;
                                const height = sprite.height;
                                const originDX = dx / width;
                                const originDY = dy / height;
                                consoleLog("---");
                                consoleLog("width " + width);
                                consoleLog("dx " + dx);
                                consoleLog("originDX " + originDX);
                                const newOriginX = data.initOriginX + (this._changeX ? originDX : 0);
                                const newOriginY = data.initOriginY + (this._changeY ? originDY : 0);
                                // restore position
                                const local1 = new Phaser.Math.Vector2(data.initOriginX * width, data.initOriginY * height);
                                const local2 = new Phaser.Math.Vector2(newOriginX * width, newOriginY * height);
                                const parent1 = this.localToParent(sprite, local1);
                                const parent2 = this.localToParent(sprite, local2);
                                const dx2 = parent2.x - parent1.x;
                                const dy2 = parent2.y - parent1.y;
                                sprite.x = data.initX + dx2 * flipX;
                                sprite.y = data.initY + dy2 * flipY;
                                sprite.setOrigin(newOriginX, newOriginY);
                            }
                        }
                        onMouseUp() {
                            if (this._dragging) {
                                const msg = scene.BuildMessage.SetOriginProperties(this.getObjects());
                                scene.Editor.getInstance().sendMessage(msg);
                            }
                            this._dragging = false;
                            this._handlerShape.setFillStyle(this._color);
                            this.requestRepaint = true;
                        }
                    }
                    scene.OriginTool = OriginTool;
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
                    class PositionTool extends scene.InteractiveTool {
                        constructor(changeX, changeY) {
                            super();
                            this._dragging = false;
                            this._changeX = changeX;
                            this._changeY = changeY;
                            if (changeX && changeY) {
                                this._color = 0xffff00;
                            }
                            else if (changeX) {
                                this._color = 0xff0000;
                            }
                            else {
                                this._color = 0x00ff00;
                            }
                            this._handlerShape = changeX && changeY ? this.createRectangleShape() : this.createArrowShape();
                            this._handlerShape.setFillStyle(this._color);
                        }
                        canEdit(obj) {
                            return obj.x !== null;
                        }
                        getX() {
                            return this._handlerShape.x;
                        }
                        getY() {
                            return this._handlerShape.y;
                        }
                        clear() {
                            this._handlerShape.visible = false;
                        }
                        render(list) {
                            const cam = scene.Editor.getInstance().getObjectScene().cameras.main;
                            const pos = new Phaser.Math.Vector2(0, 0);
                            let angle = 0;
                            const localCoords = scene.Editor.getInstance().isTransformLocalCoords();
                            const globalCenterXY = new Phaser.Math.Vector2();
                            for (let sprite of list) {
                                const worldTx = sprite.getWorldTransformMatrix();
                                const centerXY = new Phaser.Math.Vector2();
                                worldTx.transformPoint(0, 0, centerXY);
                                globalCenterXY.add(centerXY);
                                const worldXY = new Phaser.Math.Vector2();
                                let localX = 0;
                                let localY = 0;
                                if (localCoords) {
                                    if (!this._changeX || !this._changeY) {
                                        if (this._changeX) {
                                            localX += scene.ARROW_LENGTH / cam.zoom / sprite.scaleX;
                                        }
                                        else {
                                            localY += scene.ARROW_LENGTH / cam.zoom / sprite.scaleY;
                                        }
                                    }
                                    angle += this.objectGlobalAngle(sprite);
                                    worldTx.transformPoint(localX, localY, worldXY);
                                }
                                else {
                                    if (!this._changeX || !this._changeY) {
                                        worldTx.transformPoint(0, 0, worldXY);
                                        if (this._changeX) {
                                            worldXY.x += scene.ARROW_LENGTH / cam.zoom;
                                        }
                                        else {
                                            worldXY.y += scene.ARROW_LENGTH / cam.zoom;
                                        }
                                    }
                                    else {
                                        worldTx.transformPoint(0, 0, worldXY);
                                    }
                                }
                                pos.add(worldXY);
                            }
                            const len = this.getObjects().length;
                            pos.x /= len;
                            pos.y /= len;
                            this._arrowPoint = new Phaser.Math.Vector2(pos.x, pos.y);
                            this._centerPoint = new Phaser.Math.Vector2(globalCenterXY.x / len, globalCenterXY.y / len);
                            const cameraX = (pos.x - cam.scrollX) * cam.zoom;
                            const cameraY = (pos.y - cam.scrollY) * cam.zoom;
                            this._handlerShape.setPosition(cameraX, cameraY);
                            this._handlerShape.angle = angle / len;
                            if (this._changeX) {
                                this._handlerShape.angle -= 90;
                            }
                            this._handlerShape.visible = true;
                        }
                        containsPointer() {
                            const pointer = this.getToolPointer();
                            const d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);
                            return d <= this._handlerShape.width;
                        }
                        isEditing() {
                            return this._dragging;
                        }
                        onMouseDown() {
                            if (this.containsPointer()) {
                                this._dragging = true;
                                this.requestRepaint = true;
                                const pointer = this.getToolPointer();
                                this._startCursor = this.getScenePoint(pointer.x, pointer.y);
                                this._handlerShape.setFillStyle(0xffffff);
                                this._startVector = new Phaser.Math.Vector2(this._arrowPoint.x - this._centerPoint.x, this._arrowPoint.y - this._centerPoint.y);
                                let p = new Phaser.Math.Vector2();
                                for (let obj of this.getObjects()) {
                                    const sprite = obj;
                                    const tx = sprite.getWorldTransformMatrix();
                                    tx.applyInverse(0, 0, p);
                                    sprite.setData("PositionTool", {
                                        initX: sprite.x,
                                        initY: sprite.y,
                                        initWorldTx: tx
                                    });
                                }
                            }
                        }
                        onMouseMove() {
                            if (!this._dragging) {
                                return;
                            }
                            this.requestRepaint = true;
                            const pointer = this.getToolPointer();
                            const endCursor = this.getScenePoint(pointer.x, pointer.y);
                            const localCoords = scene.Editor.getInstance().isTransformLocalCoords();
                            const changeXY = this._changeX && this._changeY;
                            for (let obj of this.getObjects()) {
                                const sprite = obj;
                                const data = sprite.data.get("PositionTool");
                                const p0 = new Phaser.Math.Vector2();
                                const p1 = new Phaser.Math.Vector2();
                                if (sprite.parentContainer) {
                                    const tx = sprite.parentContainer.getWorldTransformMatrix();
                                    tx.transformPoint(this._startCursor.x, this._startCursor.y, p0);
                                    tx.transformPoint(endCursor.x, endCursor.y, p1);
                                }
                                else {
                                    p0.setFromObject(this._startCursor);
                                    p1.setFromObject(endCursor);
                                }
                                let x;
                                let y;
                                if (changeXY) {
                                    const dx = p1.x - p0.x;
                                    const dy = p1.y - p0.y;
                                    x = data.initX + dx;
                                    y = data.initY + dy;
                                }
                                else {
                                    const vector = new Phaser.Math.Vector2(this._changeX ? 1 : 0, this._changeY ? 1 : 0);
                                    if (localCoords) {
                                        const tx = new Phaser.GameObjects.Components.TransformMatrix();
                                        tx.rotate(sprite.rotation);
                                        tx.transformPoint(vector.x, vector.y, vector);
                                    }
                                    const moveVector = new Phaser.Math.Vector2(endCursor.x - this._startCursor.x, endCursor.y - this._startCursor.y);
                                    const d = moveVector.dot(this._startVector) / this._startVector.length();
                                    vector.x *= d;
                                    vector.y *= d;
                                    x = data.initX + vector.x;
                                    y = data.initY + vector.y;
                                }
                                x = this.snapValueX(x);
                                y = this.snapValueY(y);
                                sprite.setPosition(x, y);
                            }
                        }
                        onMouseUp() {
                            if (this._dragging) {
                                const msg = scene.BuildMessage.SetTransformProperties(this.getObjects());
                                scene.Editor.getInstance().sendMessage(msg);
                            }
                            this._dragging = false;
                            this._handlerShape.setFillStyle(this._color);
                            this.requestRepaint = true;
                        }
                    }
                    scene.PositionTool = PositionTool;
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
                    class ScaleTool extends scene.InteractiveTool {
                        constructor(changeX, changeY) {
                            super();
                            this._dragging = false;
                            this._changeX = changeX;
                            this._changeY = changeY;
                            if (changeX && changeY) {
                                this._color = 0xffff00;
                            }
                            else if (changeX) {
                                this._color = 0xff0000;
                            }
                            else {
                                this._color = 0x00ff00;
                            }
                            this._handlerShape = this.createRectangleShape();
                            this._handlerShape.setFillStyle(this._color);
                        }
                        canEdit(obj) {
                            return obj.scaleX !== undefined;
                        }
                        clear() {
                            this._handlerShape.visible = false;
                        }
                        render(list) {
                            const pos = new Phaser.Math.Vector2(0, 0);
                            let angle = 0;
                            for (let sprite of list) {
                                let flipX = sprite.flipX ? -1 : 1;
                                let flipY = sprite.flipY ? -1 : 1;
                                if (sprite instanceof Phaser.GameObjects.TileSprite) {
                                    flipX = 1;
                                    flipY = 1;
                                }
                                angle += this.objectGlobalAngle(sprite);
                                const width = sprite.width * flipX;
                                const height = sprite.height * flipY;
                                let x = -width * sprite.originX;
                                let y = -height * sprite.originY;
                                let worldXY = new Phaser.Math.Vector2();
                                let worldTx = sprite.getWorldTransformMatrix();
                                const localX = this._changeX ? x + width : x + width / 2;
                                const localY = this._changeY ? y + height : y + height / 2;
                                worldTx.transformPoint(localX, localY, worldXY);
                                pos.add(worldXY);
                            }
                            const len = this.getObjects().length;
                            const cam = scene.Editor.getInstance().getObjectScene().cameras.main;
                            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
                            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
                            this._handlerShape.setPosition(cameraX, cameraY);
                            this._handlerShape.angle = angle / len + (this._changeX && !this._changeY ? -90 : 0);
                            this._handlerShape.visible = true;
                        }
                        containsPointer() {
                            const toolPointer = this.getToolPointer();
                            const d = Phaser.Math.Distance.Between(toolPointer.x, toolPointer.y, this._handlerShape.x, this._handlerShape.y);
                            return d <= this._handlerShape.width;
                        }
                        isEditing() {
                            return this._dragging;
                        }
                        onMouseDown() {
                            if (this.containsPointer()) {
                                this._dragging = true;
                                this.requestRepaint = true;
                                this._handlerShape.setFillStyle(0xffffff);
                                const shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);
                                for (let obj of this.getObjects()) {
                                    const sprite = obj;
                                    const worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                                    const initLocalPos = new Phaser.Math.Vector2();
                                    sprite.getWorldTransformMatrix(worldTx);
                                    worldTx.applyInverse(shapePos.x, shapePos.y, initLocalPos);
                                    sprite.setData("ScaleTool", {
                                        initScaleX: sprite.scaleX,
                                        initScaleY: sprite.scaleY,
                                        initWidth: sprite.width,
                                        initHeight: sprite.height,
                                        initLocalPos: initLocalPos,
                                        initWorldTx: worldTx
                                    });
                                }
                            }
                        }
                        onMouseMove() {
                            if (!this._dragging) {
                                return;
                            }
                            this.requestRepaint = true;
                            const pointer = this.getToolPointer();
                            const pointerPos = this.getScenePoint(pointer.x, pointer.y);
                            for (let obj of this.getObjects()) {
                                const sprite = obj;
                                const data = sprite.data.get("ScaleTool");
                                const initLocalPos = data.initLocalPos;
                                const localPos = new Phaser.Math.Vector2();
                                const worldTx = data.initWorldTx;
                                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);
                                let flipX = sprite.flipX ? -1 : 1;
                                let flipY = sprite.flipY ? -1 : 1;
                                if (sprite instanceof Phaser.GameObjects.TileSprite) {
                                    flipX = 1;
                                    flipY = 1;
                                }
                                const dx = (localPos.x - initLocalPos.x) * flipX;
                                const dy = (localPos.y - initLocalPos.y) * flipY;
                                let width = data.initWidth - sprite.displayOriginX;
                                let height = data.initHeight - sprite.displayOriginY;
                                if (width === 0) {
                                    width = data.initWidth;
                                }
                                if (height === 0) {
                                    height = data.initHeight;
                                }
                                const scaleDX = dx / width * data.initScaleX;
                                const scaleDY = dy / height * data.initScaleY;
                                const newScaleX = data.initScaleX + scaleDX;
                                const newScaleY = data.initScaleY + scaleDY;
                                if (this._changeX) {
                                    sprite.scaleX = newScaleX;
                                }
                                if (this._changeY) {
                                    sprite.scaleY = newScaleY;
                                }
                            }
                        }
                        onMouseUp() {
                            if (this._dragging) {
                                const msg = scene.BuildMessage.SetTransformProperties(this.getObjects());
                                scene.Editor.getInstance().sendMessage(msg);
                            }
                            this._dragging = false;
                            this._handlerShape.setFillStyle(this._color);
                            this.requestRepaint = true;
                        }
                    }
                    scene.ScaleTool = ScaleTool;
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
                    class TilePositionTool extends scene.InteractiveTool {
                        constructor(changeX, changeY) {
                            super();
                            this._dragging = false;
                            this._changeX = changeX;
                            this._changeY = changeY;
                            if (changeX && changeY) {
                                this._color = 0xffff00;
                            }
                            else if (changeX) {
                                this._color = 0xff0000;
                            }
                            else {
                                this._color = 0x00ff00;
                            }
                            this._handlerShape = changeX && changeY ? this.createRectangleShape() : this.createArrowShape();
                            this._handlerShape.setFillStyle(this._color);
                        }
                        canEdit(obj) {
                            return obj instanceof Phaser.GameObjects.TileSprite;
                        }
                        getX() {
                            return this._handlerShape.x;
                        }
                        getY() {
                            return this._handlerShape.y;
                        }
                        clear() {
                            this._handlerShape.visible = false;
                        }
                        render(list) {
                            const cam = scene.Editor.getInstance().getObjectScene().cameras.main;
                            const pos = new Phaser.Math.Vector2(0, 0);
                            let angle = 0;
                            for (let obj of list) {
                                const sprite = obj;
                                const worldXY = new Phaser.Math.Vector2();
                                const worldTx = sprite.getWorldTransformMatrix();
                                const localLeft = -sprite.width * sprite.originX;
                                const localTop = -sprite.height * sprite.originY;
                                let localX = localLeft + sprite.tilePositionX;
                                let localY = localTop + sprite.tilePositionY;
                                if (!this._changeX || !this._changeY) {
                                    if (this._changeX) {
                                        localX += scene.ARROW_LENGTH / cam.zoom / sprite.scaleX;
                                    }
                                    else {
                                        localY += scene.ARROW_LENGTH / cam.zoom / sprite.scaleY;
                                    }
                                }
                                angle += this.objectGlobalAngle(sprite);
                                worldTx.transformPoint(localX, localY, worldXY);
                                pos.add(worldXY);
                            }
                            const len = this.getObjects().length;
                            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
                            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
                            this._handlerShape.setPosition(cameraX, cameraY);
                            this._handlerShape.angle = angle / len;
                            if (this._changeX) {
                                this._handlerShape.angle -= 90;
                            }
                            this._handlerShape.visible = true;
                        }
                        containsPointer() {
                            const pointer = this.getToolPointer();
                            const d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);
                            return d <= this._handlerShape.width;
                        }
                        isEditing() {
                            return this._dragging;
                        }
                        onMouseDown() {
                            if (this.containsPointer()) {
                                this._dragging = true;
                                this.requestRepaint = true;
                                this._handlerShape.setFillStyle(0xffffff);
                                const worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                                const shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);
                                for (let obj of this.getObjects()) {
                                    const sprite = obj;
                                    const initLocalPos = new Phaser.Math.Vector2();
                                    sprite.getWorldTransformMatrix(worldTx);
                                    worldTx.applyInverse(shapePos.x, shapePos.y, initLocalPos);
                                    sprite.setData("TilePositionTool", {
                                        initTilePositionX: sprite.tilePositionX,
                                        initTilePositionY: sprite.tilePositionY,
                                        initLocalPos: initLocalPos
                                    });
                                }
                            }
                        }
                        onMouseMove() {
                            if (!this._dragging) {
                                return;
                            }
                            this.requestRepaint = true;
                            const pointer = this.getToolPointer();
                            const pointerPos = this.getScenePoint(pointer.x, pointer.y);
                            const worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                            for (let obj of this.getObjects()) {
                                const sprite = obj;
                                const data = sprite.data.get("TilePositionTool");
                                const initLocalPos = data.initLocalPos;
                                const localPos = new Phaser.Math.Vector2();
                                sprite.getWorldTransformMatrix(worldTx);
                                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);
                                const dx = localPos.x - initLocalPos.x;
                                const dy = localPos.y - initLocalPos.y;
                                let tilePositionX = (data.initTilePositionX + dx) | 0;
                                let tilePositionY = (data.initTilePositionY + dy) | 0;
                                tilePositionX = this.snapValueX(tilePositionX);
                                tilePositionY = this.snapValueY(tilePositionY);
                                if (this._changeX) {
                                    sprite.tilePositionX = tilePositionX;
                                }
                                if (this._changeY) {
                                    sprite.tilePositionY = tilePositionY;
                                }
                            }
                        }
                        onMouseUp() {
                            if (this._dragging) {
                                const msg = scene.BuildMessage.SetTileSpriteProperties(this.getObjects());
                                scene.Editor.getInstance().sendMessage(msg);
                            }
                            this._dragging = false;
                            this._handlerShape.setFillStyle(this._color);
                            this.requestRepaint = true;
                        }
                    }
                    scene.TilePositionTool = TilePositionTool;
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
                    class TileScaleTool extends scene.InteractiveTool {
                        constructor(changeX, changeY) {
                            super();
                            this._dragging = false;
                            this._changeX = changeX;
                            this._changeY = changeY;
                            if (changeX && changeY) {
                                this._color = 0xffff00;
                            }
                            else if (changeX) {
                                this._color = 0xff0000;
                            }
                            else {
                                this._color = 0x00ff00;
                            }
                            this._handlerShape = this.createRectangleShape();
                            this._handlerShape.setFillStyle(this._color);
                        }
                        canEdit(obj) {
                            return obj instanceof Phaser.GameObjects.TileSprite;
                        }
                        getX() {
                            return this._handlerShape.x;
                        }
                        getY() {
                            return this._handlerShape.y;
                        }
                        clear() {
                            this._handlerShape.visible = false;
                        }
                        render(list) {
                            const pos = new Phaser.Math.Vector2(0, 0);
                            let angle = 0;
                            for (let obj of list) {
                                const sprite = obj;
                                const worldXY = new Phaser.Math.Vector2();
                                const worldTx = sprite.getWorldTransformMatrix();
                                const localLeft = -sprite.width * sprite.originX;
                                const localTop = -sprite.height * sprite.originY;
                                let localX = localLeft + sprite.tilePositionX;
                                let localY = localTop + sprite.tilePositionY;
                                if (!this._changeX || !this._changeY) {
                                    if (this._changeX) {
                                        localX += sprite.width * sprite.tileScaleX;
                                    }
                                    else {
                                        localY += sprite.height * sprite.tileScaleY;
                                    }
                                }
                                angle += this.objectGlobalAngle(sprite);
                                worldTx.transformPoint(localX, localY, worldXY);
                                pos.add(worldXY);
                            }
                            const len = this.getObjects().length;
                            const cam = scene.Editor.getInstance().getObjectScene().cameras.main;
                            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
                            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
                            this._handlerShape.setPosition(cameraX, cameraY);
                            this._handlerShape.angle = angle / len;
                            if (this._changeX) {
                                this._handlerShape.angle -= 90;
                            }
                            this._handlerShape.visible = true;
                        }
                        containsPointer() {
                            const pointer = this.getToolPointer();
                            const d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);
                            return d <= this._handlerShape.width;
                        }
                        isEditing() {
                            return this._dragging;
                        }
                        onMouseDown() {
                            if (this.containsPointer()) {
                                this._dragging = true;
                                this.requestRepaint = true;
                                this._handlerShape.setFillStyle(0xffffff);
                                const worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                                const shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);
                                for (let obj of this.getObjects()) {
                                    const sprite = obj;
                                    const initLocalPos = new Phaser.Math.Vector2();
                                    sprite.getWorldTransformMatrix(worldTx);
                                    worldTx.applyInverse(shapePos.x, shapePos.y, initLocalPos);
                                    sprite.setData("TileScaleTool", {
                                        initTileScaleX: sprite.tileScaleX,
                                        initTileScaleY: sprite.tileScaleY,
                                        initLocalPos: initLocalPos
                                    });
                                }
                            }
                        }
                        onMouseMove() {
                            if (!this._dragging) {
                                return;
                            }
                            this.requestRepaint = true;
                            const pointer = this.getToolPointer();
                            const pointerPos = this.getScenePoint(pointer.x, pointer.y);
                            const worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                            for (let obj of this.getObjects()) {
                                const sprite = obj;
                                const data = sprite.data.get("TileScaleTool");
                                const initLocalPos = data.initLocalPos;
                                const localPos = new Phaser.Math.Vector2();
                                sprite.getWorldTransformMatrix(worldTx);
                                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);
                                const dx = localPos.x - initLocalPos.x;
                                const dy = localPos.y - initLocalPos.y;
                                const tileScaleX = data.initTileScaleX + dx / sprite.width;
                                const tileScaleY = data.initTileScaleY + dy / sprite.height;
                                if (this._changeX) {
                                    sprite.tileScaleX = tileScaleX;
                                }
                                if (this._changeY) {
                                    sprite.tileScaleY = tileScaleY;
                                }
                            }
                        }
                        onMouseUp() {
                            if (this._dragging) {
                                const msg = scene.BuildMessage.SetTileSpriteProperties(this.getObjects());
                                scene.Editor.getInstance().sendMessage(msg);
                            }
                            this._dragging = false;
                            this._handlerShape.setFillStyle(this._color);
                            this.requestRepaint = true;
                        }
                    }
                    scene.TileScaleTool = TileScaleTool;
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
                    class TileSizeTool extends scene.InteractiveTool {
                        constructor(changeX, changeY) {
                            super();
                            this._dragging = false;
                            this._changeX = changeX;
                            this._changeY = changeY;
                            if (changeX && changeY) {
                                this._color = 0xffff00;
                            }
                            else if (changeX) {
                                this._color = 0xff0000;
                            }
                            else {
                                this._color = 0x00ff00;
                            }
                            this._handlerShape = this.createRectangleShape();
                            this._handlerShape.setFillStyle(this._color);
                        }
                        canEdit(obj) {
                            return obj instanceof Phaser.GameObjects.TileSprite;
                        }
                        clear() {
                            this._handlerShape.visible = false;
                        }
                        render(list) {
                            const pos = new Phaser.Math.Vector2(0, 0);
                            let angle = 0;
                            for (let obj of list) {
                                let sprite = obj;
                                angle += this.objectGlobalAngle(sprite);
                                const width = sprite.width;
                                const height = sprite.height;
                                let x = -width * sprite.originX;
                                let y = -height * sprite.originY;
                                let worldXY = new Phaser.Math.Vector2();
                                let worldTx = sprite.getWorldTransformMatrix();
                                const localX = this._changeX ? x + width : x + width / 2;
                                const localY = this._changeY ? y + height : y + height / 2;
                                worldTx.transformPoint(localX, localY, worldXY);
                                pos.add(worldXY);
                            }
                            const len = this.getObjects().length;
                            const cam = scene.Editor.getInstance().getObjectScene().cameras.main;
                            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
                            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
                            this._handlerShape.setPosition(cameraX, cameraY);
                            this._handlerShape.angle = angle / len + (this._changeX && !this._changeY ? -90 : 0);
                            this._handlerShape.visible = true;
                        }
                        containsPointer() {
                            const toolPointer = this.getToolPointer();
                            const d = Phaser.Math.Distance.Between(toolPointer.x, toolPointer.y, this._handlerShape.x, this._handlerShape.y);
                            return d <= this._handlerShape.width;
                        }
                        isEditing() {
                            return this._dragging;
                        }
                        onMouseDown() {
                            if (this.containsPointer()) {
                                this._dragging = true;
                                this.requestRepaint = true;
                                this._handlerShape.setFillStyle(0xffffff);
                                const worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                                const shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);
                                for (let obj of this.getObjects()) {
                                    const sprite = obj;
                                    const initLocalPos = new Phaser.Math.Vector2();
                                    sprite.getWorldTransformMatrix(worldTx);
                                    worldTx.applyInverse(shapePos.x, shapePos.y, initLocalPos);
                                    sprite.setData("TileSizeTool", {
                                        initWidth: sprite.width,
                                        initHeight: sprite.height,
                                        initLocalPos: initLocalPos
                                    });
                                }
                            }
                        }
                        onMouseMove() {
                            if (!this._dragging) {
                                return;
                            }
                            this.requestRepaint = true;
                            const pointer = this.getToolPointer();
                            const pointerPos = this.getScenePoint(pointer.x, pointer.y);
                            const worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                            for (let obj of this.getObjects()) {
                                const sprite = obj;
                                const data = sprite.data.get("TileSizeTool");
                                const initLocalPos = data.initLocalPos;
                                const localPos = new Phaser.Math.Vector2();
                                sprite.getWorldTransformMatrix(worldTx);
                                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);
                                const flipX = sprite.flipX ? -1 : 1;
                                const flipY = sprite.flipY ? -1 : 1;
                                const dx = (localPos.x - initLocalPos.x) * flipX;
                                const dy = (localPos.y - initLocalPos.y) * flipY;
                                let width = (data.initWidth + dx) | 0;
                                let height = (data.initHeight + dy) | 0;
                                width = this.snapValueX(width);
                                height = this.snapValueY(height);
                                if (this._changeX) {
                                    sprite.setSize(width, sprite.height);
                                }
                                if (this._changeY) {
                                    sprite.setSize(sprite.width, height);
                                }
                            }
                        }
                        onMouseUp() {
                            if (this._dragging) {
                                const msg = scene.BuildMessage.SetTileSpriteProperties(this.getObjects());
                                scene.Editor.getInstance().sendMessage(msg);
                            }
                            this._dragging = false;
                            this._handlerShape.setFillStyle(this._color);
                            this.requestRepaint = true;
                        }
                    }
                    scene.TileSizeTool = TileSizeTool;
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
                class BlocksView extends ide.ViewPart {
                    constructor() {
                        super("blocksView");
                        this.setTitle("Blocks");
                        this.setIcon(ide.Workbench.getWorkbench().getWorkbenchIcon(ide.ICON_BLOCKS));
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
                    }
                    getTreeRenderer() {
                        return this._treeRenderer;
                    }
                    setTreeRenderer(treeRenderer) {
                        this._treeRenderer = treeRenderer;
                    }
                    onClick(e) {
                        for (let icon of this._treeIconList) {
                            if (icon.rect.contains(e.offsetX, e.offsetY)) {
                                this.setExpanded(icon.obj, !this.isExpanded(icon.obj));
                                this.repaint();
                                return;
                            }
                        }
                    }
                    visitObjects(visitor) {
                        const list = this.getContentProvider().getRoots(this.getInput());
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
                        const roots = this.getContentProvider().getRoots(this.getInput());
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
                        const result = this.paintItems(roots, treeIconList, paintItems, x, y);
                        const contentHeight = result.y - viewer.getScrollY();
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
                const GRID_PADDING = 5;
                class GridTreeRenderer extends viewers.TreeViewerRenderer {
                    constructor(viewer, center = false) {
                        super(viewer);
                        viewer.setCellSize(128);
                        this._center = center;
                    }
                    paintItems(objects, treeIconList, paintItems, x, y) {
                        const viewer = this.getViewer();
                        if (viewer.getCellSize() <= 48) {
                            return super.paintItems(objects, treeIconList, paintItems, x, y + GRID_PADDING);
                        }
                        const b = viewer.getBounds();
                        const offset = this._center ? Math.floor(b.width % (viewer.getCellSize() + GRID_PADDING) / 2) : 0;
                        return this.paintItems2(objects, treeIconList, paintItems, x + offset, y + GRID_PADDING, offset);
                    }
                    paintItems2(objects, treeIconList, paintItems, x, y, offset) {
                        const viewer = this.getViewer();
                        const cellSize = Math.max(controls.ROW_HEIGHT, viewer.getCellSize());
                        const context = viewer.getContext();
                        const b = viewer.getBounds();
                        for (let obj of objects) {
                            const children = viewer.getContentProvider().getChildren(obj);
                            const expanded = viewer.isExpanded(obj);
                            if (viewer.isFilterIncluded(obj)) {
                                const renderer = viewer.getCellRendererProvider().getCellRenderer(obj);
                                const args = new viewers.RenderCellArgs(context, x, y, cellSize, cellSize, obj, viewer, true);
                                if (y > -viewer.getCellSize() && y < b.height) {
                                    this.renderGridCell(args, renderer);
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
                                x += cellSize + GRID_PADDING;
                                if (x + cellSize > b.width) {
                                    y += cellSize + GRID_PADDING;
                                    x = 0 + offset;
                                }
                            }
                            if (expanded) {
                                const result = this.paintItems(children, treeIconList, paintItems, x, y);
                                y = result.y;
                                x = result.x;
                            }
                        }
                        return {
                            x: x,
                            y: y
                        };
                    }
                    renderGridCell(args, renderer) {
                        const lineHeight = 20;
                        let x = args.x;
                        let y = args.y;
                        const ctx = args.canvasContext;
                        const label = args.viewer.getLabelProvider().getLabel(args.obj);
                        let lines = [""];
                        for (const c of label) {
                            const test = lines[lines.length - 1] + c;
                            const m = ctx.measureText(test);
                            if (m.width > args.w) {
                                if (lines.length === 2) {
                                    lines[lines.length - 1] += "..";
                                    break;
                                }
                                else {
                                    lines.push("");
                                    lines[lines.length - 1] = c;
                                }
                            }
                            else {
                                lines[lines.length - 1] += c;
                            }
                        }
                        {
                            const args2 = new viewers.RenderCellArgs(args.canvasContext, args.x + 3, args.y + 3, args.w - 6, args.h - lines.length * lineHeight - 6, args.obj, args.viewer, true);
                            const strH = lines.length * lineHeight;
                            if (args.viewer.isSelected(args.obj)) {
                                ctx.save();
                                ctx.fillStyle = controls.Controls.theme.treeItemSelectionBackground;
                                ctx.globalAlpha = 0.5;
                                ctx.fillRect(args2.x - 3, args2.y - 3, args2.w + 6, args2.h + 6);
                                ctx.globalAlpha = 1;
                                renderer.renderCell(args2);
                                ctx.globalAlpha = 0.3;
                                ctx.fillRect(args2.x - 3, args2.y - 3, args2.w + 6, args2.h + 6);
                                ctx.restore();
                            }
                            else {
                                renderer.renderCell(args2);
                            }
                            args.viewer.paintItemBackground(args.obj, args.x, args.y + args.h - strH - 3, args.w, strH, 10);
                            y += args2.h + lineHeight * lines.length;
                        }
                        ctx.save();
                        if (args.viewer.isSelected(args.obj)) {
                            ctx.fillStyle = controls.Controls.theme.treeItemSelectionForeground;
                        }
                        else {
                            ctx.fillStyle = controls.Controls.theme.treeItemForeground;
                        }
                        let y2 = y - lineHeight * (lines.length - 1) - 5;
                        for (const line of lines) {
                            const m = ctx.measureText(line);
                            ctx.fillText(line, x + args.w / 2 - m.width / 2, y2);
                            y2 += lineHeight;
                        }
                        ctx.restore();
                    }
                }
                viewers.GridTreeRenderer = GridTreeRenderer;
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
                            this.setTreeRenderer(new ui.controls.viewers.GridTreeRenderer(this, true));
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
                        super.createPart();
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
