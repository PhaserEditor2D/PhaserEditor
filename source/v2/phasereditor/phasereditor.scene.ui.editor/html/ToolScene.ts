namespace PhaserEditor2D {

    export class ToolScene extends Phaser.Scene {

        private _selectedObjects: Phaser.GameObjects.GameObject[];
        private _selectionGraphics: Phaser.GameObjects.Graphics;


        constructor() {
            super("ToolScene");

            this._selectedObjects = [];
            this._selectionGraphics = null;
        }

        create() {
            this.cameras.main.setOrigin(0, 0);
            this.scale.resize(window.innerWidth, window.innerHeight);
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
            if (this._selectionGraphics !== null) {
                this._selectionGraphics.destroy();
                this._selectionGraphics = null;
            }

            this._selectionGraphics = this.add.graphics({
                fillStyle: {
                    color: 0x00ff00
                },
                lineStyle: {
                    color: 0x00ff00
                }
            });

            const g2 = this._selectionGraphics;

            /** @type {Phaser.Cameras.Scene2D.Camera} */
            const cam = Editor.getInstance().getObjectScene().cameras.main;

            const point = new Phaser.Math.Vector2(0, 0);

            for (let obj of this._selectedObjects) {
                var worldTx = (<Phaser.GameObjects.Components.Transform><any>obj).getWorldTransformMatrix();

                worldTx.transformPoint(0, 0, point);

                point.x = (point.x - cam.scrollX) * cam.zoom;
                point.y = (point.y - cam.scrollY) * cam.zoom;

                g2.fillCircle(point.x, point.y, 10);

                this.paintSelectionBox(g2, obj);
            }
        }

        private paintSelectionBox(graphics: Phaser.GameObjects.Graphics, gameObj: any) {
            var w = gameObj.width;
            var h = gameObj.height;
            var ox = gameObj.originX;
            var oy = gameObj.originY;
            var x = -w * ox;
            var y = -h * oy;

            var worldTx = gameObj.getWorldTransformMatrix();

            var p1 = new Phaser.Math.Vector2(0, 0);
            var p2 = new Phaser.Math.Vector2(0, 0);
            var p3 = new Phaser.Math.Vector2(0, 0);
            var p4 = new Phaser.Math.Vector2(0, 0);

            worldTx.transformPoint(x, y, p1);
            worldTx.transformPoint(x + w, y, p2);
            worldTx.transformPoint(x + w, y + h, p3);
            worldTx.transformPoint(x, y + h, p4);

            const points = [p1, p2, p3, p4];
            var cam = Editor.getInstance().getObjectScene().cameras.main;
            for(let p of points) {
                p.set((p.x - cam.scrollX) * cam.zoom, (p.y - cam.scrollY) * cam.zoom)
            }

            graphics.strokePoints(points, true);
        }
    }

}