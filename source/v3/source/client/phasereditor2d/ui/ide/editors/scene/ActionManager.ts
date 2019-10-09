namespace phasereditor2d.ui.ide.editors.scene {

    export class ActionManager {

        private _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            this._editor = editor;
        }

        deleteObjects() {
            const objects = this._editor
                .getSelection()
                .filter(obj => obj instanceof Phaser.GameObjects.GameObject);

            // create the undo-operation before destroy the objects
            this._editor.getUndoManager().add(new undo.RemoveObjectsOperation(this._editor, objects));

            for (const obj of objects) {
                (<Phaser.GameObjects.GameObject>obj).destroy();
            }

            this._editor.refreshOutline();
            this._editor.getSelectionManager().cleanSelection();
            this._editor.setDirty(true);
            this._editor.repaint();
        }

    }

}