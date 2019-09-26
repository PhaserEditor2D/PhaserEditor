namespace phasereditor2d.ui.ide.editors.scene {
    export class TileSizeTool extends InteractiveTool {

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

            this._handlerShape = this.createRectangleShape();
            this._handlerShape.setFillStyle(this._color);
        }

        canEdit(obj: any): boolean {
            return obj instanceof Phaser.GameObjects.TileSprite;
        }

        clear() {
            this._handlerShape.visible = false;
        }

        render(list: Phaser.GameObjects.TileSprite[]) {
            const pos = new Phaser.Math.Vector2(0, 0);
            let angle = 0;

            for (let obj of list) {
                let sprite = <Phaser.GameObjects.TileSprite>obj;

                angle += this.objectGlobalAngle(sprite);

                const width = sprite.width;
                const height = sprite.height;

                let x = -width * sprite.originX;
                let y = -height * sprite.originY;

                let worldXY = new Phaser.Math.Vector2();
                let worldTx = sprite.getWorldTransformMatrix();

                const localX = this._changeX ? x + width : x + width / 2;
                const localY = this._changeY ? y + height : y + height / 2;

                worldTx.transformPoint(localX, localY, worldXY);

                pos.add(worldXY);
            }

            const len = this.getObjects().length;
            const cam = Editor.getInstance().getObjectScene().cameras.main;

            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;

            this._handlerShape.setPosition(cameraX, cameraY);
            this._handlerShape.angle = angle / len + (this._changeX && !this._changeY ? -90 : 0);
            this._handlerShape.visible = true;
        }


        containsPointer() {
            const toolPointer = this.getToolPointer();

            const d = Phaser.Math.Distance.Between(toolPointer.x, toolPointer.y, this._handlerShape.x, this._handlerShape.y);

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

                const worldTx = new Phaser.GameObjects.Components.TransformMatrix();

                const shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);

                for (let obj of this.getObjects()) {
                    const sprite = <Phaser.GameObjects.TileSprite>obj;

                    const initLocalPos = new Phaser.Math.Vector2();
                    sprite.getWorldTransformMatrix(worldTx);
                    worldTx.applyInverse(shapePos.x, shapePos.y, initLocalPos);

                    sprite.setData("TileSizeTool", {
                        initWidth: sprite.width,
                        initHeight: sprite.height,
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

            const worldTx = new Phaser.GameObjects.Components.TransformMatrix();

            for (let obj of this.getObjects()) {
                const sprite = <Phaser.GameObjects.TileSprite>obj;
                const data = sprite.data.get("TileSizeTool");
                const initLocalPos: Phaser.Math.Vector2 = data.initLocalPos;

                const localPos = new Phaser.Math.Vector2();
                sprite.getWorldTransformMatrix(worldTx);
                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);

                const flipX = sprite.flipX ? -1 : 1;
                const flipY = sprite.flipY ? -1 : 1;

                const dx = (localPos.x - initLocalPos.x) * flipX;
                const dy = (localPos.y - initLocalPos.y) * flipY;

                let width = (data.initWidth + dx) | 0;
                let height = (data.initHeight + dy) | 0;

                width = this.snapValueX(width);
                height = this.snapValueY(height);

                if (this._changeX) {
                    sprite.setSize(width, sprite.height);
                }

                if (this._changeY) {
                    sprite.setSize(sprite.width, height);
                }
            }
        }

        onMouseUp() {

            if (this._dragging) {
                const msg = BuildMessage.SetTileSpriteProperties(this.getObjects());
                Editor.getInstance().sendMessage(msg);
            }

            this._dragging = false;

            this._handlerShape.setFillStyle(this._color);

            this.requestRepaint = true;
        }

    }
}