var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __generator = (this && this.__generator) || function (thisArg, body) {
    var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g;
    return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
    function verb(n) { return function (v) { return step([n, v]); }; }
    function step(op) {
        if (f) throw new TypeError("Generator is already executing.");
        while (_) try {
            if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [op[0] & 2, t.value];
            switch (op[0]) {
                case 0: case 1: t = op; break;
                case 4: _.label++; return { value: op[1], done: false };
                case 5: _.label++; y = op[1]; op = [0]; continue;
                case 7: op = _.ops.pop(); _.trys.pop(); continue;
                default:
                    if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
                    if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
                    if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
                    if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
                    if (t[2]) _.ops.pop();
                    _.trys.pop(); continue;
            }
            op = body.call(thisArg, _);
        } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
        if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
    }
};
var phasereditor2d;
(function (phasereditor2d) {
    var demo;
    (function (demo) {
        function main() {
            console.log("Starting workbench.");
            var workbench = phasereditor2d.ui.Workbench.getWorkbench();
            workbench.start();
        }
        demo.main = main;
    })(demo = phasereditor2d.demo || (phasereditor2d.demo = {}));
})(phasereditor2d || (phasereditor2d = {}));
window.addEventListener("load", function () {
    phasereditor2d.ui.controls.Controls.preload(function () {
        phasereditor2d.demo.main();
    });
});
var phasereditor2d;
(function (phasereditor2d) {
    var core;
    (function (core) {
        var io;
        (function (io) {
            var EMPTY_FILES = [];
            var FilePath = /** @class */ (function () {
                function FilePath(parent, fileData) {
                    this._parent = parent;
                    this._name = fileData.name;
                    this._isFile = fileData.isFile;
                    this._contentType = this._isFile ? fileData.contentType : null;
                    if (fileData.children) {
                        this._files = [];
                        for (var _i = 0, _a = fileData.children; _i < _a.length; _i++) {
                            var child = _a[_i];
                            this._files.push(new FilePath(this, child));
                        }
                        this._files.sort(function (a, b) {
                            var a1 = a._isFile ? 1 : 0;
                            var b1 = b._isFile ? 1 : 0;
                            return a1 - b1;
                        });
                    }
                    else {
                        this._files = EMPTY_FILES;
                    }
                }
                FilePath.prototype.getContentType = function () {
                    return this._contentType;
                };
                FilePath.prototype.getName = function () {
                    return this._name;
                };
                FilePath.prototype.getParent = function () {
                    return this._parent;
                };
                FilePath.prototype.isFile = function () {
                    return this._isFile;
                };
                FilePath.prototype.isFolder = function () {
                    return !this.isFile();
                };
                FilePath.prototype.getFiles = function () {
                    return this._files;
                };
                FilePath.prototype.toString = function () {
                    if (this._parent) {
                        return this._parent.toString() + "/" + this._name;
                    }
                    return this._name;
                };
                FilePath.prototype.toStringTree = function () {
                    return this.toStringTree2(0);
                };
                FilePath.prototype.toStringTree2 = function (depth) {
                    var s = " ".repeat(depth * 4);
                    s += this.getName() + (this.isFolder() ? "/" : "") + "\n";
                    if (this.isFolder()) {
                        for (var _i = 0, _a = this._files; _i < _a.length; _i++) {
                            var file = _a[_i];
                            s += file.toStringTree2(depth + 1);
                        }
                    }
                    return s;
                };
                return FilePath;
            }());
            io.FilePath = FilePath;
        })(io = core.io || (core.io = {}));
    })(core = phasereditor2d.core || (phasereditor2d.core = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var ROW_HEIGHT = 20;
            var FONT_HEIGHT = 14;
            var FONT_OFFSET = 2;
            var ACTION_WIDTH = 20;
            var PANEL_BORDER_SIZE = 4;
            var SPLIT_OVER_ZONE_WIDTH = 6;
            var IconImpl = /** @class */ (function () {
                function IconImpl(img) {
                    this.img = img;
                }
                IconImpl.prototype.paint = function (context, x, y, w, h) {
                    // we assume the image size is under 16x16 (for now)
                    w = w ? w : 16;
                    h = h ? h : 16;
                    var imgW = this.img.naturalWidth;
                    var imgH = this.img.naturalHeight;
                    var dx = (w - imgW) / 2;
                    var dy = (h - imgH) / 2;
                    context.drawImage(this.img, (x + dx) | 0, (y + dy) | 0);
                };
                return IconImpl;
            }());
            var Controls = /** @class */ (function () {
                function Controls() {
                }
                Controls.preload = function (callback) {
                    return __awaiter(this, void 0, void 0, function () {
                        var _i, _a, name_1, icon;
                        return __generator(this, function (_b) {
                            switch (_b.label) {
                                case 0:
                                    _i = 0, _a = Controls.ICONS;
                                    _b.label = 1;
                                case 1:
                                    if (!(_i < _a.length)) return [3 /*break*/, 4];
                                    name_1 = _a[_i];
                                    icon = this.getIcon(name_1);
                                    return [4 /*yield*/, icon.img.decode()];
                                case 2:
                                    _b.sent();
                                    _b.label = 3;
                                case 3:
                                    _i++;
                                    return [3 /*break*/, 1];
                                case 4:
                                    callback();
                                    return [2 /*return*/];
                            }
                        });
                    });
                };
                Controls.getIcon = function (name) {
                    if (Controls._images.has(name)) {
                        return Controls._images.get(name);
                    }
                    var img = new Image();
                    img.src = "phasereditor2d/ui/controls/images/16/" + name + ".png";
                    var icon = new IconImpl(img);
                    Controls._images.set(name, icon);
                    return icon;
                };
                Controls.getSmoothingPrefix = function (context) {
                    var vendors = ['i', 'webkitI', 'msI', 'mozI', 'oI'];
                    for (var i = 0; i < vendors.length; i++) {
                        var s = vendors[i] + 'mageSmoothingEnabled';
                        if (s in context) {
                            return s;
                        }
                    }
                    return null;
                };
                ;
                Controls.disableCanvasSmoothing = function (context) {
                    var prefix = this.getSmoothingPrefix(context);
                    if (prefix) {
                        context[prefix] = false;
                    }
                    return context;
                };
                ;
                Controls._images = new Map();
                Controls.ICON_TREE_COLLAPSE = "tree-collapse";
                Controls.ICON_TREE_EXPAND = "tree-expand";
                Controls.ICON_FILE = "file";
                Controls.ICON_FOLDER = "folder";
                Controls.ICON_FILE_FONT = "file-font";
                Controls.ICON_FILE_IMAGE = "file-image";
                Controls.ICON_FILE_VIDEO = "file-movie";
                Controls.ICON_FILE_SCRIPT = "file-script";
                Controls.ICON_FILE_SOUND = "file-sound";
                Controls.ICON_FILE_TEXT = "file-text";
                Controls.ICONS = [
                    Controls.ICON_TREE_COLLAPSE,
                    Controls.ICON_TREE_EXPAND,
                    Controls.ICON_FILE,
                    Controls.ICON_FOLDER,
                    Controls.ICON_FILE_FONT,
                    Controls.ICON_FILE_IMAGE,
                    Controls.ICON_FILE_SCRIPT,
                    Controls.ICON_FILE_SOUND,
                    Controls.ICON_FILE_TEXT,
                    Controls.ICON_FILE_VIDEO
                ];
                Controls.LIGHT_THEME = {
                    treeItemOverBackground: "#0000001f",
                    treeItemSelectionBackground: "#5555ffdf",
                    treeItemSelectionForeground: "#fafafa"
                };
                Controls.DARK_THEME = Controls.LIGHT_THEME;
                Controls.theme = Controls.LIGHT_THEME;
                return Controls;
            }());
            controls.Controls = Controls;
            function setElementBounds(elem, bounds) {
                elem.style.left = bounds.x + "px";
                elem.style.top = bounds.y + "px";
                elem.style.width = bounds.width + "px";
                elem.style.height = bounds.height + "px";
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
            var FillLayout = /** @class */ (function () {
                function FillLayout(padding) {
                    if (padding === void 0) { padding = 0; }
                    this._padding = 0;
                    this._padding = padding;
                }
                FillLayout.prototype.getPadding = function () {
                    return this._padding;
                };
                FillLayout.prototype.setPadding = function (padding) {
                    this._padding = padding;
                };
                FillLayout.prototype.layout = function (parent) {
                    var children = parent.getChildren();
                    if (children.length > 1) {
                        console.warn("[FillLayout] Invalid number for children or parent control.");
                    }
                    var b = parent.getBounds();
                    setElementBounds(parent.getElement(), b);
                    if (children.length > 0) {
                        var child = children[0];
                        child.setBoundsValues(this._padding, this._padding, b.width - this._padding * 2, b.height - this._padding * 2);
                    }
                };
                return FillLayout;
            }());
            controls.FillLayout = FillLayout;
            var Control = /** @class */ (function () {
                function Control(tagName) {
                    if (tagName === void 0) { tagName = "div"; }
                    this._bounds = { x: 0, y: 0, width: 0, height: 0 };
                    this._children = [];
                    this._element = document.createElement(tagName);
                    this.addClass("control");
                    this._layout = null;
                }
                Control.prototype.getLayout = function () {
                    return this._layout;
                };
                Control.prototype.setLayout = function (layout) {
                    this._layout = layout;
                    this.layout();
                };
                Control.prototype.addClass = function () {
                    var tokens = [];
                    for (var _i = 0; _i < arguments.length; _i++) {
                        tokens[_i] = arguments[_i];
                    }
                    for (var _a = 0, tokens_1 = tokens; _a < tokens_1.length; _a++) {
                        var token = tokens_1[_a];
                        this._element.classList.add(token);
                    }
                };
                Control.prototype.getElement = function () {
                    return this._element;
                };
                Control.prototype.getControlPosition = function (windowX, windowY) {
                    var b = this.getElement().getBoundingClientRect();
                    return {
                        x: windowX - b.left,
                        y: windowY - b.top
                    };
                };
                Control.prototype.containsLocalPoint = function (x, y) {
                    return x >= 0 && x <= this._bounds.width && y >= 0 && y <= this._bounds.height;
                };
                Control.prototype.setBounds = function (bounds) {
                    if (bounds.x !== undefined) {
                        this._bounds.x = bounds.x;
                    }
                    this._bounds.x = bounds.x === undefined ? this._bounds.x : bounds.x;
                    this._bounds.y = bounds.y === undefined ? this._bounds.y : bounds.y;
                    this._bounds.width = bounds.width === undefined ? this._bounds.width : bounds.width;
                    this._bounds.height = bounds.height === undefined ? this._bounds.height : bounds.height;
                    this.layout();
                };
                Control.prototype.setBoundsValues = function (x, y, w, h) {
                    this.setBounds({ x: x, y: y, width: w, height: h });
                };
                Control.prototype.getBounds = function () {
                    return this._bounds;
                };
                Control.prototype.setLocation = function (x, y) {
                    this._element.style.left = x + "px";
                    this._element.style.top = y + "px";
                    this._bounds.x = x;
                    this._bounds.y = y;
                };
                Control.prototype.layout = function () {
                    setElementBounds(this._element, this._bounds);
                    if (this._layout) {
                        this._layout.layout(this);
                    }
                    else {
                        for (var _i = 0, _a = this._children; _i < _a.length; _i++) {
                            var child = _a[_i];
                            child.layout();
                        }
                    }
                };
                Control.prototype.add = function (control) {
                    this._children.push(control);
                    this._element.appendChild(control.getElement());
                };
                Control.prototype.getChildren = function () {
                    return this._children;
                };
                return Control;
            }());
            controls.Control = Control;
            var PanelTitle = /** @class */ (function (_super) {
                __extends(PanelTitle, _super);
                function PanelTitle() {
                    var _this = _super.call(this) || this;
                    _this.getElement().classList.add("panelTitle");
                    _this._textControl = new Control();
                    _this.add(_this._textControl);
                    _this._toolbar = new PanelToolbar();
                    _this.add(_this._toolbar);
                    return _this;
                }
                PanelTitle.prototype.setText = function (text) {
                    this._textControl.getElement().innerHTML = text;
                };
                PanelTitle.prototype.getToolbar = function () {
                    return this._toolbar;
                };
                PanelTitle.prototype.layout = function () {
                    _super.prototype.layout.call(this);
                    var b = this.getBounds();
                    var elem = this._textControl.getElement();
                    elem.style.top = FONT_OFFSET + "px";
                    elem.style.left = FONT_OFFSET * 2 + "px";
                    var toolbarWidth = this._toolbar.getActions().length * ACTION_WIDTH;
                    this._toolbar.setBoundsValues(b.width - toolbarWidth, 0, toolbarWidth, ROW_HEIGHT);
                };
                return PanelTitle;
            }(Control));
            var Action = /** @class */ (function () {
                function Action() {
                }
                return Action;
            }());
            controls.Action = Action;
            var ActionButton = /** @class */ (function (_super) {
                __extends(ActionButton, _super);
                function ActionButton(action) {
                    var _this = _super.call(this, "button") || this;
                    _this._action = action;
                    _this.getElement().classList.add("actionButton");
                    return _this;
                }
                ActionButton.prototype.getAction = function () {
                    return this._action;
                };
                return ActionButton;
            }(Control));
            controls.ActionButton = ActionButton;
            var PanelToolbar = /** @class */ (function (_super) {
                __extends(PanelToolbar, _super);
                function PanelToolbar() {
                    var _this = _super.call(this) || this;
                    _this._actions = [];
                    _this._buttons = [];
                    _this.getElement().classList.add("panelToolbar");
                    return _this;
                }
                PanelToolbar.prototype.addAction = function (action) {
                    this._actions.push(action);
                    var b = new ActionButton(action);
                    this._buttons.push(b);
                    this.add(b);
                };
                PanelToolbar.prototype.getActions = function () {
                    return this._actions;
                };
                PanelToolbar.prototype.layout = function () {
                    _super.prototype.layout.call(this);
                    var b = this.getBounds();
                    for (var i = 0; i < this._buttons.length; i++) {
                        var btn = this._buttons[i];
                        btn.setBoundsValues(i * ACTION_WIDTH, 0, ACTION_WIDTH, b.height);
                    }
                };
                return PanelToolbar;
            }(Control));
            var Panel = /** @class */ (function (_super) {
                __extends(Panel, _super);
                function Panel(hasTitle) {
                    if (hasTitle === void 0) { hasTitle = true; }
                    var _this = _super.call(this) || this;
                    _this._cornerElements = [null, null, null, null];
                    _this.getElement().classList.add("panel");
                    for (var i = 0; i < 4; i++) {
                        var elem = document.createElement("div");
                        elem.classList.add("panelCorner");
                        _this.getElement().appendChild(elem);
                        _this._cornerElements[i] = elem;
                    }
                    if (hasTitle) {
                        _this._panelTitle = new PanelTitle();
                        _this.add(_this._panelTitle);
                    }
                    _this._clientArea = new Control("div");
                    _this._clientArea.addClass("panelClientArea");
                    _this.add(_this._clientArea);
                    return _this;
                }
                Panel.prototype.setTitle = function (title) {
                    this._title = title;
                    this._panelTitle.setText(title);
                };
                Panel.prototype.getTitle = function () {
                    return this._title;
                };
                Panel.prototype.getToolbar = function () {
                    return this._panelTitle.getToolbar();
                };
                Panel.prototype.getClientArea = function () {
                    return this._clientArea;
                };
                Panel.prototype.layout = function () {
                    //super.layout();
                    setElementBounds(this.getElement(), this.getBounds());
                    var b = this.getBounds();
                    var cornerSize = ROW_HEIGHT;
                    setElementBounds(this._cornerElements[0], {
                        x: 0,
                        y: 0,
                        width: cornerSize,
                        height: cornerSize
                    });
                    setElementBounds(this._cornerElements[1], {
                        x: b.width - cornerSize,
                        y: 0,
                        width: cornerSize,
                        height: cornerSize
                    });
                    setElementBounds(this._cornerElements[2], {
                        x: b.width - cornerSize,
                        y: b.height - cornerSize,
                        width: cornerSize,
                        height: cornerSize
                    });
                    setElementBounds(this._cornerElements[3], {
                        x: 0,
                        y: b.height - cornerSize,
                        width: cornerSize,
                        height: cornerSize
                    });
                    if (this._panelTitle) {
                        this._panelTitle.setBoundsValues(PANEL_BORDER_SIZE, PANEL_BORDER_SIZE, b.width - PANEL_BORDER_SIZE * 2, ROW_HEIGHT);
                        this._clientArea.setBounds({
                            x: PANEL_BORDER_SIZE,
                            y: PANEL_BORDER_SIZE + ROW_HEIGHT,
                            width: b.width - PANEL_BORDER_SIZE * 2,
                            height: b.height - PANEL_BORDER_SIZE * 2 - ROW_HEIGHT
                        });
                    }
                    else {
                        this._clientArea.setBounds({
                            x: PANEL_BORDER_SIZE,
                            y: PANEL_BORDER_SIZE,
                            width: b.width - PANEL_BORDER_SIZE * 2,
                            height: b.height - PANEL_BORDER_SIZE * 2
                        });
                    }
                };
                return Panel;
            }(Control));
            controls.Panel = Panel;
            var SplitPanel = /** @class */ (function (_super) {
                __extends(SplitPanel, _super);
                function SplitPanel(left, right, horizontal) {
                    if (horizontal === void 0) { horizontal = true; }
                    var _this = _super.call(this) || this;
                    _this._startDrag = -1;
                    _this.getElement().classList.add("split");
                    _this._horizontal = horizontal;
                    _this._splitPosition = 50;
                    _this._splitFactor = 0.5;
                    _this._splitWidth = 2;
                    var l1 = function (e) { return _this.onMouseLeave(e); };
                    var l2 = function (e) { return _this.onMouseDown(e); };
                    var l3 = function (e) { return _this.onMouseUp(e); };
                    var l4 = function (e) { return _this.onMouseMove(e); };
                    var l5 = function (e) {
                        if (!_this.getElement().isConnected) {
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
                        _this.setLeftControl(left);
                    }
                    if (right) {
                        _this.setRightControl(right);
                    }
                    return _this;
                }
                SplitPanel.prototype.onMouseDown = function (e) {
                    var pos = this.getControlPosition(e.x, e.y);
                    var offset = this._horizontal ? pos.x : pos.y;
                    var inside = Math.abs(offset - this._splitPosition) <= SPLIT_OVER_ZONE_WIDTH && this.containsLocalPoint(pos.x, pos.y);
                    if (inside) {
                        e.preventDefault();
                        this._startDrag = this._horizontal ? e.x : e.y;
                        this._startPos = this._splitPosition;
                    }
                };
                SplitPanel.prototype.onMouseUp = function (e) {
                    this._startDrag = -1;
                };
                SplitPanel.prototype.onMouseMove = function (e) {
                    var pos = this.getControlPosition(e.x, e.y);
                    var offset = this._horizontal ? pos.x : pos.y;
                    var screen = this._horizontal ? e.x : e.y;
                    var boundsSize = this._horizontal ? this.getBounds().width : this.getBounds().height;
                    var cursorResize = this._horizontal ? "ew-resize" : "ns-resize";
                    var inside = Math.abs(offset - this._splitPosition) <= SPLIT_OVER_ZONE_WIDTH && this.containsLocalPoint(pos.x, pos.y);
                    if (inside) {
                        e.preventDefault();
                        this.getElement().style.cursor = cursorResize;
                    }
                    else {
                        this.getElement().style.cursor = "inherit";
                    }
                    if (this._startDrag !== -1) {
                        this.getElement().style.cursor = cursorResize;
                        var newPos = this._startPos + screen - this._startDrag;
                        if (newPos > 100 && boundsSize - newPos > 100) {
                            this._splitPosition = newPos;
                            this._splitFactor = this._splitPosition / boundsSize;
                            this.layout();
                        }
                    }
                };
                SplitPanel.prototype.onMouseLeave = function (e) {
                    this.getElement().style.cursor = "inherit";
                    this._startDrag = -1;
                };
                SplitPanel.prototype.setHorizontal = function (horizontal) {
                    if (horizontal === void 0) { horizontal = true; }
                    this._horizontal = horizontal;
                };
                SplitPanel.prototype.setVertical = function (vertical) {
                    if (vertical === void 0) { vertical = true; }
                    this._horizontal = !vertical;
                };
                SplitPanel.prototype.getSplitFactor = function () {
                    return this._splitFactor;
                };
                SplitPanel.prototype.getSize = function () {
                    var b = this.getBounds();
                    return this._horizontal ? b.width : b.height;
                };
                SplitPanel.prototype.setSplitFactor = function (factor) {
                    this._splitFactor = Math.min(Math.max(0, factor), 1);
                    this._splitPosition = this.getSize() * this._splitFactor;
                };
                SplitPanel.prototype.setLeftControl = function (control) {
                    this._leftControl = control;
                    this.add(control);
                };
                SplitPanel.prototype.getLeftControl = function () {
                    return this._leftControl;
                };
                SplitPanel.prototype.setRightControl = function (control) {
                    this._rightControl = control;
                    this.add(control);
                };
                SplitPanel.prototype.getRightControl = function () {
                    return this._rightControl;
                };
                SplitPanel.prototype.layout = function () {
                    setElementBounds(this.getElement(), this.getBounds());
                    if (!this._leftControl || !this._rightControl) {
                        return;
                    }
                    this.setSplitFactor(this._splitFactor);
                    var pos = this._splitPosition;
                    var sw = this._splitWidth;
                    var b = this.getBounds();
                    if (this._horizontal) {
                        this._leftControl.setBoundsValues(0, 0, pos - sw, b.height);
                        this._rightControl.setBoundsValues(pos + sw, 0, b.width - pos - sw, b.height);
                    }
                    else {
                        this._leftControl.setBoundsValues(0, 0, b.width, pos - sw);
                        this._rightControl.setBoundsValues(0, pos + sw, b.width, b.height - pos - sw);
                    }
                };
                return SplitPanel;
            }(Control));
            controls.SplitPanel = SplitPanel;
            var PaddingPane = /** @class */ (function (_super) {
                __extends(PaddingPane, _super);
                function PaddingPane(control, padding) {
                    if (padding === void 0) { padding = 5; }
                    var _this = _super.call(this) || this;
                    _this._padding = padding;
                    _this.getElement().classList.add("paddingPane");
                    _this.setControl(control);
                    return _this;
                }
                PaddingPane.prototype.setControl = function (control) {
                    this._control = control;
                    if (this._control) {
                        this.add(control);
                    }
                };
                PaddingPane.prototype.getControl = function () {
                    return this._control;
                };
                PaddingPane.prototype.setPadding = function (padding) {
                    this._padding = padding;
                };
                PaddingPane.prototype.getPadding = function () {
                    return this._padding;
                };
                PaddingPane.prototype.layout = function () {
                    var b = this.getBounds();
                    setElementBounds(this.getElement(), b);
                    if (this._control) {
                        this._control.setBoundsValues(this._padding, this._padding, b.width - this._padding * 2, b.height - this._padding * 2);
                    }
                };
                return PaddingPane;
            }(Control));
            controls.PaddingPane = PaddingPane;
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
                var RenderCellArgs = /** @class */ (function () {
                    function RenderCellArgs(canvasContext, x, y, obj, view) {
                        this.canvasContext = canvasContext;
                        this.x = x;
                        this.y = y;
                        this.obj = obj;
                        this.view = view;
                    }
                    return RenderCellArgs;
                }());
                viewers.RenderCellArgs = RenderCellArgs;
                ;
                var LabelCellRenderer = /** @class */ (function () {
                    function LabelCellRenderer() {
                    }
                    LabelCellRenderer.prototype.renderCell = function (args) {
                        var label = this.getLabel(args.obj);
                        var img = this.getImage(args.obj);
                        var x = args.x;
                        var ctx = args.canvasContext;
                        ctx.fillStyle = "#000";
                        if (img) {
                            var h = this.cellHeight(args);
                            img.paint(ctx, x, args.y, 16, h);
                            x += 20;
                        }
                        ctx.save();
                        if (args.view.isSelected(args.obj)) {
                            ctx.fillStyle = controls.Controls.theme.treeItemSelectionForeground;
                        }
                        ctx.fillText(label, x, args.y + 15);
                        ctx.restore();
                    };
                    LabelCellRenderer.prototype.cellHeight = function (args) {
                        return 20;
                    };
                    return LabelCellRenderer;
                }());
                viewers.LabelCellRenderer = LabelCellRenderer;
                var Rect = /** @class */ (function () {
                    function Rect(x, y, w, h) {
                        if (x === void 0) { x = 0; }
                        if (y === void 0) { y = 0; }
                        if (w === void 0) { w = 0; }
                        if (h === void 0) { h = 0; }
                        this.x = x;
                        this.y = y;
                        this.w = w;
                        this.h = h;
                    }
                    Rect.prototype.set = function (x, y, w, h) {
                        this.x = x;
                        this.y = y;
                        this.w = w;
                        this.h = h;
                    };
                    Rect.prototype.contains = function (x, y) {
                        return x >= this.x && x <= this.x + this.w && y >= this.y && y <= this.y + this.h;
                    };
                    return Rect;
                }());
                viewers.Rect = Rect;
                var PaintItem = /** @class */ (function (_super) {
                    __extends(PaintItem, _super);
                    function PaintItem(index, data) {
                        var _this = _super.call(this) || this;
                        _this.index = index;
                        _this.data = data;
                        return _this;
                    }
                    return PaintItem;
                }(Rect));
                viewers.PaintItem = PaintItem;
                var Viewer = /** @class */ (function (_super) {
                    __extends(Viewer, _super);
                    function Viewer() {
                        var _this = _super.call(this, "canvas") || this;
                        _this._lastSelectedItemIndex = -1;
                        _this._cellSize = 32;
                        _this.initContext();
                        _this._input = null;
                        _this._expandedObjects = new Set();
                        _this._selectedObjects = new Set();
                        window.cc = _this;
                        _this.initListeners();
                        return _this;
                    }
                    Viewer.prototype.initListeners = function () {
                        var _this = this;
                        var canvas = this.getCanvas();
                        canvas.addEventListener("mousemove", function (e) { return _this.onMouseMove(e); });
                        canvas.addEventListener("mouseup", function (e) { return _this.onMouseUp(e); });
                        // canvas.parentElement.addEventListener("keydown", e => this.onKeyDown(e));
                    };
                    Viewer.prototype.getPaintItemAt = function (e) {
                        for (var _i = 0, _a = this._paintItems; _i < _a.length; _i++) {
                            var item = _a[_i];
                            if (item.contains(e.offsetX, e.offsetY)) {
                                return item;
                            }
                        }
                        return null;
                    };
                    Viewer.prototype.fireSelectionChanged = function () {
                    };
                    //TODO: is not fired, I am looking the reason.
                    Viewer.prototype.onKeyDown = function (e) {
                        if (e.key === "Escape") {
                            if (this._selectedObjects.size > 0) {
                                this._selectedObjects.clear();
                                this.repaint();
                                this.fireSelectionChanged();
                            }
                        }
                    };
                    Viewer.prototype.onMouseUp = function (e) {
                        if (e.button !== 0) {
                            return;
                        }
                        var item = this.getPaintItemAt(e);
                        if (item === null) {
                            return;
                        }
                        var selChanged = false;
                        var data = item.data;
                        if (e.ctrlKey || e.metaKey) {
                            this._selectedObjects.add(data);
                            selChanged = true;
                        }
                        else if (e.shiftKey) {
                            if (this._lastSelectedItemIndex >= 0 && this._lastSelectedItemIndex != item.index) {
                                var start = Math.min(this._lastSelectedItemIndex, item.index);
                                var end = Math.max(this._lastSelectedItemIndex, item.index);
                                for (var i = start; i <= end; i++) {
                                    this._selectedObjects.add(this._paintItems[i].data);
                                }
                                selChanged = true;
                            }
                        }
                        else {
                            this._selectedObjects.clear();
                            this._selectedObjects.add(data);
                            selChanged = true;
                        }
                        if (selChanged) {
                            this.repaint();
                            this.fireSelectionChanged();
                            this._lastSelectedItemIndex = item.index;
                        }
                    };
                    Viewer.prototype.onMouseMove = function (e) {
                        if (e.buttons !== 0) {
                            return;
                        }
                        var item = this.getPaintItemAt(e);
                        var over = item === null ? null : item.data;
                        if (over !== this._overObject) {
                            this._overObject = over;
                            this.repaint();
                        }
                    };
                    Viewer.prototype.getOverObject = function () {
                        return this._overObject;
                    };
                    Viewer.prototype.initContext = function () {
                        this._context = this.getCanvas().getContext("2d");
                        this._context.imageSmoothingEnabled = false;
                        controls.Controls.disableCanvasSmoothing(this._context);
                        this._context.font = "14px sans-serif";
                        this._context.fillStyle = "red";
                        this._context.fillText("hello", 10, 100);
                    };
                    Viewer.prototype.setExpanded = function (obj, expanded) {
                        if (expanded) {
                            this._expandedObjects.add(obj);
                        }
                        else {
                            this._expandedObjects["delete"](obj);
                        }
                    };
                    Viewer.prototype.isExpanded = function (obj) {
                        return this._expandedObjects.has(obj);
                    };
                    Viewer.prototype.isCollapsed = function (obj) {
                        return !this.isExpanded(obj);
                    };
                    Viewer.prototype.isSelected = function (obj) {
                        return this._selectedObjects.has(obj);
                    };
                    Viewer.prototype.paintTreeHandler = function (x, y, collapsed) {
                        if (collapsed) {
                            this._context.strokeStyle = "#000";
                            this._context.strokeRect(x, y, 16, 16);
                        }
                        else {
                            this._context.fillStyle = "#000";
                            this._context.fillRect(x, y, 16, 16);
                        }
                    };
                    Viewer.prototype.repaint = function () {
                        this._paintItems = [];
                        var canvas = this.getCanvas();
                        this._context.clearRect(0, 0, canvas.width, canvas.height);
                        if (this._cellRendererProvider && this._contentProvider && this._input !== null) {
                            this.paint();
                        }
                    };
                    Viewer.prototype.paintItemBackground = function (obj, x, y, w, h) {
                        var fillStyle = null;
                        if (this.isSelected(obj)) {
                            fillStyle = controls.Controls.theme.treeItemSelectionBackground;
                            ;
                        }
                        else if (obj === this._overObject) {
                            fillStyle = controls.Controls.theme.treeItemOverBackground;
                        }
                        if (fillStyle != null) {
                            this._context.save();
                            this._context.fillStyle = fillStyle;
                            this._context.fillRect(x, y, w, h);
                            this._context.restore();
                        }
                    };
                    Viewer.prototype.layout = function () {
                        var b = this.getBounds();
                        ui.controls.setElementBounds(this.getElement(), b);
                        var canvas = this.getCanvas();
                        canvas.width = b.width;
                        canvas.height = b.height;
                        this.initContext();
                        this.repaint();
                    };
                    Viewer.prototype.getCanvas = function () {
                        return this.getElement();
                    };
                    Viewer.prototype.getCellSize = function () {
                        return this._cellSize;
                    };
                    Viewer.prototype.setCellSize = function (cellSize) {
                        this._cellSize = cellSize;
                    };
                    Viewer.prototype.getContentProvider = function () {
                        return this._contentProvider;
                    };
                    Viewer.prototype.setContentProvider = function (contentProvider) {
                        this._contentProvider = contentProvider;
                    };
                    Viewer.prototype.getCellRendererProvider = function () {
                        return this._cellRendererProvider;
                    };
                    Viewer.prototype.setCellRendererProvider = function (cellRendererProvider) {
                        this._cellRendererProvider = cellRendererProvider;
                    };
                    Viewer.prototype.getInput = function () {
                        return this._input;
                    };
                    Viewer.prototype.setInput = function (input) {
                        this._input = input;
                    };
                    return Viewer;
                }(controls.Control));
                viewers.Viewer = Viewer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./viewers.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var controls;
        (function (controls) {
            var viewers;
            (function (viewers) {
                var TREE_ICON_SIZE = 16;
                var LABEL_MARGIN = TREE_ICON_SIZE + 0;
                var TreeViewer = /** @class */ (function (_super) {
                    __extends(TreeViewer, _super);
                    function TreeViewer() {
                        var _this = _super.call(this) || this;
                        _this._treeIconList = [];
                        _this.getCanvas().addEventListener("click", function (e) { return _this.onClick(e); });
                        return _this;
                    }
                    TreeViewer.prototype.onClick = function (e) {
                        for (var _i = 0, _a = this._treeIconList; _i < _a.length; _i++) {
                            var icon = _a[_i];
                            if (icon.rect.contains(e.offsetX, e.offsetY)) {
                                this.setExpanded(icon.obj, !this.isExpanded(icon.obj));
                                this.repaint();
                                return;
                            }
                        }
                    };
                    TreeViewer.prototype.paint = function () {
                        var x = 0;
                        var y = 0;
                        this._treeIconList = [];
                        // TODO: missing taking the scroll offset to compute the non-painting area
                        var contentProvider = this.getContentProvider();
                        var roots = contentProvider.getRoots(this.getInput());
                        this.paintItems(roots, x, y);
                    };
                    TreeViewer.prototype.paintItems = function (objects, x, y) {
                        var b = this.getBounds();
                        for (var _i = 0, objects_1 = objects; _i < objects_1.length; _i++) {
                            var obj = objects_1[_i];
                            var children = this.getContentProvider().getChildren(obj);
                            var expanded = this.isExpanded(obj);
                            var renderer = this.getCellRendererProvider().getCellRenderer(obj);
                            var args = new viewers.RenderCellArgs(this._context, x + LABEL_MARGIN, y, obj, this);
                            var cellHeight = renderer.cellHeight(args);
                            _super.prototype.paintItemBackground.call(this, obj, 0, y, b.width, cellHeight);
                            if (y > -this.getCellSize() && y < b.height /* + scrollOffset */) {
                                // render tree icon
                                if (children.length > 0) {
                                    var iconY = y + (cellHeight - TREE_ICON_SIZE) / 2;
                                    var icon = controls.Controls.getIcon(expanded ? controls.Controls.ICON_TREE_COLLAPSE : controls.Controls.ICON_TREE_EXPAND);
                                    icon.paint(this._context, x, iconY);
                                    this._treeIconList.push({
                                        rect: new viewers.Rect(x, iconY, TREE_ICON_SIZE, TREE_ICON_SIZE),
                                        obj: obj
                                    });
                                }
                                // client render cell
                                renderer.renderCell(args);
                                var item = new viewers.PaintItem(this._paintItems.length, obj);
                                item.set(args.x, args.y, b.width, cellHeight);
                                this._paintItems.push(item);
                            }
                            y += cellHeight;
                            if (expanded) {
                                y = this.paintItems(children, x + LABEL_MARGIN, y);
                            }
                        }
                        return y;
                    };
                    TreeViewer.prototype.getContentProvider = function () {
                        return _super.prototype.getContentProvider.call(this);
                    };
                    TreeViewer.prototype.setContentProvider = function (contentProvider) {
                        _super.prototype.setContentProvider.call(this, contentProvider);
                    };
                    return TreeViewer;
                }(viewers.Viewer));
                viewers.TreeViewer = TreeViewer;
            })(viewers = controls.viewers || (controls.viewers = {}));
        })(controls = ui.controls || (ui.controls = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var parts;
        (function (parts) {
            var Part = /** @class */ (function (_super) {
                __extends(Part, _super);
                function Part(id) {
                    var _this = _super.call(this) || this;
                    _this._id = id;
                    _this.getElement().classList.add("part");
                    return _this;
                }
                Part.prototype.getId = function () {
                    return this._id;
                };
                return Part;
            }(ui.controls.Panel));
            parts.Part = Part;
            var ViewPart = /** @class */ (function (_super) {
                __extends(ViewPart, _super);
                function ViewPart(id) {
                    var _this = _super.call(this, id) || this;
                    _this.getElement().classList.add("view");
                    return _this;
                }
                return ViewPart;
            }(Part));
            parts.ViewPart = ViewPart;
            var EditorArea = /** @class */ (function (_super) {
                __extends(EditorArea, _super);
                function EditorArea() {
                    return _super.call(this, "editorArea") || this;
                }
                return EditorArea;
            }(Part));
            parts.EditorArea = EditorArea;
        })(parts = ui.parts || (ui.parts = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./parts.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var blocks;
        (function (blocks) {
            var BlocksView = /** @class */ (function (_super) {
                __extends(BlocksView, _super);
                function BlocksView() {
                    var _this = _super.call(this, "blocksView") || this;
                    _this.setTitle("Blocks");
                    return _this;
                }
                return BlocksView;
            }(ui.parts.ViewPart));
            blocks.BlocksView = BlocksView;
        })(blocks = ui.blocks || (ui.blocks = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./parts.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var files;
        (function (files) {
            var viewers = phasereditor2d.ui.controls.viewers;
            var FileTreeContentProvider = /** @class */ (function () {
                function FileTreeContentProvider() {
                }
                FileTreeContentProvider.prototype.getRoots = function (input) {
                    return this.getChildren(input);
                };
                FileTreeContentProvider.prototype.getChildren = function (parent) {
                    return parent.getFiles();
                };
                return FileTreeContentProvider;
            }());
            var FileCellRenderer = /** @class */ (function (_super) {
                __extends(FileCellRenderer, _super);
                function FileCellRenderer() {
                    return _super !== null && _super.apply(this, arguments) || this;
                }
                FileCellRenderer.prototype.getLabel = function (obj) {
                    return obj.getName();
                };
                FileCellRenderer.prototype.getImage = function (obj) {
                    var file = obj;
                    if (file.isFile()) {
                        var type = file.getContentType();
                        var icon = ui.Workbench.getWorkbench().getContentTypeIcon(type);
                        if (icon) {
                            return icon;
                        }
                    }
                    else {
                        return ui.controls.Controls.getIcon(ui.controls.Controls.ICON_FOLDER);
                    }
                    return ui.controls.Controls.getIcon(ui.controls.Controls.ICON_FILE);
                };
                return FileCellRenderer;
            }(viewers.LabelCellRenderer));
            var FileCellRendererProvider = /** @class */ (function () {
                function FileCellRendererProvider() {
                }
                FileCellRendererProvider.prototype.getCellRenderer = function (element) {
                    return new FileCellRenderer();
                };
                return FileCellRendererProvider;
            }());
            var FilesView = /** @class */ (function (_super) {
                __extends(FilesView, _super);
                function FilesView() {
                    var _this = _super.call(this, "filesView") || this;
                    _this.setTitle("Files");
                    var root = new phasereditor2d.core.io.FilePath(null, TEST_DATA);
                    //console.log(root.toStringTree());
                    var tree = new viewers.TreeViewer();
                    tree.setContentProvider(new FileTreeContentProvider());
                    tree.setCellRendererProvider(new FileCellRendererProvider());
                    tree.setInput(root);
                    _this.getClientArea().setLayout(new ui.controls.FillLayout());
                    _this.getClientArea().add(tree);
                    tree.repaint();
                    return _this;
                }
                return FilesView;
            }(ui.parts.ViewPart));
            files.FilesView = FilesView;
            var TEST_DATA = {
                "name": "",
                "isFile": false,
                "children": [
                    {
                        "name": ".gitignore",
                        "isFile": true,
                        "contentType": "any"
                    },
                    {
                        "name": "COPYRIGHTS",
                        "isFile": true,
                        "contentType": "any"
                    },
                    {
                        "name": "assets",
                        "isFile": false,
                        "children": [
                            {
                                "name": "animations.json",
                                "isFile": true,
                                "contentType": "json"
                            },
                            {
                                "name": "atlas",
                                "isFile": false,
                                "children": [
                                    {
                                        "name": ".DS_Store",
                                        "isFile": true,
                                        "contentType": "any"
                                    },
                                    {
                                        "name": "atlas-props.json",
                                        "isFile": true,
                                        "contentType": "json"
                                    },
                                    {
                                        "name": "atlas-props.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "atlas.json",
                                        "isFile": true,
                                        "contentType": "json"
                                    },
                                    {
                                        "name": "atlas.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "hello.atlas",
                                        "isFile": true,
                                        "contentType": "any"
                                    },
                                    {
                                        "name": "hello.json",
                                        "isFile": true,
                                        "contentType": "json"
                                    },
                                    {
                                        "name": "hello.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    }
                                ]
                            },
                            {
                                "name": "environment",
                                "isFile": false,
                                "children": [
                                    {
                                        "name": ".DS_Store",
                                        "isFile": true,
                                        "contentType": "any"
                                    },
                                    {
                                        "name": "bg-clouds.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "bg-mountains.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "bg-trees.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "tileset.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    }
                                ]
                            },
                            {
                                "name": "fonts",
                                "isFile": false,
                                "children": [
                                    {
                                        "name": "arcade.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "arcade.xml",
                                        "isFile": true,
                                        "contentType": "any"
                                    },
                                    {
                                        "name": "atari-classic.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "atari-classic.xml",
                                        "isFile": true,
                                        "contentType": "any"
                                    }
                                ]
                            },
                            {
                                "name": "html",
                                "isFile": false,
                                "children": [
                                    {
                                        "name": "hello.html",
                                        "isFile": true,
                                        "contentType": "any"
                                    }
                                ]
                            },
                            {
                                "name": "levels-pack-1.json",
                                "isFile": true,
                                "contentType": "json"
                            },
                            {
                                "name": "maps",
                                "isFile": false,
                                "children": [
                                    {
                                        "name": ".DS_Store",
                                        "isFile": true,
                                        "contentType": "any"
                                    },
                                    {
                                        "name": "map.json",
                                        "isFile": true,
                                        "contentType": "json"
                                    }
                                ]
                            },
                            {
                                "name": "scenes",
                                "isFile": false,
                                "children": [
                                    {
                                        "name": "Acorn.js",
                                        "isFile": true,
                                        "contentType": "js"
                                    },
                                    {
                                        "name": "Ant.js",
                                        "isFile": true,
                                        "contentType": "js"
                                    },
                                    {
                                        "name": "EnemyDeath.js",
                                        "isFile": true,
                                        "contentType": "js"
                                    },
                                    {
                                        "name": "GameOver.js",
                                        "isFile": true,
                                        "contentType": "js"
                                    },
                                    {
                                        "name": "GameOver.scene",
                                        "isFile": true,
                                        "contentType": "phasereditor2d.scene"
                                    },
                                    {
                                        "name": "Gator.js",
                                        "isFile": true,
                                        "contentType": "js"
                                    },
                                    {
                                        "name": "Grasshopper.js",
                                        "isFile": true,
                                        "contentType": "js"
                                    },
                                    {
                                        "name": "Level.js",
                                        "isFile": true,
                                        "contentType": "js"
                                    },
                                    {
                                        "name": "Level.scene",
                                        "isFile": true,
                                        "contentType": "phasereditor2d.scene"
                                    },
                                    {
                                        "name": "Player.js",
                                        "isFile": true,
                                        "contentType": "js"
                                    },
                                    {
                                        "name": "Preload.js",
                                        "isFile": true,
                                        "contentType": "js"
                                    },
                                    {
                                        "name": "Preload.scene",
                                        "isFile": true,
                                        "contentType": "phasereditor2d.scene"
                                    },
                                    {
                                        "name": "TitleScreen.js",
                                        "isFile": true,
                                        "contentType": "js"
                                    },
                                    {
                                        "name": "TitleScreen.scene",
                                        "isFile": true,
                                        "contentType": "phasereditor2d.scene"
                                    }
                                ]
                            },
                            {
                                "name": "sounds",
                                "isFile": false,
                                "children": [
                                    {
                                        "name": ".DS_Store",
                                        "isFile": true,
                                        "contentType": "any"
                                    },
                                    {
                                        "name": "enemy-death.ogg",
                                        "isFile": true,
                                        "contentType": "sound"
                                    },
                                    {
                                        "name": "hurt.ogg",
                                        "isFile": true,
                                        "contentType": "sound"
                                    },
                                    {
                                        "name": "item.ogg",
                                        "isFile": true,
                                        "contentType": "sound"
                                    },
                                    {
                                        "name": "jump.ogg",
                                        "isFile": true,
                                        "contentType": "sound"
                                    },
                                    {
                                        "name": "music-credits.txt",
                                        "isFile": true,
                                        "contentType": "txt"
                                    },
                                    {
                                        "name": "the_valley.ogg",
                                        "isFile": true,
                                        "contentType": "sound"
                                    }
                                ]
                            },
                            {
                                "name": "sprites",
                                "isFile": false,
                                "children": [
                                    {
                                        "name": ".DS_Store",
                                        "isFile": true,
                                        "contentType": "any"
                                    },
                                    {
                                        "name": "credits-text.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "game-over.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "instructions.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "loading.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "press-enter-text.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    },
                                    {
                                        "name": "title-screen.png",
                                        "isFile": true,
                                        "contentType": "img"
                                    }
                                ]
                            },
                            {
                                "name": "svg",
                                "isFile": false,
                                "children": [
                                    {
                                        "name": "demo.svg",
                                        "isFile": true,
                                        "contentType": "img"
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "name": "data.json",
                        "isFile": true,
                        "contentType": "json"
                    },
                    {
                        "name": "fake-assets",
                        "isFile": false,
                        "children": [
                            {
                                "name": "Collisions Layer.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "Main Layer.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "fake-pack.json",
                                "isFile": true,
                                "contentType": "json"
                            }
                        ]
                    },
                    {
                        "name": "index.html",
                        "isFile": true,
                        "contentType": "any"
                    },
                    {
                        "name": "jsconfig.json",
                        "isFile": true,
                        "contentType": "json"
                    },
                    {
                        "name": "lib",
                        "isFile": false,
                        "children": [
                            {
                                "name": "phaser.js",
                                "isFile": true,
                                "contentType": "js"
                            }
                        ]
                    },
                    {
                        "name": "main.js",
                        "isFile": true,
                        "contentType": "js"
                    },
                    {
                        "name": "typings",
                        "isFile": false,
                        "children": [
                            {
                                "name": "phaser.d.ts",
                                "isFile": true,
                                "contentType": "ts"
                            }
                        ]
                    }
                ]
            };
        })(files = ui.files || (ui.files = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./parts.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var inspector;
        (function (inspector) {
            var InspectorView = /** @class */ (function (_super) {
                __extends(InspectorView, _super);
                function InspectorView() {
                    var _this = _super.call(this, "inspectorView") || this;
                    _this.setTitle("Inspector");
                    return _this;
                }
                return InspectorView;
            }(ui.parts.ViewPart));
            inspector.InspectorView = InspectorView;
        })(inspector = ui.inspector || (ui.inspector = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="./parts.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var outline;
        (function (outline) {
            var OutlineView = /** @class */ (function (_super) {
                __extends(OutlineView, _super);
                function OutlineView() {
                    var _this = _super.call(this, "outlineView") || this;
                    _this.setTitle("Outline");
                    return _this;
                }
                return OutlineView;
            }(ui.parts.ViewPart));
            outline.OutlineView = OutlineView;
        })(outline = ui.outline || (ui.outline = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var toolbar;
        (function (toolbar) {
            var Toolbar = /** @class */ (function () {
                function Toolbar() {
                    this._toolbarElement = document.createElement("div");
                    this._toolbarElement.innerHTML = "\n\n            <button>Load</button>\n            <button>Play</button>\n\n            ";
                    this._toolbarElement.classList.add("toolbar");
                    document.getElementsByTagName("body")[0].appendChild(this._toolbarElement);
                }
                Toolbar.prototype.getElement = function () {
                    return this._toolbarElement;
                };
                return Toolbar;
            }());
            toolbar.Toolbar = Toolbar;
        })(toolbar = ui.toolbar || (ui.toolbar = {}));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
/// <reference path="../controls/controls.ts"/>
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var Workbench = /** @class */ (function () {
            function Workbench() {
                this._contentType_icon_Map = new Map();
                this._contentType_icon_Map.set("img", ui.controls.Controls.getIcon(ui.controls.Controls.ICON_FILE_IMAGE));
                this._contentType_icon_Map.set("sound", ui.controls.Controls.getIcon(ui.controls.Controls.ICON_FILE_SOUND));
                this._contentType_icon_Map.set("video", ui.controls.Controls.getIcon(ui.controls.Controls.ICON_FILE_VIDEO));
                this._contentType_icon_Map.set("js", ui.controls.Controls.getIcon(ui.controls.Controls.ICON_FILE_SCRIPT));
                this._contentType_icon_Map.set("ts", ui.controls.Controls.getIcon(ui.controls.Controls.ICON_FILE_SCRIPT));
                this._contentType_icon_Map.set("json", ui.controls.Controls.getIcon(ui.controls.Controls.ICON_FILE_SCRIPT));
                this._contentType_icon_Map.set("txt", ui.controls.Controls.getIcon(ui.controls.Controls.ICON_FILE_TEXT));
            }
            Workbench.getWorkbench = function () {
                if (!Workbench._workbench) {
                    Workbench._workbench = new Workbench();
                }
                return this._workbench;
            };
            Workbench.prototype.start = function () {
                this._designWindow = new DesignWindow();
                document.getElementById("body").appendChild(this._designWindow.getElement());
            };
            Workbench.prototype.getContentTypeIcon = function (contentType) {
                if (this._contentType_icon_Map.has(contentType)) {
                    return this._contentType_icon_Map.get(contentType);
                }
                return null;
            };
            return Workbench;
        }());
        ui.Workbench = Workbench;
        var DesignWindow = /** @class */ (function (_super) {
            __extends(DesignWindow, _super);
            function DesignWindow() {
                var _this = _super.call(this) || this;
                _this._toolbar = new ui.toolbar.Toolbar();
                _this._outlineView = new ui.outline.OutlineView();
                _this._filesView = new ui.files.FilesView();
                _this._inspectorView = new ui.inspector.InspectorView();
                _this._blocksView = new ui.blocks.BlocksView();
                _this._editorArea = new ui.parts.EditorArea();
                _this._split_Files_Blocks = new ui.controls.SplitPanel(_this._filesView, _this._blocksView);
                _this._split_Editor_FilesBlocks = new ui.controls.SplitPanel(_this._editorArea, _this._split_Files_Blocks, false);
                _this._split_Outline_EditorFilesBlocks = new ui.controls.SplitPanel(_this._outlineView, _this._split_Editor_FilesBlocks);
                _this._split_OutlineEditorFilesBlocks_Inspector = new ui.controls.SplitPanel(_this._split_Outline_EditorFilesBlocks, _this._inspectorView);
                _this.setControl(_this._split_OutlineEditorFilesBlocks_Inspector);
                window.addEventListener("resize", function (e) {
                    _this.setBoundsValues(0, 0, window.innerWidth, window.innerHeight);
                });
                _this.initialLayout();
                return _this;
            }
            DesignWindow.prototype.initialLayout = function () {
                var b = { x: 0, y: 0, width: window.innerWidth, height: window.innerHeight };
                this._split_Files_Blocks.setSplitFactor(0.2);
                this._split_Editor_FilesBlocks.setSplitFactor(0.6);
                this._split_Outline_EditorFilesBlocks.setSplitFactor(0.15);
                this._split_OutlineEditorFilesBlocks_Inspector.setSplitFactor(0.8);
                this.setBounds(b);
            };
            DesignWindow.prototype.getOutlineView = function () {
                return this._outlineView;
            };
            DesignWindow.prototype.getFilesView = function () {
                return this._filesView;
            };
            DesignWindow.prototype.getBlocksView = function () {
                return this._blocksView;
            };
            DesignWindow.prototype.getInspectorView = function () {
                return this._inspectorView;
            };
            DesignWindow.prototype.getEditorArea = function () {
                return this._editorArea;
            };
            return DesignWindow;
        }(ui.controls.PaddingPane));
    })(ui = phasereditor2d.ui || (phasereditor2d.ui = {}));
})(phasereditor2d || (phasereditor2d = {}));
