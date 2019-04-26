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
            _this._changeX = changeX;
            _this._changeY = changeY;
            return _this;
        }
        TilePositionTool.prototype.canEdit = function (obj) {
            return obj instanceof Phaser.GameObjects.TileSprite;
        };
        return TilePositionTool;
    }(PhaserEditor2D.InteractiveTool));
    PhaserEditor2D.TilePositionTool = TilePositionTool;
})(PhaserEditor2D || (PhaserEditor2D = {}));
