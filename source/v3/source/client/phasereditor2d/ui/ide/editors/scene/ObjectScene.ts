namespace phasereditor2d.ui.ide.editors.scene {

    export class ObjectScene extends Phaser.Scene {

        private _toolScene: ToolScene;
        private _dragCameraManager: DragCameraManager;
        private _dragObjectsManager: DragObjectsManager;
        private _pickManager: PickObjectManager;
        private _initData: any;

        constructor() {
            super("ObjectScene");
        }

        init(initData) {
            this._initData = initData;
        }

        preload() {
            consoleLog("preload()");
            this.load.setBaseURL(this._initData.projectUrl);
            this.load.pack("pack", this._initData.pack);
        }

        create() {
            const editor = Editor.getInstance();

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
        }

        updateBackground() {
            const rgb = "rgb(" + ScenePropertiesComponent.get_backgroundColor(Editor.getInstance().sceneProperties) + ")";
            this.cameras.main.setBackgroundColor(rgb);
        }

        getPickManager() {
            return this._pickManager;
        }

        getDragCameraManager() {
            return this._dragCameraManager;
        }

        getDragObjectsManager() {
            return this._dragObjectsManager;
        }

        removeAllObjects() {
            let list = this.sys.displayList.list;
            for (let obj of list) {
                obj.destroy();
            }
            this.sys.displayList.removeAll(false);
        }

        getScenePoint(pointerX: number, pointerY: number): any {
            const cam = this.cameras.main;

            const sceneX = pointerX / cam.zoom + cam.scrollX;
            const sceneY = pointerY / cam.zoom + cam.scrollY;

            return new Phaser.Math.Vector2(sceneX, sceneY);
        }


        private initSelectionScene() {
            this.scene.launch("ToolScene");
            this._toolScene = <ToolScene>this.scene.get("ToolScene");
        }

        private initCamera() {
            var cam = this.cameras.main;
            cam.setOrigin(0, 0);
            cam.setRoundPixels(true);

            this.updateBackground();

            this.scale.resize(window.innerWidth, window.innerHeight);
        };

        getToolScene() {
            return this._toolScene;
        }

        onMouseWheel(e: WheelEvent) {
            var cam = this.cameras.main;

            var delta: number = e.deltaY;

            var zoom = (delta > 0 ? 0.9 : 1.1);

            const pointer = this.input.activePointer;

            const point1 = cam.getWorldPoint(pointer.x, pointer.y);

            cam.zoom *= zoom;

            // update the camera matrix
            (<any>cam).preRender(this.scale.resolution);

            const point2 = cam.getWorldPoint(pointer.x, pointer.y);

            const dx = point2.x - point1.x;
            const dy = point2.y - point1.y;

            cam.scrollX += -dx;
            cam.scrollY += -dy;

            this.sendRecordCameraStateMessage();

        }

        sendRecordCameraStateMessage() {
            let cam = this.cameras.main;
            Editor.getInstance().sendMessage({
                method: "RecordCameraState",
                cameraState: {
                    scrollX: cam.scrollX,
                    scrollY: cam.scrollY,
                    width: cam.width,
                    height: cam.height,
                    zoom: cam.zoom
                }
            });
        }

        performResize() {
            this.cameras.main.setSize(window.innerWidth, window.innerHeight);
        }
    }

    class PickObjectManager {

        private _down: MouseEvent;

        onMouseDown(e: MouseEvent) {
            this._down = e;
        }

        onMouseUp(e: MouseEvent) : any {
            if (!this._down || this._down.x !== e.x || this._down.y !== e.y || !isLeftButton(this._down)) {
                return null;
            }

            const editor = Editor.getInstance();

            const scene = editor.getObjectScene();
            const pointer = scene.input.activePointer;


            const result = editor.hitTestPointer(scene, pointer);

            consoleLog(result);

            let gameObj = result.pop();

            editor.sendMessage({
                method: "ClickObject",
                ctrl: e.ctrlKey,
                shift: e.shiftKey,
                id: gameObj ? gameObj.name : undefined
            });

            return gameObj;
        }

        private _temp: Phaser.Math.Vector2[] = [
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0)
        ];

        selectArea(start: Phaser.Math.Vector2, end: Phaser.Math.Vector2) {
            console.log("---");
            const editor = Editor.getInstance();
            const scene = editor.getObjectScene();
            const list = scene.children.getAll();
            let x = start.x;
            let y = start.y;
            let width = end.x - start.x;
            let height = end.y - start.y;

            if (width < 0) {
                x = end.x;
                width = -width;
            }

            if (height < 0) {
                y = end.y;
                height = -height;
            }

            const area = new Phaser.Geom.Rectangle(x, y, width, height);
            const selection = [];
            for (let obj of list) {
                if (obj.name) {
                    const sprite: Phaser.GameObjects.Sprite = <any>obj;
                    const points = this._temp;
                    editor.getWorldBounds(sprite, points);
                    if (
                        area.contains(points[0].x, points[0].y)
                        && area.contains(points[1].x, points[1].y)
                        && area.contains(points[2].x, points[2].y)
                        && area.contains(points[3].x, points[3].y)
                    ) {
                        selection.push(sprite.name);
                    }
                }
            }

            editor.sendMessage({
                method: "SetSelection",
                list: selection
            });
        }
    }

    class DragObjectsManager {

        private _startPoint: Phaser.Math.Vector2;
        private _dragging: boolean;
        private _now: integer;
        private _paintDelayUtil: PaintDelayUtil;

        constructor() {
            this._startPoint = null;
            this._dragging = false;
            this._now = 0;
            this._paintDelayUtil = new PaintDelayUtil();
        }

        private getScene() {
            return Editor.getInstance().getObjectScene();
        }

        private getSelectedObjects() {
            return Editor.getInstance().getToolScene().getSelectedObjects();
        }

        private getPointer() {
            return this.getScene().input.activePointer;
        }

        onMouseDown(e: MouseEvent): boolean {
            if (!isLeftButton(e)) {
                return false;
            }

            const set1 = new Phaser.Structs.Set(Editor.getInstance().hitTestPointer(this.getScene(), this.getPointer()));
            const set2 = new Phaser.Structs.Set(this.getSelectedObjects());
            const hit = set1.intersect(set2).size > 0;

            if (!hit) {
                return false;
            }

            this._paintDelayUtil.startPaintLoop();

            this._startPoint = this.getScene().getScenePoint(this.getPointer().x, this.getPointer().y);

            const tx = new Phaser.GameObjects.Components.TransformMatrix();
            const p = new Phaser.Math.Vector2();

            for (let obj of this.getSelectedObjects()) {
                const sprite: Phaser.GameObjects.Sprite = <any>obj;
                sprite.getWorldTransformMatrix(tx);
                tx.transformPoint(0, 0, p);
                sprite.setData("DragObjectsManager", {
                    initX: p.x,
                    initY: p.y
                });
            }

            return true;
        }

        onMouseMove(e: MouseEvent) {
            if (!isLeftButton(e) || this._startPoint === null) {
                return;
            }

            this._dragging = true;

            const pos = this.getScene().getScenePoint(this.getPointer().x, this.getPointer().y);
            const dx = pos.x - this._startPoint.x;
            const dy = pos.y - this._startPoint.y;

            for (let obj of this.getSelectedObjects()) {
                const sprite: Phaser.GameObjects.Sprite = <any>obj;
                const data = sprite.getData("DragObjectsManager");

                if (!data) {
                    continue;
                }

                const x = Editor.getInstance().snapValueX(data.initX + dx);
                const y = Editor.getInstance().snapValueX(data.initY + dy);

                if (sprite.parentContainer) {
                    const tx = sprite.parentContainer.getWorldTransformMatrix();
                    const p = new Phaser.Math.Vector2();
                    tx.applyInverse(x, y, p);
                    sprite.setPosition(p.x, p.y);
                } else {
                    sprite.setPosition(x, y);
                }
            }

            if (this._paintDelayUtil.shouldPaintThisTime()) {
                Editor.getInstance().repaint();
            }
        }

        onMouseUp() {

            if (this._startPoint !== null && this._dragging) {
                this._dragging = false;
                this._startPoint = null;

                Editor.getInstance().sendMessage(BuildMessage.SetTransformProperties(this.getSelectedObjects()));
            }

            for (let obj of this.getSelectedObjects()) {
                const sprite: Phaser.GameObjects.Sprite = <any>obj;
                if (sprite.data) {
                    sprite.data.remove("DragObjectsManager");
                }
            }

            Editor.getInstance().repaint();
        }
    }

    class DragCameraManager {
        private _scene: ObjectScene;
        private _dragStartPoint: Phaser.Math.Vector2;
        private _dragStartCameraScroll: Phaser.Math.Vector2;

        constructor(scene: ObjectScene) {
            this._scene = scene;
            this._dragStartPoint = null;
        }

        onMouseDown(e: MouseEvent) {
            // if middle button peressed
            if (isMiddleButton(e)) {
                this._dragStartPoint = new Phaser.Math.Vector2(e.clientX, e.clientY);
                const cam = this._scene.cameras.main;
                this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);

                e.preventDefault();
            }

        }

        onMouseMove(e: MouseEvent) {
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

        onMouseUp() {

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