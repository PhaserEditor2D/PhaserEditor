/// <reference path="../../../../../phasereditor2d.ui.controls/ImageFrame.ts" />

namespace phasereditor2d.ui.ide.editors.pack {

    export class AssetPackImageFrame extends controls.ImageFrame {

        private _packItem: AssetPackItem;

        constructor(packItem: AssetPackItem, name: string, frameImage: controls.IImage, frameData: controls.FrameData) {
            super(name, frameImage, frameData);

            this._packItem = packItem;
        }

        getPackItem() {
            return this._packItem;
        }
    }

}