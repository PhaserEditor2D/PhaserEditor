namespace phasereditor2d.ui.ide.editors.scene {


    export class AngleTool extends InteractiveTool {
        static RADIUS = 100;

        private _color: number;
        private _handlerShape: Phaser.GameObjects.Ellipse;
        private _handlerShapeBorder: Phaser.GameObjects.Ellipse;
        private _centerShape: Phaser.GameObjects.Arc;
        private _dragging = false;
        private _cursorStartX: number;
        private _cursorStartY: number;

        constructor() {
            super();

            this._color = 0xaaaaff;

            this._handlerShapeBorder = this.createEllipseShape();
            this._handlerShapeBorder.setFillStyle(0, 0);
            this._handlerShapeBorder.setStrokeStyle(4, 0);
            this._handlerShapeBorder.setSize(AngleTool.RADIUS * 2, AngleTool.RADIUS * 2);

            this._handlerShape = this.createEllipseShape();
            this._handlerShape.setFillStyle(0, 0);
            this._handlerShape.setStrokeStyle(2, this._color);
            this._handlerShape.setSize(AngleTool.RADIUS * 2, AngleTool.RADIUS * 2);


            this._centerShape = this.createCircleShape();
            this._centerShape.setFillStyle(this._color);
        }

        canEdit(obj: any): boolean {
            return obj.angle !== undefined;
        }

        render(objects: Phaser.GameObjects.Sprite[]) {
            const cam = Editor.getInstance().getObjectScene().cameras.main;
            const pos = new Phaser.Math.Vector2(0, 0);

            for (let sprite of objects) {
                const worldXY = new Phaser.Math.Vector2();
                const worldTx = sprite.getWorldTransformMatrix();

                worldTx.transformPoint(0, 0, worldXY);

                pos.add(worldXY);
            }

            const len = this.getObjects().length;

            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;

            this._handlerShape.setPosition(cameraX, cameraY);
            this._handlerShape.visible = true;
            this._handlerShapeBorder.setPosition(cameraX, cameraY);
            this._handlerShapeBorder.visible = true;

            this._centerShape.setPosition(cameraX, cameraY);
            this._centerShape.visible = true;
        }

        containsPointer() {
            const pointer = this.getToolPointer();

            const d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);

            return Math.abs(d - AngleTool.RADIUS) <= 10;
        }

        clear() {
            this._handlerShape.visible = false;
            this._handlerShapeBorder.visible = false;

            this._centerShape.visible = false;
        }

        isEditing() {
            return this._dragging;
        }

        onMouseDown() {
            if (this.containsPointer()) {

                this._dragging = true;

                this.requestRepaint = true;

                this._handlerShape.strokeColor = 0xffffff;
                this._centerShape.setFillStyle(0xffffff);

                this._cursorStartX = this.getToolPointer().x;
                this._cursorStartY = this.getToolPointer().y;

                for (let obj of this.getObjects()) {
                    const sprite = <Phaser.GameObjects.Sprite>obj;
                    obj.setData("AngleTool", {
                        initAngle: sprite.angle
                    });
                }
            }
        }

        onMouseMove() {
            if (!this._dragging) {
                return;
            }

            const pointer = this.getToolPointer();
            const cursorX = pointer.x;
            const cursorY = pointer.y;

            const dx = this._cursorStartX - cursorX;
            const dy = this._cursorStartY - cursorY;

            if (Math.abs(dx) < 1 || Math.abs(dy) < 1) {
                return;
            }

            this.requestRepaint = true;

            for (let obj of this.getObjects()) {
                const sprite = <Phaser.GameObjects.Sprite>obj;
                const data = obj.data.get("AngleTool");

                const deltaRadians = angleBetweenTwoPointsWithFixedPoint(
                    cursorX, cursorY,
                    this._cursorStartX, this._cursorStartY,
                    this._centerShape.x, this._centerShape.y
                );

                const deltaAngle = Phaser.Math.RadToDeg(deltaRadians);
                sprite.angle = data.initAngle + deltaAngle;
            }
        }

        onMouseUp() {

            if (this._dragging) {
                const msg = BuildMessage.SetTransformProperties(this.getObjects());
                Editor.getInstance().sendMessage(msg);
            }

            this._dragging = false;

            this._handlerShape.strokeColor = this._color;
            this._centerShape.setFillStyle(this._color);

            this.requestRepaint = true;
        }

    }

    function angleBetweenTwoPointsWithFixedPoint(point1X: number, point1Y: number, point2X: number, point2Y: number,
        fixedX: number, fixedY: number) {

        const angle1 = Math.atan2(point1Y - fixedY, point1X - fixedX);
        const angle2 = Math.atan2(point2Y - fixedY, point2X - fixedX);

        return angle1 - angle2;
    }
}