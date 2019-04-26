namespace PhaserEditor2D {
    export class TileSizeTool extends InteractiveTool {

        private _handlerShape: Phaser.GameObjects.Rectangle;
        private _changeX: boolean;
        private _changeY: boolean;
        private _dragging = false;
       

        private static FILL_STYLE = 0xff0000;

        constructor(changeX: boolean, changeY: boolean) {
            super();
   
            this._changeX = changeX;
            this._changeY = changeY;

            this._handlerShape = this.toolScene.add.rectangle(0, 0, 12, 12, TileSizeTool.FILL_STYLE);
        }

        canEdit(obj: any): boolean {
            return obj instanceof Phaser.GameObjects.TileSprite;
        }

        clear() {
            this._handlerShape.visible = false;
        }

        render(list: Phaser.GameObjects.TileSprite[]) {
            const pos = new Phaser.Math.Vector2(0, 0);

            for (let obj of list) {
                let sprite = <Phaser.GameObjects.TileSprite>obj;

                let localLeft = -sprite.width * sprite.originX;
                let localTop = -sprite.height * sprite.originY;

                let worldXY = new Phaser.Math.Vector2();
                let worldTx = sprite.getWorldTransformMatrix();

                let localX = this._changeX ? localLeft + sprite.width : localLeft + sprite.width / 2;
                let localY = this._changeY ? localTop + sprite.height : localTop + sprite.height / 2;

                worldTx.transformPoint(localX, localY, worldXY);

                pos.add(worldXY);
            }

            const len = this.getObjects().length;
            const cam = Editor.getInstance().getObjectScene().cameras.main;

            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;

            this._handlerShape.setPosition(cameraX, cameraY);
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

                    console.log("initLocalPos " + initLocalPos.x + " " + initLocalPos.y);

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

            console.log("onMouseMove");

            const worldTx = new Phaser.GameObjects.Components.TransformMatrix();

            for (let obj of this.getObjects()) {
                const sprite = <Phaser.GameObjects.TileSprite>obj;
                const data = sprite.data.get("TileSizeTool");
                const initLocalPos: Phaser.Math.Vector2 = data.initLocalPos;

                const localPos = new Phaser.Math.Vector2();
                sprite.getWorldTransformMatrix(worldTx);
                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);

                const dx = localPos.x - initLocalPos.x;
                const dy = localPos.y - initLocalPos.y;

                const width = (data.initWidth + dx) | 0;
                const height = (data.initHeight + dy) | 0;

                if (this._changeX) {
                    sprite.setSize(width, sprite.height);
                }

                if (this._changeY) {
                    sprite.setSize(sprite.width, height);
                }

                console.log(dx + " " + dy);
            }
        }

        onMouseUp() {

            if (this._dragging) {
                const msg = BuildMessage.SetTileSpriteProperties(this.getObjects());                
                Editor.getInstance().sendMessage(msg);
            }

            this._dragging = false;

            this._handlerShape.setFillStyle(TileSizeTool.FILL_STYLE);

            this.requestRepaint = true;
        }

    }
}