namespace phasereditor2d.ui.ide.editors.pack.viewers {

    import controls = colibri.ui.controls;

    export class ImageAssetPackItemCellRenderer extends controls.viewers.ImageCellRenderer {

        getImage(obj: any): controls.IImage {

            const item = <AssetPackItem>obj;
            const data = item.getData();

            return AssetPackUtils.getImageFromPackUrl(data.url);
        }

    }
}