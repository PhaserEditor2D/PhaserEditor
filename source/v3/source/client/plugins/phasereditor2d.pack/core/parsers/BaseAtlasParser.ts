/// <reference path="./ImageFrameParser.ts" />

namespace phasereditor2d.pack.core.parsers {

    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export abstract class BaseAtlasParser extends ImageFrameParser {

        constructor(packItem: AssetPackItem) {
            super(packItem);
        }

        addToPhaserCache(game: Phaser.Game) {
            const item = this.getPackItem();

            if (!game.textures.exists(item.getKey())) {

                const atlasURL = item.getData().atlasURL;
                const atlasData = AssetPackUtils.getFileJSONFromPackUrl(atlasURL);
                const textureURL = item.getData().textureURL;
                
                const image = <controls.DefaultImage>AssetPackUtils.getImageFromPackUrl(textureURL);

                if (image) {
                    game.textures.addAtlas(item.getKey(), image.getImageElement(), atlasData);
                }
            }
        }

        async preloadFrames(): Promise<controls.PreloadResult> {
            const data = this.getPackItem().getData();

            const dataFile = AssetPackUtils.getFileFromPackUrl(data.atlasURL);
            let result1 = await ide.FileUtils.preloadFileString(dataFile);

            const imageFile = AssetPackUtils.getFileFromPackUrl(data.textureURL);
            const image = ide.FileUtils.getImage(imageFile);

            if (image) {
                let result2 = await image.preload();

                return Math.max(result1, result2);
            }

            return result1;
        }

        protected abstract parseFrames2(frames: AssetPackImageFrame[], image: controls.IImage, atlas: string);

        parseFrames(): AssetPackImageFrame[] {
            const list: AssetPackImageFrame[] = [];

            const data = this.getPackItem().getData();
            const dataFile = AssetPackUtils.getFileFromPackUrl(data.atlasURL);
            const imageFile = AssetPackUtils.getFileFromPackUrl(data.textureURL);
            const image = ide.FileUtils.getImage(imageFile);

            if (dataFile) {
                const str = ide.FileUtils.getFileString(dataFile);
                try {
                    this.parseFrames2(list, image, str);
                } catch (e) {
                    console.error(e);
                }
            }

            return list;
        }
    }
}