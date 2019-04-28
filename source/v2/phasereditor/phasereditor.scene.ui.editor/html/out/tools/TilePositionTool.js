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
    var TilePositionTool = (function (_super) {
        __extends(TilePositionTool, _super);
        function TilePositionTool(changeX, changeY) {
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
        TilePositionTool.prototype.canEdit = function (obj) {
            return obj instanceof Phaser.GameObjects.TileSprite;
        };
        TilePositionTool.prototype.getX = function () {
            return this._handlerShape.x;
        };
        TilePositionTool.prototype.getY = function () {
            return this._handlerShape.y;
        };
        TilePositionTool.prototype.clear = function () {
            this._handlerShape.visible = false;
        };
        TilePositionTool.prototype.render = function (list) {
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var pos = new Phaser.Math.Vector2(0, 0);
            var angle = 0;
            for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                var obj = list_1[_i];
                var sprite = obj;
                var worldXY = new Phaser.Math.Vector2();
                var worldTx = sprite.getWorldTransformMatrix();
                var flipX = sprite.flipX ? -1 : 1;
                var flipY = sprite.flipY ? -1 : 1;
                var localLeft = -sprite.width * sprite.originX;
                var localTop = -sprite.height * sprite.originY;
                var localX = localLeft + sprite.tilePositionX;
                var localY = localTop + sprite.tilePositionY;
                if (!this._changeX || !this._changeY) {
                    if (this._changeX) {
                        localX += PhaserEditor2D.ARROW_LENGTH / cam.zoom;
                    }
                    else {
                        localY += PhaserEditor2D.ARROW_LENGTH / cam.zoom;
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
        TilePositionTool.prototype.containsPointer = function () {
            var pointer = this.getToolPointer();
            var d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);
            return d <= this._handlerShape.width;
        };
        TilePositionTool.prototype.isEditing = function () {
            return this._dragging;
        };
        TilePositionTool.prototype.onMouseDown = function () {
            if (this.containsPointer()) {
                this._dragging = true;
                this.requestRepaint = true;
                this._handlerShape.setFillStyle(0xffffff);
                var worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                var shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);
                for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                    var obj = _a[_i];
                    var sprite = obj;
                    var initLocalPos = new Phaser.Math.Vector2();
                    sprite.getWorldTransformMatrix(worldTx);
                    worldTx.applyInverse(shapePos.x, shapePos.y, initLocalPos);
                    sprite.setData("TilePositionTool", {
                        initTilePositionX: sprite.tilePositionX,
                        initTilePositionY: sprite.tilePositionY,
                        initLocalPos: initLocalPos
                    });
                }
            }
        };
        TilePositionTool.prototype.onMouseMove = function () {
            if (!this._dragging) {
                return;
            }
            this.requestRepaint = true;
            var pointer = this.getToolPointer();
            var pointerPos = this.getScenePoint(pointer.x, pointer.y);
            var worldTx = new Phaser.GameObjects.Components.TransformMatrix();
            for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                var data = sprite.data.get("TilePositionTool");
                var initLocalPos = data.initLocalPos;
                var localPos = new Phaser.Math.Vector2();
                sprite.getWorldTransformMatrix(worldTx);
                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);
                var dx = localPos.x - initLocalPos.x;
                var dy = localPos.y - initLocalPos.y;
                var tilePositionX = (data.initTilePositionX + dx) | 0;
                var tilePositionY = (data.initTilePositionY + dy) | 0;
                if (this._changeX) {
                    sprite.tilePositionX = tilePositionX;
                }
                if (this._changeY) {
                    sprite.tilePositionY = tilePositionY;
                }
            }
        };
        TilePositionTool.prototype.onMouseUp = function () {
            if (this._dragging) {
                var msg = PhaserEditor2D.BuildMessage.SetTileSpriteProperties(this.getObjects());
                PhaserEditor2D.Editor.getInstance().sendMessage(msg);
            }
            this._dragging = false;
            this._handlerShape.setFillStyle(this._color);
            this.requestRepaint = true;
        };
        return TilePositionTool;
    }(PhaserEditor2D.InteractiveTool));
    PhaserEditor2D.TilePositionTool = TilePositionTool;
})(PhaserEditor2D || (PhaserEditor2D = {}));
