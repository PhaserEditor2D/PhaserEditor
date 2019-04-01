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
            _this._selectionBoxPoints = [
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0)
            ];
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
                    color: 0x00ff00,
                    width: 2
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
            worldTx.transformPoint(x, y, this._selectionBoxPoints[0]);
            worldTx.transformPoint(x + w, y, this._selectionBoxPoints[1]);
            worldTx.transformPoint(x + w, y + h, this._selectionBoxPoints[2]);
            worldTx.transformPoint(x, y + h, this._selectionBoxPoints[3]);
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            for (var _i = 0, _a = this._selectionBoxPoints; _i < _a.length; _i++) {
                var p = _a[_i];
                p.set((p.x - cam.scrollX) * cam.zoom, (p.y - cam.scrollY) * cam.zoom);
            }
            graphics.lineStyle(4, 0x000000);
            graphics.strokePoints(this._selectionBoxPoints, true);
            graphics.lineStyle(2, 0x00ff00);
            graphics.strokePoints(this._selectionBoxPoints, true);
        };
        return ToolScene;
    }(Phaser.Scene));
    PhaserEditor2D.ToolScene = ToolScene;
})(PhaserEditor2D || (PhaserEditor2D = {}));
