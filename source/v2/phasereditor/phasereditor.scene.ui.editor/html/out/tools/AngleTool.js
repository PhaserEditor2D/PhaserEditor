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
    var RADIUS = 100;
    var AngleTool = (function (_super) {
        __extends(AngleTool, _super);
        function AngleTool() {
            var _this = _super.call(this) || this;
            _this._dragging = false;
            _this._color = Phaser.Display.Color.GetColor(138, 43, 225);
            _this._handlerShape = _this.createCircleShape();
            _this._handlerShape.setFillStyle(0, 0);
            _this._handlerShape.setStrokeStyle(1, _this._color);
            _this._handlerShape.setRadius(RADIUS);
            _this._centerShape = _this.createCircleShape();
            _this._centerShape.setFillStyle(_this._color);
            return _this;
        }
        AngleTool.prototype.canEdit = function (obj) {
            return obj.angle !== undefined;
        };
        AngleTool.prototype.render = function (objects) {
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var pos = new Phaser.Math.Vector2(0, 0);
            for (var _i = 0, objects_1 = objects; _i < objects_1.length; _i++) {
                var sprite = objects_1[_i];
                var worldXY = new Phaser.Math.Vector2();
                var worldTx = sprite.getWorldTransformMatrix();
                worldTx.transformPoint(0, 0, worldXY);
                pos.add(worldXY);
            }
            var len = this.getObjects().length;
            var cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            var cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
            this._handlerShape.setPosition(cameraX, cameraY);
            this._handlerShape.visible = true;
            this._centerShape.setPosition(cameraX, cameraY);
            this._centerShape.visible = true;
        };
        AngleTool.prototype.containsPointer = function () {
            var pointer = this.getToolPointer();
            var d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);
            return Math.abs(d - RADIUS) <= 10;
        };
        AngleTool.prototype.clear = function () {
            this._handlerShape.visible = false;
            this._centerShape.visible = false;
        };
        AngleTool.prototype.isEditing = function () {
            return this._dragging;
        };
        AngleTool.prototype.onMouseDown = function () {
            if (this.containsPointer()) {
                this._dragging = true;
                this.requestRepaint = true;
                this._handlerShape.setStrokeStyle(2, 0xffffff);
                this._centerShape.setFillStyle(0xffffff);
                this._cursorStartX = this.getToolPointer().x;
                this._cursorStartY = this.getToolPointer().y;
                for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                    var obj = _a[_i];
                    var sprite = obj;
                    obj.setData("AngleTool", {
                        initAngle: sprite.angle
                    });
                }
            }
        };
        AngleTool.prototype.onMouseMove = function () {
            console.log(this._dragging);
            if (!this._dragging) {
                return;
            }
            var pointer = this.getToolPointer();
            var cursorX = pointer.x;
            var cursorY = pointer.y;
            var dx = this._cursorStartX - cursorX;
            var dy = this._cursorStartY - cursorY;
            if (Math.abs(dx) < 1 || Math.abs(dy) < 1) {
                return;
            }
            this.requestRepaint = true;
            for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                var data = obj.data.get("AngleTool");
                var deltaRadians = angleBetweenTwoPointsWithFixedPoint(cursorX, cursorY, this._cursorStartX, this._cursorStartY, this._centerShape.x, this._centerShape.y);
                var deltaAngle = Phaser.Math.RadToDeg(deltaRadians);
                sprite.angle = data.initAngle + deltaAngle;
            }
        };
        AngleTool.prototype.onMouseUp = function () {
            if (this._dragging) {
                var msg = PhaserEditor2D.BuildMessage.SetTransformProperties(this.getObjects());
                PhaserEditor2D.Editor.getInstance().sendMessage(msg);
            }
            this._dragging = false;
            this._handlerShape.setStrokeStyle(1, this._color);
            this._centerShape.setFillStyle(this._color);
            this.requestRepaint = true;
        };
        return AngleTool;
    }(PhaserEditor2D.InteractiveTool));
    PhaserEditor2D.AngleTool = AngleTool;
    function angleBetweenTwoPointsWithFixedPoint(point1X, point1Y, point2X, point2Y, fixedX, fixedY) {
        var angle1 = Math.atan2(point1Y - fixedY, point1X - fixedX);
        var angle2 = Math.atan2(point2Y - fixedY, point2X - fixedX);
        return angle1 - angle2;
    }
})(PhaserEditor2D || (PhaserEditor2D = {}));
