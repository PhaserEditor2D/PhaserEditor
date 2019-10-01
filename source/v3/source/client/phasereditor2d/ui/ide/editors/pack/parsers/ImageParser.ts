namespace phasereditor2d.ui.ide.editors.pack.parsers {

    export class ImageParser extends ImageFrameParser {

        constructor(packItem: AssetPackItem) {
            super(packItem);
        }

        addToPhaserCache(game: Phaser.Game) {
            const item = this.getPackItem();

            if (!game.textures.exists(item.getKey())) {
                const url = item.getData().url;
                const image = <controls.DefaultImage>AssetPackUtils.getImageFromPackUrl(url);
                game.textures.addImage(item.getKey(), image.getImageElement());
            }
        }

        protected preloadFrames(): Promise<controls.PreloadResult> {
            const url = this.getPackItem().getData().url;
            const img = AssetPackUtils.getImageFromPackUrl(url);
            return img.preload();
        }

        protected parseFrames(): AssetPackImageFrame[] {
            const url = this.getPackItem().getData().url;
            const img = AssetPackUtils.getImageFromPackUrl(url);

            const fd = new controls.FrameData(0,
                new controls.Rect(0, 0, img.getWidth(), img.getHeight()),
                new controls.Rect(0, 0, img.getWidth(), img.getHeight()),
                new controls.Point(img.getWidth(), img.getWidth())
            );

            return [new AssetPackImageFrame(this.getPackItem(), this.getPackItem().getKey(), img, fd)];
        }


    }

}