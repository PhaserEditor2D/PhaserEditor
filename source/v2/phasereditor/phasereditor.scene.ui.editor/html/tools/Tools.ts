namespace PhaserEditor2D {

    export abstract class InteractiveTool {

        protected toolScene: ToolScene = Editor.getInstance().getToolScene();
        protected objScene: ObjectScene = Editor.getInstance().getObjectScene();
        requestRepaint = false;

        constructor() {
        }

        abstract canEdit(obj: any): boolean;

        getObjects(): Phaser.GameObjects.GameObject[] {
            const sel = this.toolScene.getSelectedObjects();
            return sel.filter(obj => this.canEdit(obj));
        }

        containsPointer(): boolean {
            return false;
        }

        isEditing() {
            return false;
        }

        clear() {

        }

        update() {
            const list = this.getObjects();

            if (list.length === 0) {
                this.clear();
            } else {
                this.render(list);
            }
        }

        render(objects: Phaser.GameObjects.GameObject[]) {

        }

        onMouseDown() {

        }

        onMouseUp() {

        }

        onMouseMove() {

        }

        protected getToolPointer() {
            return this.toolScene.input.activePointer;
        }

        protected getScenePoint(toolX : number, toolY : number) {
            const cam = this.objScene.cameras.main;
            
            const sceneX = toolX / cam.zoom + cam.scrollX;
            const sceneY = toolY / cam.zoom + cam.scrollY;

            return new Phaser.Math.Vector2(sceneX, sceneY);
        }
    }

    export class ToolFactory {

        static createByName(name: string): InteractiveTool[] {
            switch (name) {
                case "TileSize":
                    return [
                        new TileSizeTool(true, false),
                        new TileSizeTool(false, true),
                        new TileSizeTool(true, true)
                    ];
                case "TilePosition":
                    return [
                        new TilePositionTool(true, false),
                        new TilePositionTool(false, true),
                        new TilePositionTool(true, true)
                    ];
            }

            return [];
        }
    }
}