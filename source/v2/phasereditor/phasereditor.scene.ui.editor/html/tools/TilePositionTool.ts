namespace PhaserEditor2D {
    export class TilePositionTool extends InteractiveTool {

        private _changeX: boolean;
        private _changeY: boolean;

        constructor(changeX: boolean, changeY: boolean) {
            super();

            this._changeX = changeX;
            this._changeY = changeY;
            
        }

        canEdit(obj: any): boolean {
            return obj instanceof Phaser.GameObjects.TileSprite;
        }
    }
}