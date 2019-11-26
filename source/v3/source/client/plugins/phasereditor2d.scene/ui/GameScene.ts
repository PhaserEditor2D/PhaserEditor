
namespace phasereditor2d.scene.ui {

    export class GameScene extends Phaser.Scene {

        private _sceneType: json.SceneType;
        private _inEditor: boolean;
        private _initialState: any;

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

        setInitialState(state: any) {
            this._initialState = state;
        }

        create() {

            if (this._inEditor) {

                const camera = this.getCamera();
                camera.setOrigin(0, 0);
                //camera.backgroundColor = Phaser.Display.Color.ValueToColor("#6e6e6e");
                camera.backgroundColor = Phaser.Display.Color.ValueToColor("#8e8e8e");

                if (this._initialState) {

                    camera.zoom = this._initialState.cameraZoom ?? camera.zoom;
                    camera.scrollX = this._initialState.cameraScrollX ?? camera.scrollX;
                    camera.scrollY = this._initialState.cameraScrollY ?? camera.scrollY;

                    this._initialState = null;
                }
            }

        }
    }

}