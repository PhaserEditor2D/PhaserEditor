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
            var _this = _super.call(this, "ObjectScene") || this;
            _this.cameraZoom = function (delta) {
                var cam = this.cameras.main;
                if (delta < 0) {
                    cam.zoom *= 1.1;
                }
                else {
                    cam.zoom *= 0.9;
                }
            };
            _this.cameraPan = function (dx, dy) {
                var cam = this.cameras.main;
                cam.scrollX += dx * 30 / cam.zoom;
                cam.scrollY += dy * 30 / cam.zoom;
            };
            return _this;
        }
        ObjectScene.prototype.preload = function () {
            console.log("preload()");
            this.load.setBaseURL(PhaserEditor2D.Models.projectUrl);
            this.load.pack("pack", PhaserEditor2D.Models.pack);
        };
        ObjectScene.prototype.create = function () {
            PhaserEditor2D.Editor.getInstance().stop();
            this._dragManager = new DragManager(this);
            this.initCamera();
            this.initKeyboard();
            this.initSelectionScene();
            var editor = PhaserEditor2D.Editor.getInstance();
            this.initBackground();
            editor.getCreate().createWorld(this.add);
            editor.sendMessage({
                method: "GetSelectObjects"
            });
            this.initBackground();
            editor.repaint();
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
        ObjectScene.prototype.initKeyboard = function () {
            this.input.keyboard.on("keydown_I", function () {
                this.cameraZoom(-1);
            }, this);
            this.input.keyboard.on("keydown_O", function () {
                this.cameraZoom(1);
            }, this);
            this.input.keyboard.on("keydown_LEFT", function () {
                this.cameraPan(-1, 0);
            }, this);
            this.input.keyboard.on("keydown_RIGHT", function () {
                this.cameraPan(1, 0);
            }, this);
            this.input.keyboard.on("keydown_UP", function () {
                this.cameraPan(0, -1);
            }, this);
            this.input.keyboard.on("keydown_DOWN", function () {
                this.cameraPan(0, 1);
            }, this);
        };
        ;
        ObjectScene.prototype.initCamera = function () {
            var cam = this.cameras.main;
            cam.setOrigin(0, 0);
            cam.setRoundPixels(true);
            this.scale.resize(window.innerWidth, window.innerHeight);
            this.input.keyboard.addCapture([
                Phaser.Input.Keyboard.KeyCodes.I,
                Phaser.Input.Keyboard.KeyCodes.O,
                Phaser.Input.Keyboard.KeyCodes.LEFT,
                Phaser.Input.Keyboard.KeyCodes.RIGHT,
                Phaser.Input.Keyboard.KeyCodes.UP,
                Phaser.Input.Keyboard.KeyCodes.DOWN,
            ]);
        };
        ;
        ObjectScene.prototype.getToolScene = function () {
            return this._toolScene;
        };
        ObjectScene.prototype.onMouseWheel = function (e) {
            var cam = this.cameras.main;
            var delta = e.wheelDelta;
            var zoom = (delta < 0 ? 0.9 : 1.1);
            cam.zoom *= zoom;
        };
        ObjectScene.prototype.performResize = function () {
            this.scale.resize(window.innerWidth, window.innerHeight);
            this._backgroundScene.scale.resize(window.innerWidth, window.innerHeight);
        };
        return ObjectScene;
    }(Phaser.Scene));
    PhaserEditor2D.ObjectScene = ObjectScene;
    var DragManager = (function () {
        function DragManager(scene) {
            this._scene = scene;
            this._dragStartPoint = null;
            var self = this;
            scene.game.canvas.addEventListener("mousedown", function (e) {
                self.pointerDown(e);
            });
            scene.game.canvas.addEventListener("mousemove", function (e) {
                self.pointerMove(e);
            });
            scene.game.canvas.addEventListener("mouseup", function () {
                self.pointerUp();
            });
            scene.game.canvas.addEventListener("mouseleave", function () {
                self.pointerUp();
            });
        }
        DragManager.prototype.pointerDown = function (e) {
            if (e.buttons === 4) {
                this._dragStartPoint = new Phaser.Math.Vector2(e.clientX, e.clientY);
                var cam = this._scene.cameras.main;
                this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);
                e.preventDefault();
            }
        };
        DragManager.prototype.pointerMove = function (e) {
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
        DragManager.prototype.pointerUp = function () {
            this._dragStartPoint = null;
            this._dragStartCameraScroll = null;
        };
        return DragManager;
    }());
    var BackgroundScene = (function (_super) {
        __extends(BackgroundScene, _super);
        function BackgroundScene() {
            return _super.call(this, "BackgroundScene") || this;
        }
        BackgroundScene.prototype.create = function () {
            this._bg = this.add.graphics();
        };
        BackgroundScene.prototype.update = function () {
            this._bg.clear();
            var bgColor = Phaser.Display.Color.RGBStringToColor("rgb(" + PhaserEditor2D.Models.sceneProperties.backgroundColor + ")");
            this._bg.fillStyle(bgColor.color, 1);
            this._bg.fillRect(0, 0, window.innerWidth, window.innerHeight);
        };
        return BackgroundScene;
    }(Phaser.Scene));
    PhaserEditor2D.BackgroundScene = BackgroundScene;
})(PhaserEditor2D || (PhaserEditor2D = {}));
