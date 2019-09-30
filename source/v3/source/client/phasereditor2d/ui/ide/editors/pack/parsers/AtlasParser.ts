/// <reference path="./BaseAtlasParser.ts" />

namespace phasereditor2d.ui.ide.editors.pack.parsers {

    export class AtlasParser extends BaseAtlasParser {

        constructor(packItem : AssetPackItem) {
            super(packItem);
        }

        protected parseFrames2(imageFrames: ImageFrame[], image: controls.IImage, atlas: string) {
            try {
                const data = JSON.parse(atlas);
                if (Array.isArray(data.frames)) {
                    for (const frame of data.frames) {
                        const frameData = AtlasParser.buildFrameData(image, frame, imageFrames.length);
                        imageFrames.push(frameData);
                    }
                } else {
                    for(const name in data.frames) {
                        const frame = data.frames[name];
                        frame.filename = name;
                        const frameData = AtlasParser.buildFrameData(image, frame, imageFrames.length);
                        imageFrames.push(frameData);
                    }
                }
            } catch (e) {
                console.error(e);
            }
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