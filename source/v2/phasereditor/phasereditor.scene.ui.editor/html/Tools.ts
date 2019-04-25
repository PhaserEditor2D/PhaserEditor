namespace PhaserEditor2D {

    export abstract class InteractiveTool {

        protected toolScene: ToolScene = Editor.getInstance().getToolScene();

        constructor() {
        }

        abstract canEdit(obj: any): boolean;

        getObjects(): Phaser.GameObjects.GameObject[] {
            const sel = this.toolScene.getSelectedObjects();
            return sel.filter(obj => this.canEdit(obj));
        }

        containsPointer(): boolean {
            return false;
        }

        clear() {

        }

        update() {
            const list = this.getObjects();

            if (list.length === 0) {
                this.clear();
            } else {
                this.render(list);
            }
        }

        render(objects: Phaser.GameObjects.GameObject[]) {

        }

        onMouseDown() {

        }

        onMouseUp() {

        }

        onMouseMove() {

        }

        protected getPointer() {
            return this.toolScene.input.activePointer;
        }
    }

    export class TileSizeTool extends InteractiveTool {

        private _shape: Phaser.GameObjects.Rectangle;
        private _changeX: boolean;
        private _changeY: boolean;

        constructor(changeX: boolean, changeY: boolean) {
            super();

            this._changeX = changeX;
            this._changeY = changeY;

            this._shape = this.toolScene.add.rectangle(0, 0, 12, 12, 0xff0000);
        }

        canEdit(obj: any): boolean {
            return obj instanceof Phaser.GameObjects.TileSprite;
        }

        clear() {
            this._shape.visible = false;
        }

        render(list: Phaser.GameObjects.TileSprite[]) {
            this._worldPos.set(0, 0);

            for (let obj of list) {
                let sprite = <Phaser.GameObjects.TileSprite>obj;

                let localLeft = -sprite.width * sprite.originX;
                let localTop = -sprite.height * sprite.originY;

                let worldXY = new Phaser.Math.Vector2();
                let worldTx = sprite.getWorldTransformMatrix();

                let localX = this._changeX ? localLeft + sprite.width : localLeft + sprite.width / 2;
                let localY = this._changeY ? localTop + sprite.height : localTop + sprite.height / 2;

                worldTx.transformPoint(localX, localY, worldXY);

                this._worldPos.add(worldXY);
            }

            const len = this.getObjects().length;
            const cam = Editor.getInstance().getObjectScene().cameras.main;

            const cameraX = (this._worldPos.x / len - cam.scrollX) * cam.zoom;
            const cameraY = (this._worldPos.y / len - cam.scrollY) * cam.zoom;

            this._shape.setPosition(cameraX, cameraY);
            this._shape.visible = true;
        }


        containsPointer() {
            const pointer = this.getPointer();

            const d = this._worldPos.distance(new Phaser.Math.Vector2(pointer.worldX, pointer.worldY));

            return d <= this._shape.width / 2;
        }

        private _dragging = false;
        private _worldPos = new Phaser.Math.Vector2();
        private _initWorldPos = new Phaser.Math.Vector2();

        onMouseDown() {
            if (this.containsPointer()) {
                this._dragging = true;

                this._initWorldPos.setFromObject(this._initWorldPos);

                const worldTx = new Phaser.GameObjects.Components.TransformMatrix();

                for (let obj of this.getObjects()) {
                    let sprite = <Phaser.GameObjects.TileSprite>obj;

                    const initLocalPos = new Phaser.Math.Vector2();
                    sprite.getWorldTransformMatrix(worldTx);
                    worldTx.applyInverse(this._initWorldPos.x, this._initWorldPos.y, initLocalPos);

                    sprite.setData("TileSizeTool", {
                        initWidth: sprite.width,
                        initHeight: sprite.height,
                        initLocalPos: initLocalPos
                    });

                }
            }
        }

    }

    export class ToolFactory {

        static createByName(name: string): InteractiveTool[] {
            switch (name) {
                case "TileSize":
                    return [
                        new TileSizeTool(true, false),
                        new TileSizeTool(false, true),
                        new TileSizeTool(true, true)
                    ]
            }

            return [];
        }
    }
}