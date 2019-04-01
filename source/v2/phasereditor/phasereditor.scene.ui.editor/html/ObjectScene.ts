namespace PhaserEditor2D {

    export class ObjectScene extends Phaser.Scene {

        private _loadingSprite: Phaser.GameObjects.Text = null;
        private _toolScene: ToolScene;

        constructor() {
            super("ObjectScene");
        }

        preload() {
            if (!Models.isReady()) {
                // we do not load nothing, we open the socket to request the first refresh all.
                console.log("First preload");
                Editor.getInstance().openSocket();
                return;
            }
            console.log("Common preload");

            this.load.reset();

            var urls = Models.packs;

            for (var i = 0; i < urls.length; i++) {
                var url = urls[i];

                console.log("Preload: " + url);

                this.load.setBaseURL(Models.projectUrl);
                this.load.pack("-asset-pack" + i, url);
            }
        }

        create() {
            this.scale.resize(window.innerWidth, window.innerHeight);

            if (!Models.isReady()) {
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

            const editor = Editor.getInstance();

            editor.getCreate().createWorld(this.add);

            if (Models.displayList) {
                editor.sendMessage({
                    method: "GetSelectObjects"
                });
            }
        }

        private initSelectionScene() {
            this.scene.launch("ToolScene");
            this._toolScene = <ToolScene>this.scene.get("ToolScene");
        }

        private initKeyboard() {
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

        private initCamera() {
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

        private cameraZoom = function (delta : number) {
            /** @type {Phaser.Scene} */
            var scene = this;
            var cam = scene.cameras.main;
            if (delta < 0) {
                cam.zoom *= 1.1;
            } else {
                cam.zoom *= 0.9;
            }
        }
        
        private cameraPan = function (dx : number, dy : number) {
            var cam = this.cameras.main;
        
            cam.scrollX += dx * 30 / cam.zoom;
            cam.scrollY += dy * 30 / cam.zoom;
        }

        getToolScene() {
            return this._toolScene;
        }

        onMouseWheel(e : any) {
            var cam = this.cameras.main;
            
            var delta : number = e.wheelDelta;
        
            var zoom = (delta < 0 ? 0.9 : 1.1);
        
            cam.zoom *= zoom;
        
        }
    }
}