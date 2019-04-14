namespace PhaserEditor2D {

    export class ObjectScene extends Phaser.Scene {

        private _toolScene: ToolScene;
        private _dragManager: DragManager;
        private _backgroundScene: Phaser.Scene;
        private _initData: any;

        constructor() {
            super("ObjectScene");
        }

        init(initData) {
            this._initData = initData;
        }

        preload() {
            console.log("preload()");
            this.load.setBaseURL(this._initData.projectUrl);
            this.load.pack("pack", this._initData.pack);
        }

        create() {
            Editor.getInstance().stop();

            this._dragManager = new DragManager(this);

            new DropManager();

            this.initCamera();

            this.initSelectionScene();

            const editor = Editor.getInstance();

            this.initBackground();

            editor.getCreate().createWorld(this, this._initData.displayList);

            editor.sendMessage({
                method: "GetSelectObjects"
            });

            editor.sendMessage({
                method: "GetCameraState"
            });

            this.initBackground();

            editor.repaint();

        }

        removeAllObjects() {
            let list = this.sys.displayList.list;
            for (let obj of list) {
                obj.destroy();
            }
            this.sys.displayList.removeAll(false);
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

        private initCamera() {
            var cam = this.cameras.main;
            cam.setOrigin(0, 0);
            cam.setRoundPixels(true);

            this.scale.resize(window.innerWidth, window.innerHeight);
        };

        getToolScene() {
            return this._toolScene;
        }

        getBackgroundScene() {
            return this._backgroundScene;
        }

        onMouseWheel(e: WheelEvent) {
            var cam = this.cameras.main;

            var delta: number = e.deltaY;

            var zoom = (delta > 0 ? 0.9 : 1.1);

            cam.zoom *= zoom;

            this.sendRecordCameraStateMessage();

        }

        sendRecordCameraStateMessage() {
            let cam = this.cameras.main;
            Editor.getInstance().sendMessage({
                method: "RecordCameraState",
                cameraState: {
                    scrollX: cam.scrollX,
                    scrollY: cam.scrollY,
                    zoom: cam.zoom
                }
            });
        }

        performResize() {
            this.cameras.main.setSize(window.innerWidth, window.innerHeight);
            this._backgroundScene.cameras.main.setSize(window.innerWidth, window.innerHeight);
        }
    }

    export class BackgroundScene extends Phaser.Scene {
        private _bg: Phaser.GameObjects.Graphics;

        constructor() {
            super("BackgroundScene");
        }

        create() {
            this._bg = this.add.graphics();
            this.repaint();
        }

        private repaint() {
            this._bg.clear();
            const bgColor = Phaser.Display.Color.RGBStringToColor("rgb(" + ScenePropertiesComponent.get_backgroundColor(Editor.getInstance().sceneProperties) + ")");
            this._bg.fillStyle(bgColor.color, 1);
            this._bg.fillRect(0, 0, window.innerWidth, window.innerHeight);
        }

        update() {
            this.repaint();
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

            if (this._dragStartPoint !== null) {
                this._scene.sendRecordCameraStateMessage();
            }

            this._dragStartPoint = null;
            this._dragStartCameraScroll = null;
        }
    }

    class DropManager {

        constructor() {
            window.addEventListener("drop", function (e) {
                let editor = Editor.getInstance();

                let point = editor.getObjectScene().cameras.main.getWorldPoint(e.clientX, e.clientY);

                editor.sendMessage({
                    method: "DropEvent",
                    x: point.x,
                    y: point.y
                });
            })

            window.addEventListener("dragover", function (e: DragEvent) {
                e.preventDefault();
            });
        }
    }



}