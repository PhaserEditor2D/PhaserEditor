namespace phasereditor2d.ui.ide.editors.scene.json {

    export class SceneWriter {

        private _scene: GameScene;

        constructor(scene: GameScene) {
            this._scene = scene;
        }

        toJSON(): SceneData {

            const sceneData: SceneData = {
                sceneType: this._scene.getSceneType(),
                displayList: []
            };

            for (const obj of this._scene.sys.displayList.getChildren()) {
                const objData = {};
                obj.writeJSON(objData);
                sceneData.displayList.push(objData);
            }

            return sceneData;
        }


        toString() : string {

            const json = this.toJSON();

            return JSON.stringify(json);
        }

    }

}