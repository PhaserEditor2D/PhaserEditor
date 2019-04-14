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

        contains(x: number, y: number): boolean {
            return false;
        }

        render() {

        }

        onMouseDown() {

        }

        onMouseUp() {

        }

        onMouseMove() {

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

        render() {

            const list = this.getObjects();

            if (list.length === 0) {
                this._shape.visible = false;
                return;
            }

            const shapePos = new Phaser.Math.Vector2();

            for (let obj of list) {
                let sprite = <Phaser.GameObjects.TileSprite>obj;

                let localLeft = -sprite.width * sprite.originX;
                let localTop = -sprite.height * sprite.originY;

                let worldXY = new Phaser.Math.Vector2();
                let worldTx = sprite.getWorldTransformMatrix();

                let localX = this._changeX ? localLeft + sprite.width : localLeft + sprite.width / 2;
                let localY = this._changeY ? localTop + sprite.height : localTop + sprite.height / 2;

                worldTx.transformPoint(localX, localY, worldXY);

                shapePos.add(worldXY);
            }

            const len = this.getObjects().length;
            const cam = Editor.getInstance().getObjectScene().cameras.main;

            shapePos.x = (shapePos.x / len - cam.scrollX) * cam.zoom;
            shapePos.y = (shapePos.y / len - cam.scrollY) * cam.zoom;

            this._shape.setPosition(shapePos.x, shapePos.y);
            this._shape.visible = true;
        }

    }
}