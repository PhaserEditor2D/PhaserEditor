namespace PhaserEditor2D {

    export  const ARROW_LENGTH = 80;

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

        protected objectGlobalAngle(obj : Phaser.GameObjects.GameObject) {
            let a : number = (<any>obj).angle;

            const parent = obj.parentContainer;

            if (parent) {
                a += this.objectGlobalAngle(parent);
            }

            return a;
        }

        protected createArrowShape() {
            const s = this.toolScene.add.triangle(0, 0, 0, 0, 12, 0, 6, 12);
            s.setStrokeStyle(1, 0, 0.8);
            return s;
        }

        protected createRectangleShape() {
            const s = this.toolScene.add.rectangle(0, 0, 12, 12);
            s.setStrokeStyle(1, 0, 0.8);
            return s;
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