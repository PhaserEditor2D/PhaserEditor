var GAME = null;
namespace phasereditor2d.scene.ui {

    export class GameScene extends Phaser.Scene {

        private _sceneType: json.SceneType;
        private _inEditor: boolean;

        constructor(inEditor = true) {
            super("ObjectScene");

            this._inEditor = inEditor;

            this._sceneType = "Scene";
        }

        getSceneType() {
            return this._sceneType;
        }

        setSceneType(sceneType: json.SceneType): void {
            this._sceneType = sceneType;
        }

        getCamera() {
            return this.cameras.main;
        }

        create() {

            if (this._inEditor) {

                const camera = this.getCamera();
                camera.setOrigin(0, 0);
                camera.backgroundColor = Phaser.Display.Color.ValueToColor("#6e6e6e");
            }

        }
    }

}