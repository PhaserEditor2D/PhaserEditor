namespace phasereditor2d.ui.ide.editors.scene.undo {

    import ide = colibri.ui.ide;

    export abstract class SceneEditorOperation extends ide.undo.Operation {

        protected _editor: SceneEditor;

        constructor(editor: SceneEditor) {
            super();

            this._editor = editor;
        }

    }

}