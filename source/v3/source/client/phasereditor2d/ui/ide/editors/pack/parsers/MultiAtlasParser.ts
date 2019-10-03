namespace phasereditor2d.ui.ide.editors.pack.parsers {

    export class MultiAtlasParser extends ImageFrameParser {

        constructor(packItem: AssetPackItem) {
            super(packItem);
        }

        addToPhaserCache(game: Phaser.Game) {
            const item = this.getPackItem();

            if (!game.textures.exists(item.getKey())) {
                const packItemData = item.getData();
                const atlasDataFile = AssetPackUtils.getFileFromPackUrl(packItemData.url);
                const atlasData = AssetPackUtils.getFileJSONFromPackUrl(packItemData.url);

                const images: HTMLImageElement[] = [];
                const jsonArrayData = [];

                for (const textureData of atlasData.textures) {
                    const imageName = textureData.image;
                    const imageFile = atlasDataFile.getSibling(imageName);
                    const image = <controls.DefaultImage>FileUtils.getImage(imageFile);
                    images.push(image.getImageElement());
                    jsonArrayData.push(textureData);
                }

                game.textures.addAtlasJSONArray(this.getPackItem().getKey(), images, jsonArrayData);
            }
        }

        async preloadFrames(): Promise<controls.PreloadResult> {
            const data = this.getPackItem().getData();
            const dataFile = AssetPackUtils.getFileFromPackUrl(data.url);

            if (dataFile) {
                let result = await FileUtils.preloadFileString(dataFile);
                const str = FileUtils.getFileStringFromCache(dataFile);
                try {
                    const data = JSON.parse(str);
                    if (data.textures) {
                        for (const texture of data.textures) {
                            const imageName: string = texture.image;
                            const imageFile = dataFile.getSibling(imageName);
                            if (imageFile) {
                                const image = Workbench.getWorkbench().getFileImage(imageFile);
                                const result2 = await image.preload();
                                result = Math.max(result, result2);
                            }
                        }
                    }
                } catch (e) {

                }

                return result;
            }

            return controls.Controls.resolveNothingLoaded();
        }

        parseFrames(): AssetPackImageFrame[] {
            const list: AssetPackImageFrame[] = [];

            const data = this.getPackItem().getData();
            const dataFile = AssetPackUtils.getFileFromPackUrl(data.url);

            if (dataFile) {

                const str = Workbench.getWorkbench().getFileStorage().getFileStringFromCache(dataFile);
                try {
                    const data = JSON.parse(str);
                    if (data.textures) {
                        for (const textureData of data.textures) {
                            const imageName = textureData.image;
                            const imageFile = dataFile.getSibling(imageName);
                            const image = FileUtils.getImage(imageFile);
                            for (const frame of textureData.frames) {
                                const frameData = AtlasParser.buildFrameData(this.getPackItem(), image, frame, list.length);
                                list.push(frameData);
                            }
                        }
                    }
                } catch (e) {
                    console.error(e);
                }
            }
            return list;
        }
    }

}