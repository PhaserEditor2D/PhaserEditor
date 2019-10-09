namespace phasereditor2d.ui.ide.editors.scene.undo {

    export class RemoveObjectsOperation extends SceneEditorOperation {
        
        private _idList : string[];

        constructor(editor : SceneEditor, objects : Phaser.GameObjects.GameObject[]) {
            super(editor);
            this._idList = objects.map(obj => obj.getEditorId());
        }
        
        undo(): void {
            throw new Error("Method not implemented.");
        }        
        
        redo(): void {
            throw new Error("Method not implemented.");
        }


    }

}