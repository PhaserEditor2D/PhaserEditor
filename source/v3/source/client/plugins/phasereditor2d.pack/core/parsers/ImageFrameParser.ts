namespace phasereditor2d.pack.core.parsers {

    import controls = colibri.ui.controls;

    export abstract class ImageFrameParser {
        
        private _packItem: AssetPackItem;

        constructor(packItem: AssetPackItem) {
            this._packItem = packItem;
        }

        protected setCachedFrames(frames: AssetPackImageFrame[]) {
            this._packItem.getEditorData()["__frames_cache"] = frames;
        }

        protected getCachedFrames(): AssetPackImageFrame[] {
            return this._packItem.getEditorData()["__frames_cache"];
        }

        protected hasCachedFrames() {
            return "__frames_cache" in this._packItem.getEditorData();
        }

        abstract addToPhaserCache(game: Phaser.Game): void;

        getPackItem() {
            return this._packItem;
        }

        async preload(): Promise<controls.PreloadResult> {

            if (this.hasCachedFrames()) {
                return controls.Controls.resolveNothingLoaded();
            }

            return this.preloadFrames();
        }

        parse(): AssetPackImageFrame[] {
            if (this.hasCachedFrames()) {
                return this.getCachedFrames();
            }

            const frames = this.parseFrames();

            this.setCachedFrames(frames);

            return frames;
        }

        protected abstract async preloadFrames(): Promise<controls.PreloadResult>;

        protected abstract parseFrames(): AssetPackImageFrame[];
    }

}