namespace phasereditor2d.ui.ide.editors.pack {

    const IMAGE_FRAME_CONTAINER_TYPES = new Set([
        IMAGE_TYPE,
        MULTI_ATLAS_TYPE,
        ATLAS_TYPE,
        UNITY_ATLAS_TYPE,
        ATLAS_XML_TYPE,
        SPRITESHEET_TYPE
    ]);

    export class AssetPackUtils {

        static isImageFrameContainer(packItem: AssetPackItem) {
            return IMAGE_FRAME_CONTAINER_TYPES.has(packItem.getType());
        }

        static getImageFrames(packItem: AssetPackItem) {
            const parser = this.getImageFrameParser(packItem);
            if (parser) {
                return parser.parse();
            }
            return [];
        }

        static getImageFrameParser(packItem: AssetPackItem) {
            switch (packItem.getType()) {
                case IMAGE_TYPE:
                    return new pack.parsers.ImageParser(packItem);
                case ATLAS_TYPE:
                    return new pack.parsers.AtlasParser(packItem);
                case ATLAS_XML_TYPE:
                    return new pack.parsers.AtlasXMLParser(packItem);
                case UNITY_ATLAS_TYPE:
                    return new pack.parsers.UnityAtlasParser(packItem);
                case MULTI_ATLAS_TYPE:
                    return new pack.parsers.MultiAtlasParser(packItem);
                case SPRITESHEET_TYPE:
                    return new pack.parsers.SpriteSheetParser(packItem);
                default:
                    break;
            }
            return null;
        }

        static async preloadAssetPackItems(packItems: AssetPackItem[]) {
            for (const item of packItems) {
                if (this.isImageFrameContainer(item)) {
                    const parser = this.getImageFrameParser(item);
                    await parser.preload();
                }
            }
        }

        static async getAllPacks() {
            const files = await FileUtils.getFilesWithContentType(CONTENT_TYPE_ASSET_PACK);

            const packs: AssetPack[] = [];

            for (const file of files) {
                const pack = await AssetPack.createFromFile(file);
                packs.push(pack);
            }

            return packs;
        }

        static findAssetPackItem(packs: AssetPack[], key: string) {
            return packs
                .flatMap(pack => pack.getItems())
                .find(item => item.getKey() === key);
        }

        static getAssetPackItemOrFrame(packs: AssetPack[], key: string, frame: any) {

            let item = this.findAssetPackItem(packs, key);

            if (!item) {
                return null;
            }

            if (item.getType() === IMAGE_TYPE) {

                if (frame === null || frame === undefined) {
                    return item;
                }

                return null;

            } else if (this.isImageFrameContainer(item)) {

                const frames = this.getImageFrames(item);

                const imageFrame = frames.find(imageFrame => imageFrame.getName() === frame);

                return imageFrame;
            }

            return item;
        }

        static getAssetPackItemImage(packs: AssetPack[], key: string, frame: any): controls.IImage {

            const asset = this.getAssetPackItemOrFrame(packs, key, frame);

            if (asset instanceof pack.AssetPackItem && asset.getType() === pack.IMAGE_TYPE) {

                return pack.AssetPackUtils.getImageFromPackUrl(asset.getData().url);

            } else if (asset instanceof pack.AssetPackImageFrame) {

                return asset;

            }

            return new controls.ImageWrapper(null);
        }

        static getFileFromPackUrl(url: string): core.io.FilePath {
            return FileUtils.getFileFromPath(url);
        }

        static getFileStringFromPackUrl(url: string): string {
            const file = FileUtils.getFileFromPath(url);
            const str = Workbench.getWorkbench().getFileStorage().getFileStringFromCache(file);
            return str;
        }

        static getFileJSONFromPackUrl(url: string): any {
            const str = this.getFileStringFromPackUrl(url);
            return JSON.parse(str);
        }
        static getFileXMLFromPackUrl(url: string): Document {
            const str = this.getFileStringFromPackUrl(url);
            const parser = new DOMParser();
            return parser.parseFromString(str, "text/xml");
        }

        static getImageFromPackUrl(url: string): controls.IImage {
            const file = this.getFileFromPackUrl(url);
            if (file) {
                return Workbench.getWorkbench().getFileImage(file);
            }
            return null;
        }

    }
}