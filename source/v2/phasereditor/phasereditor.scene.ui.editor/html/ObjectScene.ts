namespace PhaserEditor2D {

    export class ObjectScene extends Phaser.Scene {            

        private _toolScene: ToolScene;
        private _dragManager: DragManager;
        private _backgroundScene: Phaser.Scene;

        constructor() {
            super("ObjectScene");
        }

        preload() {
            console.log("preload()");
            this.load.setBaseURL(Models.projectUrl);
            this.load.pack("pack", Models.pack);
        }

        create() {
            Editor.getInstance().stop();

            this._dragManager = new DragManager(this);

            this.initCamera();

            this.initKeyboard();

            this.initSelectionScene();

            const editor = Editor.getInstance();

            this.initBackground();

            editor.getCreate().createWorld(this.add);

            editor.sendMessage({
                method: "GetSelectObjects"
            });

            this.initBackground();

            editor.repaint();

        }

        private initBackground() {
            this.scene.launch("BackgroundScene");
            this._backgroundScene = this.scene.get("BackgroundScene");
            this.scene.moveDown("BackgroundScene");
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

        private cameraZoom = function (delta: number) {
            var cam = this.cameras.main;

            if (delta < 0) {
                cam.zoom *= 1.1;
            } else {
                cam.zoom *= 0.9;
            }
        }

        private cameraPan = function (dx: number, dy: number) {
            var cam = this.cameras.main;

            cam.scrollX += dx * 30 / cam.zoom;
            cam.scrollY += dy * 30 / cam.zoom;
        }

        getToolScene() {
            return this._toolScene;
        }

        onMouseWheel(e: any) {
            var cam = this.cameras.main;

            var delta: number = e.wheelDelta;

            var zoom = (delta < 0 ? 0.9 : 1.1);

            cam.zoom *= zoom;

        }

        performResize() {
            this.scale.resize(window.innerWidth, window.innerHeight);
            this._backgroundScene.scale.resize(window.innerWidth, window.innerHeight);
        }
    }

    class DragManager {
        private _scene: ObjectScene;
        private _dragStartPoint: Phaser.Math.Vector2;
        private _dragStartCameraScroll: Phaser.Math.Vector2;

        constructor(scene: ObjectScene) {
            this._scene = scene;
            this._dragStartPoint = null;

            const self = this;


            scene.game.canvas.addEventListener("mousedown", function (e: MouseEvent) {
                self.pointerDown(e);
            })

            scene.game.canvas.addEventListener("mousemove", function (e: MouseEvent) {
                self.pointerMove(e);
            })

            scene.game.canvas.addEventListener("mouseup", function () {
                self.pointerUp();
            })

            scene.game.canvas.addEventListener("mouseleave", function () {
                self.pointerUp();
            })
        }

        private pointerDown(e: MouseEvent) {
            // if middle button peressed
            if (e.buttons === 4) {
                this._dragStartPoint = new Phaser.Math.Vector2(e.clientX, e.clientY);
                const cam = this._scene.cameras.main;
                this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);

                e.preventDefault();
            }

        }

        private pointerMove(e: MouseEvent) {
            if (this._dragStartPoint === null) {
                return;
            }

            const dx = this._dragStartPoint.x - e.clientX;
            const dy = this._dragStartPoint.y - e.clientY;

            const cam = this._scene.cameras.main;

            cam.scrollX = this._dragStartCameraScroll.x + dx / cam.zoom;
            cam.scrollY = this._dragStartCameraScroll.y + dy / cam.zoom;

            Editor.getInstance().repaint();

            e.preventDefault();
        }

        private pointerUp() {
            this._dragStartPoint = null;
            this._dragStartCameraScroll = null;
        }
    }


    export class BackgroundScene extends Phaser.Scene {
        private _bg: Phaser.GameObjects.Graphics;

        constructor() {
            super("BackgroundScene");
        }

        create() {
            this._bg = this.add.graphics();
        }

        update() {
            this._bg.clear();
            const bgColor = Phaser.Display.Color.RGBStringToColor("rgb(" + Models.sceneProperties.backgroundColor + ")");
            this._bg.fillStyle(bgColor.color, 1);
            this._bg.fillRect(0, 0, window.innerWidth, window.innerHeight);
        }
    }
}