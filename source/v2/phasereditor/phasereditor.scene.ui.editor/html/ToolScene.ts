namespace PhaserEditor2D {

    export class ToolScene extends Phaser.Scene {


        private _selectedObjects: Phaser.GameObjects.GameObject[];
        private _selectionGraphics: Phaser.GameObjects.Graphics;
        private _gridGraphics: Phaser.GameObjects.Graphics;
        private _tools: InteractiveTool[];


        constructor() {
            super("ToolScene");

            this._selectedObjects = [];
            this._selectionGraphics = null;
            this._tools = [];
        }

        create() {
            this.initCamera();

            this._axisToken = "";

            this._gridGraphics = this.add.graphics();

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

        initCamera() {
            this.cameras.main.setRoundPixels(true);
            this.cameras.main.setOrigin(0, 0);
        }


        updateFromSceneProperties() {
            this._axisToken = "";

            this.renderAxis();
        }

        private _axisToken: string = null;
        private _axisLabels: Phaser.GameObjects.Text[] = [];

        private renderAxis() {
            const editor = Editor.getInstance();

            const cam = editor.getObjectScene().cameras.main;


            const w = window.innerWidth;
            const h = window.innerHeight;

            let dx = 16;
            let dy = 16;

            if (ScenePropertiesComponent.get_snapEnabled(editor.sceneProperties)) {
                dx = ScenePropertiesComponent.get_snapWidth(editor.sceneProperties);
                dy = ScenePropertiesComponent.get_snapHeight(editor.sceneProperties);
            }

            let i = 1;
            while (dx * i * cam.zoom < 32) {
                i++;
            }
            dx = dx * i;

            i = 1;
            while (dy * i * cam.zoom < 32) {
                i++;
            }
            dy = dy * i;

            const sx = ((cam.scrollX / dx) | 0) * dx;
            const sy = ((cam.scrollY / dy) | 0) * dy;

            const bx = ScenePropertiesComponent.get_borderX(editor.sceneProperties);
            const by = ScenePropertiesComponent.get_borderY(editor.sceneProperties);
            const bw = ScenePropertiesComponent.get_borderWidth(editor.sceneProperties);
            const bh = ScenePropertiesComponent.get_borderHeight(editor.sceneProperties);

            const token = w + "-" + h + "-" + dx + "-" + dy + "-" + cam.zoom + "-" + cam.scrollX + "-" + cam.scrollY
                + "-" + bx + "-" + by + "-" + bw + "-" + bh;

            if (this._axisToken !== null && this._axisToken === token) {
                return;
            }

            this._axisToken = token;

            this._gridGraphics.clear();

            const fg = Phaser.Display.Color.RGBStringToColor("rgb(" + ScenePropertiesComponent.get_foregroundColor(editor.sceneProperties) + ")");

            this._gridGraphics.lineStyle(1, fg.color, 0.5);

            for (const label of this._axisLabels) {
                label.destroy();
            }

            // labels

            let label: Phaser.GameObjects.Text = null;
            let labelHeight = 0;

            for (let x = sx; ; x += dx) {
                const x2 = (x - cam.scrollX) * cam.zoom;

                if (x2 > w) {
                    break;
                }

                if (label != null) {
                    if (label.x + label.width * 2 > x2) {
                        continue;
                    }
                }

                label = this.add.text(x2, 0, x.toString());
                label.style.setShadow(1, 1);
                this._axisLabels.push(label);
                labelHeight = label.height;
                label.setOrigin(0.5, 0);
            }



            let labelWidth = 0;

            for (let y = sy; ; y += dy) {
                const y2 = (y - cam.scrollY) * cam.zoom;
                if (y2 > h) {
                    break;
                }

                if (y2 < labelHeight) {
                    continue;
                }

                const label = this.add.text(0, y2, (y).toString());
                label.style.setShadow(1, 1);
                label.setOrigin(0, 0.5);
                this._axisLabels.push(label);

                labelWidth = Math.max(label.width, labelWidth);
            }

            // lines

            for (let x = sx; ; x += dx) {
                const x2 = (x - cam.scrollX) * cam.zoom;

                if (x2 > w) {
                    break;
                }

                if (x2 < labelWidth) {
                    continue;
                }

                this._gridGraphics.lineBetween(x2, labelHeight, x2, h);
            }

            for (let y = sy; ; y += dy) {
                const y2 = (y - cam.scrollY) * cam.zoom;
                if (y2 > h) {
                    break;
                }

                if (y2 < labelHeight) {
                    continue;
                }

                this._gridGraphics.lineBetween(labelWidth, y2, w, y2);
            }

            // border

            this._gridGraphics.lineStyle(4, 0x000000, 1);

            this._gridGraphics.strokeRect(
                (bx - cam.scrollX) * cam.zoom, (by - cam.scrollY) * cam.zoom,
                bw * cam.zoom, bh * cam.zoom);

            this._gridGraphics.lineStyle(2, 0xffffff, 1);

            this._gridGraphics.strokeRect(
                (bx - cam.scrollX) * cam.zoom, (by - cam.scrollY) * cam.zoom,
                bw * cam.zoom, bh * cam.zoom);
        }

        getSelectedObjects() {
            return this._selectedObjects;
        }

        updateSelectionObjects() {
            const editor = Editor.getInstance();

            this._selectedObjects = [];

            let objectScene = Editor.getInstance().getObjectScene();

            for (let id of editor.selection) {
                const obj = objectScene.sys.displayList.getByName(id);
                if (obj) {
                    this._selectedObjects.push(obj);
                }
            }
        }

        update() {
            this.renderAxis();
            this.renderSelection();
            this.updateTools();
        }

        setTools(tools: InteractiveTool[]) {

            for (let tool of this._tools) {
                tool.clear();
            }

            this._tools = tools;
        }

        private updateTools() {
            for (let tool of this._tools) {
                tool.update();
            }
        }


        private renderSelection() {
            this._selectionGraphics.clear();


            const g2 = this._selectionGraphics;

            const cam = Editor.getInstance().getObjectScene().cameras.main;

            const point = new Phaser.Math.Vector2(0, 0);

            for (let obj of this._selectedObjects) {
                let worldTx = (<Phaser.GameObjects.Components.Transform><any>obj).getWorldTransformMatrix();

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
            let w = gameObj.width;
            let h = gameObj.height;

            if (gameObj instanceof Phaser.GameObjects.BitmapText) {
                // the bitmaptext width is considered a displayWidth, it is already multiplied by the scale
                w = w / gameObj.scaleX;
                h = h / gameObj.scaleY;
            }

            let ox = gameObj.originX;
            let oy = gameObj.originY;
            let x = -w * ox;
            let y = -h * oy;

            let worldTx = gameObj.getWorldTransformMatrix();

            worldTx.transformPoint(x, y, this._selectionBoxPoints[0]);
            worldTx.transformPoint(x + w, y, this._selectionBoxPoints[1]);
            worldTx.transformPoint(x + w, y + h, this._selectionBoxPoints[2]);
            worldTx.transformPoint(x, y + h, this._selectionBoxPoints[3]);

            let cam = Editor.getInstance().getObjectScene().cameras.main;
            for (let p of this._selectionBoxPoints) {
                p.set((p.x - cam.scrollX) * cam.zoom, (p.y - cam.scrollY) * cam.zoom)
            }

            graphics.lineStyle(4, 0x000000);
            graphics.strokePoints(this._selectionBoxPoints, true);
            graphics.lineStyle(2, 0x00ff00);
            graphics.strokePoints(this._selectionBoxPoints, true);
        }

        containsPointer(): boolean {
            for (let tool of this._tools) {
                const b = tool.containsPointer();
                if (b) {
                    return true;
                }
            }

            return false;
        }

        isEditing() {
            for (let tool of this._tools) {
                if (tool.isEditing()) {
                    return true;
                }
            }
            return false;
        }

        private testRepaint() {
            for (let tool of this._tools) {
                if (tool.requestRepaint) {
                    tool.requestRepaint = false;
                    Editor.getInstance().repaint();
                    return;
                }
            }
        }

        onMouseDown() {
            for (let tool of this._tools) {
                tool.onMouseDown();
            }

            this.testRepaint();
        }

        onMouseMove() {
            for (let tool of this._tools) {
                tool.onMouseMove();
            }

            this.testRepaint();
        }

        onMouseUp() {
            for (let tool of this._tools) {
                tool.onMouseUp();
            }

            this.testRepaint();
        }

    }
}