namespace PhaserEditor2D {

    var PAINT_COUNT = 0;

    export class ToolScene extends Phaser.Scene {


        private _selectedObjects: Phaser.GameObjects.GameObject[];
        private _selectionGraphics: Phaser.GameObjects.Graphics;
        private _gridGraphics: Phaser.GameObjects.Graphics;
        private _paintCallsLabel: Phaser.GameObjects.Text;
        private _tools: InteractiveTool[];
        private _delayPaintOnMove: boolean;
        private _now: integer;

        constructor() {
            super("ToolScene");

            this._selectedObjects = [];
            this._selectionGraphics = null;
            this._tools = [];
            this._delayPaintOnMove = PhaserEditor2D.Editor.getInstance().isChromiumWebview();
        }

        create() {
            this.initCamera();

            this._axisToken = "";

            this._gridGraphics = this.add.graphics();
            this._gridGraphics.depth = -1;

            this._selectionGraphics = this.add.graphics({
                fillStyle: {
                    color: 0x00ff00
                },
                lineStyle: {
                    color: 0x00ff00,
                    width: 2
                }
            });

            this._selectionGraphics.depth = -1;

            this._paintCallsLabel = this.add.text(10, 10, "", { "color": "blue", "backgroundColor": "red" });
            this._paintCallsLabel.depth = 1000;
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
            this._axisLabels = [];

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
                ((bx - cam.scrollX) * cam.zoom), (by - cam.scrollY) * cam.zoom,
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


            this._paintCallsLabel.visible = Editor.getInstance().sceneProperties.debugPaintCalls;

            if (this._paintCallsLabel.visible) {
                this._paintCallsLabel.text = PAINT_COUNT.toString();
                PAINT_COUNT += 1;
            }
        }

        setTools(tools: InteractiveTool[]) {

            for (let tool of this._tools) {
                tool.clear();
            }

            for (let tool of tools) {
                tool.activated();
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

            for (let obj of this._selectedObjects) {
                this.paintSelectionBox(g2, <any>obj);
            }

            if (this._selectionDragStart) {
                const x = this._selectionDragStart.x;
                const y = this._selectionDragStart.y;
                const width = this._selectionDragEnd.x - x;
                const height = this._selectionDragEnd.y - y;
                const g2 = this._selectionGraphics;
                g2.lineStyle(4, 0x000000);
                g2.strokeRect(x, y, width, height);
                g2.lineStyle(2, 0x00ff00);
                g2.strokeRect(x, y, width, height);
            }
        }

        private _selectionBoxPoints: Phaser.Math.Vector2[] = [
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0),
            new Phaser.Math.Vector2(0, 0)
        ];

        private paintSelectionBox(graphics: Phaser.GameObjects.Graphics, sprite: Phaser.GameObjects.Sprite) {
            Editor.getInstance().getWorldBounds(sprite, this._selectionBoxPoints);

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

        onToolsMouseDown() {
            if (this._delayPaintOnMove) {
                this._now = Date.now();
            }

            for (let tool of this._tools) {
                tool.onMouseDown();
            }

            this.testRepaint();
        }

        onToolsMouseMove() {
            for (let tool of this._tools) {
                tool.onMouseMove();
            }

            if (this._delayPaintOnMove) {
                const now = Date.now();
                if (now - this._now > 40) {
                    this._now = now;
                    this.testRepaint();
                }
            } else {
                this.testRepaint();
            }
        }

        onToolsMouseUp() {
            for (let tool of this._tools) {
                tool.onMouseUp();
            }

            this.testRepaint();
        }


        private _selectionDragStart: Phaser.Math.Vector2 = null;
        private _selectionDragEnd: Phaser.Math.Vector2 = null;

        onSelectionDragMouseDown(e: MouseEvent) {
            if (!isLeftButton(e)) {
                return;
            }
            const pointer = this.input.activePointer;
            this._selectionDragStart = new Phaser.Math.Vector2(pointer.x, pointer.y);
            this._selectionDragEnd = this._selectionDragStart.clone();
        }

        onSelectionDragMouseMove(e: MouseEvent): boolean {
            if (this._selectionDragStart) {
                const pointer = this.input.activePointer;
                this._selectionDragEnd.set(pointer.x, pointer.y);
                return true;
            }
            return false;
        }

        selectionDragClear() {
            this._selectionDragStart = null;
            this._selectionDragEnd = null;
        }

        onSelectionDragMouseUp(e: MouseEvent) {
            if (this._selectionDragStart) {
                Editor.getInstance().getObjectScene().getPickManager().selectArea(this._selectionDragStart, this._selectionDragEnd);
                this.selectionDragClear();
            }
        }

    }

}