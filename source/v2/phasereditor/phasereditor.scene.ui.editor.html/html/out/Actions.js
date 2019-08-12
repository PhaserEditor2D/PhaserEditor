var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var PhaserEditor2D;
(function (PhaserEditor2D) {
    var PositionAction = (function () {
        function PositionAction(msg) {
            var displayList = PhaserEditor2D.Editor.getInstance().getObjectScene().sys.displayList;
            var list = msg.list;
            this._objects = list.map(function (id) { return displayList.getByName(id); });
        }
        PositionAction.prototype.run = function () {
            this.runPositionAction();
            var list = this._objects.map(function (obj) {
                return {
                    id: obj.name,
                    x: obj.x,
                    y: obj.y
                };
            });
            PhaserEditor2D.Editor.getInstance().sendMessage({
                method: "SetObjectPosition",
                list: list
            });
        };
        return PositionAction;
    }());
    PhaserEditor2D.PositionAction = PositionAction;
    var AlignAction = (function (_super) {
        __extends(AlignAction, _super);
        function AlignAction(msg) {
            var _this = _super.call(this, msg) || this;
            _this._align = msg.actionData.align;
            return _this;
        }
        AlignAction.prototype.runPositionAction = function () {
            var editor = PhaserEditor2D.Editor.getInstance();
            var minX = Number.MAX_VALUE;
            var minY = Number.MAX_VALUE;
            var maxX = Number.MIN_VALUE;
            var maxY = Number.MIN_VALUE;
            var width = 0;
            var height = 0;
            var tx = new Phaser.GameObjects.Components.TransformMatrix();
            var point = new Phaser.Math.Vector2();
            if (this._objects.length === 1) {
                minX = PhaserEditor2D.ScenePropertiesComponent.get_borderX(editor.sceneProperties);
                maxX = minX + PhaserEditor2D.ScenePropertiesComponent.get_borderWidth(editor.sceneProperties);
                minY = PhaserEditor2D.ScenePropertiesComponent.get_borderY(editor.sceneProperties);
                maxY = minY + PhaserEditor2D.ScenePropertiesComponent.get_borderHeight(editor.sceneProperties);
            }
            else {
                var points = [];
                var objects = [];
                for (var _i = 0, _a = this._objects; _i < _a.length; _i++) {
                    var obj = _a[_i];
                    var obj2 = obj;
                    var w = obj2.width;
                    var h = obj2.height;
                    var ox = obj2.originX;
                    var oy = obj2.originY;
                    var x = -w * ox;
                    var y = -h * oy;
                    obj2.getWorldTransformMatrix(tx);
                    tx.transformPoint(x, y, point);
                    points.push(point.clone());
                    tx.transformPoint(x + w, y, point);
                    points.push(point.clone());
                    tx.transformPoint(x + w, y + h, point);
                    points.push(point.clone());
                    tx.transformPoint(x, y + h, point);
                    points.push(point.clone());
                }
                for (var _b = 0, points_1 = points; _b < points_1.length; _b++) {
                    var point_1 = points_1[_b];
                    minX = Math.min(minX, point_1.x);
                    minY = Math.min(minY, point_1.y);
                    maxX = Math.max(maxX, point_1.x);
                    maxY = Math.max(maxY, point_1.y);
                }
            }
            for (var _c = 0, _d = this._objects; _c < _d.length; _c++) {
                var obj = _d[_c];
                var objWidth = obj.displayWidth;
                var objHeight = obj.displayHeight;
                var objOriginX = obj.displayOriginX * obj.scaleX;
                var objOriginY = obj.displayOriginY * obj.scaleY;
                switch (this._align) {
                    case "LEFT":
                        this.setX(obj, minX + objOriginX);
                        break;
                    case "RIGHT":
                        this.setX(obj, maxX - objWidth + objOriginX);
                        break;
                    case "HORIZONTAL_CENTER":
                        this.setX(obj, (minX + maxX) / 2 - objWidth / 2 + objOriginX);
                        break;
                    case "TOP":
                        this.setY(obj, minY + objOriginY);
                        break;
                    case "BOTTOM":
                        this.setY(obj, maxY + height - objHeight + objOriginY);
                        break;
                    case "VERTICAL_CENTER":
                        this.setY(obj, (minY + maxY) / 2 - objHeight / 2 + objOriginY);
                        break;
                }
            }
        };
        AlignAction.prototype.setX = function (obj, x) {
            if (obj.parentContainer) {
                var tx = obj.parentContainer.getWorldTransformMatrix();
                var point = tx.applyInverse(x, 0);
                obj.x = point.x;
            }
            else {
                obj.x = x;
            }
        };
        AlignAction.prototype.setY = function (obj, y) {
            if (obj.parentContainer) {
                var tx = obj.parentContainer.getWorldTransformMatrix();
                var point = tx.applyInverse(0, y);
                obj.y = point.y;
            }
            else {
                obj.y = y;
            }
        };
        return AlignAction;
    }(PositionAction));
    PhaserEditor2D.AlignAction = AlignAction;
})(PhaserEditor2D || (PhaserEditor2D = {}));
