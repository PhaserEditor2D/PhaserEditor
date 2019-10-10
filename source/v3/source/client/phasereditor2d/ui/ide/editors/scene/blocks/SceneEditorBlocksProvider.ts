namespace phasereditor2d.ui.ide.editors.scene.blocks {

    export class SceneEditorBlocksProvider extends EditorViewerProvider {

        private _contentProvider: SceneEditorBlocksContentProvider;
        private _assetFinder: pack.AssetFinder;

        constructor(assetFinder: pack.AssetFinder) {
            super();

            this._assetFinder = assetFinder;
        }

        async preload() {

            if (this._contentProvider) {
                return;
            }

            await this._assetFinder.update();

            this._contentProvider = new SceneEditorBlocksContentProvider(this._assetFinder);

            await pack.AssetPackUtils.preloadAssetPackItems(this._contentProvider.getPackItems());
        }

        getContentProvider(): controls.viewers.ITreeContentProvider {
            return this._contentProvider;
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

        getPropertySectionProvider(): controls.properties.PropertySectionProvider {
            return new SceneEditorBlocksPropertyProvider();
        }

        getInput() {
            return this;
        }
    }
}