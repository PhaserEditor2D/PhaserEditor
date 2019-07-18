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
            consoleLog("preload()");
            this.load.setBaseURL(this._initData.projectUrl);
            this.load.pack("pack", this._initData.pack);
        };
        ObjectScene.prototype.create = function () {
            var editor = PhaserEditor2D.Editor.getInstance();
            this._dragCameraManager = new DragCameraManager(this);
            this._dragObjectsManager = new DragObjectsManager();
            this._pickManager = new PickObjectManager();
            new DropManager();
            this.initCamera();
            this.initSelectionScene();
            editor.getCreate().createWorld(this, this._initData.displayList);
            editor.sceneCreated();
            this.sendRecordCameraStateMessage();
            editor.stop();
        };
        ObjectScene.prototype.updateBackground = function () {
            var rgb = "rgb(" + PhaserEditor2D.ScenePropertiesComponent.get_backgroundColor(PhaserEditor2D.Editor.getInstance().sceneProperties) + ")";
            this.cameras.main.setBackgroundColor(rgb);
        };
        ObjectScene.prototype.getPickManager = function () {
            return this._pickManager;
        };
        ObjectScene.prototype.getDragCameraManager = function () {
            return this._dragCameraManager;
        };
        ObjectScene.prototype.getDragObjectsManager = function () {
            return this._dragObjectsManager;
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
        ObjectScene.prototype.initSelectionScene = function () {
            this.scene.launch("ToolScene");
            this._toolScene = this.scene.get("ToolScene");
        };
        ObjectScene.prototype.initCamera = function () {
            var cam = this.cameras.main;
            cam.setOrigin(0, 0);
            cam.setRoundPixels(true);
            this.updateBackground();
            this.scale.resize(window.innerWidth, window.innerHeight);
        };
        ;
        ObjectScene.prototype.getToolScene = function () {
            return this._toolScene;
        };
        ObjectScene.prototype.onMouseWheel = function (e) {
            var cam = this.cameras.main;
            var delta = e.deltaY;
            var zoom = (delta > 0 ? 0.9 : 1.1);
            var pointer = this.input.activePointer;
            var point1 = cam.getWorldPoint(pointer.x, pointer.y);
            cam.zoom *= zoom;
            cam.preRender(this.scale.resolution);
            var point2 = cam.getWorldPoint(pointer.x, pointer.y);
            var dx = point2.x - point1.x;
            var dy = point2.y - point1.y;
            cam.scrollX += -dx;
            cam.scrollY += -dy;
            this.sendRecordCameraStateMessage();
        };
        ObjectScene.prototype.sendRecordCameraStateMessage = function () {
            var cam = this.cameras.main;
            PhaserEditor2D.Editor.getInstance().sendMessage({
                method: "RecordCameraState",
                cameraState: {
                    scrollX: cam.scrollX,
                    scrollY: cam.scrollY,
                    width: cam.width,
                    height: cam.height,
                    zoom: cam.zoom
                }
            });
        };
        ObjectScene.prototype.performResize = function () {
            this.cameras.main.setSize(window.innerWidth, window.innerHeight);
        };
        return ObjectScene;
    }(Phaser.Scene));
    PhaserEditor2D.ObjectScene = ObjectScene;
    var PickObjectManager = (function () {
        function PickObjectManager() {
        }
        PickObjectManager.prototype.onMouseDown = function (e) {
            var editor = PhaserEditor2D.Editor.getInstance();
            var scene = editor.getObjectScene();
            var pointer = scene.input.activePointer;
            if (!PhaserEditor2D.isLeftButton(e)) {
                return;
            }
            var result = editor.hitTestPointer(scene, pointer);
            consoleLog(result);
            var gameObj = result.pop();
            editor.sendMessage({
                method: "ClickObject",
                ctrl: e.ctrlKey,
                shift: e.shiftKey,
                id: gameObj ? gameObj.name : undefined
            });
            return gameObj;
        };
        return PickObjectManager;
    }());
    var DragObjectsManager = (function () {
        function DragObjectsManager() {
            this._startPoint = null;
            this._dragging = false;
            this._now = 0;
            this._filterPaintOnMove = PhaserEditor2D.Editor.getInstance().isChromiumWebview();
        }
        DragObjectsManager.prototype.getScene = function () {
            return PhaserEditor2D.Editor.getInstance().getObjectScene();
        };
        DragObjectsManager.prototype.getSelectedObjects = function () {
            return PhaserEditor2D.Editor.getInstance().getToolScene().getSelectedObjects();
        };
        DragObjectsManager.prototype.getPointer = function () {
            return this.getScene().input.activePointer;
        };
        DragObjectsManager.prototype.onMouseDown = function (e) {
            if (!PhaserEditor2D.isLeftButton(e) || this.getSelectedObjects().length === 0) {
                return;
            }
            if (this._filterPaintOnMove) {
                this._now = Date.now();
            }
            this._startPoint = this.getScene().getScenePoint(this.getPointer().x, this.getPointer().y);
            var tx = new Phaser.GameObjects.Components.TransformMatrix();
            var p = new Phaser.Math.Vector2();
            for (var _i = 0, _a = this.getSelectedObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                sprite.getWorldTransformMatrix(tx);
                tx.transformPoint(0, 0, p);
                sprite.setData("DragObjectsManager", {
                    initX: p.x,
                    initY: p.y
                });
            }
        };
        DragObjectsManager.prototype.onMouseMove = function (e) {
            if (!PhaserEditor2D.isLeftButton(e) || this._startPoint === null) {
                return;
            }
            this._dragging = true;
            var pos = this.getScene().getScenePoint(this.getPointer().x, this.getPointer().y);
            var dx = pos.x - this._startPoint.x;
            var dy = pos.y - this._startPoint.y;
            for (var _i = 0, _a = this.getSelectedObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                var data = sprite.getData("DragObjectsManager");
                var x = PhaserEditor2D.Editor.getInstance().snapValueX(data.initX + dx);
                var y = PhaserEditor2D.Editor.getInstance().snapValueX(data.initY + dy);
                if (sprite.parentContainer) {
                    var tx = sprite.parentContainer.getWorldTransformMatrix();
                    var p = new Phaser.Math.Vector2();
                    tx.applyInverse(x, y, p);
                    sprite.setPosition(p.x, p.y);
                }
                else {
                    sprite.setPosition(x, y);
                }
            }
            if (this._filterPaintOnMove) {
                var now = Date.now();
                if (now - this._now > 40) {
                    this._now = now;
                    PhaserEditor2D.Editor.getInstance().repaint();
                }
            }
            else {
                PhaserEditor2D.Editor.getInstance().repaint();
            }
        };
        DragObjectsManager.prototype.onMouseUp = function () {
            if (this._startPoint !== null && this._dragging) {
                this._dragging = false;
                this._startPoint = null;
                PhaserEditor2D.Editor.getInstance().sendMessage(PhaserEditor2D.BuildMessage.SetTransformProperties(this.getSelectedObjects()));
            }
            PhaserEditor2D.Editor.getInstance().repaint();
        };
        return DragObjectsManager;
    }());
    var DragCameraManager = (function () {
        function DragCameraManager(scene) {
            this._scene = scene;
            this._dragStartPoint = null;
        }
        DragCameraManager.prototype.onMouseDown = function (e) {
            if (PhaserEditor2D.isMiddleButton(e)) {
                this._dragStartPoint = new Phaser.Math.Vector2(e.clientX, e.clientY);
                var cam = this._scene.cameras.main;
                this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);
                e.preventDefault();
            }
        };
        DragCameraManager.prototype.onMouseMove = function (e) {
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
        DragCameraManager.prototype.onMouseUp = function () {
            if (this._dragStartPoint !== null) {
                this._scene.sendRecordCameraStateMessage();
            }
            this._dragStartPoint = null;
            this._dragStartCameraScroll = null;
        };
        return DragCameraManager;
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
