namespace phasereditor2d.ui.ide.editors.scene {

    export class GameScene extends Phaser.Scene {

        private _editor : SceneEditor;

        constructor(editor : SceneEditor) {
            super("ObjectScene");

            this._editor = editor;
        }

        getCamera() {
            return this.cameras.main;
        }

        create() {
            this.add.text(100, 100, "Hello scene editor");
        }
    }

}