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
        InteractiveTool.prototype.containsPointer = function () {
            return false;
        };
        InteractiveTool.prototype.clear = function () {
        };
        InteractiveTool.prototype.update = function () {
            var list = this.getObjects();
            if (list.length === 0) {
                this.clear();
            }
            else {
                this.render(list);
            }
        };
        InteractiveTool.prototype.render = function (objects) {
        };
        InteractiveTool.prototype.onMouseDown = function () {
        };
        InteractiveTool.prototype.onMouseUp = function () {
        };
        InteractiveTool.prototype.onMouseMove = function () {
        };
        InteractiveTool.prototype.getPointer = function () {
            return this.toolScene.input.activePointer;
        };
        return InteractiveTool;
    }());
    PhaserEditor2D.InteractiveTool = InteractiveTool;
    var TileSizeTool = (function (_super) {
        __extends(TileSizeTool, _super);
        function TileSizeTool(changeX, changeY) {
            var _this = _super.call(this) || this;
            _this._dragging = false;
            _this._worldPos = new Phaser.Math.Vector2();
            _this._initWorldPos = new Phaser.Math.Vector2();
            _this._changeX = changeX;
            _this._changeY = changeY;
            _this._shape = _this.toolScene.add.rectangle(0, 0, 12, 12, 0xff0000);
            return _this;
        }
        TileSizeTool.prototype.canEdit = function (obj) {
            return obj instanceof Phaser.GameObjects.TileSprite;
        };
        TileSizeTool.prototype.clear = function () {
            this._shape.visible = false;
        };
        TileSizeTool.prototype.render = function (list) {
            this._worldPos.set(0, 0);
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
                this._worldPos.add(worldXY);
            }
            var len = this.getObjects().length;
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var cameraX = (this._worldPos.x / len - cam.scrollX) * cam.zoom;
            var cameraY = (this._worldPos.y / len - cam.scrollY) * cam.zoom;
            this._shape.setPosition(cameraX, cameraY);
            this._shape.visible = true;
        };
        TileSizeTool.prototype.containsPointer = function () {
            var pointer = this.getPointer();
            var d = this._worldPos.distance(new Phaser.Math.Vector2(pointer.worldX, pointer.worldY));
            return d <= this._shape.width / 2;
        };
        TileSizeTool.prototype.onMouseDown = function () {
            if (this.containsPointer()) {
                this._dragging = true;
                this._initWorldPos.setFromObject(this._initWorldPos);
                var worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                for (var _i = 0, _a = this.getObjects(); _i < _a.length; _i++) {
                    var obj = _a[_i];
                    var sprite = obj;
                    var initLocalPos = new Phaser.Math.Vector2();
                    sprite.getWorldTransformMatrix(worldTx);
                    worldTx.applyInverse(this._initWorldPos.x, this._initWorldPos.y, initLocalPos);
                    sprite.setData("TileSizeTool", {
                        initWidth: sprite.width,
                        initHeight: sprite.height,
                        initLocalPos: initLocalPos
                    });
                }
            }
        };
        return TileSizeTool;
    }(InteractiveTool));
    PhaserEditor2D.TileSizeTool = TileSizeTool;
    var ToolFactory = (function () {
        function ToolFactory() {
        }
        ToolFactory.createByName = function (name) {
            switch (name) {
                case "TileSize":
                    return [
                        new TileSizeTool(true, false),
                        new TileSizeTool(false, true),
                        new TileSizeTool(true, true)
                    ];
            }
            return [];
        };
        return ToolFactory;
    }());
    PhaserEditor2D.ToolFactory = ToolFactory;
})(PhaserEditor2D || (PhaserEditor2D = {}));
