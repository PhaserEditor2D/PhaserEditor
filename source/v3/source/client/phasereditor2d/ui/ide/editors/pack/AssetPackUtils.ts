namespace phasereditor2d.ui.ide.editors.pack {

    const IMAGE_FRAME_CONTAINER_TYPES = new Set([
        "multiatlas",
        "atlas",
        "unityAtlas",
        "atlasXML",
        "spritesheet"
    ]);

    export class AssetPackUtils {

        static isImageFrameContainer(packItem : AssetPackItem) {
            return IMAGE_FRAME_CONTAINER_TYPES.has(packItem.getType());
        }

        static getImageFrames(packItem : AssetPackItem) {
             
        }

        static async preloadAssetPackItems(packItems : AssetPackItem[]) {
            for(const item of packItems) {
                const type = item.getType();
                switch(type) {
                    case "multiatlas": {
                        const parser = new pack.parsers.MultiAtlasParser(item);
                        await parser.preload();
                        break;
                    }
                    case "atlas" : {
                        const parser = new pack.parsers.AtlasParser(item);
                        await parser.preload();
                        break;
                    }
                    case "unityAtlas" : {
                        const parser = new pack.parsers.UnityAtlasParser(item);
                        await parser.preload();
                        break;
                    }
                    case "atlasXML" : {
                        const parser = new pack.parsers.AtlasXMLParser(item);
                        await parser.preload();
                        break;
                    }
                    case "spritesheet" : {
                        const parser = new pack.parsers.SpriteSheetParser(item);
                        await parser.preload();
                        break;
                    }
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