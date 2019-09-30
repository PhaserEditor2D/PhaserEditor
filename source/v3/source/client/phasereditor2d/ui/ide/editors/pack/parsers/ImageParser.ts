namespace phasereditor2d.ui.ide.editors.pack.parsers {

    export class ImageParser extends ImageFrameParser {

        constructor(packItem: AssetPackItem) {
            super(packItem);
        }

        protected preloadFrames(): Promise<controls.PreloadResult> {
            const url = this.getPackItem().getData().url;
            const img = AssetPackUtils.getImageFromPackUrl(url);
            return img.preload();
        }

        protected parseFrames(): controls.ImageFrame[] {
            const url = this.getPackItem().getData().url;
            const img = AssetPackUtils.getImageFromPackUrl(url);

            const fd = new controls.FrameData(0, 
                new controls.Rect(0, 0, img.getWidth(), img.getHeight()),
                new controls.Rect(0, 0, img.getWidth(), img.getHeight()), 
                new controls.Point(img.getWidth(), img.getWidth())
            );

            return [new controls.ImageFrame(this.getPackItem().getKey(), img, fd)];
        }


    }

}