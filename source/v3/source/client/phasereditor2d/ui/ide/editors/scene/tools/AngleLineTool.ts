/// <reference path="./Tools.ts" />


namespace phasereditor2d.ui.ide.editors.scene {

    export class AngleLineTool extends InteractiveTool {

        private _shape: Phaser.GameObjects.Line;
        private _shapeBorder: Phaser.GameObjects.Rectangle;
        private _start: boolean;
        private _angleTool: AngleTool;
        private _color : number;

        constructor(angleTool: AngleTool, start: boolean) {
            super();

            this._angleTool = angleTool;
            this._start = start;

            this._color = 0xaaaaff;
            this._shapeBorder = this.toolScene.add.rectangle(0, 0, AngleTool.RADIUS, 4);
            this._shapeBorder.setFillStyle(0);
            this._shapeBorder.setOrigin(0, 0.5);
            this._shapeBorder.depth = -1;

            this._shape = this.createLineShape();
            this._shape.setStrokeStyle(2, this._color);
            this._shape.setOrigin(0, 0);
            this._shape.setTo(0, 0, AngleTool.RADIUS, 0);
            this._shape.depth = -1;
        }

        clear() {
            this._shape.visible = false;
            this._shapeBorder.visible = false;
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
            this._shapeBorder.setPosition(cameraX, cameraY);
            this._shapeBorder.angle = this._shape.angle;
            this._shapeBorder.visible = this._shape.visible;
        }

        onMouseDown() {
            this._shape.strokeColor = 0xffffff;
        }

        onMouseUp() {
            this._shape.strokeColor = this._color;
        }

    }
}