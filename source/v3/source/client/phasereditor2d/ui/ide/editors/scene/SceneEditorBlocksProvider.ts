namespace phasereditor2d.ui.ide.editors.scene {

    //const SUPPORTED_PACK_ITEM_TYPES = new Set(["image", "atlas", "atlasXML", "multiatlas", "unityAtlas", "spritesheet"]);
    const SUPPORTED_PACK_ITEM_TYPES = new Set(["multiatlas"]);

    class SceneEditorBlocksContentProvider extends pack.AssetPackContentProvider {
        private _items: pack.AssetPackItem[];

        constructor(packs: pack.AssetPack[]) {
            super();

            this._items = packs
                .flatMap(pack => pack.getItems())
                .filter(item => SUPPORTED_PACK_ITEM_TYPES.has(item.getType()));
        }

        getItems() {
            return this._items;
        }

        getRoots(input: any): any[] {
            return this._items;
        }

        getChildren(parent: any): any[] {
            return super.getChildren(parent);
        }
    }

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

        getInput() {
            return this;
        }
    }
}