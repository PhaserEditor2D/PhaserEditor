namespace phasereditor2d.pack.core {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    import io = colibri.core.io;

    const IMAGE_FRAME_CONTAINER_TYPES = new Set([
        IMAGE_TYPE,
        MULTI_ATLAS_TYPE,
        ATLAS_TYPE,
        UNITY_ATLAS_TYPE,
        ATLAS_XML_TYPE,
        SPRITESHEET_TYPE
    ]);

    const ATLAS_TYPES = new Set([
        MULTI_ATLAS_TYPE,
        ATLAS_TYPE,
        UNITY_ATLAS_TYPE,
        ATLAS_XML_TYPE,
    ]);

    export class AssetPackUtils {

        static isAtlasType(type : string) {
            return ATLAS_TYPES.has(type);
        }

        static isAtlasPackItem(packItem: AssetPackItem) {
            return this.isAtlasType(packItem.getType());
        }

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
                    return new parsers.ImageParser(packItem);
                case ATLAS_TYPE:
                    return new parsers.AtlasParser(packItem);
                case ATLAS_XML_TYPE:
                    return new parsers.AtlasXMLParser(packItem);
                case UNITY_ATLAS_TYPE:
                    return new parsers.UnityAtlasParser(packItem);
                case MULTI_ATLAS_TYPE:
                    return new parsers.MultiAtlasParser(packItem);
                case SPRITESHEET_TYPE:
                    return new parsers.SpriteSheetParser(packItem);
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
            const files = await ide.FileUtils.getFilesWithContentType(CONTENT_TYPE_ASSET_PACK);

            const packs: AssetPack[] = [];

            for (const file of files) {
                const pack = await AssetPack.createFromFile(file);
                packs.push(pack);
            }

            return packs;
        }

        static getFileFromPackUrl(url: string): io.FilePath {
            return ide.FileUtils.getFileFromPath(url);
        }

        static getFileStringFromPackUrl(url: string): string {

            const file = ide.FileUtils.getFileFromPath(url);
            const str = ide.FileUtils.getFileString(file);

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
                return ide.Workbench.getWorkbench().getFileImage(file);
            }

            return null;
        }

    }
}