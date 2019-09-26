namespace phasereditor2d.ui.ide.editors.scene {

    export class PositionTool extends InteractiveTool implements SpotTool {
        private _handlerShape: Phaser.GameObjects.Shape;
        private _changeX: boolean;
        private _changeY: boolean;
        private _dragging = false;
        private _color: number;
        private _startCursor: Phaser.Math.Vector2;
        private _arrowPoint: Phaser.Math.Vector2;
        private _centerPoint: Phaser.Math.Vector2;
        private _startVector: Phaser.Math.Vector2;

        constructor(changeX: boolean, changeY: boolean) {
            super();

            this._changeX = changeX;
            this._changeY = changeY;

            if (changeX && changeY) {
                this._color = 0xffff00;
            } else if (changeX) {
                this._color = 0xff0000;
            } else {
                this._color = 0x00ff00;
            }

            this._handlerShape = changeX && changeY ? this.createRectangleShape() : this.createArrowShape();
            this._handlerShape.setFillStyle(this._color);
        }

        canEdit(obj: any): boolean {
            return obj.x !== null;
        }

        getX() {
            return this._handlerShape.x;
        }

        getY() {
            return this._handlerShape.y;
        }

        clear() {
            this._handlerShape.visible = false;
        }

        render(list: Phaser.GameObjects.Sprite[]) {
            const cam = Editor.getInstance().getObjectScene().cameras.main;

            const pos = new Phaser.Math.Vector2(0, 0);
            let angle = 0;
            const localCoords = Editor.getInstance().isTransformLocalCoords();
            const globalCenterXY = new Phaser.Math.Vector2();

            for (let sprite of list) {
                const worldTx = sprite.getWorldTransformMatrix();

                const centerXY = new Phaser.Math.Vector2();
                worldTx.transformPoint(0, 0, centerXY)
                globalCenterXY.add(centerXY);

                const worldXY = new Phaser.Math.Vector2();

                let localX = 0;
                let localY = 0;

                if (localCoords) {

                    if (!this._changeX || !this._changeY) {
                        if (this._changeX) {
                            localX += ARROW_LENGTH / cam.zoom / sprite.scaleX;
                        } else {
                            localY += ARROW_LENGTH / cam.zoom / sprite.scaleY;
                        }
                    }

                    angle += this.objectGlobalAngle(sprite);

                    worldTx.transformPoint(localX, localY, worldXY);

                } else {

                    if (!this._changeX || !this._changeY) {
                        worldTx.transformPoint(0, 0, worldXY);
                        if (this._changeX) {
                            worldXY.x += ARROW_LENGTH / cam.zoom;
                        } else {
                            worldXY.y += ARROW_LENGTH / cam.zoom;
                        }
                    } else {
                        worldTx.transformPoint(0, 0, worldXY);
                    }

                }

                pos.add(worldXY);
            }

            const len = this.getObjects().length;

            pos.x /= len;
            pos.y /= len;

            this._arrowPoint = new Phaser.Math.Vector2(pos.x, pos.y);
            this._centerPoint = new Phaser.Math.Vector2(globalCenterXY.x / len, globalCenterXY.y / len);

            const cameraX = (pos.x - cam.scrollX) * cam.zoom;
            const cameraY = (pos.y - cam.scrollY) * cam.zoom;

            this._handlerShape.setPosition(cameraX, cameraY);
            this._handlerShape.angle = angle / len;
            if (this._changeX) {
                this._handlerShape.angle -= 90;
            }
            this._handlerShape.visible = true;
        }

        containsPointer() {
            const pointer = this.getToolPointer();

            const d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);

            return d <= this._handlerShape.width;
        }

        isEditing() {
            return this._dragging;
        }

        onMouseDown() {
            if (this.containsPointer()) {

                this._dragging = true;

                this.requestRepaint = true;

                const pointer = this.getToolPointer();

                this._startCursor = this.getScenePoint(pointer.x, pointer.y);

                this._handlerShape.setFillStyle(0xffffff);

                this._startVector = new Phaser.Math.Vector2(this._arrowPoint.x - this._centerPoint.x, this._arrowPoint.y - this._centerPoint.y);

                let p = new Phaser.Math.Vector2();

                for (let obj of this.getObjects()) {
                    const sprite = <Phaser.GameObjects.TileSprite>obj;
                    const tx = sprite.getWorldTransformMatrix();
                    tx.applyInverse(0, 0, p);

                    sprite.setData("PositionTool", {
                        initX: sprite.x,
                        initY: sprite.y,
                        initWorldTx: tx
                    });

                }
            }
        }

        onMouseMove() {
            if (!this._dragging) {
                return;
            }

            this.requestRepaint = true;

            const pointer = this.getToolPointer();

            const endCursor = this.getScenePoint(pointer.x, pointer.y);

            const localCoords = Editor.getInstance().isTransformLocalCoords();
            const changeXY = this._changeX && this._changeY;

            for (let obj of this.getObjects()) {
                const sprite = <Phaser.GameObjects.Sprite>obj;
                const data = sprite.data.get("PositionTool");

                const p0 = new Phaser.Math.Vector2();
                const p1 = new Phaser.Math.Vector2();

                if (sprite.parentContainer) {
                    const tx = sprite.parentContainer.getWorldTransformMatrix();
                    tx.transformPoint(this._startCursor.x, this._startCursor.y, p0);
                    tx.transformPoint(endCursor.x, endCursor.y, p1);
                } else {
                    p0.setFromObject(this._startCursor);
                    p1.setFromObject(endCursor);
                }

                let x: number;
                let y: number;

                if (changeXY) {
                    const dx = p1.x - p0.x;
                    const dy = p1.y - p0.y;

                    x = data.initX + dx;
                    y = data.initY + dy;
                } else {
                    const vector = new Phaser.Math.Vector2(this._changeX ? 1 : 0, this._changeY ? 1 : 0);

                    if (localCoords) {
                        const tx = new Phaser.GameObjects.Components.TransformMatrix();
                        tx.rotate(sprite.rotation);
                        tx.transformPoint(vector.x, vector.y, vector);
                    }

                    const moveVector = new Phaser.Math.Vector2(endCursor.x - this._startCursor.x, endCursor.y - this._startCursor.y);

                    const d = moveVector.dot(this._startVector) / this._startVector.length();

                    vector.x *= d;
                    vector.y *= d;

                    x = data.initX + vector.x;
                    y = data.initY + vector.y;
                }

                x = this.snapValueX(x);
                y = this.snapValueY(y);

                sprite.setPosition(x, y);
            }
        }

        onMouseUp() {

            if (this._dragging) {
                const msg = BuildMessage.SetTransformProperties(this.getObjects());
                Editor.getInstance().sendMessage(msg);
            }

            this._dragging = false;

            this._handlerShape.setFillStyle(this._color);

            this.requestRepaint = true;
        }
    }
}