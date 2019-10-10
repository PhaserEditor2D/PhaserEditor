namespace phasereditor2d.ui.ide.editors.pack.viewers {

    export class AssetPackCellRendererProvider implements controls.viewers.ICellRendererProvider {

        getCellRenderer(element: any): controls.viewers.ICellRenderer {

            if (element instanceof AssetPackItem) {

                const type = element.getType();

                switch (type) {
                    case pack.IMAGE_TYPE:
                        return new ImageAssetPackItemCellRenderer();
                    case pack.MULTI_ATLAS_TYPE:
                    case pack.ATLAS_TYPE:
                    case pack.UNITY_ATLAS_TYPE:
                    case pack.ATLAS_XML_TYPE:
                    case pack.SPRITESHEET_TYPE:
                        return new controls.viewers.FolderCellRenderer();
                    default:
                        break;
                }

            } else if (element instanceof controls.ImageFrame) {

                return new controls.viewers.ImageCellRenderer();

            }

            return new controls.viewers.EmptyCellRenderer();
        }

        preload(element: any): Promise<controls.PreloadResult> {
            return controls.Controls.resolveNothingLoaded();
        }
    }

}