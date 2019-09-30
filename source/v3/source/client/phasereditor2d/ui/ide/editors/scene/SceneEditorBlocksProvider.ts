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
            return new pack.viewers.AssetPackLabelProvider();
        }

        getCellRendererProvider(): controls.viewers.ICellRendererProvider {
            return new pack.viewers.AssetPackCellRendererProvider();
        }

        getTreeViewerRenderer(viewer : controls.viewers.TreeViewer) {
            return new pack.viewers.AssetPackBlocksTreeViewerRenderer(viewer);
        }

        getPropertySectionProvider(): controls.properties.PropertySectionProvider {
            return new SceneEditorBlockPropertyProvider();
        }

        getInput() {
            return this;
        }
    }
}