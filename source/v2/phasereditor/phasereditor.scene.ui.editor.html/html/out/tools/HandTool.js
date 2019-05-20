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
    var HandTool = (function (_super) {
        __extends(HandTool, _super);
        function HandTool() {
            return _super.call(this) || this;
        }
        HandTool.prototype.canEdit = function (obj) {
            return true;
        };
        HandTool.prototype.containsPointer = function () {
            return true;
        };
        HandTool.prototype.update = function () {
        };
        HandTool.prototype.isEditing = function () {
            return this._dragging;
        };
        HandTool.prototype.onMouseDown = function () {
            this._dragging = true;
            var pointer = this.getToolPointer();
            this._dragStartPoint = new Phaser.Math.Vector2(pointer.x, pointer.y);
            var cam = this.objScene.cameras.main;
            this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);
        };
        HandTool.prototype.onMouseMove = function () {
            if (this._dragging) {
                this.objScene.input.setDefaultCursor("grabbing");
                var pointer = this.getToolPointer();
                var dx = this._dragStartPoint.x - pointer.x;
                var dy = this._dragStartPoint.y - pointer.y;
                var cam = this.objScene.cameras.main;
                cam.scrollX = this._dragStartCameraScroll.x + dx / cam.zoom;
                cam.scrollY = this._dragStartCameraScroll.y + dy / cam.zoom;
                PhaserEditor2D.Editor.getInstance().repaint();
            }
        };
        HandTool.prototype.onMouseUp = function () {
            if (this._dragging) {
                this._dragging = false;
                this.objScene.input.setDefaultCursor("grab");
                this.objScene.sendRecordCameraStateMessage();
            }
        };
        HandTool.prototype.activated = function () {
            this.objScene.input.setDefaultCursor("grab");
        };
        HandTool.prototype.clear = function () {
            this.objScene.input.setDefaultCursor("default");
        };
        return HandTool;
    }(PhaserEditor2D.InteractiveTool));
    PhaserEditor2D.HandTool = HandTool;
})(PhaserEditor2D || (PhaserEditor2D = {}));
