namespace phasereditor2d.ui.ide.editors.pack {
    export abstract class AbstractAtlasParser {

        private _packItem: AssetPackItem;

        constructor(packItem: AssetPackItem) {
            this._packItem = packItem;
        }

        async preload(): Promise<controls.PreloadResult> {
            if (this._packItem["__frames"]) {
                return controls.Controls.resolveNothingLoaded();
            }

            const data = this._packItem.getData();

            const dataFile = AssetPackUtils.getFileFromPackUrl(data.atlasURL);
            let result1 = await FileUtils.preloadFileString(dataFile);

            const imageFile = AssetPackUtils.getFileFromPackUrl(data.textureURL);
            const image = FileUtils.getImage(imageFile);
            let result2 = await image.preload();

            return Math.max(result1, result2);
        }

        protected abstract parse2(frames : ImageFrame[], image : controls.IImage, atlas : string);

        parse(): ImageFrame[] {

            if (this._packItem["__frames"]) {
                return this._packItem["__frames"];
            }

            const list: ImageFrame[] = [];

            const data = this._packItem.getData();
            const dataFile = AssetPackUtils.getFileFromPackUrl(data.atlasURL);
            const imageFile = AssetPackUtils.getFileFromPackUrl(data.textureURL);
            const image = FileUtils.getImage(imageFile); 

            if (dataFile) {
                const str = FileUtils.getFileStringFromCache(dataFile);
                try {
                    this.parse2(list, image, str);
                } catch (e) {
                    console.error(e);
                }
            }

            this._packItem["__frames"] = list;

            return list;
        }

        static buildFrameData(image: controls.IImage, frame: FrameDataType, index: number) {
            const src = new controls.Rect(frame.frame.x, frame.frame.y, frame.frame.w, frame.frame.h);
            const dst = new controls.Rect(frame.spriteSourceSize.x, frame.spriteSourceSize.y, frame.spriteSourceSize.w, frame.spriteSourceSize.h);
            const srcSize = new controls.Point(frame.sourceSize.w, frame.sourceSize.h);

            const frameData = new FrameData(index, src, dst, srcSize);
            return new ImageFrame(frame.filename, image, frameData);
        }


    }
}