namespace phasereditor2d.ui.ide.editors.scene {

    export class SceneEditorBlocksProvider extends EditorBlocksProvider {

        private _contentProvider: SceneEditorBlocksContentProvider;

        async preload() {
            const packs = await pack.AssetPackUtils.getAllPacks();

            this._contentProvider = new SceneEditorBlocksContentProvider(packs);

            await pack.AssetPackUtils.preloadAssetPackItems(this._contentProvider.getItems());
        }

        getContentProvider(): controls.viewers.ITreeContentProvider {
            return this._contentProvider;
        }

        getLabelProvider(): controls.viewers.ILabelProvider {
            return new pack.AssetPackLabelProvider();
        }

        getCellRendererProvider(): controls.viewers.ICellRendererProvider {
            return new pack.AssetPackCellRendererProvider();
        }

        getTreeViewerRenderer(viewer : controls.viewers.TreeViewer) {
            return new pack.BlocksTreeViewerRenderer(viewer);
        }

        getInput() {
            return this;
        }
    }
}