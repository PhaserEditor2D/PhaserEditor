var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var PhaserEditor2D;
(function (PhaserEditor2D) {
    var ToolScene = (function (_super) {
        __extends(ToolScene, _super);
        function ToolScene() {
            var _this = _super.call(this, "ToolScene") || this;
            _this._selectedObjects = [];
            _this._selectionGraphics = null;
            return _this;
        }
        ToolScene.prototype.create = function () {
            this.cameras.main.setOrigin(0, 0);
            this.scale.resize(window.innerWidth, window.innerHeight);
        };
        ToolScene.prototype.updateSelectionObjects = function () {
            this._selectedObjects = [];
            var objectScene = PhaserEditor2D.Editor.getInstance().getObjectScene();
            for (var _i = 0, _a = PhaserEditor2D.Models.selection; _i < _a.length; _i++) {
                var id = _a[_i];
                var obj = objectScene.sys.displayList.getByName(id);
                if (obj) {
                    this._selectedObjects.push(obj);
                }
            }
        };
        ToolScene.prototype.update = function () {
            if (this._selectionGraphics !== null) {
                this._selectionGraphics.destroy();
                this._selectionGraphics = null;
            }
            this._selectionGraphics = this.add.graphics({
                fillStyle: {
                    color: 0x00ff00
                },
                lineStyle: {
                    color: 0x00ff00
                }
            });
            var g2 = this._selectionGraphics;
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var point = new Phaser.Math.Vector2(0, 0);
            for (var _i = 0, _a = this._selectedObjects; _i < _a.length; _i++) {
                var obj = _a[_i];
                var worldTx = obj.getWorldTransformMatrix();
                worldTx.transformPoint(0, 0, point);
                point.x = (point.x - cam.scrollX) * cam.zoom;
                point.y = (point.y - cam.scrollY) * cam.zoom;
                g2.fillCircle(point.x, point.y, 10);
                this.paintSelectionBox(g2, obj);
            }
        };
        ToolScene.prototype.paintSelectionBox = function (graphics, gameObj) {
            var w = gameObj.width;
            var h = gameObj.height;
            var ox = gameObj.originX;
            var oy = gameObj.originY;
            var x = -w * ox;
            var y = -h * oy;
            var worldTx = gameObj.getWorldTransformMatrix();
            var p1 = new Phaser.Math.Vector2(0, 0);
            var p2 = new Phaser.Math.Vector2(0, 0);
            var p3 = new Phaser.Math.Vector2(0, 0);
            var p4 = new Phaser.Math.Vector2(0, 0);
            worldTx.transformPoint(x, y, p1);
            worldTx.transformPoint(x + w, y, p2);
            worldTx.transformPoint(x + w, y + h, p3);
            worldTx.transformPoint(x, y + h, p4);
            var points = [p1, p2, p3, p4];
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            for (var _i = 0, points_1 = points; _i < points_1.length; _i++) {
                var p = points_1[_i];
                p.set((p.x - cam.scrollX) * cam.zoom, (p.y - cam.scrollY) * cam.zoom);
            }
            graphics.strokePoints(points, true);
        };
        return ToolScene;
    }(Phaser.Scene));
    PhaserEditor2D.ToolScene = ToolScene;
})(PhaserEditor2D || (PhaserEditor2D = {}));
