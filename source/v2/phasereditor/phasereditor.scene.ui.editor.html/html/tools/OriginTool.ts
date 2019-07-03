namespace PhaserEditor2D {
    export class OriginTool extends InteractiveTool implements SpotTool {

        private _handlerShape: Phaser.GameObjects.Shape;
        private _changeX: boolean;
        private _changeY: boolean;
        private _dragging = false;
        private _color: number;

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

            this._handlerShape = changeX && changeY ? this.createCircleShape() : this.createArrowShape();
            this._handlerShape.setFillStyle(this._color);
        }

        canEdit(obj: any): boolean {
            return obj.hasOwnProperty("originX");
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

        render(list: Phaser.GameObjects.GameObject[]) {
            const cam = Editor.getInstance().getObjectScene().cameras.main;
            const pos = new Phaser.Math.Vector2(0, 0);
            let angle = 0;

            for (let obj of list) {
                const sprite = <Phaser.GameObjects.Sprite>obj;

                const worldXY = new Phaser.Math.Vector2();
                const worldTx = sprite.getWorldTransformMatrix();

                let localX = 0;
                let localY = 0;

                const scale = this.objectGlobalScale(sprite);

                if (!this._changeX || !this._changeY) {
                    if (this._changeX) {
                        localX += ARROW_LENGTH / scale.x / cam.zoom * (sprite.flipX ? -1 : 1);
                        if (sprite.flipX) {
                            angle += 180;
                        }
                    } else {
                        localY += ARROW_LENGTH / scale.y / cam.zoom * (sprite.flipY ? -1 : 1);
                        if (sprite.flipY) {
                            angle += 180;
                        }
                    }
                }

                angle += this.objectGlobalAngle(sprite);

                worldTx.transformPoint(localX, localY, worldXY);

                pos.add(worldXY);
            }

            const len = this.getObjects().length;

            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;

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

                this._handlerShape.setFillStyle(0xffffff);

                const shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);

                for (let obj of this.getObjects()) {
                    const sprite = <Phaser.GameObjects.Sprite>obj;

                    const initLocalPos = new Phaser.Math.Vector2();
                    const worldTx = new Phaser.GameObjects.Components.TransformMatrix();
                    sprite.getWorldTransformMatrix(worldTx);
                    worldTx.applyInverse(shapePos.x, shapePos.y, initLocalPos);

                    obj.setData("OriginTool", {
                        initOriginX: sprite.originX,
                        initOriginY: sprite.originY,
                        initX: sprite.x,
                        initY: sprite.y,
                        initWorldTx: worldTx,
                        initLocalPos: initLocalPos
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

            const pointerPos = this.getScenePoint(pointer.x, pointer.y);

            for (let obj of this.getObjects()) {
                const sprite = <Phaser.GameObjects.Sprite>obj;
                const data = obj.data.get("OriginTool");
                const initLocalPos: Phaser.Math.Vector2 = data.initLocalPos;

                const flipX = sprite.flipX ? -1 : 1;
                const flipY = sprite.flipY ? -1 : 1;

                const localPos = new Phaser.Math.Vector2();
                const worldTx: Phaser.GameObjects.Components.TransformMatrix = data.initWorldTx;
                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);

                const dx = (localPos.x - initLocalPos.x) * flipX;
                const dy = (localPos.y - initLocalPos.y) * flipY;

                const width = sprite.width;
                const height = sprite.height;

                const originDX = dx / width;
                const originDY = dy / height;

                consoleLog("---");
                consoleLog("width " + width);
                consoleLog("dx " + dx);
                consoleLog("originDX " + originDX);

                const newOriginX = data.initOriginX + (this._changeX ? originDX : 0);
                const newOriginY = data.initOriginY + (this._changeY ? originDY : 0);

                // restore position

                const local1 = new Phaser.Math.Vector2(data.initOriginX * width, data.initOriginY * height);
                const local2 = new Phaser.Math.Vector2(newOriginX * width, newOriginY * height);

                const parent1 = this.localToParent(sprite, local1);
                const parent2 = this.localToParent(sprite, local2);

                const dx2 = parent2.x - parent1.x;
                const dy2 = parent2.y - parent1.y;

                sprite.x = data.initX + dx2 * flipX;
                sprite.y = data.initY + dy2 * flipY;
                sprite.setOrigin(newOriginX, newOriginY);
            }

        }

        onMouseUp() {

            if (this._dragging) {
                const msg = BuildMessage.SetOriginProperties(this.getObjects());
                Editor.getInstance().sendMessage(msg);
            }

            this._dragging = false;

            this._handlerShape.setFillStyle(this._color);

            this.requestRepaint = true;
        }
    }
}