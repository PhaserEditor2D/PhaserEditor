namespace phasereditor2d.pack.core {

    import controls = colibri.ui.controls;

    export class AtlasAssetPackItem extends AssetPackItem {

        constructor(pack : AssetPack, data : any) {
            super(pack, data)
        }

        preload() {

            return controls.Controls.resolveNothingLoaded();
        }

    }

}