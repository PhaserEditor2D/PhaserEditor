namespace phasereditor2d.pack.core {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import core = colibri.core;

    export class PackFinder {

        private static _packs: AssetPack[] = [];
        private static _loaded: boolean = false;

        private constructor() {

        }

        static async preload(monitor: controls.IProgressMonitor = controls.EmptyProgressMonitor): Promise<controls.PreloadResult> {

            if (this._loaded) {

                monitor.addTotal(1);
                monitor.step();

                return controls.Controls.resolveNothingLoaded();

            }

            this._packs = await AssetPackUtils.getAllPacks();

            const items = this._packs.flatMap(pack => pack.getItems());

            monitor.addTotal(items.length);

            for (const item of items) {

                await item.preload();
                
                monitor.step();
            }

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

            } else if (item instanceof ImageFrameContainerAssetPackItem) {

                const imageFrame = item.findFrame(frame);

                return imageFrame;
            }

            return item;
        }

        static getAssetPackItemImage(key: string, frame: any): controls.IImage {

            const asset = this.getAssetPackItemOrFrame(key, frame);

            if (asset instanceof AssetPackItem && asset.getType() === IMAGE_TYPE) {

                return AssetPackUtils.getImageFromPackUrl(asset.getData().url);

            } else if (asset instanceof AssetPackImageFrame) {

                return asset;

            }

            return new controls.ImageWrapper(null);
        }

    }

}