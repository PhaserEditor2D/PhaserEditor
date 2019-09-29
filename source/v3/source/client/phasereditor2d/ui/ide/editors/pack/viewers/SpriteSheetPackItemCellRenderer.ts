namespace phasereditor2d.ui.ide.editors.pack.viewers {

    export class SpriteSheetPackItemCellRenderer extends controls.viewers.ImageCellRenderer {

        getImage(obj: any): controls.IImage {
            const item = <AssetPackItem> obj;
            const url = item.getData().url;
            const image = AssetPackUtils.getImageFromPackUrl(url);
            return image;
        }

    }
}