namespace PhaserEditor2D {

    export class ToolScene extends Phaser.Scene {

        private _selectedObjects: Phaser.GameObjects.GameObject[];
        private _selectionGraphics: Phaser.GameObjects.Graphics;
        private _gridGraphics: Phaser.GameObjects.Graphics;


        constructor() {
            super("ToolScene");

            this._selectedObjects = [];
            this._selectionGraphics = null;
        }

        create() {
            this.cameras.main.setOrigin(0, 0);
            this.scale.resize(window.innerWidth, window.innerHeight);

            this._gridGraphics = this.add.graphics({
                lineStyle: {
                    width: 1,
                    color: 0xffffff,
                    alpha: 0.5
                }
            });

            this._selectionGraphics = this.add.graphics({
                fillStyle: {
                    color: 0x00ff00
                },
                lineStyle: {
                    color: 0x00ff00,
                    width: 2
                }
            });
        }


        private _gridToken = null;

        private renderGrid() {
            var cam = Editor.getInstance().getObjectScene().cameras.main;


            var w = this.scale.baseSize.width;
            var h = this.scale.baseSize.height;

            var dx = 32 * cam.zoom;
            var dy = 32 * cam.zoom;

            var sx = dx - cam.scrollX * cam.zoom % dx;
            var sy = dy - cam.scrollY * cam.zoom % dy;

            sx -= dx;
            sy -= dy;

            const token = w + "-" + h + "-" + dx + "-" + dy + "-" + sx + "-" + sy;

            if (this._gridToken !== null && this._gridToken === token) {
                return;
            }

            this._gridToken = token;

            this._gridGraphics.clear();

            for (let x = sx; x < w; x += dx) {
                this._gridGraphics.lineBetween(x, 0, x, h);
            }

            for (let y = sy; y < window.innerHeight; y += dy) {
                this._gridGraphics.lineBetween(0, y, w, y);
            }
        }

        updateSelectionObjects() {
            this._selectedObjects = [];

            var objectScene = Editor.getInstance().getObjectScene();

            for (let id of Models.selection) {
                const obj = objectScene.sys.displayList.getByName(id);
                if (obj) {
                    this._selectedObjects.push(obj);
                }
            }
        }

        update() {
            this.renderGrid();
            this.renderSelection();
        }

        private renderSelection() {
            this._selectionGraphics.clear();


            const g2 = this._selectionGraphics;

            /** @type {Phaser.Cameras.Scene2D.Camera} */
            const cam = Editor.getInstance().getObjectScene().cameras.main;

            const point = new Phaser.Math.Vector2(0, 0);

            for (let obj of this._selectedObjects) {
                var worldTx = (<Phaser.GameObjects.Components.Transform><any>obj).getWorldTransformMatrix();

                worldTx.transformPoint(0, 0, point);

                point.x = (point.x - cam.scrollX) * cam.zoom;
                point.y = (point.y - cam.scrollY) * cam.zoom;

                this.paintSelectionBox(g2, obj);
            }
        }

        private _selectionBoxPoints: Phaser.Math.Vector2[] = [
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0)
        ];

        private paintSelectionBox(graphics: Phaser.GameObjects.Graphics, gameObj: any) {
            var w = gameObj.width;
            var h = gameObj.height;
            var ox = gameObj.originX;
            var oy = gameObj.originY;
            var x = -w * ox;
            var y = -h * oy;

            var worldTx = gameObj.getWorldTransformMatrix();

            worldTx.transformPoint(x, y, this._selectionBoxPoints[0]);
            worldTx.transformPoint(x + w, y, this._selectionBoxPoints[1]);
            worldTx.transformPoint(x + w, y + h, this._selectionBoxPoints[2]);
            worldTx.transformPoint(x, y + h, this._selectionBoxPoints[3]);

            var cam = Editor.getInstance().getObjectScene().cameras.main;
            for (let p of this._selectionBoxPoints) {
                p.set((p.x - cam.scrollX) * cam.zoom, (p.y - cam.scrollY) * cam.zoom)
            }

            graphics.lineStyle(4, 0x000000);
            graphics.strokePoints(this._selectionBoxPoints, true);
            graphics.lineStyle(2, 0x00ff00);
            graphics.strokePoints(this._selectionBoxPoints, true);
        }
    }

}