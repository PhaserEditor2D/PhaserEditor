namespace phasereditor2d.ui.ide.editors.pack {

    export class AssetFinder {

        private _packs : AssetPack[];

        constructor(packs : AssetPack[]) {
            this._packs = packs;
        }

        static async create() {
            return new AssetFinder(await AssetPackUtils.getAllPacks());
        }

        async update() {
            this._packs = await AssetPackUtils.getAllPacks();
        }

        getPacks() {
            return this._packs;
        }

        findAssetPackItem(key: string) {
            return this._packs
                .flatMap(pack => pack.getItems())
                .find(item => item.getKey() === key);
        }

        getAssetPackItemOrFrame(key: string, frame: any) {

            let item = this.findAssetPackItem(key);

            if (!item) {
                return null;
            }

            if (item.getType() === IMAGE_TYPE) {

                if (frame === null || frame === undefined) {
                    return item;
                }

                return null;

            } else if (AssetPackUtils.isImageFrameContainer(item)) {

                const frames = AssetPackUtils.getImageFrames(item);

                const imageFrame = frames.find(imageFrame => imageFrame.getName() === frame);

                return imageFrame;
            }

            return item;
        }

        getAssetPackItemImage(key: string, frame: any): controls.IImage {

            const asset = this.getAssetPackItemOrFrame(key, frame);

            if (asset instanceof pack.AssetPackItem && asset.getType() === pack.IMAGE_TYPE) {

                return pack.AssetPackUtils.getImageFromPackUrl(asset.getData().url);

            } else if (asset instanceof pack.AssetPackImageFrame) {

                return asset;

            }

            return new controls.ImageWrapper(null);
        }

    }

}