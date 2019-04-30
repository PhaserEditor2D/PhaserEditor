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
    var PositionTool = (function (_super) {
        __extends(PositionTool, _super);
        function PositionTool(changeX, changeY) {
            var _this = _super.call(this) || this;
            _this._dragging = false;
            _this._changeX = changeX;
            _this._changeY = changeY;
            if (changeX && changeY) {
                _this._color = 0xffff00;
            }
            else if (changeX) {
                _this._color = 0xff0000;
            }
            else {
                _this._color = 0x00ff00;
            }
            _this._handlerShape = changeX && changeY ? _this.createRectangleShape() : _this.createArrowShape();
            _this._handlerShape.setFillStyle(_this._color);
            return _this;
        }
        PositionTool.prototype.canEdit = function (obj) {
            return obj.x !== null;
        };
        PositionTool.prototype.getX = function () {
            return this._handlerShape.x;
        };
        PositionTool.prototype.getY = function () {
            return this._handlerShape.y;
        };
        PositionTool.prototype.clear = function () {
            this._handlerShape.visible = false;
        };
        PositionTool.prototype.render = function (list) {
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var pos = new Phaser.Math.Vector2(0, 0);
            var angle = 0;
            var localCoords = PhaserEditor2D.Editor.getInstance().isTransformLocalCoords();
            var globalCenterXY = new Phaser.Math.Vector2();
            for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                var sprite = list_1[_i];
                var worldTx = sprite.getWorldTransformMatrix();
                var centerXY = new Phaser.Math.Vector2();
                worldTx.transformPoint(0, 0, centerXY);
                globalCenterXY.add(centerXY);
                var worldXY = new Phaser.Math.Vector2();
                var localX = 0;
                var localY = 0;
                if (localCoords) {
                    if (!this._changeX || !this._changeY) {
                        if (this._changeX) {
                            localX += PhaserEditor2D.ARROW_LENGTH / cam.zoom / sprite.scaleX;
                        }
                        else {
                            localY += PhaserEditor2D.ARROW_LENGTH / cam.zoom / sprite.scaleY;
                        }
                    }
                    angle += this.objectGlobalAngle(sprite);
                    worldTx.transformPoint(localX, localY, worldXY);
                }
                else {
                    if (!this._changeX || !this._changeY) {
                        worldTx.transformPoint(0, 0, worldXY);
                        if (this._changeX) {
                            worldXY.x += PhaserEditor2D.ARROW_LENGTH / cam.zoom;
                        }
                        else {
                            worldXY.y += PhaserEditor2D.ARROW_LENGTH / cam.zoom;
                        }
                    }
                    else {
                        worldTx.transformPoint(0, 0, worldXY);
                    }
                }
                pos.add(worldXY);
            }
            var len = this.getObjects().length;
            pos.x /= len;
            pos.y /= len;
            this._arrowPoint = new Phaser.Math.Vector2(pos.x, pos.y);
            this._centerPoint = new Phaser.Math.Vector2(globalCenterXY.x / len, globalCenterXY.y / len);
            var cameraX = (pos.x - cam.scrollX) * cam.zoom;
            var cameraY = (pos.y - cam.scrollY) * cam.zoom;
            this._handlerShape.setPosition(cameraX, cameraY);
            this._handlerShape.angle = angle / len;
            if (this._changeX) {
                this._handlerShape.angle -= 90;
            }
            this._handlerShape.visible = true;
        };
        PositionTool.prototype.containsPointer = function () {
            var pointer = this.getToolPointer();
            var d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);
            return d <= this._handlerShape.width;
        };
        PositionTool.prototype.isEditing = function () {
            return this._dragging;
        };
        PositionTool.prototype.onMouseDown = function () {
            if (this.containsPointer()) {
                this._dragging = true;
                this.requestRepaint = true;
                var pointer = this.getToolPointer();
                this._startCursor = this.getScenePoint(pointer.x, pointer.y);
                this._handlerShape.setFillStyle(0xffffff);
                this._startVector = new Phaser.Math.Vector2(this._arrowPoint.x - this._centerPoint.x, this._arrowPoint.y - this._centerPoint.y);
                var p = new Phaser.Math.Vector2();
                for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                    var obj = _a[_i];
                    var sprite = obj;
                    var tx = sprite.getWorldTransformMatrix();
                    tx.applyInverse(0, 0, p);
                    sprite.setData("PositionTool", {
                        initX: sprite.x,
                        initY: sprite.y,
                        initWorldTx: tx
                    });
                }
            }
        };
        PositionTool.prototype.onMouseMove = function () {
            if (!this._dragging) {
                return;
            }
            this.requestRepaint = true;
            var pointer = this.getToolPointer();
            var endCursor = this.getScenePoint(pointer.x, pointer.y);
            var localCoords = PhaserEditor2D.Editor.getInstance().isTransformLocalCoords();
            var changeXY = this._changeX && this._changeY;
            for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                var data = sprite.data.get("PositionTool");
                var p0 = new Phaser.Math.Vector2();
                var p1 = new Phaser.Math.Vector2();
                if (sprite.parentContainer) {
                    var tx = sprite.parentContainer.getWorldTransformMatrix();
                    tx.transformPoint(this._startCursor.x, this._startCursor.y, p0);
                    tx.transformPoint(endCursor.x, endCursor.y, p1);
                }
                else {
                    p0.setFromObject(this._startCursor);
                    p1.setFromObject(endCursor);
                }
                var x = void 0;
                var y = void 0;
                if (changeXY) {
                    var dx = p1.x - p0.x;
                    var dy = p1.y - p0.y;
                    x = data.initX + dx;
                    y = data.initY + dy;
                }
                else {
                    var vector = new Phaser.Math.Vector2(this._changeX ? 1 : 0, this._changeY ? 1 : 0);
                    if (localCoords) {
                        var tx = new Phaser.GameObjects.Components.TransformMatrix();
                        tx.rotate(sprite.rotation);
                        tx.transformPoint(vector.x, vector.y, vector);
                    }
                    var moveVector = new Phaser.Math.Vector2(endCursor.x - this._startCursor.x, endCursor.y - this._startCursor.y);
                    var d = moveVector.dot(this._startVector) / this._startVector.length();
                    vector.x *= d;
                    vector.y *= d;
                    x = data.initX + vector.x;
                    y = data.initY + vector.y;
                }
                x = this.snapValueX(x);
                y = this.snapValueY(y);
                sprite.setPosition(x, y);
            }
        };
        PositionTool.prototype.onMouseUp = function () {
            if (this._dragging) {
                var msg = PhaserEditor2D.BuildMessage.SetTransformProperties(this.getObjects());
                PhaserEditor2D.Editor.getInstance().sendMessage(msg);
            }
            this._dragging = false;
            this._handlerShape.setFillStyle(this._color);
            this.requestRepaint = true;
        };
        return PositionTool;
    }(PhaserEditor2D.InteractiveTool));
    PhaserEditor2D.PositionTool = PositionTool;
})(PhaserEditor2D || (PhaserEditor2D = {}));
