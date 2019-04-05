namespace PhaserEditor2D {

    export class ObjectScene extends Phaser.Scene {
        private _toolScene: ToolScene;
        private _dragManager: DragManager;

        constructor() {
            super("ObjectScene");
        }

        preload() {
            console.log("preload()");
            this.load.setBaseURL(Models.projectUrl);
            this.load.pack("pack", Models.pack);
        }

        create() {
            this._dragManager = new DragManager(this);

            this.initCamera();

            this.initKeyboard();

            this.initMouse();

            this.initSelectionScene();

            const editor = Editor.getInstance();

            editor.getCreate().createWorld(this.add);

            editor.sendMessage({
                method: "GetSelectObjects"
            });
        }

        update() {
            this._dragManager.update();
        }

        private initMouse() {
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
    }

    class DragManager {
        private _scene: ObjectScene;
        private _dragStartPoint: Phaser.Math.Vector2;
        private _dragStartCameraScroll: Phaser.Math.Vector2;
        private _spaceKey: Phaser.Input.Keyboard.Key;

        constructor(scene: ObjectScene) {
            this._scene = scene;
            this._dragStartPoint = null;

            this._scene.input.on(Phaser.Input.Events.POINTER_DOWN, this.pointerDown, this);
            this._scene.input.on(Phaser.Input.Events.POINTER_MOVE, this.pointerMove, this);
            this._scene.input.on(Phaser.Input.Events.POINTER_UP, this.pointerUp, this);
            this._scene.input.on(Phaser.Input.Events.POINTER_UP_OUTSIDE, this.pointerUp, this);
        }

        private pointerDown() {

            const pointer = this._scene.input.activePointer;

            if (pointer.middleButtonDown()) {
                this._dragStartPoint = new Phaser.Math.Vector2(pointer.x, pointer.y);
                const cam = this._scene.cameras.main;
                this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);
            }

        }

        private pointerMove() {
            if (this._dragStartPoint == null) {
                return;
            }

            const pointer = this._scene.input.activePointer;

            const dx = this._dragStartPoint.x - pointer.x;
            const dy = this._dragStartPoint.y - pointer.y;

            const cam = this._scene.cameras.main;

            cam.scrollX = this._dragStartCameraScroll.x + dx / cam.zoom;
            cam.scrollY = this._dragStartCameraScroll.y + dy / cam.zoom;
        }

        private pointerUp() {
            this._dragStartPoint = null;
            this._dragStartCameraScroll = null;
        }

        private isDragging() {
            return this._dragStartPoint !== null;
        }

        update() {
            const pointer = this._scene.input.activePointer;

            if (this.isDragging() && pointer.isDown) {
                const cam = this._scene.cameras.main;
                cam.scrollX += pointer.movementX;
                cam.scrollY += pointer.movementY;
            } else {
                if (this.isDragging()) {
                    this.pointerUp();
                }
            }
        }
    }
}