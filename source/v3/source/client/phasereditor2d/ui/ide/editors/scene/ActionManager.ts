namespace phasereditor2d.ui.ide.editors.scene {

    export class ActionManager {

        private _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            this._editor = editor;
        }

        deleteObjects() {
            console.log("scene editor delete objects!");
        }

    }

}