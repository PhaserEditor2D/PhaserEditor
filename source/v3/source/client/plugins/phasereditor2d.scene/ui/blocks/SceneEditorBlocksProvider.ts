namespace phasereditor2d.scene.ui.blocks {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export class SceneEditorBlocksProvider extends ide.EditorViewerProvider {

        private _editor: editor.SceneEditor;

        constructor(editor: editor.SceneEditor) {
            super();

            this._editor = editor;
        }

        async preload() {
            await pack.core.PackFinder.preload();
        }

        getContentProvider(): controls.viewers.ITreeContentProvider {
            return new SceneEditorBlocksContentProvider();
        }

        getLabelProvider(): controls.viewers.ILabelProvider {
            return new SceneEditorBlocksLabelProvider();
        }

        getCellRendererProvider(): controls.viewers.ICellRendererProvider {
            return new SceneEditorBlocksCellRendererProvider();
        }

        getTreeViewerRenderer(viewer: controls.viewers.TreeViewer) {
            return new SceneEditorBlocksTreeRendererProvider(viewer);
        }

        getUndoManager() {
            return this._editor;
        }

        getPropertySectionProvider(): controls.properties.PropertySectionProvider {
            return new SceneEditorBlocksPropertyProvider();
        }

        getInput() {
            return this;
        }
    }
}