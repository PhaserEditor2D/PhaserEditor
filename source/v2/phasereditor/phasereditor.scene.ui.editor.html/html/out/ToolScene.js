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
var PhaserEditor2D;
(function (PhaserEditor2D) {
    var PAINT_COUNT = 0;
    var ToolScene = (function (_super) {
        __extends(ToolScene, _super);
        function ToolScene() {
            var _this = _super.call(this, "ToolScene") || this;
            _this._axisToken = null;
            _this._axisLabels = [];
            _this._selectionBoxPoints = [
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0)
            ];
            _this._selectionDragStart = null;
            _this._selectionDragEnd = null;
            _this._selectedObjects = [];
            _this._selectionGraphics = null;
            _this._tools = [];
            _this._paintDelayUtils = new PhaserEditor2D.PaintDelayUtil();
            return _this;
        }
        ToolScene.prototype.create = function () {
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
        };
        ToolScene.prototype.initCamera = function () {
            this.cameras.main.setRoundPixels(true);
            this.cameras.main.setOrigin(0, 0);
        };
        ToolScene.prototype.updateFromSceneProperties = function () {
            this._axisToken = "";
            this.renderAxis();
        };
        ToolScene.prototype.renderAxis = function () {
            var editor = PhaserEditor2D.Editor.getInstance();
            var cam = editor.getObjectScene().cameras.main;
            var w = window.innerWidth;
            var h = window.innerHeight;
            var dx = 16;
            var dy = 16;
            if (PhaserEditor2D.ScenePropertiesComponent.get_snapEnabled(editor.sceneProperties)) {
                dx = PhaserEditor2D.ScenePropertiesComponent.get_snapWidth(editor.sceneProperties);
                dy = PhaserEditor2D.ScenePropertiesComponent.get_snapHeight(editor.sceneProperties);
            }
            var i = 1;
            while (dx * i * cam.zoom < 32) {
                i++;
            }
            dx = dx * i;
            i = 1;
            while (dy * i * cam.zoom < 32) {
                i++;
            }
            dy = dy * i;
            var sx = ((cam.scrollX / dx) | 0) * dx;
            var sy = ((cam.scrollY / dy) | 0) * dy;
            var bx = PhaserEditor2D.ScenePropertiesComponent.get_borderX(editor.sceneProperties);
            var by = PhaserEditor2D.ScenePropertiesComponent.get_borderY(editor.sceneProperties);
            var bw = PhaserEditor2D.ScenePropertiesComponent.get_borderWidth(editor.sceneProperties);
            var bh = PhaserEditor2D.ScenePropertiesComponent.get_borderHeight(editor.sceneProperties);
            var token = w + "-" + h + "-" + dx + "-" + dy + "-" + cam.zoom + "-" + cam.scrollX + "-" + cam.scrollY
                + "-" + bx + "-" + by + "-" + bw + "-" + bh;
            if (this._axisToken !== null && this._axisToken === token) {
                return;
            }
            this._axisToken = token;
            this._gridGraphics.clear();
            var fg = Phaser.Display.Color.RGBStringToColor("rgb(" + PhaserEditor2D.ScenePropertiesComponent.get_foregroundColor(editor.sceneProperties) + ")");
            this._gridGraphics.lineStyle(1, fg.color, 0.5);
            for (var _i = 0, _a = this._axisLabels; _i < _a.length; _i++) {
                var label_1 = _a[_i];
                label_1.destroy();
            }
            var label = null;
            var labelHeight = 0;
            this._axisLabels = [];
            for (var x = sx;; x += dx) {
                var x2 = (x - cam.scrollX) * cam.zoom;
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
            var labelWidth = 0;
            for (var y = sy;; y += dy) {
                var y2 = (y - cam.scrollY) * cam.zoom;
                if (y2 > h) {
                    break;
                }
                if (y2 < labelHeight) {
                    continue;
                }
                var label_2 = this.add.text(0, y2, (y).toString());
                label_2.style.setShadow(1, 1);
                label_2.setOrigin(0, 0.5);
                this._axisLabels.push(label_2);
                labelWidth = Math.max(label_2.width, labelWidth);
            }
            for (var x = sx;; x += dx) {
                var x2 = (x - cam.scrollX) * cam.zoom;
                if (x2 > w) {
                    break;
                }
                if (x2 < labelWidth) {
                    continue;
                }
                this._gridGraphics.lineBetween(x2, labelHeight, x2, h);
            }
            for (var y = sy;; y += dy) {
                var y2 = (y - cam.scrollY) * cam.zoom;
                if (y2 > h) {
                    break;
                }
                if (y2 < labelHeight) {
                    continue;
                }
                this._gridGraphics.lineBetween(labelWidth, y2, w, y2);
            }
            this._gridGraphics.lineStyle(4, 0x000000, 1);
            this._gridGraphics.strokeRect((bx - cam.scrollX) * cam.zoom, (by - cam.scrollY) * cam.zoom, bw * cam.zoom, bh * cam.zoom);
            this._gridGraphics.lineStyle(2, 0xffffff, 1);
            this._gridGraphics.strokeRect(((bx - cam.scrollX) * cam.zoom), (by - cam.scrollY) * cam.zoom, bw * cam.zoom, bh * cam.zoom);
        };
        ToolScene.prototype.getSelectedObjects = function () {
            return this._selectedObjects;
        };
        ToolScene.prototype.updateSelectionObjects = function () {
            var editor = PhaserEditor2D.Editor.getInstance();
            this._selectedObjects = [];
            var objectScene = PhaserEditor2D.Editor.getInstance().getObjectScene();
            for (var _i = 0, _a = editor.selection; _i < _a.length; _i++) {
                var id = _a[_i];
                var obj = objectScene.sys.displayList.getByName(id);
                if (obj) {
                    this._selectedObjects.push(obj);
                }
            }
        };
        ToolScene.prototype.update = function () {
            this.renderAxis();
            this.renderSelection();
            this.updateTools();
            this._paintCallsLabel.visible = PhaserEditor2D.Editor.getInstance().sceneProperties.debugPaintCalls;
            if (this._paintCallsLabel.visible) {
                this._paintCallsLabel.text = PAINT_COUNT.toString();
                PAINT_COUNT += 1;
            }
        };
        ToolScene.prototype.setTools = function (tools) {
            for (var _i = 0, _a = this._tools; _i < _a.length; _i++) {
                var tool = _a[_i];
                tool.clear();
            }
            for (var _b = 0, tools_1 = tools; _b < tools_1.length; _b++) {
                var tool = tools_1[_b];
                tool.activated();
            }
            this._tools = tools;
        };
        ToolScene.prototype.updateTools = function () {
            for (var _i = 0, _a = this._tools; _i < _a.length; _i++) {
                var tool = _a[_i];
                tool.update();
            }
        };
        ToolScene.prototype.renderSelection = function () {
            this._selectionGraphics.clear();
            var g2 = this._selectionGraphics;
            for (var _i = 0, _a = this._selectedObjects; _i < _a.length; _i++) {
                var obj = _a[_i];
                this.paintSelectionBox(g2, obj);
            }
            if (this._selectionDragStart && !this._selectionDragStart.equals(this._selectionDragEnd)) {
                var x = this._selectionDragStart.x;
                var y = this._selectionDragStart.y;
                var width = this._selectionDragEnd.x - x;
                var height = this._selectionDragEnd.y - y;
                var g2_1 = this._selectionGraphics;
                g2_1.lineStyle(4, 0x000000);
                g2_1.strokeRect(x, y, width, height);
                g2_1.lineStyle(2, 0x00ff00);
                g2_1.strokeRect(x, y, width, height);
            }
        };
        ToolScene.prototype.paintSelectionBox = function (graphics, sprite) {
            PhaserEditor2D.Editor.getInstance().getWorldBounds(sprite, this._selectionBoxPoints);
            graphics.lineStyle(4, 0x000000);
            graphics.strokePoints(this._selectionBoxPoints, true);
            graphics.lineStyle(2, 0x00ff00);
            graphics.strokePoints(this._selectionBoxPoints, true);
        };
        ToolScene.prototype.containsPointer = function () {
            for (var _i = 0, _a = this._tools; _i < _a.length; _i++) {
                var tool = _a[_i];
                var b = tool.containsPointer();
                if (b) {
                    return true;
                }
            }
            return false;
        };
        ToolScene.prototype.isEditing = function () {
            for (var _i = 0, _a = this._tools; _i < _a.length; _i++) {
                var tool = _a[_i];
                if (tool.isEditing()) {
                    return true;
                }
            }
            return false;
        };
        ToolScene.prototype.testRepaint = function () {
            for (var _i = 0, _a = this._tools; _i < _a.length; _i++) {
                var tool = _a[_i];
                if (tool.requestRepaint) {
                    tool.requestRepaint = false;
                    PhaserEditor2D.Editor.getInstance().repaint();
                    return;
                }
            }
        };
        ToolScene.prototype.onToolsMouseDown = function () {
            this._paintDelayUtils.startPaintLoop();
            for (var _i = 0, _a = this._tools; _i < _a.length; _i++) {
                var tool = _a[_i];
                tool.onMouseDown();
            }
            this.testRepaint();
        };
        ToolScene.prototype.onToolsMouseMove = function () {
            for (var _i = 0, _a = this._tools; _i < _a.length; _i++) {
                var tool = _a[_i];
                tool.onMouseMove();
            }
            if (this._paintDelayUtils.shouldPaintThisTime()) {
                this.testRepaint();
            }
        };
        ToolScene.prototype.onToolsMouseUp = function () {
            for (var _i = 0, _a = this._tools; _i < _a.length; _i++) {
                var tool = _a[_i];
                tool.onMouseUp();
            }
            this.testRepaint();
        };
        ToolScene.prototype.onSelectionDragMouseDown = function (e) {
            if (!PhaserEditor2D.isLeftButton(e)) {
                return;
            }
            this._paintDelayUtils.startPaintLoop();
            var pointer = this.input.activePointer;
            this._selectionDragStart = new Phaser.Math.Vector2(pointer.x, pointer.y);
            this._selectionDragEnd = this._selectionDragStart.clone();
        };
        ToolScene.prototype.onSelectionDragMouseMove = function (e) {
            if (this._selectionDragStart) {
                var pointer = this.input.activePointer;
                this._selectionDragEnd.set(pointer.x, pointer.y);
                return this._paintDelayUtils.shouldPaintThisTime();
            }
            return false;
        };
        ToolScene.prototype.selectionDragClear = function () {
            this._selectionDragStart = null;
            this._selectionDragEnd = null;
        };
        ToolScene.prototype.onSelectionDragMouseUp = function (e) {
            if (this._selectionDragStart) {
                PhaserEditor2D.Editor.getInstance().getObjectScene().getPickManager().selectArea(this._selectionDragStart, this._selectionDragEnd);
                this.selectionDragClear();
            }
        };
        return ToolScene;
    }(Phaser.Scene));
    PhaserEditor2D.ToolScene = ToolScene;
})(PhaserEditor2D || (PhaserEditor2D = {}));
