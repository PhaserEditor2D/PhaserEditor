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
    var ScaleTool = (function (_super) {
        __extends(ScaleTool, _super);
        function ScaleTool(changeX, changeY) {
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
            _this._handlerShape = _this.createRectangleShape();
            _this._handlerShape.setFillStyle(_this._color);
            return _this;
        }
        ScaleTool.prototype.canEdit = function (obj) {
            return obj.scaleX !== undefined;
        };
        ScaleTool.prototype.clear = function () {
            this._handlerShape.visible = false;
        };
        ScaleTool.prototype.render = function (list) {
            var pos = new Phaser.Math.Vector2(0, 0);
            var angle = 0;
            for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                var sprite = list_1[_i];
                var flipX = sprite.flipX ? -1 : 1;
                var flipY = sprite.flipY ? -1 : 1;
                if (sprite instanceof Phaser.GameObjects.TileSprite) {
                    flipX = 1;
                    flipY = 1;
                }
                angle += this.objectGlobalAngle(sprite);
                var width = sprite.width * flipX;
                var height = sprite.height * flipY;
                var x = -width * sprite.originX;
                var y = -height * sprite.originY;
                var worldXY = new Phaser.Math.Vector2();
                var worldTx = sprite.getWorldTransformMatrix();
                var localX = this._changeX ? x + width : x + width / 2;
                var localY = this._changeY ? y + height : y + height / 2;
                worldTx.transformPoint(localX, localY, worldXY);
                pos.add(worldXY);
            }
            var len = this.getObjects().length;
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            var cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
            this._handlerShape.setPosition(cameraX, cameraY);
            this._handlerShape.angle = angle / len + (this._changeX && !this._changeY ? -90 : 0);
            this._handlerShape.visible = true;
        };
        ScaleTool.prototype.containsPointer = function () {
            var toolPointer = this.getToolPointer();
            var d = Phaser.Math.Distance.Between(toolPointer.x, toolPointer.y, this._handlerShape.x, this._handlerShape.y);
            return d <= this._handlerShape.width;
        };
        ScaleTool.prototype.isEditing = function () {
            return this._dragging;
        };
        ScaleTool.prototype.onMouseDown = function () {
            if (this.containsPointer()) {
                this._dragging = true;
                this.requestRepaint = true;
                this._handlerShape.setFillStyle(0xffffff);
                var shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);
                for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                    var obj = _a[_i];
                    var sprite = obj;
                    var worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                    var initLocalPos = new Phaser.Math.Vector2();
                    sprite.getWorldTransformMatrix(worldTx);
                    worldTx.applyInverse(shapePos.x, shapePos.y, initLocalPos);
                    sprite.setData("ScaleTool", {
                        initScaleX: sprite.scaleX,
                        initScaleY: sprite.scaleY,
                        initWidth: sprite.width,
                        initHeight: sprite.height,
                        initLocalPos: initLocalPos,
                        initWorldTx: worldTx
                    });
                }
            }
        };
        ScaleTool.prototype.onMouseMove = function () {
            if (!this._dragging) {
                return;
            }
            this.requestRepaint = true;
            var pointer = this.getToolPointer();
            var pointerPos = this.getScenePoint(pointer.x, pointer.y);
            for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                var data = sprite.data.get("ScaleTool");
                var initLocalPos = data.initLocalPos;
                var localPos = new Phaser.Math.Vector2();
                var worldTx = data.initWorldTx;
                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);
                var flipX = sprite.flipX ? -1 : 1;
                var flipY = sprite.flipY ? -1 : 1;
                if (sprite instanceof Phaser.GameObjects.TileSprite) {
                    flipX = 1;
                    flipY = 1;
                }
                var dx = (localPos.x - initLocalPos.x) * flipX;
                var dy = (localPos.y - initLocalPos.y) * flipY;
                var width = data.initWidth - sprite.displayOriginX;
                var height = data.initHeight - sprite.displayOriginY;
                if (width === 0) {
                    width = data.initWidth;
                }
                if (height === 0) {
                    height = data.initHeight;
                }
                var scaleDX = dx / width * data.initScaleX;
                var scaleDY = dy / height * data.initScaleY;
                var newScaleX = data.initScaleX + scaleDX;
                var newScaleY = data.initScaleY + scaleDY;
                if (this._changeX) {
                    sprite.scaleX = newScaleX;
                }
                if (this._changeY) {
                    sprite.scaleY = newScaleY;
                }
            }
        };
        ScaleTool.prototype.onMouseUp = function () {
            if (this._dragging) {
                var msg = PhaserEditor2D.BuildMessage.SetTransformProperties(this.getObjects());
                PhaserEditor2D.Editor.getInstance().sendMessage(msg);
            }
            this._dragging = false;
            this._handlerShape.setFillStyle(this._color);
            this.requestRepaint = true;
        };
        return ScaleTool;
    }(PhaserEditor2D.InteractiveTool));
    PhaserEditor2D.ScaleTool = ScaleTool;
})(PhaserEditor2D || (PhaserEditor2D = {}));
