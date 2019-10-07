/// <reference path="./SceneEditorOperation.ts" />

namespace phasereditor2d.ui.ide.editors.scene.undo {

    export class AddObjectsOperation extends SceneEditorOperation {


        undo(): void {
            console.log("Remove the objects");
        }

        redo(): void {
            console.log("Add the objects");
        }

        execute(): void {
            console.log("Add the objects");
        }

    }

}