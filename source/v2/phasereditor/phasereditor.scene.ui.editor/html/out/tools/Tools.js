var PhaserEditor2D;
(function (PhaserEditor2D) {
    PhaserEditor2D.ARROW_LENGTH = 80;
    var InteractiveTool = (function () {
        function InteractiveTool() {
            this.toolScene = PhaserEditor2D.Editor.getInstance().getToolScene();
            this.objScene = PhaserEditor2D.Editor.getInstance().getObjectScene();
            this.requestRepaint = false;
        }
        InteractiveTool.prototype.getObjects = function () {
            var _this = this;
            var sel = this.toolScene.getSelectedObjects();
            return sel.filter(function (obj) { return _this.canEdit(obj); });
        };
        InteractiveTool.prototype.containsPointer = function () {
            return false;
        };
        InteractiveTool.prototype.isEditing = function () {
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
        InteractiveTool.prototype.getToolPointer = function () {
            return this.toolScene.input.activePointer;
        };
        InteractiveTool.prototype.getScenePoint = function (toolX, toolY) {
            var cam = this.objScene.cameras.main;
            var sceneX = toolX / cam.zoom + cam.scrollX;
            var sceneY = toolY / cam.zoom + cam.scrollY;
            return new Phaser.Math.Vector2(sceneX, sceneY);
        };
        InteractiveTool.prototype.objectGlobalAngle = function (obj) {
            var a = obj.angle;
            var parent = obj.parentContainer;
            if (parent) {
                a += this.objectGlobalAngle(parent);
            }
            return a;
        };
        InteractiveTool.prototype.createArrowShape = function () {
            var s = this.toolScene.add.triangle(0, 0, 0, 0, 12, 0, 6, 12);
            s.setStrokeStyle(1, 0, 0.8);
            return s;
        };
        InteractiveTool.prototype.createRectangleShape = function () {
            var s = this.toolScene.add.rectangle(0, 0, 12, 12);
            s.setStrokeStyle(1, 0, 0.8);
            return s;
        };
        return InteractiveTool;
    }());
    PhaserEditor2D.InteractiveTool = InteractiveTool;
    var ToolFactory = (function () {
        function ToolFactory() {
        }
        ToolFactory.createByName = function (name) {
            switch (name) {
                case "TileSize":
                    return [
                        new PhaserEditor2D.TileSizeTool(true, false),
                        new PhaserEditor2D.TileSizeTool(false, true),
                        new PhaserEditor2D.TileSizeTool(true, true)
                    ];
                case "TilePosition":
                    return [
                        new PhaserEditor2D.TilePositionTool(true, false),
                        new PhaserEditor2D.TilePositionTool(false, true),
                        new PhaserEditor2D.TilePositionTool(true, true)
                    ];
            }
            return [];
        };
        return ToolFactory;
    }());
    PhaserEditor2D.ToolFactory = ToolFactory;
})(PhaserEditor2D || (PhaserEditor2D = {}));
