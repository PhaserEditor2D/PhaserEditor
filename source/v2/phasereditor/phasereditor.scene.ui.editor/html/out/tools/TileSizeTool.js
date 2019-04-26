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
    var TileSizeTool = (function (_super) {
        __extends(TileSizeTool, _super);
        function TileSizeTool(changeX, changeY) {
            var _this = _super.call(this) || this;
            _this._dragging = false;
            _this._changeX = changeX;
            _this._changeY = changeY;
            _this._handlerShape = _this.toolScene.add.rectangle(0, 0, 12, 12, TileSizeTool.FILL_STYLE);
            return _this;
        }
        TileSizeTool.prototype.canEdit = function (obj) {
            return obj instanceof Phaser.GameObjects.TileSprite;
        };
        TileSizeTool.prototype.clear = function () {
            this._handlerShape.visible = false;
        };
        TileSizeTool.prototype.render = function (list) {
            var pos = new Phaser.Math.Vector2(0, 0);
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
                pos.add(worldXY);
            }
            var len = this.getObjects().length;
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            var cameraY = (pos.y / len - cam.scrollY) * cam.zoom;
            this._handlerShape.setPosition(cameraX, cameraY);
            this._handlerShape.visible = true;
        };
        TileSizeTool.prototype.containsPointer = function () {
            var toolPointer = this.getToolPointer();
            var d = Phaser.Math.Distance.Between(toolPointer.x, toolPointer.y, this._handlerShape.x, this._handlerShape.y);
            return d <= this._handlerShape.width;
        };
        TileSizeTool.prototype.isEditing = function () {
            return this._dragging;
        };
        TileSizeTool.prototype.onMouseDown = function () {
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
                    console.log("initLocalPos " + initLocalPos.x + " " + initLocalPos.y);
                    sprite.setData("TileSizeTool", {
                        initWidth: sprite.width,
                        initHeight: sprite.height,
                        initLocalPos: initLocalPos
                    });
                }
            }
        };
        TileSizeTool.prototype.onMouseMove = function () {
            if (!this._dragging) {
                return;
            }
            this.requestRepaint = true;
            var pointer = this.getToolPointer();
            var pointerPos = this.getScenePoint(pointer.x, pointer.y);
            console.log("onMouseMove");
            var worldTx = new Phaser.GameObjects.Components.TransformMatrix();
            for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                var data = sprite.data.get("TileSizeTool");
                var initLocalPos = data.initLocalPos;
                var localPos = new Phaser.Math.Vector2();
                sprite.getWorldTransformMatrix(worldTx);
                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);
                var dx = localPos.x - initLocalPos.x;
                var dy = localPos.y - initLocalPos.y;
                var width = (data.initWidth + dx) | 0;
                var height = (data.initHeight + dy) | 0;
                if (this._changeX) {
                    sprite.setSize(width, sprite.height);
                }
                if (this._changeY) {
                    sprite.setSize(sprite.width, height);
                }
                console.log(dx + " " + dy);
            }
        };
        TileSizeTool.prototype.onMouseUp = function () {
            if (this._dragging) {
                var msg = PhaserEditor2D.BuildMessage.SetTileSpriteProperties(this.getObjects());
                PhaserEditor2D.Editor.getInstance().sendMessage(msg);
            }
            this._dragging = false;
            this._handlerShape.setFillStyle(TileSizeTool.FILL_STYLE);
            this.requestRepaint = true;
        };
        TileSizeTool.FILL_STYLE = 0xff0000;
        return TileSizeTool;
    }(PhaserEditor2D.InteractiveTool));
    PhaserEditor2D.TileSizeTool = TileSizeTool;
})(PhaserEditor2D || (PhaserEditor2D = {}));
