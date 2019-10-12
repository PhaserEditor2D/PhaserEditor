namespace phasereditor2d.ui.ide.editors.pack {
    
    import controls = colibri.ui.controls;

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