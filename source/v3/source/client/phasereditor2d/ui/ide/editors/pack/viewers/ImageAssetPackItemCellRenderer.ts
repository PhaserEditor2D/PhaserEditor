/// <reference path="../../../../../../phasereditor2d.ui.controls/viewers/ImageCellRenderer.ts" />

namespace phasereditor2d.ui.ide.editors.pack.viewers {

    export class ImageAssetPackItemCellRenderer extends controls.viewers.ImageCellRenderer {

        getImage(obj: any): controls.IImage {
            const item = <AssetPackItem>obj;
            const data: Phaser.Loader.FileTypes.ImageFileConfig = item.getData();
            return AssetPackUtils.getImageFromPackUrl(data.url);
        }

    }
}