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
    var ObjectScene = (function (_super) {
        __extends(ObjectScene, _super);
        function ObjectScene() {
            return _super.call(this, "ObjectScene") || this;
        }
        ObjectScene.prototype.init = function (initData) {
            this._initData = initData;
        };
        ObjectScene.prototype.preload = function () {
            console.log("preload()");
            this.load.setBaseURL(this._initData.projectUrl);
            this.load.pack("pack", this._initData.pack);
        };
        ObjectScene.prototype.create = function () {
            PhaserEditor2D.Editor.getInstance().stop();
            this._dragManager = new DragManager(this);
            this._pickManager = new PickManager();
            new DropManager();
            this.initCamera();
            this.initSelectionScene();
            var editor = PhaserEditor2D.Editor.getInstance();
            this.initBackground();
            editor.getCreate().createWorld(this, this._initData.displayList);
            this.initBackground();
            editor.sceneCreated();
            editor.repaint();
        };
        ObjectScene.prototype.getPickManager = function () {
            return this._pickManager;
        };
        ObjectScene.prototype.getDragManager = function () {
            return this._dragManager;
        };
        ObjectScene.prototype.removeAllObjects = function () {
            var list = this.sys.displayList.list;
            for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                var obj = list_1[_i];
                obj.destroy();
            }
            this.sys.displayList.removeAll(false);
        };
        ObjectScene.prototype.getScenePoint = function (pointerX, pointerY) {
            var cam = this.cameras.main;
            var sceneX = pointerX / cam.zoom + cam.scrollX;
            var sceneY = pointerY / cam.zoom + cam.scrollY;
            return new Phaser.Math.Vector2(sceneX, sceneY);
        };
        ObjectScene.prototype.initBackground = function () {
            this.scene.launch("BackgroundScene");
            this._backgroundScene = this.scene.get("BackgroundScene");
            this.scene.moveDown("BackgroundScene");
        };
        ObjectScene.prototype.initSelectionScene = function () {
            this.scene.launch("ToolScene");
            this._toolScene = this.scene.get("ToolScene");
        };
        ObjectScene.prototype.initCamera = function () {
            var cam = this.cameras.main;
            cam.setOrigin(0, 0);
            cam.setRoundPixels(true);
            this.scale.resize(window.innerWidth, window.innerHeight);
        };
        ;
        ObjectScene.prototype.getToolScene = function () {
            return this._toolScene;
        };
        ObjectScene.prototype.getBackgroundScene = function () {
            return this._backgroundScene;
        };
        ObjectScene.prototype.onMouseWheel = function (e) {
            var cam = this.cameras.main;
            var delta = e.deltaY;
            var zoom = (delta > 0 ? 0.9 : 1.1);
            cam.zoom *= zoom;
            this.sendRecordCameraStateMessage();
        };
        ObjectScene.prototype.sendRecordCameraStateMessage = function () {
            var cam = this.cameras.main;
            PhaserEditor2D.Editor.getInstance().sendMessage({
                method: "RecordCameraState",
                cameraState: {
                    scrollX: cam.scrollX,
                    scrollY: cam.scrollY,
                    zoom: cam.zoom
                }
            });
        };
        ObjectScene.prototype.performResize = function () {
            this.cameras.main.setSize(window.innerWidth, window.innerHeight);
            this._backgroundScene.cameras.main.setSize(window.innerWidth, window.innerHeight);
        };
        return ObjectScene;
    }(Phaser.Scene));
    PhaserEditor2D.ObjectScene = ObjectScene;
    var BackgroundScene = (function (_super) {
        __extends(BackgroundScene, _super);
        function BackgroundScene() {
            return _super.call(this, "BackgroundScene") || this;
        }
        BackgroundScene.prototype.create = function () {
            this._bg = this.add.graphics();
            this.repaint();
        };
        BackgroundScene.prototype.repaint = function () {
            this._bg.clear();
            var bgColor = Phaser.Display.Color.RGBStringToColor("rgb(" + PhaserEditor2D.ScenePropertiesComponent.get_backgroundColor(PhaserEditor2D.Editor.getInstance().sceneProperties) + ")");
            this._bg.fillStyle(bgColor.color, 1);
            this._bg.fillRect(0, 0, window.innerWidth, window.innerHeight);
        };
        BackgroundScene.prototype.update = function () {
            this.repaint();
        };
        return BackgroundScene;
    }(Phaser.Scene));
    PhaserEditor2D.BackgroundScene = BackgroundScene;
    var PickManager = (function () {
        function PickManager() {
        }
        PickManager.prototype.onMouseDown = function (e) {
            var editor = PhaserEditor2D.Editor.getInstance();
            var scene = editor.getObjectScene();
            var pointer = scene.input.activePointer;
            if (pointer.buttons !== 1) {
                return;
            }
            var result = scene.input.hitTestPointer(pointer);
            var gameObj = result.pop();
            editor.sendMessage({
                method: "ClickObject",
                ctrl: e.ctrlKey,
                shift: e.shiftKey,
                id: gameObj ? gameObj.name : undefined
            });
        };
        return PickManager;
    }());
    var DragManager = (function () {
        function DragManager(scene) {
            this._scene = scene;
            this._dragStartPoint = null;
        }
        DragManager.prototype.onMouseDown = function (e) {
            if (e.buttons === 4) {
                this._dragStartPoint = new Phaser.Math.Vector2(e.clientX, e.clientY);
                var cam = this._scene.cameras.main;
                this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);
                e.preventDefault();
            }
        };
        DragManager.prototype.onMouseMove = function (e) {
            if (this._dragStartPoint === null) {
                return;
            }
            var dx = this._dragStartPoint.x - e.clientX;
            var dy = this._dragStartPoint.y - e.clientY;
            var cam = this._scene.cameras.main;
            cam.scrollX = this._dragStartCameraScroll.x + dx / cam.zoom;
            cam.scrollY = this._dragStartCameraScroll.y + dy / cam.zoom;
            PhaserEditor2D.Editor.getInstance().repaint();
            e.preventDefault();
        };
        DragManager.prototype.onMouseUp = function () {
            if (this._dragStartPoint !== null) {
                this._scene.sendRecordCameraStateMessage();
            }
            this._dragStartPoint = null;
            this._dragStartCameraScroll = null;
        };
        return DragManager;
    }());
    var DropManager = (function () {
        function DropManager() {
            window.addEventListener("drop", function (e) {
                var editor = PhaserEditor2D.Editor.getInstance();
                var point = editor.getObjectScene().cameras.main.getWorldPoint(e.clientX, e.clientY);
                editor.sendMessage({
                    method: "DropEvent",
                    x: point.x,
                    y: point.y
                });
            });
            window.addEventListener("dragover", function (e) {
                e.preventDefault();
            });
        }
        return DropManager;
    }());
})(PhaserEditor2D || (PhaserEditor2D = {}));
