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
            _this._loadingSprite = null;
            _this.cameraZoom = function (delta) {
                var scene = this;
                var cam = scene.cameras.main;
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
            if (!PhaserEditor2D.Models.isReady()) {
                console.log("First preload");
                PhaserEditor2D.Editor.getInstance().openSocket();
                return;
            }
            console.log("Common preload");
            this.load.reset();
            var urls = PhaserEditor2D.Models.packs;
            for (var i = 0; i < urls.length; i++) {
                var url = urls[i];
                console.log("Preload: " + url);
                this.load.setBaseURL(PhaserEditor2D.Models.projectUrl);
                this.load.pack("-asset-pack" + i, url);
            }
        };
        ObjectScene.prototype.create = function () {
            this.scale.resize(window.innerWidth, window.innerHeight);
            if (!PhaserEditor2D.Models.isReady()) {
                this._loadingSprite = this.add.text(10, 10, "Loading...", { fill: "#ff0000" });
                return;
            }
            if (this._loadingSprite !== null) {
                this._loadingSprite.destroy();
                delete this._loadingSprite;
            }
            this.initCamera();
            this.initKeyboard();
            this.initSelectionScene();
            var editor = PhaserEditor2D.Editor.getInstance();
            editor.getCreate().createWorld(this.add);
            if (PhaserEditor2D.Models.displayList) {
                editor.sendMessage({
                    method: "GetSelectObjects"
                });
            }
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
        return ObjectScene;
    }(Phaser.Scene));
    PhaserEditor2D.ObjectScene = ObjectScene;
})(PhaserEditor2D || (PhaserEditor2D = {}));
