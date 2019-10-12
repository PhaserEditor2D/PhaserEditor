/// <reference path="../../core/io/SyncFileContentCache.ts" />

namespace colibri.ui.ide {

    export class ImageFileCache extends core.io.SyncFileContentCache<controls.IImage> {

        constructor() {
            super(file => new controls.DefaultImage(new Image(), file.getUrl()));
        }

    }

}