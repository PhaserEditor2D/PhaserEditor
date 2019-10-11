/// <reference path="../../../phasereditor2d/core/io/SyncFileContentCache.ts" />

namespace phasereditor2d.ui.ide {

    export class ImageFileCache extends core.io.SyncFileContentCache<controls.IImage> {

        constructor() {
            super(file => new controls.DefaultImage(new Image(), file.getUrl()));
        }

    }

}