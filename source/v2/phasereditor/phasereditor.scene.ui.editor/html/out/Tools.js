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
    var InteractiveTool = (function () {
        function InteractiveTool() {
            this.toolScene = PhaserEditor2D.Editor.getInstance().getToolScene();
        }
        InteractiveTool.prototype.getObjects = function () {
            var _this = this;
            var sel = this.toolScene.getSelectedObjects();
            return sel.filter(function (obj) { return _this.canEdit(obj); });
        };
        InteractiveTool.prototype.contains = function (x, y) {
            return false;
        };
        InteractiveTool.prototype.render = function () {
        };
        InteractiveTool.prototype.onMouseDown = function () {
        };
        InteractiveTool.prototype.onMouseUp = function () {
        };
        InteractiveTool.prototype.onMouseMove = function () {
        };
        return InteractiveTool;
    }());
    PhaserEditor2D.InteractiveTool = InteractiveTool;
    var TileSizeTool = (function (_super) {
        __extends(TileSizeTool, _super);
        function TileSizeTool(changeX, changeY) {
            var _this = _super.call(this) || this;
            _this._changeX = changeX;
            _this._changeY = changeY;
            _this._shape = _this.toolScene.add.rectangle(0, 0, 12, 12, 0xff0000);
            return _this;
        }
        TileSizeTool.prototype.canEdit = function (obj) {
            return obj instanceof Phaser.GameObjects.TileSprite;
        };
        TileSizeTool.prototype.render = function () {
            var list = this.getObjects();
            if (list.length === 0) {
                this._shape.visible = false;
                return;
            }
            var shapePos = new Phaser.Math.Vector2();
            for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                var obj = list_1[_i];
                var sprite = obj;
                var localLeft = -sprite.width * sprite.originX;
                var localTop = -sprite.height * sprite.originY;
                var worldXY = new Phaser.Math.Vector2();
                var worldTx = sprite.getWorldTransformMatrix();
                var localX = this._changeX ? localLeft + sprite.width : localLeft + sprite.width / 2;
                var localY = this._changeY ? localTop + sprite.height : localTop + sprite.height / 2;
                worldTx.transformPoint(localX, localY, worldXY);
                shapePos.add(worldXY);
            }
            var len = this.getObjects().length;
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            shapePos.x = (shapePos.x / len - cam.scrollX) * cam.zoom;
            shapePos.y = (shapePos.y / len - cam.scrollY) * cam.zoom;
            this._shape.setPosition(shapePos.x, shapePos.y);
            this._shape.visible = true;
        };
        return TileSizeTool;
    }(InteractiveTool));
    PhaserEditor2D.TileSizeTool = TileSizeTool;
})(PhaserEditor2D || (PhaserEditor2D = {}));
