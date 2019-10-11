namespace phasereditor2d.ui.ide.editors.scene.blocks {

    export class SceneThumbnailCache extends core.io.FileContentCache<controls.IImage> {

        static _instance: SceneThumbnailCache;

        static getInstance() {

            if (!this._instance) {
                this._instance = new SceneThumbnailCache();
            }

            return this._instance;
        }

        private constructor() {
            super(async (file) => {

                const image = new SceneThumbnail(file);

                await image.preload();

                return Promise.resolve(image);
            });
        }

    }

}