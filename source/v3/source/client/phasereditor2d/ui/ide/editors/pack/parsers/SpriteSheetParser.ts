/// <reference path="./AbstractAtlasParser.ts" />

namespace phasereditor2d.ui.ide.editors.pack.parsers {

    export class SpriteSheetParser {

        private _packItem: AssetPackItem;

        constructor(packItem: AssetPackItem) {
            this._packItem = packItem;
        }

        async preload(): Promise<controls.PreloadResult> {
            if (this._packItem["__frames"]) {
                return controls.Controls.resolveNothingLoaded();
            }

            const data = this._packItem.getData();
            const imageFile = AssetPackUtils.getFileFromPackUrl(data.url);
            const image = FileUtils.getImage(imageFile);
            return await image.preload();
        }

        parse(): ImageFrame[] {

            if (this._packItem["__frames"]) {
                return this._packItem["__frames"];
            }

            const frames: ImageFrame[] = [];

            const data = this._packItem.getData();

            const imageFile = AssetPackUtils.getFileFromPackUrl(data.url);
            const image = FileUtils.getImage(imageFile);

            const w = data.frameConfig.frameWidth;
            const h = data.frameConfig.frameHeight;
            const margin = data.frameConfig.margin || 0;
            const spacing = data.frameConfig.spacing || 0;
            const startFrame = data.frameConfig.startFrame || 0;
            const endFrame = data.frameConfig.endFrame || -1;

            if (w <= 0 || h <= 0 || spacing < 0 || margin < 0) {
                // invalid values
                return frames;
            }

            const start = startFrame < 0 ? 0 : startFrame;
            const end = endFrame < 0 ? Number.MAX_VALUE : endFrame;

            let i = 0;
            let row = 0;
            let column = 0;
            let x = margin;
            let y = margin;

            while (true) {
                if (i > end || y >= image.getHeight() || i > 50) {
                    break;
                }

                if (i >= start) {
                    if (x + w <= image.getWidth() && y + h <= image.getHeight()) {
                        // FrameModel frame = new FrameModel(this, i, row, column, new Rectangle(x, y, w, h));
                        // list.add(frame);
                        const fd = new FrameData(i,
                            new controls.Rect(x, y, w, h),
                            new controls.Rect(0, 0, w, h),
                            new controls.Point(w, h)
                        );
                        frames.push(new ImageFrame(i.toString(), image, fd));
                    }
                }

                column++;

                x += w + spacing;

                if (x >= image.getWidth()) {
                    x = margin;
                    y += h + spacing;
                    column = 0;
                    row++;
                }

                i++;
            }

            this._packItem["__frames"] = frames;

            return frames;
        }
    }
}