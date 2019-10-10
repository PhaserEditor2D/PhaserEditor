namespace phasereditor2d.ui.ide.editors.pack {

    export class PackFinder {

        private static _packs: AssetPack[] = [];
        private static _loaded: boolean = false;

        private constructor() {
            
        }

        static async preload(): Promise<controls.PreloadResult> {

            if (this._loaded) {

                return controls.Controls.resolveNothingLoaded();
                
            }

            this._packs = await AssetPackUtils.getAllPacks();

            const items = this._packs.flatMap(pack => pack.getItems());

            await AssetPackUtils.preloadAssetPackItems(items);

            return controls.Controls.resolveResourceLoaded();
        }

        static getPacks() {
            return this._packs;
        }

        static findAssetPackItem(key: string) {
            return this._packs
                .flatMap(pack => pack.getItems())
                .find(item => item.getKey() === key);
        }

        static getAssetPackItemOrFrame(key: string, frame: any) {

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

        static getAssetPackItemImage(key: string, frame: any): controls.IImage {

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