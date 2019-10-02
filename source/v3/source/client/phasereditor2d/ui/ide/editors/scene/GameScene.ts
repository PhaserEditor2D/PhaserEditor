
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
            this.getCamera().setOrigin(0, 0);
            
            this.add.text(0, 0, "Hello scene editor").setScale(2, 2).setOrigin(0, 0);
        }
    }

}