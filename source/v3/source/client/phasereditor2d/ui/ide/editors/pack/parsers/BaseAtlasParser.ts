/// <reference path="./ImageFrameParser.ts" />

namespace phasereditor2d.ui.ide.editors.pack.parsers {

    export abstract class BaseAtlasParser extends ImageFrameParser {

        constructor(packItem: AssetPackItem) {
            super(packItem);
        }

        async preloadFrames(): Promise<controls.PreloadResult> {
            const data = this.getPackItem().getData();

            const dataFile = AssetPackUtils.getFileFromPackUrl(data.atlasURL);
            let result1 = await FileUtils.preloadFileString(dataFile);

            const imageFile = AssetPackUtils.getFileFromPackUrl(data.textureURL);
            const image = FileUtils.getImage(imageFile);
            let result2 = await image.preload();

            return Math.max(result1, result2);
        }

        protected abstract parseFrames2(frames: controls.ImageFrame[], image: controls.IImage, atlas: string);

        parseFrames(): controls.ImageFrame[] {

            if (this.hasCachedFrames()) {
                return this.getCachedFrames();
            }

            const list: controls.ImageFrame[] = [];

            const data = this.getPackItem().getData();
            const dataFile = AssetPackUtils.getFileFromPackUrl(data.atlasURL);
            const imageFile = AssetPackUtils.getFileFromPackUrl(data.textureURL);
            const image = FileUtils.getImage(imageFile);

            if (dataFile) {
                const str = FileUtils.getFileStringFromCache(dataFile);
                try {
                    this.parseFrames2(list, image, str);
                } catch (e) {
                    console.error(e);
                }
            }

            return list;
        }

        static buildFrameData(image: controls.IImage, frame: FrameDataType, index: number) {
            const src = new controls.Rect(frame.frame.x, frame.frame.y, frame.frame.w, frame.frame.h);
            const dst = new controls.Rect(frame.spriteSourceSize.x, frame.spriteSourceSize.y, frame.spriteSourceSize.w, frame.spriteSourceSize.h);
            const srcSize = new controls.Point(frame.sourceSize.w, frame.sourceSize.h);

            const frameData = new controls.FrameData(index, src, dst, srcSize);
            return new controls.ImageFrame(frame.filename, image, frameData);
        }


    }
}