namespace PhaserEditor2D {
    export class AngleLineTool extends InteractiveTool {

        private _shape: Phaser.GameObjects.Line;
        private _start: boolean;
        private _angleTool: AngleTool;
        private _color : number;

        constructor(angleTool: AngleTool, start: boolean) {
            super();

            this._angleTool = angleTool;
            this._start = start;

            this._shape = this.createLineShape();

            this._color = Phaser.Display.Color.GetColor(0.5411765 * 255, 0.16862746 * 255, 0.8862745 * 255);

            this._shape.setStrokeStyle(1, this._color);
            this._shape.setOrigin(0, 0);
            this._shape.setTo(0, 0, AngleTool.RADIUS, 0);
        }

        clear() {
            this._shape.visible = false;
        }

        containsPointer() {
            return false;
        }

        canEdit(obj: any): boolean {
            return this._angleTool.canEdit(obj);
        }

        isEditing() {
            return this._angleTool.isEditing();
        }

        render(objects: Phaser.GameObjects.Sprite[]) {
            const cam = Editor.getInstance().getObjectScene().cameras.main;
            const pos = new Phaser.Math.Vector2(0, 0);
            let globalStartAngle = 0;
            let globalEndAngle = 0;

            const localCoords = Editor.getInstance().isTransformLocalCoords();


            for (let sprite of objects) {
                const worldXY = new Phaser.Math.Vector2();
                const worldTx = sprite.getWorldTransformMatrix();

                worldTx.transformPoint(0, 0, worldXY);
                pos.add(worldXY);

                const endAngle = this.objectGlobalAngle(sprite);
                const startAngle = localCoords ? endAngle - sprite.angle : 0;

                globalStartAngle += startAngle;
                globalEndAngle += endAngle;
            }

            const len = this.getObjects().length;

            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;

            globalStartAngle /= len;
            globalEndAngle /= len;

            this._shape.setPosition(cameraX, cameraY);
            this._shape.angle = this._start ? globalStartAngle : globalEndAngle;
            this._shape.visible = true;
        }

        onMouseDown() {
            this._shape.setStrokeStyle(1, 0xffffff);
        }

        onMouseUp() {
            this._shape.setStrokeStyle(1, this._color);
        }

    }
}