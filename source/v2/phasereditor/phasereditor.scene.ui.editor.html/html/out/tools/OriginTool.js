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
    var OriginTool = (function (_super) {
        __extends(OriginTool, _super);
        function OriginTool(changeX, changeY) {
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
            _this._handlerShape = changeX && changeY ? _this.createCircleShape() : _this.createArrowShape();
            _this._handlerShape.setFillStyle(_this._color);
            return _this;
        }
        OriginTool.prototype.canEdit = function (obj) {
            return obj.hasOwnProperty("originX");
        };
        OriginTool.prototype.getX = function () {
            return this._handlerShape.x;
        };
        OriginTool.prototype.getY = function () {
            return this._handlerShape.y;
        };
        OriginTool.prototype.clear = function () {
            this._handlerShape.visible = false;
        };
        OriginTool.prototype.render = function (list) {
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var pos = new Phaser.Math.Vector2(0, 0);
            var angle = 0;
            for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                var obj = list_1[_i];
                var sprite = obj;
                var worldXY = new Phaser.Math.Vector2();
                var worldTx = sprite.getWorldTransformMatrix();
                var localX = 0;
                var localY = 0;
                var scale = this.objectGlobalScale(sprite);
                if (!this._changeX || !this._changeY) {
                    if (this._changeX) {
                        localX += PhaserEditor2D.ARROW_LENGTH / scale.x / cam.zoom * (sprite.flipX ? -1 : 1);
                        if (sprite.flipX) {
                            angle += 180;
                        }
                    }
                    else {
                        localY += PhaserEditor2D.ARROW_LENGTH / scale.y / cam.zoom * (sprite.flipY ? -1 : 1);
                        if (sprite.flipY) {
                            angle += 180;
                        }
                    }
                }
                angle += this.objectGlobalAngle(sprite);
                worldTx.transformPoint(localX, localY, worldXY);
                pos.add(worldXY);
            }
            var len = this.getObjects().length;
            var cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            var cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
            this._handlerShape.setPosition(cameraX, cameraY);
            this._handlerShape.angle = angle / len;
            if (this._changeX) {
                this._handlerShape.angle -= 90;
            }
            this._handlerShape.visible = true;
        };
        OriginTool.prototype.containsPointer = function () {
            var pointer = this.getToolPointer();
            var d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);
            return d <= this._handlerShape.width;
        };
        OriginTool.prototype.isEditing = function () {
            return this._dragging;
        };
        OriginTool.prototype.onMouseDown = function () {
            if (this.containsPointer()) {
                this._dragging = true;
                this.requestRepaint = true;
                this._handlerShape.setFillStyle(0xffffff);
                var shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);
                for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                    var obj = _a[_i];
                    var sprite = obj;
                    var initLocalPos = new Phaser.Math.Vector2();
                    var worldTx = new Phaser.GameObjects.Components.TransformMatrix();
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
        };
        OriginTool.prototype.onMouseMove = function () {
            if (!this._dragging) {
                return;
            }
            this.requestRepaint = true;
            var pointer = this.getToolPointer();
            var pointerPos = this.getScenePoint(pointer.x, pointer.y);
            for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                var data = obj.data.get("OriginTool");
                var initLocalPos = data.initLocalPos;
                var flipX = sprite.flipX ? -1 : 1;
                var flipY = sprite.flipY ? -1 : 1;
                var localPos = new Phaser.Math.Vector2();
                var worldTx = data.initWorldTx;
                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);
                var dx = (localPos.x - initLocalPos.x) * flipX;
                var dy = (localPos.y - initLocalPos.y) * flipY;
                var width = sprite.width;
                var height = sprite.height;
                var originDX = dx / width;
                var originDY = dy / height;
                console.log("---");
                console.log("width " + width);
                console.log("dx " + dx);
                console.log("originDX " + originDX);
                var newOriginX = data.initOriginX + (this._changeX ? originDX : 0);
                var newOriginY = data.initOriginY + (this._changeY ? originDY : 0);
                var local1 = new Phaser.Math.Vector2(data.initOriginX * width, data.initOriginY * height);
                var local2 = new Phaser.Math.Vector2(newOriginX * width, newOriginY * height);
                var parent1 = this.localToParent(sprite, local1);
                var parent2 = this.localToParent(sprite, local2);
                var dx2 = parent2.x - parent1.x;
                var dy2 = parent2.y - parent1.y;
                sprite.x = data.initX + dx2 * flipX;
                sprite.y = data.initY + dy2 * flipY;
                sprite.setOrigin(newOriginX, newOriginY);
            }
        };
        OriginTool.prototype.onMouseUp = function () {
            if (this._dragging) {
                var msg = PhaserEditor2D.BuildMessage.SetOriginProperties(this.getObjects());
                PhaserEditor2D.Editor.getInstance().sendMessage(msg);
            }
            this._dragging = false;
            this._handlerShape.setFillStyle(this._color);
            this.requestRepaint = true;
        };
        return OriginTool;
    }(PhaserEditor2D.InteractiveTool));
    PhaserEditor2D.OriginTool = OriginTool;
})(PhaserEditor2D || (PhaserEditor2D = {}));
