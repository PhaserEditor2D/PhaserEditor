namespace phasereditor2d.ui.ide.editors.pack.parsers {

    export abstract class ImageFrameParser {
        private _packItem: AssetPackItem;

        constructor(packItem: AssetPackItem) {
            this._packItem = packItem;
        }

        protected setCachedFrames(frames: controls.ImageFrame[]) {
            this._packItem.getEditorData()["__frames_cache"] = frames;
        }

        protected getCachedFrames(): controls.ImageFrame[] {
            return this._packItem.getEditorData()["__frames_cache"];
        }

        protected hasCachedFrames() {
            return "__frames_cache" in this._packItem.getEditorData();
        }

        getPackItem() {
            return this._packItem;
        }

        async preload(): Promise<controls.PreloadResult> {
            if (this.hasCachedFrames()) {
                return controls.Controls.resolveNothingLoaded();
            }

            return this.preloadFrames();
        }

        parse(): controls.ImageFrame[] {
            if (this.hasCachedFrames()) {
                return this.getCachedFrames();
            }

            const frames = this.parseFrames();

            this.setCachedFrames(frames);

            return frames;
        }

        protected abstract async preloadFrames(): Promise<controls.PreloadResult>;

        protected abstract parseFrames(): controls.ImageFrame[];
    }

}