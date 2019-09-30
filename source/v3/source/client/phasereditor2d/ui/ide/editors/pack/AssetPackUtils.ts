namespace phasereditor2d.ui.ide.editors.pack {

    const IMAGE_FRAME_CONTAINER_TYPES = new Set([
        "multiatlas",
        "atlas",
        "unityAtlas",
        "atlasXML",
        "spritesheet"
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
                case "atlas":
                    return new pack.parsers.AtlasParser(packItem);
                case "atlasXML":
                    return new pack.parsers.AtlasXMLParser(packItem);
                case "unityAtlas":
                    return new pack.parsers.UnityAtlasParser(packItem);
                case "multiatlas":
                    return new pack.parsers.MultiAtlasParser(packItem);
                case "spritesheet":
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

            const packs = [];

            for (const file of files) {
                const pack = await AssetPack.createFromFile(file);
                packs.push(pack);
            }

            return packs;
        }

        static getFileFromPackUrl(url: string): core.io.FilePath {
            return FileUtils.getFileFromPath(url);
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