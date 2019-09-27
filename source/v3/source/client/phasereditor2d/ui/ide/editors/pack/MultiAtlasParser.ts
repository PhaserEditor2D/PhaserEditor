namespace phasereditor2d.ui.ide.editors.pack {

    export class MultiAtlasParser {

        private _packItem: AssetPackItem;

        constructor(packItem: AssetPackItem) {
            this._packItem = packItem;
        }

        async preload(): Promise<controls.PreloadResult> {
            if (this._packItem["__frames"]) {
                return controls.Controls.resolveNothingLoaded();
            }

            const data: Phaser.Loader.FileTypes.MultiAtlasFileConfig = this._packItem.getData();
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

        parse(): ImageFrame[] {

            if (this._packItem["__frames"]) {
                return this._packItem["__frames"];
            }

            const list: ImageFrame[] = [];

            const data: Phaser.Loader.FileTypes.MultiAtlasFileConfig = this._packItem.getData();
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
                                const frameData = this.buildFrameData(image, frame, list.length);
                                list.push(frameData);
                            }
                        }
                    }
                } catch (e) {
                    console.error(e);
                }
            }

            this._packItem["__frames"] = list;

            return list;
        }

        private buildFrameData(image: controls.IImage, frame: FrameDataType, index: number) {
            const src = new controls.Rect(frame.frame.x, frame.frame.y, frame.frame.w, frame.frame.h);
            const dst = new controls.Rect(frame.spriteSourceSize.x, frame.spriteSourceSize.y, frame.spriteSourceSize.w, frame.spriteSourceSize.h);
            const srcSize = new controls.Point(frame.sourceSize.w, frame.sourceSize.h);

            const frameData = new FrameData(index, src, dst, srcSize);
            return new ImageFrame(frame.filename, image, frameData);
        }
    }

}