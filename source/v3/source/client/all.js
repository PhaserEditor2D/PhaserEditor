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
var phasereditor2d;
(function (phasereditor2d) {
    var demo;
    (function (demo) {
        function main() {
            console.log("Booting!!!");
            phasereditor2d.ui.Workbench.getWorkbench();
        }
        demo.main = main;
    })(demo = phasereditor2d.demo || (phasereditor2d.demo = {}));
})(phasereditor2d || (phasereditor2d = {}));
window.addEventListener("load", function () {
    phasereditor2d.demo.main();
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
                    if (fileData.children) {
                        this._files = [];
                        for (var _i = 0, _a = fileData.children; _i < _a.length; _i++) {
                            var child = _a[_i];
                            this._files.push(new FilePath(this, child));
                        }
                    }
                    else {
                        this._files = EMPTY_FILES;
                    }
                }
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
                        var ctx = args.canvasContext;
                        ctx.fillStyle = "#000";
                        ctx.fillText(label, args.x, args.y + 13);
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
                    Rect.prototype.contains = function (x, y) {
                        return x >= this.x && x <= this.x + this.w && y >= this.y && y <= this.y + this.h;
                    };
                    return Rect;
                }());
                viewers.Rect = Rect;
                var PaintItem = /** @class */ (function (_super) {
                    __extends(PaintItem, _super);
                    function PaintItem(data) {
                        var _this = _super.call(this) || this;
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
                        _this._cellSize = 32;
                        _this._context = _this.getCanvas().getContext("2d");
                        _this._input = null;
                        _this._expandedObjects = new Set();
                        _this._selectedObjects = new Set();
                        return _this;
                    }
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
                    Viewer.prototype.paintSelectionBackground = function (x, y, w, h) {
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
                    Viewer.prototype.layout = function () {
                        var b = this.getBounds();
                        ui.controls.setElementBounds(this.getElement(), b);
                        var canvas = this.getCanvas();
                        canvas.width = b.width;
                        canvas.height = b.height;
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
                var LABEL_MARGIN = TREE_ICON_SIZE + 5;
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
                            if (y > -this.getCellSize() && y < b.height /* + scrollOffset */) {
                                if (this.isSelected(obj)) {
                                    this.paintSelectionBackground(x, y, b.width, cellHeight);
                                }
                                // render tree icon
                                if (children.length > 0) {
                                    var iconY = y + (cellHeight - TREE_ICON_SIZE) / 2;
                                    if (expanded) {
                                        this._context.strokeStyle = "#000";
                                        this._context.strokeRect(x, iconY, 16, 16);
                                    }
                                    else {
                                        this._context.fillStyle = "#000";
                                        this._context.fillRect(x, iconY, 16, 16);
                                    }
                                    this._treeIconList.push({
                                        rect: new viewers.Rect(x, iconY, TREE_ICON_SIZE, TREE_ICON_SIZE),
                                        obj: obj
                                    });
                                }
                                // client render cell
                                renderer.renderCell(args);
                            }
                            y += cellHeight;
                            if (expanded) {
                                this.paintItems(children, x + LABEL_MARGIN, y);
                            }
                        }
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
                    var root = new phasereditor2d.core.io.FilePath(null, {
                        name: "<root>",
                        isFile: false,
                        children: [
                            {
                                name: "index.html",
                                isFile: true
                            },
                            {
                                name: "assets",
                                isFile: false,
                                children: [
                                    {
                                        name: "bg.png",
                                        isFile: true
                                    }
                                ]
                            }
                        ]
                    });
                    console.log(root.toStringTree());
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
var phasereditor2d;
(function (phasereditor2d) {
    var ui;
    (function (ui) {
        var Workbench = /** @class */ (function () {
            function Workbench() {
                this._designWindow = new DesignWindow();
                document.getElementById("body").appendChild(this._designWindow.getElement());
            }
            Workbench.getWorkbench = function () {
                if (!Workbench._workbench) {
                    Workbench._workbench = new Workbench();
                }
                return this._workbench;
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
