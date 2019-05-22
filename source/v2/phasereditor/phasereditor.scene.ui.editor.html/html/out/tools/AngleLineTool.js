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
    var AngleLineTool = (function (_super) {
        __extends(AngleLineTool, _super);
        function AngleLineTool(angleTool, start) {
            var _this = _super.call(this) || this;
            _this._angleTool = angleTool;
            _this._start = start;
            _this._color = 0xaaaaff;
            _this._shapeBorder = _this.toolScene.add.rectangle(0, 0, PhaserEditor2D.AngleTool.RADIUS, 4);
            _this._shapeBorder.setFillStyle(0);
            _this._shapeBorder.setOrigin(0, 0.5);
            _this._shapeBorder.depth = -1;
            _this._shape = _this.createLineShape();
            _this._shape.setStrokeStyle(2, _this._color);
            _this._shape.setOrigin(0, 0);
            _this._shape.setTo(0, 0, PhaserEditor2D.AngleTool.RADIUS, 0);
            _this._shape.depth = -1;
            return _this;
        }
        AngleLineTool.prototype.clear = function () {
            this._shape.visible = false;
            this._shapeBorder.visible = false;
        };
        AngleLineTool.prototype.containsPointer = function () {
            return false;
        };
        AngleLineTool.prototype.canEdit = function (obj) {
            return this._angleTool.canEdit(obj);
        };
        AngleLineTool.prototype.isEditing = function () {
            return this._angleTool.isEditing();
        };
        AngleLineTool.prototype.render = function (objects) {
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var pos = new Phaser.Math.Vector2(0, 0);
            var globalStartAngle = 0;
            var globalEndAngle = 0;
            var localCoords = PhaserEditor2D.Editor.getInstance().isTransformLocalCoords();
            for (var _i = 0, objects_1 = objects; _i < objects_1.length; _i++) {
                var sprite = objects_1[_i];
                var worldXY = new Phaser.Math.Vector2();
                var worldTx = sprite.getWorldTransformMatrix();
                worldTx.transformPoint(0, 0, worldXY);
                pos.add(worldXY);
                var endAngle = this.objectGlobalAngle(sprite);
                var startAngle = localCoords ? endAngle - sprite.angle : 0;
                globalStartAngle += startAngle;
                globalEndAngle += endAngle;
            }
            var len = this.getObjects().length;
            var cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            var cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
            globalStartAngle /= len;
            globalEndAngle /= len;
            this._shape.setPosition(cameraX, cameraY);
            this._shape.angle = this._start ? globalStartAngle : globalEndAngle;
            this._shape.visible = true;
            this._shapeBorder.setPosition(cameraX, cameraY);
            this._shapeBorder.angle = this._shape.angle;
            this._shapeBorder.visible = this._shape.visible;
        };
        AngleLineTool.prototype.onMouseDown = function () {
            this._shape.strokeColor = 0xffffff;
        };
        AngleLineTool.prototype.onMouseUp = function () {
            this._shape.strokeColor = this._color;
        };
        return AngleLineTool;
    }(PhaserEditor2D.InteractiveTool));
    PhaserEditor2D.AngleLineTool = AngleLineTool;
})(PhaserEditor2D || (PhaserEditor2D = {}));
