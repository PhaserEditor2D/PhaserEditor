namespace phasereditor2d.ui.ide.editors.scene.undo {

    export class RemoveObjectsOperation extends AddObjectsOperation {


        constructor(editor: SceneEditor, objects: Phaser.GameObjects.GameObject[]) {
            super(editor, objects);
        }

        undo(): void {
            super.redo();
        }

        redo(): void {
            super.undo();
        }
    }

}