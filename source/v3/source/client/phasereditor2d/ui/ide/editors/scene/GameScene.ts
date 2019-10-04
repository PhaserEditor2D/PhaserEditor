var GAME = null;
namespace phasereditor2d.ui.ide.editors.scene {

    export class GameScene extends Phaser.Scene {

        private _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            super("ObjectScene");

            this._editor = editor;
        }

        getCamera() {
            return this.cameras.main;
        }

        create() {
            const camera = this.getCamera();
            camera.setOrigin(0, 0);
            camera.backgroundColor = Phaser.Display.Color.ValueToColor("#6e6e6e");
        }
    }

}