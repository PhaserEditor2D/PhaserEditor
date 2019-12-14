namespace phasereditor2d.scene.ui.blocks {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export class SceneEditorBlocksProvider extends ide.EditorViewerProvider {

        private _editor: editor.SceneEditor;
        private _packs : pack.core.AssetPack[];

        constructor(editor: editor.SceneEditor) {
            super();

            this._editor = editor;
            this._packs = [];
        }

        async preload() {

            const finder = new pack.core.PackFinder();

            await finder.preload();

            this._packs = finder.getPacks();
        }

        getContentProvider(): controls.viewers.ITreeContentProvider {
            return new SceneEditorBlocksContentProvider(() => this._packs);
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