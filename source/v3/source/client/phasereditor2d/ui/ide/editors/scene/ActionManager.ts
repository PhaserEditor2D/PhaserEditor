namespace phasereditor2d.ui.ide.editors.scene {

    export class ActionManager {

        private _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            this._editor = editor;
        }

        deleteObjects() {
            const sel = this._editor.getSelection();

            for (const obj of sel) {
                if (obj instanceof Phaser.GameObjects.GameObject) {
                    obj.destroy();
                }
            }

            this._editor.getSelectionManager().cleanSelection();
            this._editor.repaint();
        }

    }

}